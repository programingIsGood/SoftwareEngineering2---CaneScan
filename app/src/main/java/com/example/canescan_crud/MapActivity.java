package com.example.canescan_crud;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private MapView map = null;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox requirement: load configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();
        map = findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // Initial default zoom (Barangay level)
        map.getController().setZoom(20.0);
        map.getController().setCenter(new GeoPoint(12.8797, 121.7740));

        findViewById(R.id.fab_back).setOnClickListener(v -> finish());

        loadScanPins();
    }

    private void loadScanPins() {
        db.collection("scan_logs").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<GeoPoint> points = new ArrayList<>();
                for (DocumentSnapshot document : task.getResult()) {
                    Double lat = document.getDouble("latitude");
                    Double lon = document.getDouble("longitude");

                    if (lat != null && lon != null && lat != 0.0) {
                        GeoPoint center = new GeoPoint(lat, lon);
                        points.add(center);

                        // Create a circle to show general location (radius 50 meters)
                        Polygon circle = new Polygon();
                        circle.setPoints(Polygon.pointsAsCircle(center, 20.0));
                        circle.setFillColor(0x4484AD7F); // Semi-transparent green
                        circle.setStrokeColor(0xFF84AD7F); // Solid green border
                        circle.setStrokeWidth(2);
                        circle.setTitle("Scan Area: " + document.getId());
                        
                        map.getOverlays().add(circle);
                    }
                }

                if (!points.isEmpty()) {
                    // Zoom to fit all points
                    zoomToFitPoints(points);
                }
                map.invalidate(); // Refresh map
            } else {
                Toast.makeText(this, "Error loading scans", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void zoomToFitPoints(List<GeoPoint> points) {
        map.post(() -> {
            if (points.isEmpty()) return;

            // Center on the most recent scan (last in list) with a consistent zoom level
            GeoPoint lastPoint = points.get(points.size() - 1);
            
            // Using a consistent zoom level (14.0 is Barangay level)
            map.getController().setZoom(20.0);
            map.getController().animateTo(lastPoint);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}