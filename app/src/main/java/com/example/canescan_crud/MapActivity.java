package com.example.canescan_crud;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private MapView map = null;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Map<String, Object>> scanLogs = new ArrayList<>();
    private String currentFilter = "All";
    private String searchQuery = "";

    private CardView cvDetails;
    private EditText etEditName, etEditDescription;
    private ImageView ivDetailImage;
    private Button btnSave, btnCancel;
    private String selectedScanId;

    private RecyclerView rvSearchResults;
    private SearchSuggestionAdapter searchAdapter;
    private List<Map<String, Object>> filteredSuggestions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load OpenStreetMap configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        map = findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // UI Detail Components
        cvDetails = findViewById(R.id.cv_details);
        ivDetailImage = findViewById(R.id.iv_detail_image);
        etEditName = findViewById(R.id.et_edit_name);
        etEditDescription = findViewById(R.id.et_edit_description);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        rvSearchResults = findViewById(R.id.rv_search_results);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchSuggestionAdapter(filteredSuggestions, this::onSuggestionSelected);
        rvSearchResults.setAdapter(searchAdapter);

        // Set initial default zoom (Barangay level)
        map.getController().setZoom(18.0);
        map.getController().setCenter(new GeoPoint(12.8797, 121.7740));

        // Use iv_back (Standard navigation)
        View backButton = findViewById(R.id.iv_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                finish();
            });
        }

        setupFiltersAndSearch();
        setupDetailsActions();
        loadScanPins();

        AccessibilityHelper.init(this);
    }

    private void setupFiltersAndSearch() {
        TextView btnAll = findViewById(R.id.btn_filter_all);
        TextView btnInfected = findViewById(R.id.btn_filter_infected);
        TextView btnHealthy = findViewById(R.id.btn_filter_healthy);
        EditText etSearch = findViewById(R.id.et_map_search);

        if (btnAll != null) {
            btnAll.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                currentFilter = "All";
                refreshMap();
            });
        }
        if (btnInfected != null) {
            btnInfected.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                currentFilter = "Infected";
                refreshMap();
            });
        }
        if (btnHealthy != null) {
            btnHealthy.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                currentFilter = "Healthy";
                refreshMap();
            });
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().toLowerCase();
                    updateSearchSuggestions(searchQuery);
                    refreshMap();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void updateSearchSuggestions(String query) {
        filteredSuggestions.clear();
        if (query.isEmpty()) {
            rvSearchResults.setVisibility(View.GONE);
        } else {
            for (Map<String, Object> log : scanLogs) {
                String name = String.valueOf(log.getOrDefault("name", ""));
                if (name.toLowerCase().contains(query)) {
                    filteredSuggestions.add(log);
                }
            }
            if (!filteredSuggestions.isEmpty()) {
                searchAdapter.updateList(filteredSuggestions);
                rvSearchResults.setVisibility(View.VISIBLE);
            } else {
                rvSearchResults.setVisibility(View.GONE);
            }
        }
    }

    private void onSuggestionSelected(Map<String, Object> suggestion) {
        rvSearchResults.setVisibility(View.GONE);
        Double lat = (Double) suggestion.get("latitude");
        Double lon = (Double) suggestion.get("longitude");
        if (lat != null && lon != null) {
            GeoPoint target = new GeoPoint(lat, lon);
            map.getController().animateTo(target);
            map.getController().setZoom(21.0);
            showScanDetails(suggestion);
        }
    }

    private void setupDetailsActions() {
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                cvDetails.setVisibility(View.GONE);
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                String newName = etEditName.getText().toString().trim();
                String newDesc = etEditDescription.getText().toString().trim();

                if (selectedScanId != null) {
                    db.collection("scan_logs").document(selectedScanId)
                            .update("name", newName, "description", newDesc)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show();
                                cvDetails.setVisibility(View.GONE);
                                loadScanPins(); // Reload data from Firestore to keep map synchronized
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private void loadScanPins() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

        db.collection("scan_logs")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        scanLogs.clear();
                        List<GeoPoint> points = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> data = document.getData();
                            data.put("id", document.getId());
                            scanLogs.add(data);

                            Double lat = (Double) data.get("latitude");
                            Double lon = (Double) data.get("longitude");
                            if (lat != null && lon != null && lat != 0.0) {
                                points.add(new GeoPoint(lat, lon));
                            }
                        }

                        refreshMap();

                        // If points are found, dynamically shift map focus to the newest entry
                        if (!points.isEmpty()) {
                            zoomToFitPoints(points);
                        }
                    } else {
                        Toast.makeText(this, "Error loading scans", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void refreshMap() {
        if (map == null) return;
        map.getOverlays().clear();

        for (Map<String, Object> logItem : scanLogs) {
            String name = String.valueOf(logItem.getOrDefault("name", "Unknown"));
            String type = String.valueOf(logItem.getOrDefault("type", "Healthy"));
            Double lat = (Double) logItem.get("latitude");
            Double lon = (Double) logItem.get("longitude");

            if (lat == null || lon == null || lat == 0.0) continue;

            // Apply Search Filtering and Category Sorting Layout
            if (!currentFilter.equals("All") && !type.equalsIgnoreCase(currentFilter)) continue;
            if (!searchQuery.isEmpty() && !name.toLowerCase().contains(searchQuery)) continue;

            GeoPoint center = new GeoPoint(lat, lon);
            Polygon circle = new Polygon();
            circle.setPoints(Polygon.pointsAsCircle(center, 15.0)); // 15 meter radius

            // Set Hex color variables: Crimson Red for Infected, Soft Green for Healthy
            int color = type.equalsIgnoreCase("Infected") ? 0xFFFF0000 : 0xFF84AD7F;
            circle.setFillColor(0x44FFFFFF & color); // Maintains opacity channel masking
            circle.setStrokeColor(color);
            circle.setStrokeWidth(2);
            circle.setTitle("Scan Area: " + logItem.get("id"));

            circle.setOnClickListener((polygon, mapView, eventPos) -> {
                if (cvDetails != null) {
                    AccessibilityHelper.handleViewClick(this, cvDetails);
                    showScanDetails(logItem);
                }
                return true;
            });

            map.getOverlays().add(circle);
        }
        map.invalidate(); // Redraw map elements layout canvas safely
    }

    private void showScanDetails(Map<String, Object> log) {
        selectedScanId = (String) log.get("id");
        etEditName.setText(String.valueOf(log.getOrDefault("name", "")));
        etEditDescription.setText(String.valueOf(log.getOrDefault("description", "")));

        String imageUrl = (String) log.get("image_url");
        if (ivDetailImage != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.sugarcane_close)
                    .error(R.drawable.sugarcane_close)
                    .into(ivDetailImage);

            ivDetailImage.setOnClickListener(v -> showFullImageDialog(imageUrl));
        }

        cvDetails.setVisibility(View.VISIBLE);
    }

    private void showFullImageDialog(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        final Dialog fullImageDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        fullImageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        fullImageDialog.setContentView(R.layout.dialog_full_image);

        ImageView ivFull = fullImageDialog.findViewById(R.id.iv_full_image);
        View btnBack = fullImageDialog.findViewById(R.id.btn_back_full);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.sugarcane_close)
                .into(ivFull);

        btnBack.setOnClickListener(v -> fullImageDialog.dismiss());
        fullImageDialog.show();
    }

    private void zoomToFitPoints(List<GeoPoint> points) {
        map.post(() -> {
            if (points.isEmpty()) return;
            // Target focus onto the latest scan position
            GeoPoint lastPoint = points.get(points.size() - 1);
            map.getController().setZoom(20.0);
            map.getController().animateTo(lastPoint);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
        AccessibilityHelper.updateSettings(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
    }
}