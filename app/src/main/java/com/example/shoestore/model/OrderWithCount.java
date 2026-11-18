package com.example.shoestore.model;

import androidx.room.Embedded;

public class OrderWithCount {
    @Embedded
    private Order order;
    private int itemCount;

    public OrderWithCount(Order order, int itemCount) {
        this.order = order;
        this.itemCount = itemCount;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }
}
