package com.example.shoestore.model;

public class RevenueByDate {
    private String date;
    private double revenue;
    private int orderCount;

    public RevenueByDate(String date, double revenue, int orderCount) {
        this.date = date;
        this.revenue = revenue;
        this.orderCount = orderCount;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }
    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
}
