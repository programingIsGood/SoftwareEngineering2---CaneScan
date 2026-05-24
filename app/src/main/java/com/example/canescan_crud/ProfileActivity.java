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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView ivProfileHeader, ivProfileMain;
    private EditText etUsername, etBio;
    private AutoCompleteTextView etGender;
    private SwitchCompat swDarkMode;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference BEFORE super.onCreate
        sharedPreferences = getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivProfileHeader = findViewById(R.id.iv_profile_circle);
        ivProfileMain = findViewById(R.id.iv_profile_main);
        
        etUsername = findViewById(R.id.et_username);
        etGender = findViewById(R.id.et_gender);
        etBio = findViewById(R.id.et_bio);

        swDarkMode = findViewById(R.id.sw_dark_mode);
        if (swDarkMode != null) {
            swDarkMode.setChecked(isDarkMode);
            swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                // Activity will recreate automatically to apply theme
            });
        }

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

        // Account logic
        TextView tvLogout = findViewById(R.id.tv_logout);
        TextView tvDeleteAccount = findViewById(R.id.tv_delete_account);

        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> {
                mAuth.signOut();
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        if (tvDeleteAccount != null) {
            tvDeleteAccount.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Account")
                        .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                // Delete from Firestore first
                                db.collection("users").document(userId).delete()
                                        .addOnSuccessListener(aVoid -> {
                                            // Then delete Auth user
                                            user.delete().addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(ProfileActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    Toast.makeText(ProfileActivity.this, "Failed to delete account: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(ProfileActivity.this, "Error deleting data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
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

        View navMap = findViewById(R.id.nav_map);
        if (navMap != null) {
            navMap.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, MapActivity.class);
                startActivity(intent);
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