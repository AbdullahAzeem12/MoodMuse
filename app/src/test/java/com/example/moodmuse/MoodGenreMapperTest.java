package com.example.moodmuse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.example.moodmuse.api.spotify.MoodGenreMapper;

import org.junit.Test;

public class MoodGenreMapperTest {

    @Test
    public void happyMoodProvidesUsableSearchQueries() {
        MoodGenreMapper.MoodConfig config = MoodGenreMapper.getMoodConfig("happy");

        assertNotNull(config);
        assertNotNull(config.primarySearchQuery);
        assertNotNull(config.fallbackSearchQuery);
        assertFalse(config.primarySearchQuery.trim().isEmpty());
        assertFalse(config.fallbackSearchQuery.trim().isEmpty());
        assertFalse(config.emptyMessage.trim().isEmpty());
    }

    @Test
    public void unknownMoodFallsBackToNeutralQueries() {
        MoodGenreMapper.MoodConfig config = MoodGenreMapper.getMoodConfig("unknown_mood");

        assertNotNull(config);
        assertFalse(config.primarySearchQuery.trim().isEmpty());
        assertFalse(config.fallbackSearchQuery.trim().isEmpty());
        assertFalse(config.seedGenres.trim().isEmpty());
    }
}
