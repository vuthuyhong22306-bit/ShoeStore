package com.example.shoestore.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.shoestore.model.DeletedNotification;

import java.util.List;

@Dao
public interface DeletedNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DeletedNotification deletedNotification);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DeletedNotification> deletedNotifications);

    @Query("SELECT id FROM deleted_notification_ids")
    List<String> getAllDeletedIdsSync();

    @Query("SELECT COUNT(*) FROM deleted_notification_ids WHERE id = :id")
    int isDeletedSync(String id);
}
