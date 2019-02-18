package com.manouti.itemfinder.item.detail;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;
import com.google.zxing.client.android.SoundManager;
import com.manouti.itemfinder.BaseActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.PlacedItemInfo;
import com.manouti.itemfinder.location.provider.AbstractLocationProvider;
import com.manouti.itemfinder.location.provider.CurrentLocationProvider;
import com.manouti.itemfinder.map.MapsActivity;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.util.LocationUtils;
import com.manouti.itemfinder.util.PermissionUtils;
import com.manouti.itemfinder.util.firebase.FirebaseImageLoader;
import com.manouti.itemfinder.util.firebase.FirebaseStorageUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemDetailActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = ItemDetailActivity.class.getSimpleName();

    private static final int REQUEST_LOCATION_PERMISSION_FOR_NEARBY_ITEM_CHECK = 124;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1313;
    private static final int REQUEST_SIGN_IN_TO_FAVOR_ITEM = 112;
    private static final int REQUEST_SIGN_IN_TO_RATE_ITEM = 113;

    private PlacedItemInfo mPlacedItemInfo;
    private Item mItem;

    private DatabaseReference mDatabaseReference;

    private GeoFire mPlacesGeoFire;
    private GeoQuery mGeoQuery;
    private CurrentLocationProvider mLocationProvider;

    private TextView mRatingTextView;
    private RatingBar mRatingBarIndicator;
    private TextView mRatingUserCountTextView;

    private ImageButton mFavoriteButton;
    private boolean mFavorite;

    private ImageButton mNearbyCheckButton;
    private ProgressBar mNearbyCheckProgressBar;
    private SoundManager mSoundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        mRatingTextView = (TextView) findViewById(R.id.item_rating_text_view);
        mRatingBarIndicator = (RatingBar) findViewById(R.id.item_rating_bar_indicator);
        mRatingUserCountTextView = (TextView) findViewById(R.id.item_rating_user_count);

        mFavoriteButton = (ImageButton) findViewById(R.id.button_set_favorite);
        mFavoriteButton.setOnClickListener(this);

        mNearbyCheckButton = (ImageButton) findViewById(R.id.button_check_nearby);
        mNearbyCheckButton.setOnClickListener(this);
        mNearbyCheckProgressBar = (ProgressBar) findViewById(R.id.nearby_check_progress_bar);

        findViewById(R.id.button_show_on_map).setOnClickListener(this);
    }

    private GeoFire getOrCreatePlacesGeoFire() {
        if(mPlacesGeoFire == null) {
            mPlacesGeoFire = new GeoFire(mDatabaseReference.child("places-geo"));
        }
        return mPlacesGeoFire;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_item_detail;
    }

    @Override
    protected void onStart() {
        super.onStart();

        final String itemId;
        mPlacedItemInfo = getIntent().getParcelableExtra(Intents.ITEM_DETAIL_ACTIVITY_PLACED_INPUT);
        if(mPlacedItemInfo != null) {
            itemId = mPlacedItemInfo.getItem().getId();
            loadPlaceDetails();
        } else {
            mItem = getIntent().getParcelableExtra(Intents.ITEM_DETAIL_ACTIVITY_ITEM_INPUT);
            if(mItem != null) {
                itemId = mItem.getId();
                loadItemDetails();
            } else {
                itemId = getIntent().getStringExtra(Intents.ITEM_DETAIL_ACTIVITY_ID_INPUT);
            }
        }

        if (itemId != null) {
            loadItemImage(itemId);

            if(mItem == null) {
                mDatabaseReference.child("items").child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            mItem = dataSnapshot.getValue(Item.class);
                            mItem.setId(itemId);
                            loadItemDetails();
                        } else {
                            Log.w(TAG, "Item with ID " + itemId + " does not exist");
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getItem:onCancelled", databaseError.toException());
                    }
                });
            }
        } else {
            Log.w(TAG, "Input item ID is unexpectedly null");
            finish();
            return;
        }

        if(getCurrentUser() != null) {
            DatabaseReference favItemReference = mDatabaseReference.child("user-favitems")
                    .child(getCurrentUser().getUid())
                    .child(itemId);
            favItemReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        mFavoriteButton.setImageResource(R.drawable.ic_star_black_36dp);
                        mFavorite = true;
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "getFavItem:onCancelled", databaseError.toException());
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mLocationProvider != null) {
            mLocationProvider.stop();
        }
        if(mGeoQuery != null) {
            mGeoQuery.removeAllListeners();
        }
        if(mSoundManager != null) {
            mSoundManager.close();
        }
    }

    private void loadPlaceDetails() {
        LinearLayout placeLayout = (LinearLayout) findViewById(R.id.place_layout);
        placeLayout.setVisibility(View.VISIBLE);
        TextView placeNameTextView = (TextView) findViewById(R.id.place_name_text_view);
        placeNameTextView.setText(mPlacedItemInfo.getPlaceName());

        final TextView userNameTextView = (TextView) findViewById(R.id.user_text_view);
        if(StringUtils.isNotBlank(mPlacedItemInfo.getUserDisplayName())) {
            userNameTextView.setText(mPlacedItemInfo.getUserDisplayName());
        } else if(mPlacedItemInfo.getUserId() != null) {
            mDatabaseReference.child("users").child(mPlacedItemInfo.getUserId()).child("dn").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String userDisplayName = dataSnapshot.getValue(String.class);
                    userNameTextView.setText(userDisplayName);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "getUserDisplayName:onCancelled", databaseError.toException());
                }
            });
        }
    }

    private void loadItemDetails() {
        findViewById(R.id.item_detail_progress_bar).setVisibility(View.GONE);
        findViewById(R.id.item_detail_layout).setVisibility(View.VISIBLE);

        setTitle(mItem.getS());

        TextView itemSummaryTextView = (TextView) findViewById(R.id.item_summary_text_view);
        itemSummaryTextView.setText(mItem.getS());

        TextView itemDescriptionTextView = (TextView) findViewById(R.id.item_description_text_view);
        itemDescriptionTextView.setText(mItem.getDesc());

        loadUserRating();

        mRatingTextView.setText(String.format("%.1f", mItem.getRating()));
        mRatingBarIndicator.setRating((float) mItem.getRating());
        mRatingUserCountTextView.setText("(" + mItem.getVoteCount() + ")");
    }

    private void loadUserRating() {
        final RatingBar editableRatingBar = (RatingBar) findViewById(R.id.item_rating_bar);
        if(getCurrentUser() != null) {
            DatabaseReference userRatingRef = mDatabaseReference.child("item-ratings")
                    .child(mItem.getId())
                    .child("users")
                    .child(getCurrentUser().getUid());
            syncDatabaseReference(userRatingRef);
            userRatingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Double userRating = dataSnapshot.getValue(Double.class);
                        if (userRating != null) {
                            editableRatingBar.setRating(userRating.floatValue());
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "getUserRatingOfItem:onCancelled", databaseError.toException());
                }
            });
        } else {
            editableRatingBar.setRating(0f);
        }

        // Intercept touch event to sign in if needed
        editableRatingBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && getCurrentUser() == null) {
                    logIn(REQUEST_SIGN_IN_TO_RATE_ITEM);
                    return true;
                }
                return false;
            }
        });

        // Handle rating bar change by the user
        editableRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (fromUser) {
                    rateItem((int) Math.ceil(rating));
                }
            }
        });
    }

    private void rateItem(final int userRating) {
        if(getCurrentUser() != null) {
            DatabaseReference itemRatingRef = mDatabaseReference.child("item-ratings")
                    .child(mItem.getId());

            itemRatingRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    if(mutableData.getValue() == null) {
                        return Transaction.success(mutableData);
                    }

                    MutableData userRatingsData = mutableData.child("users");
                    if (userRatingsData.hasChild(getCurrentUser().getUid())) {
                        int oldRating = userRatingsData.child(getCurrentUser().getUid()).getValue(Integer.class);
                        decrementRatingCount(oldRating, mutableData);
                        incrementRatingCount(userRating, mutableData);
                        updateAverageRating(mutableData, false);
                    } else {
                        incrementRatingCount(userRating, mutableData);
                        updateAverageRating(mutableData, true);
                    }

                    // Set user rating and report transaction success
                    userRatingsData.child(getCurrentUser().getUid()).setValue(userRating);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed,
                                       DataSnapshot currentData) {
                    // Transaction completed
                    Log.d(TAG, "ratingTransaction:onComplete: committed " + committed + ", error: " + error);
                    if(error != null) {
                        FirebaseCrash.report(error.toException());
                    } else if(committed && currentData != null) {
                        double averageRating = currentData.child("rating").getValue(Double.class);
                        int voteCount = currentData.child("voteCount").getValue(Integer.class);
                        mItem.setRating(averageRating);
                        mItem.setVoteCount(voteCount);

                        mRatingTextView.setText(String.format("%.1f", averageRating));
                        mRatingBarIndicator.setRating((float) averageRating);
                        mRatingUserCountTextView.setText("(" + voteCount + ")");

                        // Update average rating and rating count in item node
                        Map<String, Object> updateMap = new HashMap<>();
                        updateMap.put("/items/" + mItem.getId() + "/rating", averageRating);
                        updateMap.put("/items/" + mItem.getId() + "/voteCount", voteCount);
                        mDatabaseReference.updateChildren(updateMap);
                    }
                }

                private void decrementRatingCount(int rating, MutableData mutableData) {
                    MutableData ratingCountRef = mutableData.child(Integer.toString(rating));
                    int oldRatingCount = ratingCountRef.getValue(Integer.class);
                    ratingCountRef.setValue(oldRatingCount - 1);
                }

                private void incrementRatingCount(int rating, MutableData mutableData) {
                    MutableData ratingCountRef = mutableData.child(Integer.toString(rating));
                    int newRatingCount = ratingCountRef.getValue(Integer.class);
                    ratingCountRef.setValue(newRatingCount + 1);
                }

                private void updateAverageRating(MutableData mutableData, boolean updateTotalRatingCount) {
                    int weightedRateSum = 0;
                    int totalRatingCount = 0;
                    for(int rating = 0; rating < 5; rating++) {
                        int ratingCount = mutableData.child(Integer.toString(rating)).getValue(Integer.class);
                        weightedRateSum += rating * ratingCount;
                        totalRatingCount += ratingCount;
                    }
                    double averageRating = weightedRateSum / totalRatingCount;
                    mutableData.child("rating").setValue(averageRating);

                    if(updateTotalRatingCount) {
                        mutableData.child("voteCount").setValue(totalRatingCount);
                    }
                }

            });
        } else {
            logIn(REQUEST_SIGN_IN_TO_RATE_ITEM);
        }
    }

    private void loadItemImage(String itemId) {
        StorageReference photoStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseStorageUtil.STORAGE_URL)
                .child(FirebaseStorageUtil.IMAGES_PATH)
                .child(FirebaseStorageUtil.ITEMS_PATH)
                .child(itemId);

        ImageView itemImageView = (ImageView) findViewById(R.id.item_image_view);
        Glide.with(this)
                .using(new FirebaseImageLoader())
                .load(photoStorageReference)
                .fitCenter()
                .into(itemImageView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_set_favorite:
                toggleFavoriteItem();
                break;
            case R.id.button_check_nearby:
                checkIfNearby();
                break;
            case R.id.button_show_on_map:
                showOnMap();
                break;
            default:
                // do nothing
        }
    }

    private void toggleFavoriteItem() {
        if (getCurrentUser() != null) {
            mFavorite = !mFavorite;
            mFavoriteButton.setImageResource(mFavorite ? R.drawable.ic_star_black_36dp
                    : R.drawable.ic_star_border_black_36dp);
            mDatabaseReference.child("user-favitems")
                    .child(getCurrentUser().getUid())
                    .child(mItem.getId())
                    .setValue(mFavorite ? mItem.toMap() : null)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            FirebaseCrash.report(e);
                            showSnackbar(R.string.generic_error_message);
                        }
                    });
        } else {
            logIn(REQUEST_SIGN_IN_TO_FAVOR_ITEM);
        }
    }

    private void checkIfNearby() {
        if (LocationUtils.locationSettingsEnabled(this)) {
            // Current location provider requires permission to access fine location, ask for permission first.
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission to access the location is missing.
                PermissionUtils.requestPermission(this, REQUEST_LOCATION_PERMISSION_FOR_NEARBY_ITEM_CHECK,
                        android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            } else {
                doCheckIfNearby();
            }
        } else {
            showSnackbar(R.string.location_settings_disabled_nearby_item_check);
        }
    }

    @SuppressWarnings({"ResourceType"})
    private void doCheckIfNearby() {
        showNearbyCheckProgressBar();

        final AtomicBoolean foundNearby = new AtomicBoolean();
        mLocationProvider = new CurrentLocationProvider(this, REQUEST_GOOGLE_PLAY_SERVICES, new AbstractLocationProvider.LocationResultCallback() {

            @Override
            public void onLocationReceived(@NonNull final Location location) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ItemDetailActivity.this);
                float radius = Float.parseFloat(prefs.getString(ItemfinderPreferencesActivity.KEY_LOCATION_RADIUS,
                        getString(R.string.preferences_default_nearby_fav_item_proximity))); // in kilometers

                mGeoQuery = getOrCreatePlacesGeoFire().queryAtLocation(
                        new GeoLocation(location.getLatitude(), location.getLongitude()), radius);
                mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

                    private AtomicInteger enteredKeyCountDuringInit = new AtomicInteger();

                    private void countDownKeys() {
                        if(enteredKeyCountDuringInit.decrementAndGet() == 0) {
                            geoQueryFinished();
                        }
                    }

                    @Override
                    public void onKeyEntered(final String placeId, GeoLocation location) {
                        // Increment number of keys entered
                        enteredKeyCountDuringInit.incrementAndGet();
                        mDatabaseReference.child("places").child(placeId).child("items").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot placeItemsSnapshot) {
                                final Iterable<DataSnapshot> placeItems = placeItemsSnapshot.getChildren();
                                for (DataSnapshot placeItemSnapshot : placeItems) {
                                    String itemId = placeItemSnapshot.getKey();
                                    if (StringUtils.equals(itemId, mItem.getId())) {
                                        mGeoQuery.removeAllListeners();
                                        foundNearby.set(true);
                                        playSoundAndVibrate();
                                        break;
                                    }
                                }
                                countDownKeys();
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
                    }

                    @Override
                    public void onKeyMoved(final String placeId, final GeoLocation location) {
                    }

                    @Override
                    public void onGeoQueryReady() {
                        mGeoQuery.removeAllListeners();
                    }

                    @Override
                    public void onGeoQueryError(DatabaseError error) {
                        showSnackbar(R.string.error_geo_query_nearby_item);
                    }

                    private void geoQueryFinished() {
                        hideNearbyCheckProgressBar();
                        showSnackbar(foundNearby.get() ? R.string.item_found_nearby : R.string.item_not_found_nearby);
                    }
                });
            }

            @Override
            public void onLocationNotAvailable(String failureMessage) {
                showSnackbar(failureMessage);
                hideNearbyCheckProgressBar();
            }

            @Override
            public void onLocationSettingsNotAvailable() {
                showSnackbar(R.string.location_settings_disabled_nearby_item_check);
                hideNearbyCheckProgressBar();
            }
        });
        mLocationProvider.start();
    }

    private void showNearbyCheckProgressBar() {
        mNearbyCheckProgressBar.setVisibility(View.VISIBLE);
        mNearbyCheckButton.setVisibility(View.GONE);
    }

    private void hideNearbyCheckProgressBar() {
        mNearbyCheckProgressBar.setVisibility(View.GONE);
        mNearbyCheckButton.setVisibility(View.VISIBLE);
    }

    private void playSoundAndVibrate() {
        getOrCreateSoundManager().playSoundAndVibrate();
    }

    public SoundManager getOrCreateSoundManager() {
        if(mSoundManager == null) {
            mSoundManager = new SoundManager(this, true, true);
        }
        return mSoundManager;
    }

    private void showOnMap() {
        Intent mapIntent = new Intent(this, MapsActivity.class);
        if(mPlacedItemInfo != null) {
            mapIntent.putExtra(Intents.MAP_PLACED_ITEM_INPUT, mPlacedItemInfo);
        } else {
            mapIntent.putExtra(Intents.MAP_ITEM_INPUT, mItem);
        }
        startActivity(mapIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode != REQUEST_LOCATION_PERMISSION_FOR_NEARBY_ITEM_CHECK) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            checkIfNearby();
        } else {
            PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getSupportFragmentManager(), "dialog");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_SIGN_IN_TO_FAVOR_ITEM || requestCode == REQUEST_SIGN_IN_TO_RATE_ITEM) {
            if (resultCode != RESULT_OK) {
                showSnackbar(R.string.unknown_sign_in_response);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
