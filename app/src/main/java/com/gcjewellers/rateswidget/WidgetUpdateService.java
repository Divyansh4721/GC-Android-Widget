package com.gcjewellers.rateswidget;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

/**
 * JobService for updating widget in the background
 */
public class WidgetUpdateService extends JobService {
    private static final String TAG = "WidgetUpdateService";
    
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "WidgetUpdateService: onStartJob");
        
        // Perform widget update in background thread
        new Thread(() -> {
            try {
                RatesRepository repository = new RatesRepository(getApplicationContext());
                repository.fetchRates(new RatesRepository.RatesFetchCallback() {
                    @Override
                    public void onSuccess(String goldRate, String silverRate, String lastUpdated,
                                         double goldChangePercent, double silverChangePercent) {
                        // Create update intent
                        Intent updateIntent = new Intent(getApplicationContext(), RatesWidgetProvider.class);
                        updateIntent.setAction(RatesWidgetProvider.ACTION_REFRESH);
                        updateIntent.putExtra("GOLD_RATE", goldRate);
                        updateIntent.putExtra("SILVER_RATE", silverRate);
                        updateIntent.putExtra("LAST_UPDATED", lastUpdated);
                        
                        // Send broadcast to update widget
                        sendBroadcast(updateIntent);
                        
                        // Job finished
                        jobFinished(params, false);
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "WidgetUpdateService error: " + errorMessage);
                        // Reschedule on failure
                        jobFinished(params, true);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in WidgetUpdateService", e);
                jobFinished(params, true);
            }
        }).start();
        
        // Return true as we're handling the job in a separate thread
        return true;
    }
    
    @Override
    public boolean onStopJob(JobParameters params) {
        // Return true to reschedule the job
        return true;
    }
}