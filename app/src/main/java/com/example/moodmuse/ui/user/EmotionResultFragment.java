package com.example.moodmuse.ui.user;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.moodmuse.R;
import com.example.moodmuse.ml.EmotionRecognitionModel;
import com.example.moodmuse.ml.MoodProfile;
import com.example.moodmuse.stats.UserStatsManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;

/**
 * EmotionResultFragment - Professional result presentation with high-end aesthetics.
 * Updated to support 7 distinct AI emotions with detailed descriptions and themes.
 */
public class EmotionResultFragment extends Fragment {
    private static final String TAG = "EmotionResult";
    private static final String ARG_EMOTION = "emotion";
    private static final String ARG_IMAGE_PATH = "imagePath";
    private static final String ARG_PROBABILITIES = "probabilities";
    
    private String detectedEmotion;
    private String scanImagePath;
    private float[] probabilities;
    private UserStatsManager statsManager;
    private DynamicThemeCoordinator themeCoordinator;
    private AestheticPresentationEngine presentationEngine;

    // UI Bindings
    private TextView tvEmoji, tvTitle, tvDescription, tvHeader;
    private TextView tvDebugDetails;
    private View emojiCard, emojiGlow, insightCard, btnDiscover, btnTryAgain;
    private View debugCard;
    private ImageView ivScanImage;
    private View[] decorativeViews;
    private CircleData[] circleData;
    private int screenWidth, screenHeight;
    private Handler bubbleHandler;
    private Runnable bubbleRunnable;

    public static EmotionResultFragment newInstance(String emotion) {
        return newInstance(emotion, null);
    }

    public static EmotionResultFragment newInstance(String emotion, String imagePath) {
        return newInstance(emotion, imagePath, null);
    }

    public static EmotionResultFragment newInstance(String emotion, String imagePath, float[] probabilities) {
        EmotionResultFragment fragment = new EmotionResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMOTION, emotion != null ? emotion : "Neutral");
        if (imagePath != null) args.putString(ARG_IMAGE_PATH, imagePath);
        if (probabilities != null) args.putFloatArray(ARG_PROBABILITIES, probabilities);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            detectedEmotion = getArguments().getString(ARG_EMOTION);
            scanImagePath = getArguments().getString(ARG_IMAGE_PATH);
            probabilities = getArguments().getFloatArray(ARG_PROBABILITIES);
        }
        if (detectedEmotion != null) detectedEmotion = EmotionRecognitionModel.canonicalizeEmotion(detectedEmotion.trim());
        statsManager = new UserStatsManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_emotion_result, container, false);
        initializeUI(view);
        setupInteractionLogic();
        return view;
    }

    private void initializeUI(View v) {
        tvEmoji = v.findViewById(R.id.emotion_emoji);
        tvTitle = v.findViewById(R.id.emotion_title);
        tvDescription = v.findViewById(R.id.emotion_description);
        tvHeader = v.findViewById(R.id.result_header);
        ivScanImage = v.findViewById(R.id.scan_image);
        tvDebugDetails = v.findViewById(R.id.debug_details);
        debugCard = v.findViewById(R.id.debug_card);
        
        emojiCard = v.findViewById(R.id.emotion_emoji_card);
        emojiGlow = v.findViewById(R.id.emoji_glow_ring);
        insightCard = v.findViewById(R.id.insight_card);
        btnDiscover = v.findViewById(R.id.btn_discover_music);
        btnTryAgain = v.findViewById(R.id.btn_try_again);

        decorativeViews = new View[]{
            v.findViewById(R.id.decorCircle1), v.findViewById(R.id.decorCircle2),
            v.findViewById(R.id.decorCircle3), v.findViewById(R.id.decorCircle4),
            v.findViewById(R.id.decorCircle5), v.findViewById(R.id.decorCircle6),
            v.findViewById(R.id.decorCircle7), v.findViewById(R.id.decorCircle8),
            v.findViewById(R.id.decorCircle9), v.findViewById(R.id.decorCircle10),
            v.findViewById(R.id.decorCircle11)
        };

        themeCoordinator = new DynamicThemeCoordinator();
        presentationEngine = new AestheticPresentationEngine();
        
        applyEmotionData();
    }

    private void applyEmotionData() {
        Map<String, String> data = EmotionDataMap.get(detectedEmotion);
        tvTitle.setText(data.get("title"));
        tvEmoji.setText(data.get("emoji"));
        tvDescription.setText(data.get("description"));

        if (ivScanImage != null) {
            if (scanImagePath != null && new File(scanImagePath).exists()) {
                ivScanImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(new File(scanImagePath)).into(ivScanImage);
            } else {
                ivScanImage.setVisibility(View.GONE);
            }
        }

        if (debugCard != null && tvDebugDetails != null) {
            if (probabilities != null && probabilities.length > 0) {
                debugCard.setVisibility(View.VISIBLE);
                tvDebugDetails.setText(buildDebugDetails(probabilities));
            } else {
                debugCard.setVisibility(View.GONE);
            }
        }

        String topEmotion = detectedEmotion;
        if (probabilities != null && probabilities.length > 0) {
            List<String> labels = loadLabels();
            int bestIdx = 0;
            float best = probabilities[0];
            for (int i = 1; i < probabilities.length; i++) {
                if (probabilities[i] > best) {
                    best = probabilities[i];
                    bestIdx = i;
                }
            }
            if (bestIdx >= 0 && bestIdx < labels.size()) {
                topEmotion = EmotionRecognitionModel.canonicalizeEmotion(labels.get(bestIdx));
            }
        }
        EmotionRecognitionModel.saveLastInferredEmotion(requireContext(), topEmotion, System.currentTimeMillis());
        
        // Log MoodProfile for verification
        MoodProfile profile = MoodProfile.getMoodProfile(detectedEmotion);
        Log.d(TAG, "Displaying Emotion Result: " + detectedEmotion + " | Profile: V=" + profile.getTargetValence() + " E=" + profile.getTargetEnergy());

        themeCoordinator.applyTheme(detectedEmotion);
    }

    private String buildDebugDetails(float[] probs) {
        List<String> labels = loadLabels();
        Map<String, Float> byEmotion = new HashMap<>();
        float sum = 0f;
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            String emotion = EmotionRecognitionModel.canonicalizeEmotion(label);
            float v = probs != null && i < probs.length ? probs[i] : 0f;
            if (v < 0f) v = 0f;
            byEmotion.put(emotion, v);
            sum += v;
        }
        if (sum <= 0f) sum = 1f;

        String[] order = new String[]{
                EmotionRecognitionModel.EMOTION_HAPPY,
                EmotionRecognitionModel.EMOTION_SURPRISED,
                EmotionRecognitionModel.EMOTION_ANGRY,
                EmotionRecognitionModel.EMOTION_SAD,
                EmotionRecognitionModel.EMOTION_FEAR,
                EmotionRecognitionModel.EMOTION_DISGUST,
                EmotionRecognitionModel.EMOTION_NEUTRAL
        };

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.length; i++) {
            String emotion = order[i];
            float v = byEmotion.containsKey(emotion) ? byEmotion.get(emotion) : 0f;
            float pct = (v / sum) * 100f;
            sb.append(emotion)
                    .append(" (")
                    .append(cueForEmotion(emotion))
                    .append(") - ")
                    .append(String.format(Locale.getDefault(), "%.2f%%", pct));
            if (i < order.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private static String cueForEmotion(String emotion) {
        String k = emotion != null ? emotion.trim().toLowerCase() : "";
        switch (k) {
            case "happy":
                return "teeth/smile";
            case "surprised":
                return "open eyes/mouth";
            case "angry":
                return "shrunk eyes";
            case "sad":
                return "watery/frown";
            case "fear":
                return "shrunk eyes/open mouth";
            case "disgust":
                return "squint/nose wrinkle";
            case "neutral":
                return "neutral";
            default:
                return "";
        }
    }

    private List<String> loadLabels() {
        List<String> out = new ArrayList<>();
        try {
            InputStream is = requireContext().getAssets().open("labels.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                if (s.isEmpty()) continue;
                if (s.matches("^\\d+\\s+.+")) s = s.replaceFirst("^\\d+\\s+", "").trim();
                out.add(s);
            }
            br.close();
        } catch (Exception ignored) {
        }
        return out;
    }

    private void setupInteractionLogic() {
        btnDiscover.setOnClickListener(v -> {
            animateClick(v);
            if (getActivity() instanceof UserMainActivity) {
                ((UserMainActivity) getActivity()).navigateToMusicRecommendations(detectedEmotion);
            }
        });

        btnTryAgain.setOnClickListener(v -> {
            animateClick(v);
            if (getActivity() instanceof UserMainActivity) {
                ((UserMainActivity) getActivity()).navigateToHome();
            }
        });
    }

    private void animateClick(View v) {
        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).withEndAction(() -> 
            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        ).start();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presentationEngine.startEntranceAnimations();
        setupFloatingAnimations();
    }

    // --- PROFESSIONAL ARCHITECTURE (EXTENDED LOGIC) ---

    private class AestheticPresentationEngine {
        public void startEntranceAnimations() {
            View[] sequence = {tvHeader, emojiCard, tvTitle, insightCard, btnDiscover, btnTryAgain};
            for (int i = 0; i < sequence.length; i++) {
                if (sequence[i] == null) continue;
                sequence[i].setAlpha(0f);
                sequence[i].setTranslationY(60f);
                sequence[i].animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(800)
                        .setStartDelay(100 + (i * 120))
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
            startGlowPulse();
        }

        private void startGlowPulse() {
            if (emojiGlow == null) return;
            ScaleAnimation pulse = new ScaleAnimation(0.9f, 1.3f, 0.9f, 1.3f, 
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            pulse.setDuration(2500);
            pulse.setRepeatCount(Animation.INFINITE);
            pulse.setRepeatMode(Animation.REVERSE);
            emojiGlow.startAnimation(pulse);
        }
    }

    private class DynamicThemeCoordinator {
        public void applyTheme(String emotion) {
            int themeColor = getThemeColorForEmotion(emotion);
            if (tvTitle != null) tvTitle.setShadowLayer(20, 0, 0, ContextCompat.getColor(requireContext(), themeColor));
            if (emojiGlow != null) emojiGlow.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), themeColor));
            if (emojiCard != null) ((com.google.android.material.card.MaterialCardView)emojiCard).setStrokeColor(ContextCompat.getColor(requireContext(), themeColor));
        }

        private int getThemeColorForEmotion(String emotion) {
            switch (emotion != null ? emotion.trim().toLowerCase() : "neutral") {
                case "happy": return R.color.neon_yellow;
                case "sad": return R.color.material_indigo;
                case "surprised": return R.color.neon_green;
                case "fear": return R.color.neon_purple;
                case "angry": return R.color.material_red;
                case "disgust": return R.color.material_brown;
                case "neutral": return R.color.neon_blue;
                default: return R.color.neon_blue;
            }
        }
    }

    private static class EmotionDataMap {
        public static Map<String, String> get(String emotion) {
            Map<String, String> data = new HashMap<>();
            switch (emotion != null ? emotion.trim().toLowerCase() : "neutral") {
                case "happy":
                    data.put("title", "Radiant Joy");
                    data.put("emoji", "\uD83D\uDE0A");
                    data.put("description", "Your energy is vibrant and light! Let's match this glow with some uplifting melodies.");
                    break;
                case "sad":
                    data.put("title", "Gentle Reflection");
                    data.put("emoji", "\uD83D\uDE14");
                    data.put("description", "It's a quiet moment. Allow soft, soulful music to accompany your thoughts and bring comfort.");
                    break;
                case "surprised":
                    data.put("title", "Shocking Harmony");
                    data.put("emoji", "\uD83D\uDE32");
                    data.put("description", "You're clearly taken aback! Let's explore some unpredictable, exciting sounds to match your vibe.");
                    break;
                case "fear":
                    data.put("title", "Restless Spirit");
                    data.put("emoji", "\uD83D\uDE28");
                    data.put("description", "Sensing a surge of adrenaline. Let's find some grounding, rhythmic tracks to bring you back to balance.");
                    break;
                case "angry":
                    data.put("title", "Intense Fire");
                    data.put("emoji", "\uD83D\uDE21");
                    data.put("description", "Strong internal tension detected. Let's channel this raw energy into powerful, driving rhythms.");
                    break;
                case "disgust":
                    data.put("title", "Aversive Flow");
                    data.put("emoji", "\uD83D\uDE12");
                    data.put("description", "Sensing some dissatisfaction. Let's switch the mood with something refreshing and completely different.");
                    break;
                case "neutral":
                    data.put("title", "Balanced Core");
                    data.put("emoji", "\uD83D\uDE10");
                    data.put("description", "Stable and centered. A perfect baseline for discovering something new and interesting.");
                    break;
                default:
                    data.put("title", "Emotion Detected");
                    data.put("emoji", "\uD83E\uDDD0");
                    data.put("description", "Our AI has calibrated your mood. Explore the sounds that match your current inner state.");
            }
            return data;
        }
    }

    // --- DECORATIVE ANIMATION ENGINE ---

    private void setupFloatingAnimations() {
        if (getView() == null) return;
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        getView().post(() -> {
            if (!isAdded()) return;
            circleData = new CircleData[decorativeViews.length];
            for (int i = 0; i < decorativeViews.length; i++) {
                if (decorativeViews[i] == null) continue;
                decorativeViews[i].measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                float r = Math.max(decorativeViews[i].getMeasuredWidth(), decorativeViews[i].getMeasuredHeight()) / 2f;
                circleData[i] = new CircleData(decorativeViews[i], (float)(Math.random()*screenWidth), (float)(Math.random()*screenHeight), r);
            }
            startBouncingLoop();
        });
    }

    private void startBouncingLoop() {
        bubbleHandler = new Handler(Looper.getMainLooper());
        bubbleRunnable = new Runnable() {
            @Override public void run() {
                if (!isAdded() || circleData == null) return;
                for (CircleData c : circleData) {
                    if (c == null) continue;
                    c.x += c.vx; c.y += c.vy;
                    if (c.x <= c.radius || c.x >= screenWidth - c.radius) c.vx = -c.vx;
                    if (c.y <= c.radius || c.y >= screenHeight - c.radius) c.vy = -c.vy;
                    c.view.setX(c.x - c.radius); c.view.setY(c.y - c.radius);
                }
                bubbleHandler.postDelayed(this, 16);
            }
        };
        bubbleHandler.post(bubbleRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bubbleHandler != null) bubbleHandler.removeCallbacks(bubbleRunnable);
    }

    private static class CircleData {
        View view; float x, y, vx, vy, radius;
        CircleData(View v, float x, float y, float r) {
            view = v; this.x = x; this.y = y; radius = r;
            vx = (float)(Math.random()*4-2); vy = (float)(Math.random()*4-2);
        }
    }
}
