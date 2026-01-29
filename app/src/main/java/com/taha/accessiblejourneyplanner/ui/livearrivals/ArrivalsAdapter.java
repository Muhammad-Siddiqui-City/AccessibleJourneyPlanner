package com.taha.accessiblejourneyplanner.ui.livearrivals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taha.accessiblejourneyplanner.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple adapter for the arrivals list. No DiffUtil; set a new list when data changes.
 */
public class ArrivalsAdapter extends RecyclerView.Adapter<ArrivalsAdapter.ViewHolder> {

    private final List<ArrivalItem> items = new ArrayList<>();

    public void setItems(List<ArrivalItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_arrival, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArrivalItem item = items.get(position);
        holder.line.setText(item.lineName);
        holder.towards.setText(item.towards.isEmpty() ? "—" : item.towards);
        holder.mins.setText(item.minutesToArrival + " min");
        holder.platform.setText(item.platformName.isEmpty() ? "" : "Platform " + item.platformName);
        holder.crowding.setText(item.crowdingLabel);
        holder.itemView.setContentDescription(item.lineName + " towards " + item.towards + ", " + item.minutesToArrival + " minutes, " + item.crowdingLabel);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView line, towards, mins, platform, crowding;

        ViewHolder(View itemView) {
            super(itemView);
            line = itemView.findViewById(R.id.item_arrival_line);
            towards = itemView.findViewById(R.id.item_arrival_towards);
            mins = itemView.findViewById(R.id.item_arrival_mins);
            platform = itemView.findViewById(R.id.item_arrival_platform);
            crowding = itemView.findViewById(R.id.item_arrival_crowding);
        }
    }
}
