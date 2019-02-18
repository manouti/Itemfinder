package com.manouti.itemfinder.user.items;


public class UserAddedItem {
    private String mItemId;
    private String mItemSummary;
    private String mPlaceName;
    private String mStatus;

    public UserAddedItem(String itemId, String itemSummary, String placeName, String status) {
        mItemId = itemId;
        this.mItemSummary = itemSummary;
        this.mPlaceName = placeName;
        this.mStatus = status;
    }

    public String getItemId() {
        return mItemId;
    }

    public void setItemId(String itemId) {
        this.mItemId = itemId;
    }

    public String getItemSummary() {
        return mItemSummary;
    }

    public void setItemSummary(String itemSummary) {
        this.mItemSummary = itemSummary;
    }

    public String getPlaceName() {
        return mPlaceName;
    }

    public void setPlaceName(String placeName) {
        this.mPlaceName = placeName;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStstus(String status) {
        this.mStatus = status;
    }
}