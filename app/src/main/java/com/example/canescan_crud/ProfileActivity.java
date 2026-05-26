package com.example.canescan_crud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView ivProfileHeader, ivProfileMain;
    private EditText etUsername, etBio;
    private AutoCompleteTextView etGender;
    private SwitchCompat swDarkMode, swSounds, swTTS;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference BEFORE super.onCreate to prevent flicker frames
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

        // UI Binding Initialization
        ivProfileHeader = findViewById(R.id.iv_profile_circle);
        ivProfileMain = findViewById(R.id.iv_profile_main);
        etUsername = findViewById(R.id.et_username);
        etGender = findViewById(R.id.et_gender);
        etBio = findViewById(R.id.et_bio);

        swDarkMode = findViewById(R.id.switch_dark_mode);
        swSounds = findViewById(R.id.sw_sounds);
        swTTS = findViewById(R.id.sw_tts);

        // Accessibility Announcements Initial Entry Hook
        AccessibilityHelper.init(this);
        AccessibilityHelper.speak("You're now in settings");

        setupTogglePreferences(isDarkMode);
        setupGenderDropdown();
        setupClearAction();
        updateProfileImage();
        setupAccountActions();
        setupNavigation();
    }

    private void setupTogglePreferences(boolean isDarkMode) {
        if (swDarkMode != null) {
            swDarkMode.setChecked(isDarkMode);
            swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AccessibilityHelper.handleViewClick(this, buttonView);
                sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
                AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                recreate(); // Dynamic contextual interface redraw
            });
        }

        if (swSounds != null) {
            swSounds.setChecked(sharedPreferences.getBoolean("enable_sounds", false));
            swSounds.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AccessibilityHelper.handleViewClick(this, buttonView);
                sharedPreferences.edit().putBoolean("enable_sounds", isChecked).apply();
            });
        }

        if (swTTS != null) {
            swTTS.setChecked(sharedPreferences.getBoolean("enable_tts", false));
            swTTS.setOnCheckedChangeListener((buttonView, isChecked) -> {
                AccessibilityHelper.handleViewClick(this, buttonView);
                sharedPreferences.edit().putBoolean("enable_tts", isChecked).apply();
                AccessibilityHelper.updateSettings(this);
            });
        }
    }

    private void setupGenderDropdown() {
        String[] genderOptions = {"Male", "Female", "Not prefer to say"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genderOptions);
        etGender.setAdapter(adapter);

        etGender.setOnClickListener(v -> etGender.showDropDown());
        etGender.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etGender.showDropDown();
            }
        });
    }

    private void setupClearAction() {
        View tvClear = findViewById(R.id.tv_upload_clear);
        if (tvClear != null) {
            tvClear.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);

                // Reset local state cache preferences safely
                sharedPreferences.edit().remove("profile_image_uri").apply();

                ivProfileHeader.setImageResource(R.drawable.user);
                ivProfileMain.setImageResource(R.drawable.user);

                if (etUsername != null) etUsername.setText("");
                if (etGender != null) etGender.setText("");
                if (etBio != null) etBio.setText("");

                Toast.makeText(this, "Profile cleared", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateProfileImage() {
        String savedImageUri = sharedPreferences.getString("profile_image_uri", null);
        if (savedImageUri != null) {
            Uri imageUri = Uri.parse(savedImageUri);
            loadProfileImage(imageUri);
        }

        View.OnClickListener uploadListener = v -> {
            AccessibilityHelper.handleViewClick(this, v);
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        };

        if (ivProfileHeader != null) ivProfileHeader.setOnClickListener(uploadListener);
        if (ivProfileMain != null) ivProfileMain.setOnClickListener(uploadListener);
    }

    private void loadProfileImage(Uri uri) {
        Glide.with(this).load(uri).placeholder(R.drawable.user).error(R.drawable.user).into(ivProfileHeader);
        Glide.with(this).load(uri).placeholder(R.drawable.user).error(R.drawable.user).into(ivProfileMain);
    }

    private void setupAccountActions() {
        TextView tvLogout = findViewById(R.id.tv_logout);
        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                mAuth.signOut();
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        TextView tvDeleteAccount = findViewById(R.id.tv_delete_account);
        if (tvDeleteAccount != null) {
            tvDeleteAccount.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                new AlertDialog.Builder(this)
                        .setTitle("Delete Account")
                        .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> executePermanentAccountDeletion())
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void executePermanentAccountDeletion() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();

        // 1. Fetch user's personal logs safely
        db.collection("scan_logs")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(scanLogs -> {
                    WriteBatch batch = db.batch();
                    List<Task<com.google.firebase.firestore.QuerySnapshot>> diagnosticTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot scanDoc : scanLogs) {
                        String scanId = scanDoc.getId();
                        diagnosticTasks.add(db.collection("diagnostic_results")
                                .whereEqualTo("scan_id", scanId)
                                .get());

                        batch.delete(scanDoc.getReference());
                    }

                    // 2. Query target structures inside diagnostic collections via combined dynamic threads
                    Tasks.whenAllSuccess(diagnosticTasks).addOnSuccessListener(resultsList -> {
                        for (Object result : resultsList) {
                            com.google.firebase.firestore.QuerySnapshot diagnosticResults = (com.google.firebase.firestore.QuerySnapshot) result;
                            for (QueryDocumentSnapshot diagDoc : diagnosticResults) {
                                batch.delete(diagDoc.getReference());
                            }
                        }

                        // 3. Mark core profile records for extraction transaction
                        batch.delete(db.collection("users").document(userId));

                        // Atomically execute relational database purge
                        batch.commit().addOnSuccessListener(aVoid -> {
                            // 4. Remove verification layer profile via Firebase Authentication standard routing
                            user.delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // 5. Hard purge runtime configuration caches and local databases
                                    sharedPreferences.edit().clear().apply();
                                    PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this).edit().clear().apply();

                                    db.clearPersistence().addOnCompleteListener(t -> {
                                        // Dynamic asset pipeline cleanups
                                        Glide.get(ProfileActivity.this).clearMemory();
                                        new Thread(() -> Glide.get(ProfileActivity.this).clearDiskCache()).start();

                                        Toast.makeText(ProfileActivity.this, "Account and all associated data permanently deleted", Toast.LENGTH_LONG).show();

                                        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    });
                                } else {
                                    String fallbackMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                    Toast.makeText(ProfileActivity.this, "Failed to delete auth account: " + fallbackMsg, Toast.LENGTH_LONG).show();
                                }
                            });
                        }).addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error committing batch deletion: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }).addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error fetching diagnostic results: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error fetching scan logs: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setupNavigation() {
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View navMap = findViewById(R.id.nav_map);
        if (navMap != null) {
            navMap.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                Intent intent = new Intent(this, MapActivity.class);
                startActivity(intent);
            });
        }

        View navHistory = findViewById(R.id.nav_history);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                Intent intent = new Intent(this, HistoryActivity.class);
                startActivity(intent);
            });
        }

        View navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                Intent intent = new Intent(this, ProfileActivity.class);
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
            if (imageUri != null) {
                sharedPreferences.edit().putString("profile_image_uri", imageUri.toString()).apply();
                loadProfileImage(imageUri);
                Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
            }
        }
    }
}