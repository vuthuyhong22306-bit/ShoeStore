package com.example.shoestore.repository;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.shoestore.dao.NotificationDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.NotificationItem;
import com.example.shoestore.model.Order;
import com.example.shoestore.utils.NotificationHelper;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class NotificationRepository {
    private static final String TAG = "NotificationRepository";
    private final NotificationDao notificationDao;
    private final ExecutorService executor;
    private final Context context;
    private static final Set<String> shownNotificationIds = new HashSet<>();

    public NotificationRepository(Application application) {
        this.context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(application);
        this.notificationDao = db.notificationDao();
        this.executor = AppDatabase.databaseWriteExecutor;

        initFirebaseSync();
    }

    private void initFirebaseSync() {
        // 1. Firebase Realtime Database Sync for Notifications
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications");
        notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                executor.execute(() -> {
                    List<NotificationItem> syncedList = new ArrayList<>();
                    String currentUserId = String.valueOf(SharedPrefManager.getInstance(context).getUserId());
                    String currentRole = SharedPrefManager.getInstance(context).getUserRole();
                    if (currentRole == null) currentRole = "CUSTOMER";

                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            NotificationItem item = child.getValue(NotificationItem.class);
                            if (item != null) {
                                if (item.getId() == null || item.getId().isEmpty()) {
                                    item.setId(child.getKey() != null ? child.getKey() : String.valueOf(System.currentTimeMillis()));
                                }
                                syncedList.add(item);

                                // Check if notification is for current user/role and unread -> trigger Android System Notification!
                                boolean isForMe = matchesUserOrRole(item, currentUserId, currentRole);
                                if (isForMe && !item.isRead() && !shownNotificationIds.contains(item.getId())) {
                                    shownNotificationIds.add(item.getId());
                                    NotificationHelper.showNotification(context, item.getTitle(), item.getContent(), item.getOrderId());
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification snapshot: " + e.getMessage());
                        }
                    }

                    if (!syncedList.isEmpty()) {
                        notificationDao.insertNotifications(syncedList);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Notification Realtime Database sync cancelled: " + error.getMessage());
            }
        });

        // 2. Cloud Firestore Realtime Sync
        try {
            FirebaseFirestore.getInstance().collection("notifications").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (error != null || value == null) return;
                    executor.execute(() -> {
                        List<NotificationItem> syncedList = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                NotificationItem item = doc.toObject(NotificationItem.class);
                                if (item != null) {
                                    if (item.getId() == null || item.getId().isEmpty()) {
                                        item.setId(doc.getId());
                                    }
                                    syncedList.add(item);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing Firestore notification: " + e.getMessage());
                            }
                        }
                        if (!syncedList.isEmpty()) {
                            notificationDao.insertNotifications(syncedList);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Firestore notification listener error: " + e.getMessage());
        }
    }

    private boolean matchesUserOrRole(NotificationItem item, String userId, String role) {
        if (item == null) return false;
        String tUser = item.getTargetUserId();
        String tRole = item.getTargetRole();

        if ("ALL".equalsIgnoreCase(tRole) || "ALL".equalsIgnoreCase(tUser)) return true;

        if (tUser != null && !tUser.isEmpty() && tUser.equalsIgnoreCase(userId)) {
            return true;
        }

        if (tRole != null && !tRole.isEmpty() && tRole.equalsIgnoreCase(role)) {
            return true;
        }

        return false;
    }

    public LiveData<List<NotificationItem>> getNotificationsForUser(String userId, String role) {
        return notificationDao.getNotificationsForUser(userId, role != null ? role.toUpperCase() : "CUSTOMER");
    }

    public LiveData<Integer> getUnreadCountForUser(String userId, String role) {
        return notificationDao.getUnreadCountForUser(userId, role != null ? role.toUpperCase() : "CUSTOMER");
    }

    public void sendNotification(NotificationItem item) {
        executor.execute(() -> {
            if (item.getId() == null || item.getId().isEmpty()) {
                item.setId(String.valueOf(System.currentTimeMillis()) + "_" + (int)(Math.random() * 1000));
            }

            notificationDao.insert(item);

            // Write to Firebase Realtime Database
            FirebaseDatabase.getInstance()
                    .getReference("notifications")
                    .child(item.getId())
                    .setValue(item);

            // Write to Cloud Firestore
            try {
                FirebaseFirestore.getInstance()
                        .collection("notifications")
                        .document(item.getId())
                        .set(item);
            } catch (Exception e) {
                Log.e(TAG, "Error writing notification to Firestore: " + e.getMessage());
            }
        });
    }

    public void sendOrderPlacedNotifications(Order order) {
        if (order == null) return;
        DecimalFormat df = new DecimalFormat("#,###");
        String priceStr = df.format(order.getTotalAmount()).replace(',', '.') + "đ";
        String customerName = order.getReceiverName() != null ? order.getReceiverName() : "Khách hàng";

        long now = System.currentTimeMillis();

        // 1. Notification for Staff
        NotificationItem staffNotif = new NotificationItem(
                "notif_staff_" + order.getId() + "_" + now,
                "Có đơn hàng mới!",
                "Khách hàng " + customerName + " vừa đặt một đơn hàng mới #DH" + String.format("%04d", order.getId()) + " (" + priceStr + ").",
                order.getId(),
                "STAFF",
                "",
                now,
                false,
                "NEW_ORDER"
        );
        sendNotification(staffNotif);

        // 2. Notification for Admin
        NotificationItem adminNotif = new NotificationItem(
                "notif_admin_" + order.getId() + "_" + now,
                "Có đơn hàng mới!",
                "Khách hàng " + customerName + " vừa đặt một đơn hàng mới #DH" + String.format("%04d", order.getId()) + " (" + priceStr + ").",
                order.getId(),
                "ADMIN",
                "",
                now,
                false,
                "NEW_ORDER"
        );
        sendNotification(adminNotif);

        // 3. Notification for Customer
        NotificationItem customerNotif = new NotificationItem(
                "notif_cust_" + order.getId() + "_" + now,
                "Đặt hàng thành công!",
                "Đơn hàng #DH" + String.format("%04d", order.getId()) + " của bạn đã được khởi tạo thành công với trạng thái: " + order.getStatus() + ".",
                order.getId(),
                "CUSTOMER",
                String.valueOf(order.getUserId()),
                now,
                false,
                "ORDER_STATUS"
        );
        sendNotification(customerNotif);
    }

    public void sendOrderStatusNotification(Order order) {
        if (order == null) return;
        long now = System.currentTimeMillis();
        String status = order.getStatus() != null ? order.getStatus() : "Cập nhật";

        // 1. Notification for Customer
        NotificationItem customerNotif = new NotificationItem(
                "notif_cust_status_" + order.getId() + "_" + now,
                "Cập nhật đơn hàng #DH" + String.format("%04d", order.getId()),
                "Đơn hàng của bạn đã chuyển sang trạng thái: " + status + ".",
                order.getId(),
                "CUSTOMER",
                String.valueOf(order.getUserId()),
                now,
                false,
                "ORDER_STATUS"
        );
        sendNotification(customerNotif);

        // 2. Notification for Admin
        NotificationItem adminNotif = new NotificationItem(
                "notif_admin_status_" + order.getId() + "_" + now,
                "Đơn hàng #DH" + String.format("%04d", order.getId()) + " được cập nhật",
                "Trạng thái đơn hàng #" + order.getId() + " vừa được chuyển sang: " + status + ".",
                order.getId(),
                "ADMIN",
                "",
                now,
                false,
                "ORDER_STATUS"
        );
        sendNotification(adminNotif);
    }

    public void markAsRead(String id) {
        executor.execute(() -> {
            notificationDao.markAsRead(id);
            FirebaseDatabase.getInstance()
                    .getReference("notifications")
                    .child(id)
                    .child("read")
                    .setValue(true);
        });
    }

    public void markAllAsRead(String userId, String role) {
        executor.execute(() -> {
            notificationDao.markAllAsRead(userId, role != null ? role.toUpperCase() : "CUSTOMER");
        });
    }

    public void deleteNotification(String id) {
        executor.execute(() -> {
            notificationDao.deleteNotification(id);
            FirebaseDatabase.getInstance()
                    .getReference("notifications")
                    .child(id)
                    .removeValue();
        });
    }
}
