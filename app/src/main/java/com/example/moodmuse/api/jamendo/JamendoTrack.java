package com.example.moodmuse.api.jamendo;

import com.google.gson.annotations.SerializedName;

public class JamendoTrack {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("duration")
    private int duration;

    @SerializedName("artist_name")
    private String artistName;

    @SerializedName("album_name")
    private String albumName;

    @SerializedName("image")
    private String image;

    @SerializedName("audio")
    private String audio;

    @SerializedName("shareurl")
    private String shareUrl;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getImage() {
        return image;
    }

    public String getAudio() {
        return audio;
    }

    public String getShareUrl() {
        return shareUrl;
    }
}

