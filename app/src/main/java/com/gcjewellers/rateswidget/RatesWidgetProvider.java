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
import android.os.SystemClock;
import android.util.Log;
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
        if (action == null) return;
        
        Log.d(TAG, "onReceive called with action: " + action);
        
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, RatesWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        
        switch (action) {
            case ACTION_UPDATE_WIDGET:
                // Manual refresh button clicked
                fetchRatesAndUpdateWidgets(context, appWidgetManager, appWidgetIds);
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
                    String goldChangeValue = intent.getStringExtra("goldChangeValue");
                    String silverChangeValue = intent.getStringExtra("silverChangeValue");
                    
                    Log.d(TAG, "Received data in widget update: Gold=" + goldRate + ", Silver=" + silverRate);
                    
                    updateWidgetWithData(context, appWidgetManager, appWidgetIds, 
                            goldRate, silverRate, lastUpdated,
                            goldChangeValue, silverChangeValue);
                } else {
                    Log.d(TAG, "No rate data in update intent, doing regular widget update");
                    // Regular widget update
                    updateWidgets(context, appWidgetManager, appWidgetIds);
                }
                break;
        }
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
                
                // Update all widgets with fetched data
                updateWidgetWithData(context, appWidgetManager, appWidgetIds,
                        goldRate, silverRate, lastUpdated,
                        goldChangeValue, silverChangeValue);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching rates for widget: " + errorMessage);
                
                // Show error state in widgets
                for (int appWidgetId : appWidgetIds) {
                    showErrorState(context, appWidgetManager, appWidgetId);
                }
            }
        });
    }
    
    private void updateWidgetWithData(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds,
                                     String goldRate, String silverRate, String lastUpdated,
                                     String goldChangeValue, String silverChangeValue) {
        
        Log.d(TAG, "updateWidgetWithData: Gold=" + goldRate + ", Silver=" + silverRate);
        
        for (int appWidgetId : appWidgetIds) {
            try {
                // Get the layout for the widget
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);
                
                // Set the rates data
                views.setTextViewText(R.id.widget_gold_rate, "₹" + goldRate);
                views.setTextViewText(R.id.widget_silver_rate, "₹" + silverRate);
                views.setTextViewText(R.id.widget_last_updated, "Updated: " + lastUpdated);
                
                // Handle price changes
                try {
                    double goldChange = Double.parseDouble(goldChangeValue);
                    double silverChange = Double.parseDouble(silverChangeValue);
                    
                    // Format with + sign for positive values
                    String formattedGoldChange = (goldChange >= 0 ? "+" : "") + goldChangeValue;
                    String formattedSilverChange = (silverChange >= 0 ? "+" : "") + silverChangeValue;
                    
                    views.setTextViewText(R.id.widget_gold_change, formattedGoldChange);
                    views.setTextViewText(R.id.widget_silver_change, formattedSilverChange);
                    
                    // Set color based on change direction
                    int goldChangeColor = goldChange >= 0 ? 
                            context.getResources().getColor(R.color.price_up) : 
                            context.getResources().getColor(R.color.price_down);
                    
                    int silverChangeColor = silverChange >= 0 ? 
                            context.getResources().getColor(R.color.price_up) : 
                            context.getResources().getColor(R.color.price_down);
                    
                    views.setTextColor(R.id.widget_gold_change, goldChangeColor);
                    views.setTextColor(R.id.widget_silver_change, silverChangeColor);
                    
                    // Set up/down arrow icons
                    views.setImageViewResource(R.id.widget_gold_arrow, 
                            goldChange >= 0 ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
                    views.setImageViewResource(R.id.widget_silver_arrow, 
                            silverChange >= 0 ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
                    
                } catch (NumberFormatException e) {
                    // Handle parsing errors
                    Log.e(TAG, "Error parsing change values", e);
                    views.setTextViewText(R.id.widget_gold_change, "0");
                    views.setTextViewText(R.id.widget_silver_change, "0");
                }
                
                // Set up refresh button click intent
                Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
                refreshIntent.setAction(ACTION_UPDATE_WIDGET);
                PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                        context, 0, refreshIntent, 
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : 
                                PendingIntent.FLAG_UPDATE_CURRENT);
                views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent);
                
                // Set last updated timestamp
                views.setTextViewText(R.id.widget_last_updated, "Updated: " + lastUpdated);
                
                // Hide loading indicators
                views.setViewVisibility(R.id.widget_loading, android.view.View.GONE);
                views.setViewVisibility(R.id.widget_content, android.view.View.VISIBLE);
                
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
            
            views.setViewVisibility(R.id.widget_loading, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widget_content, android.view.View.GONE);
            
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            Log.e(TAG, "Error showing loading state for widget " + appWidgetId, e);
        }
    }
    
    private void showErrorState(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rates_widget);
            
            views.setTextViewText(R.id.widget_gold_rate, "Error");
            views.setTextViewText(R.id.widget_silver_rate, "Check connection");
            views.setTextViewText(R.id.widget_last_updated, "Update Failed");
            
            views.setViewVisibility(R.id.widget_loading, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_content, android.view.View.VISIBLE);
            
            // Set up refresh button click intent
            Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
            refreshIntent.setAction(ACTION_UPDATE_WIDGET);
            PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                    context, 0, refreshIntent, 
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : 
                            PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent);
            
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } catch (Exception e) {
            Log.e(TAG, "Error showing error state for widget " + appWidgetId, e);
        }
    }
    
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
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                        PendingIntent.FLAG_UPDATE_CURRENT);
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