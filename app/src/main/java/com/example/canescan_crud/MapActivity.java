package com.example.canescan_crud;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    private MapView map = null;
    private FirebaseFirestore db;
    private List<Map<String, Object>> scanLogs = new ArrayList<>();
    private String currentFilter = "All";
    private String searchQuery = "";
    
    private CardView cvDetails;
    private EditText etEditName, etEditDescription;
    private Button btnSave, btnCancel;
    private String selectedScanId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        map = findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        cvDetails = findViewById(R.id.cv_details);
        etEditName = findViewById(R.id.et_edit_name);
        etEditDescription = findViewById(R.id.et_edit_description);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        map.getController().setZoom(18.0);
        map.getController().setCenter(new GeoPoint(12.8797, 121.7740));

        findViewById(R.id.iv_back).setOnClickListener(v -> {
            AccessibilityHelper.handleViewClick(this, v);
            finish();
        });

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

        btnAll.setOnClickListener(v -> {
            AccessibilityHelper.handleViewClick(this, v);
            currentFilter = "All";
            refreshMap();
        });
        btnInfected.setOnClickListener(v -> {
            AccessibilityHelper.handleViewClick(this, v);
            currentFilter = "Infected";
            refreshMap();
        });
        btnHealthy.setOnClickListener(v -> {
            AccessibilityHelper.handleViewClick(this, v);
            currentFilter = "Healthy";
            refreshMap();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                refreshMap();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupDetailsActions() {
        btnCancel.setOnClickListener(v -> {
            AccessibilityHelper.handleViewClick(this, v);
            cvDetails.setVisibility(View.GONE);
        });

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
                            loadScanPins(); // Reload to refresh map data
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadScanPins() {
        db.collection("scan_logs").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                scanLogs.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Map<String, Object> data = document.getData();
                    data.put("id", document.getId());
                    scanLogs.add(data);
                }
                refreshMap();
            } else {
                Toast.makeText(this, "Error loading scans", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshMap() {
        map.getOverlays().clear();
        for (Map<String, Object> log : scanLogs) {
            String name = String.valueOf(log.getOrDefault("name", "Unknown"));
            String type = String.valueOf(log.getOrDefault("type", "Healthy"));
            Double lat = (Double) log.get("latitude");
            Double lon = (Double) log.get("longitude");

            if (lat == null || lon == null || lat == 0.0) continue;

            // Filter logic
            if (!currentFilter.equals("All") && !type.equalsIgnoreCase(currentFilter)) continue;
            if (!searchQuery.isEmpty() && !name.toLowerCase().contains(searchQuery)) continue;

            GeoPoint center = new GeoPoint(lat, lon);
            Polygon circle = new Polygon();
            circle.setPoints(Polygon.pointsAsCircle(center, 15.0));
            
            int color = type.equalsIgnoreCase("Infected") ? 0xFFFF0000 : 0xFF84AD7F;
            circle.setFillColor(0x44FFFFFF & color);
            circle.setStrokeColor(color);
            circle.setStrokeWidth(2);

            circle.setOnClickListener((polygon, mapView, eventPos) -> {
                AccessibilityHelper.handleViewClick(this, cvDetails);
                showScanDetails(log);
                return true;
            });

            map.getOverlays().add(circle);
        }
        map.invalidate();
    }

    private void showScanDetails(Map<String, Object> log) {
        selectedScanId = (String) log.get("id");
        etEditName.setText(String.valueOf(log.getOrDefault("name", "")));
        etEditDescription.setText(String.valueOf(log.getOrDefault("description", "")));
        cvDetails.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        AccessibilityHelper.updateSettings(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}
