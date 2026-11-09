package com.example.shoestore.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoestore.model.Product;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert
    long insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);

    @Query("SELECT * FROM products")
    LiveData<List<Product>> getAllProducts();

    @Query("SELECT * FROM products WHERE id = :id")
    LiveData<Product> getProductById(int id);

    @Query("SELECT * FROM products WHERE categoryId = :categoryId")
    LiveData<List<Product>> getProductsByCategory(int categoryId);

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%'")
    LiveData<List<Product>> searchProducts(String query);

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId")
    void reduceStock(int productId, int quantity);
}
