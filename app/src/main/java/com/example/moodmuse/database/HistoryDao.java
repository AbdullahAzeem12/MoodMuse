package com.example.moodmuse.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntry entry);

    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC")
    List<HistoryEntry> getAllHistory();

    @Query("DELETE FROM playback_history")
    void clearAll();

    @Query("DELETE FROM playback_history WHERE id = :id")
    void deleteById(String id);
}
