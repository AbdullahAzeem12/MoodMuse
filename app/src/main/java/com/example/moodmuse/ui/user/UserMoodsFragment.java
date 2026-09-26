package com.example.moodmuse.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.moodmuse.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class UserMoodsFragment extends Fragment {

    private String selectedMood = "";
    private MaterialCardView selectedMoodCard;
    private TextInputEditText etNote;
    private MaterialButton btnSave;
    
    private DatabaseReference userRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_moods, container, false);
        
        initializeFirebase();
        setupMoodCards(view);
        
        etNote = view.findViewById(R.id.et_mood_note);
        btnSave = view.findViewById(R.id.btn_save_mood);
        
        btnSave.setOnClickListener(v -> saveMood());
        
        return view;
    }

    private void initializeFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        }
    }

    private void setupMoodCards(View view) {
        int[] cardIds = {
            R.id.card_mood_happy, R.id.card_mood_calm, R.id.card_mood_excited,
            R.id.card_mood_anxious, R.id.card_mood_stressed, R.id.card_mood_sad
        };
        
        String[] moods = {"Happy", "Calm", "Excited", "Anxious", "Stressed", "Sad"};
        
        for (int i = 0; i < cardIds.length; i++) {
            final String mood = moods[i];
            MaterialCardView card = view.findViewById(cardIds[i]);
            card.setOnClickListener(v -> {
                selectMood(card, mood);
            });
        }
    }

    private void selectMood(MaterialCardView card, String mood) {
        // Reset previous selection
        if (selectedMoodCard != null) {
            selectedMoodCard.setStrokeWidth(2); // Original stroke width
            selectedMoodCard.setCardElevation(2);
        }
        
        // Update selection
        selectedMood = mood;
        selectedMoodCard = card;
        selectedMoodCard.setStrokeWidth(8); // Highlight selection
        selectedMoodCard.setCardElevation(12);
        
        Toast.makeText(getContext(), mood + " selected", Toast.LENGTH_SHORT).show();
    }

    private void saveMood() {
        if (selectedMood.isEmpty()) {
            Toast.makeText(getContext(), "Please select a mood first", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
        
        if (userRef != null) {
            long timestamp = System.currentTimeMillis();
            Map<String, Object> moodData = new HashMap<>();
            moodData.put("lastMood", selectedMood);
            moodData.put("lastMoodNote", note);
            moodData.put("lastMoodTimestamp", timestamp);
            
            // Also push to a history list
            String logId = userRef.child("moodHistory").push().getKey();
            if (logId != null) {
                Map<String, Object> historyEntry = new HashMap<>();
                historyEntry.put("mood", selectedMood);
                historyEntry.put("note", note);
                historyEntry.put("timestamp", timestamp);
                userRef.child("moodHistory").child(logId).setValue(historyEntry);
            }

            userRef.updateChildren(moodData).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Mood logged successfully!", Toast.LENGTH_LONG).show();
                if (getActivity() instanceof UserMainActivity) {
                    ((UserMainActivity) getActivity()).navigateToUserHome();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to log mood: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}


