package com.example.canescan_crud;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Map<String, Object>> historyList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
    }

    // Constructor with 1 argument
    public HistoryAdapter(List<Map<String, Object>> historyList) {
        this.historyList = historyList;
    }

    // Constructor with 2 arguments to fix compilation issues
    public HistoryAdapter(List<Map<String, Object>> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

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
        Map<String, Object> item = historyList.get(position);
        
        holder.tvScanName.setText(String.valueOf(item.getOrDefault("name", "Unknown Section")));
        holder.tvStatusPill.setText(String.valueOf(item.getOrDefault("status", "Unknown")));
    }

    @Override
    public int getItemCount() {
        return historyList == null ? 0 : historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvScanName, tvStatusPill;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvScanName = itemView.findViewById(R.id.tv_scan_name);
            tvStatusPill = itemView.findViewById(R.id.tv_status_pill);
        }
    }
}