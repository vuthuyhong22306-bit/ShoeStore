package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // 1. Query Firebase Realtime Database node "users" by phoneNumber first
        usersRef.orderByChild("phoneNumber").equalTo(account).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    processFirebaseLoginSnapshot(snapshot, password);
                } else {
                    // 2. Fallback: Query by email if phone search returns no snapshot
                    usersRef.orderByChild("email").equalTo(account).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot emailSnapshot) {
                            if (emailSnapshot.exists()) {
                                processFirebaseLoginSnapshot(emailSnapshot, password);
                            } else {
                                // 3. Fallback to Room DB if Firebase RTDB has no match (or offline)
                                loginWithRoomFallback(account, password);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            loginWithRoomFallback(account, password);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loginWithRoomFallback(account, password);
            }
        });
    }

    private void processFirebaseLoginSnapshot(DataSnapshot snapshot, String inputPassword) {
        boolean matchFound = false;

        for (DataSnapshot userSnap : snapshot.getChildren()) {
            String dbPassword = userSnap.child("password").getValue(String.class);
            Boolean dbLocked = userSnap.child("locked").getValue(Boolean.class);
            if (dbLocked == null) {
                dbLocked = userSnap.child("isLocked").getValue(Boolean.class);
            }
            if (dbLocked == null) {
                dbLocked = false;
            }
            String dbRole = userSnap.child("role").getValue(String.class);
            Long dbIdLong = userSnap.child("id").getValue(Long.class);
            int dbId = dbIdLong != null ? dbIdLong.intValue() : 0;
            String dbFullName = userSnap.child("fullName").getValue(String.class);
            String dbEmail = userSnap.child("email").getValue(String.class);
            String dbPhone = userSnap.child("phoneNumber").getValue(String.class);
            String dbAddress = userSnap.child("address").getValue(String.class);

            if (dbPassword != null && dbPassword.equals(inputPassword)) {
                matchFound = true;

                if (!"CUSTOMER".equalsIgnoreCase(dbRole)) {
                    Toast.makeText(this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_LONG).show();
                    return;
                }

                if (Boolean.TRUE.equals(dbLocked)) {
                    Toast.makeText(this, "Tài khoản đã bị khóa", Toast.LENGTH_LONG).show();
                    return;
                }

                User user = new User(
                        dbFullName != null ? dbFullName : "",
                        dbEmail != null ? dbEmail : "",
                        inputPassword,
                        dbPhone != null ? dbPhone : "",
                        dbAddress != null ? dbAddress : "Việt Nam",
                        "CUSTOMER"
                );
                user.setId(dbId);
                user.setLocked(false);

                // Sync to local Room DB for session cache
                userRepository.syncUserFromFirebase(user, new UserRepository.OnUserResultCallback() {
                    @Override
                    public void onResult(User syncedUser) {
                        runOnUiThread(() -> {
                            SharedPrefManager.getInstance(CustomerLoginActivity.this).userLogin(syncedUser);
                            Toast.makeText(CustomerLoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            navigateByRole("CUSTOMER");
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            SharedPrefManager.getInstance(CustomerLoginActivity.this).userLogin(user);
                            Toast.makeText(CustomerLoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            navigateByRole("CUSTOMER");
                        });
                    }
                });
                return;
            }
        }

        if (!matchFound) {
            Toast.makeText(this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_LONG).show();
        }
    }

    private void loginWithRoomFallback(String account, String password) {
        userRepository.loginCustomer(account, password, new UserRepository.OnUserResultCallback() {
            @Override
            public void onResult(User user) {
                runOnUiThread(() -> {
                    if (user.isLocked()) {
                        Toast.makeText(CustomerLoginActivity.this, "Tài khoản đã bị khóa", Toast.LENGTH_LONG).show();
                    } else {
                        SharedPrefManager.getInstance(CustomerLoginActivity.this).userLogin(user);
                        Toast.makeText(CustomerLoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        navigateByRole(user.getRole());
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(CustomerLoginActivity.this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_LONG).show());
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