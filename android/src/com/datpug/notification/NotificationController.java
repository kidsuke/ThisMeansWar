package com.datpug.notification;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.datpug.presentation.menu.MenuActivity;
import com.datpug.R;

/**
 * Created by phocphoc on 04/10/2017.
 */

public class NotificationController {
    public static final long REMINDER_TIME = 1800000;
    public static final int NOTIFICATION_ID = 0x0001;

    private NotificationController() {}

    public static void setReminder(Context context) {
        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + REMINDER_TIME;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    public static void cancelReminder(Context context) {
        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    @TargetApi(19)
    public static void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MenuActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context);
        builder
        .setContentTitle(context.getString(R.string.noti_title))
        .setContentText(context.getString(R.string.noti_text))
        .setSmallIcon(R.drawable.ic_launcher)
        .setPriority(Notification.PRIORITY_MAX)
        .setDefaults(Notification.DEFAULT_ALL)
        .setContentIntent(pendingIntent);
        Notification notification = builder.build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
