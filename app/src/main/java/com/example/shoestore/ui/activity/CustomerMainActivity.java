package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.shoestore.R;
import com.example.shoestore.model.CartItem;
import com.example.shoestore.repository.CartRepository;
import com.example.shoestore.utils.GuestDataManager;
import com.example.shoestore.utils.SharedPrefManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomerMainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_customer);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            if (bottomNavigation != null) {
                NavigationUI.setupWithNavController(bottomNavigation, navController);
            }
        }

        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                boolean isLoggedIn = SharedPrefManager.getInstance(this).isLoggedIn();

                if (!isLoggedIn && itemId != R.id.nav_home) {
                    String action = "NAV_CART";
                    String dest = "MAIN";
                    if (itemId == R.id.nav_favorites) {
                        action = "NAV_FAVORITES";
                    } else if (itemId == R.id.nav_orders) {
                        action = "NAV_ORDERS";
                        dest = "ORDERS";
                    } else if (itemId == R.id.nav_account) {
                        action = "NAV_ACCOUNT";
                        dest = "ACCOUNT";
                    }
                    GuestDataManager.getInstance(this).savePendingAction(dest, action, -1, "", "", 1, 0);
                    Toast.makeText(this, R.string.msg_please_login, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CustomerMainActivity.this, LoginActivity.class);
                    intent.putExtra(LoginActivity.EXTRA_TARGET_TAB, itemId);
                    startActivity(intent);
                    return false;
                }

                if (navController != null) {
                    return NavigationUI.onNavDestinationSelected(item, navController);
                }
                return false;
            });
        }

        checkAndNavigateToTargetTab(getIntent());

        boolean isLoggedIn = SharedPrefManager.getInstance(this).isLoggedIn();
        int userId = SharedPrefManager.getInstance(this).getUserId();
        if (isLoggedIn && userId > 0 && bottomNavigation != null) {
            CartRepository cartRepository = new CartRepository(getApplication());
            cartRepository.getCartItems(userId).observe(this, items -> {
                BadgeDrawable badge = bottomNavigation.getOrCreateBadge(R.id.nav_cart);
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
            });
        } else if (bottomNavigation != null) {
            BadgeDrawable badge = bottomNavigation.getBadge(R.id.nav_cart);
            if (badge != null) {
                badge.setVisible(false);
                badge.clearNumber();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkAndNavigateToTargetTab(intent);
    }

    private void checkAndNavigateToTargetTab(Intent intent) {
        if (intent == null || bottomNavigation == null) return;
        int targetTab = intent.getIntExtra(LoginActivity.EXTRA_TARGET_TAB, -1);
        if (targetTab != -1) {
            bottomNavigation.setSelectedItemId(targetTab);
            intent.removeExtra(LoginActivity.EXTRA_TARGET_TAB);
            return;
        }

        String navigateTo = intent.getStringExtra("NAVIGATE_TO");
        if ("CART".equalsIgnoreCase(navigateTo)) {
            bottomNavigation.setSelectedItemId(R.id.nav_cart);
            intent.removeExtra("NAVIGATE_TO");
        }
    }
}
