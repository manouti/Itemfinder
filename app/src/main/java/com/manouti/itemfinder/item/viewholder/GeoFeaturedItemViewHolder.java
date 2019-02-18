package com.manouti.itemfinder.item.viewholder;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.item.PlacedItemInfo;
import com.manouti.itemfinder.item.detail.ItemDetailActivity;
import com.manouti.itemfinder.map.MapsActivity;
import com.manouti.itemfinder.util.recyclerview.adapter.RecyclerViewTrackSelectionAdapter;


public class GeoFeaturedItemViewHolder extends FeaturedItemViewHolder {

    public PlacedItemInfo placedItemInfo;
    public TextView itemPlaceNameView;

    public GeoFeaturedItemViewHolder(Context context, RecyclerViewTrackSelectionAdapter<GeoFeaturedItemViewHolder> adapter, View itemView) {
        super(context, adapter, itemView);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        Intent itemDetailIntent = new Intent(mContext, ItemDetailActivity.class);
        itemDetailIntent.putExtra(Intents.ITEM_DETAIL_ACTIVITY_PLACED_INPUT, placedItemInfo);
        mContext.startActivity(itemDetailIntent);
    }

    public void showOnMap() {
        Intent mapIntent = new Intent(mContext, MapsActivity.class);
        mapIntent.putExtra(Intents.MAP_PLACED_ITEM_INPUT, placedItemInfo);
        mContext.startActivity(mapIntent);
    }
}