package com.example.moodmuse.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

/**
 * AppConfigRepository - Centralized Firebase operations for app configuration
 * Uses Repository pattern to separate data layer from UI
 * Handles global app config (Maintenance Mode, Auto-Approve) and Admin profile
 */
public class AppConfigRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private ListenerRegistration configListenerRegistration;
    private final MutableLiveData<AppConfig> appConfigLiveData = new MutableLiveData<>();
    private final MutableLiveData<AdminProfile> adminProfileLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();

    public AppConfigRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Observe app_config document in real-time
     */
    public LiveData<AppConfig> getAppConfig() {
        if (configListenerRegistration == null) {
            configListenerRegistration = db.collection("app_config")
                    .document("global")
                    .addSnapshotListener((snap, e) -> {
                        if (snap != null && snap.exists()) {
                            AppConfig config = new AppConfig();
                            config.maintenanceMode = snap.getBoolean("maintenanceMode") != null ? snap.getBoolean("maintenanceMode") : false;
                            config.autoApprove = snap.getBoolean("autoApprove") != null ? snap.getBoolean("autoApprove") : false;
                            config.debugMode = snap.getBoolean("debugMode") != null ? snap.getBoolean("debugMode") : false;
                            config.analyticsEnabled = snap.getBoolean("analyticsEnabled") != null ? snap.getBoolean("analyticsEnabled") : true;
                            appConfigLiveData.postValue(config);
                        } else {
                            // Create default config if it doesn't exist
                            createDefaultConfig();
                        }
                    });
        }
        return appConfigLiveData;
    }

    /**
     * Observe current admin profile
     */
    public LiveData<AdminProfile> getAdminProfile() {
        fetchAdminProfile();
        return adminProfileLiveData;
    }

    /**
     * Fetch current admin profile from Firebase Auth and Firestore
     */
    private void fetchAdminProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        AdminProfile profile = new AdminProfile();
                        profile.name = doc.getString("name");
                        profile.email = doc.getString("email");
                        profile.role = doc.getString("role");
                        if (profile.name == null) profile.name = currentUser.getDisplayName();
                        if (profile.email == null) profile.email = currentUser.getEmail();
                        if (profile.role == null) profile.role = "Admin";
                        adminProfileLiveData.postValue(profile);
                    })
                    .addOnFailureListener(e -> {
                        AdminProfile profile = new AdminProfile();
                        profile.name = currentUser.getDisplayName();
                        profile.email = currentUser.getEmail();
                        profile.role = "Admin";
                        adminProfileLiveData.postValue(profile);
                    });
        }
    }

    /**
     * Update maintenance mode
     */
    public void updateMaintenanceMode(boolean enabled) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("maintenanceMode", enabled);
        
        db.collection("app_config").document("global")
                .update(payload)
                .addOnSuccessListener(aVoid -> operationResult.postValue(true))
                .addOnFailureListener(e -> operationResult.postValue(false));
    }

    /**
     * Update auto-approve setting
     */
    public void updateAutoApprove(boolean enabled) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("autoApprove", enabled);
        
        db.collection("app_config").document("global")
                .update(payload)
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
     * Create default app config if it doesn't exist
     */
    private void createDefaultConfig() {
        Map<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("maintenanceMode", false);
        defaultConfig.put("autoApprove", false);
        defaultConfig.put("debugMode", false);
        defaultConfig.put("analyticsEnabled", true);
        
        db.collection("app_config").document("global")
                .set(defaultConfig)
                .addOnSuccessListener(aVoid -> {
                    // Config created successfully
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    /**
     * Clean up listeners
     */
    public void cleanup() {
        if (configListenerRegistration != null) {
            configListenerRegistration.remove();
            configListenerRegistration = null;
        }
    }

    /**
     * Data model for App Configuration
     */
    public static class AppConfig {
        public boolean maintenanceMode;
        public boolean autoApprove;
        public boolean debugMode;
        public boolean analyticsEnabled;
    }

    /**
     * Data model for Admin Profile
     */
    public static class AdminProfile {
        public String name;
        public String email;
        public String role;
    }
}
