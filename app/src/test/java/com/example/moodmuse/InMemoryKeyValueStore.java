package com.example.moodmuse;

import com.example.moodmuse.storage.KeyValueStore;

import java.util.HashMap;
import java.util.Map;

public class InMemoryKeyValueStore implements KeyValueStore {
    private final Map<String, Object> values = new HashMap<>();

    @Override
    public String getString(String key, String defaultValue) {
        Object value = values.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        Object value = values.get(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    @Override
    public long getLong(String key, long defaultValue) {
        Object value = values.get(key);
        return value instanceof Long ? (Long) value : defaultValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    @Override
    public void putString(String key, String value) {
        values.put(key, value);
    }

    @Override
    public void putInt(String key, int value) {
        values.put(key, value);
    }

    @Override
    public void putLong(String key, long value) {
        values.put(key, value);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        values.put(key, value);
    }

    @Override
    public void remove(String key) {
        values.remove(key);
    }

    @Override
    public void clear() {
        values.clear();
    }
}
