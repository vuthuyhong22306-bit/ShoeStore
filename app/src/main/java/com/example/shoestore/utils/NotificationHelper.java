package com.example.shoestore.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.shoestore.R;
import com.example.shoestore.ui.activity.OrderDetailActivity;

public class NotificationHelper {
    public static final String CHANNEL_ID = "shoestore_notifications_channel";
    public static final String CHANNEL_NAME = "Thông báo ShoeStore";
    public static final String CHANNEL_DESC = "Kênh thông báo đơn hàng và hệ thống ShoeStore";

    @android.annotation.SuppressLint("NotificationPermission")
    public static void showNotification(Context context, String title, String content, int orderId) {
        if (context == null) return;

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        Intent intent;
        if (orderId > 0) {
            intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", orderId);
        } else {
            intent = new Intent(context, com.example.shoestore.ui.activity.CustomerMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            int notificationId = (int) (System.currentTimeMillis() % 10000);
            manager.notify(notificationId, builder.build());
        } catch (Exception ignored) {
        }
    }
}
