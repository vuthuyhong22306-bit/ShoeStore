package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.shoestore.databinding.ActivityAdminMainBinding;
import com.example.shoestore.utils.SharedPrefManager;

public class AdminMainActivity extends AppCompatActivity {
    private ActivityAdminMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String role = SharedPrefManager.getInstance(this).getUserRole();
        if (!"ADMIN".equals(role)) {
            finish();
            return;
        }

        binding = ActivityAdminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnManageProducts.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageProductsActivity.class));
        });

        binding.btnManageCategories.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageCategoriesActivity.class));
        });

        binding.btnManageOrders.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageOrdersActivity.class));
        });

        binding.btnStatistics.setOnClickListener(v -> {
            startActivity(new Intent(this, StatisticsActivity.class));
        });

        binding.btnLogout.setOnClickListener(v -> {
            SharedPrefManager.getInstance(this).logout();
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
