package com.example.moodmuse.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmuse.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for User RecyclerView
 * Displays user information with action buttons
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<ManageUsersFragment.User> users;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onEditClick(ManageUsersFragment.User user, int position);
        void onDeleteClick(ManageUsersFragment.User user, int position);
        void onStatusToggle(ManageUsersFragment.User user, int position);
        void onItemClick(ManageUsersFragment.User user);
    }

    public UserAdapter(List<ManageUsersFragment.User> users, OnUserClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        ManageUsersFragment.User user = users.get(position);
        
        holder.tvName.setText(user.name);
        holder.tvEmail.setText(user.email);
        
        // Role chip
        holder.chipRole.setText(user.role);
        int roleColor;
        switch (user.role) {
            case "Admin":
                roleColor = R.color.admin_error;
                break;
            case "Moderator":
                roleColor = R.color.admin_warning;
                break;
            default:
                roleColor = R.color.admin_info;
                break;
        }
        holder.chipRole.setChipBackgroundColorResource(roleColor);
        
        // Join date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.tvJoinDate.setText("Joined: " + sdf.format(user.joinDate));
        
        // Status switch
        holder.switchStatus.setChecked(user.isActive);
        holder.switchStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null && buttonView.isPressed()) {
                listener.onStatusToggle(user, holder.getAdapterPosition());
            }
        });
        
        // Status text
        holder.tvStatus.setText(user.isActive ? "Active" : "Inactive");
        holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
            user.isActive ? R.color.admin_success : R.color.admin_error));
        
        // Click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(user, holder.getAdapterPosition());
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(user, holder.getAdapterPosition());
            }
        });
        
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvName, tvEmail, tvJoinDate, tvStatus;
        Chip chipRole;
        SwitchMaterial switchStatus;
        ImageButton btnEdit, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvJoinDate = itemView.findViewById(R.id.tvJoinDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            chipRole = itemView.findViewById(R.id.chipRole);
            switchStatus = itemView.findViewById(R.id.switchStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

