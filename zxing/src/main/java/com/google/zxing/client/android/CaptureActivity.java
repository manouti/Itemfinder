package com.google.zxing.client.android;

import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;

import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * Created by Mahmoud.
 */
public abstract class CaptureActivity extends FragmentActivity {

    protected CaptureActivityHandler handler;
    protected ViewfinderView viewfinderView;
    protected CameraManager cameraManager;

    public Handler getHandler() {
        return handler;
    }

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public abstract void handleDecode(final Result rawResult, final Bitmap barcode);

}
