package com.manouti.itemfinder.user.locations;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.manouti.itemfinder.BaseActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.model.user.UserLocation;
import com.manouti.itemfinder.util.PermissionUtils;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class AddUserLocationActivity extends BaseActivity implements PlaceSelectionListener, View.OnClickListener {

    private static final String TAG = AddUserLocationActivity.class.getSimpleName();

    private static final int REQUEST_LOCATION_PERMISSION = 1231;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1232;
    private static final int REQUEST_PLACE_PICKER = 23;

    private Place mPlace;
    private TextView mPlaceTitle;
    private TextView mPlaceDescription;
    private ImageView mGoogleAttributionLogo;

    private DatabaseReference mDatabaseLocationsReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Retrieve the SupportPlaceAutocompleteFragment.
        SupportPlaceAutocompleteFragment autocompleteFragment =
                (SupportPlaceAutocompleteFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        // Register a listener to receive callbacks when a place has been selected or an error has
        // occurred.
        autocompleteFragment.setOnPlaceSelectedListener(this);

        ImageButton pickPlaceButton = (ImageButton) findViewById(R.id.button_pick_place);
        pickPlaceButton.setOnClickListener(this);

        mPlaceTitle = (TextView) findViewById(R.id.place_title_text_view);
        mPlaceDescription = (TextView) findViewById(R.id.place_description_text_view);

        final Button saveLocationButton = (Button) findViewById(R.id.button_save_location);

        mPlaceTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean validPlaceInput = mPlaceTitle.getText().toString().length() > 0;
                saveLocationButton.setEnabled(validPlaceInput);
            }
        });
        mGoogleAttributionLogo = (ImageView) findViewById(R.id.google_attribution_logo);
        mGoogleAttributionLogo.setVisibility(View.INVISIBLE);

        FirebaseUser currentUser = getCurrentUser();
        if(currentUser == null) {
            logIn(null);
        } else {
            String userId = currentUser.getUid();
            mDatabaseLocationsReference = FirebaseDatabase.getInstance().getReference()
                    .child("user-locations").child(userId);
        }
    }

    @Override
    protected void onLoginSucceeded() {
        super.onLoginSucceeded();
        String userId = getCurrentUser().getUid();
        mDatabaseLocationsReference = FirebaseDatabase.getInstance().getReference()
                .child("user-locations").child(userId);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_new_user_location;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPlaceSelected(Place place) {
        this.mPlace = place;
        showPlaceDetails(place);
    }

    @Override
    public void onError(Status status) {
        Log.e(TAG, "onError: Status = " + status.toString());
        FirebaseCrash.log("PlaceSelectionListener:onError:status=" + status.getStatusCode() + ",statusMessage=" + status.getStatusMessage());
        showSnackbar("Place selection failed: " + status.getStatusMessage());
    }

    /**
     * Helper method to show place details in the fragment's text views.
     * A Place object contains details about that place, such as its name, address
     and phone number. Extract the name, address, phone number, place ID and place types.
     */
    private void showPlaceDetails(Place place) {
        mGoogleAttributionLogo.setVisibility(View.VISIBLE);

        final CharSequence name = place.getName();
        final CharSequence address = place.getAddress();
        final CharSequence phone = StringUtils.defaultIfBlank(place.getPhoneNumber(), getString(R.string.not_available));
        final CharSequence attributions = ObjectUtils.defaultIfNull(place.getAttributions(), "");

        mPlaceTitle.setText(name.toString());
        mPlaceDescription.setText(getString(R.string.google_place_detail_text, address, phone.toString(), attributions).trim());
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_pick_place) {
            selectPlace();
        }
    }

    public void selectPlace() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, REQUEST_LOCATION_PERMISSION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else {
            // Access to the location has been granted to the app.
            startPlacePicker();
        }
    }

    private void startPlacePicker() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this), REQUEST_PLACE_PICKER);
        } catch (GooglePlayServicesRepairableException e) {
            FirebaseCrash.report(e);
            GoogleApiAvailability.getInstance().getErrorDialog(this, e.getConnectionStatusCode(), REQUEST_GOOGLE_PLAY_SERVICES).show();
        } catch (GooglePlayServicesNotAvailableException e) {
            FirebaseCrash.report(e);
            showSnackbar(getString(R.string.google_play_services_not_available) + ": " + GoogleApiAvailability.getInstance().getErrorString(e.errorCode));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                // The user has resolved the error that occurred when starting the place picker.
                if (resultCode == Activity.RESULT_OK) {
                    startPlacePicker();
                }
                break;
            case REQUEST_PLACE_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    mPlace = PlacePicker.getPlace(this, data);
                    showPlaceDetails(mPlace);
                } else {
                    showSnackbar("Place picker not successful: " + resultCode
                            + " " + CommonStatusCodes.getStatusCodeString(resultCode));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Start place picker if the permission has been granted.
            startPlacePicker();
        } else {
            PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getSupportFragmentManager(), "dialog");
        }
    }

    public void saveLocation(View view) {
        if(mPlace == null) {
            showQuickSnackbar("Invalid place");
            return;
        }

        String placeId = mPlace.getId();
        LatLng latLng = mPlace.getLatLng();
        UserLocation userLocation = new UserLocation(placeId, mPlaceTitle.getText().toString(),
                mPlaceDescription.getText().toString(), latLng.latitude, latLng.longitude, false);
        mDatabaseLocationsReference.child(placeId).setValue(userLocation.toMap());
        finish();
    }
}
