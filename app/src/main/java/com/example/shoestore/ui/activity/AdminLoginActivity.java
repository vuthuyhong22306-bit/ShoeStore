package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.model.User;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminLoginActivity extends AppCompatActivity {

    private View btnBack;
    private EditText etUsername;
    private EditText etPassword;
    private ImageView btnTogglePassword;
    private View btnLogin;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SharedPrefManager.getInstance(this).isLoggedIn() && "ADMIN".equalsIgnoreCase(SharedPrefManager.getInstance(this).getUserRole())) {
            startActivity(new Intent(this, AdminMainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnLogin = findViewById(R.id.btnLogin);
    }

    private void setupListeners() {
        Runnable goBack = () -> {
            Intent intent = new Intent(AdminLoginActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        };

        View btnBackTop = findViewById(R.id.btnBackTop);
        if (btnBackTop != null) btnBackTop.setOnClickListener(v -> goBack.run());
        if (btnBack != null) btnBack.setOnClickListener(v -> goBack.run());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack.run();
            }
        });

        btnTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                isPasswordVisible = false;
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                isPasswordVisible = true;
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu Admin", Toast.LENGTH_SHORT).show();
                return;
            }

            // Credential check: admin / 123456
            if (!("admin".equalsIgnoreCase(username) || "admin@shoestore.com".equalsIgnoreCase(username)) || !"123456".equals(password)) {
                Toast.makeText(this, "Tài khoản hoặc mật khẩu Admin không chính xác! (Gợi ý: admin / 123456)", Toast.LENGTH_LONG).show();
                return;
            }

            // Save Admin user session
            User adminUser = new User("Admin", username, password, "0123456789", "Hà Nội", "ADMIN");
            adminUser.setId(1);
            SharedPrefManager.getInstance(this).userLogin(adminUser);

            // Save login data to Firebase Realtime Database and Firestore
            saveAdminLoginToFirebase(username);

            Toast.makeText(this, "Đăng nhập Quản trị viên thành công!", Toast.LENGTH_SHORT).show();

            // Navigate to Admin Dashboard (AdminMainActivity)
            Intent intent = new Intent(AdminLoginActivity.this, AdminMainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void saveAdminLoginToFirebase(String username) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("username", username);
            logData.put("role", "ADMIN");
            logData.put("login_time", System.currentTimeMillis());
            logData.put("status", "SUCCESS");
            logData.put("device_model", Build.MODEL);

            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference ref = db.getReference("admin_logins");
            ref.push().setValue(logData);

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("admin_logins").add(logData);
        } catch (Exception e) {
            Log.e("AdminLoginActivity", "Error saving to Firebase: " + e.getMessage());
        }
    }
}
