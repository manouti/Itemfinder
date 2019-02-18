package com.manouti.itemfinder.item.additem;

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.Result;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.manouti.itemfinder.Intents;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.ItemFactory;
import com.manouti.itemfinder.item.detail.ItemDetailArrayAdapter;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.result.ParcelableBarcodeResult;
import com.manouti.itemfinder.util.DialogUtils;
import com.manouti.itemfinder.util.FileSystemUtils;
import com.manouti.itemfinder.util.PermissionUtils;
import com.manouti.itemfinder.util.firebase.FirebaseStorageUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import id.zelory.compressor.Compressor;

public class AddNewItemActivity extends AppCompatActivity {

    private static final String TAG = AddNewItemActivity.class.getSimpleName();

    private static final int ADD_NEW_ITEM_ITEM_ID = 113;

    private static final int REQUEST_PHOTO = 131;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 123;

    private static final int IMAGE_UPLOAD_PROGRESS_THRESHOLD = 15;

    private ImageView mItemImageView;
    private EditText mItemSummaryEditText;
    private EditText mItemDescriptionEditText;
    private CardView mItemDetailsCardView;
    private ListView mItemDetailsListView;

    private MenuItem mAddMenuItem;

    private Item mNewItem;
    private DatabaseReference mDatabaseReference;
    private StorageReference mPhotoStorageReference;
    private Uri mNewImageFileUri;
    private File mCurrentCameraImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_item);

        String itemId = getIntent().getStringExtra(Intents.NEW_ITEM_ID_INPUT);
        if(itemId == null) {
            ParcelableBarcodeResult parcelableBarcodeResult = getIntent().getParcelableExtra(Intents.NEW_ITEM_PARCELABLE_BARCODE);
            if(parcelableBarcodeResult == null) {
                Log.w(TAG, "Neither an item ID nor a barcode were provided for the new item");
                finish();
                return;
            }
            Result rawResult = parcelableBarcodeResult.getRawResult();
            ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
            itemId = resultHandler.getDisplayContents();

            mNewItem = ItemFactory.fromBarcodeResult(parcelableBarcodeResult, this);
        } else {
            mNewItem = new Item();
            //TODO type
            mNewItem.setId(itemId);
        }

        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.add_new_item_relative_layout);
        relativeLayout.requestFocus();

        TextView itemIdTextView = (TextView) findViewById(R.id.item_id_text_view);
        itemIdTextView.setText(itemId);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mPhotoStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(FirebaseStorageUtil.STORAGE_URL)
                .child(FirebaseStorageUtil.IMAGES_PATH)
                .child(FirebaseStorageUtil.ITEMS_PATH)
                .child(itemId);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Button addNewItemButton = (Button) findViewById(R.id.button_add_new_item);

        mItemImageView = (ImageView) findViewById(R.id.item_image_view);
        mItemSummaryEditText = (EditText) findViewById(R.id.item_summary_edit_text);
        mItemSummaryEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean validItemInput = mItemSummaryEditText.getText().toString().length() > 0;
                mAddMenuItem.setEnabled(validItemInput);
                addNewItemButton.setEnabled(validItemInput);
            }
        });

        mItemDescriptionEditText = (EditText) findViewById(R.id.item_description_edit_text);
        mItemDescriptionEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (v.getId() == R.id.item_description_edit_text) {
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

        mItemDetailsCardView = (CardView) findViewById(R.id.new_item_details_card_view);
        mItemDetailsListView = (ListView) findViewById(R.id.new_item_details_listview);
        setItemDetailsListAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mAddMenuItem = menu.add(Menu.NONE, ADD_NEW_ITEM_ITEM_ID, Menu.NONE, R.string.action_add_new_item);
        mAddMenuItem.setIcon(R.drawable.ic_save_white_24dp);
        mAddMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mAddMenuItem.setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == ADD_NEW_ITEM_ITEM_ID) {
            addItem(null);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
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

    public void addItem(View view) {
        final Dialog progressDialog = DialogUtils.showProgressDialog(this, getString(R.string.progress_adding_item), null);
        mNewItem.setS(mItemSummaryEditText.getText().toString());
        mNewItem.setDesc(mItemDescriptionEditText.getText() != null ? mItemDescriptionEditText.getText().toString() : "");
        mDatabaseReference.child("items").child(mNewItem.getId()).setValue(mNewItem, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Log.e(TAG, getString(R.string.error_adding_item) + ": " + databaseError);
                    DialogUtils.showErrorDialog(AddNewItemActivity.this, getString(R.string.error_adding_item), null);
                    progressDialog.dismiss();
                } else {
                    if(mNewImageFileUri != null) {
                        uploadItemImageAndReturnFromActivity(mNewImageFileUri, progressDialog);
                    } else {
                        returnToCallerActivity(mNewItem);
                    }
                }
            }
        });
    }

    private void setItemDetailsListViewHeightBasedOnChildren() {
        ListAdapter listAdapter = mItemDetailsListView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(mItemDetailsListView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, mItemDetailsListView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = mItemDetailsListView.getLayoutParams();
        params.height = totalHeight + (mItemDetailsListView.getDividerHeight() * (listAdapter.getCount() - 1));
        mItemDetailsListView.setLayoutParams(params);
        mItemDetailsListView.requestLayout();
    }

    private void uploadItemImageAndReturnFromActivity(Uri imageUri, final Dialog progressDialog) {
        mPhotoStorageReference.putFile(imageUri).addOnProgressListener(AddNewItemActivity.this, new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                if (progress >= IMAGE_UPLOAD_PROGRESS_THRESHOLD) {
                    returnToCallerActivity(mNewItem);
                }
            }
        }).addOnFailureListener(AddNewItemActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, getString(R.string.generic_error_message), e);
                DialogUtils.showErrorDialog(AddNewItemActivity.this, getString(R.string.generic_error_message), e.getLocalizedMessage());
                progressDialog.dismiss();
            }
        });
    }

    private void returnToCallerActivity(Item item) {
        Intent data = new Intent();
        data.putExtra(Intents.NEW_ITEM_RETURNED_INFO, item);
        setResult(RESULT_OK, data);
        finish();
    }

    private void setItemDetailsListAdapter() {
        mItemDetailsListView.setAdapter(new ItemDetailArrayAdapter(this, mNewItem));
        setItemDetailsListViewHeightBasedOnChildren();
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

    private void startPhotoPicker() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(photoPickerIntent, "Item image");

        try {
            mCurrentCameraImageFile = FileSystemUtils.getUniqueImageFile();
            Uri cameraImageUri = Uri.fromFile(mCurrentCameraImageFile);

            final List<Intent> cameraIntents = new ArrayList<>();
            final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            final PackageManager packageManager = getPackageManager();
            final List<ResolveInfo> cameraActivities = packageManager.queryIntentActivities(captureIntent, 0);
            for(ResolveInfo camera : cameraActivities) {
                String packageName = camera.activityInfo.packageName;
                Intent intent = new Intent(captureIntent);
                intent.setComponent(new ComponentName(camera.activityInfo.packageName, camera.activityInfo.name));
                intent.setPackage(packageName);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraIntents.add(intent);
            }

            // Add the camera options.
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));
        } catch (IOException e) {
            e.printStackTrace();
        }

        startActivityForResult(chooserIntent, REQUEST_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if(requestCode == REQUEST_PHOTO && resultCode == RESULT_OK) {
            boolean isImageFromCamera;
            if (imageReturnedIntent == null) {
                isImageFromCamera = true;
            } else {
                final String action = imageReturnedIntent.getAction();
                isImageFromCamera = android.provider.MediaStore.ACTION_IMAGE_CAPTURE.equals(action);
            }

            if (isImageFromCamera) {
                File compressedImageFile = Compressor.getDefault(this).compressToFile(mCurrentCameraImageFile);
                mNewImageFileUri = Uri.fromFile(compressedImageFile);
            } else {
                Uri imageUrl = imageReturnedIntent.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(imageUrl, filePathColumn, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String imagePath = cursor.getString(columnIndex);
                    cursor.close();
                    File imageFile = new File(imagePath);
                    File compressedImageFile = Compressor.getDefault(this).compressToFile(imageFile);
                    mNewImageFileUri = Uri.fromFile(compressedImageFile);
                } else {
                    Log.e(TAG, "Content resolver cursor null");
                    FirebaseCrash.report(new NullPointerException("Content resolver cursor null"));
                    DialogUtils.showErrorDialog(AddNewItemActivity.this, getString(R.string.generic_error_message), null);
                    return;
                }
            }

            Glide.with(this)
                    .load(mNewImageFileUri)
                    .fitCenter()
                    .into(mItemImageView);
        }
    }

}
