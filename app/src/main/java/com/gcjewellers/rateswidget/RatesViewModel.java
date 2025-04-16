package com.gcjewellers.rateswidget;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class RatesViewModel extends AndroidViewModel {
    private static final String TAG = "RatesViewModel";

    private final MutableLiveData<RatesData> ratesData = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final RatesRepository repository;

    public RatesViewModel(@NonNull Application application) {
        super(application);
        repository = new RatesRepository(application);
    }

    public LiveData<RatesData> getRatesData() {
        return ratesData;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void refreshRates() {
        Log.d(TAG, "Refreshing rates...");
        repository.fetchRates(new RatesRepository.RatesFetchCallback() {
            @Override
            public void onSuccess(String goldRate, String silverRate, String lastUpdated,
                                 String yesterdayGoldRate, String yesterdaySilverRate,
                                 String goldChangeValue, String silverChangeValue) {
                
                // Try to convert change values to doubles for UI formatting
                double goldChangePercent = 0;
                double silverChangePercent = 0;
                
                try {
                    goldChangePercent = Double.parseDouble(goldChangeValue);
                    silverChangePercent = Double.parseDouble(silverChangeValue);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing change values", e);
                }
                
                RatesData data = new RatesData(goldRate, silverRate, lastUpdated, 
                        goldChangePercent, silverChangePercent, false);
                ratesData.postValue(data);
            }

            @Override
            public void onError(String errorMessage) {
                error.postValue(errorMessage);
            }
        });
    }

    public static class RatesData {
        private final String goldRate;
        private final String silverRate;
        private final String lastUpdated;
        private final double goldChangePercent;
        private final double silverChangePercent;
        private final boolean fromRealTimeUpdate;

        public RatesData(String goldRate, String silverRate, String lastUpdated,
                         double goldChangePercent, double silverChangePercent,
                         boolean fromRealTimeUpdate) {
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