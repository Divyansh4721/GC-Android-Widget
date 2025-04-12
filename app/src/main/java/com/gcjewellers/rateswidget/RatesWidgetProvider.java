package com.gcjewellers.rateswidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RatesWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "RatesWidgetProvider";

    // Preferences constants
    public static final String PREFS_NAME = "widget_prefs";
    public static final String PREF_AUTO_REFRESH_ENABLED = "auto_refresh_enabled";
    public static final String PREF_WIDGET_WIDTH = "widget_width";
    public static final String PREF_WIDGET_HEIGHT = "widget_height";

    // Action constants
    public static final String ACTION_REFRESH = "com.gcjewellers.rateswidget.ACTION_REFRESH";
    public static final String ACTION_START_UPDATES = "com.gcjewellers.rateswidget.START_UPDATES";
    public static final String ACTION_STOP_UPDATES = "com.gcjewellers.rateswidget.STOP_UPDATES";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        
        if (ACTION_REFRESH.equals(intent.getAction())) {
            Log.d(TAG, "Refresh action received");
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, RatesWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            
            for (int appWidgetId : appWidgetIds) {
                updateWidgetWithSize(context, appWidgetManager, appWidgetId);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidgetWithSize(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateWidgetWithSize(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            views.setTextViewText(R.id.widget_title, "GC Jewellers");
            views.setTextViewText(R.id.gold_rate, "Sign In");
            views.setTextColor(R.id.gold_rate, Color.WHITE);
            views.setTextViewText(R.id.silver_rate, "");
            views.setTextColor(R.id.silver_rate, Color.WHITE);
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

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void saveWidgetSize(Context context, int appWidgetId, int width, int height) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_WIDGET_WIDTH + "_" + appWidgetId, width)
              .putInt(PREF_WIDGET_HEIGHT + "_" + appWidgetId, height)
              .apply();
    }

    public static int[] getWidgetSize(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int width = prefs.getInt(PREF_WIDGET_WIDTH + "_" + appWidgetId, 220);
        int height = prefs.getInt(PREF_WIDGET_HEIGHT + "_" + appWidgetId, 100);
        return new int[]{width, height};
    }

    public static int selectBestLayout(int width, int height) {
        // For now, always return the single layout
        return R.layout.rates_widget;
    }

    public static boolean isAutoRefreshEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_AUTO_REFRESH_ENABLED, true);
    }

    private static int getPendingIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE;
        } else {
            return 0;
        }
    }
}