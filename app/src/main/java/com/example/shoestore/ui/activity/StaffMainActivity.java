package com.example.shoestore.ui.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityStaffMainBinding;
import com.example.shoestore.ui.fragment.StaffAccountFragment;
import com.example.shoestore.ui.fragment.StaffOrdersFragment;
import com.example.shoestore.ui.fragment.StaffReturnsFragment;
import com.example.shoestore.ui.fragment.StaffStockFragment;

public class StaffMainActivity extends AppCompatActivity {

    private ActivityStaffMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStaffMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Default tab: Orders
        if (savedInstanceState == null) {
            replaceFragment(new StaffOrdersFragment());
        }

        binding.staffBottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_staff_orders) {
                replaceFragment(new StaffOrdersFragment());
                return true;
            } else if (itemId == R.id.nav_staff_stock) {
                replaceFragment(new StaffStockFragment());
                return true;
            } else if (itemId == R.id.nav_staff_returns) {
                replaceFragment(new StaffReturnsFragment());
                return true;
            } else if (itemId == R.id.nav_staff_account) {
                replaceFragment(new StaffAccountFragment());
                return true;
            }
            return false;
        });
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.staff_fragment_container, fragment)
                .commit();
    }
}
