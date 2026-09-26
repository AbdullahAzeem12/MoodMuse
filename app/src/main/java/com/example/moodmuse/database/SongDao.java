package com.example.moodmuse.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SongEntity song);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SongEntity> songs);

    @Query("DELETE FROM liked_songs WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM liked_songs")
    void clearAll();

    @Query("SELECT * FROM liked_songs ORDER BY title ASC")
    LiveData<List<SongEntity>> getAllLikedSongs();

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE id = :id)")
    LiveData<Boolean> isLiked(String id);

    @Query("SELECT * FROM liked_songs WHERE title LIKE :query OR artist LIKE :query")
    List<SongEntity> searchLikedSongs(String query);
    
    @Query("SELECT * FROM liked_songs ORDER BY title ASC")
    List<SongEntity> getAllLikedSongsSync();
}
