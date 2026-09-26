package com.example.moodmuse.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.moodmuse.ui.admin.EmotionMappingFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EmotionMappingRepository - Centralized Firebase operations for Emotion-Genre mappings
 * Uses Repository pattern to separate data layer from UI
 */
public class EmotionMappingRepository {
    private final FirebaseFirestore db;
    private ListenerRegistration mappingsListenerRegistration;
    private final MutableLiveData<List<EmotionMappingFragment.EmotionMapping>> mappingsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();

    public EmotionMappingRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Observe emotion_mappings collection in real-time
     */
    public LiveData<List<EmotionMappingFragment.EmotionMapping>> getMappings() {
        if (mappingsListenerRegistration == null) {
            mappingsListenerRegistration = db.collection("emotion_mappings")
                    .addSnapshotListener((snap, e) -> {
                        if (snap == null) return;
                        List<EmotionMappingFragment.EmotionMapping> mappings = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                            String emotion = d.getString("emotion");
                            String description = d.getString("description");
                            @SuppressWarnings("unchecked")
                            List<String> genres = (List<String>) d.get("genres");
                            String colorHex = d.getString("colorHex");
                            if (genres == null) genres = new ArrayList<>();
                            
                            EmotionMappingFragment.EmotionMapping m = new EmotionMappingFragment.EmotionMapping(
                                    emotion != null ? emotion : "",
                                    description != null ? description : "",
                                    genres,
                                    colorHex != null ? colorHex : "#00d4ff"
                            );
                            m.id = d.getId();
                            mappings.add(m);
                        }
                        mappingsLiveData.postValue(mappings);
                    });
        }
        return mappingsLiveData;
    }

    /**
     * Add a new emotion mapping to Firestore
     */
    public void addMapping(String emotion, String description, List<String> genres, String colorHex) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("emotion", emotion);
        payload.put("description", description);
        payload.put("genres", genres);
        payload.put("colorHex", colorHex);
        payload.put("createdAt", System.currentTimeMillis());
        
        db.collection("emotion_mappings").add(payload)
                .addOnSuccessListener(documentReference -> operationResult.postValue(true))
                .addOnFailureListener(e -> operationResult.postValue(false));
    }

    /**
     * Update an existing emotion mapping in Firestore
     */
    public void updateMapping(String mappingId, String emotion, String description, List<String> genres) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("emotion", emotion);
        payload.put("description", description);
        payload.put("genres", genres);
        
        db.collection("emotion_mappings").document(mappingId).update(payload)
                .addOnSuccessListener(aVoid -> operationResult.postValue(true))
                .addOnFailureListener(e -> operationResult.postValue(false));
    }

    /**
     * Delete an emotion mapping from Firestore
     */
    public void deleteMapping(String mappingId) {
        db.collection("emotion_mappings").document(mappingId).delete()
                .addOnSuccessListener(aVoid -> operationResult.postValue(true))
                .addOnFailureListener(e -> operationResult.postValue(false));
    }

    /**
     * Observe operation results for success/failure
     */
    public LiveData<Boolean> getOperationResult() {
        return operationResult;
    }

    /**
     * Clean up listeners
     */
    public void cleanup() {
        if (mappingsListenerRegistration != null) {
            mappingsListenerRegistration.remove();
            mappingsListenerRegistration = null;
        }
    }
}
