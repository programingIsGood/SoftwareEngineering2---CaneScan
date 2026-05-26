package com.example.canescan_crud;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private com.google.android.material.button.MaterialButton btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Theme customization dynamic loading
        SharedPreferences prefs = getSharedPreferences("CaneScanPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        // Warm up accessibility services early
        AccessibilityHelper.init(this);

        mAuth = FirebaseAuth.getInstance();

        // 0. Auto-Login Logic (Stay Signed In verification check)
        boolean staySignedIn = prefs.getBoolean("stay_signed_in", false);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (staySignedIn && currentUser != null) {
            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
            finish();
            return;
        }

        // Set visual layout context if session verification conditions aren't bypassed
        setContentView(R.layout.activity_main);

        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);

        // Click Listeners assignments
        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class)));

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }
}