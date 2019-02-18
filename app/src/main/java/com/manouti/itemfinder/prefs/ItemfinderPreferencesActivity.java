package com.manouti.itemfinder.prefs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.google.zxing.client.android.PreferencesActivity;
import com.manouti.itemfinder.R;

/**
 * This relies on the PreferencesActivity provided by the ZXing library to provide preferences specific to ZXing features.
 */
public class ItemfinderPreferencesActivity extends PreferencesActivity {

    public static final String KEY_NEARBY_ITEMS_RUN_SERVICE = "preferences_nearby_items_task_bg";
    public static final String KEY_LOCATION_RADIUS = "preferences_nearby_items_proximity";
    public static final String KEY_PLAY_SOUND_ON_NEARBY_ITEMS = "preferences_play_sound_on_nearby_items";
    public static final String KEY_VIBRATE_ON_NEARBY_ITEMS = "preferences_vibrate_on_nearby_items";
    public static final String KEY_PREFS_LAST_TIME_MAIN_ACTIVITY_DESTROYED = "preferences_last_time_main_activity_destroyed";
    public static final String KEY_CLEAR_SEARCH_HISTORY = "preferences_clear_search_history";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//    getFragmentManager().beginTransaction().replace(R.id.settings_view, new PreferencesFragment()).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        PreferencesFragment fragment = (PreferencesFragment) getFragmentManager().findFragmentById(R.id.preferences_fragment);
        fragment.onActivityResult(requestCode, resultCode, data);
    }

}
