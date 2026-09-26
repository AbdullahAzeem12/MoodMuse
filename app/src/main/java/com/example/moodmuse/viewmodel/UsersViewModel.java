package com.example.moodmuse.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.moodmuse.repository.UserRepository;
import com.example.moodmuse.ui.admin.ManageUsersFragment;

import java.util.List;

/**
 * UsersViewModel - ViewModel for User management
 * Uses Repository pattern to centralize Firebase operations
 * Survives screen rotations and configuration changes
 */
public class UsersViewModel extends ViewModel {
    private final UserRepository repository;
    private LiveData<List<ManageUsersFragment.User>> usersLiveData;
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();

    public UsersViewModel() {
        repository = new UserRepository();
        usersLiveData = repository.getUsers();
        
        // Observe repository operation results
        repository.getOperationResult().observeForever(result -> {
            operationResult.postValue(result);
        });
    }

    public LiveData<List<ManageUsersFragment.User>> getUsers() {
        return usersLiveData;
    }

    public LiveData<Boolean> getOperationResult() {
        return operationResult;
    }

    public void addUser(String name, String email, String role, boolean isActive) {
        repository.addUser(name, email, role, isActive);
    }

    public void updateUser(String userId, String name, String email, String role, boolean isActive) {
        repository.updateUser(userId, name, email, role, isActive);
    }

    public void deleteUser(String userId) {
        repository.deleteUser(userId);
    }

    public void toggleUserStatus(String userId, boolean isActive) {
        repository.toggleUserStatus(userId, isActive);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}
