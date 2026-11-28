package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.model.NotificationItem;
import com.example.shoestore.repository.NotificationRepository;
import com.example.shoestore.ui.adapter.NotificationAdapter;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.android.material.snackbar.Snackbar;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {
    private NotificationRepository notificationRepository;
    private NotificationAdapter adapter;
    private String currentUserId;
    private String currentRole;

    private RecyclerView rvNotifications;
    private View layoutEmpty;
    private ImageView btnBack;
    private TextView btnMarkAllRead;
    private TextView btnDeleteAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SharedPrefManager.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem thông báo", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_TARGET_ACTION, "NOTIFICATIONS");
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_notification);

        btnBack = findViewById(R.id.btnBack);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);
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
            setupSwipeToDelete();
        }

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                notificationRepository.markAllAsRead(currentUserId, currentRole);
                Toast.makeText(this, "Đã đánh dấu đọc tất cả thông báo", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnDeleteAll != null) {
            btnDeleteAll.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Xóa tất cả thông báo")
                        .setMessage("Bạn có chắc chắn muốn xóa tất cả thông báo không?")
                        .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                            notificationRepository.deleteAllForUser(currentUserId, currentRole);
                            Toast.makeText(this, "Đã xóa tất cả thông báo", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
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

    private void setupSwipeToDelete() {
        if (rvNotifications == null) return;

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    position = viewHolder.getAdapterPosition();
                }

                NotificationItem item = adapter.getItemAt(position);
                if (item != null) {
                    notificationRepository.deleteNotification(item.getId());
                    Snackbar.make(findViewById(android.R.id.content), "Đã xóa thông báo", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    Paint paint = new Paint();
                    paint.setColor(Color.parseColor("#E5192C"));
                    float cornerRadius = 14 * getResources().getDisplayMetrics().density;

                    RectF background = new RectF(
                            itemView.getRight() + dX,
                            itemView.getTop() + 12,
                            itemView.getRight(),
                            itemView.getBottom() - 12
                    );
                    c.drawRoundRect(background, cornerRadius, cornerRadius, paint);

                    Drawable icon = ContextCompat.getDrawable(NotificationActivity.this, android.R.drawable.ic_menu_delete);
                    if (icon != null) {
                        icon.setTint(Color.WHITE);
                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();
                        int iconRight = itemView.getRight() - iconMargin;
                        int iconLeft = iconRight - icon.getIntrinsicWidth();

                        if (itemView.getRight() + dX < iconLeft) {
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                            icon.draw(c);
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(rvNotifications);
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
