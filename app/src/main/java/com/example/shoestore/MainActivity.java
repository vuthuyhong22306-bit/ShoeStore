package com.example.shoestore;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.ui.activity.AdminMainActivity;
import com.example.shoestore.ui.activity.CustomerMainActivity;
import com.example.shoestore.ui.activity.RoleSelectionActivity;
import com.example.shoestore.ui.activity.StaffMainActivity;
import com.example.shoestore.utils.SharedPrefManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String role = getIntent().getStringExtra("SELECTED_ROLE");
        if (role == null) {
            role = SharedPrefManager.getInstance(this).getUserRole();
        }

        if (role == null) {
            startActivity(new Intent(this, RoleSelectionActivity.class));
            finish();
            return;
        }

        Intent intent;
        if ("ADMIN".equals(role)) {
            intent = new Intent(this, AdminMainActivity.class);
        } else if ("STAFF".equals(role)) {
            intent = new Intent(this, StaffMainActivity.class);
        } else {
            intent = new Intent(this, CustomerMainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
