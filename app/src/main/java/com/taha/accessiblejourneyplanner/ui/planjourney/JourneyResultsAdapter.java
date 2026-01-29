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

/**
 * Adapter for journey results list. Tap opens JourneyDetailActivity.
 */
public class JourneyResultsAdapter extends RecyclerView.Adapter<JourneyResultsAdapter.ViewHolder> {

    private final List<JourneyResultItem> items = new ArrayList<>();
    private OnJourneyClickListener listener;

    public interface OnJourneyClickListener {
        void onJourneyClick(JourneyResultItem item, int position);
    }

    public void setOnJourneyClickListener(OnJourneyClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<JourneyResultItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journey_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JourneyResultItem item = items.get(position);
        holder.duration.setText(item.durationMinutes + " min");
        holder.modes.setText(item.modesSummary);
        holder.stepFree.setText(item.stepFreeBadge);
        holder.lift.setText(item.liftBadge);
        holder.busChip.setVisibility(item.busIncluded ? View.VISIBLE : View.GONE);
        holder.rankReason.setText(item.rankReason);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onJourneyClick(item, position);
        });
        holder.itemView.setContentDescription("Route " + (position + 1) + ", " + item.durationMinutes + " minutes, " + item.modesSummary);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView duration, modes, stepFree, lift, busChip, rankReason;

        ViewHolder(View itemView) {
            super(itemView);
            duration = itemView.findViewById(R.id.item_journey_duration);
            modes = itemView.findViewById(R.id.item_journey_modes);
            stepFree = itemView.findViewById(R.id.item_journey_step_free);
            lift = itemView.findViewById(R.id.item_journey_lift);
            busChip = itemView.findViewById(R.id.item_journey_bus);
            rankReason = itemView.findViewById(R.id.item_journey_rank_reason);
        }
    }
}
