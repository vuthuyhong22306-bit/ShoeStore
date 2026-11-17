package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivitySplashBinding;

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
            startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
            finish();
        }, 1500);
    }
}
