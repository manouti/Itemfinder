package com.manouti.itemfinder.model.place;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Models an item located in a place. It could be still proposed, rejected or approved.
 */
public class PlaceItem {

    public enum ItemAcceptanceStatus {
        PROPOSED,
        APPROVED,
        REJECTED;
    }

    @Exclude
    private String mItemId;
    @Exclude
    private String mItemSummary;
    @Exclude
    private String mItemDesc;
    private String mUid;
    @Exclude
    private long mUpvote;
    @Exclude
    private long mDownvote;
    private double mRating = -1;

    public PlaceItem() {
    }

    public PlaceItem(String itemId, String itemSummary, String itemDesc, String uid, long upvote, long downvote) {
        this.mItemId = itemId;
        this.mItemSummary = itemSummary;
        this.mItemDesc = itemDesc;
        this.mUid = uid;
        this.mUpvote = upvote;
        this.mDownvote = downvote;
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

    public String getItemDesc() {
        return mItemDesc;
    }

    public void setItemDesc(String itemDesc) {
        this.mItemDesc = itemDesc;
    }

    public String getUid() {
        return mUid;
    }

    public void setUid(String uid) {
        this.mUid = uid;
    }

    public long getUpvote() {
        return mUpvote;
    }

    public void setUpvote(long upvote) {
        this.mUpvote = upvote;
    }

    public long getDownvote() {
        return mDownvote;
    }

    public void setDownvote(long downvote) {
        this.mDownvote = downvote;
    }

    public String getUd() {
        return mUpvote + "/" + mDownvote;
    }

    public void setUd(String upvoteDownvote) {
        int indexOfSlash = upvoteDownvote.indexOf('/');
        this.mUpvote = Long.valueOf(upvoteDownvote.substring(0, indexOfSlash));
        this.mDownvote = Long.valueOf(upvoteDownvote.substring(indexOfSlash + 1));
    }

    public double getRating() {
        return mRating;
    }

    public void setRating(double rating) {
        this.mRating = rating;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("uid", mUid);
        result.put("ud", getUd());
        result.put("rating", mRating);

        return result;
    }
}
