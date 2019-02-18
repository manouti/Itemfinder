package com.manouti.itemfinder.location.provider;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;


public abstract class AbstractLocationProvider {
    protected Context mContext;
    protected LocationResultCallback mLocationResultCallback;

    protected AbstractLocationProvider(Context context, LocationResultCallback locationResultCallback) {
        this.mContext = context;
        this.mLocationResultCallback = locationResultCallback;
    }

    public abstract void start();

    public abstract void stop();

    public void restart() {
        stop();
        start();
    }

    public interface LocationResultCallback {
        void onLocationReceived(@NonNull Location location);

        /**
         * Called when the location provider could not get the current location.
         * @param failureMessage a failure message
         */
        void onLocationNotAvailable(String failureMessage);

        /**
         * Called when required location settings are disabled or not available for this provider.
         */
        void onLocationSettingsNotAvailable();
    }
}
