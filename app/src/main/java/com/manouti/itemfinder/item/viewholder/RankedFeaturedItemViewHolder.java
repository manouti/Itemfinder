package com.manouti.itemfinder.item.viewholder;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.item.detail.ItemDetailActivity;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;


public class RankedFeaturedItemViewHolder extends FeaturedItemViewHolder {

    public Item item;
    public TextView itemDescriptionView;

    public RankedFeaturedItemViewHolder(Context context, RecyclerViewTrackSelectionAdapter<RankedFeaturedItemViewHolder> adapter, View itemView) {
        super(context, adapter, itemView);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        Intent itemDetailIntent = new Intent(mContext, ItemDetailActivity.class);
        if(item != null) {
            itemDetailIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_ITEM_INPUT, item);
        } else {
            itemDetailIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_ID_INPUT, itemId);
        }
        mContext.startActivity(itemDetailIntent);
    }

}