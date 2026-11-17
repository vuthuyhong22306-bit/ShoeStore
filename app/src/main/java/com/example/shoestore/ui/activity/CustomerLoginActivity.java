package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CustomerLoginActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etEmailOrPhone;
    private EditText etPassword;
    private ImageView btnTogglePassword;
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

        tvForgotPassword.setOnClickListener(v -> Toast.makeText(this, "Tính năng lấy lại mật khẩu qua Firebase Email", Toast.LENGTH_SHORT).show());

        btnLogin.setOnClickListener(v -> performLogin());

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerLoginActivity.this, CustomerRegisterActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String account = etEmailOrPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (account.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang xác thực tài khoản...", Toast.LENGTH_SHORT).show();

        // 1. Authenticate using Firebase Authentication
        FirebaseAuth.getInstance().signInWithEmailAndPassword(account, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful() && FirebaseAuth.getInstance().getCurrentUser() != null) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // 2. Read User Profile from Cloud Firestore (users/{uid})
                FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                    String fetchedRole = documentSnapshot.exists() && documentSnapshot.getString("role") != null 
                            ? documentSnapshot.getString("role") : "customer";
                    String fullName = documentSnapshot.exists() && documentSnapshot.getString("fullName") != null 
                            ? documentSnapshot.getString("fullName") : account;
                    String phone = documentSnapshot.exists() && documentSnapshot.getString("phone") != null 
                            ? documentSnapshot.getString("phone") : "";

                    User user = new User(fullName, account, password, phone, "Việt Nam", fetchedRole);
                    SharedPrefManager.getInstance(CustomerLoginActivity.this).userLogin(user);

                    Toast.makeText(CustomerLoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    navigateByRole(fetchedRole);
                }).addOnFailureListener(e -> {
                    User user = new User(account, account, password, "", "Việt Nam", "customer");
                    SharedPrefManager.getInstance(CustomerLoginActivity.this).userLogin(user);
                    navigateByRole("customer");
                });
            } else {
                // Fallback to Room Database login
                userRepository.loginCustomer(account, password, new UserRepository.OnUserResultCallback() {
                    @Override
                    public void onResult(User user) {
                        runOnUiThread(() -> {
                            SharedPrefManager.getInstance(CustomerLoginActivity.this).userLogin(user);
                            Toast.makeText(CustomerLoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            navigateByRole(user.getRole());
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(Toast.makeText(CustomerLoginActivity.this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_LONG)::show);
                    }
                });
            }
        });
    }

    private void navigateByRole(String role) {
        Intent intent;
        if ("admin".equalsIgnoreCase(role)) {
            intent = new Intent(this, AdminMainActivity.class);
        } else if ("staff".equalsIgnoreCase(role) || "employee".equalsIgnoreCase(role)) {
            intent = new Intent(this, StaffMainActivity.class);
        } else {
            intent = new Intent(this, CustomerMainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
