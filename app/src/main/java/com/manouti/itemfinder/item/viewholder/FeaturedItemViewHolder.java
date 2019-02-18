package com.manouti.itemfinder.item.viewholder;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;


public class FeaturedItemViewHolder extends RecyclerViewTrackSelectionAdapter.ViewHolder {

    public String itemId;
    public ImageView itemImageView;
    public TextView itemSummaryView;
    public RatingBar itemRatingBar;

    public FeaturedItemViewHolder(Context context, RecyclerViewTrackSelectionAdapter<? extends RecyclerViewTrackSelectionAdapter.ViewHolder> adapter, View itemView) {
        super(context, adapter, itemView);
    }
}
