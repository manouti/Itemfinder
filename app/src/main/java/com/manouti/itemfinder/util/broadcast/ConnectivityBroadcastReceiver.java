package com.manouti.itemfinder.util.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class ConnectivityBroadcastReceiver extends BroadcastReceiver {

    private ConnectivityBroadcastHandler mConnectivityBroadcastHandler;

    public ConnectivityBroadcastReceiver(ConnectivityBroadcastHandler connectivityBroadcastHandler) {
        mConnectivityBroadcastHandler = connectivityBroadcastHandler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mConnectivityBroadcastHandler.handleConnectivityBroadcast(context, intent);
    }

    public interface ConnectivityBroadcastHandler {
        void handleConnectivityBroadcast(Context context, Intent intent);
    }
}
