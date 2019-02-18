package com.manouti.itemfinder.item.adapter;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.item.viewholder.RankedFeaturedItemViewHolder;
import com.manouti.itemfinder.model.item.Item;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This adapter handles fetching items located near a specified location and updating a recycler view
 * to show these items sorted by rating.
 */
public class RankedItemsRecyclerViewAdapter extends FeaturedItemsRecyclerViewAdapter<RankedFeaturedItemViewHolder> {

    private static final String TAG = RankedItemsRecyclerViewAdapter.class.getSimpleName();

    private static final int PAGE_SIZE = 15;

    private DatabaseReference mDatabaseReference;
    private Map<Query, ChildEventListener> queryListenersMap = new HashMap<>();
    private int mCurrentPage;
    private double mLastPagedItemRating;

    private RankedAdapterEventListener<RankedFeaturedItemViewHolder> mAdapterEventListener;
    private List<Item> mItems = new LinkedList<>();
    private List<String> mItemIds = new LinkedList<>();

    public RankedItemsRecyclerViewAdapter(Context context, RankedAdapterEventListener<RankedFeaturedItemViewHolder> adapterEventListener) {
        super(context);
        mAdapterEventListener = adapterEventListener;
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        queryItems();
    }

    private void queryItems() {
        // Load an extra item off the page size in order to track next rating value to start with in next load
        Query topItemsQuery = mDatabaseReference.child("items").orderByChild("rating").limitToLast(PAGE_SIZE + 1);
        ChildEventListener topItemsChildEventListener = new PagedChildEventListener(mCurrentPage);
        queryListenersMap.put(topItemsQuery, topItemsChildEventListener);
        topItemsQuery.addChildEventListener(topItemsChildEventListener);

        topItemsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                updateLastPagedItemRating();
                mAdapterEventListener.onQueryResultReady();
                if (dataSnapshot.getChildrenCount() == 0) {
                    mAdapterEventListener.onEmptyItems();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "topItemsQuery:singeValueEventListener:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public RankedFeaturedItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mAdapterEventListener.onCreateItemViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(RankedFeaturedItemViewHolder viewHolder, int position) {
        Item rankedItem = mItems.get(position);
        mAdapterEventListener.onBindItem(viewHolder, rankedItem);
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

    public void cleanUpListener() {
        for(Query query : queryListenersMap.keySet()) {
            ChildEventListener listener = queryListenersMap.get(query);
            query.removeEventListener(listener);
        }
    }

    public void loadMoreItems() {
        if(mLastPagedItemRating > 0) {
            Query topItemsQuery = mDatabaseReference.child("items")
                    .orderByChild("rating")
                    .endAt(mLastPagedItemRating)
                    .limitToLast(PAGE_SIZE);

            ChildEventListener topItemsChildEventListener = new PagedChildEventListener(mCurrentPage);
            queryListenersMap.put(topItemsQuery, topItemsChildEventListener);
            topItemsQuery.addChildEventListener(topItemsChildEventListener);

            topItemsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    updateLastPagedItemRating();
                    mAdapterEventListener.onQueryResultReady();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "topItemsQuery:singeValueEventListener:onCancelled", databaseError.toException());
                }
            });
        } else {
            mAdapterEventListener.onQueryResultReady();
        }
    }

    private void updateLastPagedItemRating() {
        mCurrentPage++;
        if(mItems.size() % PAGE_SIZE == 1) {
            mLastPagedItemRating = mItems.get(mItems.size() - 1).getRating();
        }
    }

    private class PagedChildEventListener implements ChildEventListener {

        private int currentPage;

        PagedChildEventListener(int currentPage) {
            this.currentPage = currentPage;
        }

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
            String itemId = dataSnapshot.getKey();
            Log.d(TAG, "onChildAdded:" + itemId);

            // A new item has been added, add it to the displayed list
            Item item = dataSnapshot.getValue(Item.class);
            item.setId(itemId);

            int insertionIndex = Math.min(mItems.size(), currentPage * PAGE_SIZE); // To prevent any IndexOutOfBoundsException
            mItemIds.add(insertionIndex, dataSnapshot.getKey());
            mItems.add(insertionIndex, item);

            // Update RecyclerView
            notifyItemInserted(insertionIndex);
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
            DatabaseException throwable = databaseError.toException();
            Log.w(TAG, "itemChildEventListener:onCancelled", throwable);
            FirebaseCrash.report(throwable);
            mAdapterEventListener.onQueryError(throwable);
        }
    }
}