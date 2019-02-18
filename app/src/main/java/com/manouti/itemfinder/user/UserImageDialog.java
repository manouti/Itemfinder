package com.manouti.itemfinder.user;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.R;

public class UserImageDialog extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_image_dialog);

        ImageView imageView = (ImageView) findViewById(R.id.user_image_view);
        Uri imageUri = getIntent().getParcelableExtra(Intents.USER_PROFILE_PHOTO_URI);
        imageView.setImageURI(imageUri);
        imageView.setClickable(true);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}