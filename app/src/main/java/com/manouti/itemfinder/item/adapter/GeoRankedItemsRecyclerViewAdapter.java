package com.manouti.itemfinder.item.adapter;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.score.LocationItemScoreEvaluator;
import com.manouti.itemfinder.item.score.ScoreRankedItem;
import com.manouti.itemfinder.item.viewholder.GeoFeaturedItemViewHolder;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.place.Place;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Similar to <code>RankedItemsRecyclerViewAdapter</code> but sorts or "scores" items by their distance to a specified location,
 * in addition to their rating.
 */
public class GeoRankedItemsRecyclerViewAdapter extends FeaturedItemsRecyclerViewAdapter<GeoFeaturedItemViewHolder> {

    private static final String TAG = GeoRankedItemsRecyclerViewAdapter.class.getSimpleName();

    private DatabaseReference mDatabaseReference;
    private GeoFire mPlacesGeoFire;
    private GeoQuery mGeoQuery;
    private LocationItemScoreEvaluator mItemScoreEvaluator;

    private GeoRankedAdapterEventListener<GeoFeaturedItemViewHolder> mAdapterEventListener;

    private Set<ScoreRankedItem> mRankedItems = new HashSet<>();
    private List<ScoreRankedItem> mSortedRankedItemList = new ArrayList<>();

    // Store place event listeners, mapped by place ID
    private Map<String, PlaceValueEventListener> placeListeners = new ConcurrentHashMap<>();
    // Store item event listeners, mapped by item ID
    private Map<String, ItemValueEventListener> globalItemListeners = new ConcurrentHashMap<>();

    public GeoRankedItemsRecyclerViewAdapter(Context context, @NonNull Location preferredLocation, GeoRankedAdapterEventListener<GeoFeaturedItemViewHolder> adapterEventListener) {
        super(context);
        mAdapterEventListener = adapterEventListener;
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mPlacesGeoFire = new GeoFire(mDatabaseReference.child("places-geo"));

        queryGeoLocation(preferredLocation);
    }

    private void queryGeoLocation(Location preferredLocation) {
        mItemScoreEvaluator = new LocationItemScoreEvaluator(preferredLocation);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        float radius = Float.parseFloat(prefs.getString(ItemfinderPreferencesActivity.KEY_LOCATION_RADIUS, mContext.getString(R.string.preferences_default_nearby_fav_item_proximity))); // in kilometers

        mGeoQuery = mPlacesGeoFire.queryAtLocation(
                new GeoLocation(preferredLocation.getLatitude(), preferredLocation.getLongitude()), radius);
        mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(String placeId, GeoLocation location) {
                PlaceValueEventListener placeListener = new PlaceValueEventListener(location);
                placeListeners.put(placeId, placeListener);
                mDatabaseReference.child("places").child(placeId).addValueEventListener(placeListener);
            }

            @Override
            public void onKeyExited(final String placeId) {
                PlaceValueEventListener placeListener = placeListeners.get(placeId);
                if(placeListener != null) {
                    placeListener.removeItemListeners();
                    mDatabaseReference.child("places").child(placeId).removeEventListener(placeListener);
                    placeListeners.remove(placeId);
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
                PlaceValueEventListener placeValueEventListener = placeListeners.get(placeId);
                if(placeValueEventListener != null) {
                    placeValueEventListener.setLocation(location);
                }
            }

            @Override
            public void onGeoQueryReady() {
                mAdapterEventListener.onQueryResultReady();
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
    public GeoFeaturedItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mAdapterEventListener.onCreateItemViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(GeoFeaturedItemViewHolder viewHolder, int position) {
        ScoreRankedItem rankedItem = IterableUtils.get(mSortedRankedItemList, position);
        mAdapterEventListener.onBindItem(viewHolder, rankedItem);
    }

    @Override
    public void clear() {
        mRankedItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mRankedItems.size();
    }

    private void sortItemsAndNotifyDataSetChanged() {
        mSortedRankedItemList = new ArrayList<>(mRankedItems);
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

        for(String placeId : placeListeners.keySet()) {
            PlaceValueEventListener placeValueEventListener = placeListeners.get(placeId);
            placeValueEventListener.removeItemListeners();
            mDatabaseReference.child("places").child(placeId).removeEventListener(placeValueEventListener);
        }
        placeListeners.clear();
        placeListeners = null;

        for(String itemId : globalItemListeners.keySet()) {
            ItemValueEventListener itemValueEventListener = globalItemListeners.get(itemId);
            mDatabaseReference.child("items").child(itemId).removeEventListener(itemValueEventListener);
        }
        globalItemListeners.clear();
        globalItemListeners = null;
    }

    private class PlaceValueEventListener implements ValueEventListener {

        private GeoLocation location;
        private Map<String, ItemValueEventListener> itemListenersForThisPlace = new HashMap<>();
        private String placeId;

        PlaceValueEventListener(GeoLocation location) {
            this.location = location;
        }

        public void setLocation(GeoLocation location) {
            this.location = location;
        }

        @Override
        public void onDataChange(DataSnapshot placeSnapshot) {
            placeId = placeSnapshot.getKey();
            Place place = placeSnapshot.getValue(Place.class);
            if(place != null) {
                place.setId(placeId);
                place.setLatitude(location.latitude);
                place.setLongitude(location.longitude);

                DataSnapshot itemsSnapshot = placeSnapshot.child("items");
                List<String> itemIds = new ArrayList<>();
                for (DataSnapshot child : itemsSnapshot.getChildren()) {
                    String itemId = child.getKey();
                    itemIds.add(itemId);
                    ItemValueEventListener itemValueEventListener = globalItemListeners.get(itemId);
                    if(itemValueEventListener == null) {
                        itemValueEventListener = new ItemValueEventListener(place);
                        globalItemListeners.put(itemId, itemValueEventListener);
                        itemListenersForThisPlace.put(itemId, itemValueEventListener);
                        mDatabaseReference.child("items").child(itemId).addValueEventListener(itemValueEventListener);
                    } else {
                        // Item value event listener exists, so notify it that this place has changed to re-compute its rank.
                        itemListenersForThisPlace.put(itemId, itemValueEventListener);
                        itemValueEventListener.notifyPlaceChanged(place);
                    }
                }

                // Handle removed items for this place by notifying its item listeners
                Iterator<Map.Entry<String, ItemValueEventListener>> iterator = itemListenersForThisPlace.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, ItemValueEventListener> entry = iterator.next();
                    if(!itemIds.contains(entry.getKey())) {
                        entry.getValue().notifyPlaceRemoved(placeId);
                        iterator.remove();
                    }
                }
            } else {
                // Notify all item listeners for this place that the place does not contain any of their items
                Iterator<Map.Entry<String, ItemValueEventListener>> iterator = itemListenersForThisPlace.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, ItemValueEventListener> entry = iterator.next();
                    entry.getValue().notifyPlaceRemoved(placeId);
                    iterator.remove();
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "placeValueEventListener:onCancelled", databaseError.toException());
        }

        private void removeItemListeners() {
            for(ItemValueEventListener itemListener : itemListenersForThisPlace.values()) {
                itemListener.notifyPlaceRemoved(placeId);
            }
            itemListenersForThisPlace.clear();
        }
    }

    private class ItemValueEventListener implements ValueEventListener {

        private Item item;
        private Map<String, Place> placesContainingItem = new ConcurrentHashMap<>();

        ItemValueEventListener(Place place) {
            placesContainingItem.put(place.getId(), place);
        }

        @Override
        public void onDataChange(DataSnapshot itemSnapshot) {
            Item item = itemSnapshot.getValue(Item.class);
            if(item != null) {
                this.item = item;
                String itemId = itemSnapshot.getKey();
                this.item.setId(itemId);
                for(Place place : placesContainingItem.values()) {
                    double itemScore = mItemScoreEvaluator.getItemScore(this.item, place, place.getLatitude(), place.getLongitude());
                    ScoreRankedItem rankedItem = new ScoreRankedItem(this.item, place.getId(), place.getName(), null, null, itemScore);

                    mRankedItems.remove(rankedItem);
                    mRankedItems.add(rankedItem);
                }
            } else {
                filterRankedItems(new Predicate<ScoreRankedItem>() {
                    @Override
                    public boolean evaluate(ScoreRankedItem rankedItem) {
                        return !StringUtils.equals(rankedItem.getItemId(), ItemValueEventListener.this.item.getId());
                    }
                });
            }
            sortItemsAndNotifyDataSetChanged();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w(TAG, "itemValueEventListener:onCancelled", databaseError.toException());
        }

        private void notifyPlaceChanged(Place place) {
            placesContainingItem.put(place.getId(), place);
            if(item != null) {
                double itemScore = mItemScoreEvaluator.getItemScore(item, place, place.getLatitude(), place.getLongitude());
                ScoreRankedItem rankedItem = new ScoreRankedItem(item, place.getId(), place.getName(), null, null, itemScore);

                mRankedItems.remove(rankedItem);
                mRankedItems.add(rankedItem);
                sortItemsAndNotifyDataSetChanged();
            }
        }

        /**
         * Informs this item listener that the specified place no longer has the item.
         * This item listener will be removed once no place has the item.
         * @param placeId the ID of the place
         */
        private void notifyPlaceRemoved(final String placeId) {
            if(StringUtils.isNotBlank(placeId)) {
                CollectionUtils.filter(placesContainingItem.keySet(), new Predicate<String>() {
                    @Override
                    public boolean evaluate(String currentPlaceId) {
                        return !StringUtils.equals(currentPlaceId, placeId);
                    }
                });

                filterRankedItems(new Predicate<ScoreRankedItem>() {
                    @Override
                    public boolean evaluate(ScoreRankedItem rankedItem) {
                        return !(StringUtils.equals(rankedItem.getItemId(), item.getId())
                                && StringUtils.equals(rankedItem.getPlaceId(), placeId));
                    }
                });
                sortItemsAndNotifyDataSetChanged();

                if(placesContainingItem.isEmpty() && item != null) {
                    String itemId = item.getId();
                    mDatabaseReference.child("items").child(itemId).removeEventListener(this);
                    globalItemListeners.remove(itemId);
                }
            }
        }
    }

    /**
     * Filter the ranked items collection by applying a Predicate to each element.
     * If the predicate returns false, remove the element.
     * @param predicate the predicate to be used to filter items
     */
    private void filterRankedItems(Predicate<ScoreRankedItem> predicate) {
        CollectionUtils.filter(mRankedItems, predicate);
        if(mRankedItems.isEmpty()) {
            mAdapterEventListener.onEmptyItems();
        }
    }

}