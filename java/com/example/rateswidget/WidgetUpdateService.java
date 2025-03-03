package com.example.rateswidget;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;

public class WidgetUpdateService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        // Update all widgets
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, RatesWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        
        Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(updateIntent);
        
        // Reschedule the job for continuous updates
        scheduleNextUpdate(this);
        
        return false; // Job is done, no more work needed in background
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Reschedule if job is killed
        return true;
    }
    
    public static void scheduleNextUpdate(android.content.Context context) {
        android.app.job.JobScheduler jobScheduler = 
                (android.app.job.JobScheduler) context.getSystemService(android.content.Context.JOB_SCHEDULER_SERVICE);
        
        android.app.job.JobInfo.Builder builder = new android.app.job.JobInfo.Builder(
                1,
                new ComponentName(context, WidgetUpdateService.class))
                .setMinimumLatency(10 * 1000) // 10 seconds
                .setOverrideDeadline(15 * 1000) // Maximum delay of 15 seconds
                .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true);
        
        jobScheduler.schedule(builder.build());
    }
}