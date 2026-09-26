package com.example.moodmuse.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.moodmuse.repository.DashboardRepository;
import com.example.moodmuse.ui.admin.AdminDashboardFragment;

/**
 * DashboardViewModel - ViewModel for Dashboard analytics
 * Uses Repository pattern to centralize Firebase operations
 * Survives screen rotations and configuration changes
 */
public class DashboardViewModel extends ViewModel {
    private final DashboardRepository repository;
    private LiveData<AdminDashboardFragment.DashboardStats> dashboardStats;
    private LiveData<Boolean> isLoading;
    private LiveData<String> error;

    public DashboardViewModel() {
        repository = new DashboardRepository();
        dashboardStats = repository.getDashboardStats();
        isLoading = repository.isLoading();
        error = repository.getError();
    }

    public LiveData<AdminDashboardFragment.DashboardStats> getDashboardStats() {
        return dashboardStats;
    }

    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void refreshStats() {
        repository.fetchDashboardStats();
    }
}
