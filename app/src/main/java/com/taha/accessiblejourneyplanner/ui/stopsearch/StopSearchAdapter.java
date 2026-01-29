package com.taha.accessiblejourneyplanner.ui.stopsearch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taha.accessiblejourneyplanner.R;

import java.util.ArrayList;
import java.util.List;

public class StopSearchAdapter extends RecyclerView.Adapter<StopSearchAdapter.ViewHolder> {

    private final List<StopSearchItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String id, String name, String modesStr);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<StopSearchItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stop_search, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StopSearchItem item = items.get(position);
        holder.name.setText(item.name);
        holder.id.setText("ID: " + item.id);
        holder.modes.setText(item.modesStr.isEmpty() ? "" : item.modesStr);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item.id, item.name, item.modesStr);
        });
        holder.itemView.setContentDescription(item.name + ", " + item.id + ", " + item.modesStr);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, id, modes;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_stop_name);
            id = itemView.findViewById(R.id.item_stop_id);
            modes = itemView.findViewById(R.id.item_stop_modes);
        }
    }
}
