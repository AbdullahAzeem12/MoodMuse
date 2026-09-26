package com.example.moodmuse.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playback_history")
public class HistoryEntry {
    @PrimaryKey
    @NonNull
    public String id; // Use song ID as primary key or UUID
    public String title;
    public String artist;
    public String imageUrl;
    public String previewUrl;
    public String externalUrl;
    public String platform; // "jamendo" or "youtube"
    public long timestamp;
    public String mood;

    public HistoryEntry() {}

    @androidx.room.Ignore
    public HistoryEntry(@NonNull String id, String title, String artist, String imageUrl, String previewUrl, String externalUrl, String platform, long timestamp, String mood) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.previewUrl = previewUrl;
        this.externalUrl = externalUrl;
        this.platform = platform;
        this.timestamp = timestamp;
        this.mood = mood;
    }
}
