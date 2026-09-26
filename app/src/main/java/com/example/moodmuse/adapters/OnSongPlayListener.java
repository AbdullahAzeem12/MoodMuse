package com.example.moodmuse.adapters;

import androidx.annotation.NonNull;

public interface OnSongPlayListener {
    void onSongPlayRequested(@NonNull String videoId, int position);
}

