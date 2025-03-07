package com.example.rateswidget;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WidgetUpdateService extends JobService {
    private static final String TAG = "WidgetUpdateService";
    private static final int JOB_ID = 1001;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "onStartJob: Updating widgets");
        
        // Update all widgets
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, RatesWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            sendBroadcast(updateIntent);
            Log.d(TAG, "Widget update broadcast sent for " + appWidgetIds.length + " widgets");
        } else {
            Log.d(TAG, "No widgets found to update");
        }
        
        // Reschedule the job for continuous updates
        scheduleNextUpdate(this);
        
        return false; // Job is done, no more work needed in background
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "onStopJob: Job was stopped, rescheduling");
        // Reschedule if job is killed
        return true;
    }
    
    public static void scheduleNextUpdate(Context context) {
        JobScheduler jobScheduler = 
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        
       JobInfo.Builder builder = new JobInfo.Builder(
                  JOB_ID,
                  new ComponentName(context, WidgetUpdateService.class))
                      .setMinimumLatency(300 * 1000) // 5 minutes (changed from 10 seconds)
                      .setOverrideDeadline(310 * 1000) // Maximum delay of 5 minutes + 10 seconds
                      .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPersisted(true);
        
        int result = jobScheduler.schedule(builder.build());
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled successfully");
        } else {
            Log.e(TAG, "Job scheduling failed");
        }
    }
}