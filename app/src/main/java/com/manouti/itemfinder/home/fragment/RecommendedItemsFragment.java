package com.manouti.itemfinder.home.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.PlacedItemInfo;
import com.manouti.itemfinder.item.adapter.GeoRankedAdapterEventListener;
import com.manouti.itemfinder.item.adapter.RecommendedItemsRecyclerViewAdapter;
import com.manouti.itemfinder.item.score.ScoreRankedItem;
import com.manouti.itemfinder.item.viewholder.GeoFeaturedItemViewHolder;
import com.manouti.itemfinder.util.NetworkUtils;
import com.manouti.itemfinder.util.broadcast.ConnectivityBroadcastReceiver;
import com.manouti.itemfinder.util.firebase.FirebaseImageLoader;
import com.manouti.itemfinder.util.firebase.FirebaseStorageUtil;
import com.manouti.itemfinder.util.ui.SwipeUpOnlyRefreshLayout;


public class RecommendedItemsFragment extends BaseHomeFragment implements SwipeRefreshLayout.OnRefreshListener,
                                                                          GeoRankedAdapterEventListener<GeoFeaturedItemViewHolder>,
                                                                          ConnectivityBroadcastReceiver.ConnectivityBroadcastHandler {

    private static final String TAG = RecommendedItemsFragment.class.getSimpleName();

    private static final int MAX_ITEM_COUNT = 100;

    private RecyclerView mRecommendedItemsRecyclerView;
    private RecyclerView.OnScrollListener mRecyclerViewScrollListener;
    private ProgressBar mProgressBar;
    private boolean mIsLoading;
    private SwipeUpOnlyRefreshLayout mSwipeRefreshLayout;
    private TextView mNoConnectionTextView;

    private RecommendedItemsRecyclerViewAdapter mRecommendedItemsAdapter;

    private ConnectivityBroadcastReceiver mConnectivityBroadcastReceiver;
    private boolean mConnectivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated properly.
        View rootView = inflater.inflate(R.layout.fragment_recommended_items, container, false);

        mRecommendedItemsRecyclerView = (RecyclerView) rootView.findViewById(R.id.recommended_items_recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecommendedItemsRecyclerView.setLayoutManager(layoutManager);
        mRecommendedItemsRecyclerView.setNestedScrollingEnabled(false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.nearby_items_progress_bar);
        mNoConnectionTextView = (TextView) rootView.findViewById(R.id.no_connection_text_view);

        mSwipeRefreshLayout = (SwipeUpOnlyRefreshLayout) rootView.findViewById(R.id.swiperefresh_main);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mRecyclerViewScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if(mRecommendedItemsRecyclerView.getChildCount() < MAX_ITEM_COUNT) {
                    super.onScrolled(recyclerView, dx, dy);
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!mIsLoading) {
                        boolean condition = (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0;
                        Log.d(TAG, "onScrolled: condition=" + condition);
                        if (condition) {
                            if (mRecommendedItemsAdapter != null) {
                                mIsLoading = true;
                                mRecommendedItemsAdapter.loadMoreItems();
                            }
                        }
                    }
                }
            }
        };

        return rootView;
    }

    @Override
    public void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();

        mConnectivity = NetworkUtils.isNetworkAvailable(mMainActivity);
        mConnectivityBroadcastReceiver = new ConnectivityBroadcastReceiver(this);
        mMainActivity.registerReceiver(mConnectivityBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        if(!mConnectivity) {
            mNoConnectionTextView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
        } else {
            loadRecommendedItems();
        }
    }

    @Override
    public void onStop() {
        mMainActivity.unregisterReceiver(mConnectivityBroadcastReceiver);
        mConnectivityBroadcastReceiver = null;
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case RecommendedItemsRecyclerViewAdapter.REQUEST_GOOGLE_PLAY_SERVICES_FOR_LOCATION:
                // The user has resolved the error that occurred connecting to Google Play Services
                if (resultCode == Activity.RESULT_OK) {
                    loadRecommendedItems();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void handleRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        loadRecommendedItems();
    }

    @Override
    public void onRefresh() {
        loadRecommendedItems();
    }

    private void loadRecommendedItems() {
        if(mRecommendedItemsAdapter != null) {
            mRecommendedItemsAdapter.cleanUpListener();
        }
        mRecommendedItemsAdapter = new RecommendedItemsRecyclerViewAdapter(mMainActivity, this);
        mIsLoading = true;
        mRecommendedItemsRecyclerView.setAdapter(mRecommendedItemsAdapter);
        mRecommendedItemsRecyclerView.addOnScrollListener(mRecyclerViewScrollListener);
    }

    @Override
    public void onQueryResultReady() {
        mIsLoading = false;
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onQueryError(Throwable error) {
        mIsLoading = false;
        showSnackbar(R.string.unexpected_error_try_later);
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public GeoFeaturedItemViewHolder onCreateItemViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mMainActivity);
        View view = inflater.inflate(R.layout.featured_recommended_item, parent, false);

        final GeoFeaturedItemViewHolder featuredItemViewHolder = new GeoFeaturedItemViewHolder(mMainActivity, mRecommendedItemsAdapter, view);
        featuredItemViewHolder.itemImageView = (ImageView) view.findViewById(R.id.item_image_view);
        featuredItemViewHolder.itemSummaryView = (TextView) view.findViewById(R.id.item_summary);
        featuredItemViewHolder.itemPlaceNameView = (TextView) view.findViewById(R.id.item_place_name);
        featuredItemViewHolder.itemRatingBar = (RatingBar) view.findViewById(R.id.item_rating_bar_indicator);

        return featuredItemViewHolder;
    }

    @Override
    public void onBindItem(final GeoFeaturedItemViewHolder itemViewHolder, ScoreRankedItem scoreRankedItem) {
        PlacedItemInfo placedItemInfo = scoreRankedItem.getPlacedItemInfo();
        String itemId = placedItemInfo.getItem().getId();
        itemViewHolder.placedItemInfo = placedItemInfo;
        itemViewHolder.itemId = itemId;
        itemViewHolder.itemSummaryView.setText(scoreRankedItem.getItemSummary());
        itemViewHolder.itemRatingBar.setRating((float) scoreRankedItem.getRating());

        StorageReference photoStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseStorageUtil.STORAGE_URL)
                .child(FirebaseStorageUtil.IMAGES_PATH)
                .child(FirebaseStorageUtil.ITEMS_PATH)
                .child(itemId);
        Glide.with(this)
                .using(new FirebaseImageLoader())
                .load(photoStorageReference)
                .listener(new RequestListener<StorageReference, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, StorageReference model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, StorageReference model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        itemViewHolder.itemImageView.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(itemViewHolder.itemImageView);
    }

    @Override
    public void onEmptyItems() {
    }

    @Override
    public void handleConnectivityBroadcast(Context context, Intent intent) {
        if(NetworkUtils.isIntentActionConnectivityEstablished(context, intent) && !mConnectivity) {
            mConnectivity = true;
            loadRecommendedItems();
        }
    }
}
