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

    public HistoryAdapter(List<Map<String, Object>> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = historyList.get(position);
        
        holder.tvPathogenName.setText((String) item.getOrDefault("pathogen_name", "Unknown"));
        holder.tvConfidence.setText(String.format(Locale.getDefault(), "Confidence: %.0f%%", (double) item.getOrDefault("confidence_score", 0.0) * 100));

        Timestamp timestamp = (Timestamp) item.get("timestamp");
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            holder.tvTimestamp.setText(sdf.format(timestamp.toDate()));
        }

        Glide.with(holder.itemView.getContext())
                .load((String) item.get("image_url"))
                .placeholder(R.drawable.sugarcane_close)
                .into(holder.ivScanImage);

        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(position));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivScanImage, btnDelete;
        TextView tvPathogenName, tvTimestamp, tvConfidence;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivScanImage = itemView.findViewById(R.id.iv_scan_image);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            tvPathogenName = itemView.findViewById(R.id.tv_pathogen_name);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
        }
    }
}