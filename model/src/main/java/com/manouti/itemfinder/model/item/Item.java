package com.manouti.itemfinder.model.item;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class for an item.
 */
@IgnoreExtraProperties
//TODO create a builder for this
public class Item implements Parcelable {
    @Exclude
    private String mId;  // Key
    @Exclude
    private long mTimestamp = -1;
    protected ItemType mType;
    protected String mSummary;
    protected String mDescription;
    protected double mRating = -1;
    protected int mVoteCount;
    protected List<String> mCategories;

    public Item() {

    }

    public Item(String id, long timestamp, ItemType type, double rating, int voteCount) {
        this.mId = id;
        this.mTimestamp = timestamp;
        this.mType = type;
        this.mRating = rating;
        this.mVoteCount = voteCount;
    }

    public Item(Parcel in) {
        mId = in.readString();
        mTimestamp = in.readLong();
        if(in.readInt() != 0) {
            mType = ItemType.valueOf(in.readString());
        }
        mSummary = in.readString();
        mDescription = in.readString();
        mRating = in.readDouble();
        mVoteCount = in.readInt();
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long mTimestamp) {
        this.mTimestamp = mTimestamp;
    }

    public ItemType getItemType() {
        return mType;
    }

    public void setItemType(ItemType mType) {
        this.mType = mType;
    }

    public String getType() {
        // Convert enum to string
        if (mType == null) {
            return null;
        } else {
            return mType.toString();
        }
    }

    public void setType(String type) {
        // Get enum from string
        if (type == null) {
            this.mType = null;
        } else {
            this.mType = ItemType.valueOf(type);
        }
    }

    public String getS() {
        return mSummary;
    }

    public void setS(String summary) {
        this.mSummary = summary;
    }

    public String getDesc() {
        return mDescription;
    }

    public void setDesc(String description) {
        this.mDescription = description;
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

    @PropertyName("c")
    public List<String> getCategories() {
        return mCategories;
    }

    @PropertyName("c")
    public void setCategories(List<String> categories) {
        this.mCategories = categories;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        if(mType != null) {
            result.put("type", mType.toString());
        }
        result.put("s", mSummary);
        result.put("desc", mDescription);
        result.put("rating", mRating);
        result.put("voteCount", mVoteCount);
        result.put("c", mCategories);

        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeLong(mTimestamp);
        dest.writeInt(mType == null ? 0 : 1);
        if(mType != null) {
            dest.writeString(mType.toString());
        }
        dest.writeString(mSummary);
        dest.writeString(mDescription);
        dest.writeDouble(mRating);
        dest.writeInt(mVoteCount);
    }

    public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() {
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };

}
