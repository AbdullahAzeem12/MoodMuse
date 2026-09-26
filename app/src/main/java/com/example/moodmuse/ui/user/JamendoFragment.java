package com.example.moodmuse.ui.user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.moodmuse.R;
import com.example.moodmuse.adapters.SongWithControlsAdapter;
import com.example.moodmuse.api.jamendo.JamendoRepository;
import com.example.moodmuse.database.AppDatabase;
import com.example.moodmuse.database.HistoryEntry;
import com.example.moodmuse.models.Song;
import com.example.moodmuse.storage.LikedSongsStore;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JamendoFragment extends Fragment {
    private static final String ARG_EMOTION = "emotion";
    private static final int PAGE_SIZE = 50;

    private String currentEmotion = "Happy";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private View loadingState;
    private TextView loadingText;
    private MaterialCardView emptyState;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private View retryButton;

    private SongWithControlsAdapter songAdapter;
    private final List<Song> songs = new ArrayList<>();

    private JamendoRepository repository;

    private ExoPlayer player;
    private int currentlyPlayingPosition = -1;

    public static JamendoFragment newInstance(String emotion) {
        JamendoFragment fragment = new JamendoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMOTION, emotion);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentEmotion = getArguments().getString(ARG_EMOTION, "Happy");
        }
        repository = JamendoRepository.getInstance(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_jamendo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout = view.findViewById(R.id.jamendo_swipe_refresh);
        recyclerView = view.findViewById(R.id.jamendo_recycler_view);
        loadingState = view.findViewById(R.id.jamendo_loading_state);
        loadingText = view.findViewById(R.id.jamendo_loading_text);
        emptyState = view.findViewById(R.id.jamendo_empty_state);
        emptyTitle = view.findViewById(R.id.jamendo_empty_title);
        emptyMessage = view.findViewById(R.id.jamendo_empty_message);
        retryButton = view.findViewById(R.id.jamendo_retry_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        songAdapter = new SongWithControlsAdapter(songs, new SongWithControlsAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(Song song, int position) {
                togglePlayback(song, position);
            }

            @Override
            public void onPlayPauseClick(Song song, int position) {
                togglePlayback(song, position);
            }

            @Override
            public void onNextClick(Song song, int position) {
                playAt(position + 1);
            }

            @Override
            public void onPreviousClick(Song song, int position) {
                playAt(position - 1);
            }

            @Override
            public void onExternalOpenClick(Song song, int position) {
                openExternal(song);
            }

            @Override
            public void onShareClick(Song song, int position) {
                shareSong(song);
            }

            @Override
            public void onLikeClick(Song song, int position) {
                if (song == null) return;
                boolean added = LikedSongsStore.add(requireContext(), song);
                Toast.makeText(getContext(), added ? "Added to Liked Songs" : "Already in Liked Songs", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddToPlaylistClick(Song song, int position) {
                showAddToPlaylistDialog(song);
            }
        }, getLifecycle());
        songAdapter.setMood(currentEmotion != null ? currentEmotion.toLowerCase(Locale.ROOT) : "neutral");
        recyclerView.setAdapter(songAdapter);

        swipeRefreshLayout.setOnRefreshListener(() -> requestTracks(true, true));
        retryButton.setOnClickListener(v -> requestTracks(true, true));

        initPlayer();
        List<Song> cached = repository.getCached(currentEmotion);
        if (cached != null && !cached.isEmpty()) {
            songs.clear();
            songs.addAll(cached);
            hideStates();
            songAdapter.updateSongs(songs);
        } else {
            requestTracks(true, false);
        }
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(requireContext()).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (!isAdded()) return;
                if (currentlyPlayingPosition != -1) {
                    songAdapter.setPlaybackState(currentlyPlayingPosition, isPlaying);
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (!isAdded()) return;
                if (playbackState == Player.STATE_ENDED) {
                    playAt(currentlyPlayingPosition + 1);
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Playback error", Toast.LENGTH_SHORT).show();
                int next = currentlyPlayingPosition + 1;
                songAdapter.clearPlaybackState();
                currentlyPlayingPosition = -1;
                playAt(next);
            }
        });
    }

    private void requestTracks(boolean reset, boolean randomized) {
        if (reset) {
            songs.clear();
            songAdapter.clearPlaybackState();
            currentlyPlayingPosition = -1;
            if (player != null) player.stop();
        }

        showLoading();
        JamendoRepository.TracksCallback callback = new JamendoRepository.TracksCallback() {
            @Override
            public void onSuccess(@NonNull List<Song> fetched) {
                if (!isAdded()) return;
                swipeRefreshLayout.setRefreshing(false);
                if (fetched.isEmpty()) {
                    showEmpty("No Tracks Found", "Try a different mood or refresh.", true);
                    return;
                }
                songs.clear();
                songs.addAll(fetched);
                hideStates();
                songAdapter.updateSongs(songs);
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                swipeRefreshLayout.setRefreshing(false);
                showEmpty("Network Error", "Unable to reach Jamendo. Please retry.", true);
            }
        };

        if (randomized) {
            repository.requestTracksRandom(currentEmotion, PAGE_SIZE, callback);
        } else {
            repository.requestTracks(currentEmotion, PAGE_SIZE, callback);
        }
    }

    private void togglePlayback(Song song, int position) {
        if (song == null || !song.hasPreview()) {
            Toast.makeText(getContext(), "No preview available", Toast.LENGTH_SHORT).show();
            return;
        }
        if (position == currentlyPlayingPosition) {
            if (player.isPlaying()) player.pause();
            else player.play();
            return;
        }
        playSong(song, position);
    }

    private void playAt(int position) {
        if (songs.isEmpty()) return;
        int size = songs.size();
        int start = position;
        int p = start;
        for (int i = 0; i < size; i++) {
            if (p < 0) p = size - 1;
            if (p >= size) p = 0;
            Song s = songs.get(p);
            if (s != null && s.hasPreview()) {
                playSong(s, p);
                return;
            }
            p++;
        }
        songAdapter.clearPlaybackState();
        currentlyPlayingPosition = -1;
    }

    private void playSong(Song song, int position) {
        String url = song != null ? song.getPreviewUrl() : null;
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(getContext(), "No preview available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to history
        saveToHistory(song);

        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Invalid preview URL", Toast.LENGTH_SHORT).show();
            return;
        }
        currentlyPlayingPosition = position;
        MediaItem item = MediaItem.fromUri(uri);
        player.setMediaItem(item, true);
        player.prepare();
        player.play();
        songAdapter.setPlaybackState(position, true);
    }

    private void saveToHistory(Song song) {
        new Thread(() -> {
            HistoryEntry entry = new HistoryEntry(
                    song.getId(),
                    song.getTitle(),
                    song.getArtist(),
                    song.getImageUrl(),
                    song.getPreviewUrl(),
                    song.getExternalUrl(),
                    "jamendo",
                    System.currentTimeMillis(),
                    currentEmotion
            );
            AppDatabase.getDatabase(requireContext()).historyDao().insert(entry);
        }).start();
    }

    private void openExternal(Song song) {
        if (song == null || !song.hasExternalUrl()) {
            Toast.makeText(getContext(), "No link available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(song.getExternalUrl()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddToPlaylistDialog(Song song) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        db.collection("users").document(userId).collection("library")
                .whereEqualTo("type", "Playlist")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> playlistNames = new ArrayList<>();
                    List<String> playlistIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        playlistNames.add(doc.getString("name"));
                        playlistIds.add(doc.getId());
                    }

                    if (playlistNames.isEmpty()) {
                        Toast.makeText(getContext(), "No playlists found. Create one in Library first!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Add to Playlist")
                            .setItems(playlistNames.toArray(new String[0]), (dialog, which) -> {
                                String selectedId = playlistIds.get(which);
                                addSongToFirestorePlaylist(selectedId, song);
                            })
                            .show();
                });
    }

    private void addSongToFirestorePlaylist(String playlistId, Song song) {
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection("library").document(playlistId)
                .collection("songs").document(song.getId())
                .set(song)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Added to playlist", Toast.LENGTH_SHORT).show());
    }

    private void shareSong(Song song) {
        if (song == null) return;
        String title = song.getTitle() != null ? song.getTitle() : "Song";
        String artist = song.getArtist() != null ? song.getArtist() : "";
        String link = song.getExternalUrl() != null ? song.getExternalUrl() : "";
        String text = artist.isEmpty() ? title : (title + " • " + artist);
        if (!link.isEmpty()) text = text + "\n" + link;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void showLoading() {
        loadingText.setText("Fetching tracks...");
        loadingState.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideStates() {
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmpty(String title, String message, boolean showRetry) {
        loadingState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        emptyTitle.setText(title);
        emptyMessage.setText(message);
        retryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
