package com.example.shoestore.ui.activity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.databinding.ActivityAddEditAddressBinding;
import com.example.shoestore.model.Address;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.utils.SharedPrefManager;

public class AddEditAddressActivity extends AppCompatActivity {
    private ActivityAddEditAddressBinding binding;
    private UserRepository userRepository;
    private Address editingAddress;
    private String userIdStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        userRepository = new UserRepository(getApplication());
        int userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Không tìm thấy thông tin đăng nhập! Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userIdStr = String.valueOf(userId);

        if (getIntent().hasExtra("EXTRA_ADDRESS")) {
            editingAddress = (Address) getIntent().getSerializableExtra("EXTRA_ADDRESS");
        }

        if (editingAddress != null) {
            binding.tvScreenTitle.setText("Chỉnh sửa địa chỉ");
            binding.etRecipientName.setText(editingAddress.getRecipientName());
            binding.etPhoneNumber.setText(editingAddress.getPhoneNumber());
            binding.etFullAddress.setText(editingAddress.getFullAddress());
            binding.etDetailAddress.setText(editingAddress.getDetailAddress());
            binding.switchDefault.setChecked(editingAddress.isDefault());
        } else {
            binding.tvScreenTitle.setText("Thêm địa chỉ mới");
        }

        binding.btnAllowLocation.setOnClickListener(v -> {
            Toast.makeText(this, "Đã lấy vị trí thành công!", Toast.LENGTH_SHORT).show();
            validateInput();
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInput();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etRecipientName.addTextChangedListener(watcher);
        binding.etPhoneNumber.addTextChangedListener(watcher);
        binding.etFullAddress.addTextChangedListener(watcher);

        binding.btnSave.setOnClickListener(v -> saveAddress());
        validateInput();
    }

    private void validateInput() {
        String name = binding.etRecipientName.getText().toString().trim();
        String phone = binding.etPhoneNumber.getText().toString().trim();
        String address = binding.etFullAddress.getText().toString().trim();

        boolean isValid = !name.isEmpty() && !phone.isEmpty() && !address.isEmpty();
        binding.btnSave.setEnabled(isValid);
        binding.btnSave.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isValid ? "#E5192C" : "#FFB6C1")));
    }

    private void saveAddress() {
        String name = binding.etRecipientName.getText().toString().trim();
        String phone = binding.etPhoneNumber.getText().toString().trim();
        String fullAddress = binding.etFullAddress.getText().toString().trim();
        String detailAddress = binding.etDetailAddress.getText().toString().trim();
        boolean isDefault = binding.switchDefault.isChecked();

        if (name.isEmpty() || phone.isEmpty() || fullAddress.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc!", Toast.LENGTH_SHORT).show();
            return;
        }

        Address addr;
        if (editingAddress != null) {
            addr = editingAddress;
            addr.setRecipientName(name);
            addr.setPhoneNumber(phone);
            addr.setFullAddress(fullAddress);
            addr.setDetailAddress(detailAddress);
            addr.setDefault(isDefault);
        } else {
            addr = new Address(null, userIdStr, name, phone, fullAddress, detailAddress, "", isDefault);
        }

        UserRepository.OnUserActionCallback callback = new UserRepository.OnUserActionCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(AddEditAddressActivity.this, "Lưu địa chỉ thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(AddEditAddressActivity.this, message, Toast.LENGTH_SHORT).show()
                );
            }
        };

        if (editingAddress != null) {
            userRepository.updateAddressFirebase(addr, callback);
        } else {
            userRepository.addAddressFirebase(addr, callback);
        }
    }
}
