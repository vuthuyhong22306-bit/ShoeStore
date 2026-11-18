package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shoestore.databinding.ActivityAddressListBinding;
import com.example.shoestore.model.Address;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.ui.adapter.AddressAdapter;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddressListActivity extends AppCompatActivity implements AddressAdapter.OnAddressActionListener {
    private ActivityAddressListBinding binding;
    private UserRepository userRepository;
    private AddressAdapter adapter;
    private String userIdStr;
    private boolean isSelectMode = false;
    private String selectedAddressId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isSelectMode = getIntent().getBooleanExtra("EXTRA_SELECT_MODE", false);
        selectedAddressId = getIntent().getStringExtra("EXTRA_SELECTED_ADDRESS_ID");
        if (selectedAddressId == null) selectedAddressId = "";

        binding.btnBack.setOnClickListener(v -> finish());

        userRepository = new UserRepository(getApplication());
        int userId = SharedPrefManager.getInstance(this).getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Không tìm thấy thông tin đăng nhập! Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userIdStr = String.valueOf(userId);

        setupRecyclerView();

        binding.layoutAddAddress.setOnClickListener(v -> {
            Intent intent = new Intent(AddressListActivity.this, AddEditAddressActivity.class);
            startActivity(intent);
        });

        loadAddressesFromFirebase();
    }

    private void setupRecyclerView() {
        adapter = new AddressAdapter(this, isSelectMode, selectedAddressId);
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAddresses.setAdapter(adapter);
    }

    private void loadAddressesFromFirebase() {
        userRepository.listenAddressesFromFirebase(userIdStr, new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Address> addresses = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Address address = null;
                    try {
                        address = child.getValue(Address.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (address == null) {
                        String name = child.hasChild("recipientName") ? String.valueOf(child.child("recipientName").getValue()) : "";
                        String phone = child.hasChild("phoneNumber") ? String.valueOf(child.child("phoneNumber").getValue()) : "";
                        String full = child.hasChild("fullAddress") ? String.valueOf(child.child("fullAddress").getValue()) : "";
                        String detail = child.hasChild("detailAddress") ? String.valueOf(child.child("detailAddress").getValue()) : "";
                        String label = child.hasChild("label") ? String.valueOf(child.child("label").getValue()) : "";
                        boolean isDef = false;
                        if (child.hasChild("isDefault")) {
                            Object val = child.child("isDefault").getValue();
                            if (val instanceof Boolean) isDef = (Boolean) val;
                            else if (val != null) isDef = "true".equalsIgnoreCase(String.valueOf(val));
                        } else if (child.hasChild("default")) {
                            Object val = child.child("default").getValue();
                            if (val instanceof Boolean) isDef = (Boolean) val;
                            else if (val != null) isDef = "true".equalsIgnoreCase(String.valueOf(val));
                        }

                        address = new Address(child.getKey(), userIdStr, name, phone, full, detail, label, isDef);
                    }

                    if (address != null) {
                        if (address.getId() == null || address.getId().trim().isEmpty()) {
                            address.setId(child.getKey());
                        }
                        addresses.add(address);
                    }
                }

                runOnUiThread(() -> {
                    if (addresses.isEmpty()) {
                        binding.rvAddresses.setVisibility(View.GONE);
                        binding.tvEmptyAddresses.setVisibility(View.VISIBLE);
                    } else {
                        binding.rvAddresses.setVisibility(View.VISIBLE);
                        binding.tvEmptyAddresses.setVisibility(View.GONE);
                        adapter.setAddresses(addresses);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() ->
                        Toast.makeText(AddressListActivity.this, "Lỗi đọc địa chỉ từ Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onSelectAddress(Address address) {
        if (address == null) return;
        Intent resultIntent = new Intent();
        resultIntent.putExtra("EXTRA_SELECTED_ADDRESS_ID", address.getId());
        resultIntent.putExtra("EXTRA_SELECTED_ADDRESS", address);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onEdit(Address address) {
        Intent intent = new Intent(AddressListActivity.this, AddEditAddressActivity.class);
        intent.putExtra("EXTRA_ADDRESS", address);
        startActivity(intent);
    }

    @Override
    public void onDelete(Address address) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa địa chỉ")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    userRepository.deleteAddressFirebase(userIdStr, address.getId(), new UserRepository.OnUserActionCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() ->
                                    Toast.makeText(AddressListActivity.this, "Đã xóa địa chỉ!", Toast.LENGTH_SHORT).show()
                            );
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() ->
                                    Toast.makeText(AddressListActivity.this, message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onSetDefault(Address address) {
        userRepository.setDefaultAddressFirebase(userIdStr, address.getId(), new UserRepository.OnUserActionCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() ->
                        Toast.makeText(AddressListActivity.this, "Đã đặt làm địa chỉ mặc định!", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(AddressListActivity.this, message, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}
