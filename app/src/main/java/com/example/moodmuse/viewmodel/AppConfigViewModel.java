package com.example.moodmuse.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.moodmuse.repository.AppConfigRepository;

/**
 * AppConfigViewModel - ViewModel for App Configuration management
 * Uses Repository pattern to centralize Firebase operations
 * Survives screen rotations and configuration changes
 */
public class AppConfigViewModel extends ViewModel {
    private final AppConfigRepository repository;
    private LiveData<AppConfigRepository.AppConfig> appConfig;
    private LiveData<AppConfigRepository.AdminProfile> adminProfile;
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();

    public AppConfigViewModel() {
        repository = new AppConfigRepository();
        appConfig = repository.getAppConfig();
        adminProfile = repository.getAdminProfile();
        
        // Observe repository operation results
        repository.getOperationResult().observeForever(result -> {
            operationResult.postValue(result);
        });
    }

    public LiveData<AppConfigRepository.AppConfig> getAppConfig() {
        return appConfig;
    }

    public LiveData<AppConfigRepository.AdminProfile> getAdminProfile() {
        return adminProfile;
    }

    public LiveData<Boolean> getOperationResult() {
        return operationResult;
    }

    public void updateMaintenanceMode(boolean enabled) {
        repository.updateMaintenanceMode(enabled);
    }

    public void updateAutoApprove(boolean enabled) {
        repository.updateAutoApprove(enabled);
    }

    public void refreshAdminProfile() {
        repository.getAdminProfile();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}
