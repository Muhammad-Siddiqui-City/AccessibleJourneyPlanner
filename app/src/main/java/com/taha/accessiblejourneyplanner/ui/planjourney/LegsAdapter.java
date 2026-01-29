package com.taha.accessiblejourneyplanner.ui.planjourney;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taha.accessiblejourneyplanner.R;

import java.util.ArrayList;
import java.util.List;

/** Simple adapter for journey legs in JourneyDetailActivity. */
public class LegsAdapter extends RecyclerView.Adapter<LegsAdapter.ViewHolder> {

    private final List<LegDisplayItem> items = new ArrayList<>();

    public void setItems(List<LegDisplayItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leg, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LegDisplayItem item = items.get(position);
        holder.mode.setText(item.modeName.isEmpty() ? "—" : item.modeName);
        holder.duration.setText(item.durationMinutes + " min");
        String fromTo = (item.fromName.isEmpty() ? "—" : item.fromName) + " → " + (item.toName.isEmpty() ? "—" : item.toName);
        holder.fromTo.setText(fromTo);
        holder.instruction.setText(item.instructionSummary.isEmpty() ? "" : item.instructionSummary);
        holder.instruction.setVisibility(item.instructionSummary.isEmpty() ? View.GONE : View.VISIBLE);
        holder.itemView.setContentDescription(item.modeName + ", " + item.durationMinutes + " minutes, " + fromTo);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mode, duration, instruction, fromTo;

        ViewHolder(View itemView) {
            super(itemView);
            mode = itemView.findViewById(R.id.item_leg_mode);
            duration = itemView.findViewById(R.id.item_leg_duration);
            instruction = itemView.findViewById(R.id.item_leg_instruction);
            fromTo = itemView.findViewById(R.id.item_leg_from_to);
        }
    }
}
