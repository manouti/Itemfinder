package com.manouti.itemfinder.home.fragment;

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
import com.manouti.itemfinder.item.adapter.RankedAdapterEventListener;
import com.manouti.itemfinder.item.viewholder.RankedFeaturedItemViewHolder;
import com.manouti.itemfinder.item.adapter.RankedItemsRecyclerViewAdapter;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.util.firebase.FirebaseImageLoader;
import com.manouti.itemfinder.util.firebase.FirebaseStorageUtil;
import com.manouti.itemfinder.util.ui.SwipeUpOnlyRefreshLayout;


public class TopRatedItemsFragment extends BaseHomeFragment implements SwipeRefreshLayout.OnRefreshListener,
                                                                       RankedAdapterEventListener<RankedFeaturedItemViewHolder> {

    private static final String TAG = TopRatedItemsFragment.class.getSimpleName();

    private RecyclerView mTopRatedItemsRecyclerView;
    private RecyclerView.OnScrollListener mRecyclerViewScrollListener;
    private ProgressBar mProgressBar;
    private boolean mIsLoading;
    private SwipeUpOnlyRefreshLayout mSwipeRefreshLayout;

    private RankedItemsRecyclerViewAdapter mTopRatedItemsAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated properly.
        View rootView = inflater.inflate(R.layout.fragment_top_rated_items, container, false);

        mTopRatedItemsRecyclerView = (RecyclerView) rootView.findViewById(R.id.top_rated_items_recycler_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mTopRatedItemsRecyclerView.setLayoutManager(layoutManager);
        mTopRatedItemsRecyclerView.setNestedScrollingEnabled(false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.top_rated_items_progress_bar);

        mSwipeRefreshLayout = (SwipeUpOnlyRefreshLayout) rootView.findViewById(R.id.swiperefresh_main);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mRecyclerViewScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!mIsLoading) {
                    boolean condition = (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0;
                    Log.d(TAG, "onScrolled: condition=" + condition);
                    if (condition) {
                        if(mTopRatedItemsAdapter != null) {
                            mIsLoading = true;
                            mTopRatedItemsAdapter.loadMoreItems();
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
        resetAdapter();
    }

    @Override
    public void onStop() {
        if(mTopRatedItemsAdapter != null) {
            mTopRatedItemsAdapter.cleanUpListener();
            mTopRatedItemsAdapter = null;
        }
        super.onStop();
    }

    private void resetAdapter() {
        if(mTopRatedItemsAdapter != null) {
            mTopRatedItemsAdapter.cleanUpListener();
        }
        mTopRatedItemsAdapter = new RankedItemsRecyclerViewAdapter(mMainActivity, this);
        mIsLoading = true;
        mTopRatedItemsRecyclerView.setAdapter(mTopRatedItemsAdapter);
        mTopRatedItemsRecyclerView.addOnScrollListener(mRecyclerViewScrollListener);
    }

    @Override
    public void handleRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        resetAdapter();
    }

    @Override
    public void onRefresh() {
        Log.i(TAG, "onRefresh");
        resetAdapter();
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
        showSnackbar(R.string.error_top_rated_query);
        mProgressBar.setVisibility(View.GONE);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public RankedFeaturedItemViewHolder onCreateItemViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mMainActivity);
        View view = inflater.inflate(R.layout.featured_ranked_item, parent, false);

        final RankedFeaturedItemViewHolder featuredItemViewHolder = new RankedFeaturedItemViewHolder(mMainActivity, mTopRatedItemsAdapter, view);
        featuredItemViewHolder.itemImageView = (ImageView) view.findViewById(R.id.item_image_view);
        featuredItemViewHolder.itemSummaryView = (TextView) view.findViewById(R.id.item_summary);
        featuredItemViewHolder.itemDescriptionView = (TextView) view.findViewById(R.id.item_description);
        featuredItemViewHolder.itemRatingBar = (RatingBar) view.findViewById(R.id.item_rating_bar_indicator);

        return featuredItemViewHolder;
    }

    @Override
    public void onBindItem(final RankedFeaturedItemViewHolder itemViewHolder, Item rankedItem) {
        String itemId = rankedItem.getId();
        itemViewHolder.item = rankedItem;
        itemViewHolder.itemId = itemId;
        itemViewHolder.itemSummaryView.setText(rankedItem.getS());
        itemViewHolder.itemDescriptionView.setText(rankedItem.getDesc());
        itemViewHolder.itemRatingBar.setRating((float) rankedItem.getRating());

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

}
