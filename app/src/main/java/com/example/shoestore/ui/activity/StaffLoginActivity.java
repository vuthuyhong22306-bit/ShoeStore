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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.model.User;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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

        btnLogin.setOnClickListener(v -> performStaffLoginFirebase());

        Runnable goBack = () -> {
            Intent intent = new Intent(StaffLoginActivity.this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        };

        View btnBackTop = findViewById(R.id.btnBackTop);
        if (btnBackTop != null) btnBackTop.setOnClickListener(v -> goBack.run());

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> goBack.run());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack.run();
            }
        });
    }

    private void performStaffLoginFirebase() {
        String inputAccount = etUsername.getText().toString().trim();
        String inputPassword = etPassword.getText().toString().trim();

        if (inputAccount.isEmpty() || inputPassword.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang kiểm tra tài khoản Nhân viên...", Toast.LENGTH_SHORT).show();

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean accountFound = false;

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String email = getSnapString(child, "email");
                        String phone = getSnapString(child, "phoneNumber", "phone");
                        String username = getSnapString(child, "username", "uname");
                        String name = getSnapString(child, "fullName", "name");
                        String role = getSnapString(child, "role");
                        String password = getSnapString(child, "password");
                        boolean isLocked = getSnapBoolean(child, "locked", "isLocked");

                        boolean isMatchingAccount = false;
                        if (inputAccount.equalsIgnoreCase(email)
                                || inputAccount.equalsIgnoreCase(username)
                                || inputAccount.equalsIgnoreCase(phone)
                                || inputAccount.equalsIgnoreCase(name)
                                || ("Nhanvien1".equalsIgnoreCase(inputAccount) && ("staff@shoestore.com".equalsIgnoreCase(email) || "0987654321".equals(phone)))) {
                            isMatchingAccount = true;
                        }

                        boolean isStaffRole = role != null && ("STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role) || "NHANVIEN".equalsIgnoreCase(role));

                        if (isMatchingAccount && isStaffRole) {
                            accountFound = true;

                            if (isLocked) {
                                Toast.makeText(StaffLoginActivity.this, "Tài khoản đã bị khóa", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (password != null && password.equals(inputPassword)) {
                                int userId = getSnapInt(child, "id");
                                User user = new User(
                                        name != null ? name : "Nguyễn Văn Staff",
                                        email != null ? email : "staff@shoestore.com",
                                        password,
                                        phone != null ? phone : "0987654321",
                                        "TP. HCM",
                                        "STAFF"
                                );
                                if (userId > 0) user.setId(userId);
                                else user.setId(2);
                                user.setLocked(false);

                                SharedPrefManager.getInstance(StaffLoginActivity.this).userLogin(user);
                                saveStaffLoginToFirebase(inputAccount);

                                Toast.makeText(StaffLoginActivity.this, "Đăng nhập Nhân viên thành công!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(StaffLoginActivity.this, StaffMainActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            } else {
                                Toast.makeText(StaffLoginActivity.this, "Tài khoản hoặc mật khẩu Nhân viên không chính xác!", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }
                }

                // Fallback for demo hardcoded credentials Nhanvien1 / 123123
                if (!accountFound) {
                    if (("Nhanvien1".equalsIgnoreCase(inputAccount) || "staff@shoestore.com".equalsIgnoreCase(inputAccount)) && "123123".equals(inputPassword)) {
                        User user = new User("Nguyễn Văn Staff", "staff@shoestore.com", "123123", "0987654321", "TP. HCM", "STAFF");
                        user.setId(2);
                        user.setLocked(false);

                        SharedPrefManager.getInstance(StaffLoginActivity.this).userLogin(user);
                        saveStaffLoginToFirebase(inputAccount);

                        Toast.makeText(StaffLoginActivity.this, "Đăng nhập Nhân viên thành công!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(StaffLoginActivity.this, StaffMainActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    Toast.makeText(StaffLoginActivity.this, "Tài khoản hoặc mật khẩu Nhân viên không chính xác!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StaffLoginActivity.this, "Lỗi kết nối Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String getSnapString(DataSnapshot snap, String... keys) {
        for (String key : keys) {
            if (snap.hasChild(key)) {
                Object val = snap.child(key).getValue();
                if (val != null) return String.valueOf(val);
            }
        }
        return null;
    }

    private static int getSnapInt(DataSnapshot snap, String key) {
        if (snap.hasChild(key)) {
            Object val = snap.child(key).getValue();
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            if (val != null) {
                try {
                    return Integer.parseInt(String.valueOf(val));
                } catch (Exception ignored) {}
            }
        }
        try {
            return Integer.parseInt(snap.getKey());
        } catch (Exception ignored) {}
        return 0;
    }

    private static boolean getSnapBoolean(DataSnapshot snap, String... keys) {
        for (String key : keys) {
            if (snap.hasChild(key)) {
                Object val = snap.child(key).getValue();
                if (val instanceof Boolean) return (Boolean) val;
                if (val instanceof Number) return ((Number) val).intValue() != 0;
                if (val != null) return "true".equalsIgnoreCase(String.valueOf(val));
            }
        }
        return false;
    }

    private void saveStaffLoginToFirebase(String username) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("username", username);
            logData.put("role", "STAFF");
            logData.put("login_time", System.currentTimeMillis());
            logData.put("status", "SUCCESS");
            logData.put("device_model", Build.MODEL);

            FirebaseDatabase db = FirebaseDatabase.getInstance();
            DatabaseReference ref = db.getReference("staff_logins");
            ref.push().setValue(logData);

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("staff_logins").add(logData);
        } catch (Exception e) {
            Log.e("StaffLoginActivity", "Error saving to Firebase: " + e.getMessage());
        }
    }
}