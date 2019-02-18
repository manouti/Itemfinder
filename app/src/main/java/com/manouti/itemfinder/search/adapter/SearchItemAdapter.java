package com.manouti.itemfinder.search.adapter;

import android.util.Log;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.manouti.itemfinder.model.search.SearchHit;
import com.manouti.itemfinder.model.search.SearchHits;
import com.manouti.itemfinder.model.search.SearchRequest;
import com.manouti.itemfinder.model.search.SearchResult;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;

import java.util.ArrayList;
import java.util.List;


public class SearchItemAdapter extends RecyclerViewTrackSelectionAdapter<SearchItemViewHolder> {

    private static final String TAG = SearchItemAdapter.class.getSimpleName();

    private static final String SEARCH_REQUEST_PATH = "search/request";
    private static final String SEARCH_RESPONSE_PATH = "search/response";
    private static final String INDEX = "firebase";
    private static final String TYPE = "item";

    private RecyclerViewAdapterEventListener<SearchItemViewHolder, SearchResultItem> mAdapterEventListener;
    private DatabaseReference mDatabaseReference;
    private List<SearchResultItem> mSearchResultItems = new ArrayList<>();

    public SearchItemAdapter(RecyclerViewAdapterEventListener<SearchItemViewHolder, SearchResultItem> adapterEventListener) {
        this.mAdapterEventListener = adapterEventListener;
        this.mDatabaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public void clear() {
    }

    @Override
    public SearchItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mAdapterEventListener.onCreateAddedItemViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(SearchItemViewHolder viewHolder, int position) {
        SearchResultItem searchResultItem = mSearchResultItems.get(position);
        mAdapterEventListener.onBindItem(viewHolder, searchResultItem);
    }

    @Override
    public int getItemCount() {
        return mSearchResultItems.size();
    }

    public void doSearch(String query) {
        mSearchResultItems.clear();

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setIndex(INDEX);
        searchRequest.setType(TYPE);
        searchRequest.setQ(query);

        DatabaseReference requestRef = mDatabaseReference.child(SEARCH_REQUEST_PATH);
        String requestKey = requestRef.push().getKey();
        requestRef.child(requestKey).setValue(searchRequest.toMap());

        final DatabaseReference responseRef = mDatabaseReference.child(SEARCH_RESPONSE_PATH).child(requestKey);
        responseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                SearchResult searchResult = dataSnapshot.getValue(SearchResult.class);
                if (searchResult != null) {
                    SearchHits searchHits = searchResult.getHits();
                    if(searchHits != null) {
                        if (searchHits.isError()) {
                            Log.e(TAG, "doSearch:searchResult:error", null);
                            mAdapterEventListener.onError(null);
                        } else {
                            List<SearchHit> hits = searchHits.getHits();
                            if (hits != null) {
                                for (SearchHit hit : hits) {
                                    mSearchResultItems.add(new SearchResultItem(hit.get_id(), hit.get_source().getS(), hit.get_source().getDesc()));
                                }
                            }
                        }

                        dataSnapshot.getRef().removeValue();
                        responseRef.removeEventListener(this);

                        mAdapterEventListener.onItemCountReady(mSearchResultItems.size());
                        notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "doSearch:onCancelled", databaseError.toException());
            }
        });
    }

}
