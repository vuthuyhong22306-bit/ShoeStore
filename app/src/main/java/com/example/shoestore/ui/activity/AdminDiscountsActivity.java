package com.example.shoestore.ui.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivityAdminDiscountsBinding;

public class AdminDiscountsActivity extends AppCompatActivity {
    private ActivityAdminDiscountsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDiscountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý mã giảm giá");
        }

        binding.btnAddVoucher.setOnClickListener(v -> {
            Toast.makeText(this, "Tạo mã giảm giá mới thành công!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
