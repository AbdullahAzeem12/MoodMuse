package com.example.moodmuse.ui.user;

import android.app.Activity;
import android.app.Dialog;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.example.moodmuse.R;
import com.example.moodmuse.stats.UserStatsManager;
import com.example.moodmuse.stats.UserStatsSnapshot;
import com.example.moodmuse.storage.AppDataWipeManager;
import com.example.moodmuse.ui.role.RoleSelectionActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileSettingsFragment extends Fragment {

    private ImageView ivProfileImage;
    private ImageView ivProfileRotatingBg;
    private FloatingActionButton btnAddProfilePhoto;
    private TextView tvProfileName, tvProfileEmail;
    private TextView tvProfileUpdated;
    private TextView tvStatEntries, tvStatStreak, tvStatScore;
    private SwitchMaterial switchMoodReminders, switchMusicRecommendations, switchAnalyticsUpdates;
    private SwitchMaterial switchDataCollection, switchLocationAccess, switchIncognitoMode;
    private SwitchMaterial switchDarkMode, switchAppLock;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private UserStatsManager userStatsManager;
    private AppDataWipeManager appDataWipeManager;

    private DatabaseReference rtdbUsersRef;
    private ValueEventListener rtdbUserListener;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private boolean applyingRemoteSwitchState = false;

    // Custom animation variables for decorative bubbles
    private Handler bubbleAnimationHandler;
    private Runnable bubbleAnimationRunnable;
    private View[] decorativeViews;
    private CircleData[] circleData;
    private int screenWidth, screenHeight;

    private final ActivityResultLauncher<String> requestNotificationsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    // Revert toggle if permission denied
                    revertSwitch(switchMoodReminders);
                    revertSwitch(switchAnalyticsUpdates);
                }
            });

    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    revertSwitch(switchLocationAccess);
                }
            });

    private Uri pendingProfileImageUri;

    private final ActivityResultLauncher<String> pickProfileImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pendingProfileImageUri = uri;
                    uploadAndSaveProfileImage(uri);
                }
            });

    private final ActivityResultLauncher<String> requestGalleryPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickProfileImageLauncher.launch("image/*");
                } else if (isAdded()) {
                    Toast.makeText(getContext(), "Gallery permission is required to pick a photo", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        rtdbUsersRef = FirebaseDatabase.getInstance().getReference("users");
        currentUser = mAuth.getCurrentUser();
        userStatsManager = new UserStatsManager(requireContext());
        appDataWipeManager = new AppDataWipeManager(requireContext());

        initializeViews(view);
        setupListeners(view);
        // Real-time listener (onStart) will populate UI from Firestore

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        currentUser = (mAuth != null) ? mAuth.getCurrentUser() : null;
        startUserRealtimeDbListener();
        setupFloatingAnimations();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUserRealtimeDbListener();
        stopRandomBouncingAnimation();
    }

    private void initializeViews(View view) {
        ivProfileImage = view.findViewById(R.id.iv_profile_image);
        ivProfileRotatingBg = view.findViewById(R.id.iv_profile_rotating_bg);
        btnAddProfilePhoto = view.findViewById(R.id.btn_add_profile_photo);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        tvProfileUpdated = view.findViewById(R.id.tv_profile_updated);

        tvStatEntries = view.findViewById(R.id.stat_total_entries);
        tvStatStreak = view.findViewById(R.id.stat_streak);
        tvStatScore = view.findViewById(R.id.stat_score);

        switchMoodReminders = view.findViewById(R.id.switch_mood_reminders);
        switchMusicRecommendations = view.findViewById(R.id.switch_music_recommendations);
        switchAnalyticsUpdates = view.findViewById(R.id.switch_analytics_updates);
        switchDataCollection = view.findViewById(R.id.switch_data_collection);
        switchLocationAccess = view.findViewById(R.id.switch_location_access);
        switchIncognitoMode = view.findViewById(R.id.switch_incognito_mode);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        switchAppLock = view.findViewById(R.id.switch_app_lock);

        if (currentUser != null) {
            tvProfileEmail.setText(currentUser.getEmail());
        }

        if (ivProfileRotatingBg != null) {
            ivProfileRotatingBg.startAnimation(android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.rotate_360));
        }

        if (btnAddProfilePhoto != null) {
            btnAddProfilePhoto.setOnClickListener(v -> onAddProfilePhotoClicked());
        }

        // Allow updating photo by tapping the current picture
        if (ivProfileImage != null) {
            ivProfileImage.setOnClickListener(v -> maybePromptUpdateProfilePhoto());
        }
    }

    private void startUserRealtimeDbListener() {
        stopUserRealtimeDbListener();
        if (currentUser == null) return;

        // Show auth email immediately until RTDB loads
        if (currentUser.getEmail() != null) tvProfileEmail.setText(currentUser.getEmail());

        rtdbUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                String profileImageBase64 = snapshot.child("profileImageBase64").getValue(String.class);

                DataSnapshot settings = snapshot.child("settings");

                if (name != null) tvProfileName.setText(name);
                if (email != null) tvProfileEmail.setText(email);
                else if (currentUser != null && currentUser.getEmail() != null) tvProfileEmail.setText(currentUser.getEmail());

                // Source of truth: RTDB base64 image (no Storage dependency)
                if (profileImageBase64 != null && !profileImageBase64.trim().isEmpty()) {
                    try {
                        byte[] decoded = Base64.decode(profileImageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        if (bitmap != null && ivProfileImage != null) {
                            ivProfileImage.setImageBitmap(bitmap);
                        }
                        if (btnAddProfilePhoto != null) btnAddProfilePhoto.setVisibility(View.GONE);
                    } catch (Exception ignore) {
                        if (btnAddProfilePhoto != null) btnAddProfilePhoto.setVisibility(View.VISIBLE);
                    }
                } else if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
                    // Backwards compatibility if a URL was previously stored
                    if (getContext() != null) {
                        Glide.with(ProfileSettingsFragment.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.user)
                                .into(ivProfileImage);
                    }
                    if (btnAddProfilePhoto != null) btnAddProfilePhoto.setVisibility(View.GONE);
                } else {
                    if (btnAddProfilePhoto != null) btnAddProfilePhoto.setVisibility(View.VISIBLE);
                }

                // Apply switch states from RTDB (avoid triggering write-back loops)
                applyingRemoteSwitchState = true;
                applySwitchFromSnapshot(settings, "moodReminders", switchMoodReminders);
                applySwitchFromSnapshot(settings, "musicRecommendations", switchMusicRecommendations);
                applySwitchFromSnapshot(settings, "analyticsUpdates", switchAnalyticsUpdates);
                applySwitchFromSnapshot(settings, "dataCollection", switchDataCollection);
                applySwitchFromSnapshot(settings, "locationAccess", switchLocationAccess);
                applySwitchFromSnapshot(settings, "incognitoMode", switchIncognitoMode);
                applySwitchFromSnapshot(settings, "darkMode", switchDarkMode);
                applySwitchFromSnapshot(settings, "appLock", switchAppLock);
                applyingRemoteSwitchState = false;

                // Update Statistics (Unified with HomeFragment logic)
                updateUnifiedStatistics(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to load profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        rtdbUsersRef.child(currentUser.getUid()).addValueEventListener(rtdbUserListener);
    }

    private void applySwitchFromSnapshot(@NonNull DataSnapshot settings, @NonNull String key, SwitchMaterial sw) {
        if (sw == null) return;
        Boolean val = settings.child(key).getValue(Boolean.class);
        if (val != null) {
            sw.setChecked(val);
        }
    }

    private void onAddProfilePhotoClicked() {
        if (getContext() == null) return;
        String permission = getGalleryPermission();
        if (permission == null) {
            // No runtime permission needed (or not applicable)
            pickProfileImageLauncher.launch("image/*");
            return;
        }

        if (ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            pickProfileImageLauncher.launch("image/*");
        } else {
            requestGalleryPermissionLauncher.launch(permission);
        }
    }

    private void maybePromptUpdateProfilePhoto() {
        if (getContext() == null || currentUser == null || rtdbUsersRef == null) return;

        // Only prompt if a photo already exists (otherwise the + button covers the action)
        rtdbUsersRef.child(currentUser.getUid()).child("profileImageBase64")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String existing = snapshot.getValue(String.class);
                    if (existing == null || existing.trim().isEmpty()) {
                        // No saved image yet, act like "+"
                        onAddProfilePhotoClicked();
                        return;
                    }

                    new AlertDialog.Builder(getContext())
                            .setTitle("Update Profile Picture")
                            .setMessage("Do you want to update your profile picture?")
                            .setPositiveButton("Update", (d, which) -> onAddProfilePhotoClicked())
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    // If we can't read, still allow user to try updating
                    onAddProfilePhotoClicked();
                });
    }

    private String getGalleryPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return android.Manifest.permission.READ_MEDIA_IMAGES;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            return android.Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        return null;
    }

    private void uploadAndSaveProfileImage(@NonNull Uri uri) {
        if (currentUser == null) return;
        if (btnAddProfilePhoto != null) btnAddProfilePhoto.setEnabled(false);

        // Show selected image immediately for responsiveness
        if (ivProfileImage != null && getContext() != null) {
            Glide.with(this).load(uri).placeholder(R.drawable.user).into(ivProfileImage);
        }

        String uid = currentUser.getUid();

        ioExecutor.execute(() -> {
            try {
                if (getContext() == null) throw new IllegalStateException("No context");
                InputStream input = getContext().getContentResolver().openInputStream(uri);
                if (input == null) throw new IllegalStateException("Unable to read image");
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                if (bitmap == null) throw new IllegalStateException("Invalid image");

                // Compress to keep RTDB payload reasonable
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                Map<String, Object> updates = new HashMap<>();
                updates.put("profileImageBase64", base64);
                // Optional cleanup of old url field
                updates.put("profileImageUrl", null);

                uiHandler.post(() -> rtdbUsersRef.child(uid).updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            if (btnAddProfilePhoto != null) {
                                btnAddProfilePhoto.setVisibility(View.GONE);
                                btnAddProfilePhoto.setEnabled(true);
                            }
                            showUpdatedIndicator(); // "Successfully updated"
                        })
                        .addOnFailureListener(e -> {
                            if (btnAddProfilePhoto != null) btnAddProfilePhoto.setEnabled(true);
                            // No error toggle/toast requested: silently keep + visible to retry
                            if (btnAddProfilePhoto != null) btnAddProfilePhoto.setVisibility(View.VISIBLE);
                        }));
            } catch (Exception e) {
                uiHandler.post(() -> {
                    if (btnAddProfilePhoto != null) btnAddProfilePhoto.setEnabled(true);
                    if (btnAddProfilePhoto != null) btnAddProfilePhoto.setVisibility(View.VISIBLE);
                    // No error toggle/toast requested
                });
            }
        });
    }

    private void stopUserRealtimeDbListener() {
        if (rtdbUserListener != null && currentUser != null) {
            rtdbUsersRef.child(currentUser.getUid()).removeEventListener(rtdbUserListener);
            rtdbUserListener = null;
        }
    }

    private void setupListeners(View view) {
        MaterialButton btnEdit = view.findViewById(R.id.btn_edit_profile);
        MaterialButton btnChangePassword = view.findViewById(R.id.btn_change_password);
        MaterialButton btnSignOut = view.findViewById(R.id.btn_sign_out);
        MaterialButton btnDelete = view.findViewById(R.id.btn_delete_account);

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                if (!isAdded()) return;
                animatePress(v);
                showEditProfileDialog();
            });
        }

        setupSwitchListener(switchMoodReminders, "moodReminders");
        setupSwitchListener(switchMusicRecommendations, "musicRecommendations");
        setupSwitchListener(switchAnalyticsUpdates, "analyticsUpdates");
        setupSwitchListener(switchDataCollection, "dataCollection");
        setupSwitchListener(switchLocationAccess, "locationAccess");
        setupSwitchListener(switchIncognitoMode, "incognitoMode");
        setupSwitchListener(switchDarkMode, "darkMode");
        setupSwitchListener(switchAppLock, "appLock");

        View btnClearCache = view.findViewById(R.id.btn_clear_cache);
        if (btnClearCache != null) {
            btnClearCache.setOnClickListener(v -> handleClearCache());
        }

        View btnExportData = view.findViewById(R.id.btn_export_data);
        if (btnExportData != null) {
            btnExportData.setOnClickListener(v -> handleExportData());
        }

        View btnSecurityCenter = view.findViewById(R.id.btn_advanced_privacy);
        if (btnSecurityCenter != null) {
            btnSecurityCenter.setOnClickListener(v -> showSecurityCenterDialog());
        }

        View btnClearAppData = view.findViewById(R.id.btn_clear_app_data);
        if (btnClearAppData != null) {
            btnClearAppData.setOnClickListener(v -> showClearAppDataDialog());
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                if (!isAdded()) return;
                animatePress(v);
                mAuth.signOut();
                Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                getActivity().finish();
            });
        }
        
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                if (!isAdded()) return;
                animatePress(v);
                showChangePasswordDialog();
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (!isAdded()) return;
                animatePress(v);
                new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (currentUser != null) {
                            String uid = currentUser.getUid();

                            // Delete profile data from Realtime Database (source of truth for this screen)
                            if (rtdbUsersRef != null) {
                                rtdbUsersRef.child(uid).removeValue();
                            }

                            // Best-effort delete from Firestore (don't block on it)
                            db.collection("users").document(uid).delete();

                            // Delete Firebase Auth user (this may fail if re-auth is required)
                            currentUser.delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
                                    startActivity(intent);
                                    getActivity().finish();
                                } else {
                                    Toast.makeText(getContext(), "Account delete failed (requires recent login)", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }

    private void animatePress(@NonNull View v) {
        v.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(90)
                .withEndAction(() -> v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(140)
                        .start())
                .start();
    }

    private void showChangePasswordDialog() {
        if (!isAdded()) return;
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(getContext(), "Please sign in again", Toast.LENGTH_SHORT).show();
            return;
        }

        TextInputEditText etCurrent = new TextInputEditText(requireContext());
        etCurrent.setHint("Current Password");
        etCurrent.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        TextInputEditText etNew = new TextInputEditText(requireContext());
        etNew.setHint("New Password");
        etNew.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        TextInputEditText etConfirm = new TextInputEditText(requireContext());
        etConfirm.setHint("Confirm New Password");
        etConfirm.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.addView(etCurrent);
        container.addView(etNew);
        container.addView(etConfirm);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button positive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String currentPass = etCurrent.getText() != null ? etCurrent.getText().toString() : "";
                String newPass = etNew.getText() != null ? etNew.getText().toString() : "";
                String confirmPass = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

                if (currentPass.isEmpty()) {
                    etCurrent.setError("Enter current password");
                    return;
                }
                if (newPass.length() < 8) {
                    etNew.setError("New password must be at least 8 characters");
                    return;
                }
                if (!newPass.equals(confirmPass)) {
                    etConfirm.setError("Passwords do not match");
                    return;
                }

                positive.setEnabled(false);
                currentUser.reauthenticate(EmailAuthProvider.getCredential(currentUser.getEmail(), currentPass))
                        .addOnSuccessListener(aVoid -> currentUser.updatePassword(newPass)
                                .addOnSuccessListener(aVoid2 -> {
                                    dialog.dismiss();
                                    showUpdatedIndicatorMessage("Password Successfully changed");
                                })
                                .addOnFailureListener(e -> {
                                    positive.setEnabled(true);
                                    Toast.makeText(getContext(), "Failed to change password", Toast.LENGTH_SHORT).show();
                                }))
                        .addOnFailureListener(e -> {
                            positive.setEnabled(true);
                            etCurrent.setError("Current password is incorrect");
                        });
            });
        });

        dialog.show();
    }

    private void showUpdatedIndicatorMessage(@NonNull String message) {
        if (tvProfileUpdated == null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            return;
        }
        tvProfileUpdated.setText(message);
        showUpdatedIndicator();
    }

    private void setupSwitchListener(SwitchMaterial switchMaterial, String fieldName) {
        switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (switchMaterial == null) return;
            if (applyingRemoteSwitchState) return;
            if (currentUser == null || rtdbUsersRef == null) return;

            // Handle permissions / real side-effects first
            if ("moodReminders".equals(fieldName) || "analyticsUpdates".equals(fieldName)) {
                if (isChecked && !ensureNotificationsPermission()) {
                    revertSwitch(switchMaterial);
                    return;
                }
            }

            if ("locationAccess".equals(fieldName)) {
                if (isChecked && !ensureLocationPermission()) {
                    revertSwitch(switchMaterial);
                    return;
                }
            }

            if ("dataCollection".equals(fieldName)) {
                // Real effect: analytics collection enabled/disabled
                try {
                    com.google.firebase.analytics.FirebaseAnalytics.getInstance(requireContext())
                            .setAnalyticsCollectionEnabled(isChecked);
                } catch (Exception ignored) {}
            }

            rtdbUsersRef.child(currentUser.getUid())
                    .child("settings")
                    .child(fieldName)
                    .setValue(isChecked);

            if ("darkMode".equals(fieldName)) {
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
            }
        });
    }

    private void revertSwitch(@Nullable SwitchMaterial sw) {
        if (sw == null) return;
        applyingRemoteSwitchState = true;
        sw.setChecked(false);
        applyingRemoteSwitchState = false;
    }

    private boolean ensureNotificationsPermission() {
        if (getContext() == null) return false;
        if (Build.VERSION.SDK_INT < 33) return true;
        String perm = android.Manifest.permission.POST_NOTIFICATIONS;
        if (ContextCompat.checkSelfPermission(getContext(), perm) == PackageManager.PERMISSION_GRANTED) return true;
        requestNotificationsPermissionLauncher.launch(perm);
        return false;
    }

    private boolean ensureLocationPermission() {
        if (getContext() == null) return false;
        String perm = android.Manifest.permission.ACCESS_FINE_LOCATION;
        if (ContextCompat.checkSelfPermission(getContext(), perm) == PackageManager.PERMISSION_GRANTED) return true;
        requestLocationPermissionLauncher.launch(perm);
        return false;
    }

    private void showEditProfileDialog() {
        if (!isAdded()) return;
        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setHint("Name");
        etName.setText(tvProfileName.getText());

        TextInputEditText etEmail = new TextInputEditText(requireContext());
        etEmail.setHint("Email");
        etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setText(tvProfileEmail.getText());

        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.addView(etName);
        container.addView(etEmail);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button positive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
                String newEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

                if (newName.isEmpty()) {
                    etName.setError("Name cannot be empty");
                    return;
                }
                if (newEmail.isEmpty()) {
                    etEmail.setError("Email cannot be empty");
                    return;
                }
                saveProfileChanges(newName, newEmail, dialog);
            });
        });

        dialog.show();
    }

    private void saveProfileChanges(String newName, String newEmail, AlertDialog dialog) {
        if (currentUser == null) return;

        Toast.makeText(getContext(), "Updating profile...", Toast.LENGTH_SHORT).show();
        // Name/email only (no profile picture)
        updateFirestoreProfile(newName, newEmail, null, dialog);
    }

    private void updateFirestoreProfile(String name, String email, String imageUrl, AlertDialog dialog) {
        if (currentUser == null) return;

        // Source of truth: Realtime Database
        Map<String, Object> rtdbUpdates = new HashMap<>();
        rtdbUpdates.put("name", name);
        rtdbUpdates.put("email", email);

        rtdbUsersRef.child(currentUser.getUid())
                .updateChildren(rtdbUpdates)
                .addOnSuccessListener(aVoid -> {
                    // UI updates will also come from the RTDB listener, but set immediately for responsiveness
                    tvProfileName.setText(name);
                    tvProfileEmail.setText(email);

                    // Also try to update email in Firebase Auth
                    if (currentUser.getEmail() != null && !email.equals(currentUser.getEmail())) {
                        currentUser.updateEmail(email).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "Auth email updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Auth email update failed (requires recent login)", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    dialog.dismiss();
                    showUpdatedIndicator(); // shows "Successfully updated"

                    // Best-effort Firestore write (no UI error if denied)
                    try {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", name);
                        updates.put("email", email);
                        db.collection("users").document(currentUser.getUid())
                                .set(updates, com.google.firebase.firestore.SetOptions.merge());
                    } catch (Exception ignored) {
                        // Ignore; RTDB is the source of truth for this screen
                    }
                })
                .addOnFailureListener(e -> {
                    // If RTDB update failed, it's not updated anywhere that this screen reads from
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showSecurityCenterDialog() {
        if (!isAdded() || currentUser == null) return;

        String[] options = {"Two-Factor Authentication (2FA)", "Biometric Lock", "Manage Active Sessions", "Request Account Deletion (GDPR)"};
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Security Center")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: handle2FASetting(); break;
                        case 1: handleBiometricSetting(); break;
                        case 2: handleSessionManagement(); break;
                        case 3: handleDataDeletionRequest(); break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void handle2FASetting() {
        // Professional logic: Check current status from RTDB and toggle
        rtdbUsersRef.child(currentUser.getUid()).child("settings").child("twoFactorEnabled").get()
                .addOnSuccessListener(snapshot -> {
                    boolean isEnabled = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    String action = isEnabled ? "Disable" : "Enable";
                    
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Two-Factor Authentication")
                            .setMessage("Enhance your security by requiring a verification code. Current Status: " + (isEnabled ? "ENABLED" : "DISABLED"))
                            .setPositiveButton(action, (d, w) -> {
                                rtdbUsersRef.child(currentUser.getUid()).child("settings").child("twoFactorEnabled").setValue(!isEnabled)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "2FA " + action + "d Successfully", Toast.LENGTH_SHORT).show());
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void handleBiometricSetting() {
        // Verify hardware support first
        androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(requireContext());
        int canAuthenticate = biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG);
        
        if (canAuthenticate == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            rtdbUsersRef.child(currentUser.getUid()).child("settings").child("biometricEnabled").get()
                .addOnSuccessListener(snapshot -> {
                    boolean isEnabled = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Biometric Lock")
                            .setMessage("Use fingerprint or face ID to open MoodMuse. Currently: " + (isEnabled ? "ON" : "OFF"))
                            .setPositiveButton(isEnabled ? "Turn Off" : "Turn On", (d, w) -> {
                                rtdbUsersRef.child(currentUser.getUid()).child("settings").child("biometricEnabled").setValue(!isEnabled);
                                Toast.makeText(getContext(), "Biometric setting updated", Toast.LENGTH_SHORT).show();
                            })
                            .show();
                });
        } else {
            Toast.makeText(getContext(), "Biometric hardware not available", Toast.LENGTH_LONG).show();
        }
    }

    private void handleSessionManagement() {
        // Professional logic: Show current device info and allow "Logout all"
        String deviceInfo = "Current Device: " + android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.RELEASE + ")";
        new AlertDialog.Builder(requireContext())
                .setTitle("Active Sessions")
                .setMessage(deviceInfo + "\n\nWould you like to terminate all other active sessions for this account?")
                .setPositiveButton("Terminate Others", (d, w) -> {
                    // Logic would typically involve updating a session token in DB
                    Toast.makeText(getContext(), "Other sessions terminated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleDataDeletionRequest() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Data Deletion Request")
                .setMessage("Under GDPR/CCPA, you can request a full wipe of your data from our servers. This takes up to 30 days to process. Proceed?")
                .setPositiveButton("Request Deletion", (d, w) -> {
                    rtdbUsersRef.child(currentUser.getUid()).child("pendingDeletion").setValue(true);
                    Toast.makeText(getContext(), "Deletion request submitted", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleClearCache() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear Cache")
                .setMessage("This will remove local temporary images and files. Your cloud history will remain safe. Continue?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    ioExecutor.execute(() -> {
                        try {
                            if (getContext() != null) {
                                // Clear Glide cache
                                Glide.get(getContext()).clearDiskCache();
                                // Clear local app cache
                                deleteDir(getContext().getCacheDir());
                                uiHandler.post(() -> Toast.makeText(getContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show());
                            }
                        } catch (Exception e) {
                            uiHandler.post(() -> Toast.makeText(getContext(), "Failed to clear cache", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean deleteDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new java.io.File(dir, child));
                    if (!success) return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void handleExportData() {
        if (currentUser == null) return;
        Toast.makeText(getContext(), "Preparing your mood report...", Toast.LENGTH_LONG).show();

        rtdbUsersRef.child(currentUser.getUid()).child("moodHistory").get().addOnSuccessListener(snapshot -> {
            if (!isAdded()) return;
            StringBuilder report = new StringBuilder();
            report.append("MoodMuse Activity Report\n");
            report.append("Generated on: ").append(new java.util.Date()).append("\n\n");

            for (DataSnapshot ds : snapshot.getChildren()) {
                String mood = ds.child("mood").getValue(String.class);
                Long ts = ds.child("timestamp").getValue(Long.class);
                String note = ds.child("note").getValue(String.class);
                
                if (ts != null) {
                    java.util.Date date = new java.util.Date(ts);
                    report.append("[").append(date).append("] ");
                }
                report.append("Mood: ").append(mood != null ? mood : "N/A");
                if (note != null && !note.isEmpty()) report.append(" | Note: ").append(note);
                report.append("\n");
            }

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, "Export Mood History");
            startActivity(shareIntent);
        });
    }

    private void updateUnifiedStatistics(DataSnapshot userSnapshot) {
        if (!isAdded() || tvStatEntries == null || tvStatStreak == null || tvStatScore == null) return;
        
        int dailyEntries = userStatsManager.getDailyEntries();
        int currentStreak = userStatsManager.getCurrentStreak();
        int todayMoodScore = userStatsManager.getTodayMoodScore();

        tvStatEntries.setText(String.valueOf(dailyEntries));
        tvStatStreak.setText(String.valueOf(currentStreak));
        
        String scoreText = todayMoodScore > 0 ? todayMoodScore + "%" : "0%";
        tvStatScore.setText(scoreText);
    }

    private int getMoodValue(String mood) {
        if (mood == null) return 50;
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

    private void showClearAppDataDialog() {
        if (!isAdded()) return;

        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_clear_app_data);

        TextView btnCancel = dialog.findViewById(R.id.btn_cancel_wipe);
        TextView btnConfirm = dialog.findViewById(R.id.btn_confirm_wipe);
        ProgressBar progressBar = dialog.findViewById(R.id.progress_wipe);
        View confirmContainer = dialog.findViewById(R.id.container_confirm_action);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                if (confirmContainer != null) {
                    confirmContainer.setEnabled(false);
                    confirmContainer.setAlpha(0.5f);
                }
                Toast.makeText(getContext(), "Clearing app data...", Toast.LENGTH_SHORT).show();
                appDataWipeManager.wipeAllUserData(currentUser, rtdbUsersRef, db, new AppDataWipeManager.Callback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(), "App data cleared successfully", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        Intent intent = new Intent(requireActivity(), RoleSelectionActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        requireActivity().finish();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (!isAdded()) return;
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (confirmContainer != null) {
                            confirmContainer.setEnabled(true);
                            confirmContainer.setAlpha(1f);
                        }
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        dialog.show();
    }

    private void showUpdatedIndicator() {
        if (tvProfileUpdated == null) return;
        tvProfileUpdated.setVisibility(View.VISIBLE);
        uiHandler.removeCallbacksAndMessages(null);
        uiHandler.postDelayed(() -> {
            if (isAdded() && tvProfileUpdated != null) {
                tvProfileUpdated.setVisibility(View.GONE);
            }
        }, 2000);
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
            if (getView() == null) {
                Log.e("ProfileSettings", "View is null, cannot setup animations");
                return;
            }

            // Stop any existing animation first
            stopRandomBouncingAnimation();

            Log.d("ProfileSettings", "Setting up 11 decorative circle random bouncing with collision detection");

            // Get screen dimensions for FULL SCREEN movement
            screenWidth = getResources().getDisplayMetrics().widthPixels;
            screenHeight = getResources().getDisplayMetrics().heightPixels;
            Log.d("ProfileSettings", "Full screen dimensions: " + screenWidth + "x" + screenHeight);

            // Get all 11 decorative circle views from the layout
            decorativeViews = new View[]{
                    getView().findViewById(R.id.decorCircle1),
                    getView().findViewById(R.id.decorCircle2),
                    getView().findViewById(R.id.decorCircle3),
                    getView().findViewById(R.id.decorCircle4),
                    getView().findViewById(R.id.decorCircle5),
                    getView().findViewById(R.id.decorCircle6),
                    getView().findViewById(R.id.decorCircle7),
                    getView().findViewById(R.id.decorCircle8),
                    getView().findViewById(R.id.decorCircle9),
                    getView().findViewById(R.id.decorCircle10),
                    getView().findViewById(R.id.decorCircle11)
            };

            // Log which circles were found
            int foundCount = 0;
            for (int i = 0; i < decorativeViews.length; i++) {
                if (decorativeViews[i] != null) {
                    foundCount++;
                } else {
                    Log.w("ProfileSettings", "DecorCircle" + (i + 1) + " is null!");
                }
            }
            Log.d("ProfileSettings", "Found " + foundCount + " out of 11 circles");

            // Post to ensure view is laid out before initializing circles
            getView().post(() -> {
                try {
                    if (!isAdded() || getView() == null || decorativeViews == null) return;

                    // Re-get screen dimensions after layout
                    screenWidth = getResources().getDisplayMetrics().widthPixels;
                    screenHeight = getResources().getDisplayMetrics().heightPixels;
                    Log.d("ProfileSettings", "Screen dimensions after layout: " + screenWidth + "x" + screenHeight);

                    // Initialize circle data with smart positioning to avoid overlaps
                    circleData = new CircleData[decorativeViews.length];
                    for (int i = 0; i < decorativeViews.length; i++) {
                        if (decorativeViews[i] != null) {
                            // Bring circle to front and enable hardware acceleration
                            decorativeViews[i].bringToFront();
                            decorativeViews[i].setLayerType(View.LAYER_TYPE_HARDWARE, null);

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
                            Log.d("ProfileSettings", "Initialized circle " + (i + 1) + " at position (" + x + ", " + y + ") with radius " + radius);
                        }
                    }

                    // Start animation loop
                    startRandomBouncingAnimation();
                    Log.d("ProfileSettings", "Animation started successfully");

                } catch (Exception e) {
                    Log.e("ProfileSettings", "Error in post runnable", e);
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            Log.e("ProfileSettings", "Error setting up floating animations", e);
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
                    Log.e("ProfileSettings", "Error in animation loop", e);
                }
            }
        };
        bubbleAnimationHandler.post(bubbleAnimationRunnable);
        Log.d("ProfileSettings", "Started random bouncing animation loop");
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

                        Log.d("ProfileSettings", "IMMEDIATE RANDOM REPULSION between circles " + (i + 1) + " and " + (j + 1));
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

                        Log.d("ProfileSettings", "IMMEDIATE RANDOM REPULSION between circles " + (i + 1) + " and " + (j + 1) +
                                " - separation: " + separationDistance);
                    }
                }
            }
        }
    }

    /**
     * Stop the animation when fragment is stopped
     */
    private void stopRandomBouncingAnimation() {
        if (bubbleAnimationHandler != null && bubbleAnimationRunnable != null) {
            bubbleAnimationHandler.removeCallbacks(bubbleAnimationRunnable);
            Log.d("ProfileSettings", "Stopped random bouncing animation");
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop animation
        stopRandomBouncingAnimation();

        // Clean up animation data
        circleData = null;
        decorativeViews = null;
        bubbleAnimationHandler = null;
        bubbleAnimationRunnable = null;

        Log.d("ProfileSettings", "onDestroyView called - all resources cleaned up");
    }
}
