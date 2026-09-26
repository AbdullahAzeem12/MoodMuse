package com.example.moodmuse.api.jamendo;

import androidx.annotation.NonNull;

import com.example.moodmuse.ml.EmotionRecognitionModel;

import java.util.Locale;

public final class JamendoMoodTagMapper {
    private JamendoMoodTagMapper() {
    }

    @NonNull
    public static String toFuzzyTags(String emotion) {
        String e = emotion != null ? emotion.trim().toLowerCase(Locale.ROOT) : "";
        if (e.equals("happiness") || e.equals("joy")) e = EmotionRecognitionModel.EMOTION_HAPPY.toLowerCase(Locale.ROOT);
        if (e.equals("sadness")) e = EmotionRecognitionModel.EMOTION_SAD.toLowerCase(Locale.ROOT);
        if (e.equals("surprise")) e = EmotionRecognitionModel.EMOTION_SURPRISED.toLowerCase(Locale.ROOT);
        if (e.equals("fearful")) e = EmotionRecognitionModel.EMOTION_FEAR.toLowerCase(Locale.ROOT);
        if (e.equals("anger")) e = EmotionRecognitionModel.EMOTION_ANGRY.toLowerCase(Locale.ROOT);
        if (e.equals("disgusted")) e = EmotionRecognitionModel.EMOTION_DISGUST.toLowerCase(Locale.ROOT);

        if (e.equals("hollywood")) return "hollywood+soundtrack+pop";
        if (e.equals("bollywood")) return "bollywood+indian+soundtrack";

        if (e.equals(EmotionRecognitionModel.EMOTION_HAPPY.toLowerCase(Locale.ROOT))) return "happy+pop+fun";
        if (e.equals(EmotionRecognitionModel.EMOTION_SAD.toLowerCase(Locale.ROOT))) return "sad+acoustic+melancholic";
        if (e.equals(EmotionRecognitionModel.EMOTION_NEUTRAL.toLowerCase(Locale.ROOT))) return "chill+ambient+downtempo";
        if (e.equals(EmotionRecognitionModel.EMOTION_SURPRISED.toLowerCase(Locale.ROOT))) return "electronic+indie+dance";
        if (e.equals(EmotionRecognitionModel.EMOTION_DISGUST.toLowerCase(Locale.ROOT))) return "dark+experimental+industrial";
        if (e.equals(EmotionRecognitionModel.EMOTION_FEAR.toLowerCase(Locale.ROOT))) return "ambient+dark+soundtrack";
        if (e.equals(EmotionRecognitionModel.EMOTION_ANGRY.toLowerCase(Locale.ROOT))) return "rock+metal+punk";
        return "chill+pop";
    }
}
