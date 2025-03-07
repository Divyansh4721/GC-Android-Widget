package com.example.rateswidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RatesFetchTask extends AsyncTask<Void, Void, String[]> {
    private static final String TAG = "RatesFetchTask";
    private static final String API_URL = "https://bcast.jmdpatil.com:7768/VOTSBroadcastStreaming/Services/xml/GetLiveRateByTemplateID/jmd";
    private static final int GOLD_ROW = 6;
    private static final int SILVER_ROW = 5;
    private static final int RATE_COLUMN = 4;
    
    private final Context context;
    private final RemoteViews views;
    private final AppWidgetManager appWidgetManager;
    private final int appWidgetId;
    private boolean hasError = false;
    private String errorMessage = "";
    private boolean isSmallWidget = false;

    public RatesFetchTask(Context context, RemoteViews views, AppWidgetManager appWidgetManager, int appWidgetId) {
        this.context = context;
        this.views = views;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetId = appWidgetId;
        
        // Check if this is the small widget by checking if silver_rate view exists
        try {
            // If this doesn't throw an exception, it's the normal widget
            views.getLayoutId();
            this.isSmallWidget = views.getLayoutId() == R.layout.rates_widget_small;
        } catch (Exception e) {
            // Default to false
            this.isSmallWidget = false;
        }
    }

    @Override
    protected void onPreExecute() {
        // Update the last updated time in 24-hour format (HH:mm)
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = dateFormat.format(new Date());
        views.setTextViewText(R.id.last_updated, "Updated: " + currentTime);
        
        // Show loading indicator
        views.setTextViewText(R.id.gold_rate, "Loading...");
        if (!isSmallWidget) {
            views.setTextViewText(R.id.silver_rate, "Loading...");
        }
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    protected String[] doInBackground(Void... voids) {
        String[] rates = new String[2]; // [gold, silver]
        
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
        
        return rates;
    }

    @Override
    protected void onPostExecute(String[] rates) {
        if (hasError) {
            // Display error message
            views.setTextViewText(R.id.gold_rate, "Error");
            views.setTextColor(R.id.gold_rate, Color.RED);
            
            if (!isSmallWidget) {
                views.setTextViewText(R.id.silver_rate, "Check connection");
                views.setTextColor(R.id.silver_rate, Color.RED);
            }
            
            Log.e(TAG, "Failed to fetch rates: " + errorMessage);
        } else {
            // Format and display the rates
            String goldRate = rates[0] != null ? rates[0] : "N/A";
            String silverRate = rates[1] != null ? rates[1] : "N/A";
            
            // For small widget, only show gold rate
            if (isSmallWidget) {
                views.setTextViewText(R.id.gold_rate, "₹" + goldRate);
                views.setTextColor(R.id.gold_rate, Color.rgb(255, 215, 0)); // Gold color
            } else {
                // For normal widget, show both gold and silver rates
                views.setTextViewText(R.id.gold_rate, "₹" + goldRate);
                views.setTextViewText(R.id.silver_rate, "₹" + silverRate);
                
                // Set the colors
                views.setTextColor(R.id.gold_rate, Color.rgb(255, 215, 0)); // Gold color
                views.setTextColor(R.id.silver_rate, Color.rgb(192, 192, 192)); // Silver color
            }
            
            Log.d(TAG, "Widget updated with new rates");
        }
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}