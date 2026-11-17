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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

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

        // 1. Fetch Local User Profile
        userRepository.getUserById(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                binding.etName.setText(user.getFullName());
                binding.etEmail.setText(user.getEmail());
                binding.etPhone.setText(user.getPhoneNumber());
                binding.etAddress.setText(user.getAddress());
            }
        });

        // 2. Fetch Firestore User Profile for Firebase User
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser != null) {
            String uid = fUser.getUid();
            if (fUser.getEmail() != null) {
                binding.etEmail.setText(fUser.getEmail());
            }
            FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.getString("fullName") != null) {
                        binding.etName.setText(documentSnapshot.getString("fullName"));
                    }
                    if (documentSnapshot.getString("phone") != null) {
                        binding.etPhone.setText(documentSnapshot.getString("phone"));
                    }
                }
            });
        }

        // 3. Update Profile (Local & Firestore)
        binding.btnUpdate.setOnClickListener(v -> {
            String name = binding.etName.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();
            String address = binding.etAddress.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser fUserCurrent = FirebaseAuth.getInstance().getCurrentUser();
            if (fUserCurrent != null) {
                String uid = fUserCurrent.getUid();
                Map<String, Object> updates = new HashMap<>();
                updates.put("fullName", name);
                updates.put("phone", phone);
                FirebaseFirestore.getInstance().collection("users").document(uid).update(updates);
            }

            if (currentUser != null) {
                currentUser.setFullName(name);
                currentUser.setPhoneNumber(phone);
                currentUser.setAddress(address);
                userRepository.update(currentUser);
            }
            Toast.makeText(getContext(), "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
        });

        // 4. Logout Action (Firebase Auth & Session)
        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
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
