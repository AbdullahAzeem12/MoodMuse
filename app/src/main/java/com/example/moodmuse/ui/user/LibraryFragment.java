package com.example.moodmuse.ui.user;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.moodmuse.R;
import com.example.moodmuse.ui.RecyclerViewScrollFadeHelper;
import com.example.moodmuse.adapters.PlaylistAdapter;
import com.example.moodmuse.adapters.SongWithControlsAdapter;
import com.example.moodmuse.api.itunes.ItunesRepository;
import com.example.moodmuse.api.jamendo.JamendoRepository;
import com.example.moodmuse.database.PlaylistEntity;
import com.example.moodmuse.database.SongEntity;
import com.example.moodmuse.models.Song;
import com.example.moodmuse.viewmodel.SharedLibraryViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class LibraryFragment extends Fragment implements SongWithControlsAdapter.OnSongClickListener, PlaylistAdapter.OnPlaylistClickListener, SwipeRefreshLayout.OnRefreshListener {

    private List<Song> likedSongs = new ArrayList<>();
    private List<PlaylistEntity> playlists = new ArrayList<>();
    private final List<Song> fullLibrarySongs = new ArrayList<>();
    private SongWithControlsAdapter songAdapter;
    private PlaylistAdapter playlistAdapter;
    private ExoPlayer exoPlayer;
    private FirebaseFirestore db;
    private String userId;
    private ListenerRegistration likedSongsReg;
    private ListenerRegistration playlistsReg;
    private final Map<String, ListenerRegistration> playlistSongRegs = new HashMap<>();
    private final Set<String> likedSongIds = new HashSet<>();
    private final Map<String, Song> likedSongsById = new LinkedHashMap<>();
    private final Map<String, Song> playlistSongsById = new LinkedHashMap<>();
    private final Map<String, Map<String, Song>> playlistSongsByPlaylist = new HashMap<>();
    private SharedLibraryViewModel libraryViewModel;
    private String currentPlaylistId = null;

    private DatabaseReference rtdbUserRef;
    private ValueEventListener rtdbUserListener;
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View searchBarContainer;
    private EditText etSearchLib;
    private final Handler unifiedSearchHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable unifiedSearchRunnable;
    private int unifiedSearchSeq = 0;
    private final List<Song> unifiedSearchMerged = new ArrayList<>();
    
    private boolean showingPlaylists = false;
    private boolean showingAlbums = false;
    private boolean isGridView = false;

    // Bubble Animation Variables
    private View[] decorativeViews;
    private CircleData[] circleData;
    private Handler bubbleAnimationHandler;
    private Runnable bubbleAnimationRunnable;
    private int screenWidth, screenHeight;
    private final Random random = new Random();

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
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        recyclerView = view.findViewById(R.id.rv_library_items);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.neon_blue);

        // Initialize Bubble Animation
        initializeBubbleAnimation(view);

        updateLayoutManager();
        
        songAdapter = new SongWithControlsAdapter(likedSongs, this, getLifecycle());
        playlistAdapter = new PlaylistAdapter(this);
        
        recyclerView.setAdapter(songAdapter);
        RecyclerViewScrollFadeHelper.attach(recyclerView);

        libraryViewModel = new ViewModelProvider(requireActivity()).get(SharedLibraryViewModel.class);
        libraryViewModel.getLikedSongs().observe(getViewLifecycleOwner(), entities -> {
            likedSongsById.clear();
            likedSongIds.clear();
            if (entities != null) {
                for (SongEntity e : entities) {
                    if (e == null || e.id == null) continue;
                    Song s = e.toSong();
                    likedSongsById.put(e.id, s);
                    likedSongIds.add(e.id);
                }
            }
            songAdapter.setLikedSongIds(likedSongIds);
            rebuildLibrarySongs();
        });

        // Chips for filtering
        Chip chipSongs = view.findViewById(R.id.chip_songs);
        Chip chipPlaylists = view.findViewById(R.id.chip_playlists);
        Chip chipAlbums = view.findViewById(R.id.chip_albums);

        chipSongs.setOnClickListener(v -> {
            animateIcon(v);
            currentPlaylistId = null;
            TextView tvTitle = view.findViewById(R.id.tv_library_title);
            if (tvTitle != null) tvTitle.setText("Your Library");
            
            if (showingPlaylists || showingAlbums) {
                showingPlaylists = false;
                showingAlbums = false;
                if (isAdded() && recyclerView != null) {
                    rebuildLibrarySongs();
                    recyclerView.setAdapter(songAdapter);
                    updateLayoutManager();
                    recyclerView.scheduleLayoutAnimation();
                }
            } else {
                rebuildLibrarySongs();
            }
        });

        chipPlaylists.setOnClickListener(v -> {
            animateIcon(v);
            currentPlaylistId = null;
            TextView tvTitle = view.findViewById(R.id.tv_library_title);
            if (tvTitle != null) tvTitle.setText("Your Library");

            if (!showingPlaylists || showingAlbums) {
                showingPlaylists = true;
                showingAlbums = false;
                if (isAdded() && recyclerView != null) {
                    filterPlaylists("playlist");
                    recyclerView.setAdapter(playlistAdapter);
                    updateLayoutManager();
                    recyclerView.scheduleLayoutAnimation();
                }
            }
        });

        chipAlbums.setOnClickListener(v -> {
            animateIcon(v);
            currentPlaylistId = null;
            TextView tvTitle = view.findViewById(R.id.tv_library_title);
            if (tvTitle != null) tvTitle.setText("Your Library");

            if (!showingPlaylists || !showingAlbums) {
                showingPlaylists = true;
                showingAlbums = true;
                if (isAdded() && recyclerView != null) {
                    filterPlaylists("album");
                    recyclerView.setAdapter(playlistAdapter);
                    updateLayoutManager();
                    recyclerView.scheduleLayoutAnimation();
                }
            }
        });

        // Toggle View Button
        view.findViewById(R.id.btn_toggle_view).setOnClickListener(v -> {
            isGridView = !isGridView;
            ((ImageButton)v).setImageResource(isGridView ? R.drawable.ic_list_view : R.drawable.ic_grid_view);
            updateLayoutManager();
            animateIcon(v);
        });

        // Search UI
        searchBarContainer = view.findViewById(R.id.search_bar_container);
        etSearchLib = view.findViewById(R.id.et_search_lib);
        view.findViewById(R.id.btn_search_lib).setOnClickListener(v -> {
            if (searchBarContainer.getVisibility() == View.VISIBLE) {
                searchBarContainer.setVisibility(View.GONE);
                etSearchLib.setText("");
                applySongFilter("");
            } else {
                searchBarContainer.setVisibility(View.VISIBLE);
                etSearchLib.requestFocus();
            }
            animateIcon(v);
        });
        view.findViewById(R.id.btn_close_search).setOnClickListener(v -> {
            etSearchLib.setText("");
            searchBarContainer.setVisibility(View.GONE);
            applySongFilter("");
        });

        etSearchLib.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    applySongFilter(s.toString());
                } else {
                    applySongFilter("");
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Add to Library button
        view.findViewById(R.id.btn_add_lib).setOnClickListener(v -> {
            animateIcon(v);
            showAddLibraryBottomSheet();
        });

        attachFirestoreListeners();

        // Load user profile
        loadUserProfile(view);

        // Initialize search bar visibility
        searchBarContainer.setVisibility(View.GONE);

        // Add Swipe-to-Dismiss
        setupSwipeToDismiss();

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
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;

                    Set<String> currentIds = new HashSet<>();
                    playlists.clear();
                    snap.getDocuments().forEach(d -> {
                        currentIds.add(d.getId());
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

                    List<String> toRemove = new ArrayList<>();
                    for (String id : playlistSongRegs.keySet()) {
                        if (!currentIds.contains(id)) toRemove.add(id);
                    }
                    for (String id : toRemove) {
                        ListenerRegistration r = playlistSongRegs.remove(id);
                        if (r != null) r.remove();
                        playlistSongsByPlaylist.remove(id);
                    }

                    for (String pid : currentIds) {
                        if (playlistSongRegs.containsKey(pid)) continue;
                        ListenerRegistration r = db.collection("users").document(userId)
                                .collection("playlists").document(pid)
                                .collection("songs")
                                .addSnapshotListener((songsSnap, songsErr) -> {
                                    if (songsSnap == null) return;
                                    Map<String, Song> songMap = new LinkedHashMap<>();
                                    songsSnap.getDocuments().forEach(sd -> {
                                        Song s = sd.toObject(Song.class);
                                        if (s == null) return;
                                        if (s.getId() == null) s.setId(sd.getId());
                                        songMap.put(s.getId(), s);
                                    });
                                    playlistSongsByPlaylist.put(pid, songMap);
                                    rebuildLibrarySongs();
                                });
                        playlistSongRegs.put(pid, r);
                    }

                    if (showingPlaylists) {
                        filterPlaylists(showingAlbums ? "album" : "playlist");
                    }
                });
    }

    private void rebuildLibrarySongs() {
        if (currentPlaylistId != null) {
            refreshPlaylistSongsView();
            return;
        }

        playlistSongsById.clear();
        for (Map<String, Song> perPlaylist : playlistSongsByPlaylist.values()) {
            if (perPlaylist == null) continue;
            for (Song s : perPlaylist.values()) {
                if (s != null && s.getId() != null) playlistSongsById.put(s.getId(), s);
            }
        }

        fullLibrarySongs.clear();
        fullLibrarySongs.addAll(likedSongsById.values());
        for (Song s : playlistSongsById.values()) {
            if (s == null || s.getId() == null) continue;
            if (!likedSongsById.containsKey(s.getId())) fullLibrarySongs.add(s);
        }

        applySongFilter(etSearchLib != null ? etSearchLib.getText().toString() : "");
        
        if (recyclerView != null && !showingPlaylists) {
            recyclerView.scheduleLayoutAnimation();
        }
    }

    private void applySongFilter(String query) {
        if (songAdapter == null) return;

        String q = query != null ? query.trim().toLowerCase() : "";
        if (!q.isEmpty() && showingPlaylists) {
            showingPlaylists = false;
            currentPlaylistId = null;
            if (getView() != null) {
                TextView tvTitle = getView().findViewById(R.id.tv_library_title);
                if (tvTitle != null) tvTitle.setText("Your Library");
            }
            recyclerView.setAdapter(songAdapter);
        }

        if (showingPlaylists) return;

        // Cancel pending remote search
        if (unifiedSearchRunnable != null) {
            unifiedSearchHandler.removeCallbacks(unifiedSearchRunnable);
        }

        likedSongs.clear();
        if (q.isEmpty()) {
            if (currentPlaylistId != null) {
                Map<String, Song> pSongs = playlistSongsByPlaylist.get(currentPlaylistId);
                if (pSongs != null) likedSongs.addAll(pSongs.values());
            } else {
                likedSongs.addAll(fullLibrarySongs);
            }
            songAdapter.setLikedSongIds(likedSongIds);
            songAdapter.updateSongs(new ArrayList<>(likedSongs));
        } else {
            // Local prefix filtering: "starting exactly from query"
            List<Song> source = (currentPlaylistId != null && playlistSongsByPlaylist.containsKey(currentPlaylistId)) 
                    ? new ArrayList<>(playlistSongsByPlaylist.get(currentPlaylistId).values()) 
                    : fullLibrarySongs;

            for (Song s : source) {
                if (s == null) continue;
                String t = s.getTitle() != null ? s.getTitle().toLowerCase() : "";
                if (t.startsWith(q)) {
                    likedSongs.add(s);
                }
            }
            songAdapter.setLikedSongIds(likedSongIds);
            songAdapter.updateSongs(new ArrayList<>(likedSongs));
            
            if (recyclerView != null) {
                recyclerView.scheduleLayoutAnimation();
                recyclerView.post(() -> RecyclerViewScrollFadeHelper.apply(recyclerView));
            }

            // Schedule remote iTunes search for real-time results (only if not inside a playlist)
            if (currentPlaylistId == null) {
                final int seq = ++unifiedSearchSeq;
                unifiedSearchRunnable = () -> performRemoteItunesSearch(q, seq);
                unifiedSearchHandler.postDelayed(unifiedSearchRunnable, 700);
            }
        }
    }

    private void performRemoteItunesSearch(String query, int seq) {
        if (seq != unifiedSearchSeq || !isAdded()) return;

        new ItunesRepository().searchByTerm(query, 25, new ItunesRepository.ItunesSearchCallback() {
            @Override
            public void onSuccess(@NonNull List<Song> remoteSongs) {
                if (seq != unifiedSearchSeq || !isAdded()) return;

                unifiedSearchMerged.clear();
                unifiedSearchMerged.addAll(new ArrayList<>(likedSongs));

                Set<String> existingIds = new HashSet<>();
                for (Song s : likedSongs) {
                    if (s.getId() != null) existingIds.add(s.getId());
                }

                for (Song rs : remoteSongs) {
                    if (rs == null || rs.getTitle() == null) continue;
                    String title = rs.getTitle().toLowerCase();
                    
                    // Prefix matching as requested (starts exactly with)
                    if (title.startsWith(query)) {
                        String rid = rs.getId();
                        if (rid == null || !existingIds.contains(rid)) {
                            unifiedSearchMerged.add(rs);
                        }
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (seq == unifiedSearchSeq && isAdded() && !showingPlaylists) {
                        likedSongs.clear();
                        likedSongs.addAll(unifiedSearchMerged);
                        songAdapter.updateSongs(new ArrayList<>(likedSongs));
                        
                        if (recyclerView != null) {
                            recyclerView.scheduleLayoutAnimation();
                            recyclerView.post(() -> RecyclerViewScrollFadeHelper.apply(recyclerView));
                        }
                        
                        // For the user's request: The results are now in the list.
                        // Clicking any item will trigger the "Ask to play" dialog.
                    }
                });
            }

            @Override
            public void onError(@NonNull Throwable error) {
                // Fail silently for real-time search
            }
        });
    }

    private void updateLayoutManager() {
        if (recyclerView == null || !isAdded()) return;
        
        int horizontalPadding = isGridView ? (int) (12 * requireContext().getResources().getDisplayMetrics().density) : 0;
        int bottomPadding = (int) (100 * requireContext().getResources().getDisplayMetrics().density); // Preserve bottom padding for visibility
        recyclerView.setPadding(horizontalPadding, 0, horizontalPadding, bottomPadding);
        recyclerView.setClipToPadding(false);

        if (isGridView) {
            GridLayoutManager glm = new GridLayoutManager(getContext(), 2);
            recyclerView.setLayoutManager(glm);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        
        if (songAdapter != null) songAdapter.setGridView(isGridView);
        if (playlistAdapter != null) playlistAdapter.setGridView(isGridView);
        
        // Ensure adapter is re-attached if layout manager changed significantly
        RecyclerView.Adapter<?> currentAdapter = recyclerView.getAdapter();
        if (currentAdapter != null) {
            recyclerView.setAdapter(currentAdapter);
        }
    }

    private void animateIcon(View view) {
        view.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                .start();
    }

    private void filterPlaylists(String type) {
        List<PlaylistEntity> filtered = new ArrayList<>();
        for (PlaylistEntity p : playlists) {
            if (type.equals(p.type)) filtered.add(p);
        }
        playlistAdapter.updatePlaylists(filtered);
        if (recyclerView != null) {
            recyclerView.scheduleLayoutAnimation();
        }
    }

    @Override
    public void onRefresh() {
        attachFirestoreListeners();
        if (showingPlaylists) {
            filterPlaylists(showingAlbums ? "album" : "playlist");
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Library Updated", Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    private void showAddLibraryBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_add_library_bottom_sheet, null);
        
        sheetView.findViewById(R.id.ll_create_playlist).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showCreatePlaylistDialog();
        });
        
        sheetView.findViewById(R.id.ll_create_album).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showCreateAlbumDialog();
        });

        sheetView.findViewById(R.id.ll_other_actions).setOnClickListener(v -> {
            bottomSheet.dismiss();
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
            onRefresh();
        });

        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Playlist Name");
        input.setPadding(48, 48, 48, 48);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("New Playlist")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        if (userId != null) {
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("name", name);
                            payload.put("description", "Created in Library");
                            payload.put("coverImageUrl", null);
                            payload.put("type", "playlist");
                            payload.put("createdAt", System.currentTimeMillis());
                            db.collection("users").document(userId)
                                    .collection("playlists")
                                    .add(payload)
                                    .addOnSuccessListener(docRef -> {
                                        Toast.makeText(getContext(), "Playlist created!", Toast.LENGTH_SHORT).show();
                                        // Force UI refresh
                                        showingPlaylists = true;
                                        showingAlbums = false;
                                        recyclerView.setAdapter(playlistAdapter);
                                        filterPlaylists("playlist");
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error creating playlist", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateAlbumDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Album Name");
        input.setPadding(48, 48, 48, 48);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("New Album")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        if (userId != null) {
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("name", name);
                            payload.put("description", "New Album");
                            payload.put("coverImageUrl", null);
                            payload.put("type", "album");
                            payload.put("createdAt", System.currentTimeMillis());
                            db.collection("users").document(userId)
                                    .collection("playlists")
                                    .add(payload)
                                    .addOnSuccessListener(docRef -> {
                                        Toast.makeText(getContext(), "Album created!", Toast.LENGTH_SHORT).show();
                                        // Force UI refresh
                                        showingPlaylists = true;
                                        showingAlbums = true;
                                        recyclerView.setAdapter(playlistAdapter);
                                        filterPlaylists("album");
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error creating album", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadUserProfile(View view) {
        ShapeableImageView ivProfileSmall = view.findViewById(R.id.iv_profile_small);
        TextView tvUserName = view.findViewById(R.id.tv_user_name_lib);

        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        userId = currentUser.getUid();
        rtdbUserRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        rtdbUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                String name = snapshot.child("name").getValue(String.class);
                String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                String profileImageBase64 = snapshot.child("profileImageBase64").getValue(String.class);

                // Load Name
                if (name != null && !name.trim().isEmpty()) {
                    tvUserName.setText(name + "'s Library");
                } else if (currentUser.getDisplayName() != null) {
                    tvUserName.setText(currentUser.getDisplayName() + "'s Library");
                } else {
                    tvUserName.setText("Personal Collection");
                }

                // Load Image - prioritized Base64 then URL
                if (profileImageBase64 != null && !profileImageBase64.trim().isEmpty()) {
                    try {
                        byte[] decoded = Base64.decode(profileImageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        if (bitmap != null) {
                            ivProfileSmall.setImageBitmap(bitmap);
                        }
                    } catch (Exception e) {
                        ivProfileSmall.setImageResource(R.drawable.user);
                    }
                } else if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
                    Glide.with(LibraryFragment.this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.user)
                            .circleCrop()
                            .error(R.drawable.user)
                            .into(ivProfileSmall);
                } else if (currentUser.getPhotoUrl() != null) {
                    Glide.with(LibraryFragment.this)
                            .load(currentUser.getPhotoUrl())
                            .placeholder(R.drawable.user)
                            .circleCrop()
                            .error(R.drawable.user)
                            .into(ivProfileSmall);
                } else {
                    ivProfileSmall.setImageResource(R.drawable.user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Initial fallback or error handling
            }
        };

        rtdbUserRef.addValueEventListener(rtdbUserListener);
    }

    private void initializeBubbleAnimation(View view) {
        if (!isAdded()) return;
        
        decorativeViews = new View[]{
                view.findViewById(R.id.decorCircle1), view.findViewById(R.id.decorCircle2),
                view.findViewById(R.id.decorCircle3), view.findViewById(R.id.decorCircle4),
                view.findViewById(R.id.decorCircle5), view.findViewById(R.id.decorCircle6),
                view.findViewById(R.id.decorCircle7), view.findViewById(R.id.decorCircle8),
                view.findViewById(R.id.decorCircle9), view.findViewById(R.id.decorCircle10),
                view.findViewById(R.id.decorCircle11)
        };

        screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
        screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;

        circleData = new CircleData[decorativeViews.length];
        for (int i = 0; i < decorativeViews.length; i++) {
            if (decorativeViews[i] == null) continue;
            
            float radius = 40 + random.nextInt(60);
            if (decorativeViews[i].getLayoutParams() != null && decorativeViews[i].getLayoutParams().width > 0) {
                radius = decorativeViews[i].getLayoutParams().width / 2f;
            }
            
            circleData[i] = new CircleData(decorativeViews[i], 
                    random.nextInt(screenWidth), 
                    random.nextInt(screenHeight), 
                    radius);
        }

        bubbleAnimationHandler = new Handler(Looper.getMainLooper());
        bubbleAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                updateCirclePositions();
                bubbleAnimationHandler.postDelayed(this, 16);
            }
        };
        bubbleAnimationHandler.post(bubbleAnimationRunnable);
    }

    private void updateCirclePositions() {
        for (int i = 0; i < circleData.length; i++) {
            CircleData data = circleData[i];
            if (data == null || data.view == null) continue;

            // Update position
            data.x += data.vx;
            data.y += data.vy;

            // Elastic Wall Collisions (Bounce)
            if (data.x - data.radius < 0) {
                data.x = data.radius;
                data.vx *= -1;
            } else if (data.x + data.radius > screenWidth) {
                data.x = screenWidth - data.radius;
                data.vx *= -1;
            }

            if (data.y - data.radius < 0) {
                data.y = data.radius;
                data.vy *= -1;
            } else if (data.y + data.radius > screenHeight) {
                data.y = screenHeight - data.radius;
                data.vy *= -1;
            }

            // Optional: Circle-Circle Collision (Basic Repulsion)
            for (int j = i + 1; j < circleData.length; j++) {
                CircleData other = circleData[j];
                if (other == null || other.view == null) continue;

                float dx = other.x - data.x;
                float dy = other.y - data.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float minDistance = data.radius + other.radius;

                if (distance < minDistance) {
                    // Simple elastic collision response (swap velocities or push apart)
                    float tempVx = data.vx;
                    float tempVy = data.vy;
                    data.vx = other.vx;
                    data.vy = other.vy;
                    other.vx = tempVx;
                    other.vy = tempVy;
                    
                    // Push apart to prevent sticking
                    float overlap = minDistance - distance;
                    float nx = dx / distance;
                    float ny = dy / distance;
                    data.x -= nx * overlap / 2f;
                    data.y -= ny * overlap / 2f;
                    other.x += nx * overlap / 2f;
                    other.y += ny * overlap / 2f;
                }
            }

            data.view.setX(data.x - data.radius);
            data.view.setY(data.y - data.radius);
        }
    }

    private static class CircleData {
        View view;
        float x, y, vx, vy, radius;
        CircleData(View v, float x, float y, float r) {
            this.view = v;
            this.x = x;
            this.y = y;
            this.radius = r;
            Random rand = new Random();
            // Faster speed: 2-5 pixels per frame
            float speedScale = 2.5f + rand.nextFloat() * 2.5f;
            float angle = rand.nextFloat() * 2f * (float) Math.PI;
            this.vx = (float) Math.cos(angle) * speedScale;
            this.vy = (float) Math.sin(angle) * speedScale;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bubbleAnimationHandler != null) {
            bubbleAnimationHandler.removeCallbacks(bubbleAnimationRunnable);
        }
        if (likedSongsReg != null) likedSongsReg.remove();
        likedSongsReg = null;
        if (playlistsReg != null) playlistsReg.remove();
        playlistsReg = null;
        for (ListenerRegistration r : playlistSongRegs.values()) {
            if (r != null) r.remove();
        }
        playlistSongRegs.clear();
        if (rtdbUserRef != null && rtdbUserListener != null) {
            rtdbUserRef.removeEventListener(rtdbUserListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (exoPlayer != null) exoPlayer.pause();
    }

    private void setupSwipeToDismiss() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    if (showingPlaylists) {
                        // Handle playlist deletion
                        PlaylistEntity p = playlistAdapter.getPlaylists().get(position);
                        deletePlaylistRemote(p);
                        Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Song song = likedSongs.get(position);
                        removeSongFromLibraryRemote(song);
                        Toast.makeText(getContext(), "Removed from library", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void removeSongFromLibraryRemote(Song song) {
        if (userId == null || song == null || song.getId() == null) return;
        if (libraryViewModel != null) libraryViewModel.removeLikedSong(song.getId());
        WriteBatch batch = db.batch();
        batch.delete(db.collection("users").document(userId)
                .collection("liked_songs").document(song.getId()));
        for (PlaylistEntity p : playlists) {
            if (p == null || p.remoteId == null) continue;
            batch.delete(db.collection("users").document(userId)
                    .collection("playlists").document(p.remoteId)
                    .collection("songs").document(song.getId()));
        }
        batch.commit();
    }

    private void deletePlaylistRemote(PlaylistEntity playlist) {
        if (userId == null || playlist == null || playlist.remoteId == null) return;
        db.collection("users").document(userId)
                .collection("playlists").document(playlist.remoteId)
                .collection("songs")
                .get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
                    snap.getDocuments().forEach(d -> batch.delete(d.getReference()));
                    batch.delete(db.collection("users").document(userId)
                            .collection("playlists").document(playlist.remoteId));
                    batch.commit();
                });
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
        payload.put("emotion", "library");
        db.collection("users").document(userId)
                .collection("history")
                .document(docId)
                .set(payload);
    }

    @Override
    public void onSongClick(Song song, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(song.getTitle())
                .setMessage("Choose an action")
                .setPositiveButton("Playback Now", (dialog, which) -> onPlayPauseClick(song, position))
                .setNeutralButton("Add to Playlist", (dialog, which) -> onAddToPlaylistClick(song, position))
                .show();
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
            } else if (isAdded()) {
                Toast.makeText(getContext(), "No preview available", Toast.LENGTH_SHORT).show();
            }
            saveToHistory(song);
        } else {
            exoPlayer.pause();
            songAdapter.setPlaybackState(position, false);
        }
    }

    @Override
    public void onNextClick(Song song, int position) {
        if (position < likedSongs.size() - 1) {
            onPlayPauseClick(likedSongs.get(position + 1), position + 1);
        }
    }

    @Override
    public void onPreviousClick(Song song, int position) {
        if (position > 0) {
            onPlayPauseClick(likedSongs.get(position - 1), position - 1);
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
            Toast.makeText(getContext(), "Create a playlist first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) names[i] = playlists.get(i).name;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add to Playlist")
                .setItems(names, (dialog, which) -> {
                    PlaylistEntity p = playlists.get(which);
                    if (p.remoteId != null && song != null && song.getId() != null) {
                        db.collection("users").document(userId)
                                .collection("playlists").document(p.remoteId)
                                .collection("songs").document(song.getId())
                                .set(song);
                        Toast.makeText(getContext(), "Added to " + names[which], Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    @Override
    public void onShareClick(Song song, int position) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, song.getTitle() + " - " + song.getExternalUrl());
        startActivity(Intent.createChooser(intent, "Share"));
    }

    @Override
    public void onPlaylistClick(PlaylistEntity playlist) {
        if (playlist == null || playlist.remoteId == null) return;
        
        currentPlaylistId = playlist.remoteId;
        showingPlaylists = false;
        showingAlbums = false;
        
        // Update Title UI
        if (getView() != null) {
            TextView tvTitle = getView().findViewById(R.id.tv_library_title);
            if (tvTitle != null) tvTitle.setText(playlist.name);
        }
        
        recyclerView.setAdapter(songAdapter);
        updateLayoutManager();
        refreshPlaylistSongsView();
        recyclerView.scheduleLayoutAnimation();
    }

    private void refreshPlaylistSongsView() {
        if (currentPlaylistId == null) return;
        Map<String, Song> pSongs = playlistSongsByPlaylist.get(currentPlaylistId);
        likedSongs.clear();
        if (pSongs != null) {
            likedSongs.addAll(pSongs.values());
        }
        songAdapter.setLikedSongIds(likedSongIds);
        songAdapter.updateSongs(new ArrayList<>(likedSongs));
    }

    @Override
    public void onPlaylistLongClick(PlaylistEntity playlist) {
        String[] options = {"Rename", "Delete"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(playlist.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showRenamePlaylistDialog(playlist);
                    else showDeletePlaylistConfirm(playlist);
                }).show();
    }

    @Override
    public void onPlaylistOptionsClick(PlaylistEntity playlist) {
        String[] options = {"Rename", "Delete"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(playlist.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showRenamePlaylistDialog(playlist);
                    else showDeletePlaylistConfirm(playlist);
                }).show();
    }

    @Override
    public void onPlaylistPlayClick(PlaylistEntity playlist) {
        Toast.makeText(getContext(), "Playing " + playlist.name, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPlaylistDeleteClick(PlaylistEntity playlist) {
        showDeletePlaylistConfirm(playlist);
    }

    @Override
    public void onPlaylistShuffleClick(PlaylistEntity playlist) {
        Toast.makeText(getContext(), "Shuffling " + playlist.name, Toast.LENGTH_SHORT).show();
    }

    private void showRenamePlaylistDialog(PlaylistEntity playlist) {
        EditText input = new EditText(requireContext());
        input.setText(playlist.name);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Rename Playlist")
                .setView(input)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        if (userId != null && playlist.remoteId != null) {
                            db.collection("users").document(userId)
                                    .collection("playlists").document(playlist.remoteId)
                                    .update("name", newName);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeletePlaylistConfirm(PlaylistEntity playlist) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Playlist")
                .setMessage("Are you sure you want to delete '" + playlist.name + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deletePlaylistRemote(playlist);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (likedSongsReg != null) likedSongsReg.remove();
        if (playlistsReg != null) playlistsReg.remove();
        for (ListenerRegistration r : playlistSongRegs.values()) {
            if (r != null) r.remove();
        }
        playlistSongRegs.clear();
        if (exoPlayer != null) exoPlayer.release();
    }
}
