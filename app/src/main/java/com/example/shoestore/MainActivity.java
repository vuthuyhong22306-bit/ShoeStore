package com.example.shoestore;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.ui.activity.AdminMainActivity;
import com.example.shoestore.ui.activity.CustomerMainActivity;
import com.example.shoestore.ui.activity.StaffMainActivity;
import com.example.shoestore.utils.SharedPrefManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPrefManager pref = SharedPrefManager.getInstance(this);
        Intent intent;
        if (pref.isLoggedIn()) {
            String role = pref.getUserRole();
            if ("ADMIN".equalsIgnoreCase(role)) {
                intent = new Intent(this, AdminMainActivity.class);
            } else if ("STAFF".equalsIgnoreCase(role) || "EMPLOYEE".equalsIgnoreCase(role) || "NHANVIEN".equalsIgnoreCase(role)) {
                intent = new Intent(this, StaffMainActivity.class);
            } else {
                intent = new Intent(this, CustomerMainActivity.class);
            }
        } else {
            intent = new Intent(this, CustomerMainActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
