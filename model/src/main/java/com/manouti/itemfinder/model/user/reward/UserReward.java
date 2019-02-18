package com.manouti.itemfinder.model.user.reward;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

import java.util.HashMap;
import java.util.Map;


public class UserReward {

    private String mMoment;
    private String mItemSummary;
    private String mPlaceName;
    private long mNewRep;

    public UserReward() {
    }

    @PropertyName("m")
    public String getMoment() {
        return mMoment;
    }

    @PropertyName("m")
    public void setMoment(String mMoment) {
        this.mMoment = mMoment;
    }

    @PropertyName("i")
    public String getItemSummary() {
        return mItemSummary;
    }

    @PropertyName("i")
    public void setItemSummary(String itemSummary) {
        this.mItemSummary = itemSummary;
    }

    @PropertyName("p")
    public String getPlaceName() {
        return mPlaceName;
    }

    @PropertyName("p")
    public void setPlaceName(String placeName) {
        this.mPlaceName = placeName;
    }

    @PropertyName("nr")
    public long getNewRep() {
        return mNewRep;
    }

    @PropertyName("nr")
    public void setNewRep(long newRep) {
        this.mNewRep = newRep;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("m", mMoment);
        result.put("i", mItemSummary);
        result.put("p", mPlaceName);

        if(mNewRep > 0) {
            result.put("nr", mNewRep);
        }

        return result;
    }

}
