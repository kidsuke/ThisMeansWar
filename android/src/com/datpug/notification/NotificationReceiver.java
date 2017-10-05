package com.datpug.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.datpug.notification.NotificationController;

/**
 * Created by phocphoc on 04/10/2017.
 */

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationController.showNotification(context);
    }
}
