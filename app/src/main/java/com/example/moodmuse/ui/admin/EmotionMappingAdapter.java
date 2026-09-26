package com.example.moodmuse.ui.admin;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmuse.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

/**
 * Adapter for EmotionMapping RecyclerView
 * Displays emotion-genre mappings with edit/delete actions
 */
public class EmotionMappingAdapter extends RecyclerView.Adapter<EmotionMappingAdapter.MappingViewHolder> {

    private List<EmotionMappingFragment.EmotionMapping> mappings;
    private OnMappingClickListener listener;

    public interface OnMappingClickListener {
        void onEditClick(EmotionMappingFragment.EmotionMapping mapping, int position);
        void onDeleteClick(EmotionMappingFragment.EmotionMapping mapping, int position);
        void onItemClick(EmotionMappingFragment.EmotionMapping mapping);
    }

    public EmotionMappingAdapter(List<EmotionMappingFragment.EmotionMapping> mappings, 
                                OnMappingClickListener listener) {
        this.mappings = mappings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MappingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_emotion_mapping, parent, false);
        return new MappingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MappingViewHolder holder, int position) {
        EmotionMappingFragment.EmotionMapping mapping = mappings.get(position);
        
        holder.tvEmotion.setText(mapping.emotion);
        holder.tvDescription.setText(mapping.description);
        
        // Set color accent
        try {
            int color = Color.parseColor(mapping.colorHex);
            holder.tvEmotion.setTextColor(color);
        } catch (Exception e) {
            // Use default color if parsing fails
        }
        
        // Clear and populate genres
        holder.chipGroupGenres.removeAllViews();
        for (String genre : mapping.genres) {
            Chip chip = new Chip(holder.itemView.getContext());
            chip.setText(genre);
            chip.setClickable(false);
            chip.setCheckable(false);
            holder.chipGroupGenres.addView(chip);
        }
        
        // Click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(mapping, holder.getAdapterPosition());
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(mapping, holder.getAdapterPosition());
            }
        });
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(mapping);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mappings.size();
    }

    static class MappingViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvEmotion, tvDescription;
        ChipGroup chipGroupGenres;
        ImageButton btnEdit, btnDelete;

        public MappingViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvEmotion = itemView.findViewById(R.id.tvEmotion);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            chipGroupGenres = itemView.findViewById(R.id.chipGroupGenres);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

