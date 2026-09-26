package com.example.moodmuse.ui.user;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmuse.R;
import com.example.moodmuse.api.itunes.ItunesRepository;
import com.example.moodmuse.api.jamendo.JamendoRepository;
import com.example.moodmuse.models.Song;
import com.example.moodmuse.stats.UserStatsManager;
import com.example.moodmuse.ml.EmotionRecognitionModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * UserHomeFragment - Personalized dashboard for mood-based music discovery
 * Features:
 * - Personalized greeting based on time of day
 * - Current mood display with emoji
 * - Music recommendations based on mood
 * - Mood streak tracking
 * - Quick mood selection
 * - Recent listening history
 * - Mood insights and statistics
 */
public class UserHomeFragment extends Fragment {

    // UI Components
    private TextView tvGreeting, tvUserName, tvCurrentMood, tvMoodStreak, 
                    tvMoodScore, tvTodayEntry, tvWeeklyInsight, tvNoteBanner;
    private Chip chipQuickMood1, chipQuickMood2, chipQuickMood3, chipQuickMood4;
    private RecyclerView rvRecommendations;
    private MaterialButton btnLogMood, btnViewHistory, btnRefreshRecs;
    private LinearProgressIndicator progressMoodScore;
    private CircularProgressIndicator loadingRecommendations;
    private MaterialCardView cardCurrentMood, cardStreak, cardRecommendations, 
                           cardInsights, cardQuickActions, cardNoteBanner;

    // Bouncing Circles
    private View[] decorativeViews;
    private CircleData[] circleData;
    private Handler bubbleAnimationHandler;
    private Runnable bubbleAnimationRunnable;
    private int screenWidth, screenHeight;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private UserStatsManager userStatsManager;
    private EmotionRecognitionModel emotionModel;
    private ExoPlayer exoPlayer;

    // Data
    private MusicRecommendationAdapter adapter;
    private List<MusicRecommendation> recommendations;
    private int currentlyPlayingPosition = -1;
    private String currentMood = "Happy";
    private int moodStreak = 7;
    private int moodScore = 85;
    private Random random;

    // Enhanced Mood Emojis - SYNCED WITH TFLite Model (7 emotions)
    private final String[] MOOD_EMOJIS = {"\uD83D\uDE10", "\uD83D\uDE0A", "\uD83D\uDE32", "\uD83D\uDE14", "\uD83D\uDE21", "\uD83D\uDE12", "\uD83D\uDE28"};
    private final String[] MOODS = {"Neutral", "Happy", "Surprised", "Sad", "Angry", "Disgust", "Fear"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        exoPlayer = new ExoPlayer.Builder(requireContext()).build();
        random = new Random();
        userStatsManager = new UserStatsManager(requireContext());
        emotionModel = new EmotionRecognitionModel(requireContext());
        userStatsManager.onHomePageLandedAfterSignIn();
        initializeViews(view);
        setupGreeting();
        setupCurrentMood();
        applySavedInferredMood(false);
        setupMoodStreak();
        setupQuickMoodChips();
        setupRecommendations();
        setupInsights();
        setupButtons();
        setupFloatingAnimations();
        fetchUserData();
        animateEntrance();
    }

    /**
     * Fetch real user data and manage streak logic
     */
    private void fetchUserData() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                // 1. Fetch and set User Name
                String name = snapshot.child("name").getValue(String.class);
                if (name != null && !name.isEmpty()) {
                    tvUserName.setText(name);
                } else if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    tvUserName.setText(user.getDisplayName());
                } else {
                    tvUserName.setText("MoodMuse User");
                }

                // 2. Fetch Last Mood
                String lastMood = snapshot.child("lastMood").getValue(String.class);
                Long lastMoodTime = snapshot.child("lastMoodTimestamp").getValue(Long.class);
                String lastMoodNote = snapshot.child("lastMoodNote").getValue(String.class);

                String localMood = EmotionRecognitionModel.getLastInferredEmotion(requireContext());
                long localTs = EmotionRecognitionModel.getLastInferredEmotionTimestamp(requireContext());
                boolean useLocal = localMood != null
                        && !localMood.trim().isEmpty()
                        && localTs > 0L
                        && (lastMoodTime == null || localTs > lastMoodTime);

                if (useLocal) {
                    currentMood = localMood;
                    calculateMoodScore(localMood);
                    updateCurrentMoodUI(localMood, localTs);
                    loadRecommendations();
                } else if (lastMood != null && !lastMood.isEmpty()) {
                    currentMood = lastMood;
                    calculateMoodScore(lastMood);
                    updateCurrentMoodUI(lastMood, lastMoodTime);
                    loadRecommendations();
                }

                // 3. Show Note Banner if recent (last 20 seconds)
                if (lastMoodNote != null && !lastMoodNote.isEmpty() && lastMoodTime != null) {
                    long diff = System.currentTimeMillis() - lastMoodTime;
                    if (diff < 20000) { // 20 seconds
                        showNoteBanner(lastMoodNote);
                    }
                }

                // 4. Refresh statistics from the unified sign-in and scan stats engine
                refreshStatsFromManager();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("UserHomeFragment", "Database error: " + error.getMessage());
            }
        });
    }

    private void showNoteBanner(String note) {
        if (cardNoteBanner != null && tvNoteBanner != null) {
            tvNoteBanner.setText(note);
            tvNoteBanner.setSelected(true); // Required for marquee
            cardNoteBanner.setVisibility(View.VISIBLE);
            
            // Hide after 20 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && cardNoteBanner != null) {
                    cardNoteBanner.setVisibility(View.GONE);
                }
            }, 20000);
        }
    }

    private void processMoodScoreFromHistory(DataSnapshot historySnap) {
        if (!historySnap.exists()) {
            updateMoodScoreUI(moodScore); // Fallback to current if no history
            return;
        }

        long totalScore = 0;
        int count = 0;
        
        // Iterate through history (limit to last 10 entries for relevance)
        List<DataSnapshot> entries = new ArrayList<>();
        for (DataSnapshot entry : historySnap.getChildren()) {
            entries.add(entry);
        }
        
        int start = Math.max(0, entries.size() - 10);
        for (int i = entries.size() - 1; i >= start; i--) {
            String mood = entries.get(i).child("mood").getValue(String.class);
            if (mood != null) {
                totalScore += getMoodValue(mood);
                count++;
            }
        }

        if (count > 0) {
            moodScore = (int) (totalScore / count);
            updateMoodScoreUI(moodScore);
        }
    }

    private int getMoodValue(String mood) {
        switch (mood) {
            case "Happy": return 100;
            case "Energetic": return 95;
            case "Excited": return 90;
            case "Calm": return 80;
            case "Focused": return 75;
            case "Anxious": return 40;
            case "Stressed": return 30;
            case "Sad": return 20;
            case "Tired": return 15;
            default: return 50;
        }
    }

    private void updateMoodScoreUI(int score) {
        if (tvMoodScore != null) {
            tvMoodScore.setText(score + "%");
        }
        if (progressMoodScore != null) {
            progressMoodScore.setProgress(score);
        }
    }

    private void calculateMoodScore(String mood) {
        // This is now handled by processMoodScoreFromHistory
        // but we keep it for immediate feedback on chip click
        moodScore = getMoodValue(mood);
        updateMoodScoreUI(moodScore);
    }

    private void updateCurrentMoodUI(String mood, Long timestamp) {
        int moodIndex = Arrays.asList(MOODS).indexOf(mood);
        String emoji = moodIndex >= 0 ? MOOD_EMOJIS[moodIndex] : "😊";
        
        if (tvCurrentMood != null) {
            tvCurrentMood.setText(emoji + " " + mood);
        }
        
        if (tvTodayEntry != null) {
            if (timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d 'at' h:mm a", Locale.getDefault());
                tvTodayEntry.setText("Last logged: " + sdf.format(new Date(timestamp)));
            } else {
                tvTodayEntry.setText("Last logged: Today");
            }
        }
    }

    private void applySavedInferredMood(boolean updateRecommendations) {
        String mood = EmotionRecognitionModel.getLastInferredEmotion(requireContext());
        long ts = EmotionRecognitionModel.getLastInferredEmotionTimestamp(requireContext());
        if (mood == null || mood.trim().isEmpty()) return;
        currentMood = mood;
        calculateMoodScore(mood);
        updateCurrentMoodUI(mood, ts > 0L ? Long.valueOf(ts) : null);
        if (updateRecommendations && recommendations != null && adapter != null) {
            loadRecommendations();
        }
    }

    private void processStreakLogic(DataSnapshot snapshot) {
        refreshStatsFromManager();
    }

    private void refreshStatsFromManager() {
        if (userStatsManager == null) {
            return;
        }
        moodStreak = userStatsManager.getCurrentStreak();
        moodScore = userStatsManager.getTodayMoodScore();
        if (tvTodayEntry != null) {
            int todayCount = userStatsManager.getDailyEntries();
            String scoreLabel = moodScore > 0 ? " | Score from scans: " + moodScore + "%" : "";
            tvTodayEntry.setText("Today sign-ins: " + todayCount + scoreLabel);
        }
        setupMoodStreak();
        setupInsights();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews(View view) {
        // Text Views
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvCurrentMood = view.findViewById(R.id.tvCurrentMood);
        tvMoodStreak = view.findViewById(R.id.tvMoodStreak);
        tvMoodScore = view.findViewById(R.id.tvMoodScore);
        tvTodayEntry = view.findViewById(R.id.tvTodayEntry);
        tvWeeklyInsight = view.findViewById(R.id.tvWeeklyInsight);
        
        // Chips
        chipQuickMood1 = view.findViewById(R.id.chipQuickMood1);
        chipQuickMood2 = view.findViewById(R.id.chipQuickMood2);
        chipQuickMood3 = view.findViewById(R.id.chipQuickMood3);
        chipQuickMood4 = view.findViewById(R.id.chipQuickMood4);
        
        // RecyclerView
        rvRecommendations = view.findViewById(R.id.rvRecommendations);
        
        // Buttons
        btnLogMood = view.findViewById(R.id.btnLogMood);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);
        btnRefreshRecs = view.findViewById(R.id.btnRefreshRecs);
        
        // Progress Indicators
        progressMoodScore = view.findViewById(R.id.progressMoodScore);
        loadingRecommendations = view.findViewById(R.id.loadingRecommendations);
        
        // Cards
        cardCurrentMood = view.findViewById(R.id.cardCurrentMood);
        cardStreak = view.findViewById(R.id.cardStreak);
        cardRecommendations = view.findViewById(R.id.cardRecommendations);
        cardInsights = view.findViewById(R.id.cardInsights);
        cardQuickActions = view.findViewById(R.id.cardQuickActions);
        cardNoteBanner = view.findViewById(R.id.cardNoteBanner);
        tvNoteBanner = view.findViewById(R.id.tvNoteBanner);

        // Decorative Circles
        decorativeViews = new View[]{
                view.findViewById(R.id.decorCircle1),
                view.findViewById(R.id.decorCircle2),
                view.findViewById(R.id.decorCircle3),
                view.findViewById(R.id.decorCircle4),
                view.findViewById(R.id.decorCircle5),
                view.findViewById(R.id.decorCircle6),
                view.findViewById(R.id.decorCircle7),
                view.findViewById(R.id.decorCircle8),
                view.findViewById(R.id.decorCircle9),
                view.findViewById(R.id.decorCircle10),
                view.findViewById(R.id.decorCircle11)
        };
    }

    /**
     * Setup personalized greeting
     */
    private void setupGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else if (hour < 21) {
            greeting = "Good Evening";
        } else {
            greeting = "Good Night";
        }
        
        if (tvGreeting != null) tvGreeting.setText(greeting);
        if (tvUserName != null) tvUserName.setText("Loading...");
    }

    /**
     * Setup current mood display
     */
    private void setupCurrentMood() {
        int moodIndex = Arrays.asList(MOODS).indexOf(currentMood);
        String emoji = moodIndex >= 0 ? MOOD_EMOJIS[moodIndex] : "😊";
        
        if (tvCurrentMood != null) {
            tvCurrentMood.setText(emoji + " " + currentMood);
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        if (tvTodayEntry != null) {
            tvTodayEntry.setText("Last updated: " + sdf.format(Calendar.getInstance().getTime()));
        }
    }

    /**
     * Setup mood streak display
     */
    private void setupMoodStreak() {
        if (tvMoodStreak != null) {
            tvMoodStreak.setText(String.valueOf(moodStreak));
        }
        
        if (tvMoodScore != null) {
            tvMoodScore.setText(moodScore + "%");
        }
        
        if (progressMoodScore != null) {
            progressMoodScore.setProgress(moodScore);
        }
    }

    /**
     * Setup quick mood selection chips with enhanced 9-emotion support
     */
    private void setupQuickMoodChips() {
        // Quick selection of most common emotions from TFLite model
        String[] quickMoods = {"Happy", "Sad", "Neutral", "Surprised", "Angry"};
        Chip[] chips = {chipQuickMood1, chipQuickMood2, chipQuickMood3, chipQuickMood4};
        
        for (int i = 0; i < chips.length && i < quickMoods.length; i++) {
            if (chips[i] != null) {
                final String mood = quickMoods[i];
                final Chip chip = chips[i];
                
                int moodIndex = Arrays.asList(MOODS).indexOf(mood);
                String emoji = moodIndex >= 0 ? MOOD_EMOJIS[moodIndex] : "😊";
                chip.setText(emoji + " " + mood);
                
                chip.setOnClickListener(v -> {
                    currentMood = mood;
                    calculateMoodScore(mood);
                    updateCurrentMoodUI(mood, System.currentTimeMillis());
                    saveMoodToFirebase(mood);
                    loadRecommendations();
                    Toast.makeText(getContext(), "Mood updated to " + mood + " using enhanced AI detection", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void saveMoodToFirebase(String mood) {
        if (userRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMood", mood);
            updates.put("lastMoodTimestamp", System.currentTimeMillis());
            userRef.updateChildren(updates);
        }
    }

    /**
     * Setup music recommendations RecyclerView
     */
    private void setupRecommendations() {
        recommendations = new ArrayList<>();
        adapter = new MusicRecommendationAdapter(recommendations);
        
        if (rvRecommendations != null) {
            rvRecommendations.setLayoutManager(new LinearLayoutManager(getContext()));
            rvRecommendations.setAdapter(adapter);
        }
        
        loadRecommendations();
    }

    /**
     * Load music recommendations based on mood
     */
    private void loadRecommendations() {
        if (loadingRecommendations != null) {
            loadingRecommendations.setVisibility(View.VISIBLE);
        }
        if (rvRecommendations != null) {
            rvRecommendations.setVisibility(View.GONE);
        }

        // Fetch from Jamendo for real tracks
        JamendoRepository.getInstance(requireContext()).requestTracks(currentMood, 10, new JamendoRepository.TracksCallback() {
            @Override
            public void onSuccess(@NonNull List<Song> songs) {
                if (!isAdded()) return;
                recommendations.clear();
                for (Song s : songs) {
                    recommendations.add(new MusicRecommendation(
                            s.getTitle(), 
                            s.getArtist(), 
                            s.getAlbum() != null ? s.getAlbum() : "Jamendo", 
                            0, 
                            s.getPreviewUrl(),
                            s.getId()
                    ));
                }
                updateRecommendationUI();
            }

            @Override
            public void onError() {
                loadFallbackRecommendations();
            }
        });
    }

    private void loadFallbackRecommendations() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            recommendations.clear();
            String[] data = getPracticalRecommendations(currentMood);
            for (String item : data) {
                String[] parts = item.split("\\|");
                if (parts.length == 3) {
                    recommendations.add(new MusicRecommendation(parts[0], parts[2], parts[1], 10 + random.nextInt(30), null, null));
                }
            }
            updateRecommendationUI();
        }, 100);
    }

    private void updateRecommendationUI() {
        if (!isAdded()) return;
        adapter.notifyDataSetChanged();
        if (loadingRecommendations != null) loadingRecommendations.setVisibility(View.GONE);
        if (rvRecommendations != null) rvRecommendations.setVisibility(View.VISIBLE);
    }

    private String[] getPracticalRecommendations(String mood) {
        switch (mood) {
            case "Happy":
                return new String[]{
                    "Sunshine Vibes|Pop|Pharrell Williams",
                    "Good Feeling|Dance|Avicii",
                    "Walking on Sunshine|Rock|Katrina & The Waves",
                    "Can't Stop the Feeling|Pop|Justin Timberlake"
                };
            case "Sad":
                return new String[]{
                    "Someone Like You|Soul|Adele",
                    "Fix You|Alternative|Coldplay",
                    "Hurt|Acoustic|Johnny Cash",
                    "Stay With Me|Pop|Sam Smith"
                };
            case "Neutral":
                return new String[]{
                    "Daily Mix|Discovery|MoodMuse AI",
                    "Chill Vibes|Indie|Bedroom Pop",
                    "Background Coffee|Lo-Fi|Study Beats",
                    "Easy Listening|Soft Rock|Various Artists"
                };
            case "Surprised":
                return new String[]{
                    "Unexpected Joy|Electronic|ODESZA",
                    "Surprise Symphony|Classical|Mozart",
                    "Sudden Energy|Pop|Bruno Mars",
                    "Shock Value|EDM|Skrillex"
                };
            case "Disgust":
                return new String[]{
                    "Clean Slate|Ambient|Brian Eno",
                    "Fresh Start|Nature|Forest Sounds",
                    "Purification|Meditation|Zen Garden",
                    "Renewal|Instrumental|Piano Collection"
                };
            case "Fear":
                return new String[]{
                    "Brave Heart|Epic|Two Steps From Hell",
                    "Courage|Rock|Queen",
                    "Strength|Classical|Beethoven",
                    "Overcome|Pop|Kelly Clarkson"
                };
            case "Angry":
                return new String[]{
                    "Release|Metal|Metallica",
                    "Let It Out|Rock|Linkin Park",
                    "Catharsis|Alternative|Nirvana",
                    "Energy Release|EDM|The Prodigy"
                };
            default:
                return new String[]{
                    "Daily Mix|Discovery|MoodMuse AI",
                    "New Releases|Various|Top Artists",
                    "Chill Vibes|Indie|Bedroom Pop"
                };
        }
    }

    /**
     * Setup insights display
     */
    private void setupInsights() {
        if (tvWeeklyInsight != null) {
            String insightText;
            if (moodStreak >= 7) {
                insightText = "Amazing! You've logged your mood " + moodStreak + " days in a row! 🎉 " +
                    "Your consistency is helping you build great emotional awareness.";
            } else if (moodStreak >= 3) {
                insightText = "Great start! You're on a " + moodStreak + " day streak. " +
                    "Keep it up to discover patterns in your emotional wellbeing.";
            } else {
                insightText = "Start logging your mood daily to build a streak and get deep insights into your mental health. 💡";
            }
            tvWeeklyInsight.setText(insightText);
        }
    }

    /**
     * Setup button actions
     */
    private void setupButtons() {
        if (btnLogMood != null) {
            btnLogMood.setOnClickListener(v -> {
                // Navigate to manual mood selection
                if (getActivity() != null) {
                    ((UserMainActivity) getActivity()).navigateToUserMoods();
                }
            });
        }
        
        if (btnViewHistory != null) {
            btnViewHistory.setOnClickListener(v -> {
                // Navigate to history/analytics fragment
                if (getActivity() != null) {
                    ((UserMainActivity) getActivity()).navigateToHistory();
                }
            });
        }
        
        if (btnRefreshRecs != null) {
            btnRefreshRecs.setOnClickListener(v -> {
                loadRecommendations();
                Toast.makeText(getContext(), "Refreshing recommendations...", 
                    Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Animate entrance with staggered card animations
     */
    private void animateEntrance() {
        MaterialCardView[] cards = {cardCurrentMood, cardStreak, cardQuickActions, 
                                   cardRecommendations, cardInsights};
        
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                cards[i].setAlpha(0f);
                cards[i].setTranslationY(30f);
                cards[i].animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setStartDelay(i * 30L)
                    .start();
            }
        }
    }

    // --- Bouncing Circles Logic ---

    private static class CircleData {
        View view;
        float x, y;
        float velocityX, velocityY;
        float radius;

        CircleData(View view, float x, float y, float radius) {
            this.view = view;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.velocityX = (float) (Math.random() * 12 - 6);
            this.velocityY = (float) (Math.random() * 12 - 6);
        }
    }

    private void setupFloatingAnimations() {
        if (getView() == null) return;

        stopRandomBouncingAnimation();

        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;

        getView().post(() -> {
            if (!isAdded() || getView() == null || decorativeViews == null) return;
            
            screenWidth = getResources().getDisplayMetrics().widthPixels;
            screenHeight = getResources().getDisplayMetrics().heightPixels;

            circleData = new CircleData[decorativeViews.length];
            for (int i = 0; i < decorativeViews.length; i++) {
                if (decorativeViews[i] != null) {
                    decorativeViews[i].bringToFront();
                    decorativeViews[i].setLayerType(View.LAYER_TYPE_HARDWARE, null);

                    decorativeViews[i].measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    float radius = Math.max(decorativeViews[i].getMeasuredWidth(), 
                                         decorativeViews[i].getMeasuredHeight()) / 2f;
                    if (radius <= 0) radius = 50f;

                    float x = (float) (Math.random() * (Math.max(1, screenWidth - radius * 2))) + radius;
                    float y = (float) (Math.random() * (Math.max(1, screenHeight - radius * 2))) + radius;

                    circleData[i] = new CircleData(decorativeViews[i], x, y, radius);
                }
            }
            preventOverlaps();
            startRandomBouncingAnimation();
        });
    }

    private void startRandomBouncingAnimation() {
        bubbleAnimationHandler = new Handler(Looper.getMainLooper());
        bubbleAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || circleData == null) return;
                updateCirclePositions();
                checkCollisions();
                bubbleAnimationHandler.postDelayed(this, 16);
            }
        };
        bubbleAnimationHandler.post(bubbleAnimationRunnable);
    }

    private void updateCirclePositions() {
        for (CircleData circle : circleData) {
            if (circle != null && circle.view != null) {
                circle.x += circle.velocityX;
                circle.y += circle.velocityY;

                if (circle.x <= circle.radius || circle.x >= screenWidth - circle.radius) {
                    circle.velocityX = -circle.velocityX;
                    circle.x = Math.max(circle.radius, Math.min(screenWidth - circle.radius, circle.x));
                }
                if (circle.y <= circle.radius || circle.y >= screenHeight - circle.radius) {
                    circle.velocityY = -circle.velocityY;
                    circle.y = Math.max(circle.radius, Math.min(screenHeight - circle.radius, circle.y));
                }

                circle.view.setX(circle.x - circle.radius);
                circle.view.setY(circle.y - circle.radius);
            }
        }
    }

    private void checkCollisions() {
        for (int i = 0; i < circleData.length; i++) {
            for (int j = i + 1; j < circleData.length; j++) {
                CircleData c1 = circleData[i];
                CircleData c2 = circleData[j];

                if (c1 != null && c2 != null) {
                    float dx = c2.x - c1.x;
                    float dy = c2.y - c1.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float minDistance = c1.radius + c2.radius;

                    if (distance < minDistance && distance > 0) {
                        float nx = dx / distance;
                        float ny = dy / distance;
                        float separation = minDistance - distance;

                        c1.x -= nx * separation / 2f;
                        c1.y -= ny * separation / 2f;
                        c2.x += nx * separation / 2f;
                        c2.y += ny * separation / 2f;

                        float v1n = c1.velocityX * nx + c1.velocityY * ny;
                        float v2n = c2.velocityX * nx + c2.velocityY * ny;

                        float v1t = -c1.velocityX * ny + c1.velocityY * nx;
                        float v2t = -c2.velocityX * ny + c2.velocityY * nx;

                        c1.velocityX = v2n * nx - v1t * ny;
                        c1.velocityY = v2n * ny + v1t * nx;
                        c2.velocityX = v1n * nx - v2t * ny;
                        c2.velocityY = v1n * ny + v2t * nx;
                    }
                }
            }
        }
    }

    private void preventOverlaps() {
        if (circleData == null) return;
        for (int i = 0; i < 50; i++) { // Max iterations to resolve overlaps
            boolean overlapFound = false;
            for (int j = 0; j < circleData.length; j++) {
                for (int k = j + 1; k < circleData.length; k++) {
                    CircleData c1 = circleData[j];
                    CircleData c2 = circleData[k];
                    if (c1 == null || c2 == null) continue;

                    float dx = c2.x - c1.x;
                    float dy = c2.y - c1.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float minDistance = c1.radius + c2.radius + 10;

                    if (distance < minDistance) {
                        overlapFound = true;
                        float angle = (float) (Math.random() * 2 * Math.PI);
                        c2.x = c1.x + (float) Math.cos(angle) * minDistance;
                        c2.y = c1.y + (float) Math.sin(angle) * minDistance;
                        
                        // Keep within bounds
                        c2.x = Math.max(c2.radius, Math.min(screenWidth - c2.radius, c2.x));
                        c2.y = Math.max(c2.radius, Math.min(screenHeight - c2.radius, c2.y));
                    }
                }
            }
            if (!overlapFound) break;
        }
    }

    private void stopRandomBouncingAnimation() {
        if (bubbleAnimationHandler != null && bubbleAnimationRunnable != null) {
            bubbleAnimationHandler.removeCallbacks(bubbleAnimationRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) applySavedInferredMood(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRandomBouncingAnimation();
        circleData = null;
        decorativeViews = null;
        if (emotionModel != null) {
            emotionModel.close();
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }



    /**
     * Music Recommendation data model
     */
    public static class MusicRecommendation {
        String title;
        String genre;
        String artist;
        int songCount;
        String previewUrl;
        String id;

        public MusicRecommendation(String title, String artist, String genre, int songCount, String previewUrl, String id) {
            this.title = title;
            this.artist = artist;
            this.genre = genre;
            this.songCount = songCount;
            this.previewUrl = previewUrl;
            this.id = id;
        }
    }

    /**
     * RecyclerView Adapter for music recommendations
     */
    private class MusicRecommendationAdapter extends RecyclerView.Adapter<MusicRecommendationAdapter.ViewHolder> {
        private List<MusicRecommendation> items;

        public MusicRecommendationAdapter(List<MusicRecommendation> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_music_recommendation, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MusicRecommendation item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvGenre.setText(item.genre);
            holder.tvArtist.setText(item.artist);
            holder.tvSongCount.setText(item.songCount > 0 ? item.songCount + " songs" : "Jamendo Track");
            
            boolean isThisPlaying = (position == currentlyPlayingPosition);
            if (holder.btnPlay != null) {
                holder.btnPlay.setIconResource(isThisPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
            }

            // Sensitive click logic for the whole item
            holder.itemView.setOnClickListener(v -> {
                animateClick(v);
                if (item.previewUrl != null) {
                    togglePlayback(item.previewUrl, position);
                } else {
                    Toast.makeText(getContext(), "Opening playlist: " + item.title, 
                        Toast.LENGTH_SHORT).show();
                }
            });

            // Specific logic for play button
            if (holder.btnPlay != null) {
                holder.btnPlay.setOnClickListener(v -> {
                    animateClick(v);
                    if (item.previewUrl != null) {
                        togglePlayback(item.previewUrl, position);
                    } else {
                        Toast.makeText(getContext(), "Playing " + item.title + " now! 🎵", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        private void togglePlayback(String url, int position) {
            if (exoPlayer == null) return;

            if (currentlyPlayingPosition == position) {
                if (exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                } else {
                    exoPlayer.play();
                }
            } else {
                exoPlayer.stop();
                exoPlayer.setMediaItem(MediaItem.fromUri(url));
                exoPlayer.prepare();
                exoPlayer.play();
                currentlyPlayingPosition = position;
            }
            notifyDataSetChanged();
        }

        private void animateClick(View view) {
            view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvGenre, tvArtist, tvSongCount;
            MaterialButton btnPlay;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvGenre = itemView.findViewById(R.id.tvGenre);
                tvArtist = itemView.findViewById(R.id.tvArtist);
                tvSongCount = itemView.findViewById(R.id.tvSongCount);
                btnPlay = itemView.findViewById(R.id.btnPlayPlaylist);
            }
        }
    }
}
