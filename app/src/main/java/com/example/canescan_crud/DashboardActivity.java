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

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class DashboardActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView ivProfileHeader;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_activity);

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
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            });
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