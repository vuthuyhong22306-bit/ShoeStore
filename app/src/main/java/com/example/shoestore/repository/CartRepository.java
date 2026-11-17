package com.example.shoestore.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.shoestore.dao.CartItemDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.CartItem;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class CartRepository {
    private CartItemDao cartItemDao;
    private ExecutorService executor;

    public CartRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        cartItemDao = db.cartItemDao();
        executor = AppDatabase.databaseWriteExecutor;
    }

    public LiveData<List<CartItem>> getCartItems(int userId) {
        return cartItemDao.getCartItems(userId);
    }

    public interface OnCartActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public void addToCart(CartItem item, int productStock, OnCartActionCallback callback) {
        executor.execute(() -> {
            CartItem existing = cartItemDao.getExistingItem(item.getUserId(), item.getProductId(), item.getSize(), item.getColor());
            int currentQty = existing != null ? existing.getQuantity() : 0;
            if (currentQty + item.getQuantity() > productStock) {
                if (callback != null) callback.onError("Số lượng vượt quá tồn kho (Tồn kho: " + productStock + ")");
            } else {
                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + item.getQuantity());
                    cartItemDao.update(existing);
                } else {
                    cartItemDao.insert(item);
                }
                if (callback != null) callback.onSuccess();
            }
        });
    }

    public void updateCartItem(CartItem item, int productStock, OnCartActionCallback callback) {
        executor.execute(() -> {
            if (item.getQuantity() > productStock) {
                if (callback != null) callback.onError("Số lượng vượt quá tồn kho (Tồn kho: " + productStock + ")");
            } else {
                cartItemDao.update(item);
                if (callback != null) callback.onSuccess();
            }
        });
    }

    public void removeCartItem(CartItem item) {
        executor.execute(() -> cartItemDao.delete(item));
    }

    public void clearCart(int userId) {
        executor.execute(() -> cartItemDao.clearCart(userId));
    }
}
