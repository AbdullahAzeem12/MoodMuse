package com.example.moodmuse.ui.splash;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moodmuse.R;
import com.example.moodmuse.ui.role.RoleSelectionActivity;
import com.google.android.material.button.MaterialButton;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_splash);
            
            MaterialButton continueBtn = findViewById(R.id.continueBtn);
            View progress = findViewById(R.id.progress);

            // Setup floating animations for decorative elements
            setupFloatingAnimations();

            if (continueBtn != null) {
                continueBtn.setOnClickListener(v -> {
                    try {
                        Log.d(TAG, "Continue button clicked");
                        
                        // Play sound effect
                        playClickSound();
                        
                        // Animate button press
                        animateButtonPress(v);
                        
                        // Disable button to prevent multiple clicks
                        continueBtn.setEnabled(false);
                        
                        // Show progress bar
                        if (progress != null) {
                            progress.setVisibility(View.VISIBLE);
                        }

                        // Add a small delay to see the progress bar
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // Navigate immediately
                            if (isFinishing() || isDestroyed()) return;
                            try {
                            Log.d(TAG, "Starting RoleSelectionActivity");
                            Intent intent = new Intent(SplashActivity.this, RoleSelectionActivity.class);
                            Log.d(TAG, "Intent created: " + intent.toString());
                            
                            // Add some debugging
                            Log.d(TAG, "About to call startActivity");
                            startActivity(intent);
                            Log.d(TAG, "startActivity called successfully");
                            
                            Log.d(TAG, "About to call overridePendingTransition");
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            Log.d(TAG, "overridePendingTransition called successfully");
                            
                            Log.d(TAG, "About to call finish");
                            finish();
                            Log.d(TAG, "finish called successfully");
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error navigating to RoleSelectionActivity", e);
                            e.printStackTrace();
                            // Re-enable button if navigation fails
                            continueBtn.setEnabled(true);
                            if (progress != null) {
                                progress.setVisibility(View.GONE);
                            }
                        }
                        }, 500); // 500ms delay to see progress bar
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error in button click handler", e);
                    }
                });
            } else {
                Log.e(TAG, "Continue button not found!");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    /**
     * Setup continuous up-down floating animations for decorative circle elements
     */
    private void setupFloatingAnimations() {
        try {
            // Get all decorative circle views from the layout
            View[] decorativeViews = {
                findViewById(R.id.decorCircle1),
                findViewById(R.id.decorCircle2),
                findViewById(R.id.decorCircle3),
                findViewById(R.id.decorCircle4),
                findViewById(R.id.decorCircle5),
                findViewById(R.id.decorCircle6)
            };

            // Load different animation speeds
            Animation floatSlow = AnimationUtils.loadAnimation(this, R.anim.float_up_down_slow);
            Animation floatMedium = AnimationUtils.loadAnimation(this, R.anim.float_up_down);
            Animation floatFast = AnimationUtils.loadAnimation(this, R.anim.float_up_down_fast);

            // Apply animations to decorative elements with varying speeds
            for (int i = 0; i < decorativeViews.length; i++) {
                if (decorativeViews[i] != null) {
                    Animation animation;
                    
                    // Alternate between different speeds for variety
                    switch (i % 3) {
                        case 0:
                            animation = floatSlow;
                            break;
                        case 1:
                            animation = floatMedium;
                            break;
                        default:
                            animation = floatFast;
                            break;
                    }
                    
                    // Start animation with slight delay for staggered effect
                    final View view = decorativeViews[i];
                    final Animation finalAnimation = animation;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        view.startAnimation(finalAnimation);
                    }, i * 100L); // 100ms delay between each element
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up floating animations", e);
        }
    }

    /**
     * Play a short musical click sound effect
     */
    private void playClickSound() {
        try {
            // Create a pleasant musical tone using ToneGenerator
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
            
            // Play a musical beep tone
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
            
            // Release after delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    toneGen.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing tone generator", e);
                }
            }, 200);
            
        } catch (Exception e) {
            Log.e(TAG, "Error playing click sound", e);
            // Fail silently - sound is not critical
        }
    }

    /**
     * Animate button press with scale effect
     */
    private void animateButtonPress(View button) {
        try {
            button.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start();
                })
                .start();
        } catch (Exception e) {
            Log.e(TAG, "Error animating button press", e);
        }
    }
}


