package com.example.moodmuse.api.jamendo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface JamendoApiService {
    @GET("tracks/")
    Call<JamendoTrackResponse> getTracks(
            @Query("client_id") String clientId,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("fuzzytags") String fuzzyTags,
            @Query("include") String include,
            @Query("audioformat") String audioFormat,
            @Query("imagesize") int imageSize,
            @Query("order") String order
    );

    @GET("tracks/")
    Call<JamendoTrackResponse> searchTracks(
            @Query("client_id") String clientId,
            @Query("search") String query,
            @Query("limit") int limit,
            @Query("format") String format,
            @Query("include") String include,
            @Query("audioformat") String audioFormat,
            @Query("imagesize") int imageSize
    );
}

