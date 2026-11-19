package com.example.shoestore.repository;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class OrderRepository {
    private static final String TAG = "OrderRepository";
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

        initFirebaseRealtimeSync();
    }

    /**
     * Listen to Firebase Realtime Database and Firestore for all orders & order details in real time.
     * Synchronize every change directly into the local Room Database so all observers (Staff, Customer, Admin)
     * receive instant updates.
     */
    private void initFirebaseRealtimeSync() {
        // 1. Firebase Realtime Database Sync for Orders
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                executor.execute(() -> {
                    List<Order> syncedOrders = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Order order = child.getValue(Order.class);
                            if (order != null) {
                                if (order.getId() <= 0 && child.getKey() != null) {
                                    try {
                                        order.setId(Integer.parseInt(child.getKey()));
                                    } catch (Exception ignored) {}
                                }
                                syncedOrders.add(order);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing order snapshot: " + e.getMessage());
                        }
                    }
                    if (!syncedOrders.isEmpty()) {
                        orderDao.insertOrders(syncedOrders);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Orders Firebase listener cancelled: " + error.getMessage());
            }
        });

        // 2. Firebase Realtime Database Sync for Order Details
        DatabaseReference detailsRef = FirebaseDatabase.getInstance().getReference("order_details");
        detailsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                executor.execute(() -> {
                    List<OrderDetail> syncedDetails = new ArrayList<>();
                    for (DataSnapshot orderChild : snapshot.getChildren()) {
                        for (DataSnapshot detailChild : orderChild.getChildren()) {
                            try {
                                OrderDetail detail = detailChild.getValue(OrderDetail.class);
                                if (detail != null) {
                                    syncedDetails.add(detail);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing order detail snapshot: " + e.getMessage());
                            }
                        }
                    }
                    if (!syncedDetails.isEmpty()) {
                        orderDetailDao.insertOrderDetails(syncedDetails);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "OrderDetails Firebase listener cancelled: " + error.getMessage());
            }
        });

        // 3. Cloud Firestore Realtime Sync fallback
        try {
            FirebaseFirestore.getInstance().collection("orders").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (error != null || value == null) return;
                    executor.execute(() -> {
                        List<Order> syncedOrders = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                Order order = doc.toObject(Order.class);
                                if (order != null) {
                                    if (order.getId() <= 0 && doc.getId() != null) {
                                        try {
                                            order.setId(Integer.parseInt(doc.getId()));
                                        } catch (Exception ignored) {}
                                    }
                                    syncedOrders.add(order);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing Firestore document: " + e.getMessage());
                            }
                        }
                        if (!syncedOrders.isEmpty()) {
                            orderDao.insertOrders(syncedOrders);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Firestore listener setup failed: " + e.getMessage());
        }
    }

    public void placeOrder(Order order, List<CartItem> cartItems, List<Double> prices, OnOrderCallback callback) {
        placeOrder(order, cartItems, prices, false, callback);
    }

    public void placeOrder(Order order, List<CartItem> cartItems, List<Double> prices, boolean isBuyNow, OnOrderCallback callback) {
        executor.execute(() -> {
            // Generate globally unique 31-bit positive integer order ID if not set
            if (order.getId() <= 0) {
                int uniqueId = (int) (System.currentTimeMillis() & 0x7FFFFFFF);
                order.setId(uniqueId);
            }

            if (order.getStatus() == null || order.getStatus().trim().isEmpty()) {
                order.setStatus("Chờ xác nhận");
            }

            long orderId = orderDao.insert(order);

            List<OrderDetail> detailList = new ArrayList<>();
            for (int i = 0; i < cartItems.size(); i++) {
                CartItem item = cartItems.get(i);
                double price = (prices != null && i < prices.size()) ? prices.get(i) : 0.0;
                OrderDetail detail = new OrderDetail(order.getId(), item.getProductId(), item.getSize(), item.getColor(), item.getQuantity(), price);
                orderDetailDao.insert(detail);
                productDao.reduceStock(item.getProductId(), item.getQuantity());
                detailList.add(detail);
            }

            if (!isBuyNow && cartItems != null && !cartItems.isEmpty()) {
                cartItemDao.deleteCartItems(cartItems);
            }

            String orderIdStr = String.valueOf(order.getId());

            // Write Order to Firebase Realtime Database
            FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(orderIdStr)
                    .setValue(order)
                    .addOnSuccessListener(aVoid -> {
                        // Write Order Details to Firebase Realtime Database
                        FirebaseDatabase.getInstance()
                                .getReference("order_details")
                                .child(orderIdStr)
                                .setValue(detailList);

                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onError("Lỗi lưu đơn hàng trên Firebase: " + e.getMessage());
                    });

            // Write Order to Cloud Firestore
            try {
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                firestore.collection("orders").document(orderIdStr).set(order);

                Map<String, Object> detailsMap = new HashMap<>();
                detailsMap.put("items", detailList);
                firestore.collection("order_details").document(orderIdStr).set(detailsMap);
            } catch (Exception e) {
                Log.e(TAG, "Error writing order to Firestore: " + e.getMessage());
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

    public LiveData<List<OrderDetail>> getAllOrderDetails() {
        return orderDetailDao.getAllOrderDetails();
    }

    public void updateOrderStatus(Order order) {
        executor.execute(() -> {
            orderDao.update(order);
            String orderIdStr = String.valueOf(order.getId());

            // Firebase Realtime Database Updates
            DatabaseReference orderRef = FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(orderIdStr);

            orderRef.child("status").setValue(order.getStatus());
            if (order.getNote() != null) {
                orderRef.child("note").setValue(order.getNote());
            }

            // Cloud Firestore Updates
            try {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", order.getStatus());
                if (order.getNote() != null) {
                    updates.put("note", order.getNote());
                }
                FirebaseFirestore.getInstance()
                        .collection("orders")
                        .document(orderIdStr)
                        .update(updates);
            } catch (Exception e) {
                Log.e(TAG, "Error updating status in Firestore: " + e.getMessage());
            }
        });
    }

    public void cancelOrder(Order order, Runnable onComplete) {
        executor.execute(() -> {
            order.setStatus("Đã hủy");
            updateOrderStatus(order);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void requestReturnOrder(Order order, String note, Runnable onComplete) {
        executor.execute(() -> {
            String noteStr = note != null ? note : "Yêu cầu trả hàng";
            order.setStatus("Yêu cầu trả hàng");
            order.setNote(noteStr);
            updateOrderStatus(order);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void acceptReturnOrder(Order order, Runnable onComplete) {
        executor.execute(() -> {
            order.setStatus("Đã chấp nhận");
            updateOrderStatus(order);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void rejectReturnOrder(Order order, String reason, Runnable onComplete) {
        executor.execute(() -> {
            order.setStatus("Đã từ chối");
            String noteStr = (order.getNote() != null ? order.getNote() + "\n" : "") + "Từ chối: " + reason;
            order.setNote(noteStr);
            updateOrderStatus(order);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void confirmReceivedReturnGoods(Order order, Runnable onComplete) {
        executor.execute(() -> {
            order.setStatus("Đã nhận hàng trả");
            updateOrderStatus(order);

            List<OrderDetail> details = orderDetailDao.getOrderDetailsList(order.getId());
            if (details != null) {
                for (OrderDetail d : details) {
                    productDao.increaseStock(d.getProductId(), d.getQuantity());
                }
            }

            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void confirmRefund(Order order, Runnable onComplete) {
        executor.execute(() -> {
            order.setStatus("Đã hoàn tiền");
            updateOrderStatus(order);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void listenOrdersFromFirebase(ValueEventListener listener) {
        FirebaseDatabase.getInstance()
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
