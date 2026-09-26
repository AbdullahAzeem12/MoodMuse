package com.example.moodmuse.api.itunes;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ItunesApiService {
    @GET("search")
    Call<ItunesSearchResponse> search(
            @Query("term") String term,
            @Query("entity") String entity,
            @Query("limit") int limit
    );
}

