package com.example.moodmuse.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "playlist_song_join",
        primaryKeys = {"playlistId", "songId"})
public class PlaylistSongCrossRef {
    public int playlistId;
    @NonNull
    public String songId;

    public PlaylistSongCrossRef() {}

    @androidx.room.Ignore
    public PlaylistSongCrossRef(int playlistId, @NonNull String songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }
}
