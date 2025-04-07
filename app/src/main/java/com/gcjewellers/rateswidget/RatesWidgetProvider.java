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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RatesWidgetProvider extends AppWidgetProvider {
    // Logging and Action Constants - MADE PUBLIC
    public static final String ACTION_REFRESH = "com.gcjewellers.rateswidget.ACTION_REFRESH";
    public static final String ACTION_START_UPDATES = "com.gcjewellers.rateswidget.ACTION_START_UPDATES";
    public static final String ACTION_STOP_UPDATES = "com.gcjewellers.rateswidget.ACTION_STOP_UPDATES";
    public static final String ACTION_UPDATE_WIDGET_SIZE = "com.gcjewellers.rateswidget.ACTION_UPDATE_WIDGET_SIZE";

    // Preferences Constants
    public static final String PREFS_NAME = "com.gcjewellers.rateswidget.preferences";
    public static final String PREF_AUTO_REFRESH_ENABLED = "auto_refresh_enabled";
    public static final String PREF_WIDGET_WIDTH = "widget_custom_width";
    public static final String PREF_WIDGET_HEIGHT = "widget_custom_height";

    // Refresh Interval
    private static final int REFRESH_INTERVAL_SECONDS = 600;
    private static ScheduledExecutorService scheduler;

    // Size Constraints
    public static final int MIN_WIDGET_WIDTH = 180;
    public static final int MAX_WIDGET_WIDTH = 400;
    public static final int MIN_WIDGET_HEIGHT = 60;
    public static final int MAX_WIDGET_HEIGHT = 200;

    private static final String TAG = "RatesWidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called for " + appWidgetIds.length + " widgets");
        
        for (int appWidgetId : appWidgetIds) {
            updateWidgetWithSize(context, appWidgetManager, appWidgetId);
        }
        
        startContinuousRefresh(context);
        WidgetUpdateService.scheduleNextUpdate(context);
    }

    // Updated method to match the signature expected by MainActivity and WidgetConfigureActivity
    public static void updateWidgetWithSize(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "Updating widget ID: " + appWidgetId);

        // Retrieve saved widget size
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int customWidth = prefs.getInt(PREF_WIDGET_WIDTH + "_" + appWidgetId, 220);
        int customHeight = prefs.getInt(PREF_WIDGET_HEIGHT + "_" + appWidgetId, 100);

        // Select layout based on custom width
        RemoteViews views;
        if (customWidth <= 180) {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_3x1);
        } else if (customWidth <= 220) {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_4x1);
        } else if (customWidth <= 350) {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_5x1);
        } else {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);
        }

        // Dynamic height adjustments: alter text sizes based on custom height
        if (customHeight > 150) {
            views.setTextViewTextSize(R.id.gold_rate, TypedValue.COMPLEX_UNIT_SP, 28);
            views.setTextViewTextSize(R.id.gold_label, TypedValue.COMPLEX_UNIT_SP, 16);
            
            if (views.getLayoutId() != R.layout.rates_widget_3x1 && 
                views.getLayoutId() != R.layout.rates_widget_small) {
                views.setTextViewTextSize(R.id.silver_rate, TypedValue.COMPLEX_UNIT_SP, 28);
                views.setTextViewTextSize(R.id.silver_label, TypedValue.COMPLEX_UNIT_SP, 16);
            }
        } else if (customHeight > 100) {
            views.setTextViewTextSize(R.id.gold_rate, TypedValue.COMPLEX_UNIT_SP, 22);
            views.setTextViewTextSize(R.id.gold_label, TypedValue.COMPLEX_UNIT_SP, 14);
            
            if (views.getLayoutId() != R.layout.rates_widget_3x1 && 
                views.getLayoutId() != R.layout.rates_widget_small) {
                views.setTextViewTextSize(R.id.silver_rate, TypedValue.COMPLEX_UNIT_SP, 22);
                views.setTextViewTextSize(R.id.silver_label, TypedValue.COMPLEX_UNIT_SP, 14);
            }
        } else {
            views.setTextViewTextSize(R.id.gold_rate, TypedValue.COMPLEX_UNIT_SP, 18);
            views.setTextViewTextSize(R.id.gold_label, TypedValue.COMPLEX_UNIT_SP, 12);
            
            if (views.getLayoutId() != R.layout.rates_widget_3x1 && 
                views.getLayoutId() != R.layout.rates_widget_small) {
                views.setTextViewTextSize(R.id.silver_rate, TypedValue.COMPLEX_UNIT_SP, 18);
                views.setTextViewTextSize(R.id.silver_label, TypedValue.COMPLEX_UNIT_SP, 12);
            }
        }

        // Adjust widget title and last updated text size based on height
        views.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, 
            customHeight > 120 ? 16 : 14);
        views.setTextViewTextSize(R.id.last_updated, TypedValue.COMPLEX_UNIT_SP, 
            customHeight > 120 ? 14 : 12);

        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            views.setTextViewText(R.id.widget_title, "GC Jewellers");
            views.setTextViewText(R.id.gold_rate, "Sign In");
            views.setTextColor(R.id.gold_rate, Color.WHITE);
            views.setTextViewText(R.id.last_updated, "");
            views.setOnClickPendingIntent(R.id.refresh_button, null);
        } else {
            // Setup refresh intent
            Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
            refreshIntent.setAction(ACTION_REFRESH);
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                    context, 0, refreshIntent, getPendingIntentFlag());
            views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
            
            // Fetch rates
            new RatesFetchTask(context, views, appWidgetManager, appWidgetId).execute();
        }
        
        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    // Rest of the previous implementation remains the same...
    
    // Additional helper methods (saveWidgetSize, getWidgetSize, etc.) remain the same


    // Save widget size
    public static void saveWidgetSize(Context context, int appWidgetId, int width, int height) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Validate and clamp the values
        width = Math.max(MIN_WIDGET_WIDTH, Math.min(width, MAX_WIDGET_WIDTH));
        height = Math.max(MIN_WIDGET_HEIGHT, Math.min(height, MAX_WIDGET_HEIGHT));
        
        editor.putInt(PREF_WIDGET_WIDTH + "_" + appWidgetId, width)
              .putInt(PREF_WIDGET_HEIGHT + "_" + appWidgetId, height)
              .apply();
    }

    // Retrieve widget size
    public static int[] getWidgetSize(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int width = prefs.getInt(PREF_WIDGET_WIDTH + "_" + appWidgetId, 220);
        int height = prefs.getInt(PREF_WIDGET_HEIGHT + "_" + appWidgetId, 100);
        return new int[]{width, height};
    }

    // Check if auto-refresh is enabled
    public static boolean isAutoRefreshEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_REFRESH_ENABLED, true);
    }

    // Start continuous refresh
    private void startContinuousRefresh(Context context) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean autoRefreshEnabled = prefs.getBoolean(PREF_AUTO_REFRESH_ENABLED, true);
        
        if (!autoRefreshEnabled) {
            Log.d(TAG, "Auto-refresh is disabled, not starting scheduler");
            return;
        }
        
        Log.d(TAG, "Starting continuous refresh service");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
                refreshIntent.setAction(ACTION_REFRESH);
                context.sendBroadcast(refreshIntent);
            }
        }, 0, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    // Stop continuous refresh
    private void stopContinuousRefresh(Context context) {
        Log.d(TAG, "Stopping continuous refresh service");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "All widgets removed, cleaning up resources");
        
        // Stop the scheduler and cancel any pending updates
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        
        // Cancel any scheduled alarm updates
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RatesWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, getPendingIntentFlag());
        alarmManager.cancel(pendingIntent);
    }

    // Helper method to get appropriate PendingIntent flags
    private static int getPendingIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE;
        } else {
            return 0;
        }
    }
}