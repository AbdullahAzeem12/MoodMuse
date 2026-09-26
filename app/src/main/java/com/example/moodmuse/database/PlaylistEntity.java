package com.example.moodmuse.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class PlaylistEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @androidx.room.Ignore
    public String remoteId;
    public String name;
    public String description;
    public String coverImageUrl;
    public long createdAt;
    public String type; // "playlist" or "album"

    public PlaylistEntity() {
        this.createdAt = System.currentTimeMillis();
    }

    @androidx.room.Ignore
    public PlaylistEntity(String name, String description, String coverImageUrl, String type) {
        this.name = name;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.type = type != null ? type : "playlist";
        this.createdAt = System.currentTimeMillis();
    }
}
