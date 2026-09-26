package com.example.moodmuse.data;

public class Song {
    private String title;
    private String artist;
    private int coverArt;

    public Song(String title, String artist, int coverArt) {
        this.title = title;
        this.artist = artist;
        this.coverArt = coverArt;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getCoverArt() {
        return coverArt;
    }
}
