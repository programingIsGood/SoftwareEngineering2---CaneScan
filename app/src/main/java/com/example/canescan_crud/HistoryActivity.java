package com.example.canescan_crud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<Map<String, Object>> fullHistoryList;
    private EditText etSearch;
    private TextView btnAll, btnInfected, btnHealthy;
    private CircleImageView ivProfile;
    private String currentFilter = "All";
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvHistory = findViewById(R.id.rv_history);
        etSearch = findViewById(R.id.et_search);
        btnAll = findViewById(R.id.btn_filter_all);
        btnInfected = findViewById(R.id.btn_filter_infected);
        btnHealthy = findViewById(R.id.btn_filter_healthy);
        ivProfile = findViewById(R.id.iv_profile_history);
        tvEmpty = findViewById(R.id.tv_empty_history);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // Start with an empty list as requested
        fullHistoryList = new ArrayList<>();

        // Use 2-argument constructor with null listener to fix compilation error
        adapter = new HistoryAdapter(new ArrayList<>(fullHistoryList), null);
        rvHistory.setAdapter(adapter);

        updateEmptyState();
        setupListeners();
        updateProfileImage();
        setupNavigation();
        updateFilterUI();
    }

    private void setupListeners() {
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

        btnAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateFilterUI();
            applyFilters();
        });

        btnInfected.setOnClickListener(v -> {
            currentFilter = "Infected";
            updateFilterUI();
            applyFilters();
        });

        btnHealthy.setOnClickListener(v -> {
            currentFilter = "Healthy";
            updateFilterUI();
            applyFilters();
        });

        ivProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    private void updateFilterUI() {
        // Simple UI feedback for filters using alpha
        btnAll.setAlpha(currentFilter.equals("All") ? 1.0f : 0.6f);
        btnInfected.setAlpha(currentFilter.equals("Infected") ? 1.0f : 0.6f);
        btnHealthy.setAlpha(currentFilter.equals("Healthy") ? 1.0f : 0.6f);
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase();

        List<Map<String, Object>> result = fullHistoryList.stream()
            .filter(item -> {
                String name = String.valueOf(item.getOrDefault("name", ""));
                boolean matchesQuery = name.toLowerCase().contains(query);

                String type = String.valueOf(item.getOrDefault("type", "All"));
                boolean matchesFilter = currentFilter.equals("All") || type.equals(currentFilter);

                return matchesQuery && matchesFilter;
            })
            .collect(Collectors.toList());

        adapter.updateList(result);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }
    }

    private void updateProfileImage() {
        SharedPreferences sharedPreferences = getSharedPreferences("CaneScanPrefs", Context.MODE_PRIVATE);
        String savedImageUri = sharedPreferences.getString("profile_image_uri", null);
        if (savedImageUri != null) {
            Glide.with(this).load(Uri.parse(savedImageUri)).placeholder(R.drawable.user).into(ivProfile);
        }
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
        findViewById(R.id.nav_map).setOnClickListener(v -> {
            startActivity(new Intent(this, MapActivity.class));
            finish();
        });
        findViewById(R.id.nav_history).setOnClickListener(v -> {
            // Already here
        });
        findViewById(R.id.nav_settings).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }
}
