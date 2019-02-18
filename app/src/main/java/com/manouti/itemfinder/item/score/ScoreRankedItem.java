package com.manouti.itemfinder.item.score;

import android.support.annotation.NonNull;

import com.manouti.itemfinder.item.PlacedItemInfo;
import com.manouti.itemfinder.model.item.Item;


public class ScoreRankedItem {
    private PlacedItemInfo placedItemInfo;
    private double score;

    public ScoreRankedItem(@NonNull Item item, @NonNull String placeId, String placeName, String userId, String userDisplayName, double score) {
        placedItemInfo = new PlacedItemInfo(item, placeId, placeName, userId, userDisplayName);
        this.score = score;
    }

    public PlacedItemInfo getPlacedItemInfo() {
        return placedItemInfo;
    }

    public String getItemId() {
        return placedItemInfo.getItem().getId();
    }

    public String getItemSummary() {
        return placedItemInfo.getItem().getS();
    }

    public String getPlaceId() {
        return placedItemInfo.getPlaceId();
    }

    public String getPlaceName() {
        return placedItemInfo.getPlaceName();
    }

    public String getUserId() {
        return placedItemInfo.getUserId();
    }

    public String getUserDisplayName() {
        return placedItemInfo.getUserDisplayName();
    }

    public double getRating() {
        return placedItemInfo.getItem().getRating();
    }

    public double getScore() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoreRankedItem that = (ScoreRankedItem) o;

        if (!placedItemInfo.getItem().getId().equals(that.getItemId())) return false;
        return placedItemInfo.getPlaceId().equals(that.placedItemInfo.getPlaceId());

    }

    @Override
    public int hashCode() {
        int result = getItemId().hashCode();
        result = 31 * result + placedItemInfo.getPlaceId().hashCode();
        return result;
    }

}
