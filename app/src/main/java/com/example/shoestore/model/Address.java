package com.example.shoestore.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@com.google.firebase.database.IgnoreExtraProperties
@Entity(tableName = "addresses")
public class Address implements Serializable {
    @PrimaryKey
    @NonNull
    private String id = "";
    private String userId;
    private String recipientName;
    private String phoneNumber;
    private String fullAddress;
    private String detailAddress;
    private String label;
    private boolean isDefault;

    public Address() {
    }

    public Address(String id, String userId, String recipientName, String phoneNumber, String fullAddress, String detailAddress, String label, boolean isDefault) {
        this.id = id != null ? id : "";
        this.userId = userId;
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.fullAddress = fullAddress;
        this.detailAddress = detailAddress;
        this.label = label != null ? label : "";
        this.isDefault = isDefault;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id != null ? id : "";
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @com.google.firebase.database.PropertyName("isDefault")
    public boolean isDefault() {
        return isDefault;
    }

    @com.google.firebase.database.PropertyName("isDefault")
    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    // Additional fallback mapping for Firebase deserialization of "default"
    @androidx.room.Ignore
    @com.google.firebase.database.PropertyName("default")
    public boolean getDefault() {
        return isDefault;
    }

    @androidx.room.Ignore
    @com.google.firebase.database.PropertyName("default")
    public void setDefaultProp(boolean aDefault) {
        isDefault = aDefault;
    }
}
