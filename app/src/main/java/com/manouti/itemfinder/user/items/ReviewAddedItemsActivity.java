package com.manouti.itemfinder.user.items;

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
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseUser;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.BaseNavigationActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.viewholder.ItemViewHolder;
import com.manouti.itemfinder.item.score.ScoreRankedItem;
import com.manouti.itemfinder.item.adapter.GeoRankedAdapterEventListener;
import com.manouti.itemfinder.location.provider.CurrentLocationProvider;
import com.manouti.itemfinder.location.provider.AbstractLocationProvider.LocationResultCallback;
import com.manouti.itemfinder.location.provider.UserLocationProvider;
import com.manouti.itemfinder.user.items.adapter.ReviewItemsRecyclerViewAdapter;
import com.manouti.itemfinder.user.locations.AddUserLocationActivity;
import com.manouti.itemfinder.util.LocationUtils;
import com.manouti.itemfinder.util.NetworkUtils;
import com.manouti.itemfinder.util.PermissionUtils;
import com.manouti.itemfinder.util.broadcast.ConnectivityBroadcastReceiver;
import com.manouti.itemfinder.util.broadcast.LocationProviderBroadcastReceiver;
import com.manouti.itemfinder.util.ui.SwipeUpOnlyRefreshLayout;

/**
 * This activity behaves similar to MainActivity in terms of customizing the suggested items to review
 * based on the user's current location.
 */
public class ReviewAddedItemsActivity extends BaseNavigationActivity implements GeoRankedAdapterEventListener<ItemViewHolder>,
                                                                                SwipeRefreshLayout.OnRefreshListener,
                                                                                LocationProviderBroadcastReceiver.LocationBroadcastHandler,
                                                                                ConnectivityBroadcastReceiver.ConnectivityBroadcastHandler {
    private static final String TAG = "ReviewItemsActivity";

    private static final int REQUEST_LOCATION_PERMISSION = 1231;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1232;

    private static final int REFRESH_MENU_ITEM_ID = 116;

    private RecyclerView mFeaturedItemsRecyclerView;
    private TextView mNoItemsToReview;
    private ProgressBar mProgressBar;
    private SwipeUpOnlyRefreshLayout mSwipeRefreshLayout;
    private ViewStub mAddLocationViewStub;
    private TextView mLocationRequiredTextView;

    private ReviewItemsRecyclerViewAdapter<ItemViewHolder> mReviewItemsAdapter;
    private CurrentLocationProvider mCurrentLocationProvider;
    private UserLocationProvider mUserLocationProvider;

    private LocationProviderBroadcastReceiver mLocationProviderBroadcastReceiver;
    private ConnectivityBroadcastReceiver mConnectivityBroadcastReceiver;
    private boolean mConnectivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = getCurrentUser();
        if(currentUser == null) {
            Log.w(TAG, "Current user is unexpectedly null");
            finish();
            return;
        }

        mFeaturedItemsRecyclerView = (RecyclerView) findViewById(R.id.items_to_review_recycler_view);
        mFeaturedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFeaturedItemsRecyclerView.setNestedScrollingEnabled(false);

        mNoItemsToReview = (TextView) findViewById(R.id.no_items_to_review_text_view);
        mProgressBar = (ProgressBar) findViewById(R.id.review_items_progress_bar);
        mAddLocationViewStub = (ViewStub) findViewById(R.id.add_user_location_stub);

        mSwipeRefreshLayout = (SwipeUpOnlyRefreshLayout) findViewById(R.id.swiperefresh_review);
        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_review_added_items;
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshFeaturedItemsBasedOnCurrentLocation();
    }

    @Override
    protected int getCurrentNavMenuItemId() {
        return R.id.nav_rate_review_added_items;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mLocationProviderBroadcastReceiver = new LocationProviderBroadcastReceiver(this);
        registerReceiver(mLocationProviderBroadcastReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        mConnectivity = NetworkUtils.isNetworkAvailable(this);
        mConnectivityBroadcastReceiver = new ConnectivityBroadcastReceiver(this);
        registerReceiver(mConnectivityBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mLocationProviderBroadcastReceiver);
        mLocationProviderBroadcastReceiver = null;

        unregisterReceiver(mConnectivityBroadcastReceiver);
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

        if(mReviewItemsAdapter != null) {
            mReviewItemsAdapter.cleanUpListener();
        }

        super.onStop();
    }

    private void refreshFeaturedItemsBasedOnCurrentLocation() {
        Log.i(TAG, "refreshFeaturedItemsBasedOnCurrentLocation");
        disableUserLocationProvider();

        // Current location provider requires permission to access fine location, ask for permission first.
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, REQUEST_LOCATION_PERMISSION,
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
        return new CurrentLocationProvider(this, REQUEST_GOOGLE_PLAY_SERVICES, new LocationResultCallback() {
            @Override
            public void onLocationReceived(@NonNull Location location) {
                mAddLocationViewStub.setVisibility(View.GONE);
                resetLocationBasedAdapter(location);
            }

            @Override
            public void onLocationNotAvailable(String failureMessage) {
                showSnackbar(failureMessage);
            }

            @Override
            public void onLocationSettingsNotAvailable() {
                refreshFeaturedItemsBasedOnOfflineLocation();
            }
        });
    }

    private void refreshFeaturedItemsBasedOnOfflineLocation() {
        Log.i(TAG, "refreshFeaturedItemsBasedOnOfflineLocation");
        disableCurrentLocationProvider();

        // If current location is not available, use offline registered location
        if(mUserLocationProvider == null) {
            mUserLocationProvider = new UserLocationProvider(this, new LocationResultCallback() {
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
                        if(mReviewItemsAdapter != null) {
                            mReviewItemsAdapter.clear();
                        }
                        showLocationRequirementViewStub(getResources().getString(R.string.location_required_to_review_items));
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

    private void showLocationRequirementViewStub(String message) {
        if (mAddLocationViewStub.getParent() != null) { // check if not inflated yet // check if not inflated yet
            View addLocationView = mAddLocationViewStub.inflate();
            mLocationRequiredTextView = (TextView) addLocationView.findViewById(R.id.location_required_to_review_item_text_view);
            addLocationView.findViewById(R.id.button_save_location).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(AddUserLocationActivity.class);
                }
            });
        } else {
            mAddLocationViewStub.setVisibility(View.VISIBLE);
        }
        mLocationRequiredTextView.setText(message);
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

    @Override
    @SuppressWarnings({"ResourceType"})
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
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
                    refreshFeaturedItemsBasedOnOfflineLocation();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void resetLocationBasedAdapter(Location location) {
        mReviewItemsAdapter = new ReviewItemsRecyclerViewAdapter<>(this, getCurrentUser().getUid(), location, this);
        mFeaturedItemsRecyclerView.setAdapter(mReviewItemsAdapter);
        mReviewItemsAdapter.queryItems();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, REFRESH_MENU_ITEM_ID, 0, R.string.action_refresh)
                .setIcon(R.drawable.ic_refresh_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == REFRESH_MENU_ITEM_ID) {
            mSwipeRefreshLayout.setRefreshing(true);
            refreshFeaturedItemsBasedOnCurrentLocation();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public ItemViewHolder onCreateItemViewHolder(ViewGroup parent) {
        mNoItemsToReview.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.item_detail, parent, false);

        ItemViewHolder itemViewHolder = new ItemViewHolder(this, mReviewItemsAdapter, view);
        itemViewHolder.itemSummaryView = (TextView) view.findViewById(R.id.item_summary);
        itemViewHolder.itemPlaceNameView = (TextView) view.findViewById(R.id.item_place_name);
        itemViewHolder.userView = (TextView) view.findViewById(R.id.user_display_name);
        return itemViewHolder;
    }

    @Override
    public void onBindItem(ItemViewHolder viewHolder, final ScoreRankedItem scoreRankedItem) {
        viewHolder.itemSummaryView.setText(scoreRankedItem.getItemSummary());
        viewHolder.itemPlaceNameView.setText(scoreRankedItem.getPlaceName());
        viewHolder.userView.setText(scoreRankedItem.getUserDisplayName());
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent proposedItemIntent = new Intent(ReviewAddedItemsActivity.this, ReviewProposedItemActivity.class);
                proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_ID, scoreRankedItem.getItemId());
                proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_PLACE_ID, scoreRankedItem.getPlaceId());
                proposedItemIntent.putExtra(Intents.PROPOSED_ITEM_USER_ID, scoreRankedItem.getUserId());
                startActivity(proposedItemIntent);
            }
        });
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
    public void onEmptyItems() {
        mNoItemsToReview.setVisibility(View.VISIBLE);
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

}
