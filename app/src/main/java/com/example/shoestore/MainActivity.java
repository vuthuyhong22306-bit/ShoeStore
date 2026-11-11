package com.example.shoestore;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shoestore.ui.activity.CustomerLoginActivity;
import com.example.shoestore.ui.activity.CustomerMainActivity;
import com.example.shoestore.utils.SharedPrefManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SharedPrefManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, CustomerMainActivity.class));
        } else {
            startActivity(new Intent(this, CustomerLoginActivity.class));
        }
        finish();
    }
}
