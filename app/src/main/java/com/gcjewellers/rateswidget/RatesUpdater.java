package com.gcjewellers.rateswidget;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class RatesUpdater {
    private static final String TAG = "RatesUpdater";
    private static final long UPDATE_INTERVAL = 60000; // 1 minute
    
    private Timer timer;
    private UpdateListener listener;
    private boolean isRunning = false;
    
    public interface UpdateListener {
        void onRatesUpdated(String goldRate, String silverRate, String lastUpdated,
                          double goldChangePercent, double silverChangePercent);
        void onError(String errorMessage);
    }
    
    public void start(final UpdateListener listener) {
        if (isRunning) {
            stop();
        }
        
        this.listener = listener;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fetchRatesUpdate();
            }
        }, 0, UPDATE_INTERVAL);
        
        isRunning = true;
        Log.d(TAG, "Started real-time rates updates");
    }
    
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isRunning = false;
        Log.d(TAG, "Stopped real-time rates updates");
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    private void fetchRatesUpdate() {
        // Use our repository instead of the old MainActivity.RatesFetcher
        final RatesRepository repository = new RatesRepository(null);
        repository.fetchRates(new RatesRepository.RatesFetchCallback() {
            public void onSuccess(String goldRate, String silverRate, String lastUpdated,
                          double goldChangePercent, double silverChangePercent) {
                if (listener != null) {
                    listener.onRatesUpdated(
                            goldRate, silverRate, lastUpdated,
                            goldChangePercent, silverChangePercent);
                }
            }
            
            public void onError(String errorMessage) {
                if (listener != null) {
                    listener.onError(errorMessage);
                }
            }
        });
    }
}