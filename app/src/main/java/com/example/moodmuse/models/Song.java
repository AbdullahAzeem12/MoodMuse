package com.example.moodmuse.models;

public class Song {
    private String id;
    private String title;
    private String artist;
    private String album;
    private String albumUrl;
    private String imageUrl;
    private String previewUrl;
    private String externalUrl;
    private String platform; // "spotify" or "youtube"
    private String duration;
    private String moodTag;
    private String debugSource;

    public Song() {
    }

    public Song(String id, String title, String artist, String album, String imageUrl, String previewUrl, String externalUrl, String platform, String duration) {
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getAlbumUrl() {
        return albumUrl;
    }

    public void setAlbumUrl(String albumUrl) {
        this.albumUrl = albumUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getMoodTag() {
        return moodTag;
    }

    public void setMoodTag(String moodTag) {
        this.moodTag = moodTag;
    }

    public String getDebugSource() {
        return debugSource;
    }

    public void setDebugSource(String debugSource) {
        this.debugSource = debugSource;
    }

    public boolean hasPreview() {
        return previewUrl != null && !previewUrl.trim().isEmpty();
    }

    public boolean hasExternalUrl() {
        return externalUrl != null && !externalUrl.trim().isEmpty();
    }
}
