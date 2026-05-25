package com.example.canescan_crud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<Map<String, Object>> historyList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
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

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvHistory = findViewById(R.id.rv_history);
        tvEmpty = findViewById(R.id.tv_empty);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList, this::deleteHistoryItem);
        rvHistory.setAdapter(adapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        setupNavigation();
        loadHistory();
    }

    private void loadHistory() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        db.collection("scan_logs")
                .whereEqualTo("user_id", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();
                    List<Task<Void>> tasks = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> data = document.getData();
                        data.put("id", document.getId());
                        
                        // We need to fetch diagnostic results for each scan to get pathogen name and confidence
                        Task<Void> task = db.collection("diagnostic_results")
                                .whereEqualTo("scan_id", document.getId())
                                .limit(1)
                                .get()
                                .continueWithTask(diagTask -> {
                                    if (diagTask.isSuccessful() && !diagTask.getResult().isEmpty()) {
                                        DocumentSnapshot diagDoc = diagTask.getResult().getDocuments().get(0);
                                        data.put("confidence_score", diagDoc.getDouble("confidence_score"));
                                        String pathogenId = diagDoc.getString("pathogen_id");
                                        
                                        if (pathogenId != null) {
                                            return db.collection("pathogens").document(pathogenId).get()
                                                    .addOnSuccessListener(pathogenDoc -> {
                                                        data.put("pathogen_name", pathogenDoc.getString("common_name"));
                                                    }).onSuccessTask(t -> Tasks.forResult(null));
                                        }
                                    }
                                    return Tasks.forResult(null);
                                });
                        tasks.add(task);
                        historyList.add(data);
                    }

                    Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteHistoryItem(int position) {
        Map<String, Object> item = historyList.get(position);
        String scanId = (String) item.get("id");

        db.collection("scan_logs").document(scanId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Also delete associated diagnostic results
                    db.collection("diagnostic_results").whereEqualTo("scan_id", scanId).get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                    doc.getReference().delete();
                                }
                            });
                    
                    historyList.remove(position);
                    adapter.notifyItemRemoved(position);
                    tvEmpty.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                    Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                });
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
        findViewById(R.id.nav_settings).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }
}