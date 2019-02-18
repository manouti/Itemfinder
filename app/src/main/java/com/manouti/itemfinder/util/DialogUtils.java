package com.manouti.itemfinder.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.manouti.itemfinder.R;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class DialogUtils {
    public static void runAfterConfirm(Context context, final OnConfirmOperation onConfirmOperation) {
        runAfterConfirm(context, onConfirmOperation, R.string.msg_sure);
    }

    public static void runAfterConfirm(Context context, final OnConfirmOperation onConfirmOperation, int confirmMessageResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(confirmMessageResId);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i2) {
                onConfirmOperation.runOperation();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.show();
    }

    public static void showMessageDialog(Context context, String titleMessage, String contentMessage, String confirmMessage, final SweetAlertDialog.OnSweetClickListener onSweetClickListener) {
        final SweetAlertDialog alertDialog = new SweetAlertDialog(context)
                .setTitleText(titleMessage)
                .setContentText(contentMessage)
                .setConfirmText(confirmMessage);
        if(onSweetClickListener != null) {
            alertDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog) {
                    alertDialog.dismissWithAnimation();
                    onSweetClickListener.onClick(sweetAlertDialog);
                }
            });
        }
        alertDialog.show();
    }

    public static void showSuccessDialog(Context context, String successTitleMessage, String successContentMessage, final SweetAlertDialog.OnSweetClickListener onSweetClickListener) {
        final SweetAlertDialog alertDialog = new SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText(successTitleMessage)
                .setContentText(successContentMessage);
        if(onSweetClickListener != null) {
            alertDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog) {
                    alertDialog.dismissWithAnimation();
                    onSweetClickListener.onClick(sweetAlertDialog);
                }
            });
        }
        alertDialog.show();
    }

    public static void showErrorDialog(Context context, String errorTitleMessage, String errorContentMessage) {
        showErrorDialog(context, errorTitleMessage, errorContentMessage, null);
    }

    public static void showErrorDialog(Context context, String errorTitleMessage, String errorContentMessage, final SweetAlertDialog.OnSweetClickListener onSweetClickListener) {
        final SweetAlertDialog errorDialog = new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(errorTitleMessage)
                .setContentText(errorContentMessage);
        if(onSweetClickListener != null) {
            errorDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog) {
                    errorDialog.dismissWithAnimation();
                    onSweetClickListener.onClick(sweetAlertDialog);
                }
            });
        }
        errorDialog.show();
    }

    public static SweetAlertDialog showProgressDialog(Context context, String titleMessage, String contentMessage) {
        SweetAlertDialog sweetAlertDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE)
                .setTitleText(titleMessage)
                .setContentText(contentMessage);
        sweetAlertDialog.getProgressHelper().setBarColor(R.color.colorSweetDialogProgress);
        sweetAlertDialog.show();
        return sweetAlertDialog;
    }

    public interface OnConfirmOperation {
        void runOperation();
    }
}
