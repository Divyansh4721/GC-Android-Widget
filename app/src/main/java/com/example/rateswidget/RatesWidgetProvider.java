package com.example.rateswidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RatesWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "RatesWidgetProvider";
    private static final String ACTION_REFRESH = "com.example.rateswidget.ACTION_REFRESH";
    private static final int REFRESH_INTERVAL_SECONDS = 300; // Changed from 10 to 300 (5 minutes)
    private static ScheduledExecutorService scheduler;

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

        if (ACTION_REFRESH.equals(intent.getAction())) {
            Log.d(TAG, "Refresh action received");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RatesWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "Updating widget ID: " + appWidgetId);
        
        // Get the widget size
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 400);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100);
        
        // Choose layout based on size - wider than 250dp AND taller than 100dp for the large layout
        boolean isSmallWidget = !(minWidth > 150 && minHeight > 10);
        boolean isMidWidget = !(minWidth > 250 && minHeight > 10);

        // Choose layout based on size
        RemoteViews views;
        if (isSmallWidget) {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_small);
        } else if (isMidWidget) { 
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_extended);
        } else {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);
        }

        // Check if user is authenticated
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User not logged in, show sign-in message
            views.setTextViewText(R.id.widget_title, "GC Jewellers");
            
            if (isSmallWidget) {
                // Small widget
                views.setTextViewText(R.id.gold_rate, "Please Sign In");
                views.setTextColor(R.id.gold_rate, Color.WHITE);
            } else if (isMidWidget) {
                // Mid widget
                views.setTextViewText(R.id.gold_rate, "Please Sign In");
                views.setTextColor(R.id.gold_rate, Color.WHITE);
            } else {
                // Normal widget
                views.setTextViewText(R.id.gold_rate, "Please😒");
                views.setTextColor(R.id.gold_rate, Color.WHITE);
                views.setTextViewText(R.id.silver_rate, "-");
                views.setTextColor(R.id.silver_rate, Color.WHITE);
                views.setTextViewText(R.id.silver_rate, "Sign In🤦‍♂️");
                views.setTextColor(R.id.silver_rate, Color.WHITE);
            }
            
            views.setTextViewText(R.id.last_updated, "");
            
            // Disable refresh button when not logged in
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

        // Set up alarm as backup for refresh
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
}