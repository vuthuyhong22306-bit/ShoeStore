package com.example.shoestore.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoestore.model.Order;
import com.example.shoestore.model.OrderWithCount;
import com.example.shoestore.model.RevenueByDate;

import java.util.List;

@Dao
public interface OrderDao {
    @Insert
    long insert(Order order);

    @Update
    void update(Order order);

    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    LiveData<List<Order>> getAllOrders();

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    LiveData<List<Order>> getOrdersByUser(int userId);

    @Query("SELECT o.*, COALESCE((SELECT SUM(od.quantity) FROM order_details od WHERE od.orderId = o.id), 1) AS itemCount FROM orders o WHERE o.userId = :userId ORDER BY o.orderDate DESC")
    LiveData<List<OrderWithCount>> getOrdersWithCountByUser(int userId);

    @Query("SELECT o.*, COALESCE((SELECT SUM(od.quantity) FROM order_details od WHERE od.orderId = o.id), 1) AS itemCount FROM orders o WHERE o.userId = :userId AND o.status = :status ORDER BY o.orderDate DESC")
    LiveData<List<OrderWithCount>> getOrdersWithCountByUserAndStatus(int userId, String status);

    @Query("SELECT o.*, COALESCE((SELECT SUM(od.quantity) FROM order_details od WHERE od.orderId = o.id), 1) AS itemCount FROM orders o WHERE o.userId = :userId AND o.note IS NOT NULL AND o.note != '' ORDER BY o.orderDate DESC")
    LiveData<List<OrderWithCount>> getReturnedOrdersWithCountByUser(int userId);

    @Query("SELECT * FROM orders WHERE id = :id")
    LiveData<Order> getOrderById(int id);

    @Query("SELECT SUM(totalAmount) FROM orders WHERE status = 'Đã hoàn thành'")
    LiveData<Double> getTotalRevenue();

    @Query("SELECT COUNT(*) FROM orders")
    LiveData<Integer> getTotalOrders();

    @Query("SELECT strftime('%Y-%m-%d', orderDate / 1000, 'unixepoch', 'localtime') AS date, SUM(totalAmount) AS revenue, COUNT(*) AS orderCount FROM orders WHERE status = 'Đã hoàn thành' GROUP BY date ORDER BY date DESC")
    LiveData<List<RevenueByDate>> getDailyRevenue();

    @Query("SELECT strftime('%m/%Y', orderDate / 1000, 'unixepoch', 'localtime') AS date, SUM(totalAmount) AS revenue, COUNT(*) AS orderCount FROM orders WHERE status = 'Đã hoàn thành' GROUP BY date ORDER BY orderDate DESC")
    LiveData<List<RevenueByDate>> getMonthlyRevenue();
}
