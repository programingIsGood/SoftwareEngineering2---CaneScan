package com.example.canescan_crud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView ivProfileHeader, ivProfileMain;
    private android.widget.EditText etUsername, etBio;
    private AutoCompleteTextView etGender;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        ivProfileHeader = findViewById(R.id.iv_profile_circle);
        ivProfileMain = findViewById(R.id.iv_profile_main);
        
        etUsername = findViewById(R.id.et_username);
        etGender = findViewById(R.id.et_gender);
        etBio = findViewById(R.id.et_bio);
        
        // Setup Gender Dropdown
        String[] genderOptions = {"Male", "Female", "Not prefer to say"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genderOptions);
        etGender.setAdapter(adapter);
        
        // Show dropdown on click
        etGender.setOnClickListener(v -> etGender.showDropDown());
        etGender.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etGender.showDropDown();
            }
        });

        View tvClear = findViewById(R.id.tv_upload_clear);
        
        sharedPreferences = getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);

        // Dark Mode Setup
        SwitchCompat switchDarkMode = findViewById(R.id.switch_dark_mode);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
        });

        // Load saved image on startup
        String savedImageUri = sharedPreferences.getString("profile_image_uri", null);
        if (savedImageUri != null) {
            Uri imageUri = Uri.parse(savedImageUri);
            loadProfileImage(imageUri);
        }

        View.OnClickListener uploadListener = v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        };

        ivProfileHeader.setOnClickListener(uploadListener);
        ivProfileMain.setOnClickListener(uploadListener);

        // Clear Profile Logic
        if (tvClear != null) {
            tvClear.setOnClickListener(v -> {
                // Clear Preferences
                sharedPreferences.edit().remove("profile_image_uri").apply();
                
                // Reset Images to placeholder
                ivProfileHeader.setImageResource(R.drawable.user);
                ivProfileMain.setImageResource(R.drawable.user);
                
                // Clear Input Fields
                if (etUsername != null) etUsername.setText("");
                if (etGender != null) etGender.setText("");
                if (etBio != null) etBio.setText("");
                
                Toast.makeText(this, "Profile cleared", Toast.LENGTH_SHORT).show();
            });
        }

        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                // Already on Settings (Profile), just refresh or do nothing
                Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            
            // Persist the URI
            sharedPreferences.edit().putString("profile_image_uri", imageUri.toString()).apply();
            
            loadProfileImage(imageUri);
            Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileImage(Uri uri) {
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(ivProfileHeader);
        
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(ivProfileMain);
    }
}