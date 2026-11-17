package com.example.shoestore.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shoestore.databinding.ActivityAdminAccountsBinding;
import com.example.shoestore.databinding.ItemAdminAccountBinding;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class AdminAccountsActivity extends AppCompatActivity {
    private ActivityAdminAccountsBinding binding;
    private UserRepository userRepository;
    private AccountAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminAccountsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quản lý tài khoản");
        }

        userRepository = new UserRepository(getApplication());
        adapter = new AccountAdapter();
        binding.rvAdminAccounts.setAdapter(adapter);

        userRepository.getAllUsers().observe(this, users -> {
            if (users != null) {
                adapter.setUsers(users);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    class AccountAdapter extends RecyclerView.Adapter<AccountAdapter.AccountViewHolder> {
        private List<User> userList = new ArrayList<>();

        public void setUsers(List<User> users) {
            this.userList = users != null ? users : new ArrayList<>();
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
                binding.tvAccountName.setText(user.getFullName());
                binding.tvAccountRole.setText(user.getRole() != null ? user.getRole() : "CUSTOMER");
                binding.tvAccountEmail.setText("Email: " + (user.getEmail() != null ? user.getEmail() : "Chưa cập nhật"));
                binding.tvAccountPhone.setText("SĐT: " + (user.getPhoneNumber() != null ? user.getPhoneNumber() : "Chưa cập nhật"));

                if (user.isLocked()) {
                    binding.btnToggleLock.setText("Mở khóa");
                    binding.btnToggleLock.setTextColor(0xFF2E7D32);
                } else {
                    binding.btnToggleLock.setText("Khóa");
                    binding.btnToggleLock.setTextColor(0xFFD32F2F);
                }

                binding.btnToggleLock.setOnClickListener(v -> {
                    user.setLocked(!user.isLocked());
                    userRepository.update(user);
                    Toast.makeText(AdminAccountsActivity.this, user.isLocked() ? "Đã khóa tài khoản " + user.getFullName() : "Đã mở khóa tài khoản " + user.getFullName(), Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}
