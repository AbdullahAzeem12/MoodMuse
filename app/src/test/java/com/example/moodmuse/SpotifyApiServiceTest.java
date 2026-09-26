package com.example.moodmuse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.moodmuse.api.spotify.SpotifyApiService;
import com.example.moodmuse.api.spotify.SpotifySearchResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SpotifyApiServiceTest {

    private MockWebServer mockWebServer;
    private SpotifyApiService spotifyApiService;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        spotifyApiService = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SpotifyApiService.class);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void searchTracksSendsExpectedRequestAndParsesResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"tracks\":{\"items\":[{\"id\":\"1\",\"name\":\"Glow\",\"duration_ms\":30000,"
                        + "\"artists\":[{\"name\":\"Muse\"}],"
                        + "\"album\":{\"name\":\"Album\",\"images\":[{\"url\":\"img\"}],"
                        + "\"external_urls\":{\"spotify\":\"albumUrl\"}},"
                        + "\"external_urls\":{\"spotify\":\"trackUrl\"}}]}}"));

        Response<SpotifySearchResponse> response = spotifyApiService.searchTracks(
                "Bearer token_123",
                "happy pop dance upbeat",
                "track",
                20,
                0,
                "US",
                "audio"
        ).execute();

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("Bearer token_123", request.getHeader("Authorization"));
        assertTrue(request.getPath().startsWith("/search?"));
        assertTrue(request.getPath().contains("q=happy%20pop%20dance%20upbeat"));
        assertTrue(request.getPath().contains("type=track"));
        assertTrue(request.getPath().contains("limit=20"));
        assertTrue(request.getPath().contains("offset=0"));
        assertTrue(request.getPath().contains("market=US"));
        assertTrue(request.getPath().contains("include_external=audio"));

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertNotNull(response.body().getTracks());
        assertEquals(1, response.body().getTracks().length);
        assertEquals("Glow", response.body().getTracks()[0].getName());
    }
}
