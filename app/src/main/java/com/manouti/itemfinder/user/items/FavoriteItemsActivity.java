package com.manouti.itemfinder.user.items;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.BaseNavigationActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.detail.ItemDetailActivity;
import com.manouti.itemfinder.map.MapsActivity;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.NetworkUtils;
import com.manouti.itemfinder.util.recyclerview.RecyclerViewWithContextMenu;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import java.util.ArrayList;
import java.util.List;

public class FavoriteItemsActivity extends BaseNavigationActivity {

    private static final String TAG = FavoriteItemsActivity.class.getSimpleName();

    private static final int CLEAR_FAV_ITEMS_MENU_ITEM_ID = 115;

    private RecyclerView mItemsRecyclerView;
    private FavoriteItemsAdapter mAdapter;
    private DatabaseReference mDatabaseFavoriteItemsReference;
    private ToolTipRelativeLayout mToolTipRelativeLayout;
    private ToolTipView mToolTipView;
    private ProgressBar mProgressBar;
    private TextView mNoConnectionTextView;
    private TextView mNoFavoriteItemTextView;

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
        mDatabaseFavoriteItemsReference = FirebaseDatabase.getInstance().getReference()
                .child("user-favitems").child(userId);

        mItemsRecyclerView = (RecyclerView) findViewById(R.id.recycler_favorite_item);
        mItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(mItemsRecyclerView);

        mProgressBar = (ProgressBar) findViewById(R.id.favorite_items_progress_bar);
        mNoConnectionTextView = (TextView) findViewById(R.id.no_connection_text_view);

        mNoFavoriteItemTextView = (TextView) findViewById(R.id.no_favorite_item_text_view);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getString(R.string.no_favorite_items_text_view));
        Bitmap addFavItemBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_star_border_black_24dp);
        spannableStringBuilder.setSpan(new ImageSpan(this, addFavItemBitmap), 66, 67, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        mNoFavoriteItemTextView.setText(spannableStringBuilder, TextView.BufferType.SPANNABLE);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_favorite_items;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Listen for favorite items
        mAdapter = new FavoriteItemsAdapter();
        mItemsRecyclerView.setAdapter(mAdapter);

        mDatabaseFavoriteItemsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == 0) {
                    mProgressBar.setVisibility(View.GONE);
                    mNoFavoriteItemTextView.setVisibility(View.VISIBLE);
                    // Hide no connection view since we enabled Firebase disk persistence
                    mNoConnectionTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "favoriteItemsCount:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    protected int getCurrentNavMenuItemId() {
        return R.id.nav_favorite_items;
    }

    @Override
    public void onStop() {
        super.onStop();

        // Clean up items listener
        mAdapter.cleanUpListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, CLEAR_FAV_ITEMS_MENU_ITEM_ID, Menu.NONE, R.string.action_clear_items)
                .setIcon(R.drawable.ic_delete_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == CLEAR_FAV_ITEMS_MENU_ITEM_ID) {
            clearFavoriteItems();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void clearFavoriteItems() {
        DialogUtils.runAfterConfirm(this, new DialogUtils.OnConfirmOperation() {
            @Override
            public void runOperation() {
                mDatabaseFavoriteItemsReference.removeValue();
                mAdapter.notifyDataSetChanged(); // TODO is this needed?
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.favorite_items_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        RecyclerViewWithContextMenu.RecyclerViewContextMenuInfo info = (RecyclerViewWithContextMenu.RecyclerViewContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.context_fav_items_action_remove:
                removeFavoriteItem(info.getPosition());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void removeFavoriteItem(int itemRowId) {
        String itemId = mAdapter.mItemIds.get(itemRowId);
        mDatabaseFavoriteItemsReference.child(itemId).removeValue();
        mAdapter.notifyDataSetChanged();  // TODO is this needed?
    }

    public void showHelp(View view) {
        if(mToolTipView == null) {
            ToolTip toolTip = new ToolTip()
                    .withText(getString(R.string.favorite_items_info_text_view))
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

    private class FavoriteItemsAdapter extends RecyclerViewTrackSelectionAdapter<FavoriteItemsAdapter.ItemViewHolder> {
        private ChildEventListener mChildEventListener;

        private List<String> mItemIds = new ArrayList<>();
        private List<Item> mItems = new ArrayList<>();

        public FavoriteItemsAdapter() {
            // Create child event listener
            // [START child_event_listener_recycler]
            ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    String itemId = dataSnapshot.getKey();
                    Log.d(TAG, "onChildAdded:" + itemId);

                    // A new item has been added, add it to the displayed list
                    Item item = dataSnapshot.getValue(Item.class);
                    item.setId(itemId);

                    mItemIds.add(dataSnapshot.getKey());
                    mItems.add(item);
                    // Update RecyclerView
                    notifyItemInserted(mItems.size() - 1);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    String itemId = dataSnapshot.getKey();
                    Log.d(TAG, "onChildChanged:" + itemId);

                    // An item has changed, use the key to determine if we are displaying this
                    // item and if so display the changed item.
                    Item newItem = dataSnapshot.getValue(Item.class);
                    newItem.setId(itemId);

                    int itemIndex = mItemIds.indexOf(itemId);
                    if (itemIndex > -1) {
                        // Replace with the new data
                        mItems.set(itemIndex, newItem);

                        // Update the RecyclerView
                        notifyItemChanged(itemIndex);
                    } else {
                        Log.w(TAG, "onChildChanged:unknown_child:" + itemId);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                    // An item has been removed, use the key to determine if we are displaying this
                    // item and if so remove it.
                    String itemKey = dataSnapshot.getKey();

                    int itemIndex = mItemIds.indexOf(itemKey);
                    if (itemIndex > -1) {
                        // Remove data from the list
                        mItemIds.remove(itemIndex);
                        mItems.remove(itemIndex);

                        // Update the RecyclerView
                        notifyItemRemoved(itemIndex);
                    } else {
                        Log.w(TAG, "onChildRemoved:unknown_child:" + itemKey);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "favoriteItems:onCancelled", databaseError.toException());
                    FirebaseCrash.report(databaseError.toException());
                    showSnackbar("Could not load favorite items.");
                }
            };
            mDatabaseFavoriteItemsReference.addChildEventListener(childEventListener);

            if(!NetworkUtils.isNetworkAvailable(FavoriteItemsActivity.this)) {
                mNoConnectionTextView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
            }

            // Store reference to listener so it can be removed on app stop
            mChildEventListener = childEventListener;
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            mProgressBar.setVisibility(View.GONE);
            mNoConnectionTextView.setVisibility(View.GONE);

            LayoutInflater inflater = LayoutInflater.from(FavoriteItemsActivity.this);
            View view = inflater.inflate(R.layout.favorite_item, parent, false);

            final ItemViewHolder viewHolder = new ItemViewHolder(view);
            viewHolder.summaryView = (TextView) view.findViewById(R.id.item_summary);
            viewHolder.descView = (TextView) view.findViewById(R.id.item_desc);
            viewHolder.ratingView = (TextView) view.findViewById(R.id.item_rating);
            ImageButton showOnMapButton = (ImageButton) view.findViewById(R.id.button_show_on_map);
            showOnMapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewHolder.showOnMap();
                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position) {
            Item item = mItems.get(position);
            viewHolder.item = item;
            viewHolder.summaryView.setText(item.getS());
            viewHolder.descView.setText(item.getDesc());
            viewHolder.ratingView.setText(String.valueOf(item.getRating()));
        }

        @Override
        public void clear() {
            mItems.clear();
            mItemIds.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        /**
         * Removes event listeners from the Firebase database reference.
         */
        public void cleanUpListener() {
            if (mChildEventListener != null) {
                mDatabaseFavoriteItemsReference.removeEventListener(mChildEventListener);
            }
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        protected class ItemViewHolder extends RecyclerViewTrackSelectionAdapter.ViewHolder implements View.OnLongClickListener {
            protected Item item;
            protected TextView summaryView;
            protected TextView descView;
            protected TextView ratingView;

            public ItemViewHolder(View itemView) {
                super(FavoriteItemsActivity.this, FavoriteItemsAdapter.this, itemView);
                itemView.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View v) {
                super.onClick(v);
                Intent itemDetailIntent = new Intent(mContext, ItemDetailActivity.class);
                itemDetailIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_ID_INPUT, item.getId());
                mContext.startActivity(itemDetailIntent);
            }

            public void showOnMap() {
                Intent mapIntent = new Intent(mContext, MapsActivity.class);
                mapIntent.putExtra(Intents.MAP_ITEM_INPUT, item);
                mContext.startActivity(mapIntent);
            }

            @Override
            public boolean onLongClick(View view) {
                mItemsRecyclerView.showContextMenuForChild(view);
                return true;
            }
        }
    }

}
