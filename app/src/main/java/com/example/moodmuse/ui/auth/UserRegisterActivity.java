package com.example.moodmuse.ui.auth;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.moodmuse.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class UserRegisterActivity extends AppCompatActivity {

    private static final String TAG = "UserRegisterActivity";

    private LinearLayout stepOneLayout;
    private LinearLayout stepTwoLayout;
    private LinearLayout stepThreeLayout;

    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;

    private CheckBox moodTrackingCheckbox;
    private CheckBox journalingCheckbox;
    private CheckBox meditationCheckbox;
    private CheckBox sleepTrackingCheckbox;

    private MaterialButton nextStepButton;
    private MaterialButton backStepButton;

    private View step1Indicator;
    private View step2Indicator;
    private View step3Indicator;
    private TextView stepIndicator;
    private View backButton;
    private TextView signInLink;
    private ImageView logoImage;

    private int currentStep = 1;

    private Handler bubbleAnimationHandler;
    private Runnable bubbleAnimationRunnable;
    private View[] decorativeViews;
    private CircleData[] circleData;
    private int screenWidth, screenHeight;
    private Handler animationHandler = new Handler();

    private FirebaseAuth mAuth;
    private DatabaseReference rtdbUsersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_register);

        mAuth = FirebaseAuth.getInstance();
        rtdbUsersRef = FirebaseDatabase.getInstance().getReference("users");

        initializeViews();
        addTextWatchers();
        setupClickListeners();
        updateStepVisibility();
        setupFloatingAnimations();
        startLogoAnimation();
    }

    private void initializeViews() {
        stepOneLayout = findViewById(R.id.step1Layout);
        stepTwoLayout = findViewById(R.id.step2Layout);
        stepThreeLayout = findViewById(R.id.step3Layout);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        moodTrackingCheckbox = findViewById(R.id.moodTrackingCheckbox);
        journalingCheckbox = findViewById(R.id.journalingCheckbox);
        meditationCheckbox = findViewById(R.id.meditationCheckbox);
        sleepTrackingCheckbox = findViewById(R.id.sleepTrackingCheckbox);

        nextStepButton = findViewById(R.id.nextStepButton);
        backStepButton = findViewById(R.id.backStepButton);

        step1Indicator = findViewById(R.id.step1Indicator);
        step2Indicator = findViewById(R.id.step2Indicator);
        step3Indicator = findViewById(R.id.step3Indicator);
        stepIndicator = findViewById(R.id.stepTitle);
        backButton = findViewById(R.id.backButton);
        signInLink = findViewById(R.id.signInLink);
        logoImage = findViewById(R.id.logoImage);
    }

    private void addTextWatchers() {
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s) && !TextUtils.isEmpty(s.toString().trim())) {
                    nameInput.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        emailInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s) && android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                    emailInput.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s) && s.length() >= 8) {
                    passwordInput.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        confirmPasswordInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pass = (passwordInput.getText() != null) ? passwordInput.getText().toString() : "";
                if (!TextUtils.isEmpty(s) && s.toString().equals(pass)) {
                    confirmPasswordInput.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        nextStepButton.setOnClickListener(v -> {
            try {
                playClickSound();
                handleNext();
            } catch (Exception e) {
                Log.e(TAG, "Unhandled error on Next click", e);
                Toast.makeText(UserRegisterActivity.this, "An unexpected error occurred. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        backStepButton.setOnClickListener(v -> {
            playClickSound();
            if (currentStep > 1) {
                currentStep--;
                updateStepVisibility();
            }
        });

        backButton.setOnClickListener(v -> finish());
        signInLink.setOnClickListener(v -> finish());
    }

    private void handleNext() {
        if (currentStep == 1) {
            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            if (validateStep1Inputs(name, email)) {
                currentStep = 2;
                updateStepVisibility();
            }
        } else if (currentStep == 2) {
            String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";
            String confirm = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString() : "";
            if (validateStep2Inputs(password, confirm)) {
                currentStep = 3;
                updateStepVisibility();
            }
        } else {
            completeRegistration();
        }
    }

    private boolean validateStep1Inputs(String name, String email) {
        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Full name is required");
            nameInput.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email address");
            emailInput.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateStep2Inputs(String password, String confirmPassword) {
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        } else if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters");
            passwordInput.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Please confirm your password");
            confirmPasswordInput.requestFocus();
            return false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return false;
        }
        return true;
    }

    private void updateStepVisibility() {
        stepIndicator.setText("Step " + currentStep + " of 3");
        updateIndicators();

        stepOneLayout.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
        stepTwoLayout.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
        stepThreeLayout.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);

        backStepButton.setVisibility(currentStep == 1 ? View.GONE : View.VISIBLE);
        if (currentStep == 3) {
            nextStepButton.setText("Register");
            nextStepButton.setIconResource(android.R.drawable.ic_menu_save);
        } else {
            nextStepButton.setText("Next");
            nextStepButton.setIconResource(android.R.drawable.ic_media_next);
        }
    }

    private void updateIndicators() {
        int activeColor = ContextCompat.getColor(this, R.color.md_theme_primaryContainer);
        int inactiveColor = ContextCompat.getColor(this, R.color.auth_divider);

        step1Indicator.setBackgroundColor(currentStep >= 1 ? activeColor : inactiveColor);
        step2Indicator.setBackgroundColor(currentStep >= 2 ? activeColor : inactiveColor);
        step3Indicator.setBackgroundColor(currentStep >= 3 ? activeColor : inactiveColor);
    }

    private void completeRegistration() {
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(UserRegisterActivity.this, "Registration successful. Please sign in.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(UserRegisterActivity.this, UserSignInActivity.class);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        // Save basics to Realtime Database: /users/{uid}
                        Map<String, Object> payload = new HashMap<>();
                        payload.put("name", name);
                        payload.put("email", email);
                        payload.put("createdAt", ServerValue.TIMESTAMP);

                        // Optional: keep your onboarding preferences too
                        if (moodTrackingCheckbox != null) payload.put("prefMoodTracking", moodTrackingCheckbox.isChecked());
                        if (journalingCheckbox != null) payload.put("prefJournaling", journalingCheckbox.isChecked());
                        if (meditationCheckbox != null) payload.put("prefMeditation", meditationCheckbox.isChecked());
                        if (sleepTrackingCheckbox != null) payload.put("prefSleepTracking", sleepTrackingCheckbox.isChecked());

                        rtdbUsersRef.child(user.getUid())
                                .updateChildren(payload)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(UserRegisterActivity.this, "Registration successful. Please sign in.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(UserRegisterActivity.this, UserSignInActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save user profile to Realtime Database", e);
                                    String msg = (e.getMessage() != null) ? e.getMessage() : "Unknown error";
                                    Toast.makeText(
                                            UserRegisterActivity.this,
                                            "Registered, but failed to save profile: " + msg,
                                            Toast.LENGTH_LONG
                                    ).show();
                                    // Stay on this screen so the user can retry or fix connectivity/rules.
                                });
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(UserRegisterActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static class CircleData {
        View view;
        float x, y, velocityX, velocityY, radius;

        CircleData(View view, float x, float y, float radius) {
            this.view = view;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.velocityX = (float) (Math.random() * 16 - 8);
            this.velocityY = (float) (Math.random() * 16 - 8);
        }
    }

    private void setupFloatingAnimations() {
        try {
            screenWidth = getResources().getDisplayMetrics().widthPixels;
            screenHeight = getResources().getDisplayMetrics().heightPixels;

            decorativeViews = new View[]{
                findViewById(R.id.decorCircle1), findViewById(R.id.decorCircle2), findViewById(R.id.decorCircle3),
                findViewById(R.id.decorCircle4), findViewById(R.id.decorCircle5), findViewById(R.id.decorCircle6),
                findViewById(R.id.decorCircle7), findViewById(R.id.decorCircle8), findViewById(R.id.decorCircle9),
                findViewById(R.id.decorCircle10), findViewById(R.id.decorCircle11)
            };

            circleData = new CircleData[decorativeViews.length];
            for (int i = 0; i < decorativeViews.length; i++) {
                if (decorativeViews[i] != null) {
                    decorativeViews[i].measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    float radius = Math.max(decorativeViews[i].getMeasuredWidth(), decorativeViews[i].getMeasuredHeight()) / 2f;
                    float x = (float) (Math.random() * (screenWidth - radius * 2)) + radius;
                    float y = (float) (Math.random() * (screenHeight - radius * 2)) + radius;
                    circleData[i] = new CircleData(decorativeViews[i], x, y, radius);
                }
            }
            startRandomBouncingAnimation();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up floating animations", e);
        }
    }

    private void startRandomBouncingAnimation() {
        bubbleAnimationHandler = new Handler(Looper.getMainLooper());
        bubbleAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    updateCirclePositions();
                    checkCollisions();
                    bubbleAnimationHandler.postDelayed(this, 16);
                } catch (Exception e) {
                    Log.e(TAG, "Error in animation loop", e);
                }
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
        preventOverlaps();
    }

    private void preventOverlaps() {
        for (int i = 0; i < circleData.length; i++) {
            for (int j = i + 1; j < circleData.length; j++) {
                CircleData c1 = circleData[i];
                CircleData c2 = circleData[j];
                if (c1 != null && c2 != null) {
                    float dx = c2.x - c1.x;
                    float dy = c2.y - c1.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    float minDistance = c1.radius + c2.radius + 15;
                    if (distance < minDistance) {
                        float separation = (minDistance - distance) + 15;
                        float nx = dx / distance;
                        float ny = dy / distance;
                        c1.x -= nx * separation / 2f;
                        c1.y -= ny * separation / 2f;
                        c2.x += nx * separation / 2f;
                        c2.y += ny * separation / 2f;
                        c1.velocityX = (float) (Math.random() * 20 - 10);
                        c1.velocityY = (float) (Math.random() * 20 - 10);
                        c2.velocityX = (float) (Math.random() * 20 - 10);
                        c2.velocityY = (float) (Math.random() * 20 - 10);
                    }
                }
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
                    float minDistance = c1.radius + c2.radius + 20;
                    if (distance < minDistance) {
                        float separation = (minDistance - distance) + 10;
                        float nx = dx / distance;
                        float ny = dy / distance;
                        c1.x -= nx * separation / 2f;
                        c1.y -= ny * separation / 2f;
                        c2.x += nx * separation / 2f;
                        c2.y += ny * separation / 2f;
                        c1.velocityX = (float) (Math.random() * 16 - 8);
                        c1.velocityY = (float) (Math.random() * 16 - 8);
                        c2.velocityX = (float) (Math.random() * 16 - 8);
                        c2.velocityY = (float) (Math.random() * 16 - 8);
                    }
                }
            }
        }
    }

    private void startLogoAnimation() {
        animationHandler.postDelayed(() -> {
            ObjectAnimator rotation = ObjectAnimator.ofFloat(logoImage, "rotationY", 0f, 360f);
            rotation.setDuration(1000);
            rotation.setInterpolator(new AccelerateDecelerateInterpolator());
            rotation.start();
        }, 5000);
    }

    private void playClickSound() {
        try {
            android.media.ToneGenerator toneGen = new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 80);
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (circleData != null) {
            startRandomBouncingAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        animationHandler.removeCallbacksAndMessages(null);
        stopRandomBouncingAnimation();
        circleData = null;
        decorativeViews = null;
        bubbleAnimationHandler = null;
        bubbleAnimationRunnable = null;
    }

    private void stopRandomBouncingAnimation() {
        if (bubbleAnimationHandler != null && bubbleAnimationRunnable != null) {
            bubbleAnimationHandler.removeCallbacks(bubbleAnimationRunnable);
        }
    }
}
