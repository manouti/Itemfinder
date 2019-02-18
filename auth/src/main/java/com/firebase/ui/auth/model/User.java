package com.firebase.ui.auth.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;


@IgnoreExtraProperties
public class User implements Parcelable {
    private String mUid;
    private String mName;
    private String mDisplayName;
    private String mAboutUser;
    private long mReputation;

    public User() {}

    public User(String uid, String name, String displayName, String aboutUser) {
        this.mUid = uid;
        this.mName = name;
        this.mDisplayName = displayName;
        this.mAboutUser = aboutUser;
    }

    private User(Parcel in) {
        mUid = in.readString();
        mName = in.readString();
        mDisplayName = in.readString();
        mAboutUser = in.readString();
        mReputation = in.readLong();
    }

    public String getUid() {
        return mUid;
    }

    public void setUid(String mUid) {
        this.mUid = mUid;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getDN() {
        return mDisplayName;
    }

    public void setDN(String mDisplayName) {
        this.mDisplayName = mDisplayName;
    }

    public String getAboutUser() {
        return mAboutUser;
    }

    public void setAboutUser(String mAboutUser) {
        this.mAboutUser = mAboutUser;
    }

    public long getRep() {
        return mReputation;
    }

    public void setRep(long mReputation) {
        this.mReputation = mReputation;
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", mName);
        result.put("dn", mDisplayName);
        result.put("aboutUser", mAboutUser);

        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUid);
        dest.writeString(mName);
        dest.writeString(mDisplayName);
        dest.writeString(mAboutUser);
        dest.writeLong(mReputation);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
