package com.manouti.itemfinder.user.items;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.manouti.itemfinder.BaseNavigationActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.viewholder.ItemViewHolder;
import com.manouti.itemfinder.item.additem.AddItemPlaceActivity;
import com.manouti.itemfinder.user.items.adapter.AddedItemsRecyclerViewAdapter;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;
import com.manouti.itemfinder.util.ui.SwipeUpOnlyRefreshLayout;

public class UserAddedItemsActivity extends BaseNavigationActivity implements RecyclerViewAdapterEventListener<ItemViewHolder, UserAddedItem>,
                                                                              SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = UserAddedItemsActivity.class.getSimpleName();

    private static final int REFRESH_MENU_ITEM_ID = 115;

    private SwipeUpOnlyRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mAddedItemsRecyclerView;
    private AddedItemsRecyclerViewAdapter mAdapter;
    private TextView mNoAddedItems;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = getCurrentUser();
        if(currentUser == null) {
            Log.w(TAG, "Current user is unexpectedly null");
            finish();
            return;
        }

        mSwipeRefreshLayout = (SwipeUpOnlyRefreshLayout) findViewById(R.id.swiperefresh_added_items);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mAddedItemsRecyclerView = (RecyclerView) findViewById(R.id.recycler_added_items);
        mAddedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAddedItemsRecyclerView.setNestedScrollingEnabled(false);

        mNoAddedItems = (TextView) findViewById(R.id.no_added_items_text_view);
        mProgressBar = (ProgressBar) findViewById(R.id.added_items_progress_bar);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_user_added_items;
    }

    @Override
    protected void onStart() {
        super.onStart();
        queryItems();
    }

    @Override
    protected int getCurrentNavMenuItemId() {
        return R.id.nav_added_items;
    }

    private void queryItems() {
        // Listen for added items
        if(mAdapter == null) {
            mAdapter = new AddedItemsRecyclerViewAdapter(this, getCurrentUser().getUid(), this);
            mAddedItemsRecyclerView.setAdapter(mAdapter);
        }
        mAdapter.queryItems();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Clean up added items listener
        mAdapter.cleanUpListener();
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
            queryItems();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemCountReady(long itemCount) {
        mSwipeRefreshLayout.setRefreshing(false);
        mProgressBar.setVisibility(View.GONE);
        mNoAddedItems.setVisibility(itemCount == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onError(Exception ex) {
        if(ex != null) {
            FirebaseCrash.report(ex);
        }
        showSnackbar(R.string.unexpected_error_try_later);
    }

    @Override
    public ItemViewHolder onCreateAddedItemViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.item_summary_card_view, parent, false);

        ItemViewHolder itemViewHolder = new ItemViewHolder(this, mAdapter, view);
        itemViewHolder.itemSummaryView = (TextView) view.findViewById(R.id.item_summary);
        itemViewHolder.itemPlaceNameView = (TextView) view.findViewById(R.id.item_place_name);

        return itemViewHolder;
    }

    @Override
    public void onBindItem(ItemViewHolder viewHolder, UserAddedItem addedItem) {
        viewHolder.itemId = addedItem.getItemId();
        viewHolder.itemSummaryView.setText(addedItem.getItemSummary());
        viewHolder.itemPlaceNameView.setText(addedItem.getPlaceName());
    }

    public void addItem(View view) {
        if (getCurrentUser() != null) {
            startActivity(AddItemPlaceActivity.class);
        } else {
            // This case should not happen (this activity is only available when logged in)
            // but in all cases, display a login required message.
            showSnackbar(R.string.must_log_in_to_add_item);
        }
    }

    @Override
    public void onRefresh() {
        queryItems();
    }
}
