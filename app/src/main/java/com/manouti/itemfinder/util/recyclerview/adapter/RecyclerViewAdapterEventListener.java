package com.manouti.itemfinder.util.recyclerview.adapter;

import android.view.ViewGroup;

public interface RecyclerViewAdapterEventListener<VH extends RecyclerViewTrackSelectionAdapter.ViewHolder, DATA> {
    void onItemCountReady(long itemCount);
    void onError(Exception ex);
    VH onCreateAddedItemViewHolder(ViewGroup parent);
    void onBindItem(VH viewHolder, DATA dataItem);
}
