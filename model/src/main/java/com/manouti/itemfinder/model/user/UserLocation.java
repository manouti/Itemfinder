package com.manouti.itemfinder.model.user;

import android.location.Location;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

import java.util.HashMap;
import java.util.Map;


public class UserLocation {
    private String mPlaceId;
    private String mPlaceTitle;
    private String mPlaceDescription;
    @PropertyName("0")
    public double mLat;
    @PropertyName("1")
    public double mLng;
    private boolean mFavorite;

    public UserLocation() {
    }

    public UserLocation(String placeId, String placeTitle, String placeDescription, double lat, double lng, boolean favorite) {
        this.mPlaceId = placeId;
        this.mPlaceTitle = placeTitle;
        this.mPlaceDescription = placeDescription;
        this.mLat = lat;
        this.mLng = lng;
        this.mFavorite = favorite;
    }

    public String getPlaceId() {
        return mPlaceId;
    }

    public void setPlaceId(String mPlaceId) {
        this.mPlaceId = mPlaceId;
    }

    public String getPlaceTitle() {
        return mPlaceTitle;
    }

    public void setPlaceTitle(String mPlaceTitle) {
        this.mPlaceTitle = mPlaceTitle;
    }

    public String getPlaceDescription() {
        return mPlaceDescription;
    }

    public void setPlaceDescription(String mPlaceDescription) {
        this.mPlaceDescription = mPlaceDescription;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public void setFavorite(boolean mFavorite) {
        this.mFavorite = mFavorite;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("placeTitle", mPlaceTitle);
        result.put("placeDescription", mPlaceDescription);
        result.put("0", mLat);
        result.put("1", mLng);
        result.put("favorite", mFavorite);

        return result;
    }

    @Exclude
    public Location toLocation() {
        Location location = new Location("");
        location.setLatitude(mLat);
        location.setLongitude(mLng);
        return location;
    }
}
