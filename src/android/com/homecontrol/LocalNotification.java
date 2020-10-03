package com.homecontrol;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import io.ionic.starter.R;


public class LocalNotification {

    String CHANNEL_ID = "com.cordic.app";
    CharSequence notification_name = "CORDIC Notification";


    public void sendNotification(Context context, String text) {
        Log.d("localNotification****","Inside");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_HIGH;
            android.app.NotificationChannel channel = new android.app.NotificationChannel(CHANNEL_ID,notification_name, importance);
            channel.setDescription(text);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Cordic has a notification"))
                .setContentText("Click to view the activity"
                )
                .setChannelId(CHANNEL_ID);
        if (Build.VERSION.SDK_INT >= 21) mBuilder.setVibrate(new long[0]);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(001, mBuilder.build());
    }
}
