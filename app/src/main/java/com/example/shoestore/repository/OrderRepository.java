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
import com.example.shoestore.model.OrderWithCount;
import com.example.shoestore.model.RevenueByDate;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class OrderRepository {
    private final OrderDao orderDao;
    private final OrderDetailDao orderDetailDao;
    private final CartItemDao cartItemDao;
    private final ProductDao productDao;
    private final ExecutorService executor;

    public OrderRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        orderDao = db.orderDao();
        orderDetailDao = db.orderDetailDao();
        cartItemDao = db.cartItemDao();
        productDao = db.productDao();
        executor = AppDatabase.databaseWriteExecutor;
    }

    public void placeOrder(Order order, List<CartItem> cartItems, List<Double> prices, OnOrderCallback callback) {
        placeOrder(order, cartItems, prices, false, callback);
    }

    public void placeOrder(Order order, List<CartItem> cartItems, List<Double> prices, boolean isBuyNow, OnOrderCallback callback) {
        executor.execute(() -> {
            long orderId = orderDao.insert(order);
            order.setId((int) orderId);
            for (int i = 0; i < cartItems.size(); i++) {
                CartItem item = cartItems.get(i);
                OrderDetail detail = new OrderDetail((int) orderId, item.getProductId(), item.getSize(), item.getColor(), item.getQuantity(), prices.get(i));
                orderDetailDao.insert(detail);
                productDao.reduceStock(item.getProductId(), item.getQuantity());
            }
            if (!isBuyNow && cartItems != null && !cartItems.isEmpty()) {
                cartItemDao.deleteCartItems(cartItems);
            }

            // Write to Firebase Realtime Database orders/{orderId}
            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(String.valueOf(orderId))
                    .setValue(order)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onError("Lỗi lưu đơn hàng trên Firebase: " + e.getMessage());
                    });

            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("orders")
                        .document(String.valueOf(orderId))
                        .set(order);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LiveData<List<Order>> getOrdersByUser(int userId) {
        return orderDao.getOrdersByUser(userId);
    }

    public LiveData<List<OrderWithCount>> getOrdersWithCountByUser(int userId) {
        return orderDao.getOrdersWithCountByUser(userId);
    }

    public LiveData<List<OrderWithCount>> getOrdersWithCountByUserAndStatus(int userId, String status) {
        if ("Trả hàng".equals(status) || "Trả hàng / Hủy".equals(status)) {
            return orderDao.getReturnedOrdersWithCountByUser(userId);
        }
        return orderDao.getOrdersWithCountByUserAndStatus(userId, status);
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
            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(String.valueOf(order.getId()))
                    .child("status")
                    .setValue(order.getStatus());

            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("orders")
                        .document(String.valueOf(order.getId()))
                        .update("status", order.getStatus());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void cancelOrder(Order order, Runnable onComplete) {
        executor.execute(() -> {
            order.setStatus("Đã hủy");
            orderDao.update(order);
            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(String.valueOf(order.getId()))
                    .child("status")
                    .setValue("Đã hủy");
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void requestReturnOrder(Order order, String note, Runnable onComplete) {
        executor.execute(() -> {
            String noteStr = note != null ? note : "Trả hàng: Đã hoàn tiền";
            order.setNote(noteStr);
            orderDao.update(order);
            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(String.valueOf(order.getId()))
                    .child("note")
                    .setValue(noteStr);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void listenOrdersFromFirebase(com.google.firebase.database.ValueEventListener listener) {
        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("orders")
                .addValueEventListener(listener);
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
        default void onError(String message) {}
    }
}
