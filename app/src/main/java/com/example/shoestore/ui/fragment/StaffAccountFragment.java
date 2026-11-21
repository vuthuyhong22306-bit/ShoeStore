package com.example.shoestore.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.shoestore.R;
import com.example.shoestore.databinding.FragmentStaffAccountBinding;
import com.example.shoestore.ui.activity.CustomerMainActivity;
import com.example.shoestore.utils.SharedPrefManager;

public class StaffAccountFragment extends Fragment {
    private FragmentStaffAccountBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStaffAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String staffName = SharedPrefManager.getInstance(getContext()).getUserName();
        if (staffName != null && !staffName.isEmpty()) {
            binding.tvStaffName.setText(staffName);
        }

        binding.btnLogoutAccount.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        if (getContext() == null) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.msg_confirm_logout_title)
                .setMessage(R.string.msg_confirm_logout_staff)
                .setPositiveButton(R.string.btn_logout, (dialog, which) -> {
                    SharedPrefManager.getInstance(getContext()).logout();
                    Intent intent = new Intent(getContext(), CustomerMainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
