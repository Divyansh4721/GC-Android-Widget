package com.gcjewellers.rateswidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RatesWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "RatesWidgetProvider";

    public static final String ACTION_UPDATE_WIDGET = "com.gcjewellers.rateswidget.UPDATE_WIDGET";
    public static final String ACTION_START_UPDATES = "com.gcjewellers.rateswidget.START_UPDATES";
    public static final String ACTION_STOP_UPDATES = "com.gcjewellers.rateswidget.STOP_UPDATES";

    private static final String PREFS_NAME = "WidgetPrefs";
    private static final String KEY_AUTO_REFRESH = "autoRefresh";
    private static final long UPDATE_INTERVAL = 15 * 60 * 1000; // 15 minutes
    private static final long REFRESH_ANIMATION_DELAY = 500; // 0.5 second animation duration
    private static final long REFRESH_ANIMATION_DURATION = 500; // 0.5 second animation duration

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called for widget");
        // Update each widget
        updateWidgets(context, appWidgetManager, appWidgetIds);

        // Set repeating updates if enabled
        if (isAutoRefreshEnabled(context)) {
            startAutoRefresh(context);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (action == null)
            return;

        Log.d(TAG, "onReceive called with action: " + action);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, RatesWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        switch (action) {
            case ACTION_UPDATE_WIDGET:
                // Manual refresh button clicked
                performRefreshAnimation(context, appWidgetManager, appWidgetIds);
                break;

            case ACTION_START_UPDATES:
                // Enable auto-refresh
                setAutoRefreshEnabled(context, true);
                startAutoRefresh(context);
                updateWidgets(context, appWidgetManager, appWidgetIds);
                break;

            case ACTION_STOP_UPDATES:
                // Disable auto-refresh
                setAutoRefreshEnabled(context, false);
                stopAutoRefresh(context);
                updateWidgets(context, appWidgetManager, appWidgetIds);
                break;

            case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
                // Check if intent has rate data
                if (intent.hasExtra("goldRate")) {
                    String goldRate = intent.getStringExtra("goldRate");
                    String silverRate = intent.getStringExtra("silverRate");
                    String lastUpdated = intent.getStringExtra("lastUpdated");

                    Log.d(TAG, "Received data in widget update: Gold=" + goldRate + ", Silver=" + silverRate);

                    // Add a delay for the loading animation to be visible
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        updateWidgetWithData(context, appWidgetManager, appWidgetIds,
                                goldRate, silverRate, lastUpdated);
                    }, REFRESH_ANIMATION_DELAY);
                } else {
                    Log.d(TAG, "No rate data in update intent, doing regular widget update");
                    // Regular widget update
                    updateWidgets(context, appWidgetManager, appWidgetIds);
                }
                break;
        }
    }

    private void performRefreshAnimation(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "performRefreshAnimation called");

        for (int appWidgetId : appWidgetIds) {
            try {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);

                // Disable refresh button
                views.setBoolean(R.id.refresh_button, "setEnabled", false);

                // Show progress, hide refresh button
                views.setViewVisibility(R.id.refresh_progress, View.VISIBLE);
                views.setViewVisibility(R.id.refresh_button, View.GONE);

                appWidgetManager.updateAppWidget(appWidgetId, views);
            } catch (Exception e) {
                Log.e(TAG, "Error preparing refresh animation for widget " + appWidgetId, e);
            }
        }

        // Fetch new rates after animation
        fetchRatesAndUpdateWidgets(context, appWidgetManager, appWidgetIds);
    }

    private void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "updateWidgets called");

        // Handle refreshing indicator
        for (int appWidgetId : appWidgetIds) {
            // Show loading state
            showLoadingState(context, appWidgetManager, appWidgetId);
        }

        // Fetch fresh data from API
        fetchRatesAndUpdateWidgets(context, appWidgetManager, appWidgetIds);
    }

    private void fetchRatesAndUpdateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "fetchRatesAndUpdateWidgets called");
        RatesRepository repository = new RatesRepository(context);

        repository.fetchRates(new RatesRepository.RatesFetchCallback() {
            @Override
            public void onSuccess(String goldRate, String silverRate, String lastUpdated,
                    String yesterdayGoldRate, String yesterdaySilverRate,
                    String goldChangeValue, String silverChangeValue) {

                Log.d(TAG, "Successfully fetched rates for widget: Gold=" + goldRate + ", Silver=" + silverRate);

                // Show loading state for at least 0.5 seconds
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Update all widgets with fetched data
                    updateWidgetWithData(context, appWidgetManager, appWidgetIds,
                            goldRate, silverRate, lastUpdated);
                }, REFRESH_ANIMATION_DELAY);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching rates for widget: " + errorMessage);

                // Show loading state for at least 0.5 seconds
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Show error state in widgets
                    for (int appWidgetId : appWidgetIds) {
                        showErrorState(context, appWidgetManager, appWidgetId);
                    }
                }, REFRESH_ANIMATION_DELAY);
            }
        });
    }

    private void updateWidgetWithData(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds,
            String goldRate, String silverRate, String lastUpdated) {

        Log.d(TAG, "updateWidgetWithData: Gold=" + goldRate + ", Silver=" + silverRate);

        for (int appWidgetId : appWidgetIds) {
            try {
                // Get the layout for the widget
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);

                // Set the rates data with space between ₹ and rate
                views.setTextViewText(R.id.gold_rate, "₹ " + goldRate);
                views.setTextViewText(R.id.silver_rate, "₹ " + silverRate);
                views.setTextViewText(R.id.rates_updated_time, lastUpdated);

                // Set up refresh button click intent
                Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
                refreshIntent.setAction(ACTION_UPDATE_WIDGET);
                PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                        context, 0, refreshIntent,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                : PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);

                // Re-enable refresh button
                views.setBoolean(R.id.refresh_button, "setEnabled", true);

                // Hide loading indicator, show refresh button
                views.setViewVisibility(R.id.refresh_progress, View.GONE);
                views.setViewVisibility(R.id.refresh_button, View.VISIBLE);

                // Update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views);

                Log.d(TAG, "Widget " + appWidgetId + " updated successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error updating widget " + appWidgetId, e);
            }
        }
    }

    private void showLoadingState(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);

            // Show loading indicator, hide refresh button
            views.setViewVisibility(R.id.refresh_progress, View.VISIBLE);
            views.setViewVisibility(R.id.refresh_button, View.GONE);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            Log.e(TAG, "Error showing loading state for widget " + appWidgetId, e);
        }
    }

    private void showErrorState(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);

            // Set error text in rate fields with space after ₹
            views.setTextViewText(R.id.gold_rate, "₹ Error");
            views.setTextViewText(R.id.silver_rate, "₹ Error");
            views.setTextViewText(R.id.rates_updated_time, "Update Failed");

            // Hide loading indicator, show refresh button
            views.setViewVisibility(R.id.refresh_progress, View.GONE);
            views.setViewVisibility(R.id.refresh_button, View.VISIBLE);

            // Set up refresh button click intent
            Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
            refreshIntent.setAction(ACTION_UPDATE_WIDGET);
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                    context, 0, refreshIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            : PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);

            // Re-enable refresh button
            views.setBoolean(R.id.refresh_button, "setEnabled", true);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            Log.e(TAG, "Error showing error state for widget " + appWidgetId, e);
        }
    }

    // Existing methods for handling auto-refresh
    private void startAutoRefresh(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getAlarmPendingIntent(context);

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + UPDATE_INTERVAL,
                    UPDATE_INTERVAL,
                    pendingIntent);

            Log.d(TAG, "Auto-refresh started");
        }
    }

    private void stopAutoRefresh(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getAlarmPendingIntent(context);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Auto-refresh stopped");
        }
    }

    private PendingIntent getAlarmPendingIntent(Context context) {
        Intent intent = new Intent(context, WidgetUpdateService.class);
        return PendingIntent.getService(
                context, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public boolean isAutoRefreshEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_REFRESH, false);
    }

    private void setAutoRefreshEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_REFRESH, enabled).apply();
    }
}