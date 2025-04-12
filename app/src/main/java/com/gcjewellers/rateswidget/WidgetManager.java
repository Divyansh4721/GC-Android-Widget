package com.gcjewellers.rateswidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consolidated widget management that combines RatesWidgetProvider, WidgetUpdateService,
 * RatesFetchTask, RatesUpdateWorker, and BootCompletedReceiver into a single file.
 */
public class WidgetManager {

    // -------------------------------------------------------------------------
    // RatesWidgetProvider: Handles widget updates and configuration
    // -------------------------------------------------------------------------
    public static class RatesWidgetProvider extends AppWidgetProvider {
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
                // Setup refresh intent so that clicking the refresh button sends a broadcast
                Intent refreshIntent = new Intent(context, RatesWidgetProvider.class);
                refreshIntent.setAction(ACTION_REFRESH);
                PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        refreshIntent,
                        getPendingIntentFlag()
                );
                views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);

                // Launch asynchronous task to fetch and update rates
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

        // Utility method for PendingIntent flag based on Android version.
        private static int getPendingIntentFlag() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return PendingIntent.FLAG_IMMUTABLE;
            } else {
                return 0;
            }
        }

        public boolean isAutoRefreshEnabled(MainActivity mainActivity) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // WidgetUpdateService: JobService to schedule widget refreshes using JobScheduler
    // -------------------------------------------------------------------------
    public static class WidgetUpdateService extends JobService {
        private static final String TAG = "WidgetUpdateService";
        private static final int JOB_ID = 1001;

        @Override
        public boolean onStartJob(JobParameters params) {
            Log.d(TAG, "onStartJob: Updating widgets");

            // Check auto-refresh flag from shared preferences before updating
            SharedPreferences prefs = getSharedPreferences(RatesWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
            boolean autoRefreshEnabled = prefs.getBoolean(RatesWidgetProvider.PREF_AUTO_REFRESH_ENABLED, true);

            if (!autoRefreshEnabled) {
                Log.d(TAG, "Auto-refresh is disabled, skipping update");
                return false;
            }

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

            // Reschedule if auto-refresh is enabled
            if (autoRefreshEnabled) {
                scheduleNextUpdate(this);
            }

            return false; // Job complete; no ongoing background work.
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            Log.d(TAG, "onStopJob: Job was stopped, rescheduling if auto-refresh is enabled");
            SharedPreferences prefs = getSharedPreferences(RatesWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
            boolean autoRefreshEnabled = prefs.getBoolean(RatesWidgetProvider.PREF_AUTO_REFRESH_ENABLED, true);
            return autoRefreshEnabled;
        }

        // Schedules the next widget update using JobScheduler.
        public static void scheduleNextUpdate(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(RatesWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE);
            boolean autoRefreshEnabled = prefs.getBoolean(RatesWidgetProvider.PREF_AUTO_REFRESH_ENABLED, true);

            if (!autoRefreshEnabled) {
                Log.d(TAG, "Auto-refresh is disabled, not scheduling next update");
                return;
            }

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            JobInfo.Builder builder = new JobInfo.Builder(
                    JOB_ID,
                    new ComponentName(context, WidgetUpdateService.class)
            )
                    .setMinimumLatency(600 * 1000)        // 10 minutes delay
                    .setOverrideDeadline(610 * 1000)       // 10 minutes + 10 sec max delay
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

    // -------------------------------------------------------------------------
    // RatesFetchTask: Asynchronously fetches rates and updates widget views
    // -------------------------------------------------------------------------
    public static class RatesFetchTask extends AsyncTask<Void, Void, String[]> {
        private static final String TAG = "RatesFetchTask";

        private Context context;
        private RemoteViews views;
        private AppWidgetManager appWidgetManager;
        private int appWidgetId;

        public RatesFetchTask(Context context, RemoteViews views, AppWidgetManager appWidgetManager, int appWidgetId) {
            this.context = context;
            this.views = views;
            this.appWidgetManager = appWidgetManager;
            this.appWidgetId = appWidgetId;
        }

        @Override
        protected void onPreExecute() {
            // Display loading state in the widget.
            views.setTextViewText(R.id.gold_rate, "Loading...");
            views.setTextViewText(R.id.silver_rate, "Loading...");
            views.setTextColor(R.id.gold_rate, Color.WHITE);
            views.setTextColor(R.id.silver_rate, Color.WHITE);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            final String[] rates = new String[2]; // [gold, silver]
            boolean hasError = false;
            String errorMessage = "";

            try {
                Log.d(TAG, "Making API request to fetch rates");
                // Example API URL – replace with your actual endpoint as needed.
                URL url = new URL("https://goldrate.divyanshbansal.com/api/live");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "API response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();
                    // Parse the JSON response; the parsing logic below assumes a specific structure.
                    JSONArray jsonArray = new JSONArray(responseBuilder.toString());
                    int GOLD_ROW = 5;
                    int SILVER_ROW = 4;
                    int RATE_COLUMN = 1;
                    if (jsonArray.length() > Math.max(GOLD_ROW, SILVER_ROW)) {
                        JSONArray goldRow = jsonArray.getJSONArray(GOLD_ROW);
                        JSONArray silverRow = jsonArray.getJSONArray(SILVER_ROW);
                        if (goldRow.length() > RATE_COLUMN && silverRow.length() > RATE_COLUMN) {
                            rates[0] = goldRow.getString(RATE_COLUMN);
                            rates[1] = silverRow.getString(RATE_COLUMN);
                            Log.d(TAG, "Gold rate: " + rates[0] + ", Silver rate: " + rates[1]);
                        } else {
                            hasError = true;
                            errorMessage = "Invalid response format: rate column not found";
                        }
                    } else {
                        hasError = true;
                        errorMessage = "Invalid response format: required rows not found";
                    }
                } else {
                    hasError = true;
                    errorMessage = "Server returned code: " + responseCode;
                    Log.e(TAG, errorMessage);
                }
                connection.disconnect();
            } catch (Exception e) {
                hasError = true;
                errorMessage = "Error: " + e.getMessage();
                Log.e(TAG, "API request failed", e);
            }

            // Get current time stamp to represent last updated
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String currentTime = dateFormat.format(new Date());
            final String goldRate = rates[0] != null ? rates[0] : "N/A";
            final String silverRate = rates[1] != null ? rates[1] : "N/A";

            // Post result to the widget update on the main thread.
            boolean finalHasError = hasError;
            runOnUiThread(() -> {
                if (finalHasError) {
                    // Additional error handling could be added here.
                }
            });

            return new String[]{goldRate, silverRate, currentTime};
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null && result[0] != null && result[1] != null) {
                views.setTextViewText(R.id.gold_rate, "₹" + result[0]);
                views.setTextColor(R.id.gold_rate, Color.parseColor("#FFD700"));
                views.setTextViewText(R.id.silver_rate, "₹" + result[1]);
                views.setTextColor(R.id.silver_rate, Color.parseColor("#C0C0C0"));
                views.setTextViewText(R.id.rates_updated_time, "Updated at " + result[2]);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            } else {
                views.setTextViewText(R.id.gold_rate, "Error");
                views.setTextViewText(R.id.silver_rate, "Error");
                views.setTextColor(R.id.gold_rate, Color.RED);
                views.setTextColor(R.id.silver_rate, Color.RED);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }

        // Helper method to run code on the UI thread.
        private void runOnUiThread(Runnable action) {
            // This example assumes context is an Activity; adjust if necessary.
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(action);
            }
        }
    }

    // -------------------------------------------------------------------------
    // RatesUpdateWorker: WorkManager worker alternative to trigger widget refresh
    // -------------------------------------------------------------------------
    public static class RatesUpdateWorker extends Worker {
        public RatesUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Intent intent = new Intent(getApplicationContext(), RatesWidgetProvider.class);
                intent.setAction(RatesWidgetProvider.ACTION_REFRESH);
                getApplicationContext().sendBroadcast(intent);
            }
            return Result.success();
        }
    }

    // -------------------------------------------------------------------------
    // BootCompletedReceiver: Listens for boot completion to schedule widget updates
    // -------------------------------------------------------------------------
    public static class BootCompletedReceiver extends BroadcastReceiver {
        private static final String TAG = "BootCompletedReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Log.d(TAG, "Boot completed received");

                // Schedule the widget update service
                WidgetUpdateService.scheduleNextUpdate(context);

                // Trigger an immediate widget update broadcast
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisWidget = new ComponentName(context, RatesWidgetProvider.class);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

                if (appWidgetIds != null && appWidgetIds.length > 0) {
                    Intent updateIntent = new Intent(context, RatesWidgetProvider.class);
                    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                    context.sendBroadcast(updateIntent);
                    Log.d(TAG, "Widget update broadcast sent");
                } else {
                    Log.d(TAG, "No widgets found to update");
                }
            }
        }
    }
}
