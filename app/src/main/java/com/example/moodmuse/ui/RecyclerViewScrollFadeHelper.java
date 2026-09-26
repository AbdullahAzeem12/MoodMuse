package com.example.moodmuse.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Applies dynamic scale (pop-up) and alpha (fade) to visible rows based on distance from the list center while scrolling.
 */
public final class RecyclerViewScrollFadeHelper {

    private RecyclerViewScrollFadeHelper() {
    }

    public static void attach(@NonNull RecyclerView recyclerView) {
        RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                apply(rv);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    apply(rv);
                }
            }
        };
        recyclerView.addOnScrollListener(listener);
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                apply(recyclerView));
    }

    public static void apply(@NonNull RecyclerView recyclerView) {
        // Works for both LinearLayoutManager and GridLayoutManager
        if (recyclerView.getLayoutManager() == null) {
            return;
        }
        
        int h = recyclerView.getHeight();
        if (h <= 0) return;
        
        int centerY = h / 2;
        // Adjust maxDist to control how quickly items fade/shrink
        float maxDist = h * 0.75f; 
        
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            
            // Calculate child center relative to the recycler view
            int childCenter = (child.getTop() + child.getBottom()) / 2;
            float dist = Math.abs(childCenter - centerY);
            
            // Normalized distance (0 at center, 1 at maxDist)
            float t = Math.min(1f, dist / maxDist);
            
            // Use a non-linear interpolation for a smoother "pop" effect
            // Items stay mostly full size/alpha until they move further away
            float factor = t * t; 

            // Alpha: Fade from 1.0 (center) to 0.4 (edges)
            float alpha = 1f - (0.6f * factor);
            
            // Scale: Pop up from 0.85 (edges) to 1.0 (center)
            float scale = 1f - (0.15f * factor);
            
            child.setAlpha(alpha);
            child.setScaleX(scale);
            child.setScaleY(scale);
            
            // Subtle elevation/Z-axis effect
            child.setTranslationZ((1f - t) * 8f);
        }
    }
}
