package com.manouti.itemfinder.user.rewards;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;


public class RewardViewHolder extends RecyclerViewTrackSelectionAdapter.ViewHolder {
    public TextView itemSummaryTextView;
    public TextView placeTextView;
    public TextView newRepTextView;
    public String moment;

    public RewardViewHolder(Context context, RecyclerViewTrackSelectionAdapter<? extends RecyclerViewTrackSelectionAdapter.ViewHolder> adapter, View itemView) {
        super(context, adapter, itemView);
    }

}