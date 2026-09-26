package com.example.moodmuse.ui.user;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class SmoothSwipePageTransformer implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(@NonNull View page, float position) {
        float abs = Math.abs(position);
        float scale = 0.985f + (1f - abs) * 0.015f;
        float alpha = 0.88f + (1f - abs) * 0.12f;

        page.setPivotX(position < 0 ? page.getWidth() : 0f);
        page.setPivotY(page.getHeight() * 0.5f);
        page.setScaleX(scale);
        page.setScaleY(scale);
        page.setAlpha(alpha);
        page.setTranslationX(-position * page.getWidth() * 0.06f);
    }
}
