package com.example.moodmuse.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPlaylist(PlaylistEntity playlist);

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    void renamePlaylist(int playlistId, String newName);

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    void deletePlaylist(int playlistId);

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    LiveData<List<PlaylistEntity>> getAllPlaylists();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addSongToPlaylist(PlaylistSongCrossRef crossRef);

    @Query("DELETE FROM playlist_song_join WHERE playlistId = :playlistId AND songId = :songId")
    void removeSongFromPlaylist(int playlistId, String songId);

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    List<PlaylistEntity> getAllPlaylistsSync();

    @Transaction
    @Query("SELECT * FROM liked_songs " +
           "INNER JOIN playlist_song_join ON liked_songs.id = playlist_song_join.songId " +
           "WHERE playlist_song_join.playlistId = :playlistId")
    LiveData<List<SongEntity>> getSongsForPlaylist(int playlistId);
}
