package com.gcjewellers.rateswidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver that listens for BOOT_COMPLETED broadcast to restart widget updates
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "BootCompletedReceiver: received " + intent.getAction());
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Check if auto-refresh is enabled
            RatesWidgetProvider widgetProvider = new RatesWidgetProvider();
            if (widgetProvider.isAutoRefreshEnabled(context)) {
                // Schedule widget update job
                WidgetUpdateJobScheduler.scheduleJob(context);
                Log.d(TAG, "BootCompletedReceiver: scheduled widget updates after boot");
            }
        }
    }
}