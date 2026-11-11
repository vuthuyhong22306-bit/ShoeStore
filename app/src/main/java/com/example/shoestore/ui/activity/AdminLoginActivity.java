package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
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
        View.OnClickListener goBack = v -> {
            Intent intent = new Intent(AdminLoginActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        };

        View btnBackTop = findViewById(R.id.btnBackTop);
        if (btnBackTop != null) btnBackTop.setOnClickListener(goBack);
        if (btnBack != null) btnBack.setOnClickListener(goBack);

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

            // Save login data to Firebase Realtime Database and Firestore
            saveAdminLoginToFirebase(username);

            Toast.makeText(this, "Đăng nhập Quản trị viên thành công! Dữ liệu đã lưu lên Firebase.", Toast.LENGTH_LONG).show();
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
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
            e.printStackTrace();
        }
    }
}
