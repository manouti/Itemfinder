package com.manouti.itemfinder.home.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.adapter.GeoRankedAdapterEventListener;
import com.manouti.itemfinder.item.adapter.GeoRankedItemsRecyclerViewAdapter;
import com.manouti.itemfinder.item.score.ScoreRankedItem;
import com.manouti.itemfinder.item.viewholder.GeoFeaturedItemViewHolder;
import com.manouti.itemfinder.location.provider.AbstractLocationProvider;
import com.manouti.itemfinder.location.provider.CurrentLocationProvider;
import com.manouti.itemfinder.location.provider.UserLocationProvider;
import com.manouti.itemfinder.user.locations.AddUserLocationActivity;
import com.manouti.itemfinder.util.LocationUtils;
import com.manouti.itemfinder.util.NetworkUtils;
import com.manouti.itemfinder.util.PermissionUtils;
import com.manouti.itemfinder.util.broadcast.ConnectivityBroadcastReceiver;
import com.manouti.itemfinder.util.broadcast.LocationProviderBroadcastReceiver;
import com.manouti.itemfinder.util.firebase.FirebaseImageLoader;
import com.manouti.itemfinder.util.firebase.FirebaseStorageUtil;
import com.manouti.itemfinder.util.ui.SwipeUpOnlyRefreshLayout;

public class NearbyItemsFragment extends BaseHomeFragment implements GeoRankedAdapterEventListener<GeoFeaturedItemViewHolder>,
                                                             SwipeRefreshLayout.OnRefreshListener,
                                                             LocationProviderBroadcastReceiver.LocationBroadcastHandler,
                                                             ConnectivityBroadcastReceiver.ConnectivityBroadcastHandler {

    private static final String TAG = NearbyItemsFragment.class.getSimpleName();

    private static final int REQUEST_LOCATION_PERMISSION_FOR_LOCATION_PROVIDER = 123;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES_FOR_LOCATION = 1235;

    private RecyclerView mNearbyItemsRecyclerView;
    private ProgressBar mProgressBar;
    private SwipeUpOnlyRefreshLayout mSwipeRefreshLayout;
    private ViewStub mAddLocationViewStub;
    private TextView mLocationRequiredTextView;

    private GeoRankedItemsRecyclerViewAdapter mNearbyItemsAdapter;
    private CurrentLocationProvider mCurrentLocationProvider;
    private UserLocationProvider mUserLocationProvider;

    private LocationProviderBroadcastReceiver mLocationProviderBroadcastReceiver;
    private ConnectivityBroadcastReceiver mConnectivityBroadcastReceiver;
    private Boolean mConnectivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated properly.
        View rootView = inflater.inflate(R.layout.fragment_nearby_items, container, false);

        mNearbyItemsRecyclerView = (RecyclerView) rootView.findViewById(R.id.nearby_items_recycler_view);
        mNearbyItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mNearbyItemsRecyclerView.setNestedScrollingEnabled(false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.nearby_items_progress_bar);
        mAddLocationViewStub = (ViewStub) rootView.findViewById(R.id.add_user_location_stub);

        mSwipeRefreshLayout = (SwipeUpOnlyRefreshLayout) rootView.findViewById(R.id.swiperefresh_main);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        return rootView;
    }

    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        refreshFeaturedItemsBasedOnCurrentLocation();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        mLocationProviderBroadcastReceiver = new LocationProviderBroadcastReceiver(this);
        mMainActivity.registerReceiver(mLocationProviderBroadcastReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        mConnectivity = NetworkUtils.isNetworkAvailable(mMainActivity) ? Boolean.TRUE : Boolean.FALSE;
        mConnectivityBroadcastReceiver = new ConnectivityBroadcastReceiver(this);
        mMainActivity.registerReceiver(mConnectivityBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();

        mMainActivity.unregisterReceiver(mLocationProviderBroadcastReceiver);
        mLocationProviderBroadcastReceiver = null;

        mMainActivity.unregisterReceiver(mConnectivityBroadcastReceiver);
        mConnectivityBroadcastReceiver = null;
    }

    @Override
    public void onStop() {
        if(mCurrentLocationProvider != null) {
            mCurrentLocationProvider.stop();
        }
        if(mUserLocationProvider != null) {
            mUserLocationProvider.stop();
        }

        if(mNearbyItemsAdapter != null) {
            mNearbyItemsAdapter.cleanUpListener();
            mNearbyItemsAdapter = null;
        }

        super.onStop();
    }

    private void refreshFeaturedItemsBasedOnCurrentLocation() {
        Log.i(TAG, "refreshFeaturedItemsBasedOnCurrentLocation");
        disableUserLocationProvider();

        // Current location provider requires permission to access fine location, ask for permission first.
        if (ContextCompat.checkSelfPermission(mMainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(mMainActivity, REQUEST_LOCATION_PERMISSION_FOR_LOCATION_PROVIDER,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            mSwipeRefreshLayout.setRefreshing(false);
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

    private CurrentLocationProvider createCurrentLocationProvider() {
        return new CurrentLocationProvider(mMainActivity, REQUEST_GOOGLE_PLAY_SERVICES_FOR_LOCATION, new AbstractLocationProvider.LocationResultCallback() {
            @Override
            public void onLocationReceived(@NonNull Location location) {
                mAddLocationViewStub.setVisibility(View.GONE);
                resetLocationBasedAdapter(location);
            }

            @Override
            public void onLocationNotAvailable(String failureMessage) {
                showSnackbar(failureMessage);
                showSnackbar(R.string.location_settings_unavailable_using_offline_locations);
                refreshFeaturedItemsBasedOnOfflineLocation();
            }

            @Override
            public void onLocationSettingsNotAvailable() {
                refreshFeaturedItemsBasedOnOfflineLocation();
            }
        });
    }

    private void resetLocationBasedAdapter(@NonNull Location location) {
        if(mNearbyItemsAdapter != null) {
            mNearbyItemsAdapter.cleanUpListener();
        }
        mNearbyItemsAdapter = new GeoRankedItemsRecyclerViewAdapter(mMainActivity, location, this);
        mNearbyItemsRecyclerView.setAdapter(mNearbyItemsAdapter);
    }

    private void refreshFeaturedItemsBasedOnOfflineLocation() {
        Log.i(TAG, "refreshFeaturedItemsBasedOnOfflineLocation");
        disableCurrentLocationProvider();

        // If current location is not available, use offline registered location
        if(mUserLocationProvider == null) {
            mUserLocationProvider = new UserLocationProvider(mMainActivity, new AbstractLocationProvider.LocationResultCallback() {
                @Override
                public void onLocationReceived(@NonNull Location location) {
                    mAddLocationViewStub.setVisibility(View.GONE);
                    resetLocationBasedAdapter(location);
                }

                @Override
                public void onLocationNotAvailable(String failureMessage) {
                    mProgressBar.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setRefreshing(false);
                    if (getCurrentUser() != null) {
                        if(mNearbyItemsAdapter != null) {
                            mNearbyItemsAdapter.clear();
                        }
                        showLocationRequirementViewStub(getResources().getString(R.string.location_required_for_featured_items));
                    } else {
                        showLocationRequirementViewStub(failureMessage);
                    }
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

    private void showLocationRequirementViewStub(String message) {
        if (mAddLocationViewStub.getParent() != null) { // check if not inflated yet
            View addLocationView = mAddLocationViewStub.inflate();
            mLocationRequiredTextView = (TextView) addLocationView.findViewById(R.id.location_required_for_nearby_items_text_view);
            addLocationView.findViewById(R.id.button_save_location).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mMainActivity, AddUserLocationActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            mAddLocationViewStub.setVisibility(View.VISIBLE);
        }
        mLocationRequiredTextView.setText(message);
    }

    @Override
    @SuppressWarnings({"ResourceType"})
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode != REQUEST_LOCATION_PERMISSION_FOR_LOCATION_PROVIDER) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Start current location provider if the permission has been granted.
            if (mCurrentLocationProvider != null) {
                mCurrentLocationProvider.restart();
            } else {
                mCurrentLocationProvider = createCurrentLocationProvider();
                mCurrentLocationProvider.start();
            }
        } else {
            PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getFragmentManager(), "dialog");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES_FOR_LOCATION:
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
                    refreshFeaturedItemsBasedOnOfflineLocation();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onQueryResultReady() {
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onQueryError(Throwable error) {
        showSnackbar(R.string.error_geo_query);
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public GeoFeaturedItemViewHolder onCreateItemViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mMainActivity);
        View view = inflater.inflate(R.layout.featured_nearby_item, parent, false);

        final GeoFeaturedItemViewHolder featuredItemViewHolder = new GeoFeaturedItemViewHolder(mMainActivity, mNearbyItemsAdapter, view);
        featuredItemViewHolder.itemImageView = (ImageView) view.findViewById(R.id.item_image_view);
        featuredItemViewHolder.itemSummaryView = (TextView) view.findViewById(R.id.item_summary);
        featuredItemViewHolder.itemPlaceNameView = (TextView) view.findViewById(R.id.item_place_name);
        featuredItemViewHolder.itemRatingBar = (RatingBar) view.findViewById(R.id.item_rating_bar_indicator);
        ImageButton showOnMapButton = (ImageButton) view.findViewById(R.id.button_show_on_map);
        showOnMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                featuredItemViewHolder.showOnMap();
            }
        });

        return featuredItemViewHolder;
    }

    @Override
    public void onBindItem(final GeoFeaturedItemViewHolder itemViewHolder, ScoreRankedItem scoreRankedItem) {
        itemViewHolder.placedItemInfo = scoreRankedItem.getPlacedItemInfo();
        itemViewHolder.itemSummaryView.setText(scoreRankedItem.getItemSummary());
        itemViewHolder.itemPlaceNameView.setText(scoreRankedItem.getPlaceName());
        itemViewHolder.itemRatingBar.setRating((float) scoreRankedItem.getRating());

        StorageReference photoStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseStorageUtil.STORAGE_URL)
                .child(FirebaseStorageUtil.IMAGES_PATH)
                .child(FirebaseStorageUtil.ITEMS_PATH)
                .child(scoreRankedItem.getItemId());
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
                        itemViewHolder.itemImageView.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(itemViewHolder.itemImageView);
    }

    @Override
    public void onEmptyItems() {
    }

    @Override
    public void onRefresh() {
        refreshFeaturedItemsBasedOnCurrentLocation();
    }

    @Override
    public void handleLocationBroadcast(Context context, Intent intent) {
        if(LocationUtils.isIntentActionLocationEnabled(context, intent)) {
            refreshFeaturedItemsBasedOnCurrentLocation();
        }
    }

    @Override
    public void handleConnectivityBroadcast(Context context, Intent intent) {
        // Query offline user-registered location if connection is re-established and location settings are disabled
        if(NetworkUtils.isIntentActionConnectivityEstablished(context, intent) && !mConnectivity) {
            mConnectivity = true;
            if(!LocationUtils.locationSettingsEnabled(context)) {
                showSnackbar(R.string.location_settings_unavailable_using_offline_locations);
                refreshFeaturedItemsBasedOnOfflineLocation();
            }
        } else if(!NetworkUtils.isNetworkAvailable(context) && mConnectivity) {
            mConnectivity = false;
            if(!LocationUtils.locationSettingsEnabled(context)) {
                showSnackbar(R.string.no_internet_connection);
            }
        }
    }

    @Override
    public void handleRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        refreshFeaturedItemsBasedOnCurrentLocation();
    }
}
