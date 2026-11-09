package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.model.User;
import com.example.shoestore.utils.SharedPrefManager;

public class StaffLoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private ImageView btnTogglePassword;
    private View btnLogin;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnLogin = findViewById(R.id.btnLogin);
    }

    private void setupListeners() {
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
                Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validation: Only nhanvien1 / 123123 allowed
            if (!"nhanvien1".equals(username) || !"123123".equals(password)) {
                Toast.makeText(this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_SHORT).show();
                return;
            }

            User user = new User("Nhân viên", username, password, "0987654321", "TP. HCM", "STAFF");
            user.setId(2);
            SharedPrefManager.getInstance(this).userLogin(user);

            Toast.makeText(this, "Đăng nhập nhân viên thành công!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(StaffLoginActivity.this, StaffMainActivity.class);
            startActivity(intent);
            finish();
        });

        View.OnClickListener goBack = v -> {
            Intent intent = new Intent(StaffLoginActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        };

        View btnBackTop = findViewById(R.id.btnBackTop);
        if (btnBackTop != null) {
            btnBackTop.setOnClickListener(goBack);
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(goBack);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
