package com.taha.accessiblejourneyplanner.ui.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ArrivalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedArrivalEntity> entities);

    @Query("DELETE FROM cached_arrivals WHERE stopPointId = :stopPointId")
    void deleteByStopPoint(String stopPointId);

    @Query("SELECT * FROM cached_arrivals WHERE stopPointId = :stopPointId ORDER BY timeToStation ASC")
    List<CachedArrivalEntity> getByStopPoint(String stopPointId);
}
