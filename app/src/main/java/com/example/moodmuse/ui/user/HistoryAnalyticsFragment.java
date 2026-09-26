package com.example.moodmuse.ui.user;

import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmuse.R;
import com.example.moodmuse.adapters.SongWithControlsAdapter;
import com.example.moodmuse.database.PlaylistEntity;
import com.example.moodmuse.models.Song;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HistoryAnalyticsFragment extends Fragment implements SongWithControlsAdapter.OnSongClickListener {
    private SongWithControlsAdapter songAdapter;
    private final List<Song> historySongs = new ArrayList<>();
    private final List<String> historyDocIds = new ArrayList<>();
    private ExoPlayer exoPlayer;
    private TextView tvTotalSessions, tvTopMood;
    private FirebaseFirestore db;
    private String userId;
    private ListenerRegistration historyReg;
    private ListenerRegistration moodScansReg;
    private ListenerRegistration likedSongsReg;
    private ListenerRegistration playlistsReg;
    private final Set<String> likedSongIds = new HashSet<>();
    private final List<PlaylistEntity> playlists = new ArrayList<>();

    // Animation members
    private View[] decorativeViews;
    private CircleData[] circleData;
    private Handler bubbleAnimationHandler;
    private Runnable bubbleAnimationRunnable;
    private int screenWidth, screenHeight;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(requireContext()).build();
        }
        View view = inflater.inflate(R.layout.fragment_history_analytics, container, false);

        tvTotalSessions = view.findViewById(R.id.total_entries);
        tvTopMood = view.findViewById(R.id.most_common_mood);

        MaterialCardView cardTotal = view.findViewById(R.id.card_total_sessions);
        MaterialCardView cardMood = view.findViewById(R.id.card_top_mood);
        applyGlassyEffect(cardTotal);
        applyGlassyEffect(cardMood);

        RecyclerView recyclerView = view.findViewById(R.id.history_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        songAdapter = new SongWithControlsAdapter(historySongs, this, getLifecycle());
        recyclerView.setAdapter(songAdapter);

        // Add Swipe-to-Dismiss
        setupSwipeToDismiss(recyclerView);

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

        attachFirestoreListeners();
        return view;
    }

    private void attachFirestoreListeners() {
        if (userId == null) return;

        if (historyReg != null) historyReg.remove();
        historyReg = db.collection("users").document(userId)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || !isAdded()) return;
                    historySongs.clear();
                    historyDocIds.clear();
                    snap.getDocuments().forEach(d -> {
                        Song s = d.toObject(Song.class);
                        if (s != null) {
                            historySongs.add(s);
                            historyDocIds.add(d.getId());
                        }
                    });
                    songAdapter.updateSongs(historySongs);
                });

        if (moodScansReg != null) moodScansReg.remove();
        moodScansReg = db.collection("users").document(userId)
                .collection("mood_scans")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || !isAdded()) return;
                    
                    int totalScans = snap.size();
                    if (tvTotalSessions != null) tvTotalSessions.setText(String.valueOf(totalScans));
                    
                    if (totalScans > 0) {
                        Map<String, Integer> moodCounts = new HashMap<>();
                        snap.getDocuments().forEach(d -> {
                            String mood = d.getString("emotion");
                            if (mood != null) {
                                Integer count = moodCounts.get(mood);
                                moodCounts.put(mood, (count == null ? 0 : count) + 1);
                            }
                        });
                        
                        String topMood = "None";
                        int maxCount = -1;
                        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
                            Integer val = entry.getValue();
                            if (val != null && val > maxCount) {
                                maxCount = val;
                                topMood = entry.getKey();
                            }
                        }
                        if (tvTopMood != null) tvTopMood.setText(topMood);
                    } else {
                        if (tvTopMood != null) tvTopMood.setText("None");
                    }
                });

        if (likedSongsReg != null) likedSongsReg.remove();
        likedSongsReg = db.collection("users").document(userId)
                .collection("liked_songs")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    likedSongIds.clear();
                    snap.getDocuments().forEach(d -> {
                        String id = d.getString("id");
                        if (id != null) likedSongIds.add(id);
                    });
                    songAdapter.setLikedSongIds(likedSongIds);
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

    private void applyGlassyEffect(MaterialCardView cardView) {
        if (cardView == null) return;
        // Make card fully transparent so the inner LinearLayout background (@drawable/bg_button_pill_glass) shows through
        cardView.setCardBackgroundColor(Color.TRANSPARENT);
        cardView.setStrokeWidth(0);
        cardView.setCardElevation(0);
        cardView.setRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        
        // Ensure no foreground is blocking the glass effect
        cardView.setForeground(null);
    }

    private void setupSwipeToDismiss(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && isAdded()) {
                    if (userId != null && position < historyDocIds.size()) {
                        String docId = historyDocIds.get(position);
                        db.collection("users").document(userId)
                                .collection("history")
                                .document(docId)
                                .delete();
                        
                        // We rely on the SnapshotListener to update historySongs and historyDocIds
                        // but we can remove locally for immediate feedback if the listener is slow
                        Toast.makeText(requireContext(), "Removed from history", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
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
        if (!isAdded()) return;
        screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
        screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
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

    @Override
    public void onSongClick(Song song, int position) {
        onPlayPauseClick(song, position);
    }

    @Override
    public void onPlayPauseClick(Song song, int position) {
        boolean isNowPlaying = (position != songAdapter.getCurrentlyPlayingPosition() || !songAdapter.isPlaying());
        if (isNowPlaying) {
            exoPlayer.stop();
            if (song.getPreviewUrl() != null) {
                exoPlayer.setMediaItem(MediaItem.fromUri(song.getPreviewUrl()));
                exoPlayer.prepare();
                exoPlayer.play();
                songAdapter.setPlaybackState(position, true);
            }
        } else {
            exoPlayer.pause();
            songAdapter.setPlaybackState(position, false);
        }
    }

    @Override
    public void onNextClick(Song song, int position) {
        if (position < historySongs.size() - 1) {
            onPlayPauseClick(historySongs.get(position + 1), position + 1);
        }
    }

    @Override
    public void onPreviousClick(Song song, int position) {
        if (position > 0) {
            onPlayPauseClick(historySongs.get(position - 1), position - 1);
        }
    }

    @Override
    public void onExternalOpenClick(Song song, int position) {
        if (song.getExternalUrl() != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(song.getExternalUrl()));
            startActivity(intent);
        }
    }

    @Override
    public void onLikeClick(Song song, int position) {
        if (userId == null || song == null || song.getId() == null) return;
        boolean nowLiked = songAdapter != null && songAdapter.isLikedSongId(song.getId());
        if (nowLiked) {
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
        intent.putExtra(Intent.EXTRA_TEXT, song.getTitle() + " - " + song.getExternalUrl());
        startActivity(Intent.createChooser(intent, "Share"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubbleAnimationHandler != null) bubbleAnimationHandler.removeCallbacks(bubbleAnimationRunnable);
        if (historyReg != null) historyReg.remove();
        if (moodScansReg != null) moodScansReg.remove();
        if (likedSongsReg != null) likedSongsReg.remove();
        if (playlistsReg != null) playlistsReg.remove();
        if (exoPlayer != null) exoPlayer.release();
    }

    public static class MoodEntry {
        public String mood;
        public long timestamp;
        public String note;
        public String playlist;
    }
}
