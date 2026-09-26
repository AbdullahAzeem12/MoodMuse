package com.example.moodmuse.ui.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.moodmuse.R;
import com.example.moodmuse.stats.UserStatsManager;
import com.example.moodmuse.ui.user.UserMainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class UserSignInActivity extends AppCompatActivity {

    private static final String TAG = "UserSignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton signInButton;
    private TextView forgotPasswordText;
    private TextView registerLink;
    private View backButton;
    private ImageView logoImage;
    private com.google.android.material.checkbox.MaterialCheckBox rememberMeCheckbox;

    // Social login views
    private CardView googleCardView;
    private CardView facebookCardView;
    private ImageView googleSignIn;
    private ImageView facebookSignIn;

    // Animation flags
    private boolean isGoogleAnimating = false;
    private boolean isFacebookAnimating = false;
    private Handler animationHandler = new Handler();

    // Custom animation variables for decorative bubbles
    private Handler bubbleAnimationHandler;
    private Runnable bubbleAnimationRunnable;
    private View[] decorativeViews;
    private CircleData[] circleData;
    private int screenWidth, screenHeight;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private UserStatsManager userStatsManager;

    private final androidx.activity.result.ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        // Google Sign In was successful, authenticate with Firebase
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        // Google Sign In failed, update UI appropriately
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_signin);

        mAuth = FirebaseAuth.getInstance();
        userStatsManager = new UserStatsManager(this);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signInButton = findViewById(R.id.signInButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        registerLink = findViewById(R.id.registerLink);
        backButton = findViewById(R.id.backButton);
        logoImage = findViewById(R.id.logoImage);
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox);

        // Load remembered credentials
        loadRememberedCredentials();

        // Initialize social login views
        googleCardView = findViewById(R.id.googleCardView);
        facebookCardView = findViewById(R.id.facebookCardView);
        googleSignIn = findViewById(R.id.googleSignIn);
        facebookSignIn = findViewById(R.id.facebookSignIn);

        // Enable hardware acceleration for 3D animations
        googleCardView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        facebookCardView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Setup floating animations for decorative elements
        setupFloatingAnimations();

        // Set click listeners
        signInButton.setOnClickListener(v -> {
            playClickSound();
            attemptSignIn();
        });

        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(UserSignInActivity.this, UserRegisterActivity.class);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());

        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());

        // Set up social login click listeners with 3D rotation
        setupSocialLoginAnimations();

        // Start initial entrance animation
        startEntranceAnimation();

        // Start logo animation
        startLogoAnimation();
    }

    private void attemptSignIn() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate inputs
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        // Save credentials if "Remember Me" is checked
        handleRememberMe(email, password);

        signInButton.setEnabled(false);
        signInButton.setText("Signing in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            recordEmailSignIn(user.getUid());
                        } else {
                            navigateMain();
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(UserSignInActivity.this, "Email not found.", Toast.LENGTH_SHORT).show();
                        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(UserSignInActivity.this, "Wrong password.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UserSignInActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                        resetSignInButton();
                    }
                });
    }

    private void handleRememberMe(String email, String password) {
        android.content.SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        if (rememberMeCheckbox.isChecked()) {
            editor.putBoolean("rememberMe", true);
            editor.putString("savedEmail", email);
            editor.putString("savedPassword", password);
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private void loadRememberedCredentials() {
        android.content.SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("rememberMe", false)) {
            emailInput.setText(prefs.getString("savedEmail", ""));
            passwordInput.setText(prefs.getString("savedPassword", ""));
            rememberMeCheckbox.setChecked(true);
        }
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Enter your registered email");
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(UserSignInActivity.this, "Please enter a valid email address.", Toast.LENGTH_LONG).show();
                return;
            }

            AlertDialog progressDialog = new AlertDialog.Builder(UserSignInActivity.this)
                    .setTitle("Sending Request")
                    .setMessage("Please wait...")
                    .setCancelable(false)
                    .show();

            Log.d(TAG, "Attempting to send password reset email to: " + email);

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Successfully sent password reset email request to Firebase for: " + email);
                            Toast.makeText(UserSignInActivity.this, "Password reset email sent. Please check your inbox and spam folder.", Toast.LENGTH_LONG).show();
                        } else {
                            Exception exception = task.getException();
                            Log.e(TAG, "Failed to send password reset email for: " + email, exception);

                            if (exception instanceof FirebaseAuthInvalidUserException) {
                                Toast.makeText(UserSignInActivity.this, "No account found with this email.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UserSignInActivity.this, "Failed to send reset email. Please try again later.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void setupSocialLoginAnimations() {
        // Google Sign In with 360-degree rotation
        googleCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isGoogleAnimating) {
                    isGoogleAnimating = true;
                    perform3DRotation(googleCardView, 360, new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            isGoogleAnimating = false;
                            handleGoogleSignIn();
                        }
                    });
                }
            }
        });

        googleCardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startContinuousRotation(googleCardView);
                return true;
            }
        });

        // Facebook Sign In with 180-degree rotation
        facebookCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFacebookAnimating) {
                    isFacebookAnimating = true;
                    perform3DRotation(facebookCardView, 360, new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            isFacebookAnimating = false;
                            handleFacebookSignIn();
                        }
                    });
                }
            }
        });

        facebookCardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startContinuousRotation(facebookCardView);
                return true;
            }
        });
    }

    private void perform3DRotation(View view, float degrees, AnimatorListenerAdapter listener) {
        // Create rotation animator
        ObjectAnimator rotationY = ObjectAnimator.ofFloat(view, "rotationY", 0f, degrees);
        rotationY.setDuration(1000);
        rotationY.setInterpolator(new AccelerateDecelerateInterpolator());

        // Create scale animators for depth effect
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f);
        scaleX.setDuration(1000);
        scaleY.setDuration(1000);

        // Create elevation animator for shadow effect
        ObjectAnimator elevation = ObjectAnimator.ofFloat(view, "cardElevation", 4f, 12f, 4f);
        elevation.setDuration(1000);

        // Combine all animations
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotationY, scaleX, scaleY, elevation);

        if (listener != null) {
            animatorSet.addListener(listener);
        }

        animatorSet.start();
    }

    private void startContinuousRotation(final View view) {
        ObjectAnimator rotation = ObjectAnimator.ofFloat(view, "rotationY", 0f, 360f);
        rotation.setDuration(3000);
        rotation.setRepeatCount(2); // Rotate 2 times
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.start();
    }

    private void startEntranceAnimation() {
        // Animate Google icon entrance
        googleCardView.setAlpha(0f);
        googleCardView.setScaleX(0.5f);
        googleCardView.setScaleY(0.5f);
        googleCardView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(300)
                .start();

        // Animate Facebook icon entrance with delay
        facebookCardView.setAlpha(0f);
        facebookCardView.setScaleX(0.5f);
        facebookCardView.setScaleY(0.5f);
        facebookCardView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setStartDelay(400)
                .start();

        // Add a subtle rotation on entrance
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                perform3DRotation(googleCardView, 360, null);
            }
        }, 1000);

        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                perform3DRotation(facebookCardView, 360, null);
            }
        }, 1200);
    }

    private void handleGoogleSignIn() {
        showLoadingAnimation(googleCardView);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void recordEmailSignIn(String uid) {
        // Record sign-in to UserStatsManager first
        userStatsManager.recordEmailSignIn();

        // Then sync to Firebase Database
        com.google.firebase.database.DatabaseReference userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users").child(uid);

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastLoginTimestamp", System.currentTimeMillis());
        updates.put("lastSignInMethod", "email_password");
        updates.put("dailySignInCount", userStatsManager.getDailyEntries());
        updates.put("streakCount", userStatsManager.getCurrentStreak());
        updates.put("statsDailyScore", userStatsManager.getTodayMoodScore());

        userRef.updateChildren(updates).addOnCompleteListener(task -> navigateMain());
    }

    private void navigateMain() {
        Intent intent = new Intent(UserSignInActivity.this, UserMainActivity.class);
        startActivity(intent);
        finish();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            navigateMain();
                        } else {
                            simulateSuccessfulLogin("Google");
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(UserSignInActivity.this, "Authentication Failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleFacebookSignIn() {
        // Show loading animation
        showLoadingAnimation(facebookCardView);

        // Simulate Facebook Sign-In process
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UserSignInActivity.this,
                        "Facebook Sign-In initiated! Redirecting...",
                        Toast.LENGTH_SHORT).show();

                // Here you would implement actual Facebook Sign-In SDK
                // For now, we'll just show a success message
                simulateSuccessfulLogin("Facebook");
            }
        }, 1500);
    }

    private void showLoadingAnimation(View view) {
        // Create a pulsing animation while loading
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1f);
        scaleX.setDuration(300);
        scaleY.setDuration(300);
        scaleX.setRepeatCount(3);
        scaleY.setRepeatCount(3);

        AnimatorSet pulseAnimation = new AnimatorSet();
        pulseAnimation.playTogether(scaleX, scaleY);
        pulseAnimation.start();
    }

    private void simulateSuccessfulLogin(String provider) {
        // Create a success animation
        AnimatorSet successAnimation = new AnimatorSet();

        successAnimation.playTogether(
                ObjectAnimator.ofFloat(googleCardView, "alpha", 1f, 0f).setDuration(500),
                ObjectAnimator.ofFloat(facebookCardView, "alpha", 1f, 0f).setDuration(500)
        );

        successAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Toast.makeText(UserSignInActivity.this,
                        "Successfully signed in with " + provider + "!",
                        Toast.LENGTH_LONG).show();

                // Record sign-in and Navigate to main dashboard
                navigateMain();
            }
        });

        successAnimation.start();
    }

    // Circle data class for tracking position and movement
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

                    circleData[i] = new CircleData(decorativeViews[i], x, y, radius);
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
        bubbleAnimationHandler = new Handler(Looper.getMainLooper());
        bubbleAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    updateCirclePositions();
                    checkCollisions();
                    bubbleAnimationHandler.postDelayed(this, 16); // ~60 FPS
                } catch (Exception e) {
                    Log.e(TAG, "Error in animation loop", e);
                }
            }
        };
        bubbleAnimationHandler.post(bubbleAnimationRunnable);
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

    private void startLogoAnimation() {
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ObjectAnimator rotation = ObjectAnimator.ofFloat(logoImage, "rotationY", 0f, 360f);
                rotation.setDuration(1000);
                rotation.setInterpolator(new AccelerateDecelerateInterpolator());
                rotation.start();
            }
        }, 5000);
    }

    /**
     * Stop the animation when activity is destroyed
     */
    private void stopRandomBouncingAnimation() {
        if (bubbleAnimationHandler != null && bubbleAnimationRunnable != null) {
            bubbleAnimationHandler.removeCallbacks(bubbleAnimationRunnable);
            Log.d(TAG, "Stopped random bouncing animation");
        }
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

    @Override
    protected void onPause() {
        super.onPause();
        stopRandomBouncingAnimation();
        Log.d(TAG, "onPause called - animation stopped");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset button state when activity resumes
        resetSignInButton();
        if (circleData != null) {
            startRandomBouncingAnimation();
            Log.d(TAG, "onResume called - animation restarted");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up animation handler
        animationHandler.removeCallbacksAndMessages(null);
        // Stop animation
        stopRandomBouncingAnimation();

        // Clean up animation data
        circleData = null;
        decorativeViews = null;
        bubbleAnimationHandler = null;
        bubbleAnimationRunnable = null;

        Log.d(TAG, "onDestroy called - all resources cleaned up");
    }

    private void resetSignInButton() {
        signInButton.setEnabled(true);
        signInButton.setText("Sign In");
    }
}
