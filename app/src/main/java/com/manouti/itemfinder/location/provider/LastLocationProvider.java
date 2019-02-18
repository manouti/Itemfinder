package com.manouti.itemfinder.location.provider;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.crash.FirebaseCrash;
import com.manouti.itemfinder.R;

import java.util.concurrent.atomic.AtomicBoolean;


public class LastLocationProvider extends AbstractLocationProvider {

    private static final String TAG = LastLocationProvider.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private int mGoogleServiceResolutionRequestCode;

    private final AtomicBoolean mStarted;

    public LastLocationProvider(Context context, int resolutionRequestCode, LocationResultCallback locationResultCallback) {
        super(context, locationResultCallback);
        mStarted = new AtomicBoolean(false);
        this.mGoogleServiceResolutionRequestCode = resolutionRequestCode;
    }

    @Override
    public void start() {
        if(mStarted.compareAndSet(false, true)) {
            if(mGoogleApiClient == null) {
                final LocationApiConnectionCallbacks locationConnectionCallbacks = new LocationApiConnectionCallbacks();
                mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                        .addConnectionCallbacks(locationConnectionCallbacks)
                        .addOnConnectionFailedListener(new LocationConnectionFailedListener())
                        .addApi(LocationServices.API)
                        .build();
            }
            if(!mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void stop() {
        if(mStarted.compareAndSet(true, false)) {
            if(mGoogleApiClient != null) {
                Log.i(TAG, "Disconnecting Google API Client...");
                mGoogleApiClient.disconnect();
            }
        }
    }

    private class LocationApiConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        @SuppressWarnings({"ResourceType"})
        public void onConnected(Bundle bundle) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLocationResultCallback.onLocationReceived(lastLocation);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            FirebaseCrash.log("CurrentLocationProvider:GoogleApiClient.ConnectionCallbacks:onConnectionSuspended: - cause: " + cause);
            String errorCause = null;
            if(cause == CAUSE_NETWORK_LOST) {
                errorCause = "A peer device connection was lost";
            } else if(cause == CAUSE_SERVICE_DISCONNECTED) {
                errorCause = "Service has been killed";
            }
            Toast.makeText(mContext, mContext.getResources().getString(R.string.location_connection_suspended)
                            + (errorCause != null ? ":" + System.getProperty("line.separator") + errorCause : ""),
                    Toast.LENGTH_LONG).show();
        }
    }

    private class LocationConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            String errorMessage = mContext.getResources().getString(R.string.google_api_connection_failed)
                    + ": Error " + connectionResult.getErrorCode()
                    + " (" + CommonStatusCodes.getStatusCodeString(connectionResult.getErrorCode()) + ")"
                    + (connectionResult.getErrorMessage() != null ?
                    System.getProperty("line.separator") + connectionResult.getErrorMessage() : "");
            Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();

            if(mContext instanceof Activity && connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult((Activity) mContext, mGoogleServiceResolutionRequestCode);
                } catch (IntentSender.SendIntentException e) {
                    mLocationResultCallback.onLocationNotAvailable(errorMessage);
                    FirebaseCrash.report(e);
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.google_api_connection_resolution_problem)
                            + (e.getMessage() != null ? ": " + e.getMessage() : ""), Toast.LENGTH_SHORT).show();
                }
            } else {
                mLocationResultCallback.onLocationNotAvailable(errorMessage);
            }
        }
    }
}
