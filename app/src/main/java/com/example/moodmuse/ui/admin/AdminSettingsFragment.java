package com.example.moodmuse.ui.admin;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.moodmuse.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AdminSettingsFragment - Comprehensive settings and preferences management
 * Features:
 * - Theme toggle (Light/Dark/Auto)
 * - Notification preferences
 * - Admin-specific settings
 * - App configuration
 * - Data management
 * - Account security settings
 * - Backup and restore options
 * - About information
 */
public class AdminSettingsFragment extends Fragment {

    // UI Components - Theme Settings
    private SwitchMaterial switchDarkMode;
    private TextView tvThemeStatus;
    
    // UI Components - Notification Settings
    private SwitchMaterial switchNotifications, switchEmailNotifications, 
                          switchPushNotifications, switchUserActivity;
    private Slider sliderNotificationFrequency;
    private TextView tvNotificationFrequency;
    
    // UI Components - Admin Settings
    private SwitchMaterial switchAutoApprove, switchMaintenanceMode, 
                          switchDebugMode, switchAnalytics;
    private TextView tvMaintenanceStatus, tvDebugStatus;
    
    // UI Components - Data Settings
    private TextView tvCacheSize, tvLastBackup, tvAppVersion;
    private MaterialButton btnClearCache, btnBackupData, btnRestoreData, 
                          btnExportLogs, btnResetSettings;
    
    // UI Components - Security Settings
    private SwitchMaterial switchTwoFactor, switchSessionTimeout, switchAuditLog;
    private TextView tvSessionTimeout;
    
    // Cards
    private MaterialCardView cardTheme, cardNotifications, cardAdmin, 
                            cardData, cardSecurity, cardAbout;
    
    // SharedPreferences
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "MoodMuseSettings";
    
    // Settings Keys
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_EMAIL_NOTIF = "email_notifications";
    private static final String KEY_PUSH_NOTIF = "push_notifications";
    private static final String KEY_USER_ACTIVITY = "user_activity_notif";
    private static final String KEY_NOTIF_FREQ = "notification_frequency";
    private static final String KEY_AUTO_APPROVE = "auto_approve";
    private static final String KEY_MAINTENANCE = "maintenance_mode";
    private static final String KEY_DEBUG = "debug_mode";
    private static final String KEY_ANALYTICS = "analytics_enabled";
    private static final String KEY_TWO_FACTOR = "two_factor_auth";
    private static final String KEY_SESSION_TIMEOUT = "session_timeout";
    private static final String KEY_AUDIT_LOG = "audit_log_enabled";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializePreferences();
        initializeViews(view);
        loadSettings();
        setupListeners();
        animateEntrance();
    }

    /**
     * Initialize SharedPreferences
     */
    private void initializePreferences() {
        preferences = requireContext().getSharedPreferences(PREFS_NAME, 
            android.content.Context.MODE_PRIVATE);
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews(View view) {
        // Theme Settings
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        tvThemeStatus = view.findViewById(R.id.tvThemeStatus);
        
        // Notification Settings
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchEmailNotifications = view.findViewById(R.id.switchEmailNotifications);
        switchPushNotifications = view.findViewById(R.id.switchPushNotifications);
        switchUserActivity = view.findViewById(R.id.switchUserActivity);
        sliderNotificationFrequency = view.findViewById(R.id.sliderNotificationFrequency);
        tvNotificationFrequency = view.findViewById(R.id.tvNotificationFrequency);
        
        // Admin Settings
        switchAutoApprove = view.findViewById(R.id.switchAutoApprove);
        switchMaintenanceMode = view.findViewById(R.id.switchMaintenanceMode);
        switchDebugMode = view.findViewById(R.id.switchDebugMode);
        switchAnalytics = view.findViewById(R.id.switchAnalytics);
        tvMaintenanceStatus = view.findViewById(R.id.tvMaintenanceStatus);
        tvDebugStatus = view.findViewById(R.id.tvDebugStatus);
        
        // Data Settings
        tvCacheSize = view.findViewById(R.id.tvCacheSize);
        tvLastBackup = view.findViewById(R.id.tvLastBackup);
        tvAppVersion = view.findViewById(R.id.tvAppVersion);
        btnClearCache = view.findViewById(R.id.btnClearCache);
        btnBackupData = view.findViewById(R.id.btnBackupData);
        btnRestoreData = view.findViewById(R.id.btnRestoreData);
        btnExportLogs = view.findViewById(R.id.btnExportLogs);
        btnResetSettings = view.findViewById(R.id.btnResetSettings);
        
        // Security Settings
        switchTwoFactor = view.findViewById(R.id.switchTwoFactor);
        switchSessionTimeout = view.findViewById(R.id.switchSessionTimeout);
        switchAuditLog = view.findViewById(R.id.switchAuditLog);
        tvSessionTimeout = view.findViewById(R.id.tvSessionTimeout);
        
        // Cards
        cardTheme = view.findViewById(R.id.cardTheme);
        cardNotifications = view.findViewById(R.id.cardNotifications);
        cardAdmin = view.findViewById(R.id.cardAdmin);
        cardData = view.findViewById(R.id.cardData);
        cardSecurity = view.findViewById(R.id.cardSecurity);
        cardAbout = view.findViewById(R.id.cardAbout);
        
        // Set initial values
        updateCacheSize();
        updateLastBackup();
        updateAppVersion();
    }

    /**
     * Load saved settings from SharedPreferences
     */
    private void loadSettings() {
        // Theme
        boolean darkMode = preferences.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(darkMode);
        updateThemeStatus(darkMode);
        
        // Notifications
        switchNotifications.setChecked(preferences.getBoolean(KEY_NOTIFICATIONS, true));
        switchEmailNotifications.setChecked(preferences.getBoolean(KEY_EMAIL_NOTIF, true));
        switchPushNotifications.setChecked(preferences.getBoolean(KEY_PUSH_NOTIF, true));
        switchUserActivity.setChecked(preferences.getBoolean(KEY_USER_ACTIVITY, false));
        
        float notifFreq = preferences.getFloat(KEY_NOTIF_FREQ, 3f);
        sliderNotificationFrequency.setValue(notifFreq);
        updateNotificationFrequency(notifFreq);
        
        // Admin Settings
        switchAutoApprove.setChecked(preferences.getBoolean(KEY_AUTO_APPROVE, false));
        switchMaintenanceMode.setChecked(preferences.getBoolean(KEY_MAINTENANCE, false));
        switchDebugMode.setChecked(preferences.getBoolean(KEY_DEBUG, false));
        switchAnalytics.setChecked(preferences.getBoolean(KEY_ANALYTICS, true));
        
        updateMaintenanceStatus(switchMaintenanceMode.isChecked());
        updateDebugStatus(switchDebugMode.isChecked());
        
        // Security
        switchTwoFactor.setChecked(preferences.getBoolean(KEY_TWO_FACTOR, false));
        switchSessionTimeout.setChecked(preferences.getBoolean(KEY_SESSION_TIMEOUT, true));
        switchAuditLog.setChecked(preferences.getBoolean(KEY_AUDIT_LOG, true));
    }

    /**
     * Setup all listeners
     */
    private void setupListeners() {
        // Theme Settings
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                toggleTheme(isChecked);
            }
        });
        
        // Notification Settings
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting(KEY_NOTIFICATIONS, isChecked);
                toggleNotificationControls(isChecked);
                showToast(isChecked ? "Notifications enabled" : "Notifications disabled");
            }
        });
        
        switchEmailNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting(KEY_EMAIL_NOTIF, isChecked);
                showToast("Email notifications " + (isChecked ? "enabled" : "disabled"));
            }
        });
        
        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting(KEY_PUSH_NOTIF, isChecked);
                showToast("Push notifications " + (isChecked ? "enabled" : "disabled"));
            }
        });
        
        switchUserActivity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting(KEY_USER_ACTIVITY, isChecked);
                showToast("User activity notifications " + (isChecked ? "enabled" : "disabled"));
            }
        });
        
        sliderNotificationFrequency.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                updateNotificationFrequency(value);
                preferences.edit().putFloat(KEY_NOTIF_FREQ, value).apply();
            }
        });
        
        // Admin Settings
        switchAutoApprove.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting(KEY_AUTO_APPROVE, isChecked);
                showToast("Auto-approve " + (isChecked ? "enabled" : "disabled"));
            }
        });
        
        switchMaintenanceMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                showMaintenanceModeDialog(isChecked);
            }
        });
        
        switchDebugMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting(KEY_DEBUG, isChecked);
                updateDebugStatus(isChecked);
                showToast("Debug mode " + (isChecked ? "enabled" : "disabled"));
            }
        });
        
        switchAnalytics.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting(KEY_ANALYTICS, isChecked);
                showToast("Analytics " + (isChecked ? "enabled" : "disabled"));
            }
        });
        
        // Security Settings
        switchTwoFactor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting(KEY_TWO_FACTOR, isChecked);
                showToast("Two-factor authentication " + (isChecked ? "enabled" : "disabled"));
            }
        });
        
        switchSessionTimeout.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    showSessionTimeoutDialog();
                } else {
                    saveSetting(KEY_SESSION_TIMEOUT, false);
                    showToast("Session timeout disabled");
                }
            }
        });
        
        switchAuditLog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                saveSetting(KEY_AUDIT_LOG, isChecked);
                showToast("Audit logging " + (isChecked ? "enabled" : "disabled"));
            }
        });
        
        // Data Management Buttons
        btnClearCache.setOnClickListener(v -> showClearCacheDialog());
        btnBackupData.setOnClickListener(v -> performBackup());
        btnRestoreData.setOnClickListener(v -> showRestoreDialog());
        btnExportLogs.setOnClickListener(v -> exportLogs());
        btnResetSettings.setOnClickListener(v -> showResetDialog());
    }

    /**
     * Toggle theme
     */
    private void toggleTheme(boolean isDark) {
        preferences.edit().putBoolean(KEY_DARK_MODE, isDark).apply();
        
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        updateThemeStatus(isDark);
        showToast("Theme changed to " + (isDark ? "Dark" : "Light") + " mode");
    }

    /**
     * Update theme status text
     */
    private void updateThemeStatus(boolean isDark) {
        if (tvThemeStatus != null) {
            tvThemeStatus.setText(isDark ? "Dark mode active" : "Light mode active");
        }
    }

    /**
     * Toggle notification controls
     */
    private void toggleNotificationControls(boolean enabled) {
        switchEmailNotifications.setEnabled(enabled);
        switchPushNotifications.setEnabled(enabled);
        switchUserActivity.setEnabled(enabled);
        sliderNotificationFrequency.setEnabled(enabled);
    }

    /**
     * Update notification frequency text
     */
    private void updateNotificationFrequency(float value) {
        String[] frequencies = {"Instant", "Every 15 min", "Hourly", "Daily", "Weekly"};
        int index = (int) value;
        if (index >= 0 && index < frequencies.length && tvNotificationFrequency != null) {
            tvNotificationFrequency.setText(frequencies[index]);
        }
    }

    /**
     * Update maintenance status
     */
    private void updateMaintenanceStatus(boolean enabled) {
        if (tvMaintenanceStatus != null) {
            tvMaintenanceStatus.setText(enabled ? 
                "⚠️ Maintenance mode is active" : "System operational");
            tvMaintenanceStatus.setTextColor(requireContext().getColor(
                enabled ? R.color.admin_warning : R.color.admin_success));
        }
    }

    /**
     * Update debug status
     */
    private void updateDebugStatus(boolean enabled) {
        if (tvDebugStatus != null) {
            tvDebugStatus.setText(enabled ? "Debug logs enabled" : "Debug logs disabled");
        }
    }

    /**
     * Show maintenance mode confirmation dialog
     */
    private void showMaintenanceModeDialog(boolean enable) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(enable ? "Enable Maintenance Mode?" : "Disable Maintenance Mode?")
            .setMessage(enable ? 
                "This will prevent regular users from accessing the app. Only admins will be able to log in." :
                "Users will be able to access the app normally.")
            .setPositiveButton(enable ? "Enable" : "Disable", (dialog, which) -> {
                saveSetting(KEY_MAINTENANCE, enable);
                updateMaintenanceStatus(enable);
                showToast("Maintenance mode " + (enable ? "enabled" : "disabled"));
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                switchMaintenanceMode.setChecked(!enable);
            })
            .show();
    }

    /**
     * Show session timeout dialog
     */
    private void showSessionTimeoutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Session Timeout")
            .setMessage("Choose session timeout duration:")
            .setSingleChoiceItems(
                new String[]{"15 minutes", "30 minutes", "1 hour", "2 hours", "Never"},
                1,
                null)
            .setPositiveButton("Apply", (dialog, which) -> {
                saveSetting(KEY_SESSION_TIMEOUT, true);
                showToast("Session timeout enabled");
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                switchSessionTimeout.setChecked(false);
            })
            .show();
    }

    /**
     * Show clear cache dialog
     */
    private void showClearCacheDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Cache")
            .setMessage("Are you sure you want to clear all cached data? This action cannot be undone.")
            .setPositiveButton("Clear", (dialog, which) -> {
                clearCache();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Clear cache
     */
    private void clearCache() {
        // Simulate cache clearing
        tvCacheSize.setText("0 MB");
        showToast("Cache cleared successfully");
    }

    /**
     * Perform backup
     */
    private void performBackup() {
        showToast("Creating backup...");
        
        // Simulate backup process
        new android.os.Handler().postDelayed(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            preferences.edit().putString("last_backup", timestamp).apply();
            updateLastBackup();
            showToast("Backup completed successfully");
        }, 1500);
    }

    /**
     * Show restore dialog
     */
    private void showRestoreDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Restore Data")
            .setMessage("Select a backup to restore:")
            .setItems(new String[]{
                "Backup - Oct 01, 2025",
                "Backup - Sep 28, 2025",
                "Backup - Sep 25, 2025"
            }, (dialog, which) -> {
                showToast("Restoring backup...");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Export logs
     */
    private void exportLogs() {
        showToast("Exporting logs...");
        
        new android.os.Handler().postDelayed(() -> {
            showToast("Logs exported to Downloads folder");
        }, 1000);
    }

    /**
     * Show reset settings dialog
     */
    private void showResetDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Settings")
            .setMessage("Are you sure you want to reset all settings to default values?")
            .setPositiveButton("Reset", (dialog, which) -> {
                resetSettings();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Reset all settings
     */
    private void resetSettings() {
        preferences.edit().clear().apply();
        loadSettings();
        showToast("Settings reset to defaults");
    }

    /**
     * Update cache size
     */
    private void updateCacheSize() {
        if (tvCacheSize != null) {
            tvCacheSize.setText("24.5 MB");
        }
    }

    /**
     * Update last backup time
     */
    private void updateLastBackup() {
        if (tvLastBackup != null) {
            String lastBackup = preferences.getString("last_backup", "Never");
            tvLastBackup.setText(lastBackup);
        }
    }

    /**
     * Update app version
     */
    private void updateAppVersion() {
        if (tvAppVersion != null) {
            tvAppVersion.setText("Version 1.0.0 (Build 100)");
        }
    }

    /**
     * Save boolean setting
     */
    private void saveSetting(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Animate entrance with staggered card animations
     */
    private void animateEntrance() {
        MaterialCardView[] cards = {cardTheme, cardNotifications, cardAdmin, 
                                   cardData, cardSecurity, cardAbout};
        
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                cards[i].setAlpha(0f);
                cards[i].setTranslationY(50f);
                cards[i].animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setStartDelay(i * 50L)
                    .start();
            }
        }
    }
}
