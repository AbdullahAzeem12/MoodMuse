package com.example.moodmuse.ui.user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmuse.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final List<HistoryAnalyticsFragment.MoodEntry> moodEntries;

    private static final String[] MOODS = {"Happy", "Calm", "Energetic", "Sad", "Anxious", "Stressed", "Focused", "Tired"};
    private static final String[] MOOD_EMOJIS = {"\uD83D\uDE0A", "\uD83D\uDE0C", "\u26A1", "\uD83D\uDE22", "\uD83D\uDE28", "\uD83D\uDE13", "\uD83C\uDFAF", "\uD83D\uDE2B"};

    public HistoryAdapter(List<HistoryAnalyticsFragment.MoodEntry> moodEntries) {
        this.moodEntries = moodEntries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryAnalyticsFragment.MoodEntry entry = moodEntries.get(position);

        int moodIndex = -1;
        String moodName = entry.mood;
        for (int i = 0; i < MOODS.length; i++) {
            if (MOODS[i].equalsIgnoreCase(moodName)) {
                moodIndex = i;
                break;
            }
        }

        String emoji = moodIndex >= 0 ? MOOD_EMOJIS[moodIndex] : "✨";
        holder.emojiText.setText(emoji);
        holder.emotionText.setText("Feeling " + moodName);
        
        holder.timestampText.setText(getRelativeTimeSpan(entry.timestamp));

        String infoText;
        if (entry.playlist != null && !entry.playlist.isEmpty()) {
            infoText = "Listening to: " + entry.playlist;
        } else if (entry.note != null && !entry.note.isEmpty()) {
            infoText = entry.note;
        } else {
            infoText = "No recent activity recorded";
        }
        holder.playlistText.setText(infoText);

        // Catchy Interactive Replay Button Logic
        if (holder.btnReplay != null) {
            holder.btnReplay.setOnClickListener(v -> {
                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction(() -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    if (entry.playlist != null) {
                        handleReplay(v, entry.playlist);
                    } else {
                        Toast.makeText(v.getContext(), "Opening music search for " + moodName, Toast.LENGTH_SHORT).show();
                        handleReplay(v, moodName + " music");
                    }
                }).start();
            });
        }

        // Sensitive Item Click Animation (Scaling)
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).withEndAction(() -> 
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();
        });
    }

    private String getRelativeTimeSpan(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + "m ago";
        if (diff < 86400000) return (diff / 3600000) + "h ago";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void handleReplay(View v, String playlistName) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, 
                android.net.Uri.parse("https://www.jamendo.com/search?q=" + android.net.Uri.encode(playlistName)));
            v.getContext().startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(v.getContext(), "Searching for " + playlistName, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return moodEntries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView emojiText;
        public final TextView emotionText;
        public final TextView timestampText;
        public final TextView playlistText;
        public final MaterialButton btnReplay;

        public ViewHolder(View view) {
            super(view);
            emojiText = view.findViewById(R.id.history_emotion_emoji);
            emotionText = view.findViewById(R.id.history_emotion_text);
            timestampText = view.findViewById(R.id.history_timestamp);
            playlistText = view.findViewById(R.id.history_note);
            btnReplay = view.findViewById(R.id.btn_replay);
        }
    }
}
