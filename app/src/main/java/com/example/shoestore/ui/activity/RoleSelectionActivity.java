package com.example.shoestore.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.shoestore.R;
import com.example.shoestore.utils.SharedPrefManager;

public class RoleSelectionActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;

    private View layoutRoleStaff;
    private View layoutRoleCustomer;
    private View layoutRoleAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check runtime permission for POST_NOTIFICATIONS (Android 13+)
        checkAndRequestNotificationPermission();

        // Handle navigation if launched from Push Notification click
        if (handleNotificationIntent(getIntent())) {
            return;
        }

        setContentView(R.layout.activity_role_selection);

        layoutRoleStaff = findViewById(R.id.layoutRoleStaff);
        layoutRoleCustomer = findViewById(R.id.layoutRoleCustomer);
        layoutRoleAdmin = findViewById(R.id.layoutRoleAdmin);

        if (layoutRoleStaff != null) {
            layoutRoleStaff.setOnClickListener(v -> {
                SharedPrefManager pref = SharedPrefManager.getInstance(this);
                if (pref.isLoggedIn() && ("STAFF".equalsIgnoreCase(pref.getUserRole()) || "EMPLOYEE".equalsIgnoreCase(pref.getUserRole()))) {
                    startActivity(new Intent(RoleSelectionActivity.this, StaffMainActivity.class));
                } else {
                    startActivity(new Intent(RoleSelectionActivity.this, StaffLoginActivity.class));
                }
            });
        }

        if (layoutRoleCustomer != null) {
            layoutRoleCustomer.setOnClickListener(v -> {
                SharedPrefManager pref = SharedPrefManager.getInstance(this);
                if (pref.isLoggedIn() && "CUSTOMER".equalsIgnoreCase(pref.getUserRole())) {
                    startActivity(new Intent(RoleSelectionActivity.this, CustomerMainActivity.class));
                } else {
                    startActivity(new Intent(RoleSelectionActivity.this, CustomerLoginActivity.class));
                }
            });
        }

        if (layoutRoleAdmin != null) {
            layoutRoleAdmin.setOnClickListener(v -> {
                SharedPrefManager pref = SharedPrefManager.getInstance(this);
                if (pref.isLoggedIn() && "ADMIN".equalsIgnoreCase(pref.getUserRole())) {
                    startActivity(new Intent(RoleSelectionActivity.this, AdminMainActivity.class));
                } else {
                    startActivity(new Intent(RoleSelectionActivity.this, AdminLoginActivity.class));
                }
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private boolean handleNotificationIntent(Intent intent) {
        if (intent != null) {
            int orderId = intent.getIntExtra("ORDER_ID", -1);
            if (orderId <= 0) {
                orderId = intent.getIntExtra("PENDING_NOTIF_ORDER_ID", -1);
            }

            if (orderId > 0) {
                SharedPrefManager pref = SharedPrefManager.getInstance(this);
                if (pref.isLoggedIn()) {
                    Intent detailIntent = new Intent(this, OrderDetailActivity.class);
                    detailIntent.putExtra("ORDER_ID", orderId);
                    startActivity(detailIntent);
                    finish();
                    return true;
                } else {
                    Intent loginIntent = new Intent(this, CustomerLoginActivity.class);
                    loginIntent.putExtra("ORDER_ID", orderId);
                    startActivity(loginIntent);
                    finish();
                    return true;
                }
            }
        }
        return false;
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }
}
