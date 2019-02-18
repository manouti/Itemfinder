/*
 * Copyright (C) 2013 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.manouti.itemfinder.prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.firebase.crash.FirebaseCrash;
import com.google.zxing.client.android.PreferencesActivity;
import com.manouti.itemfinder.ItemfinderApplication;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.home.MainActivity;
import com.manouti.itemfinder.search.ItemSuggestionProvider;
import com.manouti.itemfinder.service.NearbyItemsTaskService;
import com.manouti.itemfinder.util.PermissionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

public final class PreferencesFragment
        extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = PreferencesFragment.class.getSimpleName();

  private static final int REQUEST_GOOGLE_PLAY_SERVICES_FOR_GCM = 1236;
  private static final int REQUEST_LOCATION_PERMISSION_FOR_GCM_TASK_SERVICE = 124;

  private CheckBoxPreference[] checkBoxPrefs;
  private GcmNetworkManager mGcmNetworkManager;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    addPreferencesFromResource(R.xml.preferences);

    PreferenceScreen preferences = getPreferenceScreen();
    preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    checkBoxPrefs = findDecodePrefs(preferences,
            ItemfinderPreferencesActivity.KEY_DECODE_1D_PRODUCT,
            ItemfinderPreferencesActivity.KEY_DECODE_1D_INDUSTRIAL,
            ItemfinderPreferencesActivity.KEY_DECODE_QR,
            ItemfinderPreferencesActivity.KEY_DECODE_DATA_MATRIX,
            ItemfinderPreferencesActivity.KEY_DECODE_AZTEC,
            ItemfinderPreferencesActivity.KEY_DECODE_PDF417);
    disableLastCheckedPref();

    EditTextPreference customProductSearch = (EditTextPreference)
            preferences.findPreference(ItemfinderPreferencesActivity.KEY_CUSTOM_PRODUCT_SEARCH);
    customProductSearch.setOnPreferenceChangeListener(new CustomSearchURLValidator());

    Preference clearSearchHistory = preferences.findPreference(ItemfinderPreferencesActivity.KEY_CLEAR_SEARCH_HISTORY);
    clearSearchHistory.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                ItemSuggestionProvider.AUTHORITY, ItemSuggestionProvider.MODE);
        suggestions.clearHistory();
        Toast.makeText(getActivity(), R.string.search_history_cleared, Toast.LENGTH_SHORT).show();
        return true;
      }
    });
  }

  private static CheckBoxPreference[] findDecodePrefs(PreferenceScreen preferences, String... keys) {
    CheckBoxPreference[] prefs = new CheckBoxPreference[keys.length];
    for (int i = 0; i < keys.length; i++) {
      prefs[i] = (CheckBoxPreference) preferences.findPreference(keys[i]);
    }
    return prefs;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    disableLastCheckedPref();
    if (key.equals(ItemfinderPreferencesActivity.KEY_NEARBY_ITEMS_RUN_SERVICE)) {
      boolean runServiceTask = sharedPreferences.getBoolean(key, true);
      if(mGcmNetworkManager == null) {
        mGcmNetworkManager = GcmNetworkManager.getInstance(getActivity());
      }
      if(!runServiceTask) {
        try {
          mGcmNetworkManager.cancelAllTasks(NearbyItemsTaskService.class);
        } catch(Exception e) {
          FirebaseCrash.report(e);
        }
      } else {
        // Check that Google Play Services is available, since we need it to use GcmNetworkManager
        // but the API does not use GoogleApiClient, which would normally perform the check
        // automatically.
        if(checkPlayServicesAvailableForGCM()) {
          scheduleNearbyFavoriteItemsTaskService();
        }
      }
    }
  }

  private boolean checkPlayServicesAvailableForGCM() {
    GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
    int resultCode = availability.isGooglePlayServicesAvailable(getActivity());

    if (resultCode != ConnectionResult.SUCCESS) {
      if (availability.isUserResolvableError(resultCode)) {
        // Show dialog to resolve the error.
        availability.getErrorDialog(getActivity(), resultCode, REQUEST_GOOGLE_PLAY_SERVICES_FOR_GCM).show();
      } else {
        // Unresolvable error
        Toast.makeText(getActivity(), "Google Play Services error", Toast.LENGTH_LONG).show();
      }
      return false;
    }
    return true;
  }

  private void scheduleNearbyFavoriteItemsTaskService() {
    if(!((ItemfinderApplication) getActivity().getApplication()).isNearbyItemsTaskServiceScheduled()) {
      Log.d(TAG, "startNearbyFavoriteItemsTaskService");

      if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
              != PackageManager.PERMISSION_GRANTED) {
        // Permission to access the location is missing.
        PermissionUtils.requestPermission((PreferencesActivity) getActivity(), REQUEST_LOCATION_PERMISSION_FOR_GCM_TASK_SERVICE,
                android.Manifest.permission.ACCESS_FINE_LOCATION, true);
      } else {
        // Access to the location has been granted to the app.
        PeriodicTask task = new PeriodicTask.Builder()
                .setService(NearbyItemsTaskService.class)
                .setTag(MainActivity.TASK_TAG_NEARBY_FAVORITE_ITEMS)
                .setPeriod(MainActivity.SERVICE_TASK_PERIOD_SECONDS)
                .setPersisted(true)
                .build();
        mGcmNetworkManager.schedule(task);
        ((ItemfinderApplication) getActivity().getApplication()).setNearbyItemsTaskServiceScheduled(true);
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if(requestCode != REQUEST_LOCATION_PERMISSION_FOR_GCM_TASK_SERVICE) {
      return;
    }

    if (PermissionUtils.isPermissionGranted(permissions, grantResults,
            android.Manifest.permission.ACCESS_FINE_LOCATION)) {
      // Schedule the GCM task service for detecting nearby items based on current location.
      // This may be a second attempt, in case the granted permission was due to a previous attempt.
      scheduleNearbyFavoriteItemsTaskService();
    } else {
      PermissionUtils.NativePermissionDeniedDialog.newInstance(false).show(getFragmentManager(), "dialog");
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode == REQUEST_GOOGLE_PLAY_SERVICES_FOR_GCM) {
      // The user has resolved the error that occurred connecting to Google Play Services
      if (resultCode == Activity.RESULT_OK) {
        scheduleNearbyFavoriteItemsTaskService();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void disableLastCheckedPref() {
    Collection<CheckBoxPreference> checked = new ArrayList<>(checkBoxPrefs.length);
    for (CheckBoxPreference pref : checkBoxPrefs) {
      if (pref.isChecked()) {
        checked.add(pref);
      }
    }
    boolean disable = checked.size() <= 1;
    for (CheckBoxPreference pref : checkBoxPrefs) {
      pref.setEnabled(!(disable && checked.contains(pref)));
    }
  }

  private class CustomSearchURLValidator implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      if (!isValid(newValue)) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(PreferencesFragment.this.getActivity());
        builder.setTitle(R.string.msg_error);
        builder.setMessage(R.string.msg_invalid_value);
        builder.setCancelable(true);
        builder.show();
        return false;
      }
      return true;
    }

    private boolean isValid(Object newValue) {
      // Allow empty/null value
      if (newValue == null) {
        return true;
      }
      String valueString = newValue.toString();
      if (valueString.isEmpty()) {
        return true;
      }
      // Before validating, remove custom placeholders, which will not
      // be considered valid parts of the URL in some locations:
      // Blank %t and %s:
      valueString = valueString.replaceAll("%[st]", "");
      // Blank %f but not if followed by digit or a-f as it may be a hex sequence
      valueString = valueString.replaceAll("%f(?![0-9a-f])", "");
      // Require a scheme otherwise:
      try {
        URI uri = new URI(valueString);
        return uri.getScheme() != null;
      } catch (URISyntaxException use) {
        return false;
      }
    }
  }

}
