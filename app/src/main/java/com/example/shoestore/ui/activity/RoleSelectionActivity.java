package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.R;
import com.example.shoestore.utils.SharedPrefManager;

public class RoleSelectionActivity extends AppCompatActivity {

    private View layoutRoleStaff;
    private View layoutRoleCustomer;
    private View layoutRoleAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        layoutRoleStaff = findViewById(R.id.layoutRoleStaff);
        layoutRoleCustomer = findViewById(R.id.layoutRoleCustomer);
        layoutRoleAdmin = findViewById(R.id.layoutRoleAdmin);

        if (layoutRoleStaff != null) {
            layoutRoleStaff.setOnClickListener(v -> {
                startActivity(new Intent(RoleSelectionActivity.this, StaffLoginActivity.class));
            });
        }

        if (layoutRoleCustomer != null) {
            layoutRoleCustomer.setOnClickListener(v -> {
                if (SharedPrefManager.getInstance(this).isLoggedIn()) {
                    startActivity(new Intent(RoleSelectionActivity.this, CustomerMainActivity.class));
                } else {
                    startActivity(new Intent(RoleSelectionActivity.this, CustomerLoginActivity.class));
                }
            });
        }

        if (layoutRoleAdmin != null) {
            layoutRoleAdmin.setOnClickListener(v -> {
                startActivity(new Intent(RoleSelectionActivity.this, AdminLoginActivity.class));
            });
        }
    }
}
