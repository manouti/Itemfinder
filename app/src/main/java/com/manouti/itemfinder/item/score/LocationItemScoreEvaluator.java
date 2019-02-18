package com.manouti.itemfinder.item.score;

import android.location.Location;

import com.manouti.itemfinder.model.place.Place;
import com.manouti.itemfinder.model.item.Item;


public class LocationItemScoreEvaluator extends ItemScoreEvaluator {

    private static final int DISTANCE_UNIT = 50;

    private Location mCenterLocation;

    public LocationItemScoreEvaluator(Location centerLocation) {
        this.mCenterLocation = centerLocation;
    }

    public double getItemScore(Item item, Place place, double latitude, double longitude) {
        double baseScore = super.getItemScore(item, place);
        return computeLocationScore(baseScore, latitude, longitude);
    }

    public double getItemScore(double latitude, double longitude) {
        return computeLocationScore(0, latitude, longitude);
    }

    private double computeLocationScore(double currentScore, double latitude, double longitude) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        float distance = mCenterLocation.distanceTo(location);  // in meters
        float distanceUnits = distance / DISTANCE_UNIT;

        return (5 + currentScore) / (1 + distanceUnits);
    }
}
