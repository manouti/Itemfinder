package com.manouti.itemfinder.model.place;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Models a place where an item could exist. Contains basic information retrieved from a Google place.
 */
@IgnoreExtraProperties
public class Place {
    @Exclude
    private String mId;  // Key, same as Google place ID
    @Exclude
    private double mLatitude;
    @Exclude
    private double mLongitude;
    private String mName;
    private String mAddress;
    private String mPhoneNumber;
    private String mWebsiteUri;
    private double mRating = -1;
    private int mVoteCount;

    public Place() {
    }

    public Place(String id, CharSequence placeName, CharSequence placeAddress,
                 CharSequence phoneNumber, String websiteUri) {
        this.mId = id;
        this.mName = placeName.toString();
        this.mAddress = placeAddress.toString();
        this.mPhoneNumber = phoneNumber.toString();
        this.mWebsiteUri = websiteUri;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getAddr() {
        return mAddress;
    }

    public void setAddr(String mAddress) {
        this.mAddress = mAddress;
    }

    public String getPhone() {
        return mPhoneNumber;
    }

    public void setPhone(String mPhoneNumber) {
        this.mPhoneNumber = mPhoneNumber;
    }

    public String getWebsite() {
        return mWebsiteUri;
    }

    public void setWebsite(String mWebsiteUri) {
        this.mWebsiteUri = mWebsiteUri;
    }

    public double getRating() {
        return mRating;
    }

    public void setRating(double rating) {
        this.mRating = rating;
    }

    public int getVoteCount() {
        return mVoteCount;
    }

    public void setVoteCount(int voteCount) {
        this.mVoteCount = voteCount;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", mName);
        result.put("addr", mAddress);
        if(StringUtils.isNotBlank(mPhoneNumber)) {
            result.put("phone", mPhoneNumber);
        }
        if(StringUtils.isNotBlank(mWebsiteUri)) {
            result.put("website", mWebsiteUri);
        }
        result.put("rating", mRating);
        result.put("voteCount", mVoteCount);

        return result;
    }
}
