package com.example.moodmuse.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.moodmuse.R;
import com.example.moodmuse.models.Song;
import com.example.moodmuse.util.DurationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SongWithControlsAdapter extends RecyclerView.Adapter<SongWithControlsAdapter.ViewHolder> {

    private List<Song> songs;
    private OnSongClickListener listener;
    @Nullable private Lifecycle ownerLifecycle;
    private int currentlyPlayingPosition = -1;
    private boolean isPlaying = false;
    private String mood = "neutral";
    private boolean glassmorphismEnabled = false;
    private boolean isGridView = false;
    private Set<String> likedSongIds = new HashSet<>();
    private Set<Integer> entranceAnimatedPositions = new HashSet<>();
    @Nullable private MaterialButton activeOptionsLikeBtn;
    @Nullable private String activeOptionsSongId;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
        void onPlayPauseClick(Song song, int position);
        void onNextClick(Song song, int position);
        void onPreviousClick(Song song, int position);
        void onExternalOpenClick(Song song, int position);
        void onAddToPlaylistClick(Song song, int position);
        void onShareClick(Song song, int position);
        void onLikeClick(Song song, int position);
    }

    public SongWithControlsAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    public SongWithControlsAdapter(List<Song> songs, OnSongClickListener listener, @Nullable Lifecycle lifecycle) {
        this.songs = songs;
        this.listener = listener;
        this.ownerLifecycle = lifecycle;
    }

    public void setGridView(boolean gridView) {
        this.isGridView = gridView;
        notifyDataSetChanged();
    }

    public void setLikedSongIds(@Nullable Set<String> likedSongIds) {
        if (likedSongIds != null) {
            this.likedSongIds = likedSongIds;
            notifyDataSetChanged();
        }
    }

    public int getCurrentlyPlayingPosition() {
        return currentlyPlayingPosition;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void updateSongs(List<Song> newSongs) {
        this.songs = new ArrayList<>(newSongs);
        this.entranceAnimatedPositions.clear();
        notifyDataSetChanged();
    }

    public void removeSong(int position) {
        if (position >= 0 && position < songs.size()) {
            songs.remove(position);
            if (currentlyPlayingPosition == position) {
                currentlyPlayingPosition = -1;
                isPlaying = false;
            } else if (currentlyPlayingPosition > position) {
                currentlyPlayingPosition--;
            }
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, songs.size());
        }
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setMood(String mood) {
        this.mood = mood;
        notifyDataSetChanged();
    }

    public void setGlassmorphismEnabled(boolean enabled) {
        this.glassmorphismEnabled = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_with_controls, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);

        holder.titleText.setText(song.getTitle());
        holder.artistText.setText(song.getArtist());
        holder.albumText.setText(TextUtils.isEmpty(song.getAlbum()) ? "Single" : song.getAlbum());
        holder.durationText.setText(DurationUtils.formatSongDuration(song.getDuration()));
        holder.titleText.setSelected(true);

        boolean hasArt = !TextUtils.isEmpty(song.getImageUrl());
        holder.albumArt.setScaleType(hasArt ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.CENTER_INSIDE);
        holder.albumArt.setPadding(hasArt ? 0 : (int) (12 * holder.itemView.getContext().getResources().getDisplayMetrics().density),
                hasArt ? 0 : (int) (12 * holder.itemView.getContext().getResources().getDisplayMetrics().density),
                hasArt ? 0 : (int) (12 * holder.itemView.getContext().getResources().getDisplayMetrics().density),
                hasArt ? 0 : (int) (12 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
        Glide.with(holder.albumArt.getContext())
                .load(song.getImageUrl())
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(holder.albumArt);

        applyMoodCardStyle(holder.cardView, holder.cardContent);

        if (glassmorphismEnabled) {
            applyGlassmorphismEffect(holder.cardView);
        }

        View.OnClickListener playPauseClick = v -> {
            animateIcon(v);
            listener.onPlayPauseClick(song, position);
        };
        View.OnClickListener nextClick = v -> listener.onNextClick(song, position);
        View.OnClickListener prevClick = v -> listener.onPreviousClick(song, position);
        View.OnClickListener likeClick = v -> {
            animateIcon(v);
            int bindingPos = holder.getBindingAdapterPosition();
            int currentPos = (bindingPos == RecyclerView.NO_POSITION) ? position : bindingPos;
            boolean nowLiked = toggleLikedLocal(song, currentPos);
            refreshActiveOptionsIfMatches(song.getId(), nowLiked);
            listener.onLikeClick(song, currentPos);
        };
        View.OnClickListener addPlaylistClick = v -> {
            animateIcon(v);
            listener.onAddToPlaylistClick(song, position);
        };

        if (isGridView) {
            holder.albumText.setVisibility(View.GONE);
            holder.controlsLayout.setVisibility(View.GONE); // Hide old list controls bar
            holder.durationText.setVisibility(View.GONE);
            
            // Show grid specific controls in the center
            holder.gridControlsLayout.setVisibility(View.VISIBLE);

            holder.titleText.setGravity(android.view.Gravity.CENTER);
            holder.artistText.setGravity(android.view.Gravity.CENTER);

            ViewGroup.LayoutParams lp = holder.albumArt.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = (int) (120 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.albumArt.setLayoutParams(lp);
        } else {
            holder.albumText.setVisibility(View.VISIBLE);
            holder.controlsLayout.setVisibility(View.VISIBLE); // Show list controls bar
            holder.durationText.setVisibility(View.VISIBLE);

            // Hide grid specific controls
            holder.gridControlsLayout.setVisibility(View.GONE);

            holder.titleText.setGravity(android.view.Gravity.START);
            holder.artistText.setGravity(android.view.Gravity.START);

            ViewGroup.LayoutParams lp = holder.albumArt.getLayoutParams();
            lp.width = (int) (64 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            lp.height = (int) (64 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.albumArt.setLayoutParams(lp);
        }

        boolean isThisSongPlaying = (position == currentlyPlayingPosition);

        if (isThisSongPlaying) {
            int iconRes = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
            holder.playPauseButton.setImageResource(iconRes);
            holder.gridPlayPauseButton.setImageResource(iconRes);

            holder.playingIndicator.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
            holder.waveAnimation.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
            if (isPlaying) {
                holder.waveAnimation.playAnimation();
            } else {
                holder.waveAnimation.pauseAnimation();
            }
            holder.youtubeContainer.setVisibility(View.GONE);
        } else {
            holder.playPauseButton.setImageResource(R.drawable.ic_play);
            holder.gridPlayPauseButton.setImageResource(R.drawable.ic_play);

            holder.playingIndicator.setVisibility(View.GONE);
            holder.waveAnimation.cancelAnimation();
            holder.waveAnimation.setVisibility(View.GONE);
            holder.youtubeContainer.setVisibility(View.GONE);
        }

        boolean liked = song.getId() != null && likedSongIds.contains(song.getId());
        int heartRes = liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart;
        holder.likeButton.setImageResource(heartRes);
        holder.gridLikeButton.setImageResource(heartRes);

        String likedContentDesc = holder.likeButton.getContext().getString(liked ? R.string.liked : R.string.like);
        holder.likeButton.setContentDescription(likedContentDesc);
        holder.gridLikeButton.setContentDescription(likedContentDesc);

        holder.itemView.setOnClickListener(v -> {
            animateIcon(holder.albumArt);
            listener.onSongClick(song, position);
        });

        // Set listeners for both sets of buttons (one set will be hidden)
        holder.playPauseButton.setOnClickListener(playPauseClick);
        holder.gridPlayPauseButton.setOnClickListener(playPauseClick);

        holder.forwardButton.setOnClickListener(nextClick);
        holder.gridForwardButton.setOnClickListener(nextClick);

        holder.rewindButton.setOnClickListener(prevClick);
        holder.gridRewindButton.setOnClickListener(prevClick);

        holder.overflowButton.setOnClickListener(v -> showTrackOptionsBottomSheet(holder.itemView.getContext(), song, position));

        holder.likeButton.setOnClickListener(likeClick);
        holder.gridLikeButton.setOnClickListener(likeClick);

        holder.addPlaylistButton.setOnClickListener(addPlaylistClick);
        holder.gridAddPlaylistButton.setOnClickListener(addPlaylistClick);

        int bindPos = holder.getBindingAdapterPosition();
        if (bindPos != RecyclerView.NO_POSITION && !entranceAnimatedPositions.contains(bindPos)) {
            entranceAnimatedPositions.add(bindPos);
            applyAmplifyAnimation(holder.itemView);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        // Clear references or stop animations if needed
        holder.waveAnimation.cancelAnimation();
    }

    public boolean isLikedSongId(String id) {
        return likedSongIds.contains(id);
    }

    public void refreshActiveOptionsIfMatches(@Nullable String songId, boolean liked) {
        if (activeOptionsSongId != null && activeOptionsSongId.equals(songId) && activeOptionsLikeBtn != null) {
            refreshLikeButtonUi(activeOptionsLikeBtn, liked);
        }
    }

    private boolean toggleLikedLocal(Song song, int pos) {
        if (song.getId() == null) return false;
        boolean nowLiked;
        if (likedSongIds.contains(song.getId())) {
            likedSongIds.remove(song.getId());
            nowLiked = false;
        } else {
            likedSongIds.add(song.getId());
            nowLiked = true;
        }
        notifyItemChanged(pos);
        return nowLiked;
    }

    private int findPositionById(String id) {
        for (int i = 0; i < songs.size(); i++) {
            if (id.equals(songs.get(i).getId())) return i;
        }
        return -1;
    }

    private void animateIcon(View view) {
        view.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start())
                .start();
    }

    private void applyAmplifyAnimation(View view) {
        view.setAlpha(0f);
        view.setTranslationY(50f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    @Override
    public long getItemId(int position) {
        Song s = songs.get(position);
        return s.getId() != null ? s.getId().hashCode() : position;
    }

    public void setPlaybackState(int position, boolean playing) {
        this.currentlyPlayingPosition = position;
        this.isPlaying = playing;
        notifyDataSetChanged();
    }

    public void clearPlaybackState() {
        this.currentlyPlayingPosition = -1;
        this.isPlaying = false;
        notifyDataSetChanged();
    }

    private void showTrackOptionsBottomSheet(Context context, Song song, int position) {
        activeOptionsSongId = song.getId();
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_track_options, null);

        TextView title = view.findViewById(R.id.track_option_title);
        TextView artist = view.findViewById(R.id.track_option_artist);
        title.setText(song.getTitle());
        artist.setText(song.getArtist());

        activeOptionsLikeBtn = view.findViewById(R.id.btn_track_like);
        boolean isLiked = likedSongIds.contains(song.getId());
        if (activeOptionsLikeBtn != null) {
            refreshLikeButtonUi(activeOptionsLikeBtn, isLiked);

            activeOptionsLikeBtn.setOnClickListener(v -> {
                boolean nowLiked = toggleLikedLocal(song, position);
                refreshLikeButtonUi(activeOptionsLikeBtn, nowLiked);
                listener.onLikeClick(song, position);
            });
        }

        view.findViewById(R.id.btn_track_add_to_playlist).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onAddToPlaylistClick(song, position);
        });

        view.findViewById(R.id.btn_track_share).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onShareClick(song, position);
        });

        view.findViewById(R.id.btn_track_open).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onExternalOpenClick(song, position);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void refreshLikeButtonUi(@NonNull MaterialButton btn, boolean liked) {
        btn.setIconResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        btn.setText(liked ? "Liked" : "Like");
    }

    private void applyMoodCardStyle(MaterialCardView cardView, View cardContent) {
        int[] colors = resolveMoodColors();
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                colors
        );
        gradient.setCornerRadius(16f);
        cardContent.setBackground(gradient);
    }

    private int[] resolveMoodColors() {
        switch (mood.toLowerCase()) {
            case "happy":
                return new int[]{Color.parseColor("#4039D98A"), Color.parseColor("#4040C9FF")};
            case "sad":
                return new int[]{Color.parseColor("#404C6FFF"), Color.parseColor("#405D54A4")};
            case "calm":
                return new int[]{Color.parseColor("#4053C8C1"), Color.parseColor("#404784FF")};
            case "excited":
                return new int[]{Color.parseColor("#40FF8A65"), Color.parseColor("#40FFB74D")};
            default:
                return new int[]{Color.parseColor("#403A7BD5"), Color.parseColor("#4000D2FF")};
        }
    }

    private void applyGlassmorphismEffect(MaterialCardView cardView) {
        if (cardView == null) return;
        cardView.setCardBackgroundColor(Color.parseColor("#20FFFFFF"));
        cardView.setStrokeColor(Color.parseColor("#40FFFFFF"));
        cardView.setStrokeWidth((int) 1f);
        cardView.setRadius(16f);
        cardView.setCardElevation(8f);
        
        TypedValue outValue = new TypedValue();
        cardView.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        cardView.setForeground(AppCompatResources.getDrawable(cardView.getContext(), outValue.resourceId));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final MaterialCardView cardView;
        public final View cardContent;
        public final ImageView albumArt;
        public final TextView titleText, artistText, albumText, durationText;
        public final View controlsLayout;
        public final FloatingActionButton playPauseButton;
        public final ImageButton rewindButton, forwardButton, overflowButton;
        public final ImageButton likeButton, addPlaylistButton;

        // Grid specific controls
        public final View gridControlsLayout;
        public final FloatingActionButton gridPlayPauseButton;
        public final ImageButton gridRewindButton, gridForwardButton, gridLikeButton, gridAddPlaylistButton;

        public final LinearProgressIndicator playingIndicator;
        public final LottieAnimationView waveAnimation;
        public final View youtubeContainer;
        public final ImageView youtubeThumbnail;

        ViewHolder(View view) {
            super(view);
            cardView = (MaterialCardView) view;
            cardContent = view.findViewById(R.id.song_card_content);
            albumArt = view.findViewById(R.id.song_album_art);
            titleText = view.findViewById(R.id.song_title);
            artistText = view.findViewById(R.id.song_artist);
            albumText = view.findViewById(R.id.song_album);
            durationText = view.findViewById(R.id.song_duration);
            
            controlsLayout = view.findViewById(R.id.controls_layout);
            playPauseButton = view.findViewById(R.id.play_pause_button);
            rewindButton = view.findViewById(R.id.rewind_button);
            forwardButton = view.findViewById(R.id.forward_button);
            overflowButton = view.findViewById(R.id.song_overflow_button);
            likeButton = view.findViewById(R.id.song_like_button);
            addPlaylistButton = view.findViewById(R.id.song_add_playlist_button);

            gridControlsLayout = view.findViewById(R.id.grid_controls_layout);
            gridPlayPauseButton = view.findViewById(R.id.grid_play_pause_button);
            gridRewindButton = view.findViewById(R.id.grid_rewind_button);
            gridForwardButton = view.findViewById(R.id.grid_forward_button);
            gridLikeButton = view.findViewById(R.id.grid_like_button);
            gridAddPlaylistButton = view.findViewById(R.id.grid_add_playlist_button);

            playingIndicator = view.findViewById(R.id.playing_indicator);
            waveAnimation = view.findViewById(R.id.song_wave_animation);
            youtubeContainer = view.findViewById(R.id.youtube_player_container);
            youtubeThumbnail = view.findViewById(R.id.youtube_thumbnail);
        }
    }
}
