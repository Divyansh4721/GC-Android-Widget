package com.gcjewellers.rateswidget;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RatesViewModel extends AndroidViewModel {
    private static final String TAG = "RatesViewModel";

    // LiveData objects for UI updates
    private final MutableLiveData<RatesData> ratesData = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    // Data repository
    private final RatesRepository repository;

    // WebSocket manager for real-time updates
    private RatesUpdater ratesUpdater;

    // Thread pool for background operations
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public RatesViewModel(@NonNull Application application) {
        super(application);
        repository = new RatesRepository(application);
        initRealTimeUpdater();
    }

    public LiveData<RatesData> getRatesData() {
        return ratesData;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void refreshRates() {
        executorService.execute(() -> {
            repository.fetchRates(new RatesRepository.RatesFetchCallback() {
                @Override
                public void onSuccess(String goldRate, String silverRate, String lastUpdated,
                        double goldChangePercent, double silverChangePercent) {
                    RatesData data = new RatesData(
                            goldRate, silverRate, lastUpdated,
                            goldChangePercent, silverChangePercent, false);
                    ratesData.postValue(data);

                    // Update the widget with new data
                    updateWidget(data);
                }

                @Override
                public void onError(String errorMessage) {
                    error.postValue(errorMessage);
                }
            });
        });
    }

    private void initRealTimeUpdater() {
        ratesUpdater = new RatesUpdater();
    }

    public void startRealTimeUpdates() {
        if (ratesUpdater != null && !ratesUpdater.isRunning()) {
            ratesUpdater.start(new RatesUpdater.UpdateListener() {
                @Override
                public void onRatesUpdated(String goldRate, String silverRate, String lastUpdated,
                        double goldChangePercent, double silverChangePercent) {
                    // Create data object with real-time update flag set to true
                    RatesData data = new RatesData(
                            goldRate, silverRate, lastUpdated,
                            goldChangePercent, silverChangePercent, true);
                    ratesData.postValue(data);

                    // Update the widget with new data
                    updateWidget(data);
                }

                @Override
                public void onError(String errorMessage) {
                    error.postValue("Real-time update error: " + errorMessage);
                }
            });
        }
    }

    public void stopRealTimeUpdates() {
        if (ratesUpdater != null) {
            ratesUpdater.stop();
        }
    }

    private void updateWidget(RatesData data) {
        // Update the app widget if it exists
        try {
            // Send broadcast to update the widget
            Intent updateIntent = new Intent(getApplication(), RatesWidgetProvider.class);
            updateIntent.setAction(RatesWidgetProvider.ACTION_REFRESH);

            // Pass data as extras
            updateIntent.putExtra("GOLD_RATE", data.getGoldRate());
            updateIntent.putExtra("SILVER_RATE", data.getSilverRate());
            updateIntent.putExtra("LAST_UPDATED", data.getLastUpdated());

            getApplication().sendBroadcast(updateIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error updating widget", e);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopRealTimeUpdates();
        executorService.shutdown();
    }

    public static class RatesData {
        private final String goldRate;
        private final String silverRate;
        private final String lastUpdated;
        private final double goldChangePercent;
        private final double silverChangePercent;
        private final boolean fromRealTimeUpdate;

        public RatesData(String goldRate, String silverRate, String lastUpdated,
                double goldChangePercent, double silverChangePercent, boolean fromRealTimeUpdate) {
            this.goldRate = goldRate;
            this.silverRate = silverRate;
            this.lastUpdated = lastUpdated;
            this.goldChangePercent = goldChangePercent;
            this.silverChangePercent = silverChangePercent;
            this.fromRealTimeUpdate = fromRealTimeUpdate;
        }

        public String getGoldRate() {
            return goldRate;
        }

        public String getSilverRate() {
            return silverRate;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        public double getGoldChangePercent() {
            return goldChangePercent;
        }

        public double getSilverChangePercent() {
            return silverChangePercent;
        }

        public boolean isFromRealTimeUpdate() {
            return fromRealTimeUpdate;
        }
    }
}