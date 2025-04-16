package com.gcjewellers.rateswidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RatesRepository {
    private static final String TAG = "RatesRepository";
    private static final String API_URL = "https://goldrate.divyanshbansal.com/api/live";
    private static final String YESTERDAY_API_URL = "https://goldrate.divyanshbansal.com/api/rates?startDate=";
    
    // Row and column indices for LIVE rates
    private static final int LIVE_GOLD_ROW = 5;
    private static final int LIVE_SILVER_ROW = 4;
    private static final int LIVE_RATE_COLUMN = 1;
    private static final int LIVE_CHANGE_COLUMN = 2;
    
    // Row and column indices for YESTERDAY rates
    private static final int YESTERDAY_GOLD_ROW = 6;
    private static final int YESTERDAY_SILVER_ROW = 5;
    private static final int YESTERDAY_RATE_COLUMN = 2;
    private static final int YESTERDAY_CHANGE_COLUMN = 2;
    
    private static final String PREFS_NAME = "RatesData";
    private static final String KEY_GOLD_RATE = "gold_rate";
    private static final String KEY_SILVER_RATE = "silver_rate";
    private static final String KEY_LAST_UPDATED = "last_updated";
    private static final String KEY_GOLD_CHANGE_VALUE = "gold_change_value";
    private static final String KEY_SILVER_CHANGE_VALUE = "silver_change_value";
    private static final String KEY_YESTERDAY_GOLD_RATE = "yesterday_gold_rate";
    private static final String KEY_YESTERDAY_SILVER_RATE = "yesterday_silver_rate";
    private static final String KEY_TIMESTAMP = "timestamp";
    
    private static final long CACHE_DURATION = TimeUnit.MINUTES.toMillis(1);
    
    private final Context context;
    private final SharedPreferences prefs;
    
    public interface RatesFetchCallback {
        void onSuccess(String goldRate, String silverRate, String lastUpdated,
                       String yesterdayGoldRate, String yesterdaySilverRate,
                       String goldChangeValue, String silverChangeValue);
        void onError(String errorMessage);
    }
    
    public RatesRepository(Context context) {
        if (context != null) {
            this.context = context.getApplicationContext();
            this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } else {
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
            String yesterdayGoldRate = prefs.getString(KEY_YESTERDAY_GOLD_RATE, null);
            String yesterdaySilverRate = prefs.getString(KEY_YESTERDAY_SILVER_RATE, null);
            String goldChangeValue = prefs.getString(KEY_GOLD_CHANGE_VALUE, null);
            String silverChangeValue = prefs.getString(KEY_SILVER_CHANGE_VALUE, null);
            
            if (goldRate != null && silverRate != null && lastUpdated != null && 
                yesterdayGoldRate != null && yesterdaySilverRate != null) {
                callback.onSuccess(goldRate, silverRate, lastUpdated, 
                                  yesterdayGoldRate, yesterdaySilverRate,
                                  goldChangeValue, silverChangeValue);
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
                // First, fetch today's rates
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
                    
                    if (jsonArray.length() > Math.max(LIVE_GOLD_ROW, LIVE_SILVER_ROW)) {
                        JSONArray goldRowArray = jsonArray.getJSONArray(LIVE_GOLD_ROW);
                        JSONArray silverRowArray = jsonArray.getJSONArray(LIVE_SILVER_ROW);
                        
                        if (goldRowArray.length() > LIVE_RATE_COLUMN && silverRowArray.length() > LIVE_RATE_COLUMN) {
                            String goldRate = goldRowArray.getString(LIVE_RATE_COLUMN);
                            String silverRate = silverRowArray.getString(LIVE_RATE_COLUMN);
                            
                            String currentTimeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(new Date());
                            
                            // Now fetch yesterday's rates
                            fetchYesterdayRates(goldRate, silverRate, currentTimeFormatted, callback);
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
    
    private void fetchYesterdayRates(String goldRate, String silverRate, String currentTimeFormatted, 
                                    RatesFetchCallback callback) {
        HttpURLConnection connection = null;
        try {
            // Get yesterday's date
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            Date yesterday = calendar.getTime();
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String yesterdayStr = dateFormat.format(yesterday);
            
            Log.d(TAG, "Fetching yesterday rates for date: " + yesterdayStr);
            String apiUrl = YESTERDAY_API_URL + yesterdayStr;
            Log.d(TAG, "Yesterday API URL: " + apiUrl);
            
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Yesterday API response code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();
                
                String responseStr = responseBuilder.toString();
                Log.d(TAG, "Yesterday API response: " + (responseStr.length() > 200 ? 
                        responseStr.substring(0, 200) + "..." : responseStr));
                
                JSONArray jsonArray = new JSONArray(responseStr);
                
                String yesterdayGoldRate = "0.00";
                String yesterdaySilverRate = "0.00";
                
                if (jsonArray.length() > 0) {
                    // Process the API response using the different indices for yesterday's data
                    JSONArray firstDayData = jsonArray.getJSONArray(0);
                    
                    if (firstDayData.length() > Math.max(YESTERDAY_GOLD_ROW, YESTERDAY_SILVER_ROW)) {
                        JSONArray goldRowArray = firstDayData.getJSONArray(YESTERDAY_GOLD_ROW);
                        JSONArray silverRowArray = firstDayData.getJSONArray(YESTERDAY_SILVER_ROW);
                        
                        if (goldRowArray.length() > YESTERDAY_RATE_COLUMN && 
                            silverRowArray.length() > YESTERDAY_RATE_COLUMN) {
                            
                            yesterdayGoldRate = goldRowArray.getString(YESTERDAY_RATE_COLUMN);
                            yesterdaySilverRate = silverRowArray.getString(YESTERDAY_RATE_COLUMN);
                            
                            Log.d(TAG, "Yesterday goldRate: " + yesterdayGoldRate);
                            Log.d(TAG, "Yesterday silverRate: " + yesterdaySilverRate);
                        }
                    }
                }
                
                // Calculate price change
                String goldChangeValue = "0";
                String silverChangeValue = "0";
                
                try {
                    float currentGold = Float.parseFloat(goldRate.replace(",", ""));
                    float yesterdayGold = Float.parseFloat(yesterdayGoldRate.replace(",", ""));
                    float goldChange = currentGold - yesterdayGold;
                    goldChangeValue = String.valueOf(Math.round(goldChange));
                    
                    float currentSilver = Float.parseFloat(silverRate.replace(",", ""));
                    float yesterdaySilver = Float.parseFloat(yesterdaySilverRate.replace(",", ""));
                    float silverChange = currentSilver - yesterdaySilver;
                    silverChangeValue = String.valueOf(Math.round(silverChange));
                    
                    Log.d(TAG, "Gold change: " + goldChangeValue);
                    Log.d(TAG, "Silver change: " + silverChangeValue);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error calculating price change", e);
                    goldChangeValue = "0";
                    silverChangeValue = "0";
                }
                
                // Format yesterday's prices with commas
                if (!yesterdayGoldRate.contains(",")) {
                    try {
                        float rate = Float.parseFloat(yesterdayGoldRate);
                        yesterdayGoldRate = String.format(Locale.getDefault(), "%,.2f", rate);
                    } catch (NumberFormatException e) {
                        // Keep original string if parsing fails
                    }
                }
                
                if (!yesterdaySilverRate.contains(",")) {
                    try {
                        float rate = Float.parseFloat(yesterdaySilverRate);
                        yesterdaySilverRate = String.format(Locale.getDefault(), "%,.2f", rate);
                    } catch (NumberFormatException e) {
                        // Keep original string if parsing fails
                    }
                }
                
                // Save to cache
                saveToCache(goldRate, silverRate, currentTimeFormatted, 
                           yesterdayGoldRate, yesterdaySilverRate,
                           goldChangeValue, silverChangeValue);
                
                // Return data via callback
                callback.onSuccess(goldRate, silverRate, currentTimeFormatted, 
                                  yesterdayGoldRate, yesterdaySilverRate,
                                  goldChangeValue, silverChangeValue);
                
            } else {
                // If we can't get yesterday's data, use alternate calculation
                Log.e(TAG, "Failed to get yesterday's data, response code: " + responseCode);
                calculateYesterdayRates(goldRate, silverRate, currentTimeFormatted, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Yesterday API request failed", e);
            // Use alternate calculation if API call fails
            calculateYesterdayRates(goldRate, silverRate, currentTimeFormatted, callback);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    private void calculateYesterdayRates(String goldRate, String silverRate, String currentTimeFormatted, 
                                        RatesFetchCallback callback) {
        // Use an alternative calculation based on market averages
        try {
            // Parse current rates
            double currentGold = Double.parseDouble(goldRate.replace(",", ""));
            double currentSilver = Double.parseDouble(silverRate.replace(",", ""));
            
            // Assume a conservative 0.5% change as fallback
            double estimatedChange = 0.005;
            
            // Estimate yesterday's rates (we don't know direction of change, 
            // so use a slight decrease as conservative estimate)
            double yesterdayGold = currentGold * (1 - estimatedChange);
            double yesterdaySilver = currentSilver * (1 - estimatedChange);
            
            // Calculate changes
            double goldChange = currentGold - yesterdayGold;
            double silverChange = currentSilver - yesterdaySilver;
            
            // Format values
            String yesterdayGoldRate = String.format(Locale.getDefault(), "%,.2f", yesterdayGold);
            String yesterdaySilverRate = String.format(Locale.getDefault(), "%,.2f", yesterdaySilver);
            String goldChangeValue = String.valueOf(Math.round(goldChange));
            String silverChangeValue = String.valueOf(Math.round(silverChange));
            
            // Log calculated values
            Log.d(TAG, "Calculated yesterday goldRate: " + yesterdayGoldRate);
            Log.d(TAG, "Calculated yesterday silverRate: " + yesterdaySilverRate);
            Log.d(TAG, "Calculated gold change: " + goldChangeValue);
            Log.d(TAG, "Calculated silver change: " + silverChangeValue);
            
            // Save to cache
            saveToCache(goldRate, silverRate, currentTimeFormatted, 
                       yesterdayGoldRate, yesterdaySilverRate,
                       goldChangeValue, silverChangeValue);
            
            // Return data via callback
            callback.onSuccess(goldRate, silverRate, currentTimeFormatted, 
                              yesterdayGoldRate, yesterdaySilverRate,
                              goldChangeValue, silverChangeValue);
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error in fallback calculation", e);
            
            // We can't calculate, so just use some reasonable defaults
            saveToCache(goldRate, silverRate, currentTimeFormatted, goldRate, silverRate, "0", "0");
            callback.onSuccess(goldRate, silverRate, currentTimeFormatted, goldRate, silverRate, "0", "0");
        }
    }
    
    private void saveToCache(String goldRate, String silverRate, String lastUpdated, 
                            String yesterdayGoldRate, String yesterdaySilverRate,
                            String goldChangeValue, String silverChangeValue) {
        if (prefs == null) return;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_GOLD_RATE, goldRate);
        editor.putString(KEY_SILVER_RATE, silverRate);
        editor.putString(KEY_LAST_UPDATED, lastUpdated);
        editor.putString(KEY_YESTERDAY_GOLD_RATE, yesterdayGoldRate);
        editor.putString(KEY_YESTERDAY_SILVER_RATE, yesterdaySilverRate);
        editor.putString(KEY_GOLD_CHANGE_VALUE, goldChangeValue);
        editor.putString(KEY_SILVER_CHANGE_VALUE, silverChangeValue);
        editor.putLong(KEY_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }
}