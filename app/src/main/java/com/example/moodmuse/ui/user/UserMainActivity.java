package com.example.moodmuse.ui.user;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.moodmuse.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class UserMainActivity extends AppCompatActivity {

    private static final String TAG = "UserMainActivity";
    private boolean suppressTabCallback = false;
    private boolean suppressSwipeCallback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "Starting UserMainActivity");
            setContentView(R.layout.activity_user_main);

            // Replace BottomNavigationView with TabLayout because we have > 5 items
            com.google.android.material.tabs.TabLayout nav = findViewById(R.id.bottom_nav);
            if (nav != null) {
                // Add tabs based on menu
                addTab(nav, R.id.nav_user_home, "Home", R.drawable.ic_home);
                addTab(nav, R.id.nav_home, "Scan", R.drawable.ic_camera);
                addTab(nav, R.id.nav_user_library, "Library", R.drawable.ic_library);
                addTab(nav, R.id.nav_music, "Music", R.drawable.ic_music_nav);
                addTab(nav, R.id.nav_history, "History", R.drawable.ic_history_nav);
                addTab(nav, R.id.nav_profile, "Profile", R.drawable.ic_profile);

                nav.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                        handleTabSelection(tab);
                    }

                    @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                    @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                        handleTabSelection(tab);
                    }
                });

                // Default selection
                if (savedInstanceState == null) {
                    suppressTabCallback = true;
                    nav.getTabAt(0).select();
                    suppressTabCallback = false;
                    loadFragment(UserMainPagerFragment.newInstance(UserMainPagerFragment.PAGE_HOME));
                }
            } else {
                Log.e(TAG, "Critical Error: BottomNavigationView not found in layout!");
                Toast.makeText(this, "UI initialization error", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate", e);
        }
    }

    private void handleTabSelection(com.google.android.material.tabs.TabLayout.Tab tab) {
        if (suppressTabCallback) {
            return;
        }
        int id = (int) tab.getTag();
        
        Log.d(TAG, "Tab selected/reselected: " + id);

        int pageIndex = tabIdToPageIndex(id);
        if (pageIndex == -1) {
            return;
        }

        Fragment current = getSupportFragmentManager().findFragmentById(R.id.user_nav_host);
        if (current instanceof UserMainPagerFragment) {
            suppressSwipeCallback = true;
            ((UserMainPagerFragment) current).setCurrentPage(pageIndex, false);
            suppressSwipeCallback = false;
            return;
        }

        loadFragment(UserMainPagerFragment.newInstance(pageIndex));
    }

    private void addTab(com.google.android.material.tabs.TabLayout tabLayout, int id, String title, int iconRes) {
        com.google.android.material.tabs.TabLayout.Tab tab = tabLayout.newTab();
        tab.setText(title);
        tab.setIcon(iconRes);
        tab.setTag(id);
        tabLayout.addTab(tab);
    }

    private void loadFragment(Fragment fragment) {
        try {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.user_nav_host, fragment)
                    .commit();
            Log.d(TAG, "Fragment loaded: " + fragment.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Error replacing fragment", e);
        }
    }

    // Navigation helper methods
    public void navigateToEmotionResult(String emotion) {
        loadFragment(EmotionResultFragment.newInstance(emotion));
    }

    public void navigateToEmotionResult(String emotion, String imagePath) {
        loadFragment(EmotionResultFragment.newInstance(emotion, imagePath));
    }

    public void navigateToEmotionResult(String emotion, String imagePath, float[] probabilities) {
        loadFragment(EmotionResultFragment.newInstance(emotion, imagePath, probabilities));
    }

    public void navigateToMusicRecommendations(String emotion) {
        loadFragment(MusicRecommendationFragment.newInstance(emotion));
    }

    public void navigateToMoods() {
        navigateToRootPage(UserMainPagerFragment.PAGE_SCAN);
    }

    public void navigateToUserMoods() {
        loadFragment(new UserMoodsFragment());
        // Optionally update UI selection if we had a dedicated moods tab
    }

    public void navigateToUserHome() {
        navigateToRootPage(UserMainPagerFragment.PAGE_HOME);
    }

    public void navigateToHome() {
        loadFragment(new MoodScanFragment());
    }

    public void navigateToHistory() {
        navigateToRootPage(UserMainPagerFragment.PAGE_HISTORY);
    }

    public void navigateToLibrary() {
        navigateToRootPage(UserMainPagerFragment.PAGE_LIBRARY);
    }

    public void onRootPageSelected(int position) {
        if (suppressSwipeCallback) {
            return;
        }
        int tabId = pageIndexToTabId(position);
        if (tabId == -1) {
            return;
        }
        suppressTabCallback = true;
        selectTabById(tabId);
        suppressTabCallback = false;
    }

    private void navigateToRootPage(int pageIndex) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.user_nav_host);
        if (current instanceof UserMainPagerFragment) {
            suppressSwipeCallback = true;
            ((UserMainPagerFragment) current).setCurrentPage(pageIndex, false);
            suppressSwipeCallback = false;
            onRootPageSelected(pageIndex);
            return;
        }
        loadFragment(UserMainPagerFragment.newInstance(pageIndex));
        onRootPageSelected(pageIndex);
    }

    private int tabIdToPageIndex(int tabId) {
        if (tabId == R.id.nav_user_home) return UserMainPagerFragment.PAGE_HOME;
        if (tabId == R.id.nav_home) return UserMainPagerFragment.PAGE_SCAN;
        if (tabId == R.id.nav_user_library) return UserMainPagerFragment.PAGE_LIBRARY;
        if (tabId == R.id.nav_music) return UserMainPagerFragment.PAGE_MUSIC;
        if (tabId == R.id.nav_history) return UserMainPagerFragment.PAGE_HISTORY;
        if (tabId == R.id.nav_profile) return UserMainPagerFragment.PAGE_PROFILE;
        return -1;
    }

    private int pageIndexToTabId(int pageIndex) {
        if (pageIndex == UserMainPagerFragment.PAGE_HOME) return R.id.nav_user_home;
        if (pageIndex == UserMainPagerFragment.PAGE_SCAN) return R.id.nav_home;
        if (pageIndex == UserMainPagerFragment.PAGE_LIBRARY) return R.id.nav_user_library;
        if (pageIndex == UserMainPagerFragment.PAGE_MUSIC) return R.id.nav_music;
        if (pageIndex == UserMainPagerFragment.PAGE_HISTORY) return R.id.nav_history;
        if (pageIndex == UserMainPagerFragment.PAGE_PROFILE) return R.id.nav_profile;
        return -1;
    }

    private void selectTabById(int id) {
        com.google.android.material.tabs.TabLayout nav = findViewById(R.id.bottom_nav);
        if (nav != null) {
            for (int i = 0; i < nav.getTabCount(); i++) {
                if (nav.getTabAt(i).getTag() != null && (int)nav.getTabAt(i).getTag() == id) {
                    nav.getTabAt(i).select();
                    break;
                }
            }
        }
    }
}
