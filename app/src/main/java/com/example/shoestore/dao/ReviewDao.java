package com.example.shoestore.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.shoestore.model.Review;

import java.util.List;

@Dao
public interface ReviewDao {
    @Query("SELECT * FROM reviews ORDER BY timestamp DESC")
    LiveData<List<Review>> getAllReviews();

    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY timestamp DESC")
    LiveData<List<Review>> getReviewsByProduct(int productId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Review review);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReviews(List<Review> reviews);

    @Update
    void update(Review review);

    @Delete
    void delete(Review review);
}