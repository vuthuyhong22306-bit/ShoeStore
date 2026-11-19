package com.example.shoestore.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.shoestore.model.OrderDetail;

import java.util.List;

@Dao
public interface OrderDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(OrderDetail orderDetail);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrderDetails(List<OrderDetail> orderDetails);

    @Query("SELECT * FROM order_details WHERE orderId = :orderId")
    LiveData<List<OrderDetail>> getOrderDetails(int orderId);
}
