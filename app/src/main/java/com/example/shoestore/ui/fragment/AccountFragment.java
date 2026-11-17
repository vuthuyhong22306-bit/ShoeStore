package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shoestore.R;
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
    private UserRepository userRepository;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etName = view.findViewById(R.id.etName);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etAddress = view.findViewById(R.id.etAddress);
        Button btnUpdate = view.findViewById(R.id.btnUpdate);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        userRepository = new UserRepository(requireActivity().getApplication());
        int userId = SharedPrefManager.getInstance(requireContext()).getUserId();

        // 1. Fetch Local User Profile
        userRepository.getUserById(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                currentUser = user;
                if (etName != null) etName.setText(user.getFullName());
                if (etEmail != null) etEmail.setText(user.getEmail());
                if (etPhone != null) etPhone.setText(user.getPhoneNumber());
                if (etAddress != null) etAddress.setText(user.getAddress());
            }
        });

        // 2. Fetch Firestore User Profile for Firebase User
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser != null) {
            String uid = fUser.getUid();
            if (fUser.getEmail() != null && etEmail != null) {
                etEmail.setText(fUser.getEmail());
            }
            FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.getString("fullName") != null && etName != null) {
                        etName.setText(documentSnapshot.getString("fullName"));
                    }
                    if (documentSnapshot.getString("phone") != null && etPhone != null) {
                        etPhone.setText(documentSnapshot.getString("phone"));
                    }
                }
            });
        }

        // 3. Update Profile (Local & Firestore)
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                String name = etName != null ? etName.getText().toString().trim() : "";
                String phone = etPhone != null ? etPhone.getText().toString().trim() : "";
                String address = etAddress != null ? etAddress.getText().toString().trim() : "";

                if (name.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
            });
        }

        // 4. Logout Action (Firebase Auth & Session)
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                SharedPrefManager.getInstance(requireContext()).logout();
                Intent intent = new Intent(requireContext(), CustomerLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
    }
}
