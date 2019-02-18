package com.manouti.itemfinder.map;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.LocationCallback;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.model.User;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import com.google.zxing.client.android.clipboard.ClipboardInterface;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.PlacedItemInfo;
import com.manouti.itemfinder.location.provider.AbstractLocationProvider.LocationResultCallback;
import com.manouti.itemfinder.location.provider.CurrentLocationProvider;
import com.manouti.itemfinder.location.provider.UserLocationProvider;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.util.LocationUtils;
import com.manouti.itemfinder.util.NetworkUtils;
import com.manouti.itemfinder.util.PermissionUtils;
import com.manouti.itemfinder.util.broadcast.ConnectivityBroadcastReceiver;
import com.manouti.itemfinder.util.broadcast.LocationProviderBroadcastReceiver;
import com.manouti.itemfinder.util.firebase.FirebaseImageLoader;
import com.manouti.itemfinder.util.firebase.FirebaseStorageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
                                                              GoogleMap.OnCameraChangeListener,
                                                              GoogleMap.OnMarkerClickListener,
                                                              ActivityCompat.OnRequestPermissionsResultCallback,
                                                              LocationProviderBroadcastReceiver.LocationBroadcastHandler,
                                                              ConnectivityBroadcastReceiver.ConnectivityBroadcastHandler {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private static final int REQUEST_MY_LOCATION_PERMISSION = 123;
    private static final int REQUEST_LOCATION_PROVIDER_PERMISSION = 124;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1235;

    private static final int REQUEST_SIGN_IN_TO_FAVOR_ITEM = 125;
    private static final int REQUEST_SIGN_IN_TO_RATE_ITEM = 126;

    private static final float DEFAULT_ZOOM_LEVEL = 15f;
    private static final float MINIMUM_ZOOM_TO_REFRESH = 14f;
    private static final float MINIMUM_DISTANCE_TO_REFRESH = 0.01f;

    protected GoogleMap mMap;
    protected Map<String, Marker> markers;
    protected DatabaseReference mDatabaseReference;
    private Map<String, DatabaseReference> mSyncedDatabaseRefs = new HashMap<>();

    protected String mItemId;
    private Item mItem;

    private CurrentLocationProvider mCurrentLocationProvider;
    private UserLocationProvider mUserLocationProvider;

    private View mRootView;

    private GoogleApiClient mGoogleApiClient;
    private GeoFire mPlacesGeoFire;
    private GeoQuery mGeoQuery;
    private MapGeoQueryEventListener mGeoQueryEventListener;
    private float mQueryLocationRadius;

    private CameraPosition lastCameraPosition;

    private PlacedItemInfo mPlacedItemInfo;
    private TextView mPlaceNameTextView;
    private TextView mUserNameTextView;

    private ImageButton mFavoriteButton;
    private boolean mFavorite;

    private TextView mRatingTextView;
    private RatingBar mRatingBarIndicator;
    private TextView mRatingUserCountTextView;

    private LocationProviderBroadcastReceiver mLocationProviderBroadcastReceiver;
    private ConnectivityBroadcastReceiver mConnectivityBroadcastReceiver;
    private Boolean mConnectivity;

    public GoogleMap getMap() {
        return mMap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mRootView = findViewById(android.R.id.content);

        Intent intent = getIntent();
        mPlacedItemInfo = intent.getParcelableExtra(Intents.MAP_PLACED_ITEM_INPUT);
        if(mPlacedItemInfo != null) {
            mItemId = mPlacedItemInfo.getItem().getId();
        } else {
            mItem = intent.getParcelableExtra(Intents.MAP_ITEM_INPUT);
            if(mItem == null) {
                Log.w(TAG, "Input item is unexpectedly null");
                finish();
                return;
            } else {
                mItemId = mItem.getId();
            }
        }

        if(mItemId == null) {
            Log.w(TAG, "Input item ID is unexpectedly null");
            finish();
            return;
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mPlacesGeoFire = new GeoFire(mDatabaseReference.child("places-geo"));
        mGeoQueryEventListener = new MapGeoQueryEventListener(this);

        this.markers = new HashMap<>();

        mPlaceNameTextView = (TextView) findViewById(R.id.place_name_text_view);
        mUserNameTextView = (TextView) findViewById(R.id.user_text_view);

        mRatingTextView = (TextView) findViewById(R.id.item_rating_text_view);
        mRatingBarIndicator = (RatingBar) findViewById(R.id.item_rating_bar_indicator);
        mRatingUserCountTextView = (TextView) findViewById(R.id.item_rating_user_count);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(TAG, "onMapReady");
        mMap = googleMap;
        enableMyLocation();

        if (mPlacedItemInfo != null) {
            showPlacedItem();
        } else {
            showItemPlacesBasedOnCurrentLocation();
            createAndRegisterReceivers();
        }

        mMap.setOnCameraChangeListener(this);
        mMap.setOnMarkerClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        displayResultItem();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mQueryLocationRadius = Float.parseFloat(prefs.getString(ItemfinderPreferencesActivity.KEY_LOCATION_RADIUS, getString(R.string.preferences_default_nearby_fav_item_proximity))); // in kilometers
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // Broadcast receivers are removed in onStop. Re-add them in onRestart.
        // At this phase, we are sure the map is ready in case any broadcast triggers an interaction with the map.
        // No need to call this in normal onStart() because it is called in onMapReady.
        if (mPlacedItemInfo == null) {
            createAndRegisterReceivers();
        }
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, REQUEST_MY_LOCATION_PERMISSION,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    private void showPlacedItem() {
        final String presetPlaceId = mPlacedItemInfo.getPlaceId();
        mPlacesGeoFire.getLocation(presetPlaceId, new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation geoLocation) {
                showItemPlaces(geoLocation.latitude, geoLocation.longitude);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "GeoFire#getLocation:onCancelled", databaseError.toException());
                FirebaseCrash.log("GeoFire#getLocation:onCancelled: " + databaseError.getMessage() +
                        " - details: " + databaseError.getDetails());
                if (mGoogleApiClient == null) {
                    mGoogleApiClient = new GoogleApiClient.Builder(MapsActivity.this)
                            .addApi(Places.GEO_DATA_API)
                            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    Places.GeoDataApi.getPlaceById(mGoogleApiClient, presetPlaceId).setResultCallback(new ResultCallback<PlaceBuffer>() {
                                        @Override
                                        public void onResult(@NonNull PlaceBuffer placeBuffer) {
                                            if (placeBuffer.getStatus().isSuccess() && placeBuffer.getCount() > 0) {
                                                Place place = placeBuffer.get(0);
                                                Log.i(TAG, "Place found: " + place.getName());
                                                LatLng latLng = place.getLatLng();
                                                showItemPlaces(latLng.latitude, latLng.longitude);
                                            } else {
                                                Log.e(TAG, "Place not found");
                                            }
                                            placeBuffer.release();
                                        }
                                    });
                                }

                                @Override
                                public void onConnectionSuspended(int cause) {
                                    FirebaseCrash.log("CurrentLocationProvider:GoogleApiClient.ConnectionCallbacks:onConnectionSuspended: - cause: " + cause);
                                    String errorCause = null;
                                    if (cause == CAUSE_NETWORK_LOST) {
                                        errorCause = "A peer device connection was lost";
                                    } else if (cause == CAUSE_SERVICE_DISCONNECTED) {
                                        errorCause = "Service has been killed";
                                    }
                                    showSnackbar(MapsActivity.this.getResources().getString(R.string.location_connection_suspended)
                                            + (errorCause != null ? ":" + System.getProperty("line.separator") + errorCause : ""));

                                }
                            })
                            .build();
                }
                if (!mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        });
    }

    @Override
    public void onCameraChange(final CameraPosition position) {
        Log.d(TAG, "onCameraChange: " + position.toString());
        boolean zoomedIn = position.zoom > MINIMUM_ZOOM_TO_REFRESH;
        if(zoomedIn && cameraChangeThresholdExceeded(position)) {
            queryAndShowPlaces(position.target.latitude, position.target.longitude);
        }
        if(zoomedIn) {
            lastCameraPosition = position;
        }
    }

    private boolean cameraChangeThresholdExceeded(@NonNull final CameraPosition position) {
        if(lastCameraPosition == null) {
            return true;
        }
        LatLng target = position.target;
        LatLng lastTarget = lastCameraPosition.target;
        return Math.abs(target.latitude - lastTarget.latitude) > MINIMUM_DISTANCE_TO_REFRESH
                || Math.abs(target.longitude - lastTarget.longitude) > MINIMUM_DISTANCE_TO_REFRESH;
    }

    private void showItemPlacesBasedOnCurrentLocation() {
        Log.i(TAG, "showItemPlacesBasedOnCurrentLocation");
        disableUserLocationProvider();

        // Current location provider requires permission to access fine location, ask for permission first.
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, REQUEST_LOCATION_PROVIDER_PERMISSION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else {
            // Access to the location has been granted to the app.
            if(mCurrentLocationProvider == null) {
                mCurrentLocationProvider = createCurrentLocationProvider();
                mCurrentLocationProvider.start();
            } else {
                mCurrentLocationProvider.restart();
            }
        }
    }

    private void disableCurrentLocationProvider() {
        if(mCurrentLocationProvider != null) {
            mCurrentLocationProvider.stop();
        }
    }

    private void disableUserLocationProvider() {
        if(mUserLocationProvider != null) {
            mUserLocationProvider.stop();
        }
    }

    private void displayResultItem() {
        displayItemInfo();
        if(mPlacedItemInfo != null) {
            displayPlacedItemInfo(mPlacedItemInfo);
        }
    }

    private void displayPlacedItemInfo(PlacedItemInfo placedItemInfo) {
        mPlaceNameTextView.setText(placedItemInfo.getPlaceName());
        mUserNameTextView.setText(placedItemInfo.getUserDisplayName());
    }

    private void displayItemInfo() {
        boolean copyToClipboard = getIntent().getBooleanExtra(Intents.COPY_TO_CLIPBOARD, true);
        if (copyToClipboard) {
            ClipboardInterface.setText(mItemId, this);
        }

        StorageReference photoStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseStorageUtil.STORAGE_URL)
                .child(FirebaseStorageUtil.IMAGES_PATH)
                .child(FirebaseStorageUtil.ITEMS_PATH)
                .child(mItemId);

        final ImageView smallItemImageView = (ImageView) findViewById(R.id.item_small_image_view);
        Glide.with(this)
                .using(new FirebaseImageLoader())
                .load(photoStorageReference)
                .listener(new RequestListener<StorageReference, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, StorageReference model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, StorageReference model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        smallItemImageView.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .fitCenter()
                .into(smallItemImageView);

        ImageView itemImageView = (ImageView) findViewById(R.id.item_image_view);
        Glide.with(this)
                .using(new FirebaseImageLoader())
                .load(photoStorageReference)
                .fitCenter()
                .into(itemImageView);

        mDatabaseReference.child("items").child(mItemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mItem = dataSnapshot.getValue(Item.class);

                    TextView itemSummaryTextView = (TextView) findViewById(R.id.item_summary_text_view);
                    itemSummaryTextView.setText(mItem.getS());

                    mRatingTextView.setText(String.format("%.1f", mItem.getRating()));
                    mRatingBarIndicator.setRating((float) mItem.getRating());
                    mRatingUserCountTextView.setText("(" + mItem.getVoteCount() + ")");

                    TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
                    typeTextView.setText(mItem.getType());

                    TextView itemDescriptionTextView = (TextView) findViewById(R.id.item_description_text_view);
                    itemDescriptionTextView.setText(mItem.getDesc());

                    loadUserRatingBar();

                    mFavoriteButton = (ImageButton) findViewById(R.id.button_set_favorite);
                    mFavoriteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            toggleFavoriteItem();
                        }
                    });
                } else {
                    Log.w(TAG, "Item with ID " + mItemId + " does not exist");
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getItem:onCancelled", databaseError.toException());
            }
        });

        if(getCurrentUser() != null) {
            DatabaseReference favItemReference = mDatabaseReference.child("user-favitems")
                    .child(getCurrentUser().getUid())
                    .child(mItemId);
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

    private void loadUserRatingBar() {
        final RatingBar editableRatingBar = (RatingBar) findViewById(R.id.item_rating_bar);
        final FirebaseUser currentUser = getCurrentUser();
        if(currentUser != null) {
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
                if (event.getAction() == MotionEvent.ACTION_UP && currentUser == null) {
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
        final FirebaseUser currentUser = getCurrentUser();
        if(currentUser != null) {
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
                        int oldUserRating = userRatingsData.child(getCurrentUser().getUid()).getValue(Integer.class);
                        decrementRatingCount(oldUserRating, mutableData);
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

    private CurrentLocationProvider createCurrentLocationProvider() {
        return new CurrentLocationProvider(this, REQUEST_GOOGLE_PLAY_SERVICES, false, new LocationResultCallback() {
            @Override
            public void onLocationReceived(@NonNull Location location) {
                Log.i(TAG, "onLocationReceived: " + location.getLatitude() + ", " + location.getLongitude());
                showItemPlaces(location);
            }

            @Override
            public void onLocationNotAvailable(String failureMessage) {
            }

            @Override
            public void onLocationSettingsNotAvailable() {
                showItemPlacesBasedOnOfflineLocation();
            }
        });
    }

    private void showItemPlacesBasedOnOfflineLocation() {
        Log.i(TAG, "showItemPlacesBasedOnOfflineLocation");
        disableCurrentLocationProvider();

        // If current location is not available, use offline registered location
        if(mUserLocationProvider == null) {
            mUserLocationProvider = new UserLocationProvider(this, new LocationResultCallback() {
                @Override
                public void onLocationReceived(@NonNull Location location) {
                    showSnackbar(R.string.location_settings_unavailable_using_offline_locations);
                    showItemPlaces(location);
                }

                @Override
                public void onLocationNotAvailable(String failureMessage) {
                    showSnackbar(failureMessage);
                }

                @Override
                public void onLocationSettingsNotAvailable() {
                    // this is not called by the user location provider, so do nothing
                }
            });
            mUserLocationProvider.start();
        } else {
            mUserLocationProvider.restart();
        }
    }

    @Override
    protected void onStop() {
        if(mCurrentLocationProvider != null) {
            mCurrentLocationProvider.stop();
        }
        if(mUserLocationProvider != null) {
            mUserLocationProvider.stop();
        }

        // remove all event listeners to stop updating in the background
        if(mGeoQuery != null) {
            mGeoQuery.removeAllListeners();
            mGeoQuery = null;
        }

        mGeoQueryEventListener.cleanUp();

        if(markers != null) {
            for (Marker marker : markers.values()) {
                marker.remove();
            }
            markers.clear();
            markers = null;
        }

        if(mLocationProviderBroadcastReceiver != null) {
            unregisterReceiver(mLocationProviderBroadcastReceiver);
            mLocationProviderBroadcastReceiver = null;
        }

        if(mConnectivityBroadcastReceiver != null) {
            unregisterReceiver(mConnectivityBroadcastReceiver);
            mConnectivityBroadcastReceiver = null;
        }

        for(Map.Entry<String, DatabaseReference> entry : mSyncedDatabaseRefs.entrySet()) {
            entry.getValue().keepSynced(false);
        }
        super.onStop();
    }

    /**
     * Will be invoked when the location connection callback requests a permission and the request is answered.
     */
    @Override
    @SuppressWarnings({"ResourceType"})
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_MY_LOCATION_PERMISSION
                && requestCode != REQUEST_LOCATION_PROVIDER_PERMISSION) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            if(requestCode == REQUEST_MY_LOCATION_PERMISSION) {
                // Enable the my location layer if the permission has been granted.
                enableMyLocation();
            } else {
                // Start current location provider if the permission has been granted.
                if (mCurrentLocationProvider != null) {
                    mCurrentLocationProvider.restart();
                } else {
                    mCurrentLocationProvider = createCurrentLocationProvider();
                    mCurrentLocationProvider.start();
                }
            }
        } else {
            PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getSupportFragmentManager(), "dialog");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                // The user has resolved the error that occurred connecting to Google Play Services
                if (resultCode == Activity.RESULT_OK) {
                    mCurrentLocationProvider.restart();
                }
                break;
            case CurrentLocationProvider.LOCATION_REQUEST_CHECK_SETTINGS:
                // The user has enabled appropriate location settings needed for Google location requests
                if (resultCode == Activity.RESULT_OK) {
                    mCurrentLocationProvider.restart();
                } else {
                    showItemPlacesBasedOnOfflineLocation();
                }
                break;
            case REQUEST_SIGN_IN_TO_FAVOR_ITEM:
            case REQUEST_SIGN_IN_TO_RATE_ITEM:
                if (resultCode != RESULT_OK) {
                    showSnackbar(R.string.unknown_sign_in_response);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showItemPlaces(Location cameraLocation) {
        showItemPlaces(cameraLocation.getLatitude(), cameraLocation.getLongitude());
    }

    private void showItemPlaces(double latitude, double longitude) {
        LatLng center = new LatLng(latitude, longitude);
        changeCamera(CameraUpdateFactory.newLatLngZoom(center, DEFAULT_ZOOM_LEVEL));
    }

    private void queryAndShowPlaces(double latitude, double longitude) {
        if(mGeoQuery == null) {
            mGeoQuery = mPlacesGeoFire.queryAtLocation(
                    new GeoLocation(latitude, longitude), mQueryLocationRadius);
            mGeoQuery.addGeoQueryEventListener(mGeoQueryEventListener);
        } else {
            // Update center location of GeoQuery if the query already exists
            mGeoQuery.setCenter(new GeoLocation(latitude, longitude));
        }
    }

    private void changeCamera(CameraUpdate update) {
        changeCamera(update, 2000);
    }

    private void changeCamera(CameraUpdate update, int durationMs) {
        changeCamera(update, durationMs, null);
    }

    /**
     * Change the camera position by moving or animating the camera depending on the state of the
     * animate toggle button.
     */
    private void changeCamera(CameraUpdate update, int durationMs, GoogleMap.CancelableCallback callback) {
        mMap.animateCamera(update, durationMs, callback);
    }

    private void createAndRegisterReceivers() {
        mLocationProviderBroadcastReceiver = new LocationProviderBroadcastReceiver(this);
        registerReceiver(mLocationProviderBroadcastReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        mConnectivity = NetworkUtils.isNetworkAvailable(this) ? Boolean.TRUE : Boolean.FALSE;
        mConnectivityBroadcastReceiver = new ConnectivityBroadcastReceiver(this);
        registerReceiver(mConnectivityBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void handleLocationBroadcast(Context context, Intent intent) {
        if(LocationUtils.isIntentActionLocationEnabled(context, intent)) {
            showItemPlacesBasedOnCurrentLocation();
        }
    }

    @Override
    public void handleConnectivityBroadcast(Context context, Intent intent) {
        // Query offline user-registered location if connection is re-established and location settings are disabled
        if(NetworkUtils.isIntentActionConnectivityEstablished(context, intent) && !mConnectivity) {
            mConnectivity = true;
            if(!LocationUtils.locationSettingsEnabled(context)) {
                showSnackbar(R.string.location_settings_unavailable_using_offline_locations);
                showItemPlacesBasedOnOfflineLocation();
            }
        } else if(!NetworkUtils.isNetworkAvailable(context) && mConnectivity) {
            mConnectivity = false;
            if(!LocationUtils.locationSettingsEnabled(context)) {
                showSnackbar(R.string.no_internet_connection);
            }
        }
    }

    private void toggleFavoriteItem() {
        if (getCurrentUser() != null) {
            mFavorite = !mFavorite;
            mFavoriteButton.setImageResource(mFavorite ? R.drawable.ic_star_black_36dp
                    : R.drawable.ic_star_border_black_36dp);
            mDatabaseReference.child("user-favitems")
                    .child(getCurrentUser().getUid())
                    .child(mItemId)
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

    // TODO some of the below methods should be moved to a shared place
    protected void showSnackbar(@StringRes int errorMessageRes) {
        showSnackbar(getResources().getString(errorMessageRes));
    }

    protected void showSnackbar(String errorMessage) {
        Snackbar.make(mRootView, Html.fromHtml("<font color=\"#ffffff\">" + errorMessage + "</font>"), Snackbar.LENGTH_LONG).show();
    }

    protected void syncDatabaseReference(DatabaseReference databaseReference) {
        databaseReference.keepSynced(true);
        mSyncedDatabaseRefs.put(databaseReference.toString(), databaseReference);
    }

    protected FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    protected void logIn(int requestCode) {
        ArrayList<String> selectedProviders = new ArrayList<>();
        selectedProviders.add(AuthUI.EMAIL_PROVIDER);
        selectedProviders.add(AuthUI.GOOGLE_PROVIDER);

        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setProviders(selectedProviders.toArray(new String[selectedProviders.size()]))
                        .setTheme(R.style.AppTheme_NoActionBar)
                        .build(),
                requestCode);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        final String placeId = marker.getTitle();
        mDatabaseReference.child("places").child(placeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot placeSnapshot) {
                com.manouti.itemfinder.model.place.Place place = placeSnapshot.getValue(com.manouti.itemfinder.model.place.Place.class);
                place.setId(placeId);
                mPlaceNameTextView.setText(place.getName());
                String userId = placeSnapshot.child("items").child(mItemId).child("uid").getValue(String.class);
                mDatabaseReference.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot userSnapshot) {
                        User user = userSnapshot.getValue(User.class);
                        mUserNameTextView.setText(user.getDN());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "placeItemUserValueEventListener:onCancelled", databaseError.toException());
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "placeValueEventListener:onCancelled", databaseError.toException());
            }
        });
        changeCamera(CameraUpdateFactory.newLatLng(marker.getPosition()), 300);
        // Consume the event to avoid showing the title which contains the place ID.
        return true;
    }
}
