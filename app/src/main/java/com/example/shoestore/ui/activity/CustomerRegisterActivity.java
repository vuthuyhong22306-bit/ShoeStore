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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.CartRepository;
import com.example.shoestore.repository.FavoriteRepository;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.utils.GuestDataManager;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_register);

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
            Intent intent = new Intent(CustomerRegisterActivity.this, LoginActivity.class);
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

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // 1. Check duplicate phone number on Firebase Realtime Database
        usersRef.orderByChild("phoneNumber").equalTo(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot phoneSnapshot) {
                if (phoneSnapshot.exists()) {
                    Toast.makeText(CustomerRegisterActivity.this, "Số điện thoại này đã được đăng ký tài khoản!", Toast.LENGTH_LONG).show();
                    return;
                }

                // 2. Check duplicate email on Firebase Realtime Database
                usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot emailSnapshot) {
                        if (emailSnapshot.exists()) {
                            Toast.makeText(CustomerRegisterActivity.this, "Email này đã được đăng ký tài khoản!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // 3. Compute new ID without overwriting existing IDs
                        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot allSnapshot) {
                                long maxId = 0;
                                for (DataSnapshot child : allSnapshot.getChildren()) {
                                    Long idVal = child.child("id").getValue(Long.class);
                                    if (idVal == null) {
                                        try {
                                            idVal = Long.parseLong(child.getKey());
                                        } catch (Exception ignored) {}
                                    }
                                    if (idVal != null && idVal > maxId) {
                                        maxId = idVal;
                                    }
                                }
                                long newId = maxId + 1;

                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("id", newId);
                                userMap.put("fullName", fullName);
                                userMap.put("email", email);
                                userMap.put("password", password);
                                userMap.put("phoneNumber", phone);
                                userMap.put("address", "Việt Nam");
                                userMap.put("role", "CUSTOMER");
                                userMap.put("locked", false);

                                usersRef.child(String.valueOf(newId)).setValue(userMap).addOnSuccessListener(aVoid -> {
                                    User newUser = new User(fullName, email, password, phone, "Việt Nam", "CUSTOMER");
                                    newUser.setId((int) newId);
                                    userRepository.insert(newUser, null);

                                    SharedPrefManager.getInstance(CustomerRegisterActivity.this).userLogin(newUser);

                                    processCustomerPendingAction(newUser);
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(CustomerRegisterActivity.this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(CustomerRegisterActivity.this, "Lỗi kết nối Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CustomerRegisterActivity.this, "Lỗi kết nối Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerRegisterActivity.this, "Lỗi kết nối Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processCustomerPendingAction(User user) {
        Toast.makeText(this, "Đăng ký tài khoản thành công!", Toast.LENGTH_LONG).show();

        GuestDataManager guestPref = GuestDataManager.getInstance(this);
        String action = guestPref.getPendingAction();
        String returnDest = guestPref.getReturnDestination();
        int productId = guestPref.getPendingProductId();
        String size = guestPref.getPendingSize();
        String color = guestPref.getPendingColor();
        int quantity = guestPref.getPendingQuantity();
        double price = guestPref.getPendingPrice();

        guestPref.clearPendingAction();

        Intent intent;
        if ("ADD_TO_CART".equalsIgnoreCase(action) && productId > 0) {
            CartRepository cartRepository = new CartRepository(getApplication());
            CartItem newItem = new CartItem(user.getId(), productId, size, color, quantity);
            cartRepository.addToCart(newItem, 9999, null);

            intent = new Intent(this, CustomerMainActivity.class);
            intent.putExtra(LoginActivity.EXTRA_TARGET_TAB, R.id.nav_cart);
        } else if ("BUY_NOW".equalsIgnoreCase(action) && productId > 0) {
            intent = new Intent(this, CheckoutActivity.class);
            intent.putExtra(CheckoutActivity.EXTRA_MODE, CheckoutActivity.MODE_BUY_NOW);
            intent.putExtra(CheckoutActivity.EXTRA_PRODUCT_ID, productId);
            intent.putExtra(CheckoutActivity.EXTRA_SIZE, size);
            intent.putExtra(CheckoutActivity.EXTRA_COLOR, color);
            intent.putExtra(CheckoutActivity.EXTRA_QUANTITY, quantity);
            intent.putExtra(CheckoutActivity.EXTRA_PRICE, price);
        } else if ("TOGGLE_FAVORITE".equalsIgnoreCase(action) && productId > 0) {
            FavoriteRepository favoriteRepository = new FavoriteRepository(getApplication());
            favoriteRepository.toggleFavorite(user.getId(), productId);

            intent = new Intent(this, CustomerMainActivity.class);
            intent.putExtra(LoginActivity.EXTRA_TARGET_TAB, R.id.nav_favorites);
        } else if ("NAV_CART".equalsIgnoreCase(action)) {
            intent = new Intent(this, CustomerMainActivity.class);
            intent.putExtra(LoginActivity.EXTRA_TARGET_TAB, R.id.nav_cart);
        } else if ("NAV_FAVORITES".equalsIgnoreCase(action)) {
            intent = new Intent(this, CustomerMainActivity.class);
            intent.putExtra(LoginActivity.EXTRA_TARGET_TAB, R.id.nav_favorites);
        } else if ("NAV_ORDERS".equalsIgnoreCase(action) || "ORDERS".equalsIgnoreCase(returnDest)) {
            intent = new Intent(this, CustomerMainActivity.class);
            intent.putExtra(LoginActivity.EXTRA_TARGET_TAB, R.id.nav_orders);
        } else if ("NAV_ACCOUNT".equalsIgnoreCase(action) || "ACCOUNT".equalsIgnoreCase(returnDest)) {
            intent = new Intent(this, CustomerMainActivity.class);
            intent.putExtra(LoginActivity.EXTRA_TARGET_TAB, R.id.nav_account);
        } else {
            intent = new Intent(this, CustomerMainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
