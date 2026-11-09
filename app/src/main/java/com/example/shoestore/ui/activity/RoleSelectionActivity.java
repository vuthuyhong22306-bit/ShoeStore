package com.example.shoestore.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.MainActivity;
import com.example.shoestore.R;

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

        layoutRoleStaff.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, StaffLoginActivity.class);
            startActivity(intent);
        });
        layoutRoleCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, CustomerLoginActivity.class);
            startActivity(intent);
        });
        layoutRoleAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });
    }

    private void navigateToMain(String role) {
        Intent intent = new Intent(RoleSelectionActivity.this, MainActivity.class);
        intent.putExtra("SELECTED_ROLE", role);
        startActivity(intent);
    }
}
