package com.example.moodmuse.api.jamendo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.moodmuse.BuildConfig;
import com.example.moodmuse.models.Song;
import com.example.moodmuse.ml.EmotionRecognitionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class JamendoRepository {
    private static final String JAMENDO_API_BASE_URL = "https://api.jamendo.com/v3.0/";
    private static final int IMAGE_SIZE = 300;

    public interface TracksCallback {
        void onSuccess(@NonNull List<Song> songs);
        void onError();
    }

    private static volatile JamendoRepository instance;

    private final JamendoApiService api;
    private final Map<String, List<Song>> cache = new ConcurrentHashMap<>();
    private final Map<String, Call<JamendoTrackResponse>> inFlight = new ConcurrentHashMap<>();

    private JamendoRepository(@NonNull Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(JAMENDO_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(JamendoApiService.class);
    }

    @NonNull
    public static JamendoRepository getInstance(@NonNull Context context) {
        JamendoRepository local = instance;
        if (local != null) return local;
        synchronized (JamendoRepository.class) {
            if (instance == null) {
                instance = new JamendoRepository(context.getApplicationContext());
            }
            return instance;
        }
    }

    @Nullable
    public List<Song> getCached(@Nullable String emotion) {
        String key = normalizeEmotion(emotion);
        List<Song> cached = cache.get(key);
        if (cached == null || cached.isEmpty()) return null;
        return new ArrayList<>(cached);
    }

    public void prefetchAll() {
        requestTracksInternal(EmotionRecognitionModel.EMOTION_HAPPY, 20, false, null, 0, false);
        requestTracksInternal(EmotionRecognitionModel.EMOTION_SAD, 20, false, null, 0, false);
        requestTracksInternal(EmotionRecognitionModel.EMOTION_NEUTRAL, 20, false, null, 0, false);
        requestTracksInternal(EmotionRecognitionModel.EMOTION_SURPRISED, 20, false, null, 0, false);
        requestTracksInternal(EmotionRecognitionModel.EMOTION_DISGUST, 20, false, null, 0, false);
        requestTracksInternal(EmotionRecognitionModel.EMOTION_FEAR, 20, false, null, 0, false);
        requestTracksInternal(EmotionRecognitionModel.EMOTION_ANGRY, 20, false, null, 0, false);
    }

    public void requestTracks(@Nullable String emotion, int limit, @NonNull TracksCallback callback) {
        requestTracksInternal(emotion, limit, true, callback, 0, false);
    }

    public void requestTracksRandom(@Nullable String emotion, int limit, @NonNull TracksCallback callback) {
        String key = normalizeEmotion(emotion);
        Call<JamendoTrackResponse> inFlightCall = inFlight.remove(key);
        if (inFlightCall != null) inFlightCall.cancel();
        int offset = (int) (Math.random() * 500);
        requestTracksInternal(key, limit, true, callback, offset, true);
    }
    
    public void searchTracks(String query, @NonNull TracksCallback callback) {
        String clientId = BuildConfig.JAMENDO_CLIENT_ID;
        Call<JamendoTrackResponse> call = api.searchTracks(clientId, query, 20, "json", "musicinfo", "mp32", IMAGE_SIZE);
        call.enqueue(new Callback<JamendoTrackResponse>() {
            @Override
            public void onResponse(@NonNull Call<JamendoTrackResponse> call, @NonNull Response<JamendoTrackResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResults() != null) {
                    List<Song> songs = convertToSongs(response.body().getResults(), null);
                    if (callback != null) callback.onSuccess(songs);
                } else {
                    if (callback != null) callback.onError();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<JamendoTrackResponse> call, @NonNull Throwable t) {
                if (callback != null) callback.onError();
            }
        });
    }

    @NonNull
    private List<Song> convertToSongs(@NonNull List<JamendoTrack> results, @Nullable String moodKey) {
        List<Song> mapped = new ArrayList<>();
        for (JamendoTrack t : results) {
            if (t == null) continue;
            Song s = new Song();
            s.setId(t.getId());
            s.setTitle(t.getName());
            s.setArtist(t.getArtistName());
            s.setAlbum(t.getAlbumName());
            s.setImageUrl(t.getImage());
            s.setPreviewUrl(t.getAudio());
            s.setExternalUrl(t.getShareUrl());
            s.setPlatform("Jamendo");
            s.setDuration(String.valueOf(t.getDuration()));
            if (moodKey != null) s.setMoodTag(moodKey);
            mapped.add(s);
        }
        return mapped;
    }

    private void requestTracksInternal(@Nullable String emotion, int limit, boolean deliver, @Nullable TracksCallback callback, int offset, boolean forceRefresh) {
        String key = normalizeEmotion(emotion);
        if (!deliver && !forceRefresh) {
            List<Song> existing = cache.get(key);
            if (existing != null && !existing.isEmpty()) return;
        }

        Call<JamendoTrackResponse> existingCall = inFlight.get(key);
        if (existingCall != null) {
            if (forceRefresh) {
                existingCall.cancel();
                inFlight.remove(key);
            } else {
                return;
            }
        }

        String clientId = BuildConfig.JAMENDO_CLIENT_ID;
        String fuzzyTags = JamendoMoodTagMapper.toFuzzyTags(key);

        Call<JamendoTrackResponse> call = api.getTracks(
                clientId,
                "json",
                Math.max(1, Math.min(200, limit)),
                Math.max(0, offset),
                fuzzyTags,
                "musicinfo",
                "mp32",
                IMAGE_SIZE,
                "popularity_total_desc"
        );
        inFlight.put(key, call);

        call.enqueue(new Callback<JamendoTrackResponse>() {
            @Override
            public void onResponse(@NonNull Call<JamendoTrackResponse> call, @NonNull Response<JamendoTrackResponse> response) {
                inFlight.remove(key);
                if (!response.isSuccessful() || response.body() == null || response.body().getResults() == null) {
                    if (deliver && callback != null) callback.onError();
                    return;
                }
                List<JamendoTrack> results = response.body().getResults();
                List<Song> mapped = convertToSongs(results, key);
                cache.put(key, mapped);
                if (deliver && callback != null) callback.onSuccess(new ArrayList<>(mapped));
            }

            @Override
            public void onFailure(@NonNull Call<JamendoTrackResponse> call, @NonNull Throwable t) {
                inFlight.remove(key);
                if (deliver && callback != null) callback.onError();
            }
        });
    }

    @NonNull
    private static String normalizeEmotion(@Nullable String emotion) {
        String e = emotion != null ? emotion.trim() : "";
        if (e.isEmpty()) return EmotionRecognitionModel.EMOTION_NEUTRAL;
        String lower = e.toLowerCase(Locale.ROOT);
        if (lower.equals("happiness") || lower.equals("joy")) return EmotionRecognitionModel.EMOTION_HAPPY;
        if (lower.equals("sadness")) return EmotionRecognitionModel.EMOTION_SAD;
        if (lower.equals("surprise")) return EmotionRecognitionModel.EMOTION_SURPRISED;
        if (lower.equals("fearful")) return EmotionRecognitionModel.EMOTION_FEAR;
        if (lower.equals("anger")) return EmotionRecognitionModel.EMOTION_ANGRY;
        if (lower.equals("disgusted")) return EmotionRecognitionModel.EMOTION_DISGUST;
        if (lower.equals("happy")) return EmotionRecognitionModel.EMOTION_HAPPY;
        if (lower.equals("sad")) return EmotionRecognitionModel.EMOTION_SAD;
        if (lower.equals("neutral")) return EmotionRecognitionModel.EMOTION_NEUTRAL;
        if (lower.equals("surprised")) return EmotionRecognitionModel.EMOTION_SURPRISED;
        if (lower.equals("fear")) return EmotionRecognitionModel.EMOTION_FEAR;
        if (lower.equals("angry")) return EmotionRecognitionModel.EMOTION_ANGRY;
        if (lower.equals("disgust")) return EmotionRecognitionModel.EMOTION_DISGUST;
        return e;
    }
}
