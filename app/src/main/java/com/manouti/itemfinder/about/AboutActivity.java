package com.manouti.itemfinder.about;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.zxing.client.android.CaptureActivity;
import com.manouti.itemfinder.R;

public class AboutActivity extends AppCompatActivity {

    private static final int POSITION_LEGAL_NOTICES_ITEM = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView versionTextView = (TextView) findViewById(R.id.version_text_view);
        versionTextView.setText("Version " + getString(R.string.app_version_number));

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == POSITION_LEGAL_NOTICES_ITEM) {
                    Intent legalNoticesIntent = new Intent(AboutActivity.this, LegalNoticesActivity.class);
                    startActivity(legalNoticesIntent);
                }
            }
        });

    }

}
