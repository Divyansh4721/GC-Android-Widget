package com.example.rateswidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RatesFetchTask extends AsyncTask<Void, Void, String[]> {
    private static final String API_URL = "https://bcast.jmdpatil.com:7768/VOTSBroadcastStreaming/Services/xml/GetLiveRateByTemplateID/jmd";
    private static final int GOLD_ROW = 4;
    private static final int SILVER_ROW = 3;
    private static final int RATE_COLUMN = 6;
    
    private final Context context;
    private final RemoteViews views;
    private final AppWidgetManager appWidgetManager;
    private final int appWidgetId;

    public RatesFetchTask(Context context, RemoteViews views, AppWidgetManager appWidgetManager, int appWidgetId) {
        this.context = context;
        this.views = views;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetId = appWidgetId;
    }

    @Override
    protected void onPreExecute() {
        // Update the last updated time
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = dateFormat.format(new Date());
        views.setTextViewText(R.id.last_updated, "Last Updated: " + currentTime);
        
        // Show loading indicator
        views.setTextViewText(R.id.gold_rate, "Loading...");
        views.setTextViewText(R.id.silver_rate, "Loading...");
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    protected String[] doInBackground(Void... voids) {
        String[] rates = new String[2]; // [gold, silver]
        
        try {
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
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
                            } else {
                                rates[1] = columns[RATE_COLUMN].trim();
                            }
                        }
                    }
                }
                reader.close();
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return rates;
    }

    @Override
    protected void onPostExecute(String[] rates) {
        // Format and display the rates
        String goldRate = rates[0] != null ? rates[0] : "N/A";
        String silverRate = rates[1] != null ? rates[1] : "N/A";
        
        views.setTextViewText(R.id.gold_rate, "Gold: ₹" + goldRate);
        views.setTextViewText(R.id.silver_rate, "Silver: ₹" + silverRate);
        
        // Set the colors
        views.setTextColor(R.id.gold_rate, Color.rgb(255, 215, 0)); // Gold color
        views.setTextColor(R.id.silver_rate, Color.rgb(192, 192, 192)); // Silver color
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}