package com.example.moodmuse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.moodmuse.stats.UserStatsManager;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class UserStatsManagerTest {

    @Test
    public void dailyEntriesResetOnHomeLandingAndMidnight() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        MutableClock clock = new MutableClock(ZonedDateTime.of(2025, 5, 6, 10, 0, 0, 0, ZoneId.of("Asia/Karachi")));
        UserStatsManager manager = new UserStatsManager(store, clock::getZoneId, clock::now, () -> "user_a");

        manager.onHomePageLandedAfterSignIn();
        assertEquals(0, manager.getDailyEntries());

        manager.recordEmailSignIn();
        manager.recordEmailSignIn();
        assertEquals(2, manager.getDailyEntries());

        clock.plusDays(1);
        clock.withHour(0);
        manager.onHomePageLandedAfterSignIn();
        assertEquals(0, manager.getDailyEntries());
    }

    @Test
    public void streakUpdatesExactlyOncePerDay() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        MutableClock clock = new MutableClock(ZonedDateTime.of(2025, 5, 6, 8, 0, 0, 0, ZoneId.of("Asia/Karachi")));
        UserStatsManager manager = new UserStatsManager(store, clock::getZoneId, clock::now, () -> "user_a");

        manager.onHomePageLandedAfterSignIn();
        assertEquals(0, manager.getCurrentStreak());

        manager.recordEmailSignIn();
        assertEquals(1, manager.getCurrentStreak());

        manager.recordEmailSignIn();
        assertEquals(1, manager.getCurrentStreak());

        clock.plusDays(1);
        manager.onHomePageLandedAfterSignIn();
        assertEquals(2, manager.getCurrentStreak());

        clock.plusDays(1);
        manager.onHomePageLandedAfterSignIn();
        assertEquals(0, manager.getCurrentStreak());
    }

    @Test
    public void dailyScoreUsesAverageOfDetectedScanMoods() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        MutableClock clock = new MutableClock(ZonedDateTime.of(2025, 3, 29, 23, 30, 0, 0, ZoneId.of("Europe/London")));
        UserStatsManager manager = new UserStatsManager(store, clock::getZoneId, clock::now, () -> "user_a");

        manager.recordDetectedMood("Happy");
        manager.recordDetectedMood("Calm");
        manager.recordDetectedMood("Sad");

        assertEquals(67, manager.getTodayMoodScore());
    }

    @Test
    public void differentUsersKeepSeparateStatistics() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        MutableClock clock = new MutableClock(ZonedDateTime.of(2025, 6, 1, 9, 0, 0, 0, ZoneId.of("Asia/Karachi")));

        UserStatsManager userA = new UserStatsManager(store, clock::getZoneId, clock::now, () -> "user_a");
        UserStatsManager userB = new UserStatsManager(store, clock::getZoneId, clock::now, () -> "user_b");

        userA.recordEmailSignIn();
        userA.recordDetectedMood("Happy");
        userB.recordEmailSignIn();
        userB.recordEmailSignIn();
        userB.recordDetectedMood("Sad");

        assertEquals(1, userA.getDailyEntries());
        assertEquals(2, userB.getDailyEntries());
        assertEquals(100, userA.getTodayMoodScore());
        assertEquals(20, userB.getTodayMoodScore());
        assertTrue(userA.getCurrentStreak() >= 1);
        assertTrue(userB.getCurrentStreak() >= 1);
    }

    @Test
    public void staleLegacyDailyCountDoesNotKeepStartingAtThree() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        store.putInt("stats.daily_count", 3);
        store.putInt("stats.current_streak", 4);
        store.putString("stats.active_date", "2025-06-01");

        MutableClock clock = new MutableClock(ZonedDateTime.of(2025, 6, 1, 9, 0, 0, 0, ZoneId.of("Asia/Karachi")));
        UserStatsManager manager = new UserStatsManager(store, clock::getZoneId, clock::now, () -> "user_a");

        assertEquals(0, manager.getDailyEntries());

        manager.recordEmailSignIn();
        assertEquals(1, manager.getDailyEntries());

        manager.recordEmailSignIn();
        assertEquals(2, manager.getDailyEntries());
    }

    private static class MutableClock {
        private ZonedDateTime dateTime;

        MutableClock(ZonedDateTime dateTime) {
            this.dateTime = dateTime;
        }

        long now() {
            return dateTime.toInstant().toEpochMilli();
        }

        ZoneId getZoneId() {
            return dateTime.getZone();
        }

        void plusDays(long days) {
            dateTime = dateTime.plusDays(days);
        }

        void plusHours(long hours) {
            dateTime = dateTime.plusHours(hours);
        }

        void withHour(int hour) {
            dateTime = dateTime.withHour(hour);
        }

        void setZoneId(ZoneId zoneId) {
            dateTime = dateTime.withZoneSameInstant(zoneId);
        }
    }
}
