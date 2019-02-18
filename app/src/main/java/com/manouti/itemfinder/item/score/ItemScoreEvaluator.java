package com.manouti.itemfinder.item.score;

import com.manouti.itemfinder.model.place.Place;
import com.manouti.itemfinder.model.item.Item;


public class ItemScoreEvaluator {

    public double getItemScore(Item item, Place place) {
        double itemRating = item.getRating() * item.getVoteCount();
        double placeRating = place.getRating() * place.getVoteCount();

        return itemRating + placeRating;
    }

}
