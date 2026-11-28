package com.example.shoestore.repository;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.shoestore.dao.CartItemDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.CartItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class CartRepository {
    private final CartItemDao cartItemDao;
    private final ExecutorService executor;

    public CartRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        this.cartItemDao = db.cartItemDao();
        this.executor = AppDatabase.databaseWriteExecutor;
    }

    public LiveData<List<CartItem>> getCartItems(int userId) {
        if (userId > 0) {
            syncCartFromFirebase(userId);
        }
        return cartItemDao.getCartItems(userId);
    }

    public interface OnCartActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public void addToCart(CartItem item, int productStock, OnCartActionCallback callback) {
        if (item.getUserId() <= 0) {
            if (callback != null) callback.onError("Vui lòng đăng nhập để thêm vào giỏ hàng!");
            return;
        }
        executor.execute(() -> {
            CartItem existing = cartItemDao.getExistingItem(item.getUserId(), item.getProductId(), item.getSize(), item.getColor());
            int currentQty = existing != null ? existing.getQuantity() : 0;
            if (currentQty + item.getQuantity() > productStock) {
                if (callback != null) callback.onError("Số lượng vượt quá tồn kho (Tồn kho: " + productStock + ")");
            } else {
                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + item.getQuantity());
                    cartItemDao.update(existing);
                    syncSingleItemToFirebase(existing);
                } else {
                    cartItemDao.insert(item);
                    syncSingleItemToFirebase(item);
                }
                if (callback != null) callback.onSuccess();
            }
        });
    }

    public void updateCartItem(CartItem item, int productStock, OnCartActionCallback callback) {
        if (item.getUserId() <= 0) return;
        executor.execute(() -> {
            if (item.getQuantity() > productStock) {
                if (callback != null) callback.onError("Số lượng vượt quá tồn kho (Tồn kho: " + productStock + ")");
            } else {
                cartItemDao.update(item);
                syncSingleItemToFirebase(item);
                if (callback != null) callback.onSuccess();
            }
        });
    }

    public void removeCartItem(CartItem item) {
        if (item.getUserId() <= 0) return;
        executor.execute(() -> {
            cartItemDao.delete(item);
            removeSingleItemFromFirebase(item);
        });
    }

    public void clearCart(int userId) {
        if (userId <= 0) return;
        executor.execute(() -> {
            cartItemDao.clearCart(userId);
            FirebaseDatabase.getInstance().getReference("carts").child(String.valueOf(userId)).removeValue();
        });
    }

    private void syncSingleItemToFirebase(CartItem item) {
        if (item.getUserId() <= 0) return;
        String itemKey = item.getProductId() + "_" + (item.getSize() != null ? item.getSize() : "");
        FirebaseDatabase.getInstance()
                .getReference("carts")
                .child(String.valueOf(item.getUserId()))
                .child(itemKey)
                .setValue(item);
    }

    private void removeSingleItemFromFirebase(CartItem item) {
        if (item.getUserId() <= 0) return;
        String itemKey = item.getProductId() + "_" + (item.getSize() != null ? item.getSize() : "");
        FirebaseDatabase.getInstance()
                .getReference("carts")
                .child(String.valueOf(item.getUserId()))
                .child(itemKey)
                .removeValue();
    }

    private void syncCartFromFirebase(int userId) {
        if (userId <= 0) return;
        DatabaseReference userCartRef = FirebaseDatabase.getInstance().getReference("carts").child(String.valueOf(userId));
        userCartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    executor.execute(() -> {
                        List<CartItem> remoteItems = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                CartItem ci = child.getValue(CartItem.class);
                                if (ci != null) remoteItems.add(ci);
                            } catch (Exception ignored) {}
                        }
                        if (!remoteItems.isEmpty()) {
                            cartItemDao.clearCart(userId);
                            for (CartItem item : remoteItems) {
                                item.setUserId(userId);
                                cartItemDao.insert(item);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
