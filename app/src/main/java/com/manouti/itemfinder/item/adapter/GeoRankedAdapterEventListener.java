package com.manouti.itemfinder.item.adapter;

import android.view.ViewGroup;

import com.google.firebase.database.DatabaseError;
import com.manouti.itemfinder.item.score.ScoreRankedItem;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;


public interface GeoRankedAdapterEventListener<VH extends RecyclerViewTrackSelectionAdapter.ViewHolder> {
    void onQueryResultReady();
    void onQueryError(Throwable error);
    VH onCreateItemViewHolder(ViewGroup parent);
    void onBindItem(VH viewHolder, ScoreRankedItem scoreRankedItem);
    void onEmptyItems();
}
