package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivitySplashBinding;
import com.example.shoestore.utils.SharedPrefManager;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.layoutLogo.setAlpha(0f);
        binding.layoutLogo.animate().alpha(1f).setDuration(1000).start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;
            navigateNext();
        }, 1500);
    }

    private void navigateNext() {
        SharedPrefManager pref = SharedPrefManager.getInstance(this);
        Intent intent;
        if (pref.isLoggedIn()) {
            if (pref.isRemembered()) {
                String role = pref.getUserRole();
                if ("ADMIN".equalsIgnoreCase(role)) {
                    intent = new Intent(SplashActivity.this, AdminMainActivity.class);
                } else if ("STAFF".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role) || "NHANVIEN".equalsIgnoreCase(role)) {
                    intent = new Intent(SplashActivity.this, StaffMainActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, CustomerMainActivity.class);
                }
            } else {
                // Temp session from previous run -> logout and enter as guest
                pref.logout();
                intent = new Intent(SplashActivity.this, CustomerMainActivity.class);
            }
        } else {
            // Guest mode -> Open Home directly
            intent = new Intent(SplashActivity.this, CustomerMainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
