package com.manouti.itemfinder.user.items.adapter;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.item.viewholder.ItemViewHolder;
import com.manouti.itemfinder.user.items.UserAddedItem;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;

import java.util.ArrayList;
import java.util.List;


public class AddedItemsRecyclerViewAdapter extends RecyclerViewTrackSelectionAdapter<ItemViewHolder> {

    private static final String TAG = "AddedItemsRVAdapter";

    private Context mContext;

    private String mUserId;

    private List<UserAddedItem> mAddedItems = new ArrayList<>();
    private RecyclerViewAdapterEventListener<ItemViewHolder, UserAddedItem> mAdapterEventListener;

    public AddedItemsRecyclerViewAdapter(Context context, String userId, RecyclerViewAdapterEventListener<ItemViewHolder, UserAddedItem> adapterEventListener) {
        this.mContext = context;
        this.mUserId = userId;
        this.mAdapterEventListener = adapterEventListener;
    }

    public void queryItems() {
        mAddedItems.clear();
        DatabaseReference userAddedItemsReference = FirebaseDatabase.getInstance().getReference()
                .child("user-addeditems").child(mUserId);
        userAddedItemsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long itemCount = 0;
                final Iterable<DataSnapshot> placeSnapshots = dataSnapshot.getChildren();
                for(DataSnapshot placeSnapshot : placeSnapshots) {
                    final String placeName = placeSnapshot.child("placeName").getValue(String.class);
                    final Iterable<DataSnapshot> itemSnapshots = placeSnapshot.child("items").getChildren();
                    for(DataSnapshot itemSnapshot : itemSnapshots) {
                        itemCount++;
                        final String itemSummary = itemSnapshot.child("itemS").getValue(String.class);
                        final String status = itemSnapshot.child("status").getValue(String.class);
                        UserAddedItem userAddedItem = new UserAddedItem(itemSnapshot.getKey(), itemSummary, placeName, status);
                        mAddedItems.add(userAddedItem);
                    }
                }
                mAdapterEventListener.onItemCountReady(itemCount);
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "userAddedItems:onCancelled", databaseError.toException());
                FirebaseCrash.report(databaseError.toException());
                Toast.makeText(mContext, "Could not load user added items.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mAdapterEventListener.onCreateAddedItemViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder viewHolder, int position) {
        UserAddedItem addedItem = mAddedItems.get(position);
        mAdapterEventListener.onBindItem(viewHolder, addedItem);
    }

    @Override
    public void clear() {
        mAddedItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mAddedItems.size();
    }

    /**
     * Removes event listeners from the Firebase database reference.
     */
    public void cleanUpListener() {
    }

}