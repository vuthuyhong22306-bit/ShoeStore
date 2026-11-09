package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.shoestore.databinding.ActivityStaffMainBinding;
import com.example.shoestore.utils.SharedPrefManager;

public class StaffMainActivity extends AppCompatActivity {
    private ActivityStaffMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String role = SharedPrefManager.getInstance(this).getUserRole();
        if (!"STAFF".equals(role) && !"ADMIN".equals(role)) {
            finish();
            return;
        }

        binding = ActivityStaffMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnManageOrders.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageOrdersActivity.class));
        });

        binding.btnManageStock.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageStockActivity.class));
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
