package com.manouti.itemfinder.user.locations;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.manouti.itemfinder.BaseNavigationActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.location.LocationViewHolder;
import com.manouti.itemfinder.model.user.UserLocation;
import com.manouti.itemfinder.user.locations.adapter.LocationRecyclerViewAdapter;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.recyclerview.RecyclerViewWithContextMenu;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;
import com.manouti.itemfinder.util.ui.SwipeUpOnlyRefreshLayout;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

public class SavedLocationsActivity extends BaseNavigationActivity implements SwipeRefreshLayout.OnRefreshListener,
        RecyclerViewAdapterEventListener<LocationViewHolder, UserLocation> {

    private static final String TAG = SavedLocationsActivity.class.getSimpleName();

    private static final int CLEAR_LOCATIONS_MENU_ITEM_ID = 115;
    private static final int REFRESH_MENU_ITEM_ID = 116;

    private SwipeUpOnlyRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mLocationsRecyclerView;
    private LocationRecyclerViewAdapter mAdapter;
    private DatabaseReference mUserLocationsReference;
    private ToolTipRelativeLayout mToolTipRelativeLayout;
    private ToolTipView mToolTipView;
    private ProgressBar mProgressBar;
    private ViewStub mNoLocationViewStub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToolTipRelativeLayout = (ToolTipRelativeLayout) findViewById(R.id.tooltip_relative_layout);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) {
            Log.w(TAG, "Current user is unexpectedly null");
            finish();
            return;
        }
        final String userId = currentUser.getUid();
        mUserLocationsReference = FirebaseDatabase.getInstance().getReference()
                .child("user-locations").child(userId);

        mSwipeRefreshLayout = (SwipeUpOnlyRefreshLayout) findViewById(R.id.swiperefresh_saved_locations);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mLocationsRecyclerView = (RecyclerView) findViewById(R.id.recycler_location);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mLocationsRecyclerView.setLayoutManager(layoutManager);
        mLocationsRecyclerView.setNestedScrollingEnabled(false);
        registerForContextMenu(mLocationsRecyclerView);

        mProgressBar = (ProgressBar) findViewById(R.id.user_locations_progress_bar);
        mNoLocationViewStub = (ViewStub) findViewById(R.id.no_saved_location_stub);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_location_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(AddUserLocationActivity.class);
            }
        });
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_saved_locations;
    }

    @Override
    protected void onStart() {
        super.onStart();
        queryLocations();
    }

    @Override
    protected int getCurrentNavMenuItemId() {
        return R.id.nav_saved_locations;
    }

    private void queryLocations() {
        // Listen for saved locations
        if(mAdapter == null) {
            mAdapter = new LocationRecyclerViewAdapter(mUserLocationsReference, this);
            mLocationsRecyclerView.setAdapter(mAdapter);
        }
        mAdapter.queryLocations();
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.cleanUpListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, CLEAR_LOCATIONS_MENU_ITEM_ID, Menu.NONE, R.string.action_clear_locations)
                .setIcon(R.drawable.ic_delete_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, REFRESH_MENU_ITEM_ID, 0, R.string.action_refresh)
                .setIcon(R.drawable.ic_refresh_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case CLEAR_LOCATIONS_MENU_ITEM_ID:
                clearUserLocations();
                break;
            case REFRESH_MENU_ITEM_ID:
                mSwipeRefreshLayout.setRefreshing(true);
                queryLocations();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void clearUserLocations() {
        DialogUtils.runAfterConfirm(this, new DialogUtils.OnConfirmOperation() {
            @Override
            public void runOperation() {
                mUserLocationsReference.removeValue();
                mAdapter.queryLocations();
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.saved_locations_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        RecyclerViewWithContextMenu.RecyclerViewContextMenuInfo info = (RecyclerViewWithContextMenu.RecyclerViewContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.context_action_favorite:
                setAsFavorite(info.getPosition());
                return true;
            case R.id.context_saved_locations_action_remove:
                deleteLocation(info.getPosition());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void setAsFavorite(long locationRowId) {
        String placeId = mAdapter.mLocations.get((int) locationRowId).getPlaceId();
        mUserLocationsReference.child(placeId).child("favorite").setValue(true);
        mAdapter.queryLocations();
    }

    private void deleteLocation(long locationRowId) {
        String placeId = mAdapter.mLocationPlaceIds.get((int) locationRowId);
        mUserLocationsReference.child(placeId).removeValue();
        mAdapter.queryLocations();
    }

    public void showHelp(View view) {
        if(mToolTipView == null) {
            ToolTip toolTip = new ToolTip()
                    .withText(getString(R.string.saved_locations_info_text_view))
                    .withColor(ContextCompat.getColor(this, R.color.tooltip_color))
                    .withShadow()
                    .withAnimationType(ToolTip.AnimationType.FROM_TOP);
            mToolTipView = mToolTipRelativeLayout.showToolTipForView(toolTip, findViewById(R.id.help_button));
            mToolTipView.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
                @Override
                public void onToolTipViewClicked(ToolTipView toolTipView) {
                    mToolTipView = null;
                }
            });
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(mToolTipView != null) {
            Rect rect = new Rect();
            mToolTipView.getHitRect(rect);
            if (!rect.contains((int) event.getX(), (int) event.getY())) {
                mToolTipView.remove();
                mToolTipView = null;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onRefresh() {
        queryLocations();
    }

    @Override
    public void onItemCountReady(long itemCount) {
        mSwipeRefreshLayout.setRefreshing(false);
        mProgressBar.setVisibility(View.GONE);
        if(itemCount == 0) {
            if (mNoLocationViewStub.getParent() != null) {
                View addLocationView = mNoLocationViewStub.inflate();
                addLocationView.findViewById(R.id.button_save_location).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(AddUserLocationActivity.class);
                    }
                });
            } else {
                mNoLocationViewStub.setVisibility(View.VISIBLE);
            }
        } else {
            mNoLocationViewStub.setVisibility(View.GONE);
        }
    }

    @Override
    public void onError(Exception ex) {
        if(ex != null) {
            FirebaseCrash.report(ex);
        }
        showSnackbar(R.string.unexpected_error_try_later);
    }

    @Override
    public LocationViewHolder onCreateAddedItemViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.location_cardview, parent, false);

        LocationViewHolder locationViewHolder = new LocationViewHolder(this, mAdapter, view) {
            @Override
            public boolean onLongClick(View v) {
                mLocationsRecyclerView.showContextMenuForChild(v);
                return super.onLongClick(v);
            }
        };
        locationViewHolder.titleView = (TextView) view.findViewById(R.id.location_title);
        locationViewHolder.descriptionView = (TextView) view.findViewById(R.id.location_description);
        locationViewHolder.starView = (ImageView) view.findViewById(R.id.location_star);

        return locationViewHolder;
    }

    @Override
    public void onBindItem(LocationViewHolder viewHolder, UserLocation location) {
        viewHolder.titleView.setText(location.getPlaceTitle());
        viewHolder.descriptionView.setText(location.getPlaceDescription());
        if(location.isFavorite()) {
            viewHolder.starView.setImageResource(R.drawable.ic_toggle_star_24);
            viewHolder.starView.setVisibility(View.VISIBLE);
        }
    }
}
