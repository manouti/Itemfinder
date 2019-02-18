package com.manouti.itemfinder.about;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.manouti.itemfinder.R;

public class LegalNoticesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal_notices);

        TextView attributionsTextView = (TextView) findViewById(R.id.google_services_attribution);
        attributionsTextView.setText(GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(this));
    }

}
