package com.example.rateswidget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for fetching gold and silver rates from the API
 * Can be used from any activity or fragment in the app
 */
public class RatesFetcher {
    private static final String TAG = "RatesFetcher";
    private static final String API_URL = "https://bcast.jmdpatil.com:7768/VOTSBroadcastStreaming/Services/xml/GetLiveRateByTemplateID/jmd";
    private static final int GOLD_ROW = 6;
    private static final int SILVER_ROW = 5;
    private static final int RATE_COLUMN = 4;
    
    /**
     * Interface for callback when rates are fetched
     */
    public interface RatesFetchListener {
        void onRatesFetched(String goldRate, String silverRate, String lastUpdated);
        void onError(String errorMessage);
    }
    
    /**
     * Fetch rates from the API
     * 
     * @param context Context for showing Toast on error
     * @param listener Callback for when rates are fetched
     */
    public static void fetchRates(final Context context, final RatesFetchListener listener) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            String[] rates = new String[2]; // [gold, silver]
            boolean hasError = false;
            String errorMessage = "";
            
            try {
                Log.d(TAG, "Making API request to: " + API_URL);
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "API response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    int currentRow = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        currentRow++;
                        if (currentRow == GOLD_ROW || currentRow == SILVER_ROW) {
                            String[] columns = line.split("\t");
                            if (columns.length > RATE_COLUMN) {
                                if (currentRow == GOLD_ROW) {
                                    rates[0] = columns[RATE_COLUMN].trim();
                                    Log.d(TAG, "Found gold rate: " + rates[0]);
                                } else {
                                    rates[1] = columns[RATE_COLUMN].trim();
                                    Log.d(TAG, "Found silver rate: " + rates[1]);
                                }
                            }
                        }
                    }
                    reader.close();
                } else {
                    hasError = true;
                    errorMessage = "Server returned code: " + responseCode;
                    Log.e(TAG, errorMessage);
                }
                connection.disconnect();
            } catch (Exception e) {
                hasError = true;
                errorMessage = "Error: " + e.getMessage();
                Log.e(TAG, "API request failed", e);
            }
            
            // Get the current time for last updated
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String currentTime = dateFormat.format(new Date());
            
            // Final variables to use in lambda
            final boolean finalHasError = hasError;
            final String finalErrorMessage = errorMessage;
            final String goldRate = rates[0] != null ? rates[0] : "N/A";
            final String silverRate = rates[1] != null ? rates[1] : "N/A";
            final String lastUpdated = currentTime;
            
            // Post result to main thread
            handler.post(() -> {
                if (finalHasError) {
                    listener.onError(finalErrorMessage);
                } else {
                    listener.onRatesFetched(goldRate, silverRate, lastUpdated);
                }
            });
        });
    }
}