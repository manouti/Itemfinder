package com.manouti.itemfinder.util.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class LocationProviderBroadcastReceiver extends BroadcastReceiver {

    private LocationBroadcastHandler mLocationBroadcastHandler;

    public LocationProviderBroadcastReceiver(LocationBroadcastHandler locationBroadcastHandler) {
        mLocationBroadcastHandler = locationBroadcastHandler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mLocationBroadcastHandler.handleLocationBroadcast(context, intent);
    }

    public interface LocationBroadcastHandler {
        void handleLocationBroadcast(Context context, Intent intent);
    }
}
