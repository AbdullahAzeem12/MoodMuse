package com.example.moodmuse.ui.admin;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.moodmuse.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * AdminDashboardActivity - Main admin dashboard with tabbed navigation
 * Features:
 * - Dashboard: Analytics and statistics
 * - Emotion Mapping: Genre management
 * - Users: User management with CRUD
 * - Settings: App preferences and configuration
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private static final String[] TAB_TITLES = new String[]{
            "Dashboard", "Mapping", "Users", "Settings"
    };
    
    private static final int[] TAB_ICONS = new int[]{
            android.R.drawable.ic_menu_sort_by_size,    // Dashboard
            android.R.drawable.ic_menu_compass,          // Emotion Mapping
            android.R.drawable.ic_menu_myplaces,         // Users
            android.R.drawable.ic_menu_preferences       // Settings
    };

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            Log.d(TAG, "onCreate started");
            setContentView(R.layout.activity_admin_dashboard);
            Log.d(TAG, "Layout inflated successfully");

            initializeViews();
            setupViewPager();
            setupToolbar();
            
            Log.d(TAG, "onCreate completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Fatal error in onCreate", e);
            e.printStackTrace();
        }
    }

    /**
     * Initialize UI components
     */
    private void initializeViews() {
        try {
            Log.d(TAG, "Initializing views");
            viewPager = findViewById(R.id.viewPager);
            tabLayout = findViewById(R.id.tabLayout);
            toolbar = findViewById(R.id.toolbar);
            
            Log.d(TAG, "Views found: viewPager=" + (viewPager != null) + 
                ", tabLayout=" + (tabLayout != null) + ", toolbar=" + (toolbar != null));
                
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            e.printStackTrace();
        }
    }

    /**
     * Setup ViewPager2 with fragment adapter
     */
    private void setupViewPager() {
        try {
            Log.d(TAG, "Setting up ViewPager");
            AdminPagerAdapter adapter = new AdminPagerAdapter(this);
            viewPager.setAdapter(adapter);
            
            // Smooth page transitions
            viewPager.setOffscreenPageLimit(3);
            
            // Connect TabLayout with ViewPager2
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                tab.setText(TAB_TITLES[position]);
                tab.setIcon(TAB_ICONS[position]);
            }).attach();
            
            // Style the tabs
            styleTabLayout();
            Log.d(TAG, "ViewPager setup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ViewPager", e);
            e.printStackTrace();
        }
    }

    /**
     * Setup toolbar
     */
    private void setupToolbar() {
        try {
            if (toolbar != null) {
                Log.d(TAG, "Setting up toolbar");
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Admin Dashboard");
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
                Log.d(TAG, "Toolbar setup completed");
            } else {
                Log.w(TAG, "Toolbar is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar", e);
            e.printStackTrace();
        }
    }

    /**
     * Style TabLayout icons and text
     */
    private void styleTabLayout() {
        try {
            int accentColor = ContextCompat.getColor(this, R.color.admin_accent);
            int normalColor = ContextCompat.getColor(this, R.color.md_theme_outline);
            
            tabLayout.setTabTextColors(normalColor, accentColor);
            tabLayout.setSelectedTabIndicatorColor(accentColor);
            Log.d(TAG, "TabLayout styled successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error styling TabLayout", e);
            e.printStackTrace();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * ViewPager2 adapter for admin fragments
     */
    private static class AdminPagerAdapter extends FragmentStateAdapter {
        
        public AdminPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new AdminDashboardFragment();
                case 1: return new EmotionMappingFragment();
                case 2: return new ManageUsersFragment();
                case 3: return new AdminSettingsFragment();
                default: return new AdminDashboardFragment();
            }
        }

        @Override
        public int getItemCount() {
            return TAB_TITLES.length;
        }
    }
}



