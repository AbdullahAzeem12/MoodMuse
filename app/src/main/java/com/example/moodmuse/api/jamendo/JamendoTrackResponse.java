package com.example.moodmuse.api.jamendo;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class JamendoTrackResponse {
    @SerializedName("headers")
    private JamendoHeaders headers;

    @SerializedName("results")
    private List<JamendoTrack> results;

    public JamendoHeaders getHeaders() {
        return headers;
    }

    public List<JamendoTrack> getResults() {
        return results;
    }
}

