package com.example.moodmuse.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmuse.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EmotionMappingFragment - Comprehensive CRUD management for Emotion-to-Genre mappings
 * Features:
 * - RecyclerView list of all emotion-genre mappings
 * - Add new mappings with dialog
 * - Edit existing mappings
 * - Delete with swipe-to-delete
 * - Search/filter functionality
 * - Genre chips for multiple genres per emotion
 * - Visual feedback with animations
 * - Color-coded emotion categories
 */
public class EmotionMappingFragment extends Fragment {

    // UI Components
    private RecyclerView recyclerViewMappings;
    private FloatingActionButton fabAddMapping;
    private EditText etSearch;
    private TextView tvEmptyState, tvMappingCount;
    private View emptyStateLayout;

    // Adapter and Data
    private EmotionMappingAdapter adapter;
    private List<EmotionMapping> mappingsList;
    private List<EmotionMapping> filteredList;

    // Available genres for selection
    private static final String[] AVAILABLE_GENRES = {
        "Pop", "Rock", "Jazz", "Classical", "Electronic", "Hip Hop", 
        "Country", "R&B", "Indie", "Metal", "Blues", "Reggae",
        "Folk", "Soul", "Punk", "Ambient", "House", "Techno"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emotion_mapping, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerView();
        setupSearchFunctionality();
        loadSampleData();
        setupSwipeToDelete();
        animateEntrance();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews(View view) {
        recyclerViewMappings = view.findViewById(R.id.recyclerViewMappings);
        fabAddMapping = view.findViewById(R.id.fabAddMapping);
        etSearch = view.findViewById(R.id.etSearch);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        tvMappingCount = view.findViewById(R.id.tvMappingCount);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        mappingsList = new ArrayList<>();
        filteredList = new ArrayList<>();

        fabAddMapping.setOnClickListener(v -> showAddMappingDialog());
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new EmotionMappingAdapter(filteredList, new EmotionMappingAdapter.OnMappingClickListener() {
            @Override
            public void onEditClick(EmotionMapping mapping, int position) {
                showEditMappingDialog(mapping, position);
            }

            @Override
            public void onDeleteClick(EmotionMapping mapping, int position) {
                showDeleteConfirmation(mapping, position);
            }

            @Override
            public void onItemClick(EmotionMapping mapping) {
                showMappingDetails(mapping);
            }
        });

        recyclerViewMappings.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMappings.setAdapter(adapter);
    }

    /**
     * Setup search/filter functionality
     */
    private void setupSearchFunctionality() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterMappings(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    /**
     * Filter mappings based on search query
     */
    private void filterMappings(String query) {
        filteredList.clear();
        
        if (query.isEmpty()) {
            filteredList.addAll(mappingsList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (EmotionMapping mapping : mappingsList) {
                if (mapping.emotion.toLowerCase().contains(lowerQuery) ||
                    containsGenre(mapping.genres, lowerQuery)) {
                    filteredList.add(mapping);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyState();
        updateMappingCount();
    }

    /**
     * Check if any genre contains the query
     */
    private boolean containsGenre(List<String> genres, String query) {
        for (String genre : genres) {
            if (genre.toLowerCase().contains(query)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Load sample emotion-genre mappings
     */
    private void loadSampleData() {
        mappingsList.add(new EmotionMapping("😊 Happy", "Uplifting moods", 
            Arrays.asList("Pop", "Dance", "Reggae"), "#4ade80"));
        mappingsList.add(new EmotionMapping("😢 Sad", "Melancholic feelings", 
            Arrays.asList("Blues", "Classical", "Soul"), "#3b82f6"));
        mappingsList.add(new EmotionMapping("😌 Calm", "Peaceful and relaxed", 
            Arrays.asList("Ambient", "Jazz", "Classical"), "#00d4ff"));
        mappingsList.add(new EmotionMapping("😰 Anxious", "Tense and worried", 
            Arrays.asList("Electronic", "Ambient", "Lo-fi"), "#fbbf24"));
        mappingsList.add(new EmotionMapping("😡 Angry", "Intense emotions", 
            Arrays.asList("Metal", "Punk", "Hard Rock"), "#ef4444"));
        mappingsList.add(new EmotionMapping("🥰 Romantic", "Love and affection", 
            Arrays.asList("R&B", "Soul", "Jazz"), "#f093fb"));
        mappingsList.add(new EmotionMapping("💪 Energetic", "High energy vibes", 
            Arrays.asList("Hip Hop", "Electronic", "Rock"), "#ff006e"));
        mappingsList.add(new EmotionMapping("😴 Tired", "Low energy, sleepy", 
            Arrays.asList("Ambient", "Lo-fi", "Classical"), "#9ca3af"));

        filteredList.addAll(mappingsList);
        adapter.notifyDataSetChanged();
        updateEmptyState();
        updateMappingCount();
    }

    /**
     * Setup swipe-to-delete functionality
     */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, 
            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                EmotionMapping mapping = filteredList.get(position);
                
                // Remove from both lists
                filteredList.remove(position);
                mappingsList.remove(mapping);
                adapter.notifyItemRemoved(position);
                
                updateEmptyState();
                updateMappingCount();
                
                Toast.makeText(getContext(), 
                    mapping.emotion + " mapping deleted", Toast.LENGTH_SHORT).show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewMappings);
    }

    /**
     * Show dialog to add new mapping
     */
    private void showAddMappingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_emotion_mapping, null);
        
        TextInputEditText etEmotion = dialogView.findViewById(R.id.etEmotion);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
        ChipGroup chipGroupGenres = dialogView.findViewById(R.id.chipGroupGenres);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Populate genre chips
        populateGenreChips(chipGroupGenres, null);

        AlertDialog dialog = builder.setView(dialogView).create();

        btnSave.setOnClickListener(v -> {
            String emotion = etEmotion.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            List<String> selectedGenres = getSelectedGenres(chipGroupGenres);

            if (validateInput(emotion, description, selectedGenres)) {
                EmotionMapping newMapping = new EmotionMapping(emotion, description, 
                    selectedGenres, generateRandomColor());
                
                mappingsList.add(newMapping);
                filteredList.add(newMapping);
                adapter.notifyItemInserted(filteredList.size() - 1);
                
                updateEmptyState();
                updateMappingCount();
                
                Toast.makeText(getContext(), "Mapping added successfully", 
                    Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    /**
     * Show dialog to edit existing mapping
     */
    private void showEditMappingDialog(EmotionMapping mapping, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_emotion_mapping, null);
        
        TextInputEditText etEmotion = dialogView.findViewById(R.id.etEmotion);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
        ChipGroup chipGroupGenres = dialogView.findViewById(R.id.chipGroupGenres);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);

        if (tvTitle != null) tvTitle.setText("Edit Mapping");
        btnSave.setText("Update");

        // Pre-fill data
        etEmotion.setText(mapping.emotion);
        etDescription.setText(mapping.description);
        populateGenreChips(chipGroupGenres, mapping.genres);

        AlertDialog dialog = builder.setView(dialogView).create();

        btnSave.setOnClickListener(v -> {
            String emotion = etEmotion.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            List<String> selectedGenres = getSelectedGenres(chipGroupGenres);

            if (validateInput(emotion, description, selectedGenres)) {
                mapping.emotion = emotion;
                mapping.description = description;
                mapping.genres = selectedGenres;
                
                adapter.notifyItemChanged(position);
                
                Toast.makeText(getContext(), "Mapping updated successfully", 
                    Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    /**
     * Show delete confirmation dialog
     */
    private void showDeleteConfirmation(EmotionMapping mapping, int position) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Mapping")
            .setMessage("Are you sure you want to delete the mapping for \"" + 
                       mapping.emotion + "\"?")
            .setPositiveButton("Delete", (dialog, which) -> {
                filteredList.remove(position);
                mappingsList.remove(mapping);
                adapter.notifyItemRemoved(position);
                
                updateEmptyState();
                updateMappingCount();
                
                Toast.makeText(getContext(), "Mapping deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Show mapping details in a dialog
     */
    private void showMappingDetails(EmotionMapping mapping) {
        StringBuilder genresText = new StringBuilder();
        for (int i = 0; i < mapping.genres.size(); i++) {
            genresText.append(mapping.genres.get(i));
            if (i < mapping.genres.size() - 1) {
                genresText.append(", ");
            }
        }

        new AlertDialog.Builder(requireContext())
            .setTitle(mapping.emotion)
            .setMessage("Description: " + mapping.description + "\n\n" +
                       "Genres: " + genresText.toString())
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Populate genre chips in dialog
     */
    private void populateGenreChips(ChipGroup chipGroup, List<String> selectedGenres) {
        chipGroup.removeAllViews();
        
        for (String genre : AVAILABLE_GENRES) {
            Chip chip = new Chip(requireContext());
            chip.setText(genre);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            
            if (selectedGenres != null && selectedGenres.contains(genre)) {
                chip.setChecked(true);
            }
            
            chipGroup.addView(chip);
        }
    }

    /**
     * Get selected genres from chip group
     */
    private List<String> getSelectedGenres(ChipGroup chipGroup) {
        List<String> selected = new ArrayList<>();
        
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selected.add(chip.getText().toString());
            }
        }
        
        return selected;
    }

    /**
     * Validate input data
     */
    private boolean validateInput(String emotion, String description, List<String> genres) {
        if (emotion.isEmpty()) {
            Toast.makeText(getContext(), "Please enter an emotion", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (description.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a description", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (genres.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one genre", 
                Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }

    /**
     * Generate random color for emotion
     */
    private String generateRandomColor() {
        String[] colors = {"#4ade80", "#3b82f6", "#00d4ff", "#fbbf24", 
                          "#ef4444", "#f093fb", "#ff006e", "#9ca3af"};
        return colors[(int) (Math.random() * colors.length)];
    }

    /**
     * Update empty state visibility
     */
    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewMappings.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewMappings.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Update mapping count display
     */
    private void updateMappingCount() {
        if (tvMappingCount != null) {
            tvMappingCount.setText(filteredList.size() + " mappings");
        }
    }

    /**
     * Animate entrance of UI elements
     */
    private void animateEntrance() {
        if (fabAddMapping != null) {
            fabAddMapping.setScaleX(0f);
            fabAddMapping.setScaleY(0f);
            fabAddMapping.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setStartDelay(200)
                .start();
        }
    }

    /**
     * Data model for Emotion-Genre mapping
     */
    public static class EmotionMapping {
        public String id;
        public String emotion;
        public String description;
        public List<String> genres;
        public String colorHex;

        public EmotionMapping(String emotion, String description, 
                            List<String> genres, String colorHex) {
            this.emotion = emotion;
            this.description = description;
            this.genres = genres;
            this.colorHex = colorHex;
        }
    }
}

