package com.taha.accessiblejourneyplanner.ui.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_arrivals")
public class CachedArrivalEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String stopPointId;
    public String lineName;
    public String towards;
    public String platformName;
    public int timeToStation;
    public long fetchedAt;

    public CachedArrivalEntity() {}

    /** Convenience constructor for building from API response (id = 0 for insert). */
    public CachedArrivalEntity(String stopPointId, String lineName, String towards,
                               String platformName, int timeToStation, long fetchedAt) {
        this.id = 0;
        this.stopPointId = stopPointId;
        this.lineName = lineName;
        this.towards = towards;
        this.platformName = platformName;
        this.timeToStation = timeToStation;
        this.fetchedAt = fetchedAt;
    }
}
