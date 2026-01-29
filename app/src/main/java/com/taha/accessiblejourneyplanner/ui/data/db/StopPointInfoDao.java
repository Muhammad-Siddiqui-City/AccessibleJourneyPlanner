package com.taha.accessiblejourneyplanner.ui.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface StopPointInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StopPointInfoEntity entity);

    @Query("SELECT * FROM stop_point_info WHERE stopPointId = :stopPointId")
    StopPointInfoEntity getByStopPointId(String stopPointId);
}
