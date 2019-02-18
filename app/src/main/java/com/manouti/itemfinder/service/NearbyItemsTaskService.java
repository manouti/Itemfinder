package com.manouti.itemfinder.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.ui.auth.model.User;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.TaskParams;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.PlacedItemInfo;
import com.manouti.itemfinder.item.detail.ItemDetailActivity;
import com.manouti.itemfinder.item.nearby.NearbyItemsActivity;
import com.manouti.itemfinder.location.provider.CurrentLocationProvider;
import com.manouti.itemfinder.location.provider.AbstractLocationProvider.LocationResultCallback;
import com.manouti.itemfinder.home.MainActivity;
import com.manouti.itemfinder.map.MapsActivity;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.place.Place;
import com.manouti.itemfinder.model.place.PlaceItem;
import com.manouti.itemfinder.util.LocationUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class NearbyItemsTaskService extends GcmTaskService implements MediaPlayer.OnErrorListener {

    private static final String TAG = NearbyItemsTaskService.class.getSimpleName();

    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1313;

    private static final int NOTIFICATION_ID_NEARBY_ITEMS = 1;
    private static final int MESSAGE_MEDIA_PLAYER_RELEASE = 0x0000bbcc;
    private static final long MESSAGE_DELAY_MILLIS = 1000L;

    private static final long TASK_TIMEOUT_SECONDS = 20L;
    private static final float SOUND_VOLUME = 0.60f;
    private static final long VIBRATE_DURATION = 200L;

    private DatabaseReference mDatabaseReference;
    private GeoFire mPlacesGeoFire;
    private GeoQuery mGeoQuery;

    private final Object taskFinishMonitor = new Object();
    private CountDownLatch geoQueryKeyCountDownLatch;

    private final WeakReference<NearbyItemsTaskService> weakReferenceToThisTask = new WeakReference<>(this);
    private final MediaPlayerReleaseHandler mediaPlayerReleaseHandler = new MediaPlayerReleaseHandler(weakReferenceToThisTask);

    private MediaPlayer mediaPlayer;

    private DatabaseReference getOrCreateDatabaseReference() {
        if(mDatabaseReference == null) {
            mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        }
        return mDatabaseReference;
    }

    private GeoFire getOrCreatePlacesGeoFire() {
        if(mPlacesGeoFire == null) {
            mPlacesGeoFire = new GeoFire(getOrCreateDatabaseReference().child("places-geo"));
        }
        return mPlacesGeoFire;
    }

    @Override
    public void onInitializeTasks() {
        // When your package is removed or updated, all of its network tasks are cleared by
        // the GcmNetworkManager. You can override this method to reschedule them in the case of
        // an updated package. This is not called when your application is first installed.
        //
        // This is called on your application's main thread.

        Log.d(TAG, "onInitializeTasks");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean runServiceTask = prefs.getBoolean(ItemfinderPreferencesActivity.KEY_NEARBY_ITEMS_RUN_SERVICE, true);
        if(runServiceTask && FirebaseAuth.getInstance().getCurrentUser() != null) {
            GcmNetworkManager gcmNetworkManager = GcmNetworkManager.getInstance(this);
            PeriodicTask task = new PeriodicTask.Builder()
                    .setService(NearbyItemsTaskService.class)
                    .setTag(MainActivity.TASK_TAG_NEARBY_FAVORITE_ITEMS)
                    .setPeriod(MainActivity.SERVICE_TASK_PERIOD_SECONDS)
                    .build();
            gcmNetworkManager.schedule(task);
        } else {
            Log.d(TAG, "onInitializeTasks: setting disabled. Skipped scheduling of nearby items service.");
        }
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        String tag = taskParams.getTag();
        Log.d(TAG, "onRunTask: " + tag);

        NearbyItemsResult result;

        // Choose method based on the tag.
        if (MainActivity.TASK_TAG_NEARBY_FAVORITE_ITEMS.equals(tag)) {
            result = queryForNearbyFavoriteItems();
        } else {
            return GcmNetworkManager.RESULT_SUCCESS;
        }

        if(result.taskResult == GcmNetworkManager.RESULT_SUCCESS && !result.itemInfoList.isEmpty()) {
            boolean moreThanOneItem = result.itemInfoList.size() > 1;
            if(moreThanOneItem) {
                sendNotificationForMultipleItems(result.itemInfoList);
            } else {
                sendNotificationForSingleItem(result.itemInfoList.get(0));
            }

            playSoundAndVibrate();
        }

        return result.taskResult;
    }

    private void sendNotificationForMultipleItems(ArrayList<PlacedItemInfo> itemInfoList) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.itemfinder)
                .setContentTitle(getString(R.string.notification_title_items_nearby))
                .setContentText(getString(R.string.notification_text_items_nearby, itemInfoList.size()))
                .setAutoCancel(true);

        Intent contentIntent = new Intent(this, NearbyItemsActivity.class);
        contentIntent.putParcelableArrayListExtra(Intents.NEARBY_PLACE_ITEMS_INTENT_EXTRA, itemInfoList);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(NearbyItemsActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(contentIntent);
        PendingIntent contentPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // The id allows you to update the notification later on.
        notificationManager.notify(NOTIFICATION_ID_NEARBY_ITEMS, builder.build());
    }

    private void sendNotificationForSingleItem(PlacedItemInfo placedItemInfo) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_light_normal)
                .setContentTitle(getString(R.string.notification_title_items_nearby))
                .setContentText(placedItemInfo.getItem().getS() + " @" + placedItemInfo.getPlaceName())
                .setAutoCancel(true);

        Intent contentIntent = new Intent(this, ItemDetailActivity.class);
        contentIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_PLACED_INPUT, placedItemInfo);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(NearbyItemsActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(contentIntent);
        PendingIntent contentPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentPendingIntent);

        Intent viewItemIntent = new Intent(this, ItemDetailActivity.class);
        viewItemIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_PLACED_INPUT, placedItemInfo);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent viewItemPendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), viewItemIntent, 0);
        builder.addAction(R.drawable.ic_info_black_24dp, getString(R.string.button_view_item), viewItemPendingIntent);

        Intent mapIntent = new Intent(this, MapsActivity.class);
        mapIntent.putExtra(Intents.MAP_PLACED_ITEM_INPUT, placedItemInfo);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent mapPendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), mapIntent, 0);
        builder.addAction(R.drawable.ic_map_black_24dp, getString(R.string.button_show_on_map), mapPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // The id allows you to update the notification later on.
        notificationManager.notify(NOTIFICATION_ID_NEARBY_ITEMS, builder.build());
    }

    private void playSoundAndVibrate() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean playSound = shouldPlaySound(prefs, this);
            if (playSound) {
                mediaPlayer = buildMediaPlayer();
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                }
            }

            boolean vibrate = prefs.getBoolean(ItemfinderPreferencesActivity.KEY_VIBRATE_ON_NEARBY_ITEMS, false);
            if (vibrate) {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(VIBRATE_DURATION);
            }
        } finally {
            if (mediaPlayer != null) {
                mediaPlayerReleaseHandler.sendEmptyMessageDelayed(MESSAGE_MEDIA_PLAYER_RELEASE, MESSAGE_DELAY_MILLIS);
            }
        }
    }

    private MediaPlayer buildMediaPlayer() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.itemfinder);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            } finally {
                file.close();
            }
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(SOUND_VOLUME, SOUND_VOLUME);
            mediaPlayer.prepare();
            return mediaPlayer;
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            mediaPlayer.release();
            return null;
        }
    }

    private static boolean shouldPlaySound(SharedPreferences prefs, Context activity) {
        boolean shouldPlaySound = prefs.getBoolean(ItemfinderPreferencesActivity.KEY_PLAY_SOUND_ON_NEARBY_ITEMS, true);
        if (shouldPlaySound) {
            // See if sound settings overrides this
            AudioManager audioService = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                shouldPlaySound = false;
            }
        }
        return shouldPlaySound;
    }

    @Override
    public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
        Log.w(TAG, "Media playback error; error type: " + what + ", extra code: " + extra);
        if (mediaPlayer != null) {
            mediaPlayerReleaseHandler.sendEmptyMessageDelayed(MESSAGE_MEDIA_PLAYER_RELEASE, MESSAGE_DELAY_MILLIS);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayerReleaseHandler.sendEmptyMessageDelayed(MESSAGE_MEDIA_PLAYER_RELEASE, MESSAGE_DELAY_MILLIS);
        }
        if(mGeoQuery != null) {
            mGeoQuery.removeAllListeners();
        }
        super.onDestroy();
    }

    private NearbyItemsResult queryForNearbyFavoriteItems() {
        final NearbyItemsResult result = new NearbyItemsResult();

        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null && LocationUtils.locationSettingsEnabled(this)) {
            // Current location provider requires permission to access fine location, ask for permission first.
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                // Access to the location has been granted to the app.
                CurrentLocationProvider locationProvider = new CurrentLocationProvider(this, REQUEST_GOOGLE_PLAY_SERVICES, new LocationResultCallback() {
                    @Override
                    public void onLocationReceived(@NonNull final Location location) {
                        String userId = currentUser.getUid();
                        DatabaseReference databaseFavoriteItemsReference = getOrCreateDatabaseReference()
                                .child("user-favitems").child(userId);
                        databaseFavoriteItemsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot userFavoriteItemsSnapshot) {
                                if (userFavoriteItemsSnapshot.getChildrenCount() == 0) {
                                    finishTask(result, false);
                                } else {
                                    final Map<String, Item> favoriteItemMap = new HashMap<>();
                                    Iterable<DataSnapshot> favItemSnapshots = userFavoriteItemsSnapshot.getChildren();
                                    for (DataSnapshot favItemSnapshot : favItemSnapshots) {
                                        Item favoriteItem = favItemSnapshot.getValue(Item.class);
                                        favoriteItemMap.put(favItemSnapshot.getKey(), favoriteItem);
                                    }
                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NearbyItemsTaskService.this);
                                    float radius = Float.parseFloat(prefs.getString(ItemfinderPreferencesActivity.KEY_LOCATION_RADIUS,
                                            getString(R.string.preferences_default_nearby_fav_item_proximity))); // in kilometers

                                    mGeoQuery = getOrCreatePlacesGeoFire().queryAtLocation(
                                            new GeoLocation(location.getLatitude(), location.getLongitude()), radius);
                                    mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

                                        private AtomicInteger enteredKeyCountDuringInit = new AtomicInteger();

                                        private void countDownKeys() {
                                            if(geoQueryKeyCountDownLatch != null) {
                                                geoQueryKeyCountDownLatch.countDown();
                                            } else {
                                                enteredKeyCountDuringInit.decrementAndGet();
                                            }
                                        }

                                        @Override
                                        public void onKeyEntered(final String placeId, GeoLocation location) {
                                            // Increment number of keys entered
                                            enteredKeyCountDuringInit.incrementAndGet();
                                            getOrCreateDatabaseReference().child("places").child(placeId).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot placeSnapshot) {
                                                    final Place place = placeSnapshot.getValue(Place.class);
                                                    place.setId(placeId);
                                                    DataSnapshot placeItemsSnapshot = placeSnapshot.child("items");
                                                    final AtomicLong placeItemCount = new AtomicLong(placeItemsSnapshot.getChildrenCount());
                                                    if(placeItemCount.get() == 0) {
                                                        countDownKeys();
                                                    } else {
                                                        final Iterable<DataSnapshot> placeItems = placeItemsSnapshot.getChildren();
                                                        for (DataSnapshot placeItemSnapshot : placeItems) {
                                                            String itemId = placeItemSnapshot.getKey();
                                                            if (favoriteItemMap.get(itemId) != null) {
                                                                final PlaceItem placeItem = placeItemSnapshot.getValue(PlaceItem.class);
                                                                placeItem.setItemId(itemId);
                                                                getOrCreateDatabaseReference().child("users").child(placeItem.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {

                                                                    @Override
                                                                    public void onDataChange(DataSnapshot userSnapshot) {
                                                                        User user = userSnapshot.getValue(User.class);
                                                                        addNearbyItem(result, place, placeItem, user.getDN());
                                                                        if(placeItemCount.decrementAndGet() == 0) {
                                                                            countDownKeys();
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(DatabaseError databaseError) {
                                                                        Log.w(TAG, "placeItemUserValueEventListener:onCancelled", databaseError.toException());
                                                                        if(placeItemCount.decrementAndGet() == 0) {
                                                                            countDownKeys();
                                                                        }
                                                                    }
                                                                });
                                                            } else {
                                                                if(placeItemCount.decrementAndGet() == 0) {
                                                                    countDownKeys();
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                    Log.w(TAG, "placeItemsValueEventListener:onCancelled", databaseError.toException());
                                                    countDownKeys();
                                                }
                                            });
                                        }

                                        @Override
                                        public void onKeyExited(final String placeId) {
                                            enteredKeyCountDuringInit.decrementAndGet();
                                            for(Iterator<PlacedItemInfo> iterator = result.itemInfoList.iterator(); iterator.hasNext();) {
                                                PlacedItemInfo item = iterator.next();
                                                if(placeId.equals(item.getPlaceId())) {
                                                    iterator.remove();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onKeyMoved(final String placeId, final GeoLocation location) {
                                            // The key is a place ID, so this scenario should not occur. It is implemented for completeness.
                                            onKeyExited(placeId);
                                            onKeyEntered(placeId, location);
                                        }

                                        @Override
                                        public void onGeoQueryReady() {
                                            geoQueryKeyCountDownLatch = new CountDownLatch(enteredKeyCountDuringInit.get());
                                            finishTask(result, true);
                                        }

                                        @Override
                                        public void onGeoQueryError(DatabaseError error) {
                                            finishTask(result, false);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                finishTask(result, false);
                            }
                        });
                    }

                    @Override
                    public void onLocationNotAvailable(String failureMessage) {
                        finishTask(result, false);
                    }

                    @Override
                    public void onLocationSettingsNotAvailable() {
                        finishTask(result, false);
                    }
                });

                try {
                    locationProvider.start();

                    try {
                        synchronized (taskFinishMonitor) {
                            taskFinishMonitor.wait(TASK_TIMEOUT_SECONDS * 1000L);
                        }
                        if(geoQueryKeyCountDownLatch != null) {
                            geoQueryKeyCountDownLatch.await(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        result.taskResult = GcmNetworkManager.RESULT_FAILURE;
                    }
                } finally {
                    if(mGeoQuery != null) {
                        mGeoQuery.removeAllListeners();
                    }
                }
            }
        }

        return result;
    }

    private void addNearbyItem(NearbyItemsResult result, Place place, PlaceItem placedItem, String userName) {
        Item item = new Item();
        item.setId(placedItem.getItemId());
        item.setS(placedItem.getItemSummary());
        item.setDesc(placedItem.getItemDesc());

        PlacedItemInfo itemInfo = new PlacedItemInfo(item, place.getId(), place.getName(), placedItem.getUid(), userName);
        result.itemInfoList.add(itemInfo);
    }

    private void finishTask(NearbyItemsResult result, boolean success) {
        result.taskResult = success ? GcmNetworkManager.RESULT_SUCCESS : GcmNetworkManager.RESULT_FAILURE;
        synchronized (taskFinishMonitor) {
            taskFinishMonitor.notifyAll();
        }
    }

    private static class NearbyItemsResult {
        private int taskResult = GcmNetworkManager.RESULT_SUCCESS;
        private ArrayList<PlacedItemInfo> itemInfoList = new ArrayList<>();
    }

    // See https://groups.google.com/forum/#!topic/android-developers/Ciiu1C-_EmE
    private static class MediaPlayerReleaseHandler extends Handler {
        private WeakReference<NearbyItemsTaskService> taskServiceRef;

        public MediaPlayerReleaseHandler(WeakReference<NearbyItemsTaskService> taskServiceRef) {
            this.taskServiceRef = taskServiceRef;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_MEDIA_PLAYER_RELEASE) {
                if(taskServiceRef.get().mediaPlayer != null) {
                    taskServiceRef.get().mediaPlayer.release();
                    taskServiceRef.get().mediaPlayer = null;
                }
            }
        }
    }
}
