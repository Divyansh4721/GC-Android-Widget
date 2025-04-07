package com.gcjewellers.rateswidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.widget.RemoteViews;

public class RatesFetchTask extends AsyncTask<Void, Void, String[]> {
    private Context context;
    private RemoteViews views;
    private AppWidgetManager appWidgetManager;
    private int appWidgetId;

    public RatesFetchTask(Context context, RemoteViews views, 
                          AppWidgetManager appWidgetManager, int appWidgetId) {
        this.context = context;
        this.views = views;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetId = appWidgetId;
    }

    @Override
    protected void onPreExecute() {
        // Set loading state
        views.setTextViewText(R.id.gold_rate, "Loading...");
        views.setTextViewText(R.id.silver_rate, "Loading...");
        views.setTextColor(R.id.gold_rate, Color.WHITE);
        views.setTextColor(R.id.silver_rate, Color.WHITE);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    protected String[] doInBackground(Void... voids) {
        final String[] rates = new String[2];
        
        // Add initial delay
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        RatesFetcher.fetchRates(context, new RatesFetcher.RatesFetchListener() {
            @Override
            public void onRatesFetched(String goldRate, String silverRate, String lastUpdated) {
                rates[0] = goldRate;
                rates[1] = silverRate;
            }

            @Override
            public void onError(String errorMessage) {
                rates[0] = "Error";
                rates[1] = "Error";
            }
        });

        // Wait for rates to be fetched
        try {
            int attempts = 0;
            while ((rates[0] == null || rates[1] == null) && attempts < 10) {
                Thread.sleep(500);
                attempts++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return rates;
    }

    @Override
    protected void onPostExecute(String[] result) {
        if (result != null && result[0] != null && result[1] != null) {
            // Update gold rate
            views.setTextViewText(R.id.gold_rate, "₹" + result[0]);
            views.setTextColor(R.id.gold_rate, Color.parseColor("#FFD700"));

            // Update silver rate
            views.setTextViewText(R.id.silver_rate, "₹" + result[1]);
            views.setTextColor(R.id.silver_rate, Color.parseColor("#C0C0C0"));

            // Update last updated time
            views.setTextViewText(R.id.last_updated, java.text.DateFormat
                .getTimeInstance(java.text.DateFormat.SHORT)
                .format(new java.util.Date()));

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } else {
            // Handle error case
            views.setTextViewText(R.id.gold_rate, "Error");
            views.setTextViewText(R.id.silver_rate, "Error");
            views.setTextColor(R.id.gold_rate, Color.RED);
            views.setTextColor(R.id.silver_rate, Color.RED);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}