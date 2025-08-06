package com.example.defaultsmsapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SmsCache {
    private static final String TAG = "SmsCache";
    private static final String PREFS_NAME = "sms_cache";
    private static final String KEY_CACHED_MESSAGES = "cached_messages";
    private static final String KEY_LAST_UPDATE = "last_update";
    private static final long CACHE_VALIDITY_DURATION = 5 * 60 * 1000; // 5 minutes
    
    private SharedPreferences prefs;
    private Gson gson;
    
    public SmsCache(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    public void cacheMessages(List<SmsModel> messages) {
        try {
            String json = gson.toJson(messages);
            prefs.edit()
                .putString(KEY_CACHED_MESSAGES, json)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply();
            Log.d(TAG, "Cached " + messages.size() + " messages");
        } catch (Exception e) {
            Log.e(TAG, "Error caching messages", e);
        }
    }
    
    public List<SmsModel> getCachedMessages(int limit) {
        try {
            long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
            long currentTime = System.currentTimeMillis();
            
            // Check if cache is still valid
            if (currentTime - lastUpdate > CACHE_VALIDITY_DURATION) {
                Log.d(TAG, "Cache expired, returning empty list");
                return new ArrayList<>();
            }
            
            String json = prefs.getString(KEY_CACHED_MESSAGES, null);
            if (json != null) {
                Type listType = new TypeToken<List<SmsModel>>(){}.getType();
                List<SmsModel> messages = gson.fromJson(json, listType);
                
                if (messages != null) {
                    // Return only the requested number of messages
                    int size = Math.min(messages.size(), limit);
                    List<SmsModel> result = messages.subList(0, size);
                    Log.d(TAG, "Retrieved " + result.size() + " cached messages");
                    return result;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached messages", e);
        }
        
        return new ArrayList<>();
    }
    
    public long getLastUpdateTime() {
        return prefs.getLong(KEY_LAST_UPDATE, 0);
    }
    
    public void clearCache() {
        prefs.edit()
            .remove(KEY_CACHED_MESSAGES)
            .remove(KEY_LAST_UPDATE)
            .apply();
        Log.d(TAG, "Cache cleared");
    }
}