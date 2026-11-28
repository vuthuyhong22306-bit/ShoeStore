package com.example.shoestore.repository;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.shoestore.dao.FavoriteDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.Favorite;
import com.example.shoestore.model.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class FavoriteRepository {
    private final FavoriteDao favoriteDao;
    private final ExecutorService executor;

    public FavoriteRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        this.favoriteDao = db.favoriteDao();
        this.executor = AppDatabase.databaseWriteExecutor;
    }

    public LiveData<List<Integer>> getFavoriteProductIds(int userId) {
        if (userId > 0) {
            syncFavoritesFromFirebase(userId);
        }
        return favoriteDao.getFavoriteProductIds(userId);
    }

    public LiveData<List<Product>> getFavoriteProducts(int userId) {
        if (userId > 0) {
            syncFavoritesFromFirebase(userId);
        }
        return favoriteDao.getFavoriteProducts(userId);
    }

    public LiveData<Integer> isFavorite(int userId, int productId) {
        return favoriteDao.isFavorite(userId, productId);
    }

    public void toggleFavorite(int userId, int productId) {
        if (userId <= 0) return;
        executor.execute(() -> {
            int count = favoriteDao.isFavoriteSync(userId, productId);
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("favorites")
                    .child(String.valueOf(userId))
                    .child(String.valueOf(productId));

            if (count > 0) {
                favoriteDao.delete(userId, productId);
                ref.removeValue();
            } else {
                favoriteDao.insert(new Favorite(userId, productId));
                ref.setValue(true);
            }
        });
    }

    public void addFavorite(int userId, int productId) {
        if (userId <= 0) return;
        executor.execute(() -> {
            favoriteDao.insert(new Favorite(userId, productId));
            FirebaseDatabase.getInstance()
                    .getReference("favorites")
                    .child(String.valueOf(userId))
                    .child(String.valueOf(productId))
                    .setValue(true);
        });
    }

    public void removeFavorite(int userId, int productId) {
        if (userId <= 0) return;
        executor.execute(() -> {
            favoriteDao.delete(userId, productId);
            FirebaseDatabase.getInstance()
                    .getReference("favorites")
                    .child(String.valueOf(userId))
                    .child(String.valueOf(productId))
                    .removeValue();
        });
    }

    private void syncFavoritesFromFirebase(int userId) {
        if (userId <= 0) return;
        DatabaseReference userFavRef = FirebaseDatabase.getInstance().getReference("favorites").child(String.valueOf(userId));
        userFavRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    executor.execute(() -> {
                        List<Integer> pIds = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                int pId = Integer.parseInt(child.getKey());
                                pIds.add(pId);
                            } catch (Exception ignored) {}
                        }
                        for (Integer pId : pIds) {
                            if (favoriteDao.isFavoriteSync(userId, pId) == 0) {
                                favoriteDao.insert(new Favorite(userId, pId));
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
