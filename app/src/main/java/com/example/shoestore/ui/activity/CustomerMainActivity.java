package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.shoestore.R;
import com.example.shoestore.databinding.ActivityCustomerMainBinding;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.repository.CartRepository;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.android.material.badge.BadgeDrawable;

public class CustomerMainActivity extends AppCompatActivity {
    private ActivityCustomerMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String role = SharedPrefManager.getInstance(this).getUserRole();
        if (role == null) {
            startActivity(new Intent(this, CustomerLoginActivity.class));
            finish();
            return;
        }

        binding = ActivityCustomerMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_customer);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }

        int userId = SharedPrefManager.getInstance(this).getUserId();
        CartRepository cartRepository = new CartRepository(getApplication());
        cartRepository.getCartItems(userId).observe(this, items -> {
            if (binding != null) {
                BadgeDrawable badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_cart);
                if (items != null && !items.isEmpty()) {
                    int sum = 0;
                    for (CartItem item : items) {
                        sum += item.getQuantity();
                    }
                    badge.setNumber(sum);
                    badge.setBackgroundColor(Color.parseColor("#E5192C"));
                    badge.setVisible(true);
                } else {
                    badge.setVisible(false);
                }
            }
        });
    }
}
