package com.example.shoestore.model;

import java.io.Serializable;

public class Coupon implements Serializable {
    private String code;
    private int discountPercent;
    private double discountAmount;
    private double minOrderValue;
    private String description;
    private String expiryDate;
    private boolean isAvailable;
    private int usedCount;
    private int maxUsage;

    public Coupon() {
        this.isAvailable = true;
        this.maxUsage = 100;
    }

    public Coupon(String code, int discountPercent, double minOrderValue, String description, String expiryDate, boolean isAvailable) {
        this.code = code;
        this.discountPercent = discountPercent;
        this.minOrderValue = minOrderValue;
        this.description = description;
        this.expiryDate = expiryDate;
        this.isAvailable = isAvailable;
        this.maxUsage = 100;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public int getMaxUsage() {
        return maxUsage > 0 ? maxUsage : 100;
    }

    public void setMaxUsage(int maxUsage) {
        this.maxUsage = maxUsage;
    }
}