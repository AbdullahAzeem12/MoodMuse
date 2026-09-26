package com.example.moodmuse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.moodmuse.api.spotify.SpotifySearchResponse;
import com.example.moodmuse.api.spotify.SpotifyTrack;
import com.google.gson.Gson;

import org.junit.Test;

public class SpotifySearchResponseTest {

    @Test
    public void parsesSpotifySearchResponseIntoTrackModel() {
        String json = "{"
                + "\"tracks\":{"
                + "\"items\":[{"
                + "\"id\":\"track_1\","
                + "\"name\":\"Sunrise\","
                + "\"duration_ms\":30000,"
                + "\"preview_url\":\"https://p.scdn.co/mp3-preview/test\","
                + "\"artists\":[{\"name\":\"Artist One\"},{\"name\":\"Artist Two\"}],"
                + "\"album\":{"
                + "\"name\":\"Morning Lights\","
                + "\"images\":[{\"url\":\"https://i.scdn.co/image/test\"}],"
                + "\"external_urls\":{\"spotify\":\"https://open.spotify.com/album/album_1\"}"
                + "},"
                + "\"external_urls\":{\"spotify\":\"https://open.spotify.com/track/track_1\"}"
                + "}]"
                + "}"
                + "}";

        SpotifySearchResponse response = new Gson().fromJson(json, SpotifySearchResponse.class);

        assertNotNull(response);
        SpotifyTrack[] tracks = response.getTracks();
        assertNotNull(tracks);
        assertEquals(1, tracks.length);
        assertEquals("track_1", tracks[0].getId());
        assertEquals("Sunrise", tracks[0].getName());
        assertEquals("Artist One, Artist Two", tracks[0].getArtistNames());
        assertEquals("Morning Lights", tracks[0].getAlbumName());
        assertEquals("https://i.scdn.co/image/test", tracks[0].getAlbumArt());
        assertEquals("https://open.spotify.com/track/track_1", tracks[0].getExternalUrl());
    }
}
