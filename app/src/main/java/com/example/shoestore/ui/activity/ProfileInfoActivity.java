package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivityProfileInfoBinding;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.utils.SharedPrefManager;

import java.util.Locale;

public class ProfileInfoActivity extends AppCompatActivity {
    private ActivityProfileInfoBinding binding;
    private UserRepository userRepository;
    private User currentUser;

    private String originalName = "";
    private String originalEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        userRepository = new UserRepository(getApplication());
        int userId = SharedPrefManager.getInstance(this).getUserId();

        // Listen for real-time user data from Firebase
        userRepository.listenUserFromFirebase(userId, new UserRepository.OnUserResultCallback() {
            @Override
            public void onResult(User user) {
                if (user != null) {
                    currentUser = user;
                    runOnUiThread(() -> bindUserData(user));
                }
            }

            @Override
            public void onError(String message) {
                // Ignore or toast on initial fetch
            }
        });

        // Also fetch from local Room DB first for immediate display
        userRepository.getUserById(userId).observe(this, user -> {
            if (user != null && currentUser == null) {
                currentUser = user;
                bindUserData(user);
            }
        });

        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId > 0) {
            userRepository.getUserById(userId).observe(this, user -> {
                if (user != null) {
                    if (currentUser == null) {
                        currentUser = user;
                        bindUserData(user);
                    } else if (user.getAddress() != null) {
                        currentUser.setAddress(user.getAddress());
                        if (user.getAddress().isEmpty()) {
                            binding.tvDefaultAddress.setText("Chưa có địa chỉ mặc định (Bấm để thêm)");
                        } else {
                            binding.tvDefaultAddress.setText(user.getAddress());
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveChangesIfValid();
    }

    private void bindUserData(User user) {
        if (user == null) return;

        originalName = user.getFullName() != null ? user.getFullName() : "";
        originalEmail = user.getEmail() != null ? user.getEmail() : "";

        binding.etFullName.setText(originalName);
        binding.etPhoneNumber.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        binding.etEmail.setText(originalEmail);

        if (!originalName.isEmpty()) {
            binding.tvAvatar.setText(originalName.substring(0, 1).toUpperCase(Locale.ROOT));
        }

        if (user.getAddress() != null && !user.getAddress().isEmpty()) {
            binding.tvDefaultAddress.setText(user.getAddress());
        } else {
            binding.tvDefaultAddress.setText("Chưa có địa chỉ mặc định (Bấm để thêm)");
        }
    }

    private void setupListeners() {
        binding.layoutAvatar.setOnClickListener(v ->
                Toast.makeText(this, "Chức năng đổi ảnh sẽ được cập nhật ở phiên bản sau", Toast.LENGTH_SHORT).show()
        );
        binding.tvChangeAvatar.setOnClickListener(v ->
                Toast.makeText(this, "Chức năng đổi ảnh sẽ được cập nhật ở phiên bản sau", Toast.LENGTH_SHORT).show()
        );

        binding.layoutDefaultAddress.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileInfoActivity.this, AddressListActivity.class);
            startActivity(intent);
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String currentName = binding.etFullName.getText().toString().trim();
                if (!currentName.isEmpty()) {
                    binding.tvAvatar.setText(currentName.substring(0, 1).toUpperCase(Locale.ROOT));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etFullName.addTextChangedListener(watcher);
        binding.etEmail.addTextChangedListener(watcher);
    }

    private void saveChangesIfValid() {
        if (currentUser == null) return;

        String name = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return;
        }

        if (name.equals(originalName) && email.equals(originalEmail)) {
            return;
        }

        currentUser.setFullName(name);
        currentUser.setEmail(email);

        userRepository.updateProfileFirebase(currentUser, new UserRepository.OnUserActionCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    SharedPrefManager.getInstance(ProfileInfoActivity.this).userLogin(currentUser);
                    originalName = name;
                    originalEmail = email;
                });
            }

            @Override
            public void onError(String message) {}
        });
    }
}
