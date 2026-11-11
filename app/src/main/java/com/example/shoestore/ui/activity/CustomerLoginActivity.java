package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.utils.SharedPrefManager;

public class CustomerLoginActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etEmailOrPhone;
    private EditText etPassword;
    private ImageView btnTogglePassword;
    private CheckBox cbRememberMe;
    private TextView tvForgotPassword;
    private View btnLogin;
    private TextView tvGoToRegister;

    private boolean isPasswordVisible = false;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        userRepository = new UserRepository(getApplication());
        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etEmailOrPhone = findViewById(R.id.etEmailOrPhone);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerLoginActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
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

        tvForgotPassword.setOnClickListener(v -> Toast.makeText(this, "Tính năng lấy lại mật khẩu", Toast.LENGTH_SHORT).show());

        btnLogin.setOnClickListener(v -> {
            String account = etEmailOrPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (account.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            userRepository.loginCustomer(account, password, new UserRepository.OnUserResultCallback() {
                @Override
                public void onResult(User user) {
                    runOnUiThread(() -> {
                        SharedPrefManager.getInstance(CustomerLoginActivity.this).userLogin(user);
                        Toast.makeText(CustomerLoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CustomerLoginActivity.this, CustomerMainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(CustomerLoginActivity.this, message, Toast.LENGTH_LONG).show());
                }
            });
        });

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerLoginActivity.this, CustomerRegisterActivity.class);
            startActivity(intent);
        });
    }
}
