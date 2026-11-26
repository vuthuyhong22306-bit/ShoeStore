package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.model.NotificationItem;
import com.example.shoestore.repository.NotificationRepository;
import com.example.shoestore.ui.adapter.NotificationAdapter;
import com.example.shoestore.utils.SharedPrefManager;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {
    private NotificationRepository notificationRepository;
    private NotificationAdapter adapter;
    private String currentUserId;
    private String currentRole;

    private RecyclerView rvNotifications;
    private View layoutEmpty;
    private ImageView btnBack;
    private TextView btnMarkAllRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        btnBack = findViewById(R.id.btnBack);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        notificationRepository = new NotificationRepository(getApplication());
        currentUserId = String.valueOf(SharedPrefManager.getInstance(this).getUserId());
        currentRole = SharedPrefManager.getInstance(this).getUserRole();

        adapter = new NotificationAdapter(this);
        if (rvNotifications != null) {
            rvNotifications.setAdapter(adapter);
        }

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                notificationRepository.markAllAsRead(currentUserId, currentRole);
                Toast.makeText(this, "Đã đánh dấu đọc tất cả thông báo", Toast.LENGTH_SHORT).show();
            });
        }

        notificationRepository.getNotificationsForUser(currentUserId, currentRole).observe(this, notifications -> {
            if (notifications != null && !notifications.isEmpty()) {
                adapter.setNotifications(notifications);
                if (rvNotifications != null) rvNotifications.setVisibility(View.VISIBLE);
                if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            } else {
                adapter.setNotifications(null);
                if (rvNotifications != null) rvNotifications.setVisibility(View.GONE);
                if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onNotificationClick(NotificationItem notification) {
        if (notification == null) return;
        notificationRepository.markAsRead(notification.getId());

        if (notification.getOrderId() > 0) {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("ORDER_ID", notification.getOrderId());
            startActivity(intent);
        }
    }

    @Override
    public void onNotificationDelete(NotificationItem notification) {
        if (notification == null) return;
        notificationRepository.deleteNotification(notification.getId());
    }
}
