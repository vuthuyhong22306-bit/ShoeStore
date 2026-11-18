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
import androidx.navigation.Navigation;

import com.example.shoestore.R;
import com.example.shoestore.databinding.FragmentAccountBinding;
import com.example.shoestore.model.User;
import com.example.shoestore.repository.UserRepository;
import com.example.shoestore.ui.activity.CustomerLoginActivity;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

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

        userRepository = new UserRepository(requireActivity().getApplication());
        SharedPrefManager pref = SharedPrefManager.getInstance(requireContext());
        int userId = pref.getUserId();

        // 1. Immediately render header from SharedPrefManager (eliminates placeholder text)
        String prefName = pref.getUserName();
        String prefEmail = pref.getUserEmail();
        String prefPhone = pref.getUserPhone();
        updateUserInfo(prefName, prefEmail, prefPhone);

        // 2. Observe Room DB
        userRepository.getUserById(userId).observe(getViewLifecycleOwner(), user -> {
            if (user != null && isAdded() && binding != null) {
                currentUser = user;
                updateUserInfo(user.getFullName(), user.getEmail(), user.getPhoneNumber());
            }
        });

        // 3. Listen to Firebase Realtime Database
        userRepository.listenUserFromFirebase(userId, new UserRepository.OnUserResultCallback() {
            @Override
            public void onResult(User user) {
                if (user != null && isAdded() && binding != null) {
                    currentUser = user;
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded() && binding != null) {
                                updateUserInfo(user.getFullName(), user.getEmail(), user.getPhoneNumber());
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(String message) {}
        });

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser != null) {
            String uid = fUser.getUid();
            String email = fUser.getEmail() != null ? fUser.getEmail() : "";
            FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && isAdded() && binding != null) {
                    String name = documentSnapshot.getString("fullName");
                    String phone = documentSnapshot.getString("phone");
                    updateUserInfo(name, email, phone);
                }
            });
        }

        setupMenuClickListeners(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            SharedPrefManager pref = SharedPrefManager.getInstance(requireContext());
            updateUserInfo(pref.getUserName(), pref.getUserEmail(), pref.getUserPhone());
        }
    }

    private void updateUserInfo(String name, String email, String phone) {
        if (binding == null) return;
        if (name != null && !name.trim().isEmpty()) {
            binding.tvUserName.setText(name);
            binding.tvAvatar.setText(name.trim().substring(0, 1).toUpperCase(Locale.ROOT));
        } else {
            binding.tvUserName.setText("Khách hàng");
            binding.tvAvatar.setText("K");
        }

        String emailStr = email != null ? email : "";
        String phoneStr = phone != null ? phone : "";
        String contactInfo;
        if (!emailStr.isEmpty() && !phoneStr.isEmpty()) {
            contactInfo = emailStr + " · " + phoneStr;
        } else if (!emailStr.isEmpty()) {
            contactInfo = emailStr;
        } else if (!phoneStr.isEmpty()) {
            contactInfo = phoneStr;
        } else {
            contactInfo = "Chưa cập nhật liên hệ";
        }
        binding.tvUserContact.setText(contactInfo);
    }

    private void setupMenuClickListeners(View rootView) {
        binding.menuProfileInfo.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.shoestore.ui.activity.ProfileInfoActivity.class);
            startActivity(intent);
        });

        binding.menuChangePassword.setOnClickListener(v ->
            Toast.makeText(requireContext(), "Đổi mật khẩu", Toast.LENGTH_SHORT).show()
        );

        binding.menuAddressBook.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.shoestore.ui.activity.AddressListActivity.class);
            startActivity(intent);
        });

        binding.menuDiscounts.setOnClickListener(v ->
            Toast.makeText(requireContext(), "Mã giảm giá", Toast.LENGTH_SHORT).show()
        );

        binding.menuNotifications.setOnClickListener(v ->
            Toast.makeText(requireContext(), "Thông báo", Toast.LENGTH_SHORT).show()
        );

        binding.menuReturns.setOnClickListener(v -> {
            try {
                Navigation.findNavController(rootView).navigate(R.id.nav_orders);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Quản lý trả hàng", Toast.LENGTH_SHORT).show();
            }
        });

        binding.menuReviews.setOnClickListener(v ->
            Toast.makeText(requireContext(), "Đánh giá của tôi", Toast.LENGTH_SHORT).show()
        );

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SharedPrefManager.getInstance(requireContext()).logout();
            Intent intent = new Intent(requireContext(), CustomerLoginActivity.class);
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
