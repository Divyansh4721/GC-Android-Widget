package com.gcjewellers.rateswidget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class WidgetUpdateService extends Service {
    private static final String TAG = "WidgetUpdateService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started to update widgets");
        
        // Create repository and fetch latest rates
        RatesRepository repository = new RatesRepository(this);
        
        repository.fetchRates(new RatesRepository.RatesFetchCallback() {
            @Override
            public void onSuccess(String goldRate, String silverRate, String lastUpdated,
                                 String yesterdayGoldRate, String yesterdaySilverRate,
                                 String goldChangeValue, String silverChangeValue) {
                
                Log.d(TAG, "Got rates in service: Gold=" + goldRate + ", Silver=" + silverRate);
                
                // Create the update intent with the new data
                Intent updateIntent = new Intent(WidgetUpdateService.this, RatesWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                
                // Add the AppWidget IDs to be updated
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetUpdateService.this);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                        new ComponentName(WidgetUpdateService.this, RatesWidgetProvider.class));
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                
                // Add rates data - no need to format with ₹ symbol as the widget handles that
                updateIntent.putExtra("goldRate", goldRate);
                updateIntent.putExtra("silverRate", silverRate);
                updateIntent.putExtra("lastUpdated", lastUpdated);
                updateIntent.putExtra("yesterdayGoldRate", yesterdayGoldRate);
                updateIntent.putExtra("yesterdaySilverRate", yesterdaySilverRate);
                updateIntent.putExtra("goldChangeValue", goldChangeValue);
                updateIntent.putExtra("silverChangeValue", silverChangeValue);
                
                // Send the broadcast to update widgets
                sendBroadcast(updateIntent);
                
                Log.d(TAG, "Widget update broadcast sent with new rates");
                stopSelf(startId);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching rates in service: " + errorMessage);
                
                // Even with an error, we should still broadcast to show error state
                Intent updateIntent = new Intent(WidgetUpdateService.this, RatesWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetUpdateService.this);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                        new ComponentName(WidgetUpdateService.this, RatesWidgetProvider.class));
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                updateIntent.putExtra("error", true);
                
                sendBroadcast(updateIntent);
                
                stopSelf(startId);
            }
        });
        
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}