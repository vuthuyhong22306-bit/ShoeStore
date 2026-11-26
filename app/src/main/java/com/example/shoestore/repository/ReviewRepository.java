package com.example.shoestore.repository;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.shoestore.dao.ReviewDao;
import com.example.shoestore.database.AppDatabase;
import com.example.shoestore.model.Review;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ReviewRepository {
    private final ReviewDao reviewDao;
    private final ExecutorService executor;
    private final DatabaseReference reviewsRef;

    public ReviewRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        reviewDao = db.reviewDao();
        executor = AppDatabase.databaseWriteExecutor;
        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        initFirebaseSync();
    }

    private void initFirebaseSync() {
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                executor.execute(() -> {
                    List<Review> synced = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Review r = child.getValue(Review.class);
                            if (r != null) {
                                if (r.getReviewId() == null || r.getReviewId().isEmpty()) {
                                    r.setReviewId(child.getKey());
                                }
                                synced.add(r);
                            }
                        } catch (Exception ignored) {}
                    }
                    if (!synced.isEmpty()) {
                        reviewDao.insertReviews(synced);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public LiveData<List<Review>> getAllReviews() {
        return reviewDao.getAllReviews();
    }

    public LiveData<List<Review>> getReviewsByProduct(int productId) {
        return reviewDao.getReviewsByProduct(productId);
    }

    public void insertReview(Review review) {
        executor.execute(() -> {
            long id = reviewDao.insert(review);
            review.setId((int) id);
            String key = review.getReviewId() != null && !review.getReviewId().isEmpty() ? review.getReviewId() : String.valueOf(id);
            reviewsRef.child(key).setValue(review);
        });
    }

    public void updateReview(Review review) {
        executor.execute(() -> {
            reviewDao.update(review);
            String key = review.getReviewId() != null && !review.getReviewId().isEmpty() ? review.getReviewId() : String.valueOf(review.getId());
            reviewsRef.child(key).setValue(review);
        });
    }

    public void deleteReview(Review review) {
        executor.execute(() -> {
            reviewDao.delete(review);
            String key = review.getReviewId() != null && !review.getReviewId().isEmpty() ? review.getReviewId() : String.valueOf(review.getId());
            reviewsRef.child(key).removeValue();
        });
    }
}