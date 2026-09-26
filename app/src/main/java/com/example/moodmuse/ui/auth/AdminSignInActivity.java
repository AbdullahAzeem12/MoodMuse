package com.example.moodmuse.ui.auth;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.moodmuse.R;
import com.example.moodmuse.ui.admin.AdminDashboardActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AdminSignInActivity extends AppCompatActivity {

    private static final String TAG = "AdminSignInActivity";
    private TextInputEditText adminIdInput;
    private TextInputEditText passwordInput;
    private CheckBox rememberMeCheckbox;
    private MaterialButton signInButton;
    private TextView forgotPasswordLink;
    private TextView registerLink;
    private View backButton;

    // Custom animation variables for decorative bubbles
    private Handler animationHandler;
    private Runnable animationRunnable;
    private View[] decorativeViews;
    private CircleData[] circleData;
    private int screenWidth, screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_signin);

        // Initialize views
        adminIdInput = findViewById(R.id.adminIdInput);
        passwordInput = findViewById(R.id.passwordInput);
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox);
        signInButton = findViewById(R.id.signInButton);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        registerLink = findViewById(R.id.registerLink);
        backButton = findViewById(R.id.backButton);

        // Setup floating animations for decorative elements
        setupFloatingAnimations();

        // Set click listeners
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.d(TAG, "Sign in button clicked");
                    playClickSound();
                    animateButtonPress(v);
                    // Implement sign in logic here
                    attemptAdminSignIn();
                } catch (Exception e) {
                    Log.e(TAG, "Error in sign in button click", e);
                }
            }
        });

        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.d(TAG, "Register link clicked");
                    playClickSound();
                    animateButtonPress(v);
                    // Navigate to Admin Registration activity
                    Intent intent = new Intent(AdminSignInActivity.this, AdminRegisterActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Error in register link click", e);
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.d(TAG, "Back button clicked");
                    playClickSound();
                    animateButtonPress(v);
                    // Go back to role selection
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error in back button click", e);
                }
            }
        });

        forgotPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.d(TAG, "Forgot password link clicked");
                    playClickSound();
                    animateButtonPress(v);
                    // Implement forgot password functionality
                    // For now, just show a toast message
                    Toast.makeText(AdminSignInActivity.this, 
                        "Please contact system administrator to reset your password", 
                        Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error in forgot password link click", e);
                }
            }
        });
    }

    private void attemptAdminSignIn() {
        String adminId = adminIdInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        boolean rememberMe = rememberMeCheckbox.isChecked();

        // Validate inputs
        if (adminId.isEmpty()) {
            adminIdInput.setError("Admin ID is required");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return;
        }
        // For demonstration purposes, show a success message with 2FA notice
        // In a real app, you would authenticate with a server and implement 2FA
        Toast.makeText(this, 
            "Verification code sent to your registered device", 
            Toast.LENGTH_SHORT).show();
            
        // In a real app, you would navigate to a 2FA verification screen
        // For now, simulate a successful login after a delay
        signInButton.setText("Verifying...");
        
        // Change button to transparent when verifying
        signInButton.setBackgroundResource(R.drawable.bg_button_transparent);
        
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AdminSignInActivity.this,
                        "Admin sign in successful!",
                        Toast.LENGTH_SHORT).show();
                signInButton.setEnabled(true);
                signInButton.setText("Sign In");

                Intent intent = new Intent(AdminSignInActivity.this, AdminDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }, 2000); // 2 second delay to simulate 2FA verification
    }

    /**
     * Play a short click sound effect (musical chime)
     */
    private void playClickSound() {
        try {
            // Create a short beep sound using ToneGenerator as a music-like effect
            android.media.ToneGenerator toneGen = new android.media.ToneGenerator(
                android.media.AudioManager.STREAM_MUSIC, 80);

            // Play a pleasant musical tone (C note)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150);

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

    // Circle data class for tracking position and movement
    private static class CircleData {
        View view;
        float x, y;
        float velocityX, velocityY;
        float radius;
        int color;

        CircleData(View view, float x, float y, float radius, int color) {
            this.view = view;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            // Random initial velocity - FAST for full screen movement
            this.velocityX = (float) (Math.random() * 16 - 8); // -8 to 8 (much faster)
            this.velocityY = (float) (Math.random() * 16 - 8); // -8 to 8 (much faster)
        }
    }

    /**
     * Setup random bouncing animations with collision detection and repulsion
     */
    private void setupFloatingAnimations() {
        try {
            Log.d(TAG, "Setting up 11 decorative circle random bouncing with collision detection");

            // Get screen dimensions for FULL SCREEN movement
            screenWidth = getResources().getDisplayMetrics().widthPixels;
            screenHeight = getResources().getDisplayMetrics().heightPixels;
            Log.d(TAG, "Full screen dimensions: " + screenWidth + "x" + screenHeight);

            // Get all 11 decorative circle views from the layout
            decorativeViews = new View[]{
                findViewById(R.id.decorCircle1),
                findViewById(R.id.decorCircle2),
                findViewById(R.id.decorCircle3),
                findViewById(R.id.decorCircle4),
                findViewById(R.id.decorCircle5),
                findViewById(R.id.decorCircle6),
                findViewById(R.id.decorCircle7),
                findViewById(R.id.decorCircle8),
                findViewById(R.id.decorCircle9),
                findViewById(R.id.decorCircle10),
                findViewById(R.id.decorCircle11)
            };

            // Initialize circle data with smart positioning to avoid overlaps
            circleData = new CircleData[decorativeViews.length];
            for (int i = 0; i < decorativeViews.length; i++) {
                if (decorativeViews[i] != null) {
                    // Get actual view dimensions for proper radius calculation
                    decorativeViews[i].measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    int viewWidth = decorativeViews[i].getMeasuredWidth();
                    int viewHeight = decorativeViews[i].getMeasuredHeight();
                    float radius = Math.max(viewWidth, viewHeight) / 2f;

                    float x, y;

                    // Position circles 9, 10, 11 at the bottom of the screen
                    if (i >= 7) { // Circles 8, 9, 10 (indices 7, 8, 9) - bottom positioning
                        x = (float) (Math.random() * (screenWidth - radius * 2)) + radius;
                        y = (float) (Math.random() * (screenHeight * 0.3)) + (float) (screenHeight * 0.7 - radius); // Bottom 30% of screen
                    } else {
                        // Circles 1-7 - random positions in upper 70% of screen
                        x = (float) (Math.random() * (screenWidth - radius * 2)) + radius;
                        y = (float) (Math.random() * (screenHeight * 0.7 - radius * 2)) + radius;
                    }

                    // Check for overlaps with existing circles and adjust position
                    boolean hasOverlap = true;
                    int attempts = 0;
                    while (hasOverlap && attempts < 100) { // Increased attempts for better positioning
                        hasOverlap = false;
                        for (int j = 0; j < i; j++) {
                            if (circleData[j] != null) {
                                float dx = x - circleData[j].x;
                                float dy = y - circleData[j].y;
                                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                                float minDistance = radius + circleData[j].radius + 30; // 30px buffer - NO OVERLAP

                                if (distance < minDistance) {
                                    hasOverlap = true;
                                    // Move to a new random position
                                    if (i >= 7) { // Bottom circles
                                        x = (float) (Math.random() * (screenWidth - radius * 2)) + radius;
                                        y = (float) (Math.random() * (screenHeight * 0.3)) + (float) (screenHeight * 0.7 - radius);
                                    } else { // Top circles
                                        x = (float) (Math.random() * (screenWidth - radius * 2)) + radius;
                                        y = (float) (Math.random() * (screenHeight * 0.7 - radius * 2)) + radius;
                                    }
                                    break;
                                }
                            }
                        }
                        attempts++;
                    }

                    circleData[i] = new CircleData(decorativeViews[i], x, y, radius, i);
                    Log.d(TAG, "Initialized circle " + (i + 1) + " at position (" + x + ", " + y + ") with radius " + radius);
                }
            }

            // Start animation loop
            startRandomBouncingAnimation();

        } catch (Exception e) {
            Log.e(TAG, "Error setting up floating animations", e);
            e.printStackTrace();
        }
    }

    /**
     * Start the random bouncing animation with collision detection
     */
    private void startRandomBouncingAnimation() {
        animationHandler = new Handler(Looper.getMainLooper());
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    updateCirclePositions();
                    checkCollisions();
                    animationHandler.postDelayed(this, 16); // ~60 FPS
                } catch (Exception e) {
                    Log.e(TAG, "Error in animation loop", e);
                }
            }
        };
        animationHandler.post(animationRunnable);
        Log.d(TAG, "Started random bouncing animation loop");
    }

    /**
     * Update positions of all circles
     */
    private void updateCirclePositions() {
        for (CircleData circle : circleData) {
            if (circle != null && circle.view != null) {
                // Update position
                circle.x += circle.velocityX;
                circle.y += circle.velocityY;

                // Bounce off screen edges - FULL SCREEN boundaries
                if (circle.x <= circle.radius || circle.x >= screenWidth - circle.radius) {
                    circle.velocityX = -circle.velocityX;
                    circle.x = Math.max(circle.radius, Math.min(screenWidth - circle.radius, circle.x));
                    // Add random direction change on edge bounce
                    circle.velocityY += (float) (Math.random() * 2 - 1) * 0.5f;
                }
                if (circle.y <= circle.radius || circle.y >= screenHeight - circle.radius) {
                    circle.velocityY = -circle.velocityY;
                    circle.y = Math.max(circle.radius, Math.min(screenHeight - circle.radius, circle.y));
                    // Add random direction change on edge bounce
                    circle.velocityX += (float) (Math.random() * 2 - 1) * 0.5f;
                }

                // Add frequent random direction changes to prevent stuck patterns
                if (Math.random() < 0.05) { // 5% chance per frame (increased from 1%)
                    circle.velocityX += (float) (Math.random() * 4 - 2) * 0.5f; // Increased randomness
                    circle.velocityY += (float) (Math.random() * 4 - 2) * 0.5f; // Increased randomness
                }

                // Update view position
                circle.view.setX(circle.x - circle.radius);
                circle.view.setY(circle.y - circle.radius);
            }
        }

        // CONTINUOUS OVERLAP PREVENTION - Run after every position update
        preventOverlaps();
    }

    /**
     * SIMPLE overlap prevention - immediate random repulsion
     */
    private void preventOverlaps() {
        for (int i = 0; i < circleData.length; i++) {
            for (int j = i + 1; j < circleData.length; j++) {
                CircleData circle1 = circleData[i];
                CircleData circle2 = circleData[j];

                if (circle1 != null && circle2 != null &&
                    circle1.view != null && circle2.view != null) {

                    // Calculate distance between centers
                    float dx = circle2.x - circle1.x;
                    float dy = circle2.y - circle1.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float minDistance = circle1.radius + circle2.radius + 15; // 15px buffer

                    // If circles are too close - IMMEDIATE RANDOM REPULSION
                    if (distance < minDistance && distance > 0) {
                        // IMMEDIATELY separate circles
                        float separationDistance = (minDistance - distance) + 15; // Extra 15px push
                        float nx = dx / distance;
                        float ny = dy / distance;

                        // Force immediate separation
                        circle1.x -= nx * separationDistance / 2f;
                        circle1.y -= ny * separationDistance / 2f;
                        circle2.x += nx * separationDistance / 2f;
                        circle2.y += ny * separationDistance / 2f;

                        // IMMEDIATE RANDOM VELOCITIES
                        circle1.velocityX = (float) (Math.random() * 20 - 10); // -10 to 10
                        circle1.velocityY = (float) (Math.random() * 20 - 10); // -10 to 10
                        circle2.velocityX = (float) (Math.random() * 20 - 10); // -10 to 10
                        circle2.velocityY = (float) (Math.random() * 20 - 10); // -10 to 10

                        // Update view positions immediately
                        circle1.view.setX(circle1.x - circle1.radius);
                        circle1.view.setY(circle1.y - circle1.radius);
                        circle2.view.setX(circle2.x - circle2.radius);
                        circle2.view.setY(circle2.y - circle2.radius);

                        Log.d(TAG, "IMMEDIATE RANDOM REPULSION between circles " + (i + 1) + " and " + (j + 1));
                    }
                }
            }
        }
    }

    /**
     * SIMPLE and EFFECTIVE collision detection - IMMEDIATE repulsion to random directions
     */
    private void checkCollisions() {
        for (int i = 0; i < circleData.length; i++) {
            for (int j = i + 1; j < circleData.length; j++) {
                CircleData circle1 = circleData[i];
                CircleData circle2 = circleData[j];

                if (circle1 != null && circle2 != null &&
                    circle1.view != null && circle2.view != null) {

                    // Calculate distance between centers
                    float dx = circle2.x - circle1.x;
                    float dy = circle2.y - circle1.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float minDistance = circle1.radius + circle2.radius + 20; // 20px buffer

                    // If circles are touching or overlapping - IMMEDIATE ACTION
                    if (distance < minDistance && distance > 0) {
                        // IMMEDIATELY separate circles to prevent overlap
                        float separationDistance = (minDistance - distance) + 10; // Extra 10px push
                        float nx = dx / distance;
                        float ny = dy / distance;

                        // Force immediate separation
                        circle1.x -= nx * separationDistance / 2f;
                        circle1.y -= ny * separationDistance / 2f;
                        circle2.x += nx * separationDistance / 2f;
                        circle2.y += ny * separationDistance / 2f;

                        // IMMEDIATE RANDOM DIRECTION REPULSION
                        // Generate completely random velocities for both circles
                        circle1.velocityX = (float) (Math.random() * 16 - 8); // -8 to 8
                        circle1.velocityY = (float) (Math.random() * 16 - 8); // -8 to 8
                        circle2.velocityX = (float) (Math.random() * 16 - 8); // -8 to 8
                        circle2.velocityY = (float) (Math.random() * 16 - 8); // -8 to 8

                        // Update view positions immediately
                        circle1.view.setX(circle1.x - circle1.radius);
                        circle1.view.setY(circle1.y - circle1.radius);
                        circle2.view.setX(circle2.x - circle2.radius);
                        circle2.view.setY(circle2.y - circle2.radius);

                        Log.d(TAG, "IMMEDIATE RANDOM REPULSION between circles " + (i + 1) + " and " + (j + 1) +
                              " - separation: " + separationDistance);
                    }
                }
            }
        }
    }

    /**
     * Stop the animation when activity is destroyed
     */
    private void stopRandomBouncingAnimation() {
        if (animationHandler != null && animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
            Log.d(TAG, "Stopped random bouncing animation");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRandomBouncingAnimation();
        Log.d(TAG, "onPause called - animation stopped");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (circleData != null) {
            startRandomBouncingAnimation();
            Log.d(TAG, "onResume called - animation restarted");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop animation
        stopRandomBouncingAnimation();

        // Clean up animation data
        circleData = null;
        decorativeViews = null;
        animationHandler = null;
        animationRunnable = null;

        Log.d(TAG, "onDestroy called - all resources cleaned up");
    }
}
