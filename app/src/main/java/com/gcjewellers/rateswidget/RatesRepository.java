package com.gcjewellers.rateswidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RatesRepository {
    private static final String TAG = "RatesRepository";
    private static final String API_URL = "https://goldrate.divyanshbansal.com/api/live";
    private static final int GOLD_ROW = 5;
    private static final int SILVER_ROW = 4;
    private static final int RATE_COLUMN = 1;
    private static final int CHANGE_COLUMN = 2;
    
    private static final String PREFS_NAME = "RatesData";
    private static final String KEY_GOLD_RATE = "gold_rate";
    private static final String KEY_SILVER_RATE = "silver_rate";
    private static final String KEY_LAST_UPDATED = "last_updated";
    private static final String KEY_GOLD_CHANGE = "gold_change";
    private static final String KEY_SILVER_CHANGE = "silver_change";
    private static final String KEY_TIMESTAMP = "timestamp";
    
    private static final long CACHE_DURATION = TimeUnit.MINUTES.toMillis(1);
    
    private final Context context;
    private final SharedPreferences prefs;
    
    public interface RatesFetchCallback {
        void onSuccess(String goldRate, String silverRate, String lastUpdated,
                double goldChangePercent, double silverChangePercent);
        void onError(String errorMessage);
    }
    
    public RatesRepository(Context context) {
        if (context != null) {
            this.context = context.getApplicationContext();
            this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } else {
            // Handle null context case (used by RatesUpdater)
            this.context = null;
            this.prefs = null;
        }
    }
    
    public void fetchRates(RatesFetchCallback callback) {
        // First try to get cached data if context is available
        if (prefs != null && isCacheValid()) {
            String goldRate = prefs.getString(KEY_GOLD_RATE, null);
            String silverRate = prefs.getString(KEY_SILVER_RATE, null);
            String lastUpdated = prefs.getString(KEY_LAST_UPDATED, null);
            double goldChange = prefs.getFloat(KEY_GOLD_CHANGE, 0);
            double silverChange = prefs.getFloat(KEY_SILVER_CHANGE, 0);
            
            if (goldRate != null && silverRate != null && lastUpdated != null) {
                callback.onSuccess(goldRate, silverRate, lastUpdated, goldChange, silverChange);
                return;
            }
        }
        
        // No valid cache, fetch from network
        fetchFromNetwork(callback);
    }
    
    private boolean isCacheValid() {
        if (prefs == null) return false;
        long timestamp = prefs.getLong(KEY_TIMESTAMP, 0);
        return System.currentTimeMillis() - timestamp < CACHE_DURATION;
    }
    
    private void fetchFromNetwork(RatesFetchCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "API response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();
                    
                    JSONArray jsonArray = new JSONArray(responseBuilder.toString());
                    
                    if (jsonArray.length() > Math.max(GOLD_ROW, SILVER_ROW)) {
                        JSONArray goldRowArray = jsonArray.getJSONArray(GOLD_ROW);
                        JSONArray silverRowArray = jsonArray.getJSONArray(SILVER_ROW);
                        
                        if (goldRowArray.length() > RATE_COLUMN && silverRowArray.length() > RATE_COLUMN) {
                            String goldRate = goldRowArray.getString(RATE_COLUMN);
                            String silverRate = silverRowArray.getString(RATE_COLUMN);
                            
                            // Extract price change data
                            double goldChange = 0;
                            double silverChange = 0;
                            
                            if (goldRowArray.length() > CHANGE_COLUMN && silverRowArray.length() > CHANGE_COLUMN) {
                                try {
                                    goldChange = goldRowArray.getDouble(CHANGE_COLUMN);
                                    silverChange = silverRowArray.getDouble(CHANGE_COLUMN);
                                } catch (Exception e) {
                                    Log.w(TAG, "Failed to parse price changes", e);
                                    goldChange = 0;
                                    silverChange = 0;
                                }
                            }
                            
                            String currentTimeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(new Date());
                                    
                            // Save to cache
                            saveToCache(goldRate, silverRate, currentTimeFormatted, goldChange, silverChange);
                            
                            // Return data via callback
                            callback.onSuccess(goldRate, silverRate, currentTimeFormatted, goldChange, silverChange);
                        } else {
                            callback.onError("Invalid response format: rate column not found");
                        }
                    } else {
                        callback.onError("Invalid response format: required rows not found");
                    }
                } else {
                    callback.onError("Server returned code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "API request failed", e);
                callback.onError("Network error: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
    
    private void saveToCache(String goldRate, String silverRate, String lastUpdated, 
                            double goldChange, double silverChange) {
        if (prefs == null) return;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_GOLD_RATE, goldRate);
        editor.putString(KEY_SILVER_RATE, silverRate);
        editor.putString(KEY_LAST_UPDATED, lastUpdated);
        editor.putFloat(KEY_GOLD_CHANGE, (float) goldChange);
        editor.putFloat(KEY_SILVER_CHANGE, (float) silverChange);
        editor.putLong(KEY_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }
}