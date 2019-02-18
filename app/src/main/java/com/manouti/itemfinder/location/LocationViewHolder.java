package com.manouti.itemfinder.location;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;


public class LocationViewHolder extends RecyclerViewTrackSelectionAdapter.ViewHolder {
    public TextView titleView;
    public TextView descriptionView;
    public ImageView starView;

    public LocationViewHolder(Context context, RecyclerViewTrackSelectionAdapter<? extends RecyclerViewTrackSelectionAdapter.ViewHolder> adapter, View itemView) {
        super(context, adapter, itemView);
    }

}