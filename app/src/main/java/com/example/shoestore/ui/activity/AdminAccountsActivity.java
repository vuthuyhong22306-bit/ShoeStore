package com.example.shoestore.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityAdminAccountsBinding;
import com.example.shoestore.databinding.ItemAdminAccountBinding;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminAccountsActivity extends AppCompatActivity {
    private static final String TAG = "AdminAccounts";
    private ActivityAdminAccountsBinding binding;
    private UserRepository userRepository;
    private AccountAdapter adapter;
    private List<User> allFirebaseUsers = new ArrayList<>();
    private boolean isStaffTabSelected = true;

    private DatabaseReference usersRef;
    private ValueEventListener usersValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminAccountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        userRepository = new UserRepository(getApplication());
        adapter = new AccountAdapter();
        binding.rvAdminAccounts.setAdapter(adapter);

        binding.tabStaff.setOnClickListener(v -> selectTab(true));
        binding.tabCustomer.setOnClickListener(v -> selectTab(false));

        binding.btnAddStaff.setOnClickListener(v -> showAddEditStaffDialog(null));

        initFirebaseRealtimeUsersListener();
    }

    private void initFirebaseRealtimeUsersListener() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        Log.d(TAG, "Initializing Realtime Database listener on path: " + usersRef.toString());

        usersValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange - snapshot.exists(): " + snapshot.exists() + ", getChildrenCount(): " + snapshot.getChildrenCount());
                List<User> list = new ArrayList<>();

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            User user = null;
                            try {
                                user = child.getValue(User.class);
                            } catch (Exception e) {
                                Log.w(TAG, "Error parsing User.class for key " + child.getKey() + ": " + e.getMessage());
                            }

                            if (user == null) {
                                String name = getSnapString(child, "fullName", "name");
                                String email = getSnapString(child, "email");
                                String pwd = getSnapString(child, "password");
                                String phone = getSnapString(child, "phoneNumber", "phone");
                                String addr = getSnapString(child, "address");
                                String role = getSnapString(child, "role");
                                boolean locked = getSnapBoolean(child, "locked", "isLocked");

                                user = new User(
                                        name != null ? name : "",
                                        email != null ? email : "",
                                        pwd != null ? pwd : "",
                                        phone != null ? phone : "",
                                        addr != null ? addr : "Việt Nam",
                                        role != null ? role : "CUSTOMER"
                                );
                                user.setLocked(locked);
                            }

                            // Ensure ID is populated
                            int parsedId = getSnapInt(child, "id");
                            if (parsedId > 0) {
                                user.setId(parsedId);
                            } else {
                                try {
                                    user.setId(Integer.parseInt(child.getKey()));
                                } catch (Exception ignored) {}
                            }

                            list.add(user);
                            Log.d(TAG, "Loaded user: id=" + user.getId() + ", name=" + user.getFullName() + ", role=" + user.getRole() + ", locked=" + user.isLocked());
                        } catch (Exception ex) {
                            Log.e(TAG, "Error processing user snapshot child: " + ex.getMessage());
                        }
                    }
                }

                allFirebaseUsers = list;
                filterAndDisplayAccounts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase users listener onCancelled: code=" + error.getCode() + ", msg=" + error.getMessage());
                Toast.makeText(AdminAccountsActivity.this, "Lỗi đọc danh sách tài khoản: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        usersRef.addValueEventListener(usersValueEventListener);
    }

    private void selectTab(boolean isStaff) {
        isStaffTabSelected = isStaff;
        if (isStaffTabSelected) {
            binding.tabStaff.setBackgroundResource(R.drawable.bg_admin_chip_selected);
            binding.tabStaff.setTextColor(Color.WHITE);
            binding.tabStaff.setTypeface(null, android.graphics.Typeface.BOLD);

            binding.tabCustomer.setBackgroundResource(R.drawable.bg_admin_chip_unselected);
            binding.tabCustomer.setTextColor(Color.parseColor("#1C1C1E"));
            binding.tabCustomer.setTypeface(null, android.graphics.Typeface.NORMAL);

            binding.tvCustomerNote.setVisibility(View.GONE);
            binding.btnAddStaff.setVisibility(View.VISIBLE);
        } else {
            binding.tabCustomer.setBackgroundResource(R.drawable.bg_admin_chip_selected);
            binding.tabCustomer.setTextColor(Color.WHITE);
            binding.tabCustomer.setTypeface(null, android.graphics.Typeface.BOLD);

            binding.tabStaff.setBackgroundResource(R.drawable.bg_admin_chip_unselected);
            binding.tabStaff.setTextColor(Color.parseColor("#1C1C1E"));
            binding.tabStaff.setTypeface(null, android.graphics.Typeface.NORMAL);

            binding.tvCustomerNote.setVisibility(View.VISIBLE);
            binding.btnAddStaff.setVisibility(View.GONE);
        }
        filterAndDisplayAccounts();
    }

    private void filterAndDisplayAccounts() {
        List<User> filtered = new ArrayList<>();
        for (User u : allFirebaseUsers) {
            String role = u.getRole() != null ? u.getRole().trim().toUpperCase() : "CUSTOMER";
            if (isStaffTabSelected) {
                if ("STAFF".equals(role) || "ADMIN".equals(role) || "NHANVIEN".equals(role)) {
                    filtered.add(u);
                }
            } else {
                if ("CUSTOMER".equals(role) || "KHACHHANG".equals(role)) {
                    filtered.add(u);
                }
            }
        }

        if (filtered.isEmpty()) {
            binding.rvAdminAccounts.setVisibility(View.GONE);
            binding.tvEmptyAccountsState.setVisibility(View.VISIBLE);
            if (isStaffTabSelected) {
                binding.tvEmptyAccountsState.setText("Chưa có nhân viên nào");
            } else {
                binding.tvEmptyAccountsState.setText("Chưa có khách hàng nào");
            }
        } else {
            binding.rvAdminAccounts.setVisibility(View.VISIBLE);
            binding.tvEmptyAccountsState.setVisibility(View.GONE);
            adapter.setUsers(filtered, isStaffTabSelected);
        }
    }

    private void showAddEditStaffDialog(User existingStaff) {
        EditText etName = new EditText(this);
        etName.setHint("Họ tên nhân viên");
        etName.setText(existingStaff != null ? existingStaff.getFullName() : "");

        EditText etEmail = new EditText(this);
        etEmail.setHint("Email");
        etEmail.setText(existingStaff != null ? existingStaff.getEmail() : "");
        if (existingStaff != null) etEmail.setEnabled(false);

        EditText etPhone = new EditText(this);
        etPhone.setHint("Số điện thoại");
        etPhone.setText(existingStaff != null ? existingStaff.getPhoneNumber() : "");

        EditText etPassword = new EditText(this);
        etPassword.setHint(existingStaff != null ? "Mật khẩu mới (để trống nếu không đổi)" : "Mật khẩu");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);
        layout.addView(etName);
        layout.addView(etEmail);
        layout.addView(etPhone);
        layout.addView(etPassword);

        new AlertDialog.Builder(this)
                .setTitle(existingStaff == null ? "Thêm nhân viên mới" : "Sửa tài khoản " + existingStaff.getFullName())
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String pwd = etPassword.getText().toString().trim();

                    if (name.isEmpty() || email.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập tên và email nhân viên!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (existingStaff == null) {
                        if (pwd.isEmpty() || pwd.length() < 6) {
                            Toast.makeText(this, "Mật khẩu phải từ 6 ký tự!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        createStaffAccount(name, email, phone, pwd);
                    } else {
                        existingStaff.setFullName(name);
                        existingStaff.setPhoneNumber(phone);
                        if (!pwd.isEmpty()) {
                            existingStaff.setPassword(pwd);
                        }
                        updateStaffOnFirebase(existingStaff);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createStaffAccount(String name, String email, String phone, String password) {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long maxId = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    long idVal = getSnapInt(child, "id");
                    if (idVal > maxId) maxId = idVal;
                }
                long newId = maxId + 1;
                String userKey = String.valueOf(newId);

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", newId);
                userMap.put("fullName", name);
                userMap.put("email", email);
                userMap.put("password", password);
                userMap.put("phoneNumber", phone);
                userMap.put("address", "Cửa hàng ShoeStore");
                userMap.put("role", "STAFF");
                userMap.put("locked", false);

                // Create Firebase Auth user using Secondary FirebaseApp instance
                try {
                    FirebaseOptions options = FirebaseApp.getInstance().getOptions();
                    FirebaseApp secondaryApp;
                    try {
                        secondaryApp = FirebaseApp.getInstance("SecondaryApp");
                    } catch (IllegalStateException e) {
                        secondaryApp = FirebaseApp.initializeApp(AdminAccountsActivity.this, options, "SecondaryApp");
                    }
                    FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
                    secondaryAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                usersRef.child(userKey).setValue(userMap).addOnSuccessListener(aVoid -> {
                                    User staff = new User(name, email, password, phone, "Cửa hàng ShoeStore", "STAFF");
                                    staff.setId((int) newId);
                                    userRepository.insert(staff, null);
                                    Toast.makeText(AdminAccountsActivity.this, "Đã thêm nhân viên mới thành công!", Toast.LENGTH_SHORT).show();
                                });
                                secondaryAuth.signOut();
                            });
                } catch (Exception e) {
                    usersRef.child(userKey).setValue(userMap).addOnSuccessListener(aVoid -> {
                        User staff = new User(name, email, password, phone, "Cửa hàng ShoeStore", "STAFF");
                        staff.setId((int) newId);
                        userRepository.insert(staff, null);
                        Toast.makeText(AdminAccountsActivity.this, "Đã thêm nhân viên mới!", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminAccountsActivity.this, "Lỗi kết nối Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStaffOnFirebase(User user) {
        String userKey = String.valueOf(user.getId());
        usersRef.child(userKey).child("fullName").setValue(user.getFullName());
        usersRef.child(userKey).child("phoneNumber").setValue(user.getPhoneNumber());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            usersRef.child(userKey).child("password").setValue(user.getPassword());
        }
        userRepository.update(user);
        Toast.makeText(this, "Đã cập nhật thông tin nhân viên", Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && usersValueEventListener != null) {
            usersRef.removeEventListener(usersValueEventListener);
            Log.d(TAG, "Removed Firebase usersValueEventListener");
        }
    }

    class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {
        private List<User> userList = new ArrayList<>();
        private boolean isStaffMode = true;

        public void setUsers(List<User> users, boolean isStaffMode) {
            this.userList = users != null ? users : new ArrayList<>();
            this.isStaffMode = isStaffMode;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAdminAccountBinding binding = ItemAdminAccountBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new AccountViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
            holder.bind(userList.get(position));
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class AccountViewHolder extends RecyclerView.ViewHolder {
            private ItemAdminAccountBinding binding;

            public AccountViewHolder(ItemAdminAccountBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(User user) {
                String name = user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : "Người dùng";
                binding.tvAccountName.setText(name);

                String initial = name.substring(0, 1).toUpperCase();
                binding.tvAvatarLetter.setText(initial);

                String email = user.getEmail() != null ? user.getEmail() : (user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                binding.tvAccountSubtitle.setText(email);

                if (user.isLocked()) {
                    binding.tvAccountStatusBadge.setText("Đã khóa");
                    binding.tvAccountStatusBadge.setBackgroundResource(R.drawable.bg_badge_pink);
                    binding.tvAccountStatusBadge.setTextColor(Color.parseColor("#D0343C"));

                    binding.btnToggleLock.setText("Mở khóa");
                    binding.btnToggleLock.setBackgroundResource(R.drawable.bg_btn_admin_black);
                    binding.btnToggleLock.setTextColor(Color.WHITE);
                } else {
                    binding.tvAccountStatusBadge.setText("Hoạt động");
                    binding.tvAccountStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
                    binding.tvAccountStatusBadge.setTextColor(Color.parseColor("#1E7B3C"));

                    binding.btnToggleLock.setText("Khóa");
                    binding.btnToggleLock.setBackgroundResource(R.drawable.bg_btn_admin_outlined);
                    binding.btnToggleLock.setTextColor(Color.parseColor("#1C1C1E"));
                }

                if (isStaffMode) {
                    binding.layoutAccountActions.setVisibility(View.VISIBLE);
                } else {
                    binding.layoutAccountActions.setVisibility(View.GONE);
                }

                binding.btnEditAccount.setOnClickListener(v -> showAddEditStaffDialog(user));

                binding.btnToggleLock.setOnClickListener(v -> {
                    boolean newLockState = !user.isLocked();
                    user.setLocked(newLockState);

                    String userKey = String.valueOf(user.getId());
                    usersRef.child(userKey).child("locked").setValue(newLockState);
                    usersRef.child(userKey).child("isLocked").setValue(newLockState);

                    userRepository.update(user);
                    Toast.makeText(AdminAccountsActivity.this, newLockState ? "Đã khóa tài khoản " + name : "Đã mở khóa tài khoản " + name, Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}