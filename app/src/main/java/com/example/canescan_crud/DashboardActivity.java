package com.example.canescan_crud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private ImageView ivProfileHeader;
    private SharedPreferences sharedPreferences;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private double pendingLat, pendingLon;

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
        setContentView(R.layout.dashboard_activity);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ivProfileHeader = findViewById(R.id.iv_profile);
        sharedPreferences = getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);

        updateProfileImage();

        ivProfileHeader.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        View navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        View navMap = findViewById(R.id.nav_map);
        if (navMap != null) {
            navMap.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, MapActivity.class));
            });
        }

        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        Button btnScanNow = findViewById(R.id.btn_scan_now);
        if (btnScanNow != null) {
            btnScanNow.setOnClickListener(v -> {
                checkLocationPermissionAndScan();
            });
        }
    }

    private void checkLocationPermissionAndScan() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Use getCurrentLocation for higher accuracy than getLastLocation
        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        pendingLat = location.getLatitude();
                        pendingLon = location.getLongitude();
                        openCamera();
                    } else {
                        Toast.makeText(this, "Could not get accurate location. Make sure GPS is on.", Toast.LENGTH_LONG).show();
                        // Fallback to last known location if current is unavailable
                        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                pendingLat = lastLoc.getLatitude();
                                pendingLon = lastLoc.getLongitude();
                            } else {
                                pendingLat = 0.0;
                                pendingLon = 0.0;
                            }
                            openCamera();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    pendingLat = 0.0;
                    pendingLon = 0.0;
                    openCamera();
                });
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void saveScanWithLocation(double lat, double lon) {
        String userId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "anonymous";

        Map<String, Object> scanLog = new HashMap<>();
        scanLog.put("user_id", userId);
        scanLog.put("latitude", lat);
        scanLog.put("longitude", lon);
        scanLog.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        scanLog.put("image_url", "sample_url"); // Placeholder

        db.collection("scan_logs").add(scanLog)
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving scan", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Save to Firestore ONLY after successful picture capture
            saveScanWithLocation(pendingLat, pendingLon);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissionAndScan();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProfileImage() {
        String savedImageUri = sharedPreferences.getString("profile_image_uri", null);
        if (savedImageUri != null) {
            Uri imageUri = Uri.parse(savedImageUri);
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(ivProfileHeader);
        } else {
            ivProfileHeader.setImageResource(R.drawable.user);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileImage();
    }
}