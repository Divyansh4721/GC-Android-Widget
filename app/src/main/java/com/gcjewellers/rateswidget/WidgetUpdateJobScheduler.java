package com.gcjewellers.rateswidget;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * Helper class to schedule and cancel widget update jobs
 */
public class WidgetUpdateJobScheduler {
    private static final String TAG = "WidgetUpdateJobScheduler";
    private static final int JOB_ID = 1001;
    
    /**
     * Schedules the widget update job
     */
    public static void scheduleJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName serviceName = new ComponentName(context, WidgetUpdateService.class);
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, serviceName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(15)) // 15 minutes
                    .setPersisted(true)
                    .build();
                    
            int result = jobScheduler.schedule(jobInfo);
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Widget update job scheduled successfully");
            } else {
                Log.e(TAG, "Widget update job scheduling failed");
            }
        }
    }
    
    /**
     * Cancels the widget update job
     */
    public static void cancelJob(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.cancel(JOB_ID);
            Log.d(TAG, "Widget update job canceled");
        }
    }
}