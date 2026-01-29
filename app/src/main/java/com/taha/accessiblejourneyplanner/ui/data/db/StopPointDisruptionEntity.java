package com.taha.accessiblejourneyplanner.ui.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Cached StopPoint disruption (lift issues) for TTL. 10 min TTL per spec.
 */
@Entity(tableName = "stop_point_disruption")
public class StopPointDisruptionEntity {

    @PrimaryKey
    @NonNull
    public String stopPointId;

    public boolean hasLiftDisruption;

    public long fetchedAt;

    public StopPointDisruptionEntity() {}

    public StopPointDisruptionEntity(@NonNull String stopPointId, boolean hasLiftDisruption, long fetchedAt) {
        this.stopPointId = stopPointId;
        this.hasLiftDisruption = hasLiftDisruption;
        this.fetchedAt = fetchedAt;
    }
}
