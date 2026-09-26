package com.example.moodmuse.stats;

public class UserStatsSnapshot {
    private final int dailyEntries;
    private final int currentStreak;
    private final int todayMoodScore;
    private final String activeDateKey;
    private final String lastEvaluationDateKey;

    public UserStatsSnapshot(int dailyEntries, int currentStreak, int todayMoodScore,
                             String activeDateKey, String lastEvaluationDateKey) {
        this.dailyEntries = dailyEntries;
        this.currentStreak = currentStreak;
        this.todayMoodScore = todayMoodScore;
        this.activeDateKey = activeDateKey;
        this.lastEvaluationDateKey = lastEvaluationDateKey;
    }

    public int getDailyEntries() {
        return dailyEntries;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getTodayMoodScore() {
        return todayMoodScore;
    }

    public String getActiveDateKey() {
        return activeDateKey;
    }

    public String getLastEvaluationDateKey() {
        return lastEvaluationDateKey;
    }
}
