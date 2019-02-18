package com.manouti.itemfinder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AddPlaceRequest;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.crash.FirebaseCrash;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.PermissionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AddPlaceActivity extends AppCompatActivity {

    private static final String TAG = AddPlaceActivity.class.getSimpleName();

    private static final int REQUEST_LOCATION_PERMISSION = 1231;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1232;
    private static final int REQUEST_PLACE_PICKER = 23;

    private TextView mLatLngTextView;
    private AutoCompleteTextView mPlaceTypeTextView;
    private LatLng mLocationToAdd;
    private GoogleApiClient mGoogleApiClient;
    private List<String> validPlaceTypes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_place);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        mLocationToAdd = intent.getParcelableExtra(Intents.PLACE_LOCATION_TO_ADD);

        if(mLocationToAdd != null) {
            mLatLngTextView = (TextView) findViewById(R.id.latlng_text_view);
            mLatLngTextView.setText(mLocationToAdd.toString());
        }

        validPlaceTypes = Arrays.asList(getString(R.string.valid_place_types).split(","));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, validPlaceTypes);
        mPlaceTypeTextView = (AutoCompleteTextView)
                findViewById(R.id.types_auto_complete);
        mPlaceTypeTextView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if(mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    public void pickPlace(View view) {
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
            showSnackbar(getString(R.string.google_play_services_not_available)
                    + ": " + GoogleApiAvailability.getInstance().getErrorString(e.errorCode));
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
                    mLocationToAdd = PlacePicker.getPlace(this, data).getLatLng();
                    mLatLngTextView.setText(mLocationToAdd.toString());
                } else {
                    showSnackbar("Place picker not successful: " + resultCode
                            + " " + CommonStatusCodes.getStatusCodeString(resultCode));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void addPlace(View view) {
        String name = ((EditText) findViewById(R.id.name_edit_text)).getText().toString();
        if(!isValid(name)) {
            TextInputLayout placeNameTextInputLayout = (TextInputLayout) findViewById(R.id.place_name_layout);
            placeNameTextInputLayout.setError(getString(R.string.required_field));
            return;
        }

        String address = ((EditText) findViewById(R.id.address_edit_text)).getText().toString();
        if(!isValid(address)) {
            TextInputLayout placeAddressTextInputLayout = (TextInputLayout) findViewById(R.id.place_address_layout);
            placeAddressTextInputLayout.setError(getString(R.string.required_field));
            return;
        }

        String placeType = mPlaceTypeTextView.getText().toString();
        int placeTypeIndex = validPlaceTypes.indexOf(placeType);
        if(placeTypeIndex < 0) {
            TextInputLayout placeTypesTextInputLayout = (TextInputLayout) findViewById(R.id.place_types_layout);
            placeTypesTextInputLayout.setError(getString(R.string.invalid_place_type));
            return;
        }

        String phoneNumber = ((EditText) findViewById(R.id.phone_number_edit_text)).getText().toString();
        String website = ((EditText) findViewById(R.id.website_edit_text)).getText().toString();

        if(!isValid(phoneNumber) && !isValid(website)) {
            TextInputLayout placePhoneTextInputLayout = (TextInputLayout) findViewById(R.id.place_phone_number_layout);
            placePhoneTextInputLayout.setError(getString(R.string.phone_or_website_required));
            return;
        }

        AddPlaceRequest place =
                new AddPlaceRequest(name, mLocationToAdd, address,
                        Collections.singletonList(Place.TYPE_AQUARIUM),
                        phoneNumber,
                        Uri.parse(website));

        Places.GeoDataApi.addPlace(mGoogleApiClient, place)
                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        Log.i(TAG, "Place add result: " + places.getStatus().toString());
                        Log.i(TAG, "Added place: " + places.get(0).getName().toString());
                        places.release();
                        DialogUtils.showSuccessDialog(AddPlaceActivity.this, getString(R.string.notice_place_added), null, new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                finish();
                            }
                        });
                    }
                });
    }

    private boolean isValid(String string) {
        return string != null && !string.isEmpty();
    }

    private void showSnackbar(String errorMessage) {
        Snackbar.make(findViewById(android.R.id.content), Html.fromHtml("<font color=\"#ffffff\">" + errorMessage + "</font>"), Snackbar.LENGTH_LONG).show();
    }
}
