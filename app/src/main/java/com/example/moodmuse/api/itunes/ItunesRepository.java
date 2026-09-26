package com.example.moodmuse.api.itunes;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.moodmuse.models.Song;
import com.example.moodmuse.util.DurationUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ItunesRepository {
    public interface ItunesSearchCallback {
        void onSuccess(@NonNull List<Song> songs);

        void onError(@NonNull Throwable error);
    }

    private static final String TAG = "ItunesRepository";
    private static final String BASE_URL = "https://itunes.apple.com/";

    private final ItunesApiService apiService;

    public ItunesRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ItunesApiService.class);
    }

    public void searchByEmotion(@Nullable String emotion, @NonNull ItunesSearchCallback callback) {
        String e = (emotion == null || emotion.trim().isEmpty()) ? "Popular" : emotion.trim();
        // Request more to ensure we have at least 15 after filtering
        searchByTerm(e, 50, callback);
    }

    public void searchByTerm(@NonNull String term, @NonNull ItunesSearchCallback callback) {
        searchByTerm(term, 30, callback);
    }

    public void searchByTerm(@NonNull String term, int limit, @NonNull ItunesSearchCallback callback) {
        if (TextUtils.isEmpty(term)) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        apiService.search(term, "musicVideo", limit).enqueue(new Callback<ItunesSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<ItunesSearchResponse> call,
                                   @NonNull Response<ItunesSearchResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    String msg = "iTunes search failed: HTTP " + response.code();
                    Log.e(TAG, msg);
                    callback.onError(new IllegalStateException(msg));
                    return;
                }
                callback.onSuccess(mapResultsToSongs(response.body().results));
            }

            @Override
            public void onFailure(@NonNull Call<ItunesSearchResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "iTunes network error", t);
                callback.onError(t);
            }
        });
    }

    @NonNull
    private static List<Song> mapResultsToSongs(@Nullable List<ItunesSearchResponse.Result> results) {
        List<Song> out = new ArrayList<>();
        if (results == null) return out;
        for (ItunesSearchResponse.Result r : results) {
            Song s = toSong(r);
            if (s != null) out.add(s);
        }
        return out;
    }

    @Nullable
    private static Song toSong(@Nullable ItunesSearchResponse.Result r) {
        if (r == null) return null;
        if (TextUtils.isEmpty(r.previewUrl)) return null;
        if (TextUtils.isEmpty(r.trackName) && TextUtils.isEmpty(r.artistName)) return null;

        Song song = new Song();
        song.setId(r.trackId != null ? String.valueOf(r.trackId) : r.previewUrl);
        song.setTitle(r.trackName != null ? r.trackName : "Video Preview");
        song.setArtist(r.artistName != null ? r.artistName : "");
        song.setAlbum(!TextUtils.isEmpty(r.collectionName) ? r.collectionName : "iTunes");
        song.setImageUrl(r.artworkUrl100);
        song.setPreviewUrl(r.previewUrl);
        song.setExternalUrl(r.trackViewUrl);
        song.setPlatform("itunes");
        if (r.trackTimeMillis != null && r.trackTimeMillis > 0) {
            int sec = (int) (r.trackTimeMillis / 1000L);
            song.setDuration(DurationUtils.secondsToStorageString(sec));
        }
        return song;
    }
}

