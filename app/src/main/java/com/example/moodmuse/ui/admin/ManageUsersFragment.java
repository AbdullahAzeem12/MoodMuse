package com.example.moodmuse.ui.admin;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodmuse.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * ManageUsersFragment - Comprehensive user management system
 * Features:
 * - Full CRUD operations for users
 * - Search and filter by name, email, or role
 * - Role management (Admin, User, Moderator)
 * - User status toggle (Active/Inactive)
 * - Swipe to delete
 * - User statistics
 * - Sort by different criteria
 * - Batch operations
 */
public class ManageUsersFragment extends Fragment {

    // UI Components
    private RecyclerView recyclerViewUsers;
    private FloatingActionButton fabAddUser;
    private EditText etSearch;
    private TextView tvUserCount, tvActiveCount, tvInactiveCount;
    private Chip chipAll, chipActive, chipInactive, chipAdmin, chipModerator;
    private View emptyStateLayout;

    // Adapter and Data
    private UserAdapter adapter;
    private List<User> usersList;
    private List<User> filteredList;
    private Random random;

    // Filter state
    private String currentFilter = "all"; // all, active, inactive, admin, moderator

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerView();
        setupSearchFunctionality();
        setupFilterChips();
        loadSampleUsers();
        setupSwipeToDelete();
        animateEntrance();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews(View view) {
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        fabAddUser = view.findViewById(R.id.fabAddUser);
        etSearch = view.findViewById(R.id.etSearch);
        tvUserCount = view.findViewById(R.id.tvUserCount);
        tvActiveCount = view.findViewById(R.id.tvActiveCount);
        tvInactiveCount = view.findViewById(R.id.tvInactiveCount);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        
        chipAll = view.findViewById(R.id.chipAll);
        chipActive = view.findViewById(R.id.chipActive);
        chipInactive = view.findViewById(R.id.chipInactive);
        chipAdmin = view.findViewById(R.id.chipAdmin);
        chipModerator = view.findViewById(R.id.chipModerator);

        usersList = new ArrayList<>();
        filteredList = new ArrayList<>();
        random = new Random();

        fabAddUser.setOnClickListener(v -> showAddUserDialog());
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new UserAdapter(filteredList, new UserAdapter.OnUserClickListener() {
            @Override
            public void onEditClick(User user, int position) {
                showEditUserDialog(user, position);
            }

            @Override
            public void onDeleteClick(User user, int position) {
                showDeleteConfirmation(user, position);
            }

            @Override
            public void onStatusToggle(User user, int position) {
                toggleUserStatus(user, position);
            }

            @Override
            public void onItemClick(User user) {
                showUserDetails(user);
            }
        });

        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(adapter);
    }

    /**
     * Setup search functionality
     */
    private void setupSearchFunctionality() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterUsers(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    /**
     * Setup filter chips
     */
    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> applyFilter("all"));
        chipActive.setOnClickListener(v -> applyFilter("active"));
        chipInactive.setOnClickListener(v -> applyFilter("inactive"));
        chipAdmin.setOnClickListener(v -> applyFilter("admin"));
        chipModerator.setOnClickListener(v -> applyFilter("moderator"));
    }

    /**
     * Apply filter to user list
     */
    private void applyFilter(String filter) {
        currentFilter = filter;
        
        // Update chip selection
        chipAll.setChecked(filter.equals("all"));
        chipActive.setChecked(filter.equals("active"));
        chipInactive.setChecked(filter.equals("inactive"));
        chipAdmin.setChecked(filter.equals("admin"));
        chipModerator.setChecked(filter.equals("moderator"));
        
        filterUsers(etSearch != null ? etSearch.getText().toString() : "");
    }

    /**
     * Filter users based on search query and current filter
     */
    private void filterUsers(String query) {
        filteredList.clear();
        
        for (User user : usersList) {
            // Apply text search
            boolean matchesSearch = query.isEmpty() || 
                user.name.toLowerCase().contains(query.toLowerCase()) ||
                user.email.toLowerCase().contains(query.toLowerCase());
            
            if (!matchesSearch) continue;
            
            // Apply filter
            boolean matchesFilter = false;
            switch (currentFilter) {
                case "all":
                    matchesFilter = true;
                    break;
                case "active":
                    matchesFilter = user.isActive;
                    break;
                case "inactive":
                    matchesFilter = !user.isActive;
                    break;
                case "admin":
                    matchesFilter = user.role.equals("Admin");
                    break;
                case "moderator":
                    matchesFilter = user.role.equals("Moderator");
                    break;
            }
            
            if (matchesFilter) {
                filteredList.add(user);
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyState();
        updateStats();
    }

    /**
     * Load sample users
     */
    private void loadSampleUsers() {
        String[] firstNames = {"John", "Emma", "Michael", "Sophia", "William", "Olivia", 
                              "James", "Ava", "Robert", "Isabella"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", 
                             "Miller", "Davis", "Rodriguez", "Martinez"};
        
        for (int i = 0; i < 15; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String name = firstName + " " + lastName;
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@moodmuse.com";
            
            String role = random.nextInt(10) < 2 ? "Admin" : 
                         random.nextInt(10) < 3 ? "Moderator" : "User";
            
            boolean isActive = random.nextInt(10) < 8; // 80% active
            
            Date joinDate = new Date(System.currentTimeMillis() - 
                (long) (random.nextInt(365) * 24 * 60 * 60 * 1000L));
            
            usersList.add(new User(name, email, role, isActive, joinDate));
        }
        
        filteredList.addAll(usersList);
        adapter.notifyDataSetChanged();
        updateStats();
    }

    /**
     * Setup swipe-to-delete functionality
     */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                User user = filteredList.get(position);
                
                filteredList.remove(position);
                usersList.remove(user);
                adapter.notifyItemRemoved(position);
                
                updateStats();
                updateEmptyState();
                
                Toast.makeText(getContext(), 
                    user.name + " removed", Toast.LENGTH_SHORT).show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewUsers);
    }

    /**
     * Show add user dialog
     */
    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user, null);
        
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        Spinner spinnerRole = dialogView.findViewById(R.id.spinnerRole);
        RadioGroup rgStatus = dialogView.findViewById(R.id.rgStatus);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Setup role spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item,
            new String[]{"User", "Moderator", "Admin"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        AlertDialog dialog = builder.setView(dialogView).create();

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String role = spinnerRole.getSelectedItem().toString();
            boolean isActive = rgStatus.getCheckedRadioButtonId() == R.id.rbActive;

            if (validateUserInput(name, email)) {
                User newUser = new User(name, email, role, isActive, new Date());
                
                usersList.add(newUser);
                filterUsers(etSearch != null ? etSearch.getText().toString() : "");
                
                Toast.makeText(getContext(), "User added successfully", 
                    Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    /**
     * Show edit user dialog
     */
    private void showEditUserDialog(User user, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user, null);
        
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        Spinner spinnerRole = dialogView.findViewById(R.id.spinnerRole);
        RadioGroup rgStatus = dialogView.findViewById(R.id.rgStatus);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);

        if (tvTitle != null) tvTitle.setText("Edit User");
        btnSave.setText("Update");

        // Pre-fill data
        etName.setText(user.name);
        etEmail.setText(user.email);
        
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item,
            new String[]{"User", "Moderator", "Admin"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);
        spinnerRole.setSelection(user.role.equals("Admin") ? 2 : 
                                user.role.equals("Moderator") ? 1 : 0);
        
        if (user.isActive) {
            ((RadioButton) dialogView.findViewById(R.id.rbActive)).setChecked(true);
        } else {
            ((RadioButton) dialogView.findViewById(R.id.rbInactive)).setChecked(true);
        }

        AlertDialog dialog = builder.setView(dialogView).create();

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String role = spinnerRole.getSelectedItem().toString();
            boolean isActive = rgStatus.getCheckedRadioButtonId() == R.id.rbActive;

            if (validateUserInput(name, email)) {
                user.name = name;
                user.email = email;
                user.role = role;
                user.isActive = isActive;
                
                adapter.notifyItemChanged(position);
                updateStats();
                
                Toast.makeText(getContext(), "User updated successfully", 
                    Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    /**
     * Show delete confirmation
     */
    private void showDeleteConfirmation(User user, int position) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete " + user.name + "?")
            .setPositiveButton("Delete", (dialog, which) -> {
                filteredList.remove(position);
                usersList.remove(user);
                adapter.notifyItemRemoved(position);
                
                updateStats();
                updateEmptyState();
                
                Toast.makeText(getContext(), "User deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Toggle user active status
     */
    private void toggleUserStatus(User user, int position) {
        user.isActive = !user.isActive;
        adapter.notifyItemChanged(position);
        updateStats();
        
        String status = user.isActive ? "activated" : "deactivated";
        Toast.makeText(getContext(), user.name + " " + status, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show user details dialog
     */
    private void showUserDetails(User user) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String joinDate = sdf.format(user.joinDate);
        String status = user.isActive ? "Active" : "Inactive";
        
        new AlertDialog.Builder(requireContext())
            .setTitle(user.name)
            .setMessage("Email: " + user.email + "\n" +
                       "Role: " + user.role + "\n" +
                       "Status: " + status + "\n" +
                       "Joined: " + joinDate)
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Validate user input
     */
    private boolean validateUserInput(String name, String email) {
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (email.isEmpty()) {
            Toast.makeText(getContext(), "Please enter an email", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }

    /**
     * Update statistics
     */
    private void updateStats() {
        int activeCount = 0;
        int inactiveCount = 0;
        
        for (User user : usersList) {
            if (user.isActive) activeCount++;
            else inactiveCount++;
        }
        
        if (tvUserCount != null) 
            tvUserCount.setText(String.format(Locale.getDefault(), "%d users", usersList.size()));
        if (tvActiveCount != null) 
            tvActiveCount.setText(String.format(Locale.getDefault(), "%d active", activeCount));
        if (tvInactiveCount != null) 
            tvInactiveCount.setText(String.format(Locale.getDefault(), "%d inactive", inactiveCount));
    }

    /**
     * Update empty state visibility
     */
    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerViewUsers.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewUsers.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Animate entrance
     */
    private void animateEntrance() {
        if (fabAddUser != null) {
            fabAddUser.setScaleX(0f);
            fabAddUser.setScaleY(0f);
            fabAddUser.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setStartDelay(200)
                .start();
        }
    }

    /**
     * User data model
     */
    public static class User {
        public String id;
        public String name;
        public String email;
        public String role;
        public boolean isActive;
        public Date joinDate;

        public User(String name, String email, String role, boolean isActive, Date joinDate) {
            this.name = name;
            this.email = email;
            this.role = role;
            this.isActive = isActive;
            this.joinDate = joinDate;
        }
    }
}

