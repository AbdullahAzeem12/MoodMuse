package com.example.moodmuse.stats;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.moodmuse.storage.KeyValueStore;
import com.example.moodmuse.storage.SecureKeyValueStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class UserStatsManager {
    private static final String TAG = "UserStatsManager";
    private static final String PREFIX_DAILY_SIGNIN_FLAG = "stats.daily.has_sign_in.";
    private static final String PREFIX_DAILY_SCAN_COUNT = "stats.daily.scan.count.";
    private static final String PREFIX_DAILY_SCORE_SUM = "stats.daily.score.sum.";
    private static final String PREFIX_STREAK_LOCK = "stats.streak.lock.";
    private static final String KEY_ACTIVE_DATE = "stats.active_date";
    private static final String KEY_DAILY_COUNT = "stats.daily_count";
    private static final String KEY_CURRENT_STREAK = "stats.current_streak";
    private static final String KEY_LAST_EVALUATION_DATE = "stats.last_evaluation_date";
    private static final String KEY_LAST_EVALUATION_TS = "stats.last_evaluation_ts";
    private static final String KEY_LAST_SIGN_IN_TS = "stats.last_sign_in_ts";
    private static final String KEY_LAST_SIGN_IN_METHOD = "stats.last_sign_in_method";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final KeyValueStore store;
    private final ZoneProvider zoneProvider;
    private final Clock clock;
    private final UserProvider userProvider;

    public interface Clock {
        long now();
    }

    public interface ZoneProvider {
        ZoneId get();
    }

    public interface UserProvider {
        String get();
    }

    public UserStatsManager(@NonNull Context context) {
        this(
                new SecureKeyValueStore(context),
                ZoneId::systemDefault,
                System::currentTimeMillis,
                () -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    return user != null ? user.getUid() : "guest";
                }
        );
    }

    public UserStatsManager(@NonNull KeyValueStore store,
                            @NonNull ZoneProvider zoneProvider,
                            @NonNull Clock clock,
                            @NonNull UserProvider userProvider) {
        this.store = store;
        this.zoneProvider = zoneProvider;
        this.clock = clock;
        this.userProvider = userProvider;
    }

    public synchronized void recordEmailSignIn() {
        evaluatePendingDayRollover();
        String today = getCurrentDateKey();
        ensureActiveDay(today);

        // Get current daily entries and increment by 1
        int currentDailyEntries = getInt(KEY_DAILY_COUNT, 0);
        int updatedDailyEntries = currentDailyEntries + 1;
        putInt(KEY_DAILY_COUNT, updatedDailyEntries);
        putBoolean(PREFIX_DAILY_SIGNIN_FLAG + today, true);
        putLong(KEY_LAST_SIGN_IN_TS, clock.now());
        putString(KEY_LAST_SIGN_IN_METHOD, "email_password");

        // Only increment streak on first sign-in of the day when streak is 0
        if (currentDailyEntries == 0 && getInt(KEY_CURRENT_STREAK, 0) == 0) {
            putInt(KEY_CURRENT_STREAK, 1);
        }

        logDebug("Recorded email sign-in. user=" + getScopedUserId()
                + " date=" + today
                + " previousDailyEntries=" + currentDailyEntries
                + " updatedDailyEntries=" + updatedDailyEntries
                + " streak=" + getInt(KEY_CURRENT_STREAK, 0));
    }

    public synchronized void onHomePageLandedAfterSignIn() {
        evaluatePendingDayRollover();
        logDebug("Home page landed. dailyEntries=" + getDailyEntries());
    }

    public synchronized void recordDetectedMood(@NonNull String mood) {
        evaluatePendingDayRollover();
        String today = getCurrentDateKey();
        ensureActiveDay(today);

        int moodValue = getMoodValue(mood);
        int updatedScanCount = getInt(PREFIX_DAILY_SCAN_COUNT + today, 0) + 1;
        int updatedScoreSum = getInt(PREFIX_DAILY_SCORE_SUM + today, 0) + moodValue;
        putInt(PREFIX_DAILY_SCAN_COUNT + today, updatedScanCount);
        putInt(PREFIX_DAILY_SCORE_SUM + today, updatedScoreSum);

        logDebug("Recorded detected mood. user=" + getScopedUserId()
                + " mood=" + mood
                + " scansToday=" + updatedScanCount
                + " averageScore=" + Math.round((float) updatedScoreSum / (float) updatedScanCount));
    }

    public synchronized int getDailyEntries() {
        evaluatePendingDayRollover();
        ensureActiveDay(getCurrentDateKey());
        return getInt(KEY_DAILY_COUNT, 0);
    }

    public synchronized int getCurrentStreak() {
        evaluatePendingDayRollover();
        return getInt(KEY_CURRENT_STREAK, 0);
    }

    public synchronized int getTodayMoodScore() {
        evaluatePendingDayRollover();
        String today = getCurrentDateKey();
        int scanCount = getInt(PREFIX_DAILY_SCAN_COUNT + today, 0);
        if (scanCount <= 0) {
            return 0;
        }
        int sum = getInt(PREFIX_DAILY_SCORE_SUM + today, 0);
        return Math.round((float) sum / (float) scanCount);
    }

    public synchronized UserStatsSnapshot getSnapshot() {
        evaluatePendingDayRollover();
        return new UserStatsSnapshot(
                store.getInt(KEY_DAILY_COUNT, 0),
                store.getInt(KEY_CURRENT_STREAK, 0),
                getTodayMoodScore(),
                getString(KEY_ACTIVE_DATE, getCurrentDateKey()),
                getString(KEY_LAST_EVALUATION_DATE, "")
        );
    }

    public synchronized void clearAllStats() {
        store.clear();
    }

    private void evaluatePendingDayRollover() {
        String today = getCurrentDateKey();
        String activeDate = getString(KEY_ACTIVE_DATE, "");

        if (activeDate.isEmpty()) {
            putString(KEY_ACTIVE_DATE, today);
            putInt(KEY_DAILY_COUNT, 0);
            return;
        }

        if (today.equals(activeDate)) {
            return;
        }

        LocalDate active = LocalDate.parse(activeDate, DATE_FORMATTER);
        LocalDate current = LocalDate.parse(today, DATE_FORMATTER);
        long daysBetween = ChronoUnit.DAYS.between(active, current);

        String evaluationDate = getString(KEY_LAST_EVALUATION_DATE, "");
        if (!activeDate.equals(evaluationDate)) {
            boolean hadEmailSignIn = getBoolean(PREFIX_DAILY_SIGNIN_FLAG + activeDate, false);
            int currentStreak = getInt(KEY_CURRENT_STREAK, 0);
            int newStreak;
            
            if (hadEmailSignIn) {
                // User signed in on active date, maintain or increment streak
                if (daysBetween == 1) {
                    // Consecutive day - increment streak by 1
                    newStreak = currentStreak + 1;
                } else if (daysBetween > 1) {
                    // Streak broken - reset to 1 (since they signed in today)
                    newStreak = 1;
                } else {
                    // Same day - keep current streak
                    newStreak = Math.max(currentStreak, 1);
                }
            } else {
                // No sign-in on active date - streak broken
                newStreak = 0;
            }
            
            putInt(KEY_CURRENT_STREAK, newStreak);
            putString(KEY_LAST_EVALUATION_DATE, activeDate);
            putLong(KEY_LAST_EVALUATION_TS, clock.now());
            putBoolean(PREFIX_STREAK_LOCK + activeDate, true);
            logDebug("Evaluated previous sign-in day. user=" + getScopedUserId()
                    + " date=" + activeDate
                    + " hadEmailSignIn=" + hadEmailSignIn
                    + " daysBetween=" + daysBetween
                    + " currentStreak=" + currentStreak
                    + " newStreak=" + newStreak);
        }

        resetDailyCounter(today);
    }

    private void resetDailyCounter(@NonNull String today) {
        putString(KEY_ACTIVE_DATE, today);
        putInt(KEY_DAILY_COUNT, 0);
        putInt(PREFIX_DAILY_SCAN_COUNT + today, 0);
        putInt(PREFIX_DAILY_SCORE_SUM + today, 0);
        putBoolean(PREFIX_DAILY_SIGNIN_FLAG + today, false);
        logDebug("Daily stats reset for user=" + getScopedUserId() + " date=" + today);
    }

    private void ensureActiveDay(@NonNull String today) {
        String activeDate = getString(KEY_ACTIVE_DATE, "");
        if (!today.equals(activeDate)) {
            resetDailyCounter(today);
        }
    }

    private String getCurrentDateKey() {
        return Instant.ofEpochMilli(clock.now()).atZone(zoneProvider.get()).toLocalDate().format(DATE_FORMATTER);
    }

    private int getMoodValue(String mood) {
        if (mood == null) {
            return 50;
        }
        switch (mood) {
            case "Happy":
                return 100;
            case "Energetic":
                return 95;
            case "Excited":
                return 90;
            case "Calm":
                return 80;
            case "Focused":
                return 75;
            case "Anxious":
                return 40;
            case "Stressed":
                return 30;
            case "Sad":
                return 20;
            case "Tired":
                return 15;
            default:
                return 50;
        }
    }

    private String scopedKey(@NonNull String key) {
        return "user." + getScopedUserId() + "." + key;
    }

    private String getScopedUserId() {
        String rawUserId = userProvider.get();
        if (rawUserId == null || rawUserId.trim().isEmpty()) {
            return "guest";
        }
        return rawUserId.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private int getInt(@NonNull String key, int defaultValue) {
        return store.getInt(scopedKey(key), defaultValue);
    }

    private long getLong(@NonNull String key, long defaultValue) {
        return store.getLong(scopedKey(key), defaultValue);
    }

    private String getString(@NonNull String key, @NonNull String defaultValue) {
        return store.getString(scopedKey(key), defaultValue);
    }

    private boolean getBoolean(@NonNull String key, boolean defaultValue) {
        return store.getBoolean(scopedKey(key), defaultValue);
    }

    private void putInt(@NonNull String key, int value) {
        store.putInt(scopedKey(key), value);
    }

    private void putLong(@NonNull String key, long value) {
        store.putLong(scopedKey(key), value);
    }

    private void putString(@NonNull String key, @NonNull String value) {
        store.putString(scopedKey(key), value);
    }

    private void putBoolean(@NonNull String key, boolean value) {
        store.putBoolean(scopedKey(key), value);
    }

    private void logDebug(@NonNull String message) {
        try {
            Log.d(TAG, message);
        } catch (RuntimeException ignored) {
            // Plain JVM unit tests do not mock android.util.Log.
        }
    }
}
