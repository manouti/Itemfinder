package com.manouti.itemfinder.item.adapter;

import android.content.Context;

import com.manouti.itemfinder.item.viewholder.FeaturedItemViewHolder;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;


public abstract class FeaturedItemsRecyclerViewAdapter<VH extends FeaturedItemViewHolder> extends RecyclerViewTrackSelectionAdapter<VH> {

    protected Context mContext;

    public FeaturedItemsRecyclerViewAdapter(Context context) {
        mContext = context;
    }

}
