package com.gcjewellers.rateswidget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONArray;

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
    private static final String API_URL = "https://goldrate.divyanshbansal.com/api/live";
    private static final int GOLD_ROW = 5;
    private static final int SILVER_ROW = 4;
    private static final int RATE_COLUMN = 1;

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
     * @param context  Context for showing Toast on error
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
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();
                    JSONArray jsonArray = new JSONArray(responseBuilder.toString());
                    if (jsonArray.length() > Math.max(GOLD_ROW, SILVER_ROW)) {
                        JSONArray goldRow = jsonArray.getJSONArray(GOLD_ROW);
                        JSONArray silverRow = jsonArray.getJSONArray(SILVER_ROW);
                        if (goldRow.length() > RATE_COLUMN && silverRow.length() > RATE_COLUMN) {
                            rates[0] = goldRow.getString(RATE_COLUMN);
                            rates[1] = silverRow.getString(RATE_COLUMN);
                            Log.d(TAG, "Gold rate: " + rates[0] + ", Silver rate: " + rates[1]);
                        } else {
                            hasError = true;
                            errorMessage = "Invalid response format: rate column not found";
                        }
                    } else {
                        hasError = true;
                        errorMessage = "Invalid response format: required rows not found";
                    }
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