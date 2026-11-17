package com.example.shoestore.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivityAdminReviewsBinding;

public class AdminReviewsActivity extends AppCompatActivity {
    private ActivityAdminReviewsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý đánh giá");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
