package com.example.shoestore.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.shoestore.model.Favorite;
import com.example.shoestore.model.Product;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Favorite favorite);

    @Query("DELETE FROM favorites WHERE userId = :userId AND productId = :productId")
    void delete(int userId, int productId);

    @Query("SELECT productId FROM favorites WHERE userId = :userId")
    LiveData<List<Integer>> getFavoriteProductIds(int userId);

    @Query("SELECT productId FROM favorites WHERE userId = :userId")
    List<Integer> getFavoriteProductIdsSync(int userId);

    @Query("SELECT products.* FROM products INNER JOIN favorites ON products.id = favorites.productId WHERE favorites.userId = :userId")
    LiveData<List<Product>> getFavoriteProducts(int userId);

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND productId = :productId")
    int isFavoriteSync(int userId, int productId);

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND productId = :productId")
    LiveData<Integer> isFavorite(int userId, int productId);
}
