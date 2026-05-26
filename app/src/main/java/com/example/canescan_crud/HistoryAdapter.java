package com.example.canescan_crud;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Map<String, Object>> historyList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
    }

    // Constructor with 1 argument for modular instances
    public HistoryAdapter(List<Map<String, Object>> historyList) {
        this.historyList = historyList;
    }

    // Constructor with 2 arguments for full interactive management
    public HistoryAdapter(List<Map<String, Object>> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    // Dynamic filtering pipeline updates mapping data safely
    public void updateList(List<Map<String, Object>> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (historyList == null || position >= historyList.size()) return;

        Map<String, Object> item = historyList.get(position);

        // Context bindings from Version 2
        if (holder.tvScanName != null) {
            holder.tvScanName.setText(String.valueOf(item.getOrDefault("name", "Unknown Section")));
        }
        if (holder.tvStatusPill != null) {
            holder.tvStatusPill.setText(String.valueOf(item.getOrDefault("status", "Unknown")));
        }

        // Diagnostic bindings from Version 1
        if (holder.tvPathogenName != null) {
            holder.tvPathogenName.setText((String) item.getOrDefault("pathogen_name", "Unknown"));
        }

        if (holder.tvConfidence != null) {
            Object rawScore = item.getOrDefault("confidence_score", 0.0);
            double score = (rawScore instanceof Double) ? (Double) rawScore : 0.0;
            holder.tvConfidence.setText(String.format(Locale.getDefault(), "Confidence: %.0f%%", score * 100));
        }

        if (holder.tvTimestamp != null) {
            Timestamp timestamp = (Timestamp) item.get("timestamp");
            if (timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
                holder.tvTimestamp.setText(sdf.format(timestamp.toDate()));
            } else {
                holder.tvTimestamp.setText("");
            }
        }

        if (holder.ivScanImage != null) {
            Glide.with(holder.itemView.getContext())
                    .load((String) item.get("image_url"))
                    .placeholder(R.drawable.sugarcane_close)
                    .error(R.drawable.sugarcane_close)
                    .into(holder.ivScanImage);
        }

        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return historyList == null ? 0 : historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivScanImage, btnDelete;
        TextView tvPathogenName, tvTimestamp, tvConfidence, tvScanName, tvStatusPill;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Version 1 View Elements
            ivScanImage = itemView.findViewById(R.id.iv_scan_image);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            tvPathogenName = itemView.findViewById(R.id.tv_pathogen_name);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);

            // Version 2 View Elements
            tvScanName = itemView.findViewById(R.id.tv_scan_name);
            tvStatusPill = itemView.findViewById(R.id.tv_status_pill);
        }
    }
}