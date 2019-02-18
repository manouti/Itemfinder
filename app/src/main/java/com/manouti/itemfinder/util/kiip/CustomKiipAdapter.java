package com.manouti.itemfinder.util.kiip;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.manouti.itemfinder.R;

import me.kiip.sdk.Kiip;
import me.kiip.sdk.Notification;
import me.kiip.sdk.Poptart;


public class CustomKiipAdapter implements Kiip.KiipAdapter {

    @Override
    public View getNotificationView(Context context, ViewGroup parent, Poptart poptart) {
        // Note: make sure you pass in parent and attachedToRoot=false so that the view's
        // width and height are correctly inflated.
        CustomNotificationView view = (CustomNotificationView) LayoutInflater.from(context).inflate(R.layout.kiip_custom_notification, parent, false);

        // Set custom notification properties.
        Notification notification = poptart.getNotification();
        view.setIcon(notification.getIcon());
        view.setTitle(notification.getTitle());
        view.setMessage(notification.getMessage());

        return view;
    }

}
