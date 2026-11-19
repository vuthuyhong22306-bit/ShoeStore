package com.example.shoestore.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@com.google.firebase.database.IgnoreExtraProperties
@Entity(tableName = "reviews")
public class Review implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String reviewId;
    private int productId;
    private String productName;
    private int userId;
    private String userName;
    private float rating;
    private String comment;
    private String variantInfo;
    private long timestamp;
    private boolean isHidden;

    public Review() {
    }

    public Review(int productId, String productName, int userId, String userName, float rating, String comment, String variantInfo, long timestamp, boolean isHidden) {
        this.productId = productId;
        this.productName = productName;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.variantInfo = variantInfo;
        this.timestamp = timestamp;
        this.isHidden = isHidden;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getVariantInfo() {
        return variantInfo;
    }

    public void setVariantInfo(String variantInfo) {
        this.variantInfo = variantInfo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }
}