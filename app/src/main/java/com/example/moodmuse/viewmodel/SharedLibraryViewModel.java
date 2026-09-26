package com.example.moodmuse.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.moodmuse.api.jamendo.JamendoRepository;
import com.example.moodmuse.database.AppDatabase;
import com.example.moodmuse.database.PlaylistEntity;
import com.example.moodmuse.database.PlaylistSongCrossRef;
import com.example.moodmuse.database.SongDao;
import com.example.moodmuse.database.SongEntity;
import com.example.moodmuse.database.PlaylistDao;
import com.example.moodmuse.models.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SharedLibraryViewModel extends AndroidViewModel {
    private final SongDao songDao;
    private final PlaylistDao playlistDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private final LiveData<List<SongEntity>> likedSongs;
    private final LiveData<List<PlaylistEntity>> playlists;
    
    private final MutableLiveData<List<Song>> searchResults = new MutableLiveData<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    public SharedLibraryViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        songDao = db.songDao();
        playlistDao = db.playlistDao();
        likedSongs = songDao.getAllLikedSongs();
        playlists = playlistDao.getAllPlaylists();
    }

    public LiveData<List<SongEntity>> getLikedSongs() {
        return likedSongs;
    }

    public LiveData<List<PlaylistEntity>> getPlaylists() {
        return playlists;
    }

    public LiveData<List<Song>> getSearchResults() {
        return searchResults;
    }

    public void addLikedSong(Song song) {
        executor.execute(() -> songDao.insert(SongEntity.fromSong(song)));
    }

    public void removeLikedSong(String songId) {
        executor.execute(() -> songDao.deleteById(songId));
    }

    public void replaceAllLikedSongs(List<Song> songs) {
        executor.execute(() -> {
            songDao.clearAll();
            if (songs == null || songs.isEmpty()) return;
            List<SongEntity> entities = new ArrayList<>();
            for (Song s : songs) {
                if (s == null) continue;
                entities.add(SongEntity.fromSong(s));
            }
            songDao.insertAll(entities);
        });
    }

    public void createPlaylist(String name, String description) {
        executor.execute(() -> playlistDao.insertPlaylist(new PlaylistEntity(name, description, null, "playlist")));
    }

    public void createAlbum(String name, String description) {
        executor.execute(() -> playlistDao.insertPlaylist(new PlaylistEntity(name, description, null, "album")));
    }

    public void renamePlaylist(int id, String newName) {
        executor.execute(() -> playlistDao.renamePlaylist(id, newName));
    }

    public void deletePlaylist(int id) {
        executor.execute(() -> playlistDao.deletePlaylist(id));
    }

    public void addSongToPlaylist(int playlistId, String songId) {
        executor.execute(() -> playlistDao.addSongToPlaylist(new PlaylistSongCrossRef(playlistId, songId)));
    }

    public void search(String query) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        if (query == null || query.trim().isEmpty()) {
            searchResults.setValue(new ArrayList<>());
            return;
        }

        searchRunnable = () -> {
            executor.execute(() -> {
                List<SongEntity> local = songDao.searchLikedSongs("%" + query + "%");
                List<Song> results = new ArrayList<>();
                for (SongEntity entity : local) {
                    results.add(entity.toSong());
                }
                
                if (results.size() < 5) {
                    JamendoRepository.getInstance(getApplication()).searchTracks(query, new JamendoRepository.TracksCallback() {
                        @Override
                        public void onSuccess(@NonNull List<Song> songs) {
                            for (Song s : songs) {
                                boolean exists = false;
                                for (Song r : results) {
                                    if (s.getId().equals(r.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) results.add(s);
                            }
                            searchResults.postValue(results);
                        }

                        @Override
                        public void onError() {
                            searchResults.postValue(results);
                        }
                    });
                } else {
                    searchResults.postValue(results);
                }
            });
        };

        searchHandler.postDelayed(searchRunnable, 300);
    }
}
