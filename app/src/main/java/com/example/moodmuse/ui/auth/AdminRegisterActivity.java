package com.example.moodmuse.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.core.widget.NestedScrollView;

import com.example.moodmuse.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AdminRegisterActivity extends AppCompatActivity {

    private static final String TAG = "AdminRegisterActivity";
    private TextInputEditText adminIdInput;
    private TextInputEditText fullNameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private CheckBox termsCheckbox;
    private CheckBox securityPolicyCheckbox;
    private MaterialButton registerButton;
    private TextView securityLevelText;
    private ImageView securityLevelIcon;
    private View backButton;
    private TextView signInLink;
    private NestedScrollView registerScrollView;

    // Custom animation variables for decorative bubbles
    private Handler animationHandler;
    private Runnable animationRunnable;
    private View[] decorativeViews;
    private CircleData[] circleData;
    private int screenWidth, screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_register);

        initializeViews();
        setupClickListeners();
        setupTextWatchers();
        
        // Setup floating animations for decorative elements
        setupFloatingAnimations();
    }

    private void initializeViews() {
        adminIdInput = findViewById(R.id.adminIdInput);
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        termsCheckbox = findViewById(R.id.termsCheckbox);
        securityPolicyCheckbox = findViewById(R.id.securityPolicyCheckbox);
        registerButton = findViewById(R.id.registerButton);
        securityLevelText = findViewById(R.id.securityLevelText);
        securityLevelIcon = findViewById(R.id.securityLevelIcon);
        backButton = findViewById(R.id.backButton);
        signInLink = findViewById(R.id.signInLink);
        registerScrollView = findViewById(R.id.registerScrollView);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Register button clicked");
                playClickSound();
                animateButtonPress(v);
                if (validateInputs()) {
                    submitRegistration();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in register button click", e);
            }
        });

        backButton.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Back button clicked");
                playClickSound();
                animateButtonPress(v);
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Error in back button click", e);
            }
        });
        
        signInLink.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Sign in link clicked");
                playClickSound();
                animateButtonPress(v);
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Error in sign in link click", e);
            }
        });
    }

    private void setupTextWatchers() {
        if (passwordInput == null) {
            return;
        }

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordStrength(s != null ? s.toString() : "");
            }
        });
    }

    private void updatePasswordStrength(String password) {
        int strength = 0;

        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[^A-Za-z0-9].*")) strength++;

        switch (strength) {
            case 0:
            case 1:
                applyPasswordStrength("Password Strength: Weak",
                        R.color.auth_error,
                        android.R.drawable.ic_partial_secure);
                break;
            case 2:
            case 3:
                applyPasswordStrength("Password Strength: Moderate",
                        R.color.admin_warning,
                        android.R.drawable.ic_partial_secure);
                break;
            case 4:
            case 5:
                applyPasswordStrength("Password Strength: Strong",
                        R.color.auth_success,
                        android.R.drawable.ic_secure);
                break;
        }
    }

    private void applyPasswordStrength(String label, int colorRes, int iconRes) {
        int color = ContextCompat.getColor(this, colorRes);
        securityLevelText.setText(label);
        securityLevelText.setTextColor(color);
        securityLevelIcon.setImageResource(iconRes);
        securityLevelIcon.setColorFilter(color);
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (adminIdInput.getText().toString().trim().isEmpty()) {
            adminIdInput.setError("Admin ID is required");
            focusAndScroll(adminIdInput);
            isValid = false;
        }

        if (fullNameInput.getText().toString().trim().isEmpty()) {
            fullNameInput.setError("Full name is required");
            focusAndScroll(fullNameInput);
            isValid = false;
        }

        String email = emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            focusAndScroll(emailInput);
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email address");
            focusAndScroll(emailInput);
            isValid = false;
        }

        if (phoneInput.getText().toString().trim().isEmpty()) {
            phoneInput.setError("Phone number is required");
            focusAndScroll(phoneInput);
            isValid = false;
        }

        String password = passwordInput.getText().toString();
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            focusAndScroll(passwordInput);
            isValid = false;
        } else if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters");
            focusAndScroll(passwordInput);
            isValid = false;
        }

        String confirmPassword = confirmPasswordInput.getText().toString();
        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError("Please confirm your password");
            focusAndScroll(confirmPasswordInput);
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            focusAndScroll(confirmPasswordInput);
            isValid = false;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "You must accept the terms and conditions", Toast.LENGTH_SHORT).show();
            focusAndScroll(termsCheckbox);
            isValid = false;
        }

        if (!securityPolicyCheckbox.isChecked()) {
            Toast.makeText(this, "You must accept the security policy", Toast.LENGTH_SHORT).show();
            focusAndScroll(securityPolicyCheckbox);
            isValid = false;
        }

        return isValid;
    }
    private void focusAndScroll(View target) {
        if (target == null) {
            return;
        }
        target.requestFocus();
        if (registerScrollView != null) {
            registerScrollView.post(() -> registerScrollView.smoothScrollTo(0, (int) target.getY()));
        }
    }

    private void submitRegistration() {
        registerButton.setEnabled(false);
        registerButton.setText("Registering...");
        
        // Change button to transparent when registering
        registerButton.setBackgroundResource(R.drawable.bg_button_transparent);

        new Handler().postDelayed(() -> {
            Toast.makeText(AdminRegisterActivity.this,
                    "Admin registration successful! Verification email sent.",
                    Toast.LENGTH_SHORT).show();

            registerButton.setEnabled(true);
            registerButton.setText("Register");

            finish();
        }, 1500);
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
