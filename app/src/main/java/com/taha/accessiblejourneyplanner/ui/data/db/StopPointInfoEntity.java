package com.taha.accessiblejourneyplanner.ui.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Cached StopPoint accessibility (step-free) for TTL. 24h TTL per spec.
 */
@Entity(tableName = "stop_point_info")
public class StopPointInfoEntity {

    @PrimaryKey
    @NonNull
    public String stopPointId;

    /** Parsed step-free: "Yes", "No", or "Unknown". */
    public String stepFreeText;

    public long fetchedAt;

    public StopPointInfoEntity() {}

    public StopPointInfoEntity(@NonNull String stopPointId, String stepFreeText, long fetchedAt) {
        this.stopPointId = stopPointId;
        this.stepFreeText = stepFreeText != null ? stepFreeText : "Unknown";
        this.fetchedAt = fetchedAt;
    }
}
