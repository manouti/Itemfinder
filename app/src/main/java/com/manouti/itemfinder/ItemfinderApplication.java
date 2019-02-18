package com.manouti.itemfinder;

import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.manouti.itemfinder.util.kiip.CustomKiipAdapter;

import java.util.LinkedList;

import me.kiip.sdk.Kiip;
import me.kiip.sdk.KiipFragmentCompat;
import me.kiip.sdk.Poptart;


public class ItemfinderApplication extends MultiDexApplication implements Kiip.OnContentListener {

    private static final String TAG = "kiip";
    private static final String KIIP_APP_SECRET = "your_kiip_app_secret";

    private boolean nearbyItemsTaskServiceScheduled;

    public boolean isNearbyItemsTaskServiceScheduled() {
        return nearbyItemsTaskServiceScheduled;
    }

    public void setNearbyItemsTaskServiceScheduled(boolean nearbyItemsTaskServiceScheduled) {
        this.nearbyItemsTaskServiceScheduled = nearbyItemsTaskServiceScheduled;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Set a global poptart queue to persist poptarts across Activities
        KiipFragmentCompat.setDefaultQueue(new LinkedList<Poptart>());

        // Instantiate and set the shared Kiip instance
        Kiip.init(this, getString(R.string.kiip_app_key), KIIP_APP_SECRET);

        // Listen for Kiip events
        Kiip.getInstance().setOnContentListener(this);

        Kiip.getInstance().setAdapter(new CustomKiipAdapter());
    }

    @Override
    public void onContent(Kiip kiip, String content, int quantity, String transactionId, String signature) {
        Log.d(TAG, "onContent content=" + content + " quantity=" + quantity + " transactionId=" + transactionId + " signature=" + signature);
    }
}