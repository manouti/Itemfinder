package com.manouti.itemfinder.util;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;


public class LocationUtils {
    public static boolean locationSettingsEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return gpsEnabled || networkEnabled;
    }

    public static boolean isIntentActionLocationEnabled(Context context, Intent intent) {
        return intent.getAction().equals(LocationManager.PROVIDERS_CHANGED_ACTION) && locationSettingsEnabled(context);
    }
}
