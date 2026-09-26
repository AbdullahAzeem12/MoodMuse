package com.example.moodmuse.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses YouTube contentDetails.duration (ISO 8601, e.g. PT1H2M3S) and formats second counts for UI.
 */
public final class DurationUtils {

    private static final Pattern ISO8601 = Pattern.compile(
            "PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?",
            Pattern.CASE_INSENSITIVE
    );

    private DurationUtils() {
    }

    /**
     * Parses YouTube ISO 8601 duration to total seconds, or -1 if invalid.
     */
    public static int parseIso8601DurationSeconds(@Nullable String iso) {
        if (TextUtils.isEmpty(iso)) return -1;
        Matcher m = ISO8601.matcher(iso.trim());
        if (!m.matches()) return -1;
        int hours = parseGroup(m, 1);
        int minutes = parseGroup(m, 2);
        int seconds = parseGroup(m, 3);
        return hours * 3600 + minutes * 60 + seconds;
    }

    private static int parseGroup(Matcher m, int group) {
        String g = m.group(group);
        if (g == null || g.isEmpty()) return 0;
        try {
            return Integer.parseInt(g);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Formats duration for list rows: Jamendo stores seconds as decimal string; YouTube after enrichment too.
     */
    @NonNull
    public static String formatSongDuration(@Nullable String durationSecondsOrEmpty) {
        if (TextUtils.isEmpty(durationSecondsOrEmpty)) {
            return "—";
        }
        try {
            int sec = Integer.parseInt(durationSecondsOrEmpty.trim());
            if (sec < 0) return "—";
            int h = sec / 3600;
            int m = (sec % 3600) / 60;
            int s = sec % 60;
            if (h > 0) {
                return String.format("%d:%02d:%02d", h, m, s);
            }
            return String.format("%d:%02d", m, s);
        } catch (NumberFormatException e) {
            return "—";
        }
    }

    @NonNull
    public static String secondsToStorageString(int totalSeconds) {
        if (totalSeconds < 0) return "";
        return String.valueOf(totalSeconds);
    }
}
