package com.example.shoestore.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.shoestore.model.NotificationItem;

import java.util.List;

@Dao
public interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NotificationItem notification);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNotifications(List<NotificationItem> notifications);

    @Query("SELECT * FROM notifications WHERE (targetUserId = :userId OR targetRole = :role OR targetRole = 'ALL' OR targetUserId = 'ALL') ORDER BY timestamp DESC")
    LiveData<List<NotificationItem>> getNotificationsForUser(String userId, String role);

    @Query("SELECT COUNT(*) FROM notifications WHERE (targetUserId = :userId OR targetRole = :role OR targetRole = 'ALL' OR targetUserId = 'ALL') AND read = 0")
    LiveData<Integer> getUnreadCountForUser(String userId, String role);

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    void markAsRead(String id);

    @Query("UPDATE notifications SET read = 1 WHERE (targetUserId = :userId OR targetRole = :role OR targetRole = 'ALL' OR targetUserId = 'ALL')")
    void markAllAsRead(String userId, String role);

    @Query("DELETE FROM notifications WHERE id = :id")
    void deleteNotification(String id);

    @Query("DELETE FROM notifications")
    void deleteAll();
}
