package com.manouti.itemfinder.home;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;
import com.manouti.itemfinder.BaseNavigationActivity;
import com.manouti.itemfinder.ItemfinderApplication;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.home.fragment.BaseHomeFragment;
import com.manouti.itemfinder.item.additem.AddItemPlaceActivity;
import com.manouti.itemfinder.location.provider.CurrentLocationProvider;
import com.manouti.itemfinder.service.NearbyItemsTaskService;
import com.manouti.itemfinder.util.PermissionUtils;
import com.manouti.itemfinder.util.viewpager.ViewPagerScrollListener;
import com.ogaclejapan.smarttablayout.SmartTabLayout;


public class MainActivity extends BaseNavigationActivity implements ViewPagerScrollListener, SmartTabLayout.TabProvider {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_SIGN_IN_TO_ADD_ITEM = 111;

    private static final int REQUEST_LOCATION_PERMISSION_FOR_GCM_TASK_SERVICE = 124;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES_FOR_GCM = 1236;

    public static final String TASK_TAG_NEARBY_FAVORITE_ITEMS = "nearby_fav_items_task";
    public static final long SERVICE_TASK_PERIOD_SECONDS = 1800L;

    private static final int REFRESH_MENU_ITEM_ID = 115;

    private GcmNetworkManager mGcmNetworkManager;

    private ViewPager mViewPager;
    private MainActivityFragmentPagerAdapter mFragmentStatePagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewPager = (ViewPager) findViewById(R.id.main_pager);
        final SmartTabLayout tabLayout = (SmartTabLayout) findViewById(R.id.main_tabs);
        tabLayout.setCustomTabView(this);

        mFragmentStatePagerAdapter = new MainActivityFragmentPagerAdapter(getSupportFragmentManager(), this);
        mViewPager.setAdapter(mFragmentStatePagerAdapter);
        tabLayout.setViewPager(mViewPager);

        // Set default preference values for first time app launch
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean runServiceTask = prefs.getBoolean(ItemfinderPreferencesActivity.KEY_NEARBY_ITEMS_RUN_SERVICE, true);
        if(runServiceTask && getCurrentUser() != null) {
            mGcmNetworkManager = GcmNetworkManager.getInstance(this);
            // Check that Google Play Services is available, since we need it to use GcmNetworkManager
            // but the API does not use GoogleApiClient, which would normally perform the check
            // automatically.
            if(checkPlayServicesAvailableForGCM()) {
                scheduleNearbyFavoriteItemsTaskService();
            }
        }
    }

    @Override
    protected int getCurrentNavMenuItemId() {
        return R.id.nav_home;
    }

    @Override
    protected void onDestroy() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(ItemfinderPreferencesActivity.KEY_PREFS_LAST_TIME_MAIN_ACTIVITY_DESTROYED, System.currentTimeMillis());
        editor.apply();

        super.onDestroy();
    }

    private void scheduleNearbyFavoriteItemsTaskService() {
        if(!((ItemfinderApplication) getApplication()).isNearbyItemsTaskServiceScheduled()) {
            Log.d(TAG, "startNearbyFavoriteItemsTaskService");

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission to access the location is missing.
                PermissionUtils.requestPermission(this, REQUEST_LOCATION_PERMISSION_FOR_GCM_TASK_SERVICE,
                        android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            } else {
                // Access to the location has been granted to the app.
                PeriodicTask task = new PeriodicTask.Builder()
                        .setService(NearbyItemsTaskService.class)
                        .setTag(TASK_TAG_NEARBY_FAVORITE_ITEMS)
                        .setPeriod(SERVICE_TASK_PERIOD_SECONDS)
                        .setPersisted(true)
                        .build();
                mGcmNetworkManager.schedule(task);
                ((ItemfinderApplication)getApplication()).setNearbyItemsTaskServiceScheduled(true);
            }
        }
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
            mFragmentStatePagerAdapter.getItem(mViewPager.getCurrentItem()).handleRefresh();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private boolean checkPlayServicesAvailableForGCM() {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int resultCode = availability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(resultCode)) {
                // Show dialog to resolve the error.
                availability.getErrorDialog(this, resultCode, REQUEST_GOOGLE_PLAY_SERVICES_FOR_GCM).show();
            } else {
                // Unresolvable error
                showSnackbar("Google Play Services error");
            }
            return false;
        }
        return true;
    }

    @Override
    @SuppressWarnings({"ResourceType"})
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode != REQUEST_LOCATION_PERMISSION_FOR_GCM_TASK_SERVICE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Schedule the GCM task service for detecting nearby items based on current location.
            // This may be a second attempt, in case the granted permission was due to a previous attempt.
            scheduleNearbyFavoriteItemsTaskService();
        } else {
            PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getSupportFragmentManager(), "dialog");
        }
    }

    public void addItem(View view) {
        if (getCurrentUser() != null) {
            startActivity(AddItemPlaceActivity.class);
        } else {
            logIn(REQUEST_SIGN_IN_TO_ADD_ITEM);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_SIGN_IN_TO_ADD_ITEM:
                if (resultCode == RESULT_OK) {
                    // The user has signed in to add an item, so start the AddItemPlaceActivity.
                    startActivity(AddItemPlaceActivity.class);
                } else if (resultCode != RESULT_CANCELED) {
                    showSnackbar(R.string.unknown_sign_in_response);
                }
                break;
            case REQUEST_GOOGLE_PLAY_SERVICES_FOR_GCM:
                // The user has resolved the error that occurred connecting to Google Play Services
                if (resultCode == Activity.RESULT_OK) {
                    scheduleNearbyFavoriteItemsTaskService();
                }
                break;
            case CurrentLocationProvider.LOCATION_REQUEST_CHECK_SETTINGS:
                // The user has enabled appropriate location settings needed for Google location requests
                BaseHomeFragment[] locationAwareFragments = mFragmentStatePagerAdapter.getLocationAwareFragments();
                for(BaseHomeFragment locaBaseHomeFragment : locationAwareFragments) {
                    locaBaseHomeFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public int getCurrentPage() {
        return mViewPager.getCurrentItem();
    }

    @Override
    public void scrollViewPager(int page) {
        mViewPager.setCurrentItem(page, true);
    }

    @Override
    public View createTabView(ViewGroup container, int position, PagerAdapter adapter) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View tab = inflater.inflate(R.layout.custom_tab, container, false);

        TextView tabTextView = (TextView) tab.findViewById(R.id.custom_tab_text);
        tabTextView.setText(mFragmentStatePagerAdapter.getPageTitle(position));

        ImageView icon = (ImageView) tab.findViewById(R.id.custom_tab_icon);
        switch (position) {
            case 0:
                icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_home_black_24dp));
                break;
            case 1:
                icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_near_me_black_24dp));
                break;
            case 2:
                icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_star_black_24dp));
                break;
            default:
                throw new IllegalStateException("Invalid position: " + position);
        }
        return tab;
    }

}
