package com.gcjewellers.rateswidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RemoteViews;

public class RatesWidgetProvider extends AppWidgetProvider {
    private static final String PREFS_NAME = "com.gcjewellers.rateswidget.prefs";
    private static final String PREF_AUTO_REFRESH_KEY = "auto_refresh_enabled";
    
    public static final String ACTION_REFRESH = "com.gcjewellers.rateswidget.ACTION_REFRESH";
    public static final String ACTION_START_UPDATES = "com.gcjewellers.rateswidget.ACTION_START_UPDATES";
    public static final String ACTION_STOP_UPDATES = "com.gcjewellers.rateswidget.ACTION_STOP_UPDATES";
    private static final String TAG = "RatesWidgetProvider";
    
    /**
     * Checks if auto-refresh is enabled
     */
    public boolean isAutoRefreshEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_REFRESH_KEY, true); // Default to true
    }
    
    /**
     * Enables or disables auto-refresh
     */
    private void setAutoRefreshEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_AUTO_REFRESH_KEY, enabled).apply();
        
        if (enabled) {
            WidgetUpdateJobScheduler.scheduleJob(context);
        } else {
            WidgetUpdateJobScheduler.cancelJob(context);
        }
    }
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null, null, null);
        }
        
        // Schedule the background update job if auto-refresh is enabled
        if (isAutoRefreshEnabled(context)) {
            WidgetUpdateJobScheduler.scheduleJob(context);
        }
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        String action = intent.getAction();
        if (action == null) return;
        
        switch (action) {
            case ACTION_REFRESH:
                handleRefresh(context, intent);
                break;
            case ACTION_START_UPDATES:
                setAutoRefreshEnabled(context, true);
                break;
            case ACTION_STOP_UPDATES:
                setAutoRefreshEnabled(context, false);
                break;
        }
    }
    
    private void handleRefresh(Context context, Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(intent.getComponent());
        
        String goldRate = intent.getStringExtra("GOLD_RATE");
        String silverRate = intent.getStringExtra("SILVER_RATE");
        String lastUpdated = intent.getStringExtra("LAST_UPDATED");
        
        // Update all widgets with the new data
        for (int appWidgetId : appWidgetIds) {
            // Show loading indicator
            showLoadingState(context, appWidgetManager, appWidgetId);
            
            // Delay the update by 500ms to give user visual feedback
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                updateAppWidget(context, appWidgetManager, appWidgetId, goldRate, silverRate, lastUpdated);
            }, 500); // 0.5 second delay
        }
    }
    
    private void showLoadingState(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);
        
        // Hide refresh button and show progress indicator
        views.setViewVisibility(R.id.refresh_button, View.GONE);
        views.setViewVisibility(R.id.refresh_progress, View.VISIBLE);
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, 
                                int appWidgetId, String goldRate, String silverRate, String lastUpdated) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);
        
        // Set widget data
        if (goldRate != null) {
            views.setTextViewText(R.id.gold_rate, goldRate);
        }
        
        if (silverRate != null) {
            views.setTextViewText(R.id.silver_rate, silverRate);
        }
        
        if (lastUpdated != null) {
            views.setTextViewText(R.id.rates_updated_time, lastUpdated);
        }
        
        // Hide progress and show refresh button
        views.setViewVisibility(R.id.refresh_progress, View.GONE);
        views.setViewVisibility(R.id.refresh_button, View.VISIBLE);
        
        // Set up refresh button click
        Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        
        // If this is a manual refresh (not initial setup), fetch new data
        if (goldRate == null && silverRate == null && lastUpdated == null) {
            new Thread(() -> {
                RatesRepository repository = new RatesRepository(context);
                repository.fetchRates(new RatesRepository.RatesFetchCallback() {
                    @Override
                    public void onSuccess(String goldRate, String silverRate, String lastUpdated,
                                         double goldChangePercent, double silverChangePercent) {
                        // Create update intent
                        Intent updateIntent = new Intent(context, RatesWidgetProvider.class);
                        updateIntent.setAction(ACTION_REFRESH);
                        updateIntent.putExtra("GOLD_RATE", goldRate);
                        updateIntent.putExtra("SILVER_RATE", silverRate);
                        updateIntent.putExtra("LAST_UPDATED", lastUpdated);
                        
                        // Send broadcast to update widget
                        context.sendBroadcast(updateIntent);
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        // Handle error
                    }
                });
            }).start();
        }
    }
    
    @Override
    public void onEnabled(Context context) {
        // Schedule the background update job when the first widget is created
        if (isAutoRefreshEnabled(context)) {
            WidgetUpdateJobScheduler.scheduleJob(context);
        }
    }
    
    @Override
    public void onDisabled(Context context) {
        // Cancel the background update job when all widgets are removed
        WidgetUpdateJobScheduler.cancelJob(context);
    }
}