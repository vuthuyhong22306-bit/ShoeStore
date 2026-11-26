package com.example.shoestore.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
@Entity(tableName = "notifications")
public class NotificationItem {
    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String content;
    private int orderId;
    private String targetRole; // "CUSTOMER", "STAFF", "ADMIN", "ALL"
    private String targetUserId; // "123" or ""
    private long timestamp;
    private boolean read;
    private String type; // "NEW_ORDER", "ORDER_STATUS", "RETURN_REQUEST"

    public NotificationItem() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.timestamp = System.currentTimeMillis();
        this.read = false;
        this.targetRole = "ALL";
        this.targetUserId = "";
        this.type = "GENERAL";
    }

    public NotificationItem(@NonNull String id, String title, String content, int orderId, String targetRole, String targetUserId, long timestamp, boolean read, String type) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.orderId = orderId;
        this.targetRole = targetRole;
        this.targetUserId = targetUserId;
        this.timestamp = timestamp;
        this.read = read;
        this.type = type;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
