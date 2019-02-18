package com.manouti.itemfinder.item.adapter;

import android.content.Context;
import android.location.Location;
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
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.item.history.HistoryItem;
import com.manouti.itemfinder.item.history.HistoryManager;
import com.manouti.itemfinder.item.history.HistoryManagerFactory;
import com.manouti.itemfinder.item.score.LocationItemScoreEvaluator;
import com.manouti.itemfinder.item.score.ScoreRankedItem;
import com.manouti.itemfinder.item.viewholder.GeoFeaturedItemViewHolder;
import com.manouti.itemfinder.location.provider.AbstractLocationProvider;
import com.manouti.itemfinder.location.provider.LastLocationProvider;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.place.Place;

import org.apache.commons.collections4.IterableUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class RecommendedItemsRecyclerViewAdapter extends FeaturedItemsRecyclerViewAdapter<GeoFeaturedItemViewHolder> {

    private static final String TAG = "RecommendedItemsAdapter";

    public static final int REQUEST_GOOGLE_PLAY_SERVICES_FOR_LOCATION = 1235;

    private static final int PAGE_SIZE = 15;
    private static final int MAX_ITEMS_PER_PLACE_WHEN_HAVING_NO_CATEGORIES = 5;
    private static final double MAX_ITEM_RATING = 5;
    private static final int MAX_GEO_QUERY_RADIUS = 20;

    private DatabaseReference mDatabaseReference;

    private LastLocationProvider mLastLocationProvider;
    private Location mLastLocation;
    private GeoFire mPlacesGeoFire;
    private GeoQuery mGeoQuery;
    private float mGeoQueryRadius = 10; // in kilometers

    private LinkedHashMap<String, GeoLocation> mPlaces = new LinkedHashMap<>();
    private int mNextPlaceIndex;

    private HistoryManager mHistoryManager;
    private LinkedHashMap<String, Integer> weightedCategories;
    private Iterator<Map.Entry<String, Integer>> mNextCategory;

    private Map<String, Map<String, Double>> mLastRatingLimitPerCategoryPerPlace = new HashMap<>();

    private GeoRankedAdapterEventListener<GeoFeaturedItemViewHolder> mAdapterEventListener;

    private Map<String, Place> mPagedItems = new HashMap<>();

    private List<ScoreRankedItem> mItems = new LinkedList<>();

    private LocationItemScoreEvaluator mItemScoreEvaluator;

    public RecommendedItemsRecyclerViewAdapter(Context context, GeoRankedAdapterEventListener<GeoFeaturedItemViewHolder> adapterEventListener) {
        super(context);
        mHistoryManager = HistoryManagerFactory.makeHistoryManager(context);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mAdapterEventListener = adapterEventListener;

        mPlacesGeoFire = new GeoFire(mDatabaseReference.child("places-geo"));
        queryRecommendedItems();
    }

    public void loadMoreItems() {
        loadRecommendedItemsFromHistory();
    }

    @Override
    public GeoFeaturedItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mAdapterEventListener.onCreateItemViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(GeoFeaturedItemViewHolder viewHolder, int position) {
        ScoreRankedItem rankedItem = mItems.get(position);
        mAdapterEventListener.onBindItem(viewHolder, rankedItem);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public void cleanUpListener() {
        if(mLastLocationProvider != null) {
            mLastLocationProvider.stop();
        }
        if(mGeoQuery != null) {
            mGeoQuery.removeAllListeners();
        }
    }

    /**
     * This method starts the procedure of querying for recommended items.
     */
    private void queryRecommendedItems() {
        // Start by getting the user's last known location.
        mLastLocationProvider = new LastLocationProvider(mContext, REQUEST_GOOGLE_PLAY_SERVICES_FOR_LOCATION, new AbstractLocationProvider.LocationResultCallback() {

            @Override
            public void onLocationReceived(@NonNull Location location) {
                mLastLocation = location;
                mItemScoreEvaluator = new LocationItemScoreEvaluator(location);
                queryLocations();
            }

            @Override
            public void onLocationNotAvailable(String failureMessage) {
                mAdapterEventListener.onQueryError(null);
            }

            @Override
            public void onLocationSettingsNotAvailable() {
            }
        });
        mLastLocationProvider.start();
    }

    private void queryLocations() {
        // Query all places nearby user's location within a certain radius.
        mGeoQuery = mPlacesGeoFire.queryAtLocation(
                new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), mGeoQueryRadius);
        mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyEntered(String placeId, GeoLocation location) {
                mPlaces.put(placeId, location);
            }

            @Override
            public void onKeyExited(final String placeId) {
            }

            @Override
            public void onKeyMoved(final String placeId, final GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
                // We now have all nearby places collected, now we fetch the history of items the user interacted with.
                mGeoQuery.removeAllListeners();
                getHistoryItems();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Throwable throwable = error.toException();
                FirebaseCrash.report(throwable);
                mAdapterEventListener.onQueryError(throwable);
            }
        });
    }

    private void getHistoryItems() {
        mHistoryManager.getWeightSortedHistoryItems(new HistoryManager.CompletionListener() {
            @Override
            public void onSuccess(@NonNull List<HistoryItem> historyItems) {
                weightedCategories = getWeightedCategories(historyItems);
                loadRecommendedItemsFromHistory();
            }

            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "queryRecommendedItems:getHistoryItems:onError", error);
                FirebaseCrash.report(error);
                mAdapterEventListener.onQueryError(error);
            }
        });
    }

    private LinkedHashMap<String, Integer> getWeightedCategories(List<HistoryItem> historyItems) {
        LinkedHashMap<String, Integer> weightedCategories = new LinkedHashMap<>();
        for(HistoryItem historyItem : historyItems) {
            for(String category : historyItem.getCategories()) {
                if(!weightedCategories.containsKey(category)) {
                    weightedCategories.put(category, historyItem.getWeight());
                }
            }
        }
        return weightedCategories;
    }

    private void loadRecommendedItemsFromHistory() {
        visitNextPlace();
    }

    private void visitNextPlace() {
        final String nextPlace = nextPlace();
        if(nextPlace != null) {
            mDatabaseReference.child("places").child(nextPlace).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Place place = dataSnapshot.getValue(Place.class);
                        place.setId(nextPlace);
                        if(!weightedCategories.isEmpty()) {
                            visitNextCategory(place);
                        } else {
                            addPlaceListener(place);
                        }
                    } else {
                        Log.w(TAG, "Place with ID " + nextPlace + " does not exist");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    DatabaseException databaseException = databaseError.toException();
                    Log.w(TAG, "getItem:onCancelled", databaseException);
                    FirebaseCrash.report(databaseException);
                }
            });
        }
    }

    private String nextPlace() {
        if(mNextPlaceIndex >= mPlaces.size()) {
            // We expand the geo query search area because all of the places have been visited so far
            expandGeoQuerySearchArea();
            return null;
        }
        return IterableUtils.get(mPlaces.keySet(), mNextPlaceIndex++);
    }

    private void visitNextCategory(Place place) {
        Map.Entry<String, Integer> nextCategory = nextCategory();
        if(nextCategory != null) {
            addPlaceCategoryListener(place, nextCategory.getKey(), nextCategory.getValue());
        } else {
            // Visit next place because all categories have been visited for the current place.
            visitNextPlace();
        }
    }

    private Map.Entry<String, Integer> nextCategory() {
        if(mNextCategory == null) {
            mNextCategory = weightedCategories.entrySet().iterator();
        } else if(!mNextCategory.hasNext()) {
            return null;
        }
        return mNextCategory.next();
    }

    private void addPlaceCategoryListener(final Place place, final String category, int categoryWeight) {
        final String placeId = place.getId();
        double lastRatingLimitForThisPlaceAndCategory = getLastRatingLimit(placeId, category);
        mDatabaseReference.child("places")
                .child(placeId)
                .child("itemsCat")
                .child(category)
                .orderByValue()  // order by rating, because the rating is the value of each child
                        // node corresponding to an item
                .endAt(lastRatingLimitForThisPlaceAndCategory)
                .limitToLast(categoryWeight)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int index = 0;
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            String itemId = item.getKey();
                            mPagedItems.put(itemId, place);

                            if (mPagedItems.size() >= PAGE_SIZE) {
                                double lastItemRating = item.getValue(Double.class);
                                updateLastRatingLimit(placeId, category, lastItemRating);
                                appendPagedItems();
                                break;
                            } else if (++index == dataSnapshot.getChildrenCount()) { // last item
                                double lastItemRating = item.getValue(Double.class);
                                updateLastRatingLimit(placeId, category, lastItemRating);
                                visitNextCategory(place);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        DatabaseException throwable = databaseError.toException();
                        Log.w(TAG, "topRatedItemsByPlaceAndCategory:onCancelled", throwable);
                        FirebaseCrash.report(throwable);
                        mAdapterEventListener.onQueryError(throwable);
                    }
                });
    }

    private void addPlaceListener(final Place place) {
        final String placeId = place.getId();
        mDatabaseReference.child("places")
                .child(placeId)
                .child("items")
                .orderByChild("rating")
                .endAt(MAX_ITEM_RATING)
                .limitToLast(MAX_ITEMS_PER_PLACE_WHEN_HAVING_NO_CATEGORIES)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int index = 0;
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            String itemId = item.getKey();
                            mPagedItems.put(itemId, place);

                            if (mPagedItems.size() >= PAGE_SIZE) {
                                appendPagedItems();
                                break;
                            } else if (++index == dataSnapshot.getChildrenCount()) { // last item
                                visitNextCategory(place);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        DatabaseException throwable = databaseError.toException();
                        Log.w(TAG, "topRatedItemsByPlaceAndCategory:onCancelled", throwable);
                        FirebaseCrash.report(throwable);
                        mAdapterEventListener.onQueryError(throwable);
                    }
                });
    }

    private void appendPagedItems() {
        List<ScoreRankedItem> pagedItems = new ArrayList<>();
        addNextPagedItem(pagedItems);
    }

    private void addNextPagedItem(final List<ScoreRankedItem> pagedItems) {
        Iterator<String> iterator = mPagedItems.keySet().iterator();
        if(iterator.hasNext()) {
            final String nextItemId = iterator.next();
            iterator.remove();

            mDatabaseReference.child("items").child(nextItemId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Item item = dataSnapshot.getValue(Item.class);
                        item.setId(nextItemId);
                        Place place = mPagedItems.get(nextItemId);
                        double score = computeItemScore(item, place);
                        ScoreRankedItem rankedItem = new ScoreRankedItem(item, place.getId(), place.getName(), null, null, score);
                        pagedItems.add(rankedItem);
                    } else {
                        Log.w(TAG, "Item with ID " + nextItemId + " does not exist");
                    }
                    addNextPagedItem(pagedItems);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "getItem:onCancelled", databaseError.toException());
                }
            });
        } else {
            sortPagedItems(pagedItems);
            addToTotalItemsAndNotify(pagedItems);
        }
    }

    private double computeItemScore(Item item, Place place) {
        GeoLocation geoLocation = mPlaces.get(place.getId());
        double score = mItemScoreEvaluator.getItemScore(item, place, geoLocation.latitude, geoLocation.longitude);
        List<String> categories = item.getCategories();
        int totalWeight = 0;
        for(String category : categories) {
            totalWeight += weightedCategories.get(category);
        }
        return score * totalWeight;
    }

    private void sortPagedItems(List<ScoreRankedItem> pagedItems) {
        Collections.sort(pagedItems, new Comparator<ScoreRankedItem>() {
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
    }

    private void addToTotalItemsAndNotify(List<ScoreRankedItem> pagedItems) {
        int previousSize = mItems.size();
        mItems.addAll(pagedItems);
        for(int i = previousSize; i < mItems.size(); i++) {
            notifyItemInserted(i);
        }
    }

    private void expandGeoQuerySearchArea() {
        if(mGeoQueryRadius < MAX_GEO_QUERY_RADIUS) {
            mGeoQueryRadius += 5; // increase search radius in kilometers
            mGeoQuery = mPlacesGeoFire.queryAtLocation(
                    new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), mGeoQueryRadius);
            mGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

                @Override
                public void onKeyEntered(String placeId, GeoLocation location) {
                    // Places already checked in the first query won't be added, and since we are keeping
                    // track of the last place visited, a place will be processed only once.
                    if(!mPlaces.containsKey(placeId)) {
                        mPlaces.put(placeId, location);
                    }
                }

                @Override
                public void onKeyExited(final String placeId) {
                }

                @Override
                public void onKeyMoved(final String placeId, final GeoLocation location) {
                }

                @Override
                public void onGeoQueryReady() {
                    // We now have all additional nearby places with the increase search area,
                    // so we call the method that will visit them.
                    mGeoQuery.removeAllListeners();
                    loadRecommendedItemsFromHistory();
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    Throwable throwable = error.toException();
                    FirebaseCrash.report(throwable);
                    mAdapterEventListener.onQueryError(throwable);
                }
            });
        }
    }

    private double getLastRatingLimit(String placeId, String category) {
        Map<String, Double> lastRatingLimitPerCategory = mLastRatingLimitPerCategoryPerPlace.get(placeId);
        if(lastRatingLimitPerCategory != null) {
            Double lastRatingLimit = lastRatingLimitPerCategory.get(category);
            if(lastRatingLimit != null) {
                return lastRatingLimit;
            }
        }
        return MAX_ITEM_RATING;
    }

    private void updateLastRatingLimit(String placeId, String category, double lastRating) {
        Map<String, Double> lastRatingLimitPerCategory = mLastRatingLimitPerCategoryPerPlace.get(placeId);
        if(lastRatingLimitPerCategory == null) {
            lastRatingLimitPerCategory = new HashMap<>();
            mLastRatingLimitPerCategoryPerPlace.put(placeId, lastRatingLimitPerCategory);
        }
        lastRatingLimitPerCategory.put(category, lastRating);
    }

}
