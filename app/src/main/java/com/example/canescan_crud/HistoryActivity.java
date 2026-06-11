package com.example.canescan_crud;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import de.hdodenhof.circleimageview.CircleImageView;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<Map<String, Object>> fullHistoryList;
    private List<Map<String, Object>> currentFilteredList;
    private EditText etSearch;
    private TextView btnAll, btnInfected, btnHealthy;
    private CircleImageView ivProfile;
    private TextView tvEmpty;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load layout styling configuration before runtime execution layers
        SharedPreferences sharedPreferences = getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // UI View Binding Definition
        rvHistory = findViewById(R.id.rv_history);
        etSearch = findViewById(R.id.et_search);
        btnAll = findViewById(R.id.btn_filter_all);
        btnInfected = findViewById(R.id.btn_filter_infected);
        btnHealthy = findViewById(R.id.btn_filter_healthy);
        ivProfile = findViewById(R.id.iv_profile_history);

        // Handle variations of empty placeholder layout labels across XML layouts safely
        tvEmpty = findViewById(R.id.tv_empty_history);
        if (tvEmpty == null) {
            tvEmpty = findViewById(R.id.tv_empty);
        }

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        fullHistoryList = new ArrayList<>();
        currentFilteredList = new ArrayList<>();

        // Instantiate using the interactive contextual reference callback pointing directly to localized deletion logic
        adapter = new HistoryAdapter(currentFilteredList, new HistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                showScanDetailDialog(position);
            }

            @Override
            public void onDeleteClick(int position) {
                deleteHistoryItem(position);
            }
        });
        rvHistory.setAdapter(adapter);

        View backButton = findViewById(R.id.iv_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        setupListeners();
        updateProfileImage();
        setupNavigation();
        updateFilterUI();
        loadHistory();

        AccessibilityHelper.init(this);
        AccessibilityHelper.speak("You are in Scan History");
    }

    private void setupListeners() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilters();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        if (btnAll != null) {
            btnAll.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                currentFilter = "All";
                updateFilterUI();
                applyFilters();
            });
        }

        if (btnInfected != null) {
            btnInfected.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                currentFilter = "Infected";
                updateFilterUI();
                applyFilters();
            });
        }

        if (btnHealthy != null) {
            btnHealthy.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                currentFilter = "Healthy";
                updateFilterUI();
                applyFilters();
            });
        }

        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                startActivity(new Intent(this, ProfileActivity.class));
            });
        }
    }

    private void updateFilterUI() {
        if (btnAll != null) btnAll.setAlpha(currentFilter.equals("All") ? 1.0f : 0.6f);
        if (btnInfected != null) btnInfected.setAlpha(currentFilter.equals("Infected") ? 1.0f : 0.6f);
        if (btnHealthy != null) btnHealthy.setAlpha(currentFilter.equals("Healthy") ? 1.0f : 0.6f);
    }

    private void applyFilters() {
        String query = etSearch != null ? etSearch.getText().toString().toLowerCase() : "";

        currentFilteredList = fullHistoryList.stream()
                .filter(item -> {
                    // Name match validation checks
                    String name = String.valueOf(item.getOrDefault("name", ""));
                    boolean matchesQuery = name.toLowerCase().contains(query);

                    // Type or Status evaluation categorization
                    String type = String.valueOf(item.getOrDefault("type", item.getOrDefault("status", "All")));
                    boolean matchesFilter;
                    if (currentFilter.equals("All")) {
                        matchesFilter = true;
                    } else if (currentFilter.equals("Infected")) {
                        // Any class that is NOT healthy/unknown/error is considered infected for filtering
                        matchesFilter = !type.equalsIgnoreCase("Healthy") && !type.equalsIgnoreCase("Unknown") && !type.equalsIgnoreCase("Error");
                    } else {
                        // Direct match for "Healthy"
                        matchesFilter = type.equalsIgnoreCase(currentFilter);
                    }

                    return matchesQuery && matchesFilter;
                })
                .collect(Collectors.toList());

        adapter.updateList(currentFilteredList);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (tvEmpty == null || rvHistory == null) return;
        if (adapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }
    }

    private void loadHistory() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        db.collection("scan_logs")
                .whereEqualTo("user_id", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullHistoryList.clear();
                    List<Task<Void>> tasks = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> data = document.getData();
                        data.put("id", document.getId());

                        // Async dynamic background pipeline extraction fetching deeper multi-tier metadata properties
                        Task<Void> task = db.collection("diagnostic_results")
                                .whereEqualTo("scan_id", document.getId())
                                .limit(1)
                                .get()
                                .continueWithTask(diagTask -> {
                                    if (diagTask.isSuccessful() && !diagTask.getResult().isEmpty()) {
                                        DocumentSnapshot diagDoc = diagTask.getResult().getDocuments().get(0);
                                        data.put("confidence_score", diagDoc.getDouble("confidence_score"));
                                        
                                        // Use pathogen_name directly if it exists, otherwise fall back to old ID logic
                                        if (diagDoc.contains("pathogen_name")) {
                                            data.put("pathogen_name", diagDoc.getString("pathogen_name"));
                                            return Tasks.forResult(null);
                                        } else {
                                            String pathogenId = diagDoc.getString("pathogen_id");
                                            if (pathogenId != null) {
                                                return db.collection("pathogens").document(pathogenId).get()
                                                        .addOnSuccessListener(pathogenDoc -> {
                                                            data.put("pathogen_name", pathogenDoc.getString("common_name"));
                                                        }).onSuccessTask(t -> Tasks.forResult(null));
                                            }
                                        }
                                    }
                                    return Tasks.forResult(null);
                                });
                        tasks.add(task);
                        fullHistoryList.add(data);
                    }

                    Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
                        applyFilters(); // Safely triggers programmatic lists transformation UI adjustments
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteHistoryItem(int position) {
        // Map UI position context targeting back into actual references layout
        if (currentFilteredList.isEmpty() || position >= currentFilteredList.size()) return;

        Map<String, Object> item = currentFilteredList.get(position);
        String scanId = (String) item.get("id");

        db.collection("scan_logs").document(scanId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Cascade cascading deletions targeting downstream diagnostics documents collections safely
                    db.collection("diagnostic_results").whereEqualTo("scan_id", scanId).get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                    doc.getReference().delete();
                                }
                            });

                    fullHistoryList.remove(item);
                    applyFilters(); // Live update filtering collection matrices calculations layout UI frames
                    Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showScanDetailDialog(int position) {
        if (currentFilteredList.isEmpty() || position >= currentFilteredList.size()) return;
        Map<String, Object> item = currentFilteredList.get(position);

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_scan_detail);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        ImageView ivImage = dialog.findViewById(R.id.iv_detail_image_full);
        TextView tvName = dialog.findViewById(R.id.tv_detail_name);
        TextView tvType = dialog.findViewById(R.id.tv_detail_type);
        TextView tvDate = dialog.findViewById(R.id.tv_detail_timestamp);
        TextView tvDesc = dialog.findViewById(R.id.tv_detail_description);
        Button btnClose = dialog.findViewById(R.id.btn_close_detail);

        tvName.setText(String.valueOf(item.getOrDefault("name", "Unknown Section")));
        tvType.setText("Condition: " + item.getOrDefault("type", item.getOrDefault("status", "Unknown")));

        Timestamp ts = (Timestamp) item.get("timestamp");
        if (ts != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            tvDate.setText("Date: " + sdf.format(ts.toDate()));
        }

        tvDesc.setText(String.valueOf(item.getOrDefault("description", "No description provided.")));

        String imageUrl = (String) item.get("image_url");
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.sugarcane_close)
                .error(R.drawable.sugarcane_close)
                .into(ivImage);

        ivImage.setOnClickListener(v -> showFullImageDialog(imageUrl));

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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

    private void updateProfileImage() {
        if (ivProfile == null) return;
        SharedPreferences sharedPreferences = getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        String savedImageUri = sharedPreferences.getString("profile_image_uri", null);
        if (savedImageUri != null) {
            Glide.with(this).load(Uri.parse(savedImageUri)).placeholder(R.drawable.user).into(ivProfile);
        }
    }

    private void setupNavigation() {
        View navHome = findViewById(R.id.nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            });
        }
        View navMap = findViewById(R.id.nav_map);
        if (navMap != null) {
            navMap.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                startActivity(new Intent(this, MapActivity.class));
                finish();
            });
        }
        View navSettings = findViewById(R.id.nav_settings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
            });
        }

        View navHistory = findViewById(R.id.nav_history);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                AccessibilityHelper.handleViewClick(this, v);
                // Already on history
            });
        }
    }
}