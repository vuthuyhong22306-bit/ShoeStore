package com.example.shoestore.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.shoestore.R;
import com.example.shoestore.ui.activity.CustomerLoginActivity;
import com.example.shoestore.ui.activity.OrderDetailActivity;
import com.example.shoestore.ui.activity.RoleSelectionActivity;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Notification";
    public static final String CHANNEL_ID = "new_order_channel";
    public static final String CHANNEL_NAME = "Thông báo đơn hàng mới";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM Token: " + token);
        // Có thể lưu token lên Firebase Realtime Database hoặc Server tại đây
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from FCM: " + remoteMessage.getData());

        String title = "Đơn hàng mới!";
        String body = "Có đơn hàng mới vừa được khởi tạo.";
        int orderId = -1;

        // 1. Lấy dữ liệu từ Notification payload (nếu có)
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        // 2. Lấy dữ liệu từ Data payload (cho cả Foreground, Background & Killed state)
        Map<String, String> data = remoteMessage.getData();
        if (data.containsKey("title")) {
            title = data.get("title");
        }
        if (data.containsKey("body")) {
            body = data.get("body");
        } else if (data.containsKey("order_content")) {
            body = data.get("order_content");
        }

        if (data.containsKey("order_id")) {
            try {
                orderId = Integer.parseInt(data.get("order_id"));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Lỗi ép kiểu order_id: " + e.getMessage());
            }
        } else if (data.containsKey("ORDER_ID")) {
            try {
                orderId = Integer.parseInt(data.get("ORDER_ID"));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Lỗi ép kiểu ORDER_ID: " + e.getMessage());
            }
        }

        // Hiển thị thông báo trên màn hình
        showNotification(this, title, body, orderId);
    }

    /**
     * Hàm khởi tạo NotificationChannel (Android 8.0+) & tạo Notification với âm thanh, rung, màn hình khóa.
     */
    public static void showNotification(Context context, String title, String body, int orderId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        long[] vibrationPattern = new long[]{0, 500, 250, 500};

        // Tạo NotificationChannel cho Android 8.0 (API 26) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Kênh nhận thông báo về đơn hàng mới cho hệ thống ShoeStore");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(vibrationPattern);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(defaultSoundUri, audioAttributes);

            notificationManager.createNotificationChannel(channel);
        }

        // Kiểm tra trạng thái đăng nhập để cấu hình PendingIntent điều hướng
        Intent intent;
        SharedPrefManager pref = SharedPrefManager.getInstance(context);

        if (pref.isLoggedIn()) {
            // Đã đăng nhập -> Chuyển trực tiếp sang màn hình Chi tiết đơn hàng
            intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", orderId);
        } else {
            // Chưa đăng nhập -> Chuyển sang màn hình Đăng nhập / Chọn vai trò và truyền order_id
            intent = new Intent(context, RoleSelectionActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            intent.putExtra("PENDING_NOTIF_ORDER_ID", orderId);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int requestCode = (int) System.currentTimeMillis();
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                pendingIntentFlags
        );

        // Xây dựng Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orders)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setSound(defaultSoundUri)
                .setVibrate(vibrationPattern)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        int notificationId = orderId > 0 ? orderId : (int) System.currentTimeMillis();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
        }
    }
}
