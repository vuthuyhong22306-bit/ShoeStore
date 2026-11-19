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

        // 1. Query Firebase Realtime Database by phoneNumber
        usersRef.orderByChild("phoneNumber").equalTo(account).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    processFirebaseLoginSnapshot(snapshot, account, password);
                } else {
                    // 2. Query by email
                    usersRef.orderByChild("email").equalTo(account).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot emailSnapshot) {
                            if (emailSnapshot.exists()) {
                                processFirebaseLoginSnapshot(emailSnapshot, account, password);
                            } else {
                                // 3. Query all users on Firebase RTDB to match phone/email regardless of formatting
                                usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot allSnapshot) {
                                        if (allSnapshot.exists() && hasMatchingUser(allSnapshot, account)) {
                                            processFirebaseLoginSnapshot(allSnapshot, account, password);
                                        } else {
                                            // 4. Fallback to Room DB
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loginWithRoomFallback(account, password);
            }
        });
    }

    private boolean hasMatchingUser(DataSnapshot snapshot, String account) {
        String cleanAccount = account.replaceAll("[^0-9a-zA-Z@.]", "");
        for (DataSnapshot userSnap : snapshot.getChildren()) {
            String dbPhone = getSnapString(userSnap, "phoneNumber", "phone");
            String dbEmail = getSnapString(userSnap, "email");
            if (dbPhone != null) {
                String cleanPhone = dbPhone.replaceAll("[^0-9]", "");
                if (!cleanPhone.isEmpty() && (cleanPhone.equals(cleanAccount) || (cleanPhone.length() >= 9 && cleanAccount.endsWith(cleanPhone)))) {
                    return true;
                }
            }
            if (dbEmail != null && dbEmail.equalsIgnoreCase(account)) {
                return true;
            }
        }
        return false;
    }

    private void processFirebaseLoginSnapshot(DataSnapshot snapshot, String inputAccount, String inputPassword) {
        boolean matchFound = false;
        String cleanAccount = inputAccount.replaceAll("[^0-9a-zA-Z@.]", "");

        for (DataSnapshot userSnap : snapshot.getChildren()) {
            String dbPhone = getSnapString(userSnap, "phoneNumber", "phone");
            String dbEmail = getSnapString(userSnap, "email");

            // Filter child snapshot if searching all nodes
            boolean isAccountMatch = false;
            if (dbPhone != null) {
                String cleanPhone = dbPhone.replaceAll("[^0-9]", "");
                if (cleanPhone.equals(cleanAccount) || (cleanPhone.length() >= 9 && cleanAccount.endsWith(cleanPhone))) {
                    isAccountMatch = true;
                }
            }
            if (dbEmail != null && dbEmail.equalsIgnoreCase(inputAccount)) {
                isAccountMatch = true;
            }
            if (snapshot.getChildrenCount() == 1) {
                isAccountMatch = true; // Query already filtered by equalTo
            }

            if (!isAccountMatch) continue;

            String dbPassword = getSnapString(userSnap, "password");
            boolean dbLocked = getSnapBoolean(userSnap, "locked", "isLocked");
            String dbRole = getSnapString(userSnap, "role");
            int dbId = getSnapInt(userSnap, "id");
            String dbFullName = getSnapString(userSnap, "fullName", "name");
            String dbAddress = getSnapString(userSnap, "address");
            String dbGender = getSnapString(userSnap, "gender");
            String dbDob = getSnapString(userSnap, "dateOfBirth", "dob");

            if (dbPassword != null && dbPassword.equals(inputPassword)) {
                matchFound = true;

                if (dbRole != null && !"CUSTOMER".equalsIgnoreCase(dbRole)) {
                    Toast.makeText(this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_LONG).show();
                    return;
                }

                if (dbLocked) {
                    Toast.makeText(this, "Tài khoản đã bị khóa", Toast.LENGTH_LONG).show();
                    return;
                }

                User user = new User(
                        dbFullName != null ? dbFullName : "",
                        dbEmail != null ? dbEmail : "",
                        inputPassword,
                        dbPhone != null ? dbPhone : inputAccount,
                        dbAddress != null ? dbAddress : "Việt Nam",
                        "CUSTOMER"
                );
                if (dbId > 0) user.setId(dbId);
                user.setLocked(false);
                user.setGender(dbGender);
                user.setDateOfBirth(dbDob);

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
        int pendingOrderId = getIntent().getIntExtra("ORDER_ID", -1);
        if (pendingOrderId <= 0) {
            pendingOrderId = getIntent().getIntExtra("PENDING_NOTIF_ORDER_ID", -1);
        }

        if (pendingOrderId > 0) {
            Intent detailIntent = new Intent(this, OrderDetailActivity.class);
            detailIntent.putExtra("ORDER_ID", pendingOrderId);
            startActivity(detailIntent);
            finish();
            return;
        }

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
}
