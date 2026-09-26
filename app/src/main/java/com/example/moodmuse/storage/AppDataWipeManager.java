package com.example.moodmuse.storage;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppDataWipeManager {
    private static final String TAG = "AppDataWipeManager";

    public interface Callback {
        void onSuccess();
        void onError(@NonNull String message);
    }

    public interface LocalDataDelegate {
        void clearSecureStore();
        void clearDefaultPreferences();
        void clearFiles();
        void clearCaches();
        void clearDatabases();
        void clearExternalDirs();
        void signOut();
    }

    public static class LocalWipeReport {
        private final boolean success;
        private final int clearedBuckets;

        public LocalWipeReport(boolean success, int clearedBuckets) {
            this.success = success;
            this.clearedBuckets = clearedBuckets;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getClearedBuckets() {
            return clearedBuckets;
        }
    }

    private final Context context;
    private final KeyValueStore secureStore;

    public AppDataWipeManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.secureStore = new SecureKeyValueStore(this.context);
    }

    public AppDataWipeManager() {
        this.context = null;
        this.secureStore = null;
    }

    public void wipeAllUserData(@Nullable FirebaseUser currentUser,
                                @Nullable DatabaseReference usersRootRef,
                                @Nullable FirebaseFirestore firestore,
                                @NonNull Callback callback) {
        List<com.google.android.gms.tasks.Task<?>> remoteTasks = new ArrayList<>();

        if (currentUser != null && usersRootRef != null) {
            DatabaseReference userRef = usersRootRef.child(currentUser.getUid());
            remoteTasks.add(userRef.child("moodHistory").removeValue());
            remoteTasks.add(userRef.child("lastMood").removeValue());
            remoteTasks.add(userRef.child("lastMoodNote").removeValue());
            remoteTasks.add(userRef.child("lastMoodTimestamp").removeValue());
            remoteTasks.add(userRef.child("dailySignInCount").removeValue());
            remoteTasks.add(userRef.child("totalSignInCount").removeValue());
            remoteTasks.add(userRef.child("streakCount").removeValue());
            remoteTasks.add(userRef.child("lastStatsUpdateDate").removeValue());
            remoteTasks.add(userRef.child("signInCount").removeValue());
        }

        if (currentUser != null && firestore != null) {
            remoteTasks.add(firestore.collection("users").document(currentUser.getUid()).delete());
        }

        Tasks.whenAllComplete(remoteTasks).addOnCompleteListener(task -> {
            try {
                LocalWipeReport report = executeLocalWipe(new AndroidLocalDataDelegate(context, secureStore));
                if (!report.isSuccess()) {
                    callback.onError("App data wipe completed with partial cleanup.");
                    return;
                }
                callback.onSuccess();
            } catch (Exception e) {
                logError("App data wipe failed", e);
                callback.onError("Failed to clear local app data.");
            }
        }).addOnFailureListener(e -> {
            logError("Remote cleanup failed", e);
            callback.onError("Failed to clear cloud-linked mood data.");
        });
    }

    public LocalWipeReport executeLocalWipe(@NonNull LocalDataDelegate delegate) {
        int clearedBuckets = 0;
        delegate.clearSecureStore();
        clearedBuckets++;
        delegate.clearDefaultPreferences();
        clearedBuckets++;
        delegate.clearDatabases();
        clearedBuckets++;
        delegate.clearFiles();
        clearedBuckets++;
        delegate.clearCaches();
        clearedBuckets++;
        delegate.clearExternalDirs();
        clearedBuckets++;
        delegate.signOut();
        clearedBuckets++;
        logDebug("Local wipe finished. buckets=" + clearedBuckets);
        return new LocalWipeReport(true, clearedBuckets);
    }

    private static class AndroidLocalDataDelegate implements LocalDataDelegate {
        private final Context context;
        private final KeyValueStore secureStore;

        private AndroidLocalDataDelegate(@NonNull Context context, @NonNull KeyValueStore secureStore) {
            this.context = context;
            this.secureStore = secureStore;
        }

        @Override
        public void clearSecureStore() {
            secureStore.clear();
            context.getSharedPreferences(SecureKeyValueStore.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        }

        @Override
        public void clearDefaultPreferences() {
            PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();
        }

        @Override
        public void clearFiles() {
            deleteRecursively(context.getFilesDir());
            deleteRecursively(context.getNoBackupFilesDir());
        }

        @Override
        public void clearCaches() {
            deleteRecursively(context.getCacheDir());
            deleteRecursively(context.getCodeCacheDir());
        }

        @Override
        public void clearDatabases() {
            for (String databaseName : context.databaseList()) {
                context.deleteDatabase(databaseName);
            }
        }

        @Override
        public void clearExternalDirs() {
            deleteRecursively(context.getExternalFilesDir(null));
            deleteRecursively(context.getExternalCacheDir());
        }

        @Override
        public void signOut() {
            FirebaseAuth.getInstance().signOut();
        }

        private void deleteRecursively(@Nullable File file) {
            if (file == null || !file.exists()) {
                return;
            }
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursively(child);
                    }
                }
            }
            if (file.exists() && !file.delete()) {
                logWarning("Failed to delete " + file.getAbsolutePath());
            }
        }
    }

    private static void logDebug(@NonNull String message) {
        try {
            Log.d(TAG, message);
        } catch (RuntimeException ignored) {
            // Plain JVM unit tests do not mock android.util.Log.
        }
    }

    private static void logWarning(@NonNull String message) {
        try {
            Log.w(TAG, message);
        } catch (RuntimeException ignored) {
            // Plain JVM unit tests do not mock android.util.Log.
        }
    }

    private static void logError(@NonNull String message, @NonNull Throwable throwable) {
        try {
            Log.e(TAG, message, throwable);
        } catch (RuntimeException ignored) {
            // Plain JVM unit tests do not mock android.util.Log.
        }
    }
}
