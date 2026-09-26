package com.example.moodmuse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.moodmuse.storage.AppDataWipeManager;

import org.junit.Test;

public class AppDataWipeManagerTest {

    @Test
    public void clearAppDataRunsEveryCleanupBucket() {
        TrackingDelegate delegate = new TrackingDelegate();
        AppDataWipeManager manager = new AppDataWipeManager();

        AppDataWipeManager.LocalWipeReport report = manager.executeLocalWipe(delegate);

        assertTrue(report.isSuccess());
        assertEquals(7, report.getClearedBuckets());
        assertEquals(1, delegate.secureStoreClears);
        assertEquals(1, delegate.defaultPrefClears);
        assertEquals(1, delegate.databaseClears);
        assertEquals(1, delegate.fileClears);
        assertEquals(1, delegate.cacheClears);
        assertEquals(1, delegate.externalClears);
        assertEquals(1, delegate.signOutCalls);
    }

    private static class TrackingDelegate implements AppDataWipeManager.LocalDataDelegate {
        int secureStoreClears;
        int defaultPrefClears;
        int fileClears;
        int cacheClears;
        int databaseClears;
        int externalClears;
        int signOutCalls;

        @Override
        public void clearSecureStore() {
            secureStoreClears++;
        }

        @Override
        public void clearDefaultPreferences() {
            defaultPrefClears++;
        }

        @Override
        public void clearFiles() {
            fileClears++;
        }

        @Override
        public void clearCaches() {
            cacheClears++;
        }

        @Override
        public void clearDatabases() {
            databaseClears++;
        }

        @Override
        public void clearExternalDirs() {
            externalClears++;
        }

        @Override
        public void signOut() {
            signOutCalls++;
        }
    }
}
