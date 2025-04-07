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
import android.os.PowerManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RatesWidgetProvider extends AppWidgetProvider {

    public static final int MIN_WIDGET_WIDTH = 220;
    public static final int MAX_WIDGET_WIDTH = 800;
    public static final int MIN_WIDGET_HEIGHT = 100;
    public static final int MAX_WIDGET_HEIGHT = 400;

    public static final String ACTION_CONFIGURE_WIDGET = "com.gcjewellers.rateswidget.ACTION_CONFIGURE_WIDGET";
    public static final String EXTRA_WIDGET_ID = "widget_id";
    public static final String EXTRA_WIDGET_WIDTH = "widget_width";
    public static final String EXTRA_WIDGET_HEIGHT = "widget_height";
    public static final String EXTRA_WIDGET_BACKGROUND_COLOR = "widget_background_color";
    public static final String EXTRA_WIDGET_TEXT_COLOR = "widget_text_color";
    private static final String TAG = "RatesWidgetProvider";
    private static final String ACTION_REFRESH = "com.gcjewellers.rateswidget.ACTION_REFRESH";
    private static final int REFRESH_INTERVAL_SECONDS = 600;
    private static ScheduledExecutorService scheduler;

    public static final String PREFS_NAME = "com.gcjewellers.rateswidget.preferences";
    public static final String PREF_AUTO_REFRESH_ENABLED = "auto_refresh_enabled";
    public static final String PREF_WIDGET_WIDTH = "widget_custom_width";
    public static final String PREF_WIDGET_HEIGHT = "widget_custom_height";
    public static final String ACTION_START_UPDATES = "com.gcjewellers.rateswidget.ACTION_START_UPDATES";
    public static final String ACTION_STOP_UPDATES = "com.gcjewellers.rateswidget.ACTION_STOP_UPDATES";
    public static final String ACTION_UPDATE_WIDGET_SIZE = "com.gcjewellers.rateswidget.ACTION_UPDATE_WIDGET_SIZE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called for " + appWidgetIds.length + " widgets");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        startContinuousRefresh(context);
        WidgetUpdateService.scheduleNextUpdate(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
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
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(PREF_AUTO_REFRESH_ENABLED, true).apply();
            startContinuousRefresh(context);
            WidgetUpdateService.scheduleNextUpdate(context);
        } else if (ACTION_STOP_UPDATES.equals(action)) {
            Log.d(TAG, "Stop updates action received");
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(PREF_AUTO_REFRESH_ENABLED, false).apply();
            stopContinuousRefresh(context);
            WidgetUpdateService.cancelUpdates(context);
        } else if (ACTION_UPDATE_WIDGET_SIZE.equals(action)) {
            Log.d(TAG, "Update widget size action received");
            int customWidth = intent.getIntExtra("width", 400);
            int customHeight = intent.getIntExtra("height", 100);
            int appWidgetId = intent.getIntExtra("widget_id", -1);
            if (appWidgetId != -1) {
                saveWidgetSize(context, appWidgetId, customWidth, customHeight);
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        } else if (ACTION_CONFIGURE_WIDGET.equals(action)) {
            Log.d(TAG, "Configure widget action received");
            int appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1);
            if (appWidgetId != -1) {
                int width = intent.getIntExtra(EXTRA_WIDGET_WIDTH, 400);
                int height = intent.getIntExtra(EXTRA_WIDGET_HEIGHT, 100);
                int backgroundColor = intent.getIntExtra(EXTRA_WIDGET_BACKGROUND_COLOR, Color.TRANSPARENT);
                int textColor = intent.getIntExtra(EXTRA_WIDGET_TEXT_COLOR, Color.WHITE);
                saveWidgetSize(context, appWidgetId, width, height);
                saveWidgetAppearance(context, appWidgetId, backgroundColor, textColor);
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Log.d(TAG, "Updating widget ID: " + appWidgetId);

        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Retrieve widget-specific custom width and height
        int customWidth = prefs.getInt(PREF_WIDGET_WIDTH + "_" + appWidgetId,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 400));
        int customHeight = prefs.getInt(PREF_WIDGET_HEIGHT + "_" + appWidgetId,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100));

        RemoteViews views;
        // Select layout based on custom width
        if (customWidth <= 220) {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_3x1);
        } else if (customWidth <= 250) {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_4x1);
        } else if (customWidth <= 350) {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget_5x1);
        } else {
            views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);
        }

        // Dynamic height adjustments: alter text sizes based on custom height
        if (customHeight > 150) {
            views.setTextViewTextSize(R.id.gold_rate, TypedValue.COMPLEX_UNIT_SP, 28);
            views.setTextViewTextSize(R.id.silver_rate, TypedValue.COMPLEX_UNIT_SP, 28);
            // Additional UI modifications for taller widgets can be applied here.
        } else {
            views.setTextViewTextSize(R.id.gold_rate, TypedValue.COMPLEX_UNIT_SP, 22);
            views.setTextViewTextSize(R.id.silver_rate, TypedValue.COMPLEX_UNIT_SP, 22);
        }

        // Adjust widget appearance based on Firebase authentication state
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            views.setTextViewText(R.id.widget_title, "GC Jewellers");
            views.setTextViewText(R.id.gold_rate, "Sign In");
            views.setTextColor(R.id.gold_rate, Color.WHITE);
            views.setTextViewText(R.id.last_updated, "");
            views.setOnClickPendingIntent(R.id.refresh_button, null);
        } else {
            Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
            refreshIntent.setAction(ACTION_REFRESH);
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                    context, 0, refreshIntent, getPendingIntentFlag());
            views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
            new RatesFetchTask(context, views, appWidgetManager, appWidgetId).execute();
        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

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

    public static int[] getWidgetSize(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int width = prefs.getInt(PREF_WIDGET_WIDTH + "_" + appWidgetId, 400);
        int height = prefs.getInt(PREF_WIDGET_HEIGHT + "_" + appWidgetId, 100);
        return new int[]{width, height};
    }

    public static void saveWidgetAppearance(Context context, int appWidgetId, int backgroundColor, int textColor) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putInt("widget_background_color_" + appWidgetId, backgroundColor)
             .putInt("widget_text_color_" + appWidgetId, textColor)
             .apply();
    }

    public static int[] getWidgetAppearance(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int backgroundColor = prefs.getInt("widget_background_color_" + appWidgetId, Color.TRANSPARENT);
        int textColor = prefs.getInt("widget_text_color_" + appWidgetId, Color.WHITE);
        return new int[]{backgroundColor, textColor};
    }

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
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "RatesWidget:WakeLock");
        wakeLock.acquire(15 * 60 * 1000);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
                    refreshIntent.setAction(ACTION_REFRESH);
                    context.sendBroadcast(refreshIntent);
                }
            } finally {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        }, 0, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, RatesWidgetProvider.class);
        alarmIntent.setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, alarmIntent, getPendingIntentFlag());
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                REFRESH_INTERVAL_SECONDS * 1000L,
                pendingIntent);
    }

    private void stopContinuousRefresh(Context context) {
        Log.d(TAG, "Stopping continuous refresh service");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RatesWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, getPendingIntentFlag());
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "All widgets removed, cleaning up resources");
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RatesWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, getPendingIntentFlag());
        alarmManager.cancel(pendingIntent);
    }

    public static boolean isAutoRefreshEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_REFRESH_ENABLED, true);
    }

    // Helper method to provide proper PendingIntent flags based on API level
    private static int getPendingIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE;
        } else {
            return 0;
        }
    }
}
