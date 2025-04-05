package com.gcjewellers.rateswidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.util.TypedValue;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RatesWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "RatesWidgetProvider";
    private static final String ACTION_REFRESH = "com.gcjewellers.rateswidget.ACTION_REFRESH";
    private static final int REFRESH_INTERVAL_SECONDS = 600; // Changed from 10 to 600 (10 minutes)
    private static ScheduledExecutorService scheduler;
    
    // Add constants for preferences and actions
    public static final String PREFS_NAME = "com.gcjewellers.rateswidget.preferences";
    public static final String PREF_AUTO_REFRESH_ENABLED = "auto_refresh_enabled";
    public static final String ACTION_START_UPDATES = "com.gcjewellers.rateswidget.ACTION_START_UPDATES";
    public static final String ACTION_STOP_UPDATES = "com.gcjewellers.rateswidget.ACTION_STOP_UPDATES";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called for " + appWidgetIds.length + " widgets");
        
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }

        // Start the continuous refresh service
        startContinuousRefresh(context);

        // Schedule widget updates through JobScheduler
        WidgetUpdateService.scheduleNextUpdate(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                      int appWidgetId, Bundle newOptions) {
        // Widget size has changed, update the widget
        updateAppWidget(context, appWidgetManager, appWidgetId);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        
        Log.d(TAG, "Received action: " + action);

        if (ACTION_REFRESH.equals(action)) {
            Log.d(TAG, "Refresh action received");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RatesWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        } else if (ACTION_START_UPDATES.equals(action)) {
            Log.d(TAG, "Start updates action received");
            // Save preference
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(PREF_AUTO_REFRESH_ENABLED, true).apply();
            
            // Start automatic updates
            startContinuousRefresh(context);
            
            // Schedule widget updates through JobScheduler
            WidgetUpdateService.scheduleNextUpdate(context);
        } else if (ACTION_STOP_UPDATES.equals(action)) {
            Log.d(TAG, "Stop updates action received");
            // Save preference
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(PREF_AUTO_REFRESH_ENABLED, false).apply();
            
            // Stop automatic updates
            stopContinuousRefresh(context);
            
            // Cancel any pending JobScheduler updates
            WidgetUpdateService.cancelUpdates(context);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "Updating widget ID: " + appWidgetId);
        
        // Get the widget size
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 400);
        
        // Determine widget layout based primarily on width
        RemoteViews views;
        if (minWidth <= 220) {
            // 3x1 widget (smallest)
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_3x1);
        } else if (minWidth <= 250) {
            // 4x1 widget
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_4x1);
        } else if (minWidth <= 350) {
            // 5x1 widget
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_5x1);
        } else {
            // Wide widget
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);
        }

        // Check if user is authenticated
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User not logged in, show sign-in message
            views.setTextViewText(R.id.widget_title, "GC Jewellers");
            views.setTextViewText(R.id.gold_rate, "Sign In");
            views.setTextColor(R.id.gold_rate, Color.WHITE);
            
            views.setTextViewText(R.id.last_updated, "");
            views.setOnClickPendingIntent(R.id.refresh_button, null);
        } else {
            // Set up refresh button click intent
            Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
            refreshIntent.setAction(ACTION_REFRESH);
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                    context, 0, refreshIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
    
            // Start API fetch
            new RatesFetchTask(context, views, appWidgetManager, appWidgetId).execute();
        }
    
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void startContinuousRefresh(Context context) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        
        // Check if auto-refresh is enabled
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean autoRefreshEnabled = prefs.getBoolean(PREF_AUTO_REFRESH_ENABLED, true);
        
        if (!autoRefreshEnabled) {
            Log.d(TAG, "Auto-refresh is disabled, not starting scheduler");
            return;
        }

        Log.d(TAG, "Starting continuous refresh service");
        
        // Acquire wake lock to keep the service running
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "RatesWidget:WakeLock");
        wakeLock.acquire(15 * 60 * 1000);

        // Schedule periodic refresh
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            // Check if user is logged in before refreshing
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
                refreshIntent.setAction(ACTION_REFRESH);
                context.sendBroadcast(refreshIntent);
            }

            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }, 0, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // Set up alarm as backup for refresh only if auto-refresh is enabled
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, RatesWidgetProvider.class);
        alarmIntent.setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                REFRESH_INTERVAL_SECONDS * 1000L,
                pendingIntent);
    }

    private void stopContinuousRefresh(Context context) {
        Log.d(TAG, "Stopping continuous refresh service");
        
        // Shutdown the scheduler if it's running
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        
        // Cancel the alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RatesWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "All widgets removed, cleaning up resources");

        // Clean up resources when last widget is removed
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        // Cancel alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RatesWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }
    
    // Utility method to check if auto-refresh is enabled
    public static boolean isAutoRefreshEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_REFRESH_ENABLED, true); // Default to true
    }
}