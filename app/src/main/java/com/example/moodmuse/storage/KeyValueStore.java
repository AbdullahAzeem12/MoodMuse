package com.example.moodmuse.storage;

public interface KeyValueStore {
    String getString(String key, String defaultValue);
    int getInt(String key, int defaultValue);
    long getLong(String key, long defaultValue);
    boolean getBoolean(String key, boolean defaultValue);
    void putString(String key, String value);
    void putInt(String key, int value);
    void putLong(String key, long value);
    void putBoolean(String key, boolean value);
    void remove(String key);
    void clear();
}
