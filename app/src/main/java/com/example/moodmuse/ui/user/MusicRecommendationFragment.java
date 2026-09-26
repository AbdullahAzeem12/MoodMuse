package com.example.moodmuse.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmuse.R;
import com.example.moodmuse.ui.RecyclerViewScrollFadeHelper;
import com.google.android.material.tabs.TabLayout;
import com.example.moodmuse.adapters.SongWithControlsAdapter;
import com.example.moodmuse.api.jamendo.JamendoRepository;
import com.example.moodmuse.api.itunes.ItunesRepository;
import com.example.moodmuse.database.PlaylistEntity;
import com.example.moodmuse.database.SongEntity;
import com.example.moodmuse.models.Song;
import com.example.moodmuse.viewmodel.SharedLibraryViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MusicRecommendationFragment extends Fragment implements SongWithControlsAdapter.OnSongClickListener, com.example.moodmuse.adapters.SongAdapter.PlaybackHost, com.example.moodmuse.adapters.SongAdapter.OnSongPlayClickListener {
    private static final String ARG_EMOTION = "emotion";
    private String currentEmotion = "happy";
    
    private SongWithControlsAdapter jamendoAdapter;
    private com.example.moodmuse.adapters.SongAdapter itunesAdapter;
    private final List<Song> jamendoSongs = new ArrayList<>();
    private final List<Song> itunesVideos = new ArrayList<>();
    private ExoPlayer exoPlayer;
    private FirebaseFirestore db;
    private String userId;
    private ListenerRegistration likedSongsReg;
    private ListenerRegistration playlistsReg;
    private final Set<String> likedSongIds = new HashSet<>();
    private final List<PlaylistEntity> playlists = new ArrayList<>();
    private SharedLibraryViewModel libraryViewModel;

    // Animation members
    private View[] decorativeViews;
    private CircleData[] circleData;
    private Handler bubbleAnimationHandler;
    private Runnable bubbleAnimationRunnable;
    private int screenWidth, screenHeight;

    public static MusicRecommendationFragment newInstance(String emotion) {
        MusicRecommendationFragment fragment = new MusicRecommendationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMOTION, emotion != null ? emotion : "happy");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentEmotion = getArguments().getString(ARG_EMOTION, "happy");
        }
        exoPlayer = new ExoPlayer.Builder(requireContext()).build();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_recommendation, container, false);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();

        // Initialize TabLayout
        TabLayout tabLayout = view.findViewById(R.id.music_tabs);
        tabLayout.addTab(tabLayout.newTab().setText("Jamendo Tracks"));
        tabLayout.addTab(tabLayout.newTab().setText("iTunes Videos"));

        // Initialize RecyclerViews
        RecyclerView jamendoRv = view.findViewById(R.id.jamendo_recycler_view);
        RecyclerView itunesRv = view.findViewById(R.id.itunes_recycler_view);

        jamendoRv.setLayoutManager(new LinearLayoutManager(getContext()));
        itunesRv.setLayoutManager(new LinearLayoutManager(getContext()));

        jamendoAdapter = new SongWithControlsAdapter(jamendoSongs, this, getLifecycle());
        itunesAdapter = new com.example.moodmuse.adapters.SongAdapter(itunesVideos, this, this, this);

        jamendoAdapter.setMood(currentEmotion);

        jamendoRv.setAdapter(jamendoAdapter);
        itunesRv.setAdapter(itunesAdapter);

        RecyclerViewScrollFadeHelper.attach(jamendoRv);
        RecyclerViewScrollFadeHelper.attach(itunesRv);

        libraryViewModel = new ViewModelProvider(requireActivity()).get(SharedLibraryViewModel.class);
        libraryViewModel.getLikedSongs().observe(getViewLifecycleOwner(), entities -> {
            likedSongIds.clear();
            if (entities != null) {
                for (SongEntity e : entities) {
                    if (e != null && e.id != null) likedSongIds.add(e.id);
                }
            }
            jamendoAdapter.setLikedSongIds(likedSongIds);
            itunesAdapter.setLikedSongIds(likedSongIds);
        });

        attachFirestoreListeners();

        // Add Swipe-to-Dismiss
        setupSwipeToDismiss(jamendoRv, jamendoAdapter, jamendoSongs);
        setupSwipeToDismiss(itunesRv, itunesAdapter, itunesVideos);

        // Tab selection logic
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    jamendoRv.setVisibility(View.VISIBLE);
                    itunesRv.setVisibility(View.GONE);
                    jamendoRv.scheduleLayoutAnimation();
                } else {
                    jamendoRv.setVisibility(View.GONE);
                    itunesRv.setVisibility(View.VISIBLE);
                    itunesRv.scheduleLayoutAnimation();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Initialize decorative views for animation
        decorativeViews = new View[]{
                view.findViewById(R.id.decorCircle1), view.findViewById(R.id.decorCircle2),
                view.findViewById(R.id.decorCircle3), view.findViewById(R.id.decorCircle4),
                view.findViewById(R.id.decorCircle5), view.findViewById(R.id.decorCircle6),
                view.findViewById(R.id.decorCircle7), view.findViewById(R.id.decorCircle8),
                view.findViewById(R.id.decorCircle9), view.findViewById(R.id.decorCircle10),
                view.findViewById(R.id.decorCircle11)
        };
        setupFloatingAnimations(view);

        fetchMusic();
        return view;
    }

    private void attachFirestoreListeners() {
        if (userId == null) return;

        if (likedSongsReg != null) likedSongsReg.remove();
        likedSongsReg = db.collection("users").document(userId)
                .collection("liked_songs")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    if (libraryViewModel == null) return;
                    List<Song> remote = new ArrayList<>();
                    snap.getDocuments().forEach(d -> {
                        Song s = d.toObject(Song.class);
                        if (s == null) return;
                        if (s.getId() == null) s.setId(d.getId());
                        remote.add(s);
                    });
                    libraryViewModel.replaceAllLikedSongs(remote);
                });

        if (playlistsReg != null) playlistsReg.remove();
        playlistsReg = db.collection("users").document(userId)
                .collection("playlists")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    playlists.clear();
                    snap.getDocuments().forEach(d -> {
                        PlaylistEntity p = new PlaylistEntity();
                        p.remoteId = d.getId();
                        p.name = d.getString("name");
                        p.description = d.getString("description");
                        p.coverImageUrl = d.getString("coverImageUrl");
                        Long createdAt = d.getLong("createdAt");
                        p.createdAt = createdAt != null ? createdAt : System.currentTimeMillis();
                        p.type = d.getString("type");
                        playlists.add(p);
                    });
                });
    }

    private void setupSwipeToDismiss(RecyclerView recyclerView, SongWithControlsAdapter adapter, List<Song> songList) {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    adapter.removeSong(position);
                    songList.remove(position); // Keep fragment's list in sync
                    Toast.makeText(getContext(), "Item removed", Toast.LENGTH_SHORT).show();
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void setupSwipeToDismiss(RecyclerView recyclerView, com.example.moodmuse.adapters.SongAdapter adapter, List<Song> songList) {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    songList.remove(position);
                    adapter.updateSongs(songList);
                    Toast.makeText(getContext(), "Item removed", Toast.LENGTH_SHORT).show();
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private List<Song> filterDuplicatesByTrackId(@NonNull List<Song> songs) {
        Set<String> seenTrackIds = new HashSet<>();
        List<Song> filtered = new ArrayList<>();
        for (Song song : songs) {
            if (song.getId() != null && !seenTrackIds.contains(song.getId())) {
                seenTrackIds.add(song.getId());
                filtered.add(song);
            }
        }
        return filtered;
    }

    private void fetchMusic() {
        jamendoSongs.clear();
        itunesVideos.clear();

        // Jamendo
        JamendoRepository.getInstance(requireContext()).requestTracks(currentEmotion, 20, new JamendoRepository.TracksCallback() {
            @Override
            public void onSuccess(@NonNull List<Song> songs) {
                if (isAdded()) {
                    // Filter and add at least 15 if possible
                    jamendoSongs.addAll(songs);
                    jamendoAdapter.updateSongs(jamendoSongs);
                    RecyclerView rv = getView() != null ? getView().findViewById(R.id.jamendo_recycler_view) : null;
                    if (rv != null) {
                        rv.scheduleLayoutAnimation();
                        rv.post(() -> RecyclerViewScrollFadeHelper.apply(rv));
                    }
                }
            }
            @Override public void onError() {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Could not load Jamendo tracks.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        new ItunesRepository().searchByEmotion(currentEmotion, new ItunesRepository.ItunesSearchCallback() {
            @Override
            public void onSuccess(@NonNull List<Song> songs) {
                if (!isAdded()) return;
                
                List<Song> filteredSongs = filterDuplicatesByTrackId(songs);
                
                // Ensure we try to get at least 10-15
                itunesVideos.addAll(filteredSongs);
                
                // If we got very few results, try a fallback search with " music" appended
                if (itunesVideos.size() < 10) {
                    new ItunesRepository().searchByTerm(currentEmotion + " music", 20, new ItunesRepository.ItunesSearchCallback() {
                        @Override
                        public void onSuccess(@NonNull List<Song> fallbackSongs) {
                            if (!isAdded()) return;
                            List<Song> filteredFallback = filterDuplicatesByTrackId(fallbackSongs);
                            for (Song s : filteredFallback) {
                                if (itunesVideos.size() >= 20) break;
                                boolean exists = false;
                                for (Song existing : itunesVideos) {
                                    if (existing.getId() != null && existing.getId().equals(s.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) itunesVideos.add(s);
                            }
                            updateItunesUI();
                        }
                        @Override public void onError(@NonNull Throwable error) {
                            updateItunesUI();
                        }
                    });
                } else {
                    updateItunesUI();
                }
            }

            @Override
            public void onError(@NonNull Throwable error) {
                Log.e("MusicRecommendation", "iTunes load failed", error);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Could not load iTunes video previews.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateItunesUI() {
        if (!isAdded()) return;
        itunesAdapter.updateSongs(itunesVideos);
        RecyclerView rv = getView() != null ? getView().findViewById(R.id.itunes_recycler_view) : null;
        if (rv != null) {
            rv.scheduleLayoutAnimation();
            rv.post(() -> RecyclerViewScrollFadeHelper.apply(rv));
        }
    }

    @Override
    public void onSongClick(Song song, int position) {
        onPlayPauseClick(song, position);
    }

    @Override
    public void onPlayPauseClick(Song song, int position) {
        handleJamendoPlayPause(song, position);
    }

    private void handleJamendoPlayPause(@NonNull Song song, int position) {
        boolean isNowPlaying = position != jamendoAdapter.getCurrentlyPlayingPosition() || !jamendoAdapter.isPlaying();

        if (isNowPlaying) {
            if (itunesAdapter != null) itunesAdapter.stopPlayback();

            if (song.getPreviewUrl() != null && !song.getPreviewUrl().isEmpty()) {
                exoPlayer.setMediaItem(MediaItem.fromUri(song.getPreviewUrl()));
                exoPlayer.prepare();
                exoPlayer.play();
                jamendoAdapter.setPlaybackState(position, true);
            } else {
                if (isAdded()) Toast.makeText(getContext(), "No preview available", Toast.LENGTH_SHORT).show();
                jamendoAdapter.setPlaybackState(position, false);
            }
            saveToHistory(song);
        } else {
            exoPlayer.pause();
            jamendoAdapter.setPlaybackState(position, false);
        }
    }

    private void stopJamendoPlayback() {
        if (exoPlayer != null) exoPlayer.stop();
        if (jamendoAdapter != null) jamendoAdapter.clearPlaybackState();
    }

    @Override
    public void onNextClick(Song song, int position) {
        List<Song> targetList = jamendoSongs;
        if (position < targetList.size() - 1) {
            onPlayPauseClick(targetList.get(position + 1), position + 1);
        }
    }

    @Override
    public void onPreviousClick(Song song, int position) {
        List<Song> targetList = jamendoSongs;
        if (position > 0) {
            onPlayPauseClick(targetList.get(position - 1), position - 1);
        }
    }

    @Override
    public void onExternalOpenClick(Song song, int position) {
    }

    // --- Bouncing Circles Logic ---

    private static class CircleData {
        View view;
        float x, y, velocityX, velocityY, radius;
        CircleData(View view, float x, float y, float radius) {
            this.view = view; this.x = x; this.y = y; this.radius = radius;
            this.velocityX = (float) (Math.random() * 8 - 4);
            this.velocityY = (float) (Math.random() * 8 - 4);
        }
    }

    private void setupFloatingAnimations(View rootView) {
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        rootView.post(() -> {
            if (!isAdded() || decorativeViews == null) return;
            circleData = new CircleData[decorativeViews.length];
            for (int i = 0; i < decorativeViews.length; i++) {
                if (decorativeViews[i] != null) {
                    decorativeViews[i].bringToFront();
                    float radius = decorativeViews[i].getWidth() / 2f;
                    if (radius <= 0) radius = 50f;
                    float x = (float) (Math.random() * (screenWidth - radius * 2)) + radius;
                    float y = (float) (Math.random() * (screenHeight - radius * 2)) + radius;
                    circleData[i] = new CircleData(decorativeViews[i], x, y, radius);
                }
            }
            preventOverlaps();
            startRandomBouncingAnimation();
        });
    }

    private void startRandomBouncingAnimation() {
        bubbleAnimationHandler = new Handler(Looper.getMainLooper());
        bubbleAnimationRunnable = new Runnable() {
            @Override public void run() {
                if (!isAdded() || circleData == null) return;
                updateCirclePositions();
                checkCollisions();
                bubbleAnimationHandler.postDelayed(this, 16);
            }
        };
        bubbleAnimationHandler.post(bubbleAnimationRunnable);
    }

    private void updateCirclePositions() {
        for (CircleData circle : circleData) {
            if (circle == null || circle.view == null) continue;
            circle.x += circle.velocityX; circle.y += circle.velocityY;
            if (circle.x <= circle.radius || circle.x >= screenWidth - circle.radius) {
                circle.velocityX *= -1;
                circle.x = Math.max(circle.radius, Math.min(screenWidth - circle.radius, circle.x));
            }
            if (circle.y <= circle.radius || circle.y >= screenHeight - circle.radius) {
                circle.velocityY *= -1;
                circle.y = Math.max(circle.radius, Math.min(screenHeight - circle.radius, circle.y));
            }
            circle.view.setX(circle.x - circle.radius);
            circle.view.setY(circle.y - circle.radius);
        }
    }

    private void checkCollisions() {
        if (circleData == null) return;
        for (int i = 0; i < circleData.length; i++) {
            for (int j = i + 1; j < circleData.length; j++) {
                CircleData c1 = circleData[i];
                CircleData c2 = circleData[j];
                if (c1 != null && c2 != null) {
                    float dx = c2.x - c1.x;
                    float dy = c2.y - c1.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float minDistance = c1.radius + c2.radius;
                    if (distance < minDistance && distance > 0) {
                        float nx = dx / distance;
                        float ny = dy / distance;
                        float separation = minDistance - distance;
                        c1.x -= nx * separation / 2f;
                        c1.y -= ny * separation / 2f;
                        c2.x += nx * separation / 2f;
                        c2.y += ny * separation / 2f;
                        float v1n = c1.velocityX * nx + c1.velocityY * ny;
                        float v2n = c2.velocityX * nx + c2.velocityY * ny;
                        float v1t = -c1.velocityX * ny + c1.velocityY * nx;
                        float v2t = -c2.velocityX * ny + c2.velocityY * nx;
                        c1.velocityX = v2n * nx - v1t * ny;
                        c1.velocityY = v2n * ny + v1t * nx;
                        c2.velocityX = v1n * nx - v2t * ny;
                        c2.velocityY = v1n * ny + v2t * nx;
                    }
                }
            }
        }
    }

    private void preventOverlaps() {
        if (circleData == null) return;
        for (int i = 0; i < 30; i++) {
            boolean overlapFound = false;
            for (int j = 0; j < circleData.length; j++) {
                for (int k = j + 1; k < circleData.length; k++) {
                    CircleData c1 = circleData[j];
                    CircleData c2 = circleData[k];
                    if (c1 == null || c2 == null) continue;
                    float dx = c2.x - c1.x;
                    float dy = c2.y - c1.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float minDistance = c1.radius + c2.radius + 10;
                    if (distance < minDistance) {
                        overlapFound = true;
                        float angle = (float) (Math.random() * 2 * Math.PI);
                        c2.x = c1.x + (float) Math.cos(angle) * minDistance;
                        c2.y = c1.y + (float) Math.sin(angle) * minDistance;
                        c2.x = Math.max(c2.radius, Math.min(screenWidth - c2.radius, c2.x));
                        c2.y = Math.max(c2.radius, Math.min(screenHeight - c2.radius, c2.y));
                    }
                }
            }
            if (!overlapFound) break;
        }
    }

    private void saveToHistory(Song song) {
        if (userId == null || song == null) return;
        String docId = (song.getId() != null ? song.getId() : "song") + "_" + System.currentTimeMillis();
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", song.getId());
        payload.put("title", song.getTitle());
        payload.put("artist", song.getArtist());
        payload.put("album", song.getAlbum());
        payload.put("imageUrl", song.getImageUrl());
        payload.put("previewUrl", song.getPreviewUrl());
        payload.put("externalUrl", song.getExternalUrl());
        payload.put("platform", song.getPlatform());
        payload.put("duration", song.getDuration());
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("emotion", currentEmotion);
        db.collection("users").document(userId)
                .collection("history")
                .document(docId)
                .set(payload);
    }

    @Override
    public void onLikeClick(Song song, int position) {
        if (userId == null || song == null || song.getId() == null) return;
        boolean nowLiked = jamendoAdapter.isLikedSongId(song.getId()) || itunesAdapter.isLikedSongId(song.getId());
        if (nowLiked) {
            if (libraryViewModel != null) libraryViewModel.addLikedSong(song);
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", song.getId());
            payload.put("title", song.getTitle());
            payload.put("artist", song.getArtist());
            payload.put("album", song.getAlbum());
            payload.put("imageUrl", song.getImageUrl());
            payload.put("previewUrl", song.getPreviewUrl());
            payload.put("externalUrl", song.getExternalUrl());
            payload.put("platform", song.getPlatform());
            payload.put("duration", song.getDuration());
            payload.put("createdAt", System.currentTimeMillis());
            db.collection("users").document(userId)
                    .collection("liked_songs")
                    .document(song.getId())
                    .set(payload);
        } else {
            if (libraryViewModel != null) libraryViewModel.removeLikedSong(song.getId());
            db.collection("users").document(userId)
                    .collection("liked_songs")
                    .document(song.getId())
                    .delete();
        }
    }

    @Override
    public void onAddToPlaylistClick(Song song, int position) {
        if (!isAdded() || userId == null) return;
        if (playlists.isEmpty()) {
            Toast.makeText(getContext(), "Create a playlist in Library first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).name;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add to Playlist")
                .setItems(names, (dialog, which) -> {
                    PlaylistEntity p = playlists.get(which);
                    if (p.remoteId != null) addSongToPlaylist(p.remoteId, song);
                }).show();
    }

    private void addSongToPlaylist(String playlistId, Song song) {
        if (userId == null || song == null || song.getId() == null) return;
        db.collection("users").document(userId)
                .collection("playlists").document(playlistId)
                .collection("songs").document(song.getId())
                .set(song)
                .addOnSuccessListener(v -> {
                    if (isAdded()) Toast.makeText(getContext(), "Added!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onShareClick(Song song, int position) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, song.getTitle() + " - " + (song.getExternalUrl() != null ? song.getExternalUrl() : ""));
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (exoPlayer != null) exoPlayer.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bubbleAnimationHandler != null) bubbleAnimationHandler.removeCallbacks(bubbleAnimationRunnable);
        if (likedSongsReg != null) likedSongsReg.remove();
        if (playlistsReg != null) playlistsReg.remove();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    public void onItunesPlaybackStarted() {
        stopJamendoPlayback();
    }

    @Override
    public void onSongPlayClick(String previewUrl, int position) {
        handleItunesPlayPause(previewUrl, position);
    }

    private void handleItunesPlayPause(String previewUrl, int position) {
        boolean isSame = position == itunesAdapter.getCurrentlyPlayingPosition();
        if (isSame && itunesAdapter.isPlaying()) {
            exoPlayer.pause();
            itunesAdapter.setPlaybackState(position, false);
            return;
        }

        stopJamendoPlayback();

        exoPlayer.stop();
        exoPlayer.setMediaItem(MediaItem.fromUri(previewUrl));
        exoPlayer.prepare();
        exoPlayer.play();

        itunesAdapter.setPlaybackState(position, true);
        if (position < itunesVideos.size()) {
            saveToHistory(itunesVideos.get(position));
        }
    }
}
