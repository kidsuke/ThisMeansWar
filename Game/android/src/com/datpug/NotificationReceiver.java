package com.datpug;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.datpug.NotificationController.NOTIFICATION_ID;

/**
 * Created by phocphoc on 04/10/2017.
 */

public class NotificationReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationController.showNotification(context);
    }
}
