package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity Dang nhap duy nhat cho tat ca vai tro (Admin, Nhan vien, Khach hang).
 */
public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_ACTION = "EXTRA_TARGET_ACTION";
    public static final String EXTRA_TARGET_TAB = "EXTRA_TARGET_TAB";

    private View btnBack;
    private EditText etAccount;
    private EditText etPassword;
    private ImageView btnTogglePassword;
    private CheckBox cbRememberMe;
    private TextView tvForgotPassword;
    private View btnLogin;
    private ProgressBar progressBar;
    private TextView tvGoToRegister;

    private boolean isPasswordVisible = false;
    private UserRepository userRepository;
    private String targetAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiem tra neu da dang nhap tu truoc
        SharedPrefManager pref = SharedPrefManager.getInstance(this);
        if (pref.isLoggedIn()) {
            navigateByRole(pref.getUserRole());
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        targetAction = getIntent().getStringExtra(EXTRA_TARGET_ACTION);
        userRepository = new UserRepository(getApplication());

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etAccount = findViewById(R.id.etAccount);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
    }

    private void setupListeners() {
        Runnable goBack = () -> {
            Intent intent = new Intent(LoginActivity.this, CustomerMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        };

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

        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Vui lòng liên hệ Quản trị viên để đặt lại mật khẩu hoặc sử dụng tính năng Đăng ký mới.", Toast.LENGTH_LONG).show()
        );

        btnLogin.setOnClickListener(v -> performLogin());

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CustomerRegisterActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (account.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ tên đăng nhập/SĐT/Email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // 1. Tra cuu tren Firebase Realtime Database node "users"
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String email = getSnapString(child, "email");
                        String phone = getSnapString(child, "phoneNumber", "phone");
                        String username = getSnapString(child, "username", "uname");
                        String fullName = getSnapString(child, "fullName", "name");
                        String dbPassword = getSnapString(child, "password");
                        String role = getSnapString(child, "role");
                        boolean isLocked = getSnapBoolean(child, "locked", "isLocked");
                        int dbId = getSnapInt(child, "id");

                        boolean isMatchAccount = isMatchingAccount(account, email, phone, username, fullName);

                        if (isMatchAccount) {
                            if (isLocked) {
                                setLoading(false);
                                Toast.makeText(LoginActivity.this, "Tài khoản đã bị khóa", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (dbPassword != null && dbPassword.equals(password)) {
                                User user = new User(
                                        fullName != null ? fullName : account,
                                        email != null ? email : "",
                                        password,
                                        phone != null ? phone : account,
                                        getSnapString(child, "address") != null ? getSnapString(child, "address") : "Việt Nam",
                                        role != null ? role.toUpperCase() : "CUSTOMER"
                                );
                                if (dbId > 0) user.setId(dbId);
                                user.setLocked(false);
                                user.setGender(getSnapString(child, "gender", "gioiTinh"));
                                user.setDateOfBirth(getSnapString(child, "dateOfBirth", "dob", "ngaySinh"));

                                onLoginSuccess(user, role);
                                return;
                            } else {
                                setLoading(false);
                                Toast.makeText(LoginActivity.this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }
                }

                // 2. Hardcoded fallback check cho Admin & Staff
                checkHardcodedOrRoomFallback(account, password);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                checkHardcodedOrRoomFallback(account, password);
            }
        });
    }

    private boolean isMatchingAccount(String input, String email, String phone, String username, String fullName) {
        if (input == null || input.isEmpty()) return false;
        String cleanInput = input.replaceAll("[^0-9a-zA-Z@.]", "").toLowerCase();

        if (email != null && email.equalsIgnoreCase(input)) return true;
        if (username != null && username.equalsIgnoreCase(input)) return true;
        if (fullName != null && fullName.equalsIgnoreCase(input)) return true;

        if (phone != null) {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            if (!cleanPhone.isEmpty() && (cleanPhone.equals(cleanInput) || (cleanPhone.length() >= 9 && cleanInput.endsWith(cleanPhone)))) {
                return true;
            }
        }
        return false;
    }

    private void checkHardcodedOrRoomFallback(String account, String password) {
        // Check hardcoded Admin: admin / 123456
        if (("admin".equalsIgnoreCase(account) || "admin@shoestore.com".equalsIgnoreCase(account)) && "123456".equals(password)) {
            User adminUser = new User("Quản trị viên", account, password, "0123456789", "Hà Nội", "ADMIN");
            adminUser.setId(1);
            saveLoginLogToFirebase(account, "ADMIN");
            onLoginSuccess(adminUser, "ADMIN");
            return;
        }

        // Check hardcoded Staff: Nhanvien1 / 123123
        if (("Nhanvien1".equalsIgnoreCase(account) || "staff@shoestore.com".equalsIgnoreCase(account)) && "123123".equals(password)) {
            User staffUser = new User("Nguyễn Văn Staff", "staff@shoestore.com", password, "0987654321", "TP. HCM", "STAFF");
            staffUser.setId(2);
            saveLoginLogToFirebase(account, "STAFF");
            onLoginSuccess(staffUser, "STAFF");
            return;
        }

        // Fallback sang Room local database
        userRepository.loginCustomer(account, password, new UserRepository.OnUserResultCallback() {
            @Override
            public void onResult(User user) {
                runOnUiThread(() -> {
                    if (user.isLocked()) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Tài khoản đã bị khóa", Toast.LENGTH_LONG).show();
                    } else {
                        onLoginSuccess(user, user.getRole());
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Tài khoản hoặc mật khẩu không chính xác! Vui lòng kiểm tra lại.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void onLoginSuccess(User user, String role) {
        userRepository.syncUserFromFirebase(user, new UserRepository.OnUserResultCallback() {
            @Override
            public void onResult(User syncedUser) {
                runOnUiThread(() -> completeLoginFlow(syncedUser, role));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> completeLoginFlow(user, role));
            }
        });
    }

    private void completeLoginFlow(User user, String role) {
        setLoading(false);
        boolean rememberMe = cbRememberMe != null && cbRememberMe.isChecked();
        SharedPrefManager.getInstance(LoginActivity.this).userLogin(user, rememberMe);

        String displayRoleName = "Khách hàng";
        if ("ADMIN".equalsIgnoreCase(role)) displayRoleName = "Quản trị viên";
        else if ("STAFF".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role) || "NHANVIEN".equalsIgnoreCase(role)) displayRoleName = "Nhân viên";

        Toast.makeText(LoginActivity.this, "Đăng nhập thành công với vai trò " + displayRoleName + "!", Toast.LENGTH_SHORT).show();
        navigateByRole(role);
        finish();
    }

    private void navigateByRole(String role) {
        int pendingOrderId = getIntent().getIntExtra("ORDER_ID", -1);
        if (pendingOrderId > 0) {
            Intent detailIntent = new Intent(this, OrderDetailActivity.class);
            detailIntent.putExtra("ORDER_ID", pendingOrderId);
            startActivity(detailIntent);
            finish();
            return;
        }

        Intent intent;
        if ("ADMIN".equalsIgnoreCase(role)) {
            intent = new Intent(this, AdminMainActivity.class);
        } else if ("STAFF".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role) || "NHANVIEN".equalsIgnoreCase(role)) {
            intent = new Intent(this, StaffMainActivity.class);
        } else {
            intent = new Intent(this, CustomerMainActivity.class);
            if (targetAction != null && !targetAction.isEmpty()) {
                intent.putExtra(EXTRA_TARGET_ACTION, targetAction);
            }
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(!isLoading);
            btnLogin.setAlpha(isLoading ? 0.6f : 1.0f);
        }
        if (etAccount != null) etAccount.setEnabled(!isLoading);
        if (etPassword != null) etPassword.setEnabled(!isLoading);
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
            if (val instanceof Number) return ((Number) val).intValue();
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

    private void saveLoginLogToFirebase(String username, String role) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("username", username);
            logData.put("role", role);
            logData.put("login_time", System.currentTimeMillis());
            logData.put("status", "SUCCESS");
            logData.put("device_model", Build.MODEL);

            FirebaseDatabase.getInstance().getReference("login_logs").push().setValue(logData);
            FirebaseFirestore.getInstance().collection("login_logs").add(logData);
        } catch (Exception e) {
            Log.e("LoginActivity", "Error saving log to Firebase: " + e.getMessage());
        }
    }
}
