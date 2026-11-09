package com.example.shoestore.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.shoestore.dao.CartItemDao;
import com.example.shoestore.dao.OrderDao;
import com.example.shoestore.dao.OrderDetailDao;
import com.example.shoestore.dao.ProductDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.Order;
import com.example.shoestore.model.OrderDetail;
import com.example.shoestore.model.RevenueByDate;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class OrderRepository {
    private OrderDao orderDao;
    private OrderDetailDao orderDetailDao;
    private CartItemDao cartItemDao;
    private ProductDao productDao;
    private ExecutorService executor;

    public OrderRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        orderDao = db.orderDao();
        orderDetailDao = db.orderDetailDao();
        cartItemDao = db.cartItemDao();
        productDao = db.productDao();
        executor = AppDatabase.databaseWriteExecutor;
    }

    public void placeOrder(Order order, List<CartItem> cartItems, List<Double> prices, OnOrderCallback callback) {
        executor.execute(() -> {
            long orderId = orderDao.insert(order);
            order.setId((int) orderId);
            for (int i = 0; i < cartItems.size(); i++) {
                CartItem item = cartItems.get(i);
                OrderDetail detail = new OrderDetail((int) orderId, item.getProductId(), item.getSize(), item.getColor(), item.getQuantity(), prices.get(i));
                orderDetailDao.insert(detail);
                productDao.reduceStock(item.getProductId(), item.getQuantity());
            }
            cartItemDao.clearCart(order.getUserId());

            // Save Order data to Firebase Realtime Database & Firestore
            try {
                com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("orders")
                        .child(String.valueOf(orderId))
                        .setValue(order);

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("orders")
                        .document(String.valueOf(orderId))
                        .set(order);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (callback != null) callback.onSuccess();
        });
    }

    public LiveData<List<Order>> getOrdersByUser(int userId) {
        return orderDao.getOrdersByUser(userId);
    }

    public LiveData<List<Order>> getAllOrders() {
        return orderDao.getAllOrders();
    }

    public LiveData<Order> getOrderById(int id) {
        return orderDao.getOrderById(id);
    }

    public LiveData<List<OrderDetail>> getOrderDetails(int orderId) {
        return orderDetailDao.getOrderDetails(orderId);
    }

    public void updateOrderStatus(Order order) {
        executor.execute(() -> {
            orderDao.update(order);
            try {
                com.google.firebase.database.FirebaseDatabase.getInstance()
                        .getReference("orders")
                        .child(String.valueOf(order.getId()))
                        .child("status")
                        .setValue(order.getStatus());

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("orders")
                        .document(String.valueOf(order.getId()))
                        .update("status", order.getStatus());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LiveData<Integer> getTotalOrdersCount() {
        return orderDao.getTotalOrders();
    }

    public LiveData<Double> getTotalRevenue() {
        return orderDao.getTotalRevenue();
    }

    public LiveData<List<RevenueByDate>> getDailyRevenue() {
        return orderDao.getDailyRevenue();
    }

    public LiveData<List<RevenueByDate>> getMonthlyRevenue() {
        return orderDao.getMonthlyRevenue();
    }

    public interface OnOrderCallback {
        void onSuccess();
    }
}
