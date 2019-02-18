package com.manouti.itemfinder.item.viewholder;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.item.detail.ItemDetailActivity;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;


public class ItemViewHolder extends RecyclerViewTrackSelectionAdapter.ViewHolder {
    public String itemId;
    public TextView itemSummaryView;
    public TextView itemPlaceNameView;
    public TextView userView;

    public ItemViewHolder(Context context, RecyclerViewTrackSelectionAdapter<? extends RecyclerViewTrackSelectionAdapter.ViewHolder> adapter, View itemView) {
        super(context, adapter, itemView);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        Intent itemDetailIntent = new Intent(mContext, ItemDetailActivity.class);
        itemDetailIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_ID_INPUT, itemId);
        mContext.startActivity(itemDetailIntent);
    }
}