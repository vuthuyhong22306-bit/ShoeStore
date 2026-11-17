package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CustomerRegisterActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etFullName;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;

    private ImageView btnTogglePassword;
    private ImageView btnToggleConfirmPassword;
    private View btnRegister;
    private TextView tvGoToLogin;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_register);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        userRepository = new UserRepository(getApplication());

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    private void setupListeners() {
        View.OnClickListener goToLogin = v -> {
            Intent intent = new Intent(CustomerRegisterActivity.this, CustomerLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        };

        btnBack.setOnClickListener(goToLogin);
        tvGoToLogin.setOnClickListener(goToLogin);

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

        btnToggleConfirmPassword.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                isConfirmPasswordVisible = false;
            } else {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                isConfirmPasswordVisible = true;
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        btnRegister.setOnClickListener(v -> performRegistration());
    }

    private void performRegistration() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Định dạng Email không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang xử lý đăng ký...", Toast.LENGTH_SHORT).show();

        // 1. Create User in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                String uid = firebaseUser != null ? firebaseUser.getUid() : "";

                // 2. Save User Profile in Cloud Firestore (users/{uid})
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("uid", uid);
                userMap.put("fullName", fullName);
                userMap.put("email", email);
                userMap.put("phone", phone);
                userMap.put("role", "customer");
                userMap.put("createdAt", System.currentTimeMillis());
                userMap.put("status", "active");

                mFirestore.collection("users").document(uid).set(userMap).addOnSuccessListener(aVoid -> {
                    // 3. Save to local Room Database
                    User newUser = new User(fullName, email, password, phone, "Việt Nam", "customer");
                    userRepository.registerCustomer(newUser, new UserRepository.OnUserActionCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                Toast.makeText(CustomerRegisterActivity.this, "Đăng ký tài khoản thành công!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CustomerRegisterActivity.this, CustomerLoginActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                Toast.makeText(CustomerRegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CustomerRegisterActivity.this, CustomerLoginActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        }
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(CustomerRegisterActivity.this, "Lỗi lưu dữ liệu Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại";
                if (errorMsg != null && errorMsg.contains("already in use")) {
                    errorMsg = "Email này đã được đăng ký tài khoản!";
                }
                Toast.makeText(CustomerRegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
