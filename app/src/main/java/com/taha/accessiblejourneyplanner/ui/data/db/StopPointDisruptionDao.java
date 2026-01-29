package com.taha.accessiblejourneyplanner.ui.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface StopPointDisruptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StopPointDisruptionEntity entity);

    @Query("SELECT * FROM stop_point_disruption WHERE stopPointId = :stopPointId")
    StopPointDisruptionEntity getByStopPointId(String stopPointId);
}
