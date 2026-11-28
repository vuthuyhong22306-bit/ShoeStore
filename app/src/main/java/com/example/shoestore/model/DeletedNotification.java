package com.example.shoestore.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "deleted_notification_ids")
public class DeletedNotification {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private long timestamp;

    public DeletedNotification(@NonNull String id, String userId, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
