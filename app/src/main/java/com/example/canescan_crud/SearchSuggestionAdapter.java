package com.example.canescan_crud;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder> {

    private List<Map<String, Object>> suggestions;
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(Map<String, Object> suggestion);
    }

    public SearchSuggestionAdapter(List<Map<String, Object>> suggestions, OnSuggestionClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    public void updateList(List<Map<String, Object>> newList) {
        this.suggestions = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = suggestions.get(position);
        holder.tvName.setText(String.valueOf(item.getOrDefault("name", "Unknown")));
        holder.tvType.setText(String.valueOf(item.getOrDefault("type", "Healthy")));

        Glide.with(holder.itemView.getContext())
                .load((String) item.get("image_url"))
                .placeholder(R.drawable.sugarcane_close)
                .into(holder.ivIcon);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSuggestionClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName, tvType;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_suggestion_icon);
            tvName = itemView.findViewById(R.id.tv_suggestion_name);
            tvType = itemView.findViewById(R.id.tv_suggestion_type);
        }
    }
}