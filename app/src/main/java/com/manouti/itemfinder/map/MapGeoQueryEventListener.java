package com.manouti.itemfinder.map;

import android.util.Log;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MapGeoQueryEventListener implements GeoQueryEventListener {

    private static final String TAG = MapGeoQueryEventListener.class.getSimpleName();

    private MapsActivity mMapsActivity;

    private Map<String, ValueEventListener> placeItemsListeners = new ConcurrentHashMap<>();

    protected MapGeoQueryEventListener(MapsActivity mapsActivity) {
        mMapsActivity = mapsActivity;
    }

    @Override
    public void onKeyEntered(final String placeId, final GeoLocation location) {
        ValueEventListener placeItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> itemIds = new ArrayList<>();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String itemId = child.getKey();
                    itemIds.add(itemId);
                }

                if(itemIds.contains(mMapsActivity.mItemId)) {
                    Marker marker = mMapsActivity.mMap.addMarker(new MarkerOptions().title(placeId).position(new LatLng(location.latitude, location.longitude)));
                    mMapsActivity.markers.put(placeId, marker);
                } else {
                    // Remove the old marker
                    Marker marker = mMapsActivity.markers.get(placeId);
                    if (marker != null) {
                        marker.remove();
                        mMapsActivity.markers.remove(placeId);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "placeItemsListener:onCancelled", databaseError.toException());
            }
        };

        placeItemsListeners.put(placeId, placeItemsListener);
        mMapsActivity.mDatabaseReference.child("places").child(placeId).child("items").addValueEventListener(placeItemsListener);
    }

    @Override
    public void onKeyExited(String placeId) {
        // Remove any old marker
        Marker marker = mMapsActivity.markers.get(placeId);
        if (marker != null) {
            marker.remove();
            mMapsActivity.markers.remove(placeId);
        }

        ValueEventListener placeItemsListener = placeItemsListeners.get(placeId);
        if(placeItemsListener != null) {
            mMapsActivity.mDatabaseReference.child("places").child(placeId).child("items").removeEventListener(placeItemsListener);
            placeItemsListeners.remove(placeId);
        }
    }

    @Override
    public void onKeyMoved(String placeId, GeoLocation location) {
        // The key is a place ID, so this scenario should not occur. It is implemented for completeness.
        onKeyExited(placeId);
        onKeyEntered(placeId, location);
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        mMapsActivity.showSnackbar(R.string.error_geo_query);
    }

    protected void cleanUp() {
        if(placeItemsListeners != null) {
            for (String placeId : placeItemsListeners.keySet()) {
                ValueEventListener placeItemsListener = placeItemsListeners.get(placeId);
                mMapsActivity.mDatabaseReference.child("places").child(placeId).child("items").removeEventListener(placeItemsListener);
            }
            placeItemsListeners.clear();
            placeItemsListeners = null;
        }
    }
}
