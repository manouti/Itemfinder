package com.manouti.itemfinder.user.items.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.ViewGroup;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.score.LocationItemScoreEvaluator;
import com.manouti.itemfinder.item.score.ScoreRankedItem;
import com.manouti.itemfinder.item.adapter.GeoRankedAdapterEventListener;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.place.Place;
import com.manouti.itemfinder.model.place.PlaceItem;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class handles all the work for requesting featured items from the database and updating the recycler view UI
 * to show these items.
 */
public class ReviewItemsRecyclerViewAdapter<VH extends RecyclerViewTrackSelectionAdapter.ViewHolder> extends RecyclerViewTrackSelectionAdapter<VH> {

    private static final String TAG = "ReviewItemsRVAdapter";

    /**
     * 4 days required between reviews to same user
     */
    private static final long MINIMUM_TIME_BETWEEN_REVIEWS_TO_SAME_USER = 4 * 24 * 60 * 60 * 1000L;

    private Context mContext;

    private String mCurrentUid;
    private Location mPreferredLocation;
    private DatabaseReference mDatabaseReference;

    private GeoQuery mGeoQuery;
    private GeoFire mPlacesGeoFire;
    private LocationItemScoreEvaluator mItemScoreEvaluator;

    private GeoRankedAdapterEventListener<VH> mAdapterEventListener;

    private Set<ScoreRankedItem> mScoreRankedItems = new HashSet<>();
    private List<ScoreRankedItem> mSortedRankedItemList = new ArrayList<>();

    // Store place items listeners, mapped by place ID
    private Map<String, PlaceItemsListener> mPlaceItemsListeners = new ConcurrentHashMap<>();

    private Map<String, DatabaseReference> mSyncedRefs = new HashMap<>();

    public ReviewItemsRecyclerViewAdapter(Context context, String currentUid, @NonNull Location preferredLocation, GeoRankedAdapterEventListener<VH> adapterEventListener) {
        mContext = context;
        mCurrentUid = currentUid;
        mPreferredLocation = preferredLocation;
        mAdapterEventListener = adapterEventListener;
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mPlacesGeoFire = new GeoFire(mDatabaseReference.child("places-geo"));
    }

    public void queryItems() {
        mItemScoreEvaluator = new LocationItemScoreEvaluator(mPreferredLocation);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        float radius = Float.parseFloat(prefs.getString(ItemfinderPreferencesActivity.KEY_LOCATION_RADIUS,
                mContext.getString(R.string.preferences_default_nearby_fav_item_proximity))); // in kilometers

        mGeoQuery = mPlacesGeoFire.queryAtLocation(
                new GeoLocation(mPreferredLocation.getLatitude(), mPreferredLocation.getLongitude()), radius);
        mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(final String placeId, final GeoLocation location) {
                mDatabaseReference.child("places").child(placeId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Place place = dataSnapshot.getValue(Place.class);
                        if(place != null) {
                            place.setId(placeId);
                            place.setLatitude(location.latitude);
                            place.setLongitude(location.longitude);
                            PlaceItemsListener placeListener = new PlaceItemsListener(place);
                            mPlaceItemsListeners.put(placeId, placeListener);
                            mDatabaseReference.child("proposed-items").child(placeId).child("items").addChildEventListener(placeListener);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getPlace:onCancelled", databaseError.toException());
                    }
                });
            }

            @Override
            public void onKeyExited(final String placeId) {
                PlaceItemsListener placeListener = mPlaceItemsListeners.get(placeId);
                if (placeListener != null) {
                    mDatabaseReference.child("places").child(placeId).removeEventListener(placeListener);
                    mPlaceItemsListeners.remove(placeId);
                }

                // Remove all items that belong to this place
                filterRankedItems(new Predicate<ScoreRankedItem>() {
                    @Override
                    public boolean evaluate(ScoreRankedItem rankedItem) {
                        return !StringUtils.equals(rankedItem.getPlaceId(), placeId);
                    }
                });
                sortItemsAndNotifyDataSetChanged();
            }

            @Override
            public void onKeyMoved(final String placeId, final GeoLocation location) {
                // The key is a place ID, so this scenario should not occur. It is implemented for completeness.
                PlaceItemsListener placeItemsListener = mPlaceItemsListeners.get(placeId);
                if (placeItemsListener != null) {
                    placeItemsListener.setLocation(location);
                }
            }

            @Override
            public void onGeoQueryReady() {
                mAdapterEventListener.onQueryResultReady();
                if(mScoreRankedItems.isEmpty()) {
                    mAdapterEventListener.onEmptyItems();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Throwable throwable = error.toException();
                FirebaseCrash.report(throwable);
                mAdapterEventListener.onQueryError(throwable);
            }
        });
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return mAdapterEventListener.onCreateItemViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        ScoreRankedItem rankedItem = IterableUtils.get(mSortedRankedItemList, position);
        mAdapterEventListener.onBindItem(viewHolder, rankedItem);
    }

    @Override
    public void clear() {
        mScoreRankedItems.clear();
        mSortedRankedItemList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mScoreRankedItems.size();
    }

    private void sortItemsAndNotifyDataSetChanged() {
        mSortedRankedItemList = new ArrayList<>(mScoreRankedItems);
        Collections.sort(mSortedRankedItemList, new Comparator<ScoreRankedItem>() {
            @Override
            public int compare(ScoreRankedItem rankedItem1, ScoreRankedItem rankedItem2) {
                if (rankedItem1 == null) {
                    return rankedItem2 == null ? 0 : 1;
                }
                if (rankedItem2 == null) {
                    return -1;
                }

                double rank1 = rankedItem1.getScore();
                double rank2 = rankedItem2.getScore();
                if (rank1 < rank2) {
                    return 1;
                } else if (rank1 > rank2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        notifyDataSetChanged();
    }

    public void cleanUpListener() {
        // remove all event listeners to stop updating in the background
        if(mGeoQuery != null) {
            mGeoQuery.removeAllListeners();
        }

        if(mPlaceItemsListeners != null) {
            for (String placeId : mPlaceItemsListeners.keySet()) {
                PlaceItemsListener placeItemsListener = mPlaceItemsListeners.get(placeId);
                mDatabaseReference.child("places").child(placeId).removeEventListener(placeItemsListener);
            }
            mPlaceItemsListeners.clear();
            mPlaceItemsListeners = null;
        }

        for(Map.Entry<String, DatabaseReference> entry : mSyncedRefs.entrySet()) {
            entry.getValue().keepSynced(false);
        }
    }

    private class PlaceItemsListener implements ChildEventListener {

        private Place place;

        PlaceItemsListener(Place place) {
            this.place = place;
        }

        public void setLocation(GeoLocation location) {
            place.setLatitude(location.latitude);
            place.setLongitude(location.longitude);
        }

        @Override
        public void onChildAdded(DataSnapshot itemSnapshot, String previousChildName) {
            final PlaceItem placeItem = itemSnapshot.getValue(PlaceItem.class);
            final String itemId = itemSnapshot.getKey();
            placeItem.setItemId(itemId);
            if(!StringUtils.equals(placeItem.getUid(), mCurrentUid)) {
                String lastRevewTimestampPath = "user-revieweditems" + "/" + mCurrentUid + "/" + placeItem.getUid() + "/lastRev";
                DatabaseReference lastReviewTimestampRef = mDatabaseReference.child(lastRevewTimestampPath);
                lastReviewTimestampRef.keepSynced(true);
                mSyncedRefs.put(lastRevewTimestampPath, lastReviewTimestampRef);
                lastReviewTimestampRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean reviewAllowed = true;
                        if (dataSnapshot.exists()) {
                            long lastReviewDate = dataSnapshot.getValue(Long.class);
                            reviewAllowed = System.currentTimeMillis() - lastReviewDate > MINIMUM_TIME_BETWEEN_REVIEWS_TO_SAME_USER;
                        }

                        if (reviewAllowed) {
                            mDatabaseReference.child("user-revieweditems")
                                    .child(mCurrentUid)
                                    .child(placeItem.getUid())
                                    .child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    // Check if item is already reviewed by current user
                                    if (!dataSnapshot.exists()) {
                                        mDatabaseReference.child("items").child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                final Item item = dataSnapshot.getValue(Item.class);
                                                item.setId(itemId);
                                                final double itemScore = mItemScoreEvaluator.getItemScore(place.getLatitude(), place.getLongitude());
                                                mDatabaseReference.child("users").child(placeItem.getUid()).child("dn").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        String userDisplayName = dataSnapshot.getValue(String.class);
                                                        ScoreRankedItem rankedItem = new ScoreRankedItem(item, place.getId(), place.getName(), placeItem.getUid(), userDisplayName, itemScore);
                                                        mScoreRankedItems.add(rankedItem);
                                                        sortItemsAndNotifyDataSetChanged();
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                        Log.w(TAG, "getUserDisplayName:onCancelled", databaseError.toException());
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Log.w(TAG, "getItemDesc:onCancelled", databaseError.toException());
                                            }
                                        });
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.w(TAG, "getUserReviewedItem:onCancelled", databaseError.toException());
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getLastReviewDate:onCancelled", databaseError.toException());
                    }
                });
            }
        }

        @Override
        public void onChildChanged(DataSnapshot itemSnapshot, String previousChildName) {
        }

        @Override
        public void onChildRemoved(DataSnapshot itemSnapshot) {
            final PlaceItem placeItem = itemSnapshot.getValue(PlaceItem.class);
            placeItem.setItemId(itemSnapshot.getKey());
            filterRankedItems(new Predicate<ScoreRankedItem>() {
                @Override
                public boolean evaluate(ScoreRankedItem rankedItem) {
                    return !(StringUtils.equals(rankedItem.getItemId(), placeItem.getItemId())
                            && StringUtils.equals(rankedItem.getPlaceId(), place.getId()));
                }
            });
            sortItemsAndNotifyDataSetChanged();
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            Log.i(TAG, "placeItemsEventListener:onChildMoved");
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "placeItemsEventListener:onCancelled", databaseError.toException());
        }

    }

    /**
     * Filter the ranked items collection by applying a Predicate to each element.
     * If the predicate returns false, remove the element.
     * @param predicate the predicate to be used to filter items
     */
    private void filterRankedItems(Predicate<ScoreRankedItem> predicate) {
        CollectionUtils.filter(mScoreRankedItems, predicate);
        if(mScoreRankedItems.isEmpty()) {
            mAdapterEventListener.onEmptyItems();
        }
    }

}
