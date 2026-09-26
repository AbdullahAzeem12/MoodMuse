package com.example.moodmuse.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.moodmuse.R;
import com.example.moodmuse.models.Song;
import com.example.moodmuse.util.DurationUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    public interface OnSongPlayClickListener {
        void onSongPlayClick(String previewUrl, int position);
    }

    public interface PlaybackHost {
        void onItunesPlaybackStarted();
    }

    private List<Song> songs;
    private SongWithControlsAdapter.OnSongClickListener listener;
    private PlaybackHost playbackHost;
    private OnSongPlayClickListener playClickListener;
    private int currentlyPlayingPosition = -1;
    private boolean isPlaying = false;
    private Set<String> likedSongIds = new HashSet<>();

    public SongAdapter(List<Song> songs, SongWithControlsAdapter.OnSongClickListener listener, @Nullable PlaybackHost playbackHost, @Nullable OnSongPlayClickListener playClickListener) {
        this.songs = songs != null ? songs : new ArrayList<>();
        this.listener = listener;
        this.playbackHost = playbackHost;
        this.playClickListener = playClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_with_controls, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.boundPosition = position;

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvAlbum.setText(song.getAlbum() != null && !song.getAlbum().isEmpty() ? song.getAlbum() : "iTunes");
        holder.tvDuration.setText(DurationUtils.formatSongDuration(song.getDuration()));
        holder.tvTitle.setSelected(true);

        String thumb = song.getImageUrl();
        Glide.with(holder.itemView.getContext())
                .load(thumb)
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(holder.ivCover);

        boolean isThisSongPlaying = position == currentlyPlayingPosition;
        holder.btnPlay.setImageResource(isThisSongPlaying && isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        holder.playingIndicator.setVisibility(isThisSongPlaying && isPlaying ? View.VISIBLE : View.GONE);
        holder.waveAnimation.setVisibility(isThisSongPlaying && isPlaying ? View.VISIBLE : View.GONE);
        if (isThisSongPlaying && isPlaying) {
            holder.waveAnimation.playAnimation();
        } else {
            holder.waveAnimation.pauseAnimation();
        }

        boolean liked = song.getId() != null && likedSongIds.contains(song.getId());
        holder.btnLike.setImageResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart);

        holder.itemView.setOnClickListener(v -> {
            if (listener == null) return;
            listener.onSongClick(song, position);
        });

        holder.btnPlay.setOnClickListener(v -> {
            int p = holder.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) p = position;
            if (playClickListener != null && song.getPreviewUrl() != null) {
                playClickListener.onSongPlayClick(song.getPreviewUrl(), p);
            }
        });

        holder.btnPrev.setOnClickListener(v -> {
            if (listener == null) return;
            listener.onPreviousClick(song, position);
        });

        holder.btnNext.setOnClickListener(v -> {
            if (listener == null) return;
            listener.onNextClick(song, position);
        });

        holder.btnLike.setOnClickListener(v -> {
            if (listener == null) return;
            int p = holder.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) p = position;
            boolean nowLiked = toggleLikedLocal(song);
            holder.btnLike.setImageResource(nowLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
            listener.onLikeClick(song, p);
        });

        holder.btnAddPlaylist.setOnClickListener(v -> {
            if (listener == null) return;
            listener.onAddToPlaylistClick(song, position);
        });

        holder.btnOverflow.setOnClickListener(v -> showTrackOptionsBottomSheet(holder.itemView.getContext(), song, position));
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.waveAnimation.cancelAnimation();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void updateSongs(List<Song> newSongs) {
        this.songs = newSongs != null ? newSongs : new ArrayList<>();
        clearPlaybackState();
        notifyDataSetChanged();
    }

    public void setPlayClickListener(@Nullable OnSongPlayClickListener listener) {
        this.playClickListener = listener;
    }

    public void setLikedSongIds(Set<String> likedSongIds) {
        this.likedSongIds = likedSongIds != null ? likedSongIds : new HashSet<>();
        notifyDataSetChanged();
    }

    public boolean isLikedSongId(@NonNull String id) {
        return likedSongIds.contains(id);
    }

    public void setPlaybackState(int position, boolean isPlaying) {
        int oldPos = currentlyPlayingPosition;
        this.currentlyPlayingPosition = position;
        this.isPlaying = isPlaying;
        if (oldPos != -1) notifyItemChanged(oldPos);
        if (position != -1) notifyItemChanged(position);
    }

    public void stopPlayback() {
        clearPlaybackState();
    }

    public void clearPlaybackState() {
        setPlaybackState(-1, false);
    }

    public int getCurrentlyPlayingPosition() {
        return currentlyPlayingPosition;
    }

    public boolean isPlaying() {
        return isPlaying;
    }


    private boolean toggleLikedLocal(@NonNull Song song) {
        if (song.getId() == null) return false;
        if (likedSongIds.contains(song.getId())) {
            likedSongIds.remove(song.getId());
            return false;
        }
        likedSongIds.add(song.getId());
        return true;
    }

    private void showTrackOptionsBottomSheet(@NonNull Context context, @NonNull Song song, int position) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_track_options, null);

        TextView title = view.findViewById(R.id.track_option_title);
        TextView artist = view.findViewById(R.id.track_option_artist);
        ImageView art = view.findViewById(R.id.track_option_art);

        title.setText(song.getTitle());
        artist.setText(song.getArtist());

        Glide.with(art.getContext())
                .load(song.getImageUrl())
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(art);

        MaterialButton likeBtn = view.findViewById(R.id.btn_track_like);
        boolean liked = song.getId() != null && likedSongIds.contains(song.getId());
        likeBtn.setIconResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        likeBtn.setText(liked ? "Liked" : "Like");

        likeBtn.setOnClickListener(v -> {
            boolean nowLiked = toggleLikedLocal(song);
            likeBtn.setIconResource(nowLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
            likeBtn.setText(nowLiked ? "Liked" : "Like");
            notifyDataSetChanged();
            if (listener != null) listener.onLikeClick(song, position);
        });

        view.findViewById(R.id.btn_track_add_to_playlist).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onAddToPlaylistClick(song, position);
        });

        view.findViewById(R.id.btn_track_share).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onShareClick(song, position);
        });

        view.findViewById(R.id.btn_track_open).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onExternalOpenClick(song, position);
        });

        view.findViewById(R.id.btn_track_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle, tvArtist, tvAlbum, tvDuration;
        FloatingActionButton btnPlay;
        ImageButton btnPrev, btnNext, btnLike, btnAddPlaylist, btnOverflow;
        LinearProgressIndicator playingIndicator;
        LottieAnimationView waveAnimation;
        int boundPosition = RecyclerView.NO_POSITION;

        ViewHolder(View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.song_album_art);
            tvTitle = itemView.findViewById(R.id.song_title);
            tvArtist = itemView.findViewById(R.id.song_artist);
            tvAlbum = itemView.findViewById(R.id.song_album);
            tvDuration = itemView.findViewById(R.id.song_duration);
            btnPlay = itemView.findViewById(R.id.play_pause_button);
            btnPrev = itemView.findViewById(R.id.rewind_button);
            btnNext = itemView.findViewById(R.id.forward_button);
            btnLike = itemView.findViewById(R.id.song_like_button);
            btnAddPlaylist = itemView.findViewById(R.id.song_add_playlist_button);
            btnOverflow = itemView.findViewById(R.id.song_overflow_button);
            playingIndicator = itemView.findViewById(R.id.playing_indicator);
            waveAnimation = itemView.findViewById(R.id.song_wave_animation);
        }
    }
}
