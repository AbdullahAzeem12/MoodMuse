package com.example.moodmuse.storage;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.moodmuse.models.Song;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LikedSongsStore {
    private static final String KEY_LIKED_SONGS = "liked_songs_v1";
    private static final Gson gson = new Gson();
    private static final Type listType = new TypeToken<List<Song>>() {}.getType();
    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    public interface Listener {
        void onChanged(@NonNull List<Song> songs);
    }

    private LikedSongsStore() {
    }

    @NonNull
    public static List<Song> getAll(@NonNull Context context) {
        SecureKeyValueStore store = new SecureKeyValueStore(context);
        String raw = store.getString(KEY_LIKED_SONGS, "");
        if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();
        try {
            List<Song> parsed = gson.fromJson(raw, listType);
            return parsed != null ? new ArrayList<>(parsed) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static boolean add(@NonNull Context context, @NonNull Song song) {
        List<Song> current = getAll(context);
        String id = song.getId();
        String externalUrl = song.getExternalUrl();
        for (Song s : current) {
            if (s == null) continue;
            if (id != null && id.equals(s.getId())) return false;
            if (externalUrl != null && externalUrl.equals(s.getExternalUrl())) return false;
        }
        current.add(0, song);
        persist(context, current);
        notifyListeners(current);
        return true;
    }

    public static void addListener(@NonNull Context context, @NonNull Listener listener) {
        listeners.addIfAbsent(listener);
        listener.onChanged(getAll(context));
    }

    public static void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public static boolean isLiked(Context context, String songId) {
        if (songId == null) return false;
        List<Song> liked = getAll(context);
        for (Song s : liked) {
            if (songId.equals(s.getId())) return true;
        }
        return false;
    }

    public static boolean remove(Context context, String songId) {
        if (songId == null) return false;
        List<Song> liked = getAll(context);
        boolean removed = false;
        for (int i = 0; i < liked.size(); i++) {
            if (songId.equals(liked.get(i).getId())) {
                liked.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            persist(context, liked);
            notifyListeners(liked);
        }
        return removed;
    }

    private static void persist(@NonNull Context context, @NonNull List<Song> songs) {
        SecureKeyValueStore store = new SecureKeyValueStore(context);
        store.putString(KEY_LIKED_SONGS, gson.toJson(songs));
    }

    private static void notifyListeners(@NonNull List<Song> songs) {
        List<Song> snapshot = new ArrayList<>(songs);
        for (Listener l : listeners) {
            if (l != null) l.onChanged(snapshot);
        }
    }
}
