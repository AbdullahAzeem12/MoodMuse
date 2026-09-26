package com.example.moodmuse.api.itunes;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ItunesSearchResponse {
    @SerializedName("resultCount")
    public int resultCount;

    @SerializedName("results")
    public List<Result> results;

    public static class Result {
        @SerializedName("trackId")
        public Long trackId;

        @SerializedName("trackName")
        public String trackName;

        @SerializedName("artistName")
        public String artistName;

        @SerializedName("collectionName")
        public String collectionName;

        @SerializedName("artworkUrl100")
        public String artworkUrl100;

        @SerializedName("previewUrl")
        public String previewUrl;

        @SerializedName("trackViewUrl")
        public String trackViewUrl;

        @SerializedName("trackTimeMillis")
        public Long trackTimeMillis;
    }
}

