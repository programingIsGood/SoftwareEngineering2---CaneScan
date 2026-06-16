package com.example.canescan_crud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class DashboardActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private ImageView ivProfileHeader;
    private TextView tvScansToday, tvInfectionsTotal, tvHealthyTotal, tvRecentName, tvRecentCondition;
    private SharedPreferences sharedPreferences;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private double pendingLat, pendingLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme configuration preferences BEFORE super.onCreate context initializations
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
        tvScansToday = findViewById(R.id.tv_scans_today);
        tvInfectionsTotal = findViewById(R.id.tv_infections_total);
        tvHealthyTotal = findViewById(R.id.tv_healthy_total);
        tvRecentName = findViewById(R.id.tv_recent_name);
        tvRecentCondition = findViewById(R.id.tv_recent_condition);

        updateProfileImage();
        loadDashboardData();

        // Initialize Accessibility tracking layer modules
        AccessibilityHelper.init(this);
        AccessibilityHelper.speak("You're in Dashboard");

        ivProfileHeader.setOnClickListener(v -> {
            AccessibilityHelper.handleViewClick(this, v);
            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        View navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        View navHistory = findViewById(R.id.nav_history);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                startActivity(new Intent(DashboardActivity.this, HistoryActivity.class));
            });
        }

        View navMap = findViewById(R.id.nav_map);
        if (navMap != null) {
            navMap.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                startActivity(new Intent(DashboardActivity.this, MapActivity.class));
            });
        }

        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                Intent intent = new Intent(DashboardActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        // Functional Context Shortcut Link Assignments
        View btnViewPlantation = findViewById(R.id.btn_view_plantation);
        if (btnViewPlantation != null) {
            btnViewPlantation.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                startActivity(new Intent(DashboardActivity.this, HistoryActivity.class));
            });
        }

        View btnViewRecent = findViewById(R.id.btn_view_recent);
        if (btnViewRecent != null) {
            btnViewRecent.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                startActivity(new Intent(DashboardActivity.this, HistoryActivity.class));
            });
        }

        Button btnScanNow = findViewById(R.id.btn_scan_now);
        if (btnScanNow != null) {
            btnScanNow.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                showImageSourceDialog();
            });
        }
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Upload from Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkLocationPermissionAndAction(this::openCamera);
                    } else {
                        checkLocationPermissionAndAction(this::openGallery);
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void loadDashboardData() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        // Fetch all scan logs for the user to calculate stats
        db.collection("scan_logs")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int healthyCnt = 0;
                    int infectedCnt = 0;
                    int todayCnt = 0;

                    long todayStart = new java.util.Date().getTime() - (new java.util.Date().getTime() % 86400000);
                    com.google.firebase.firestore.QueryDocumentSnapshot recentDoc = null;

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        if ("Healthy".equalsIgnoreCase(type)) {
                            healthyCnt++;
                        } else if (type != null && !"Unknown".equalsIgnoreCase(type) && !"Error".equalsIgnoreCase(type)) {
                            // Any specific disease name (e.g. Smut, Red Rot) counts as Infected
                            infectedCnt++;
                        }

                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
                        if (ts != null) {
                            if (ts.toDate().getTime() >= todayStart) {
                                todayCnt++;
                            }
                            
                            // Track most recent scan manually from the list
                            if (recentDoc == null || ts.compareTo(recentDoc.getTimestamp("timestamp")) > 0) {
                                recentDoc = doc;
                            }
                        }
                    }

                    tvScansToday.setText(String.valueOf(todayCnt));
                    tvInfectionsTotal.setText(String.valueOf(infectedCnt));
                    tvHealthyTotal.setText(String.valueOf(healthyCnt));

                    // Load most recent diagnose from the results we already have
                    if (recentDoc != null) {
                        String recentType = recentDoc.getString("type");
                        tvRecentName.setText(recentDoc.getString("name"));
                        tvRecentCondition.setText(recentType);
                        
                        // Dynamic styling based on 6-class detection
                        if ("Healthy".equalsIgnoreCase(recentType)) {
                            tvRecentCondition.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                        } else if (recentType != null && !"---".equals(recentType)) {
                            // Any of the 5 disease classes
                            tvRecentCondition.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
                        }
                    } else {
                        tvRecentName.setText("---");
                        tvRecentCondition.setText("---");
                    }
                });
    }

    private void checkLocationPermissionAndAction(Runnable action) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        pendingLat = location.getLatitude();
                        pendingLon = location.getLongitude();
                        action.run();
                    } else {
                        Toast.makeText(this, "Could not get accurate location. Make sure GPS is on.", Toast.LENGTH_LONG).show();
                        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                pendingLat = lastLoc.getLatitude();
                                pendingLon = lastLoc.getLongitude();
                            } else {
                                pendingLat = 0.0;
                                pendingLon = 0.0;
                            }
                            action.run();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    pendingLat = 0.0;
                    pendingLon = 0.0;
                    action.run();
                });
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void saveScanWithLocation(double lat, double lon, String imageUrlFromServer, JsonObject rawJson) {
        String userId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "anonymous";

        // Extract classification fields from raw JSON (or use defaults if null)
        String status = "Healthy";
        double confidence = 0.0;
        Object detections = null;

        if (rawJson != null) {
            Gson gson = new Gson();
            // Parse detections array
            if (rawJson.has("best_detections") && rawJson.get("best_detections").isJsonArray()) {
                JsonArray detectionsArr = rawJson.getAsJsonArray("best_detections");
                if (detectionsArr.size() > 0) {
                    JsonObject first = detectionsArr.get(0).getAsJsonObject();
                    status = first.has("class_name") ? first.get("class_name").getAsString() : "Healthy";
                    confidence = first.has("confidence") ? first.get("confidence").getAsDouble() : 0.0;
                }
                
                detections = gson.fromJson(detectionsArr, List.class);
                detections = sanitizeForFirestore(detections);
            }
        }

        // Build scan_logs document (Slim version)
        Map<String, Object> scanLog = new HashMap<>();
        scanLog.put("user_id", userId);
        scanLog.put("latitude", lat);
        scanLog.put("longitude", lon);
        scanLog.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        // Firestore has a 1MB limit per document. 
        // We only save the image if it's within a safe size (e.g. < 800KB)
        String finalImageUrl = "https://firebasestorage.googleapis.com/.../sample.jpg";
        if (imageUrlFromServer != null && imageUrlFromServer.length() < 800000) {
            finalImageUrl = imageUrlFromServer;
        }

        scanLog.put("image_url", finalImageUrl);
        scanLog.put("name", "Scan " + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date()));
        scanLog.put("status", status);
        scanLog.put("type", status);

        // Build diagnostic_result map (Minimal version)
        Map<String, Object> diagnosticResult = new HashMap<>();
        diagnosticResult.put("pathogen_name", status);
        diagnosticResult.put("confidence_score", confidence);
        if (detections != null) diagnosticResult.put("detections", detections);

        db.collection("scan_logs").add(scanLog)
                .addOnSuccessListener(documentReference -> {
                    String scanId = documentReference.getId();
                    diagnosticResult.put("scan_id", scanId);

                    db.collection("diagnostic_results").add(diagnosticResult)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Scan and Diagnosis saved!", Toast.LENGTH_SHORT).show();
                                loadDashboardData();
                            })
                            .addOnFailureListener(e -> {
                                e.printStackTrace();
                                Toast.makeText(this, "DB Error (Diag): " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(this, "DB Error (Log): " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void performPrediction(Bitmap bitmap, double lat, double lon) {
        // Scale bitmap to a reasonable size for ML processing (e.g., max 1024px)
        Bitmap scaledBitmap = scaleBitmap(bitmap, 1024);
        
        // Convert scaled bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] byteArray = stream.toByteArray();
        
        // Prepare original photo as fallback
        String base64Fallback = "data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.jpg",
                        RequestBody.create(byteArray, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url("https://8ee5-34-151-163-122.ngrok-free.app/predict")
                .post(requestBody)
                .addHeader("ngrok-skip-browser-warning", "true")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, "Prediction failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveScanWithLocation(lat, lon, base64Fallback, null);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    android.util.Log.d("SCAN_DEBUG", "Server Response: " + responseData);
                    Gson gson = new Gson();
                    JsonObject jsonObject = gson.fromJson(responseData, JsonObject.class);

                    // Look for annotated image data in response
                    String serverImage = null;
                    if (jsonObject.has("annotated_image_base64")) {
                        serverImage = jsonObject.get("annotated_image_base64").getAsString();
                        if (!serverImage.startsWith("data:") && !serverImage.startsWith("http")) {
                            serverImage = "data:image/jpeg;base64," + serverImage;
                        }
                    } else if (jsonObject.has("annotated_image")) {
                        serverImage = jsonObject.get("annotated_image").getAsString();
                        if (!serverImage.startsWith("data:") && !serverImage.startsWith("http")) {
                            serverImage = "data:image/jpeg;base64," + serverImage;
                        }
                    } else if (jsonObject.has("image_url")) {
                        serverImage = jsonObject.get("image_url").getAsString();
                    }

                    // Priority: Server Image > Original Fallback
                    final String finalImage = (serverImage != null) ? serverImage : base64Fallback;

                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this, "Analysis complete! Saving...", Toast.LENGTH_SHORT).show();
                        saveScanWithLocation(lat, lon, finalImage, jsonObject);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                        saveScanWithLocation(lat, lon, base64Fallback, null);
                    });
                }
            }
        });
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        performPrediction(imageBitmap, pendingLat, pendingLon);
                    }
                }
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    Toast.makeText(this, "Image selected, processing...", Toast.LENGTH_SHORT).show();
                    try {
                        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageUri);
                        Bitmap bitmap = ImageDecoder.decodeBitmap(source);
                        // Ensure bitmap is mutable and not in hardware memory
                        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        performPrediction(bitmap, pendingLat, pendingLon);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                showImageSourceDialog();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProfileImage() {
        if (ivProfileHeader == null || sharedPreferences == null) return;
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

    private Object sanitizeForFirestore(Object value) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, Object> sanitized = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                sanitized.put(entry.getKey(), sanitizeForFirestore(entry.getValue()));
            }
            return sanitized;
        } else if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            for (Object element : list) {
                if (element instanceof List) {
                    return new Gson().toJson(list);
                }
            }
            List<Object> sanitized = new ArrayList<>();
            for (Object element : list) {
                sanitized.add(sanitizeForFirestore(element));
            }
            return sanitized;
        }
        return value;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileImage();
        loadDashboardData();
    }
}