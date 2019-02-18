package com.manouti.itemfinder.location.provider;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.firebase.crash.FirebaseCrash;
import com.manouti.itemfinder.prefs.ItemfinderPreferencesActivity;
import com.manouti.itemfinder.R;

import java.util.concurrent.atomic.AtomicBoolean;


public class CurrentLocationProvider extends AbstractLocationProvider implements LocationListener {
    private static final String TAG = CurrentLocationProvider.class.getSimpleName();

    public static final int LOCATION_REQUEST_CHECK_SETTINGS = 314;

    private static final float ACCEPTABLE_ACCURACY = 2000f;
    private static final float ZERO_SPEED = 0.0f;
    private static final int MAX_ATTEMPTS_FOR_LOCATION = 5;
    private static final long LOCATION_UPDATE_INTERVAL = 2000L;

    private final static AtomicBoolean staticLocationResolutionFlag = new AtomicBoolean(true);
    private final AtomicBoolean mShouldStartResolutionForLocationSettings;

    private GoogleApiClient mGoogleApiClient;
    private int mGoogleServiceResolutionRequestCode;

    private final AtomicBoolean mStarted;
    private int mLocationAttemptCount;
    private boolean mAccurate;
    private boolean mBalancedMode;

    public CurrentLocationProvider(Context context, int resolutionRequestCode, LocationResultCallback locationResultCallback) {
        this(context, resolutionRequestCode, true, locationResultCallback);
    }

    public CurrentLocationProvider(Context context, int resolutionRequestCode, boolean accurate, LocationResultCallback locationResultCallback) {
        super(context, locationResultCallback);
        mStarted = new AtomicBoolean(false);
        this.mAccurate = accurate;
        mGoogleServiceResolutionRequestCode = resolutionRequestCode;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastTimeAppClosed = prefs.getLong(ItemfinderPreferencesActivity.KEY_PREFS_LAST_TIME_MAIN_ACTIVITY_DESTROYED, -1);
        if(lastTimeAppClosed < (System.currentTimeMillis() - 24 * 60 * 60 * 1000) && staticLocationResolutionFlag.compareAndSet(true, false)) {
            mShouldStartResolutionForLocationSettings = new AtomicBoolean(true);
        } else {
            mShouldStartResolutionForLocationSettings = new AtomicBoolean(false);
        }
    }

    @RequiresPermission(
            anyOf = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}
    )
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
            } else {
                checkSettingsAndRequestLocation();
            }
        }
    }

    @Override
    public void stop() {
        if(mStarted.compareAndSet(true, false)) {
            if(mGoogleApiClient != null) {
                if(mGoogleApiClient.isConnected()) {
                    Log.i(TAG, "Removing location updates...");
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                }
                Log.i(TAG, "Disconnecting Google API Client...");
                mGoogleApiClient.disconnect();
            }
        }
    }

    private void checkSettingsAndRequestLocation() {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setNumUpdates(MAX_ATTEMPTS_FOR_LOCATION);

        if(mContext instanceof Activity) {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                            builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                    final Status status = locationSettingsResult.getStatus();
                    final LocationSettingsStates locationSettingsStates = locationSettingsResult.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            requestLastLocation(locationRequest);
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            if(mShouldStartResolutionForLocationSettings.compareAndSet(true, false)) {
                                try {
                                    status.startResolutionForResult((Activity) mContext, LOCATION_REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.w(TAG, e);
                                    logLocationSettings(locationSettingsStates);
                                    FirebaseCrash.report(e);
                                }
                            } else {
                                mLocationResultCallback.onLocationSettingsNotAvailable();
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            logLocationSettings(locationSettingsStates);
                            mLocationResultCallback.onLocationSettingsNotAvailable();
                            break;
                    }
                }
            });
        } else {
            requestLastLocation(locationRequest);
        }
    }

    private void logLocationSettings(LocationSettingsStates locationSettingsStates) {
        FirebaseCrash.log("BlePresent: " + locationSettingsStates.isBlePresent()
                + ", isBleUsable: " + locationSettingsStates.isBleUsable()
                + ", isGpsPresent: " + locationSettingsStates.isGpsPresent()
                + ", isGpsUsable: " + locationSettingsStates.isGpsUsable()
                + ", isLocationPresent: " + locationSettingsStates.isLocationPresent()
                + ", isLocationUsable: " + locationSettingsStates.isLocationUsable()
                + ", isNetworkLocationPresent: " + locationSettingsStates.isNetworkLocationPresent()
                + ", isNetworkLocationUsable: " + locationSettingsStates.isNetworkLocationUsable());
    }

    @SuppressWarnings({"ResourceType"})
    private void requestLastLocation(LocationRequest locationRequest) {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    private class LocationApiConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(Bundle bundle) {
            checkSettingsAndRequestLocation();
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
            Toast.makeText(mContext, mContext.getResources().getString(R.string.google_api_connection_failed)
                            + ": Error " + connectionResult.getErrorCode()
                            + " (" + CommonStatusCodes.getStatusCodeString(connectionResult.getErrorCode()) + ")"
                            + (connectionResult.getErrorMessage() != null ?
                            System.getProperty("line.separator") + connectionResult.getErrorMessage() : ""),
                    Toast.LENGTH_LONG).show();
            if(mContext instanceof Activity && connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult((Activity) mContext, mGoogleServiceResolutionRequestCode);
                } catch (IntentSender.SendIntentException e) {
                    FirebaseCrash.report(e);
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.google_api_connection_resolution_problem)
                            + (e.getMessage() != null ? ": " + e.getMessage() : ""), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private boolean acceptableLocation(Location location) {
        return location != null && (!mAccurate || location.getAccuracy() <= ACCEPTABLE_ACCURACY || location.getSpeed() > ZERO_SPEED);
    }

    @Override
    @SuppressWarnings({"ResourceType"})
    public void onLocationChanged(Location location) {
        Log.w(TAG, "onLocationChanged, balance=" + mBalancedMode + ": " + (location != null ? (location.getLatitude() + ", " + location.getLongitude()
         + ", acc: " + location.getAccuracy() + ", speed: " + location.getSpeed()) : "null"));
        mLocationAttemptCount++;

        if(acceptableLocation(location)) {
            mLocationResultCallback.onLocationReceived(location);
            removeLocationUpdates(false);
        } else if(mLocationAttemptCount == MAX_ATTEMPTS_FOR_LOCATION) {
            if(!mBalancedMode) {
                removeLocationUpdates(true);
                LocationRequest locationRequest = new LocationRequest();
//                locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
                locationRequest.setNumUpdates(MAX_ATTEMPTS_FOR_LOCATION);
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
            } else {
                mLocationResultCallback.onLocationNotAvailable(mContext.getResources().getString(R.string.current_location_not_available));
                removeLocationUpdates(false);
            }
        }
    }

    private void removeLocationUpdates(boolean balancedMode) {
        if(mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        this.mLocationAttemptCount = 0;
        this.mBalancedMode = balancedMode;
    }

}
