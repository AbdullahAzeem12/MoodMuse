package com.example.moodmuse.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.moodmuse.ui.admin.ManageUsersFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserRepository - Centralized Firebase operations for User management
 * Uses Repository pattern to separate data layer from UI
 */
public class UserRepository {
    private final FirebaseFirestore db;
    private ListenerRegistration usersListenerRegistration;
    private final MutableLiveData<List<ManageUsersFragment.User>> usersLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Observe users collection in real-time
     */
    public LiveData<List<ManageUsersFragment.User>> getUsers() {
        if (usersListenerRegistration == null) {
            usersListenerRegistration = db.collection("users")
                    .addSnapshotListener((snap, e) -> {
                        if (snap == null) return;
                        List<ManageUsersFragment.User> users = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                            String name = d.getString("name");
                            String email = d.getString("email");
                            String role = d.getString("role");
                            Boolean active = d.getBoolean("isActive");
                            Date joinDate = d.getDate("joinDate");
                            if (joinDate == null) {
                                Long createdAt = d.getLong("createdAt");
                                if (createdAt != null) joinDate = new Date(createdAt);
                            }
                            if (joinDate == null) joinDate = new Date();
                            
                            ManageUsersFragment.User u = new ManageUsersFragment.User(
                                    name != null ? name : "Unknown",
                                    email != null ? email : "",
                                    role != null ? role : "User",
                                    active == null || active,
                                    joinDate
                            );
                            u.id = d.getId();
                            users.add(u);
                        }
                        usersLiveData.postValue(users);
                    });
        }
        return usersLiveData;
    }

    /**
     * Add a new user to Firestore
     */
    public void addUser(String name, String email, String role, boolean isActive) {
        Date joinDate = new Date();
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("email", email);
        payload.put("role", role);
        payload.put("isActive", isActive);
        payload.put("joinDate", joinDate);
        payload.put("createdAt", System.currentTimeMillis());
        
        db.collection("users").add(payload)
                .addOnSuccessListener(documentReference -> operationResult.postValue(true))
                .addOnFailureListener(e -> operationResult.postValue(false));
    }

    /**
     * Update an existing user in Firestore
     */
    public void updateUser(String userId, String name, String email, String role, boolean isActive) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("email", email);
        payload.put("role", role);
        payload.put("isActive", isActive);
        
        db.collection("users").document(userId).update(payload)
                .addOnSuccessListener(aVoid -> operationResult.postValue(true))
                .addOnFailureListener(e -> operationResult.postValue(false));
    }

    /**
     * Delete a user from Firestore
     */
    public void deleteUser(String userId) {
        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> operationResult.postValue(true))
                .addOnFailureListener(e -> operationResult.postValue(false));
    }

    /**
     * Toggle user active status
     */
    public void toggleUserStatus(String userId, boolean isActive) {
        db.collection("users").document(userId).update("isActive", isActive)
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
        if (usersListenerRegistration != null) {
            usersListenerRegistration.remove();
            usersListenerRegistration = null;
        }
    }
}
