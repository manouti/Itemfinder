package com.manouti.itemfinder.item.nearby;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.item.PlacedItemInfo;
import com.manouti.itemfinder.item.viewholder.ItemViewHolder;
import com.manouti.itemfinder.util.recyclerview.RecyclerViewWithContextMenu;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;

import java.util.ArrayList;

public class NearbyItemsActivity extends AppCompatActivity {

    private static final String TAG = NearbyItemsActivity.class.getSimpleName();

    private DatabaseReference mDatabaseFavoriteItemsReference;
    private RecyclerView mRecyclerView;
    private NearbyItemsRecyclerViewAdapter mAdapter;
    private ArrayList<PlacedItemInfo> mItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_items);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) {
            Log.w(TAG, "Current user is unexpectedly null");
            finish();
            return;
        }
        final String userId = currentUser.getUid();
        mDatabaseFavoriteItemsReference = FirebaseDatabase.getInstance().getReference()
                .child("user-favitems").child(userId);

        mRecyclerView = (RecyclerView) findViewById(R.id.nearby_items_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(mRecyclerView);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mItems = getIntent().getParcelableArrayListExtra(Intents.NEARBY_PLACE_ITEMS_INTENT_EXTRA);
        mAdapter = new NearbyItemsRecyclerViewAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.nearby_items_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        RecyclerViewWithContextMenu.RecyclerViewContextMenuInfo info = (RecyclerViewWithContextMenu.RecyclerViewContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.context_nearby_items_action_unfavorite:
                removeFavoriteItem(info.getPosition());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void removeFavoriteItem(int itemRowId) {
        PlacedItemInfo removedItem = mItems.remove(itemRowId);
        mDatabaseFavoriteItemsReference.child(removedItem.getItem().getId()).removeValue();
        mAdapter.notifyDataSetChanged();
    }

    private class NearbyItemsRecyclerViewAdapter extends RecyclerViewTrackSelectionAdapter<ItemViewHolder> {

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(NearbyItemsActivity.this);
            View view = inflater.inflate(R.layout.item_detail, parent, false);

            ItemViewHolder itemViewHolder = new ItemViewHolder(NearbyItemsActivity.this, this, view);
            itemViewHolder.itemSummaryView = (TextView) view.findViewById(R.id.item_summary);
            itemViewHolder.itemPlaceNameView = (TextView) view.findViewById(R.id.item_place_name);
            itemViewHolder.userView = (TextView) view.findViewById(R.id.user_display_name);
            return itemViewHolder;
        }

        @Override
        public void onBindViewHolder(ItemViewHolder viewHolder, int position) {
            PlacedItemInfo itemInfo = mItems.get(position);
            viewHolder.itemSummaryView.setText(itemInfo.getItem().getS());
            viewHolder.itemPlaceNameView.setText(itemInfo.getPlaceName());
            viewHolder.userView.setText(itemInfo.getUserDisplayName());
        }

        @Override
        public void clear() {
            mItems.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

    }
}
