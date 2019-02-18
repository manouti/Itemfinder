package com.manouti.itemfinder.search;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.manouti.itemfinder.BaseActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.search.adapter.SearchItemAdapter;
import com.manouti.itemfinder.search.adapter.SearchItemViewHolder;
import com.manouti.itemfinder.search.adapter.SearchResultItem;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewAdapterEventListener;

public class SearchableItemActivity extends BaseActivity implements RecyclerViewAdapterEventListener<SearchItemViewHolder, SearchResultItem>  {

    private RecyclerView mRecyclerView;
    private SearchItemAdapter mAdapter;
    private ProgressBar mProgressBar;
    private ViewStub mNoSearchResultViewStub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = (RecyclerView) findViewById(R.id.search_results_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mProgressBar = (ProgressBar) findViewById(R.id.search_progress_bar);
        mNoSearchResultViewStub = (ViewStub) findViewById(R.id.no_result_stub);

        handleIntent(getIntent());
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_searchable;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // Get the intent, verify the action and get the query
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            if(mAdapter == null) {
                mAdapter = new SearchItemAdapter(this);
                mRecyclerView.setAdapter(mAdapter);
            }
            mProgressBar.setVisibility(View.VISIBLE);
            String query = intent.getStringExtra(SearchManager.QUERY);
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    ItemSuggestionProvider.AUTHORITY, ItemSuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);
            mAdapter.doSearch(query);
        }
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemCountReady(long itemCount) {
        mProgressBar.setVisibility(View.GONE);
        if(itemCount == 0) {
            if (mNoSearchResultViewStub.getParent() != null) {
                mNoSearchResultViewStub.inflate();
            } else {
                mNoSearchResultViewStub.setVisibility(View.VISIBLE);
            }
        } else {
            mNoSearchResultViewStub.setVisibility(View.GONE);
        }
    }

    @Override
    public void onError(Exception ex) {
        showSnackbar(R.string.unexpected_error_try_later);
    }

    @Override
    public SearchItemViewHolder onCreateAddedItemViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.search_list_item, parent, false);

        SearchItemViewHolder itemViewHolder = new SearchItemViewHolder(this, mAdapter, view);
        itemViewHolder.itemSummaryView = (TextView) view.findViewById(R.id.item_summary);
        itemViewHolder.itemDescriptionView = (TextView) view.findViewById(R.id.item_description);
        return itemViewHolder;
    }

    @Override
    public void onBindItem(SearchItemViewHolder viewHolder, SearchResultItem dataItem) {
        viewHolder.itemId = dataItem.getItemId();
        viewHolder.itemSummaryView.setText(dataItem.getSummary());
        viewHolder.itemDescriptionView.setText(dataItem.getDescription());
        viewHolder.itemId = dataItem.getItemId();
    }

}
