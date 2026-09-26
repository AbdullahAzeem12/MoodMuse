package com.example.moodmuse.ml;

/**
 * MoodProfile - Professional Energy/Valence model mapping for musical and visual alignment.
 */
public class MoodProfile {
    private final double targetValence;
    private final double targetEnergy;

    public MoodProfile(double targetValence, double targetEnergy) {
        this.targetValence = targetValence;
        this.targetEnergy = targetEnergy;
    }

    public double getTargetValence() {
        return targetValence;
    }

    public double getTargetEnergy() {
        return targetEnergy;
    }

    /**
     * Professional mapping of emotions to Valence/Energy coordinates.
     */
    public static MoodProfile getMoodProfile(String emotion) {
        String key = emotion != null ? emotion.trim() : "Neutral";
        switch (key) {
            case "Happy":
            case "Radiant Joy":
                return new MoodProfile(0.8, 0.8);
            case "Sad":
            case "Gentle Reflection":
                return new MoodProfile(0.2, 0.2);
            case "Angry":
            case "Intense Fire":
                return new MoodProfile(0.3, 0.9);
            case "Neutral":
            case "Balanced State":
            case "Balanced Core":
                return new MoodProfile(0.5, 0.5);
            case "Surprised":
            case "Shocking Harmony":
                return new MoodProfile(0.7, 0.9);
            case "Fear":
            case "Restless Spirit":
                return new MoodProfile(0.25, 0.85);
            case "Disgust":
            case "Aversive Flow":
                return new MoodProfile(0.2, 0.6);
            case "Confused":
            case "Enigmatic Mind":
                return new MoodProfile(0.45, 0.65);
            case "Shy":
            case "Soft Retreat":
                return new MoodProfile(0.55, 0.25);
            case "Excited":
            case "Electric Pulse":
                return new MoodProfile(0.9, 0.9);
            case "Calm":
            case "Serene Peace":
                return new MoodProfile(0.7, 0.3);
            case "Focused":
                return new MoodProfile(0.6, 0.6);
            case "Anxious":
            case "Restless Mind":
                return new MoodProfile(0.3, 0.7);
            case "Stressed":
                return new MoodProfile(0.4, 0.8);
            case "Tired":
                return new MoodProfile(0.4, 0.2);
            default:
                return new MoodProfile(0.5, 0.5);
        }
    }
}
