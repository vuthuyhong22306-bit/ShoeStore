package com.example.shoestore.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.shoestore.dao.FavoriteDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.Favorite;
import com.example.shoestore.model.Product;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class FavoriteRepository {
    private final FavoriteDao favoriteDao;
    private final ExecutorService executor;

    public FavoriteRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        favoriteDao = db.favoriteDao();
        executor = AppDatabase.databaseWriteExecutor;
    }

    public LiveData<List<Integer>> getFavoriteProductIds(int userId) {
        return favoriteDao.getFavoriteProductIds(userId);
    }

    public LiveData<List<Product>> getFavoriteProducts(int userId) {
        return favoriteDao.getFavoriteProducts(userId);
    }

    public LiveData<Integer> isFavorite(int userId, int productId) {
        return favoriteDao.isFavorite(userId, productId);
    }

    public void toggleFavorite(int userId, int productId) {
        executor.execute(() -> {
            int count = favoriteDao.isFavoriteSync(userId, productId);
            if (count > 0) {
                favoriteDao.delete(userId, productId);
            } else {
                favoriteDao.insert(new Favorite(userId, productId));
            }
        });
    }

    public void addFavorite(int userId, int productId) {
        executor.execute(() -> favoriteDao.insert(new Favorite(userId, productId)));
    }

    public void removeFavorite(int userId, int productId) {
        executor.execute(() -> favoriteDao.delete(userId, productId));
    }
}
