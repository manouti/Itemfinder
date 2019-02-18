package com.manouti.itemfinder.util.place;

import com.google.android.gms.location.places.Place;
import com.manouti.itemfinder.model.item.ItemType;

import java.util.List;


public class PlaceValidator {
    public static final int VALID_PLACE_FOR_ITEM = 0;
    public static final int INVALID_PLACE = 1;
    public static final int BAD_PLACE_TYPE = 2;
    public static final int BAD_PLACE_TYPE_FOR_ITEM_TYPE = 3;

    public static final int[] VALID_PLACE_TYPES = {
            Place.TYPE_AIRPORT,
            Place.TYPE_AMUSEMENT_PARK,
            Place.TYPE_AQUARIUM,
            Place.TYPE_ART_GALLERY,
            Place.TYPE_BAKERY,
            Place.TYPE_BAR,
            Place.TYPE_BEAUTY_SALON,
            Place.TYPE_BICYCLE_STORE,
            Place.TYPE_BOOK_STORE,
            Place.TYPE_CAFE,
            Place.TYPE_CAR_DEALER,
            Place.TYPE_CAR_RENTAL,
            Place.TYPE_CAR_REPAIR,
            Place.TYPE_CAR_WASH,
            Place.TYPE_CLOTHING_STORE,
            Place.TYPE_CONVENIENCE_STORE,
            Place.TYPE_DEPARTMENT_STORE,
            Place.TYPE_ELECTRICIAN,
            Place.TYPE_ELECTRONICS_STORE,
            Place.TYPE_FLORIST,
            Place.TYPE_FOOD,
            Place.TYPE_FURNITURE_STORE,
            Place.TYPE_GAS_STATION,
            Place.TYPE_GROCERY_OR_SUPERMARKET,
            Place.TYPE_GYM,
            Place.TYPE_HAIR_CARE,
            Place.TYPE_HARDWARE_STORE,
            Place.TYPE_HOME_GOODS_STORE,
            Place.TYPE_JEWELRY_STORE,
            Place.TYPE_LIBRARY,
            Place.TYPE_LIQUOR_STORE,
            Place.TYPE_MEAL_DELIVERY,
            Place.TYPE_MEAL_TAKEAWAY,
            Place.TYPE_MOVIE_RENTAL,
            Place.TYPE_MOVIE_THEATER,
            Place.TYPE_MUSEUM,
            Place.TYPE_PARK,
            Place.TYPE_PET_STORE,
            Place.TYPE_PHARMACY,
            Place.TYPE_RESTAURANT,
            Place.TYPE_SHOE_STORE,
            Place.TYPE_SHOPPING_MALL,
            Place.TYPE_STORE,
            Place.TYPE_SUBWAY_STATION,
            Place.TYPE_TRAIN_STATION,
            Place.TYPE_UNIVERSITY,
            Place.TYPE_ZOO
    };

    private static final int[] VALID_VIN_PLACE_TYPES = {
            Place.TYPE_BICYCLE_STORE,
            Place.TYPE_CAR_DEALER,
            Place.TYPE_CAR_RENTAL,
            Place.TYPE_CAR_REPAIR,
            Place.TYPE_DEPARTMENT_STORE,
            Place.TYPE_STORE
    };

    public static int validatePlace(Place place, ItemType itemType) {
        if(place.getAddress() == null || place.getAddress().toString().isEmpty()) {
            return INVALID_PLACE;
        }

        List<Integer> placeTypes = place.getPlaceTypes();
        if(itemType.equals(ItemType.VIN)) {
            for(int validVinPlaceType : VALID_VIN_PLACE_TYPES) {
                if(placeTypes.contains(validVinPlaceType)) {
                    return VALID_PLACE_FOR_ITEM;
                }
            }
            return BAD_PLACE_TYPE_FOR_ITEM_TYPE;
        }

        for(int validPlaceType : VALID_PLACE_TYPES) {
            if(placeTypes.contains(validPlaceType)) {
                return VALID_PLACE_FOR_ITEM;
            }
        }
        return BAD_PLACE_TYPE;
    }
}
