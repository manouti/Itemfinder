package com.manouti.itemfinder.item;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.manouti.itemfinder.model.item.Item;


public class PlacedItemInfo implements Parcelable {
    private Item item;
    private String placeId;
    private String placeName;
    private String userId;
    private String userDisplayName;

    public PlacedItemInfo(@NonNull Item item, @NonNull String placeId, String placeName, String userId, String userDisplayName) {
        this.item = item;
        this.placeId = placeId;
        this.placeName = placeName;
        this.userId = userId;
        this.userDisplayName = userDisplayName;
    }

    private PlacedItemInfo(Parcel in) {
        item = new Item(in);
        placeId = in.readString();
        placeName = in.readString();
        userId = in.readString();
        userDisplayName = in.readString();
    }

    public Item getItem() {
        return item;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        item.writeToParcel(dest, flags);
        dest.writeString(placeId);
        dest.writeString(placeName);
        dest.writeString(userId);
        dest.writeString(userDisplayName);
    }

    public static final Parcelable.Creator<PlacedItemInfo> CREATOR = new Parcelable.Creator<PlacedItemInfo>() {
        public PlacedItemInfo createFromParcel(Parcel in) {
            return new PlacedItemInfo(in);
        }
        public PlacedItemInfo[] newArray(int size) {
            return new PlacedItemInfo[size];
        }
    };
}
