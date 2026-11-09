package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.shoestore.databinding.ActivityLoginBinding;
import com.example.shoestore.model.User;
import com.example.shoestore.utils.SharedPrefManager;
import com.example.shoestore.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (SharedPrefManager.getInstance(this).isLoggedIn()) {
            navigateBasedOnRole(SharedPrefManager.getInstance(this).getUserRole());
            return;
        }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            authViewModel.login(email, password);
        });

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        authViewModel.getLoggedInUser().observe(this, user -> {
            if (user != null) {
                SharedPrefManager.getInstance(this).userLogin(user);
                navigateBasedOnRole(user.getRole());
            }
        });

        authViewModel.getErrorMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateBasedOnRole(String role) {
        Intent intent;
        if ("ADMIN".equals(role)) {
            intent = new Intent(this, AdminMainActivity.class);
        } else if ("STAFF".equals(role)) {
            intent = new Intent(this, StaffMainActivity.class);
        } else {
            intent = new Intent(this, CustomerMainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
