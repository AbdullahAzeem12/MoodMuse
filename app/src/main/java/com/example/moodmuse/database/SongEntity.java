package com.example.moodmuse.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.moodmuse.models.Song;

@Entity(tableName = "liked_songs")
public class SongEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String title;
    public String artist;
    public String album;
    public String imageUrl;
    public String previewUrl;
    public String externalUrl;
    public String platform;
    public String duration;

    public SongEntity() {}

    @androidx.room.Ignore
    public SongEntity(@NonNull String id, String title, String artist, String album, String imageUrl, String previewUrl, String externalUrl, String platform, String duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.imageUrl = imageUrl;
        this.previewUrl = previewUrl;
        this.externalUrl = externalUrl;
        this.platform = platform;
        this.duration = duration;
    }

    public static SongEntity fromSong(Song song) {
        return new SongEntity(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getAlbum(),
                song.getImageUrl(),
                song.getPreviewUrl(),
                song.getExternalUrl(),
                song.getPlatform(),
                song.getDuration()
        );
    }

    public Song toSong() {
        Song song = new Song();
        song.setId(id);
        song.setTitle(title);
        song.setArtist(artist);
        song.setAlbum(album);
        song.setImageUrl(imageUrl);
        song.setPreviewUrl(previewUrl);
        song.setExternalUrl(externalUrl);
        song.setPlatform(platform);
        song.setDuration(duration);
        return song;
    }
}
