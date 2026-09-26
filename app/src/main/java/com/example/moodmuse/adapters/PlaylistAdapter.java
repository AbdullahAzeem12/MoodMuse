package com.example.moodmuse.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.moodmuse.R;
import com.example.moodmuse.database.PlaylistEntity;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private final List<PlaylistEntity> playlists = new ArrayList<>();
    private final OnPlaylistClickListener listener;
    private boolean isGridView = false;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(PlaylistEntity playlist);
        void onPlaylistLongClick(PlaylistEntity playlist);
        void onPlaylistOptionsClick(PlaylistEntity playlist);
        void onPlaylistPlayClick(PlaylistEntity playlist);
        void onPlaylistDeleteClick(PlaylistEntity playlist);
        void onPlaylistShuffleClick(PlaylistEntity playlist);
    }

    public PlaylistAdapter(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setGridView(boolean isGridView) {
        this.isGridView = isGridView;
        notifyDataSetChanged();
    }

    public void updatePlaylists(List<PlaylistEntity> newPlaylists) {
        this.playlists.clear();
        if (newPlaylists != null) {
            this.playlists.addAll(newPlaylists);
        }
        notifyDataSetChanged();
    }

    public List<PlaylistEntity> getPlaylists() {
        return playlists;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isGridView ? R.layout.item_library_playlist_grid : R.layout.item_library_playlist;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaylistEntity playlist = playlists.get(position);
        holder.tvName.setText(playlist.name != null ? playlist.name : "Untitled");
        
        String typeStr = playlist.type;
        String detail = (typeStr != null && typeStr.length() > 0) 
                ? typeStr.substring(0, 1).toUpperCase() + typeStr.substring(1) 
                : "Playlist";
        holder.tvDetails.setText(detail + " • MoodMuse");

        if (playlist.coverImageUrl != null && !playlist.coverImageUrl.isEmpty()) {
            holder.ivArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.ivArt.setPadding(0, 0, 0, 0);
            Glide.with(holder.ivArt.getContext()).load(playlist.coverImageUrl).into(holder.ivArt);
        } else {
            if (holder.ivArt.getContext() != null) {
                int pad = (int) (18 * holder.ivArt.getContext().getResources().getDisplayMetrics().density);
                holder.ivArt.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                holder.ivArt.setPadding(pad, pad, pad, pad);
            }
            holder.ivArt.setImageResource(R.drawable.ic_music);
        }

        holder.itemView.setOnClickListener(v -> {
            animateIcon(holder.ivArt);
            listener.onPlaylistClick(playlist);
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            listener.onPlaylistLongClick(playlist);
            return true;
        });

        // Grid view specific controls with null safety
        if (isGridView) {
            if (holder.ibOptions != null) {
                holder.ibOptions.setVisibility(View.VISIBLE);
                holder.ibOptions.setOnClickListener(v -> listener.onPlaylistOptionsClick(playlist));
            }
            if (holder.llControls != null) {
                holder.llControls.setVisibility(View.VISIBLE);
            }
            if (holder.ibPlay != null) {
                holder.ibPlay.setOnClickListener(v -> listener.onPlaylistPlayClick(playlist));
            }
            if (holder.ibDelete != null) {
                holder.ibDelete.setOnClickListener(v -> listener.onPlaylistDeleteClick(playlist));
            }
            if (holder.ibShuffle != null) {
                holder.ibShuffle.setOnClickListener(v -> listener.onPlaylistShuffleClick(playlist));
            }
        } else {
            if (holder.ibOptions != null) holder.ibOptions.setVisibility(View.GONE);
            if (holder.llControls != null) holder.llControls.setVisibility(View.GONE);
        }

        applyAmplifyAnimation(holder.itemView);
    }

    private void animateIcon(View view) {
        view.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(150)
                .withEndAction(() -> view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start())
                .start();
    }

    private void applyAmplifyAnimation(View view) {
        view.clearAnimation();
        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new OvershootInterpolator(1.2f));
        set.setDuration(500);
        ScaleAnimation scale = new ScaleAnimation(0.7f, 1f, 0.7f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        set.addAnimation(scale);
        set.addAnimation(alpha);
        view.startAnimation(set);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArt;
        TextView tvName, tvDetails;
        ImageButton ibOptions, ibPlay, ibDelete, ibShuffle;
        View llControls;

        ViewHolder(View itemView) {
            super(itemView);
            ivArt = itemView.findViewById(R.id.iv_playlist_art);
            tvName = itemView.findViewById(R.id.tv_playlist_name);
            tvDetails = itemView.findViewById(R.id.tv_playlist_details);
            ibOptions = itemView.findViewById(R.id.ib_playlist_options);
            ibPlay = itemView.findViewById(R.id.ib_playlist_play);
            ibDelete = itemView.findViewById(R.id.ib_playlist_delete);
            ibShuffle = itemView.findViewById(R.id.ib_playlist_shuffle);
            llControls = itemView.findViewById(R.id.ll_playlist_controls);
        }
    }
}
