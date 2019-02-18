/*
 * Copyright (C) 2008 ZXing authors
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

package com.manouti.itemfinder.item.barcode;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.client.android.AmbientLightManager;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.CaptureActivityHandler;
import com.google.zxing.client.android.DecodeFormatManager;
import com.google.zxing.client.android.DecodeHintManager;
import com.google.zxing.client.android.FinishListener;
import com.google.zxing.client.android.InactivityTimer;
import com.google.zxing.client.android.IntentSource;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.android.SoundManager;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.clipboard.ClipboardInterface;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.manouti.itemfinder.HelpActivity;
import com.manouti.itemfinder.R;
import com.manouti.itemfinder.item.detail.ItemDetailActivity;
import com.manouti.itemfinder.item.history.HistoryActivity;
import com.manouti.itemfinder.item.history.HistoryEventType;
import com.manouti.itemfinder.item.history.HistoryManager;
import com.manouti.itemfinder.item.history.HistoryManagerFactory;
import com.manouti.itemfinder.model.item.ISBNItem;
import com.manouti.itemfinder.model.item.Item;
import com.manouti.itemfinder.model.item.ItemType;
import com.manouti.itemfinder.model.item.Product;
import com.manouti.itemfinder.model.item.VINItem;
import com.manouti.itemfinder.result.ParcelableBarcodeResult;
import com.manouti.itemfinder.util.PermissionUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class ItemfinderCaptureActivity extends CaptureActivity implements SurfaceHolder.Callback {

    private static final String TAG = ItemfinderCaptureActivity.class.getSimpleName();

    private static final long DEFAULT_INTENT_RESULT_DURATION_MS = 1500L;
    private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

    private static final int REQUEST_CAMERA_PERMISSION = 313;

    private static final int PLACE_PICKER_REQUEST = 23;

    private Result savedResultToShow;
    private TextView statusView;
    private View resultView;
    private Result lastResult;
    private boolean hasSurface;
    private boolean copyToClipboard;
    private IntentSource source;
    private String sourceUrl;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType,?> decodeHints;
    private String characterSet;
    private HistoryManager historyManager;
    private InactivityTimer inactivityTimer;
    private SoundManager soundManager;
    private AmbientLightManager ambientLightManager;
    private SurfaceHolder surfaceHolder;

    private DatabaseReference mDatabaseReference;
    private Map<String, DatabaseReference> mSyncedDatabaseRefs = new HashMap<>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.capture);

        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        soundManager = new SoundManager(this);
        ambientLightManager = new AmbientLightManager(this);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // historyManager must be initialized here to update the history preference
        historyManager = HistoryManagerFactory.makeHistoryManager(this);

        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        resultView = findViewById(R.id.result_view);
        statusView = (TextView) findViewById(R.id.status_view);

        handler = null;
        lastResult = null;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean(PreferencesActivity.KEY_DISABLE_AUTO_ORIENTATION, true)) {
            setRequestedOrientation(getCurrentOrientation());
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        resetStatusView();


        soundManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        Intent intent = getIntent();

        copyToClipboard = prefs.getBoolean(PreferencesActivity.KEY_COPY_TO_CLIPBOARD, true);

        source = IntentSource.NONE;
        sourceUrl = null;
        decodeFormats = null;
        characterSet = null;

        if (intent != null) {

            String action = intent.getAction();
            String dataString = intent.getDataString();

            if (Intents.Scan.ACTION.equals(action)) {

                // Scan the formats the intent requested, and return the result to the calling activity.
                source = IntentSource.NATIVE_APP_INTENT;
                decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
                decodeHints = DecodeHintManager.parseDecodeHints(intent);

                if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
                    int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
                    int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
                    if (width > 0 && height > 0) {
                        cameraManager.setManualFramingRect(width, height);
                    }
                }

                if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
                    int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
                    if (cameraId >= 0) {
                        cameraManager.setManualCameraId(cameraId);
                    }
                }

                String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
                if (customPromptMessage != null) {
                    statusView.setText(customPromptMessage);
                }

            } else if (dataString != null &&
                    dataString.contains("http://www.google") &&
                    dataString.contains("/m/products/scan")) {

                // Scan only products and send the result to mobile Product Search.
                source = IntentSource.PRODUCT_SEARCH_LINK;
                sourceUrl = dataString;
                decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;

            }

            characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

        }

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            this.surfaceHolder = surfaceHolder;
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    private int getCurrentOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
        } else {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            }
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        soundManager.close();
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        for(Map.Entry<String, DatabaseReference> entry : mSyncedDatabaseRefs.entrySet()) {
            entry.getValue().keepSynced(false);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (source == IntentSource.NATIVE_APP_INTENT) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                }
                if (source == IntentSource.NONE && lastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.capture, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        switch (item.getItemId()) {
            case R.id.menu_history:
                intent.setClassName(this, HistoryActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.menu_settings:
                intent.setClassName(this, PreferencesActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.menu_help:
                intent.setClassName(this, HelpActivity.class.getName());
                startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            this.surfaceHolder = holder;
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CAMERA_PERMISSION) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, android.Manifest.permission.CAMERA)) {
            // Init camera if the permission has been granted.
            initCamera(surfaceHolder);
        } else {
            PermissionUtils.PermissionDeniedDialog.newInstance(false, R.string.location_permission_denied).show(getSupportFragmentManager(), "dialog");
        }
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(final Result rawResult, final Bitmap barcode) {
        inactivityTimer.onActivity();
        lastResult = rawResult;

        final ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
        final String itemId = resultHandler.getDisplayContents();

//    if(!NetworkUtils.isNetworkAvailable(this)) {
//      DialogUtils.showErrorDialog(this, getString(R.string.network_connection_required),
//              getString(R.string.add_item_connection_explanation));
//      return;
//    }

        DatabaseReference itemReference = mDatabaseReference.child("items").child(itemId);
        syncDatabaseReference(itemReference);
        itemReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean fromLiveScan = barcode != null;
                Item item = null;

                //TODO
                if (dataSnapshot.exists()) {
                    ItemType type = ItemType.valueOf(dataSnapshot.child("type").getValue(String.class));
                    switch (type) {
                        case PRODUCT:
                            item = dataSnapshot.getValue(Product.class);
                            break;
                        case ISBN:
                            item = dataSnapshot.getValue(ISBNItem.class);
                            break;
                        case VIN:
                            item = dataSnapshot.getValue(VINItem.class);
                            break;
                        default:
                            item = dataSnapshot.getValue(Item.class);
                    }
                    item.setId(itemId);
                    item.setTimestamp(rawResult.getTimestamp());

                    if (fromLiveScan) {
                        // Not from history, so play sound / vibrate
                        soundManager.playSoundAndVibrate();
                        historyManager.addHistoryItem(item, HistoryEventType.ITEM_SCANNED, resultHandler);
                    }
                }

                switch (source) {
                    case NATIVE_APP_INTENT:
                    case PRODUCT_SEARCH_LINK:
                        handleDecodeExternally(rawResult, resultHandler, barcode);
                        break;
                    case NONE:
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ItemfinderCaptureActivity.this);
                        Intent intent = getIntent();
                        boolean returnCapturedItem = intent != null && intent.getBooleanExtra(com.manouti.itemfinder.Intents.RETURN_CAPTURED_ITEM_TO_ACTIVITY, false);
                        if (fromLiveScan && !returnCapturedItem && prefs.getBoolean(PreferencesActivity.KEY_BULK_MODE, false)) {
                            Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.msg_bulk_mode_scanned) + " (" + rawResult.getText() + ')',
                                    Toast.LENGTH_SHORT).show();
                            // Wait a moment or else it will scan the same barcode continuously about 3 times
                            restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
                        } else {
                            handleDecodeInternally(item, rawResult, barcode, returnCapturedItem);
                        }
                        break;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getItem:onCancelled", databaseError.toException());
            }
        });
    }

    // Put up our own UI for how to handle the decoded contents.
    private void handleDecodeInternally(Item item, Result rawResult, Bitmap barcode, boolean returnCapturedItem) {

        statusView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        if(returnCapturedItem) {
            Intent data = new Intent();
            if(item != null) {
                data.putExtra(com.manouti.itemfinder.Intents.RETURNED_ITEM_INFO_FROM_SCAN, item);
            } else {
                data.putExtra(com.manouti.itemfinder.Intents.SCANNED_ITEM_PARCELABLE_BARCODE, new ParcelableBarcodeResult(rawResult));
            }
            setResult(RESULT_OK, data);
            finish();
        } else {
            if(item != null) {
                Intent itemDetailIntent = new Intent(this, ItemDetailActivity.class);
                itemDetailIntent.putExtra(com.manouti.itemfinder.Intents.ITEM_DETAIL_ACTIVITY_ID_INPUT, item.getId());
                startActivity(itemDetailIntent);
            } else {
                //TODO handle non-existing items
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
            }
        }
    }

    // Briefly show the contents of the barcode, then handle the result outside Barcode Scanner.
    private void handleDecodeExternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {

        if (barcode != null) {
            viewfinderView.drawResultBitmap(barcode);
        }

        long resultDurationMS;
        if (getIntent() == null) {
            resultDurationMS = DEFAULT_INTENT_RESULT_DURATION_MS;
        } else {
            resultDurationMS = getIntent().getLongExtra(Intents.Scan.RESULT_DISPLAY_DURATION_MS,
                    DEFAULT_INTENT_RESULT_DURATION_MS);
        }

        if (resultDurationMS > 0) {
            String rawResultString = String.valueOf(rawResult);
            if (rawResultString.length() > 32) {
                rawResultString = rawResultString.substring(0, 32) + " ...";
            }
            statusView.setText(getString(resultHandler.getDisplayTitle()) + " : " + rawResultString);
        }

        if (copyToClipboard && !resultHandler.areContentsSecure()) {
            CharSequence text = resultHandler.getDisplayContents();
            ClipboardInterface.setText(text, this);
        }

        if (source == IntentSource.NATIVE_APP_INTENT) {

            // Hand back whatever action they requested - this can be changed to Intents.Scan.ACTION when
            // the deprecated intent is retired.
            Intent intent = new Intent(getIntent().getAction());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
            intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
            byte[] rawBytes = rawResult.getRawBytes();
            if (rawBytes != null && rawBytes.length > 0) {
                intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
            }
            Map<ResultMetadataType,?> metadata = rawResult.getResultMetadata();
            if (metadata != null) {
                if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
                    intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION,
                            metadata.get(ResultMetadataType.UPC_EAN_EXTENSION).toString());
                }
                Number orientation = (Number) metadata.get(ResultMetadataType.ORIENTATION);
                if (orientation != null) {
                    intent.putExtra(Intents.Scan.RESULT_ORIENTATION, orientation.intValue());
                }
                String ecLevel = (String) metadata.get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
                if (ecLevel != null) {
                    intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL, ecLevel);
                }
                @SuppressWarnings("unchecked")
                Iterable<byte[]> byteSegments = (Iterable<byte[]>) metadata.get(ResultMetadataType.BYTE_SEGMENTS);
                if (byteSegments != null) {
                    int i = 0;
                    for (byte[] byteSegment : byteSegments) {
                        intent.putExtra(Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i, byteSegment);
                        i++;
                    }
                }
            }
            sendReplyMessage(R.id.return_scan_result, intent, resultDurationMS);

        } else if (source == IntentSource.PRODUCT_SEARCH_LINK) {

            // Reformulate the URL which triggered us into a query, so that the request goes to the same
            // TLD as the scan URL.
            int end = sourceUrl.lastIndexOf("/scan");
            String replyURL = sourceUrl.substring(0, end) + "?q=" + resultHandler.getDisplayContents() + "&source=zxing";
            sendReplyMessage(R.id.launch_product_query, replyURL, resultDurationMS);

        }
    }

    private void sendReplyMessage(int id, Object arg, long delayMS) {
        if (handler != null) {
            Message message = Message.obtain(handler, id, arg);
            if (delayMS > 0L) {
                handler.sendMessageDelayed(message, delayMS);
            } else {
                handler.sendMessage(message);
            }
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the camera is missing.
            PermissionUtils.PermissionRequest permissionRequest = new PermissionUtils.PermissionRequestBuilder(this, REQUEST_CAMERA_PERMISSION, Manifest.permission.CAMERA, true)
                    .rationalStringResId(R.string.permission_rationale_camera).permissionRequiredInfoResId(R.string.camera_permission_required_info)
                    .build();
            PermissionUtils.requestPermission(permissionRequest);
        } else {
            // Access to the camera has been granted to the app
            try {
                cameraManager.openDriver(surfaceHolder);
                // Creating the handler starts the preview, which can also throw a RuntimeException.
                if (handler == null) {
                    handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
                }
                decodeOrStoreSavedBitmap(null, null);
            } catch (IOException ioe) {
                Log.w(TAG, ioe);
                displayFrameworkBugMessageAndExit();
            } catch (RuntimeException e) {
                // Barcode Scanner has seen crashes in the wild of this variety:
                // java.?lang.?RuntimeException: Fail to connect to camera service
                Log.w(TAG, "Unexpected error initializing camera", e);
                displayFrameworkBugMessageAndExit();
            }
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
        resetStatusView();
    }

    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    public void syncDatabaseReference(DatabaseReference databaseReference) {
        databaseReference.keepSynced(true);
        mSyncedDatabaseRefs.put(databaseReference.toString(), databaseReference);
    }
}
