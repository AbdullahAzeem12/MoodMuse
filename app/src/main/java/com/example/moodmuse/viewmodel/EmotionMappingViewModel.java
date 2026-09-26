package com.example.moodmuse.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.moodmuse.repository.EmotionMappingRepository;
import com.example.moodmuse.ui.admin.EmotionMappingFragment;

import java.util.List;

/**
 * EmotionMappingViewModel - ViewModel for Emotion-Genre mapping management
 * Uses Repository pattern to centralize Firebase operations
 * Survives screen rotations and configuration changes
 */
public class EmotionMappingViewModel extends ViewModel {
    private final EmotionMappingRepository repository;
    private LiveData<List<EmotionMappingFragment.EmotionMapping>> mappingsLiveData;
    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();

    public EmotionMappingViewModel() {
        repository = new EmotionMappingRepository();
        mappingsLiveData = repository.getMappings();
        
        // Observe repository operation results
        repository.getOperationResult().observeForever(result -> {
            operationResult.postValue(result);
        });
    }

    public LiveData<List<EmotionMappingFragment.EmotionMapping>> getMappings() {
        return mappingsLiveData;
    }

    public LiveData<Boolean> getOperationResult() {
        return operationResult;
    }

    public void addMapping(String emotion, String description, List<String> genres, String colorHex) {
        repository.addMapping(emotion, description, genres, colorHex);
    }

    public void updateMapping(String mappingId, String emotion, String description, List<String> genres) {
        repository.updateMapping(mappingId, emotion, description, genres);
    }

    public void deleteMapping(String mappingId) {
        repository.deleteMapping(mappingId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}
