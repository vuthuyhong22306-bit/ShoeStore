package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shoestore.databinding.FragmentAccountBinding;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.ui.activity.CustomerLoginActivity;
import com.example.shoestore.utils.SharedPrefManager;

public class AccountFragment extends Fragment {
    private FragmentAccountBinding binding;
    private UserRepository userRepository;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() == null) return;
        userRepository = new UserRepository(getActivity().getApplication());
        int userId = SharedPrefManager.getInstance(getContext()).getUserId();

        userRepository.getUserById(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                binding.etName.setText(user.getFullName());
                binding.etEmail.setText(user.getEmail());
                binding.etPhone.setText(user.getPhoneNumber());
                binding.etAddress.setText(user.getAddress());
            }
        });

        binding.btnUpdate.setOnClickListener(v -> {
            if (currentUser != null) {
                String name = binding.etName.getText().toString().trim();
                String phone = binding.etPhone.getText().toString().trim();
                String address = binding.etAddress.getText().toString().trim();

                if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                    Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentUser.setFullName(name);
                currentUser.setPhoneNumber(phone);
                currentUser.setAddress(address);
                userRepository.update(currentUser);
                Toast.makeText(getContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnLogout.setOnClickListener(v -> {
            SharedPrefManager.getInstance(getContext()).logout();
            Intent intent = new Intent(getContext(), CustomerLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
