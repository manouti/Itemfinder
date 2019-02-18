package com.manouti.itemfinder.user;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.auth.model.User;
import com.firebase.ui.auth.ui.email.field_validators.EmailFieldValidator;
import com.firebase.ui.auth.ui.email.field_validators.RequiredFieldValidator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.PermissionUtils;
import com.manouti.itemfinder.util.firebase.FirebaseImageLoader;
import com.manouti.itemfinder.util.firebase.FirebaseStorageUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

import cn.pedant.SweetAlert.SweetAlertDialog;
import id.zelory.compressor.Compressor;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = EditProfileActivity.class.getSimpleName();

    private static final int SAVE_MENU_ITEM_ID = 113;

    private static final int REQUEST_PHOTO = 131;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 123;

    private static final int IMAGE_UPLOAD_PROGRESS_THRESHOLD = 25;

    private DatabaseReference mDatabaseUserReference;
    private StorageReference mPhotoStorageReference;

    private FirebaseUser mCurrentUser;

    private ImageView mProfilePictureImageView;
    private EditText mDisplayNameEditText;
    private EditText mAboutUserEditText;
    private EditText mNameEditText;
    private EditText mEmailEditText;

    private RequiredFieldValidator mDisplayNameValidator;
    private RequiredFieldValidator mNameValidator;
    private EmailFieldValidator mEmailFieldValidator;

    private Uri mNewProfileImageFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mCurrentUser == null) {
            Log.w(TAG, "Current user is unexpectedly null");
            finish();
            return;
        }

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        mDatabaseUserReference = database.child("users").child(mCurrentUser.getUid());

        mPhotoStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseStorageUtil.STORAGE_URL)
                .child(FirebaseStorageUtil.IMAGES_PATH)
                .child(FirebaseStorageUtil.USERS_PATH)
                .child(mCurrentUser.getUid());

        populateProfile();
    }

    private void populateProfile() {
        mProfilePictureImageView = (ImageView) findViewById(R.id.user_profile_picture);
        mDisplayNameEditText = (EditText) findViewById(R.id.display_name_edit_text);

        mAboutUserEditText = (EditText) findViewById(R.id.about_user_edit_text);
        mAboutUserEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (v.getId() == R.id.about_user_edit_text) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_UP:
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            break;
                    }
                }
                return false;
            }
        });

        mNameEditText = (EditText) findViewById(R.id.name_edit_text);
        mEmailEditText = (EditText) findViewById(R.id.email_edit_text);

        mDisplayNameValidator = new RequiredFieldValidator((TextInputLayout)
                findViewById(R.id.display_name_input_layout));
        mNameValidator = new RequiredFieldValidator((TextInputLayout)
                findViewById(R.id.name_input_layout));
        mEmailFieldValidator = new EmailFieldValidator((TextInputLayout) findViewById(R.id.email_input_layout));

        mDatabaseUserReference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                mDisplayNameEditText.setText(user.getDN());

                String aboutUser = user.getAboutUser();
                if (StringUtils.isNotBlank(aboutUser)) {
                    mAboutUserEditText.setText(aboutUser);
                }

                mNameEditText.setText(user.getName());
                mEmailEditText.setText(mCurrentUser.getEmail());

                populatePhoto();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "populateProfile:onCancelled", databaseError.toException());
            }
        });
    }

    private void populatePhoto() {
        Glide.with(this)
                .using(new FirebaseImageLoader())
                .load(mPhotoStorageReference)
                .fitCenter()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(mProfilePictureImageView);
    }

    public void changePicture(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, REQUEST_READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE, true);
        } else {
            startPhotoPicker();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode != REQUEST_READ_EXTERNAL_STORAGE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            startPhotoPicker();
        } else {
            PermissionUtils.PermissionDeniedDialog.newInstance(false).show(getSupportFragmentManager(), "dialog");
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void startPhotoPicker() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if(requestCode == REQUEST_PHOTO && resultCode == RESULT_OK) {
            Uri imageUrl = imageReturnedIntent.getData();

            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(imageUrl, filePathColumn, null, null, null);
            if(cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String imagePath = cursor.getString(columnIndex);
                cursor.close();
                File imageFile = new File(imagePath);
                File compressedImageFile = Compressor.getDefault(this).compressToFile(imageFile);
                mNewProfileImageFileUri = Uri.fromFile(compressedImageFile);
            } else {
                Log.e(TAG, "Content resolver cursor null");
                FirebaseCrash.report(new NullPointerException("Content resolver cursor null"));
                DialogUtils.showErrorDialog(EditProfileActivity.this, getString(R.string.generic_error_message), null);
                return;
            }

            Glide.with(EditProfileActivity.this)
                    .load(mNewProfileImageFileUri)
                    .fitCenter()
                    .into(mProfilePictureImageView);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, SAVE_MENU_ITEM_ID, 0, R.string.action_save)
                .setIcon(R.drawable.ic_save_white_24dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == SAVE_MENU_ITEM_ID) {
            updateProfile(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateProfile(View view) {
        String email = mEmailEditText.getText().toString();
        final String name = mNameEditText.getText().toString();
        final String displayName = mDisplayNameEditText.getText().toString();
        final String aboutUser = mAboutUserEditText.getText().toString();

        boolean displayNameValid = mDisplayNameValidator.validate(displayName);
        boolean nameValid = mNameValidator.validate(name);
        boolean emailValid = mEmailFieldValidator.validate(email);

        if (emailValid && nameValid && displayNameValid) {
            final SweetAlertDialog progressDialog = DialogUtils.showProgressDialog(this, getString(R.string.progress_updating_profile), null);

            mCurrentUser.updateEmail(email).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error updating user email", e);
                    DialogUtils.showErrorDialog(EditProfileActivity.this, getString(R.string.generic_error_message), e.getLocalizedMessage());
                    progressDialog.dismiss();
                }
            }).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    UserProfileChangeRequest.Builder profileChangeRequestBuilder = new UserProfileChangeRequest
                            .Builder()
                            .setDisplayName(displayName);
                    if (mNewProfileImageFileUri != null) {
                        profileChangeRequestBuilder.setPhotoUri(mNewProfileImageFileUri);
                    }
                    Task<Void> updateTask = mCurrentUser.updateProfile(
                            profileChangeRequestBuilder.build());
                    updateTask
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "Error updating profile display name and photo URI", e);
                                    DialogUtils.showErrorDialog(EditProfileActivity.this, getString(R.string.generic_error_message), e.getLocalizedMessage());
                                    progressDialog.dismiss();
                                }
                            })
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        updateUserInDatabase(name, displayName, aboutUser, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                                if (databaseError != null) {
                                                    Log.e(TAG, getString(R.string.generic_error_message) + ": " + databaseError);
                                                    DialogUtils.showErrorDialog(EditProfileActivity.this, getString(R.string.generic_error_message), databaseError.getMessage());
                                                    progressDialog.dismiss();
                                                } else {
                                                    updatePhoto(progressDialog);
                                                }
                                            }
                                        });
                                    } else {
                                        Exception exception = task.getException();
                                        if (exception != null) {
                                            Log.e(TAG, getString(R.string.generic_error_message), exception);
                                            DialogUtils.showErrorDialog(EditProfileActivity.this, getString(R.string.generic_error_message), exception.getLocalizedMessage());
                                        } else {
                                            Log.e(TAG, getString(R.string.generic_error_message));
                                            DialogUtils.showErrorDialog(EditProfileActivity.this, getString(R.string.generic_error_message), null);
                                        }
                                        progressDialog.dismiss();
                                    }
                                }
                            });
                }
            });
        }
    }

    private void updateUserInDatabase(String name, String displayName, String aboutUser, DatabaseReference.CompletionListener completionListener) {
        User user = new User(mCurrentUser.getUid(), name, displayName, aboutUser);
        mDatabaseUserReference.updateChildren(user.toMap(), completionListener);
    }

    private void updatePhoto(final Dialog progressDialog) {
        if(mNewProfileImageFileUri != null) {
            mPhotoStorageReference.putFile(mNewProfileImageFileUri).addOnProgressListener(this, new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    if(progress >= IMAGE_UPLOAD_PROGRESS_THRESHOLD) {
                        progressDialog.dismiss();
                        returnToCallerActivity(mNewProfileImageFileUri);
                    }
                }
            }).addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, getString(R.string.generic_error_message), e);
                    DialogUtils.showErrorDialog(EditProfileActivity.this, getString(R.string.generic_error_message), e.getLocalizedMessage());
                    progressDialog.dismiss();
                }
            });
        } else {
            progressDialog.dismiss();
        }
    }

    private void returnToCallerActivity(Uri mNewPhotoUri) {
        Intent data = new Intent();
        data.putExtra(Intents.EDIT_PROFILE_NEW_PHOTO_URI, mNewPhotoUri);
        setResult(RESULT_OK, data);
        finish();
    }
}
