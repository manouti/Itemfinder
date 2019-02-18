/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.manouti.itemfinder.util;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.manouti.itemfinder.R;

/**
 * Utility class for access to runtime permissions.
 */
public abstract class PermissionUtils {

    /**
     * Requests the fine location permission. If a rationale with an additional explanation should
     * be shown to the user, displays a dialog that triggers the request.
     */
    public static void requestPermission(FragmentActivity activity, int requestId,
            String permission, boolean finishActivity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            // Display a dialog with rationale.
            PermissionUtils.RationaleDialog.newInstance(requestId, finishActivity)
                    .show(activity.getSupportFragmentManager(), "dialog");
        } else {
            // Location permission has not been granted yet, request it.
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestId);
        }
    }

    /**
     * Requests a permission. If a rationale with an additional explanation should
     * be shown to the user, displays a dialog that triggers the request.
     */
    public static void requestPermission(PermissionRequest permissionRequest) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(permissionRequest.activity, permissionRequest.permission)) {
            // Display a dialog with rationale.
            PermissionUtils.RationaleDialog.newInstance(permissionRequest.requestId, permissionRequest.permission, permissionRequest.finishActivity,
                    permissionRequest.rationalStringResId, permissionRequest.permissionRequiredInfoResId)
                    .show(permissionRequest.activity.getSupportFragmentManager(), "dialog");
        } else {
            // Permission has not been granted yet, request it.
            ActivityCompat.requestPermissions(permissionRequest.activity, new String[] { permissionRequest.permission }, permissionRequest.requestId);
        }
    }

    /**
     * Checks if the result contains a {@link PackageManager#PERMISSION_GRANTED} result for a
     * permission from a runtime permissions request.
     *
     * @see android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback
     */
    public static boolean isPermissionGranted(String[] grantPermissions, int[] grantResults,
            String permission) {
        for (int i = 0; i < grantPermissions.length; i++) {
            if (permission.equals(grantPermissions[i])) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

    public static class PermissionRequest {
        private final FragmentActivity activity;
        private final int requestId;
        private final String permission;
        private final boolean finishActivity;
        private final int rationalStringResId;
        private final int permissionRequiredInfoResId;

        private PermissionRequest(FragmentActivity activity, int requestId,
                                  String permission, boolean finishActivity,
                                  int rationalStringResId, int permissionRequiredInfoResId) {
            this.activity = activity;
            this.requestId = requestId;
            this.permission = permission;
            this.finishActivity = finishActivity;
            this.rationalStringResId = rationalStringResId;
            this.permissionRequiredInfoResId = permissionRequiredInfoResId;
        }
    }

    public static class PermissionRequestBuilder {
        private final FragmentActivity activity;
        private final int requestId;
        private final String permission;
        private final boolean finishActivity;
        private int rationalStringResId;
        private int permissionRequiredInfoResId;

        public PermissionRequestBuilder(FragmentActivity activity, int requestId,
                                      String permission, boolean finishActivity) {

            this.activity = activity;
            this.requestId = requestId;
            this.permission = permission;
            this.finishActivity = finishActivity;
        }

        public PermissionRequestBuilder rationalStringResId(int rationalStringResId) {
            this.rationalStringResId = rationalStringResId;
            return this;
        }

        public PermissionRequestBuilder permissionRequiredInfoResId(int permissionRequiredInfoResId) {
            this.permissionRequiredInfoResId = permissionRequiredInfoResId;
            return this;
        }

        public PermissionRequest build() {
            return new PermissionRequest(activity, requestId, permission, finishActivity, rationalStringResId, permissionRequiredInfoResId);
        }
    }

    /**
     * A dialog that displays a permission denied message.
     */
    public static class NativePermissionDeniedDialog extends android.app.DialogFragment {

        private static final String ARGUMENT_FINISH_ACTIVITY = "finish";
        private static final String ARGUMENT_PERMISSION_DENIED_MESSAGE = "permissionDeniedMsg";

        private boolean mFinishActivity = false;

        /**
         * Creates a new instance of this dialog and optionally finishes the calling Activity
         * when the 'Ok' button is clicked.
         */
        public static NativePermissionDeniedDialog newInstance(boolean finishActivity) {
            return newInstance(finishActivity, 0);
        }

        public static NativePermissionDeniedDialog newInstance(boolean finishActivity, int permissionDeniedMessageResId) {
            Bundle arguments = new Bundle();
            arguments.putBoolean(ARGUMENT_FINISH_ACTIVITY, finishActivity);
            arguments.putInt(ARGUMENT_PERMISSION_DENIED_MESSAGE, permissionDeniedMessageResId);

            NativePermissionDeniedDialog dialog = new NativePermissionDeniedDialog();
            dialog.setArguments(arguments);
            return dialog;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mFinishActivity = getArguments().getBoolean(ARGUMENT_FINISH_ACTIVITY);
            final int permissionDeniedMsgResId = getArguments().getInt(ARGUMENT_PERMISSION_DENIED_MESSAGE);

            return new AlertDialog.Builder(getActivity())
                    .setMessage(permissionDeniedMsgResId != 0 ? permissionDeniedMsgResId : R.string.location_permission_denied)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mFinishActivity) {
                Toast.makeText(getActivity(), R.string.permission_required_toast,
                        Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
    }

    /**
     * A dialog that displays a permission denied message.
     */
    public static class PermissionDeniedDialog extends DialogFragment {

        private static final String ARGUMENT_FINISH_ACTIVITY = "finish";
        private static final String ARGUMENT_PERMISSION_DENIED_MESSAGE = "permissionDeniedMsg";

        private boolean mFinishActivity = false;

        /**
         * Creates a new instance of this dialog and optionally finishes the calling Activity
         * when the 'Ok' button is clicked.
         */
        public static PermissionDeniedDialog newInstance(boolean finishActivity) {
            return newInstance(finishActivity, 0);
        }

        public static PermissionDeniedDialog newInstance(boolean finishActivity, int permissionDeniedMessageResId) {
            Bundle arguments = new Bundle();
            arguments.putBoolean(ARGUMENT_FINISH_ACTIVITY, finishActivity);
            arguments.putInt(ARGUMENT_PERMISSION_DENIED_MESSAGE, permissionDeniedMessageResId);

            PermissionDeniedDialog dialog = new PermissionDeniedDialog();
            dialog.setArguments(arguments);
            return dialog;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mFinishActivity = getArguments().getBoolean(ARGUMENT_FINISH_ACTIVITY);
            final int permissionDeniedMsgResId = getArguments().getInt(ARGUMENT_PERMISSION_DENIED_MESSAGE);

            return new AlertDialog.Builder(getActivity())
                    .setMessage(permissionDeniedMsgResId != 0 ? permissionDeniedMsgResId : R.string.location_permission_denied)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (mFinishActivity) {
                Toast.makeText(getActivity(), R.string.permission_required_toast,
                        Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
    }

    /**
     * A dialog that explains the use of the permission and requests the necessary
     * permission.
     * <p>
     * The activity should implement
     * {@link android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback}
     * to handle permit or denial of this permission request.
     */
    public static class RationaleDialog extends DialogFragment {

        private static final String ARGUMENT_PERMISSION_REQUEST_CODE = "requestCode";
        private static final String ARGUMENT_PERMISSION = "permission";
        private static final String ARGUMENT_FINISH_ACTIVITY = "finish";
        private static final String ARGUMENT_RATIONALE_STRING = "rationalStringResId";
        private static final String ARGUMENT_PERMISSION_REQUIRED_INFO_STRING = "permissionRequiredInfoResId";

        private boolean mFinishActivity = false;

        /**
         * Creates a new instance of a dialog displaying the rationale for the use of the permission.
         * <p>
         * The permission is requested after clicking 'ok'.
         *
         * @param requestCode    Id of the request that is used to request the permission. It is
         *                       returned to the
         *                       {@link android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback}.
         * @param finishActivity Whether the calling Activity should be finished if the dialog is
         *                       cancelled.
         */
        public static RationaleDialog newInstance(int requestCode, boolean finishActivity) {
            return newInstance(requestCode, null, finishActivity, 0, 0);
        }

        public static RationaleDialog newInstance(int requestCode, String permission, boolean finishActivity, int rationalStringResId, int permissionRequiredInfoResId) {
            Bundle arguments = new Bundle();
            arguments.putInt(ARGUMENT_PERMISSION_REQUEST_CODE, requestCode);
            arguments.putString(ARGUMENT_PERMISSION, permission);
            arguments.putBoolean(ARGUMENT_FINISH_ACTIVITY, finishActivity);
            arguments.putInt(ARGUMENT_RATIONALE_STRING, rationalStringResId);
            arguments.putInt(ARGUMENT_PERMISSION_REQUIRED_INFO_STRING, permissionRequiredInfoResId);
            RationaleDialog dialog = new RationaleDialog();
            dialog.setArguments(arguments);
            return dialog;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle arguments = getArguments();
            final String permission = arguments.getString(ARGUMENT_PERMISSION);
            final int requestCode = arguments.getInt(ARGUMENT_PERMISSION_REQUEST_CODE);
            final int rationalStringResId = arguments.getInt(ARGUMENT_RATIONALE_STRING);
            mFinishActivity = arguments.getBoolean(ARGUMENT_FINISH_ACTIVITY);

            return new AlertDialog.Builder(getActivity())
                    .setMessage(rationalStringResId != 0 ? rationalStringResId : R.string.permission_rationale_location)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // After click on Ok, request the permission.
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{permission != null ? permission : Manifest.permission.ACCESS_FINE_LOCATION},
                                    requestCode);
                            // Do not finish the Activity while requesting permission.
                            mFinishActivity = false;
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            final int permissionRequiredInfoResId = getArguments().getInt(ARGUMENT_PERMISSION_REQUIRED_INFO_STRING);
            if (mFinishActivity) {
                Toast.makeText(getActivity(),
                        permissionRequiredInfoResId != 0 ? permissionRequiredInfoResId : R.string.permission_required_toast,
                        Toast.LENGTH_SHORT)
                        .show();
                getActivity().finish();
            }
        }
    }
}
