package com.example.shoestore.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoestore.model.CartItem;

import java.util.List;

@Dao
public interface CartItemDao {
    @Insert
    void insert(CartItem cartItem);

    @Update
    void update(CartItem cartItem);

    @Delete
    void delete(CartItem cartItem);

    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    LiveData<List<CartItem>> getCartItems(int userId);

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId AND size = :size AND color = :color LIMIT 1")
    CartItem getExistingItem(int userId, int productId, String size, String color);

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    void clearCart(int userId);
}
