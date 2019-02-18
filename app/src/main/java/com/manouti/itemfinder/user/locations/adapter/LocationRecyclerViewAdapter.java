package com.manouti.itemfinder.user.locations.adapter;

import android.util.Log;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.location.LocationViewHolder;
import com.manouti.itemfinder.model.user.UserLocation;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;

import java.util.ArrayList;
import java.util.List;


public class LocationRecyclerViewAdapter extends RecyclerViewTrackSelectionAdapter<LocationViewHolder> {

    private static final String TAG = "LocationRecViewAdapter";

    private DatabaseReference mUserLocationsReference;

    public List<String> mLocationPlaceIds = new ArrayList<>();
    public List<UserLocation> mLocations = new ArrayList<>();
    private RecyclerViewAdapterEventListener<LocationViewHolder, UserLocation> mAdapterEventListener;

    public LocationRecyclerViewAdapter(DatabaseReference userLocationsReference,
                                       RecyclerViewAdapterEventListener<LocationViewHolder, UserLocation> adapterEventListener) {
        this.mUserLocationsReference = userLocationsReference;
        this.mAdapterEventListener = adapterEventListener;
    }

    public void queryLocations() {
        mLocations.clear();
        mLocationPlaceIds.clear();
        mUserLocationsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot userLocationSnapshot : children) {
                    String placeId = userLocationSnapshot.getKey();
                    Log.d(TAG, "onDataChange:userLocation:placeId:" + placeId);

                    UserLocation userLocation = userLocationSnapshot.getValue(UserLocation.class);

                    // Update RecyclerView
                    mLocationPlaceIds.add(placeId);
                    mLocations.add(userLocation);
                }

                mAdapterEventListener.onItemCountReady(mLocations.size());
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "userLocations:onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public LocationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mAdapterEventListener.onCreateAddedItemViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(LocationViewHolder viewHolder, int position) {
        UserLocation location = mLocations.get(position);
        mAdapterEventListener.onBindItem(viewHolder, location);
    }

    @Override
    public void clear() {
        mLocations.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mLocations.size();
    }

    /**
     * Removes event listeners from the Firebase database reference.
     */
    public void cleanUpListener() {
    }

}
