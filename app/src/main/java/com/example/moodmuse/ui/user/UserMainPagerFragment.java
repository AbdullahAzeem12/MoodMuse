package com.example.moodmuse.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.moodmuse.R;

public class UserMainPagerFragment extends Fragment {
    public static final int PAGE_HOME = 0;
    public static final int PAGE_SCAN = 1;
    public static final int PAGE_LIBRARY = 2;
    public static final int PAGE_MUSIC = 3;
    public static final int PAGE_HISTORY = 4;
    public static final int PAGE_PROFILE = 5;

    private static final String ARG_INITIAL_PAGE = "initial_page";

    private ViewPager2 viewPager;

    public static UserMainPagerFragment newInstance(int initialPage) {
        UserMainPagerFragment fragment = new UserMainPagerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_INITIAL_PAGE, initialPage);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_main_pager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int initialPage = PAGE_HOME;
        if (getArguments() != null) {
            initialPage = getArguments().getInt(ARG_INITIAL_PAGE, PAGE_HOME);
        }

        viewPager = view.findViewById(R.id.user_main_view_pager);
        viewPager.setAdapter(new UserRootPagerAdapter(this));
        viewPager.setOffscreenPageLimit(1);
        viewPager.setCurrentItem(initialPage, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (getActivity() instanceof UserMainActivity) {
                    ((UserMainActivity) getActivity()).onRootPageSelected(position);
                }
            }
        });
    }

    public void setCurrentPage(int pageIndex, boolean smoothScroll) {
        if (viewPager == null) {
            return;
        }
        viewPager.setCurrentItem(pageIndex, smoothScroll);
    }

    private static class UserRootPagerAdapter extends FragmentStateAdapter {
        UserRootPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case PAGE_HOME:
                    return new UserHomeFragment();
                case PAGE_SCAN:
                    return new MoodScanFragment();
                case PAGE_LIBRARY:
                    return new LibraryFragment();
                case PAGE_MUSIC:
                    return new MusicRecommendationFragment();
                case PAGE_HISTORY:
                    return new HistoryAnalyticsFragment();
                case PAGE_PROFILE:
                    return new ProfileSettingsFragment();
                default:
                    return new UserHomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 6;
        }
    }
}

