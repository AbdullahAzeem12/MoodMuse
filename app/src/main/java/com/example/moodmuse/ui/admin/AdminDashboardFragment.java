package com.example.moodmuse.ui.admin;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.moodmuse.R;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * AdminDashboardFragment - Comprehensive dashboard with real-time analytics
 * Features:
 * - Real-time user statistics
 * - Mood detection analytics
 * - App usage metrics
 * - Interactive charts and graphs
 * - Animated stat cards
 * - Refresh functionality
 */
public class AdminDashboardFragment extends Fragment {

    // UI Components
    private TextView tvTotalUsers, tvActiveUsers, tvTotalMoods, tvAvgSessionTime;
    private TextView tvUserGrowth, tvMoodGrowth, tvEngagementRate, tvRetentionRate;
    private ProgressBar pbUserGrowth, pbMoodDetection, pbEngagement, pbRetention;
    private MaterialCardView cardUsers, cardMoods, cardSessions, cardRetention;
    private LinearLayout layoutMoodBreakdown, layoutRecentActivity;
    private TextView tvLastUpdated, tvHappyPercent, tvSadPercent, tvCalmPercent, tvAnxiousPercent;
    private ProgressBar pbRefresh;
    private View btnRefreshData;

    // Data models
    private DashboardStats currentStats;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private Random random;

    // Animation flags
    private boolean isAnimating = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupClickListeners();
        initializeData();
        startAutoRefresh();
        animateStatsEntry();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews(View view) {
        // Stat card TextViews
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        tvActiveUsers = view.findViewById(R.id.tvActiveUsers);
        tvTotalMoods = view.findViewById(R.id.tvTotalMoods);
        tvAvgSessionTime = view.findViewById(R.id.tvAvgSessionTime);
        
        // Growth indicators
        tvUserGrowth = view.findViewById(R.id.tvUserGrowth);
        tvMoodGrowth = view.findViewById(R.id.tvMoodGrowth);
        tvEngagementRate = view.findViewById(R.id.tvEngagementRate);
        tvRetentionRate = view.findViewById(R.id.tvRetentionRate);
        
        // Progress bars
        pbUserGrowth = view.findViewById(R.id.pbUserGrowth);
        pbMoodDetection = view.findViewById(R.id.pbMoodDetection);
        pbEngagement = view.findViewById(R.id.pbEngagement);
        pbRetention = view.findViewById(R.id.pbRetention);
        pbRefresh = view.findViewById(R.id.pbRefresh);
        
        // Cards
        cardUsers = view.findViewById(R.id.cardUsers);
        cardMoods = view.findViewById(R.id.cardMoods);
        cardSessions = view.findViewById(R.id.cardSessions);
        cardRetention = view.findViewById(R.id.cardRetention);
        
        // Mood breakdown
        layoutMoodBreakdown = view.findViewById(R.id.layoutMoodBreakdown);
        tvHappyPercent = view.findViewById(R.id.tvHappyPercent);
        tvSadPercent = view.findViewById(R.id.tvSadPercent);
        tvCalmPercent = view.findViewById(R.id.tvCalmPercent);
        tvAnxiousPercent = view.findViewById(R.id.tvAnxiousPercent);
        
        // Activity section
        layoutRecentActivity = view.findViewById(R.id.layoutRecentActivity);
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated);
        btnRefreshData = view.findViewById(R.id.btnRefreshData);
        
        random = new Random();
    }

    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        // Refresh button
        if (btnRefreshData != null) {
            btnRefreshData.setOnClickListener(v -> {
                if (!isAnimating) {
                    refreshDashboardData();
                }
            });
        }

        // Card click listeners for detailed views
        setupCardClickListener(cardUsers, "Users Analytics");
        setupCardClickListener(cardMoods, "Mood Analytics");
        setupCardClickListener(cardSessions, "Session Analytics");
        setupCardClickListener(cardRetention, "Retention Analytics");
    }

    /**
     * Setup individual card click listeners
     */
    private void setupCardClickListener(MaterialCardView card, String title) {
        if (card != null) {
            card.setOnClickListener(v -> {
                // Pulse animation on click
                animateCardPulse(card);
                // Show toast or navigate to detailed view
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), 
                        "Opening " + title, android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Initialize dashboard with simulated data
     */
    private void initializeData() {
        currentStats = generateSimulatedStats();
        updateUIWithStats(currentStats);
        updateLastRefreshTime();
    }

    /**
     * Generate simulated dashboard statistics
     */
    private DashboardStats generateSimulatedStats() {
        DashboardStats stats = new DashboardStats();
        stats.totalUsers = 1247 + random.nextInt(100);
        stats.activeUsers = (int) (stats.totalUsers * (0.6 + random.nextDouble() * 0.2));
        stats.totalMoodsDetected = 8934 + random.nextInt(500);
        stats.avgSessionMinutes = 12 + random.nextInt(8);
        stats.userGrowthPercent = 15.5 + (random.nextDouble() * 10);
        stats.moodGrowthPercent = 22.3 + (random.nextDouble() * 15);
        stats.engagementRate = 68.5 + (random.nextDouble() * 20);
        stats.retentionRate = 74.2 + (random.nextDouble() * 15);
        
        // Mood breakdown percentages
        stats.happyPercent = 35 + random.nextInt(15);
        stats.sadPercent = 15 + random.nextInt(10);
        stats.calmPercent = 25 + random.nextInt(15);
        stats.anxiousPercent = 100 - (stats.happyPercent + stats.sadPercent + stats.calmPercent);
        
        return stats;
    }

    /**
     * Update UI with current stats
     */
    private void updateUIWithStats(DashboardStats stats) {
        // Update stat values
        if (tvTotalUsers != null) 
            tvTotalUsers.setText(String.format(Locale.getDefault(), "%,d", stats.totalUsers));
        
        if (tvActiveUsers != null) 
            tvActiveUsers.setText(String.format(Locale.getDefault(), "%,d active", stats.activeUsers));
        
        if (tvTotalMoods != null) 
            tvTotalMoods.setText(String.format(Locale.getDefault(), "%,d", stats.totalMoodsDetected));
        
        if (tvAvgSessionTime != null) 
            tvAvgSessionTime.setText(String.format(Locale.getDefault(), "%d min", stats.avgSessionMinutes));
        
        // Update growth indicators
        updateGrowthIndicator(tvUserGrowth, stats.userGrowthPercent, pbUserGrowth);
        updateGrowthIndicator(tvMoodGrowth, stats.moodGrowthPercent, pbMoodDetection);
        updateGrowthIndicator(tvEngagementRate, stats.engagementRate, pbEngagement);
        updateGrowthIndicator(tvRetentionRate, stats.retentionRate, pbRetention);
        
        // Update mood breakdown
        updateMoodBreakdown(stats);
    }

    /**
     * Update growth indicator with animated progress
     */
    private void updateGrowthIndicator(TextView textView, double percent, ProgressBar progressBar) {
        if (textView != null) {
            String formattedPercent = String.format(Locale.getDefault(), "%.1f%%", percent);
            textView.setText(formattedPercent);
            
            // Set color based on value
            int color = percent >= 70 ? R.color.admin_success :
                       percent >= 50 ? R.color.admin_warning : R.color.admin_error;
            textView.setTextColor(ContextCompat.getColor(requireContext(), color));
        }
        
        if (progressBar != null) {
            animateProgress(progressBar, (int) percent);
        }
    }

    /**
     * Update mood breakdown percentages
     */
    private void updateMoodBreakdown(DashboardStats stats) {
        if (tvHappyPercent != null) 
            tvHappyPercent.setText(stats.happyPercent + "%");
        if (tvSadPercent != null) 
            tvSadPercent.setText(stats.sadPercent + "%");
        if (tvCalmPercent != null) 
            tvCalmPercent.setText(stats.calmPercent + "%");
        if (tvAnxiousPercent != null) 
            tvAnxiousPercent.setText(stats.anxiousPercent + "%");
    }

    /**
     * Animate stats entry with stagger effect
     */
    private void animateStatsEntry() {
        List<View> cards = new ArrayList<>();
        if (cardUsers != null) cards.add(cardUsers);
        if (cardMoods != null) cards.add(cardMoods);
        if (cardSessions != null) cards.add(cardSessions);
        if (cardRetention != null) cards.add(cardRetention);
        
        for (int i = 0; i < cards.size(); i++) {
            View card = cards.get(i);
            card.setAlpha(0f);
            card.setTranslationY(50f);
            
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(i * 100L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }
    }

    /**
     * Animate progress bar to target value
     */
    private void animateProgress(ProgressBar progressBar, int targetProgress) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress);
        animation.setDuration(1000);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.start();
    }

    /**
     * Animate card pulse effect
     */
    private void animateCardPulse(View card) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 1.05f, 1f);
        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.start();
        scaleY.start();
    }

    /**
     * Refresh dashboard data
     */
    private void refreshDashboardData() {
        isAnimating = true;
        
        if (pbRefresh != null) {
            pbRefresh.setVisibility(View.VISIBLE);
        }
        
        // Simulate network delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            currentStats = generateSimulatedStats();
            updateUIWithStats(currentStats);
            updateLastRefreshTime();
            
            if (pbRefresh != null) {
                pbRefresh.setVisibility(View.GONE);
            }
            isAnimating = false;
            
            if (getContext() != null) {
                android.widget.Toast.makeText(getContext(), 
                    "Dashboard updated", android.widget.Toast.LENGTH_SHORT).show();
            }
        }, 1500);
    }

    /**
     * Update last refresh timestamp
     */
    private void updateLastRefreshTime() {
        if (tvLastUpdated != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            tvLastUpdated.setText("Last updated: " + timestamp);
        }
    }

    /**
     * Start auto-refresh every 30 seconds
     */
    private void startAutoRefresh() {
        updateHandler = new Handler(Looper.getMainLooper());
        updateRunnable = () -> {
            if (!isAnimating) {
                refreshDashboardData();
            }
            updateHandler.postDelayed(updateRunnable, 30000); // 30 seconds
        };
        updateHandler.postDelayed(updateRunnable, 30000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }

    /**
     * Data model for dashboard statistics
     */
    public static class DashboardStats {
        public int totalUsers;
        public int activeUsers;
        public int totalMoodsDetected;
        public int avgSessionMinutes;
        public double userGrowthPercent;
        public double moodGrowthPercent;
        public double engagementRate;
        public double retentionRate;
        public int happyPercent;
        public int sadPercent;
        public int calmPercent;
        public int anxiousPercent;
    }
}

