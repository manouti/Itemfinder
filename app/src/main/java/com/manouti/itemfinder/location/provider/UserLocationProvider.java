package com.manouti.itemfinder.location.provider;

import android.app.Activity;
import android.location.Location;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.model.user.UserLocation;


public class UserLocationProvider extends AbstractLocationProvider {
    private static final String TAG = UserLocationProvider.class.getSimpleName();

    private DatabaseReference mUserLocationsReference;


    public UserLocationProvider(Activity activity, LocationResultCallback locationResultCallback) {
        super(activity, locationResultCallback);
    }

    @Override
    public void start() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null) {
            // If location settings are disabled, get the user's registered locations (if any), and compute featured items
            // based on the favorite location, or the first registered user location if no favorite location exists.
            // If none of these criteria is matched, tant pis!
            final String userId = currentUser.getUid();
            mUserLocationsReference = FirebaseDatabase.getInstance().getReference()
                    .child("user-locations").child(userId);
            mUserLocationsReference.keepSynced(true);
            mUserLocationsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Location preferredLocation = null;
                    Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                    for (DataSnapshot userLocationSnapshot : children) {
                        UserLocation userLocation = userLocationSnapshot.getValue(UserLocation.class);
                        if (userLocation.isFavorite()) {
                            preferredLocation = userLocation.toLocation();
                            break;
                        }
                        if (preferredLocation == null) {
                            preferredLocation = userLocation.toLocation();
                        }
                    }

                    if (preferredLocation != null) {
                        mLocationResultCallback.onLocationReceived(preferredLocation);
                    } else {
                        mLocationResultCallback.onLocationNotAvailable(mContext.getResources().getString(R.string.user_location_not_available));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "userLocations:onCancelled", databaseError.toException());
                }
            });
        } else {
            mLocationResultCallback.onLocationNotAvailable(mContext.getResources().getString(R.string.neither_location_nor_user_location_available));
        }
    }

    @Override
    public void stop() {
        if(mUserLocationsReference != null) {
            mUserLocationsReference.keepSynced(false);
        }
    }

}
