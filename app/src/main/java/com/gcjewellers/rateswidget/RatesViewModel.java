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
        repository.fetchExtendedRates(new RatesRepository.ExtendedRatesFetchCallback() {
            @Override
            public void onSuccess(
                String goldRate, String silverRate, String lastUpdated,
                String yesterdayGoldRate, String yesterdaySilverRate,
                String goldChangeValue, String silverChangeValue,
                
                String goldFutureRate, String silverFutureRate,
                String goldFutureLow, String goldFutureHigh,
                String silverFutureLow, String silverFutureHigh,
                String goldDollarRate, String silverDollarRate, String inrRate,
                String gold99Buy, String gold99Sell,
                String goldRefineBuy, String goldRefineSell,
                String bankGoldBuy, String bankGoldSell
            ) {
                // Try to convert change values to doubles for UI formatting
                double goldChangePercent = 0;
                double silverChangePercent = 0;
                
                try {
                    goldChangePercent = Double.parseDouble(goldChangeValue);
                    silverChangePercent = Double.parseDouble(silverChangeValue);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing change values", e);
                }
                
                RatesData data = new RatesData(
                    goldRate, silverRate, lastUpdated, 
                    goldChangePercent, silverChangePercent, false,
                    
                    // Additional rates
                    goldFutureRate, silverFutureRate,
                    goldFutureLow, goldFutureHigh,
                    silverFutureLow, silverFutureHigh,
                    goldDollarRate, silverDollarRate, inrRate,
                    gold99Buy, gold99Sell,
                    goldRefineBuy, goldRefineSell,
                    bankGoldBuy, bankGoldSell
                );
                
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

        // Extended rates fields
        private final String goldFutureRate;
        private final String silverFutureRate;
        private final String goldFutureLow;
        private final String goldFutureHigh;
        private final String silverFutureLow;
        private final String silverFutureHigh;
        private final String goldDollarRate;
        private final String silverDollarRate;
        private final String inrRate;
        private final String gold99Buy;
        private final String gold99Sell;
        private final String goldRefineBuy;
        private final String goldRefineSell;
        private final String bankGoldBuy;
        private final String bankGoldSell;

        // Constructor with extended parameters
        public RatesData(
            String goldRate, String silverRate, String lastUpdated,
            double goldChangePercent, double silverChangePercent,
            boolean fromRealTimeUpdate,
            
            // Extended rates
            String goldFutureRate, String silverFutureRate,
            String goldFutureLow, String goldFutureHigh,
            String silverFutureLow, String silverFutureHigh,
            String goldDollarRate, String silverDollarRate, String inrRate,
            String gold99Buy, String gold99Sell,
            String goldRefineBuy, String goldRefineSell,
            String bankGoldBuy, String bankGoldSell
        ) {
            this.goldRate = goldRate;
            this.silverRate = silverRate;
            this.lastUpdated = lastUpdated;
            this.goldChangePercent = goldChangePercent;
            this.silverChangePercent = silverChangePercent;
            this.fromRealTimeUpdate = fromRealTimeUpdate;

            // Extended rates
            this.goldFutureRate = goldFutureRate;
            this.silverFutureRate = silverFutureRate;
            this.goldFutureLow = goldFutureLow;
            this.goldFutureHigh = goldFutureHigh;
            this.silverFutureLow = silverFutureLow;
            this.silverFutureHigh = silverFutureHigh;
            this.goldDollarRate = goldDollarRate;
            this.silverDollarRate = silverDollarRate;
            this.inrRate = inrRate;
            this.gold99Buy = gold99Buy;
            this.gold99Sell = gold99Sell;
            this.goldRefineBuy = goldRefineBuy;
            this.goldRefineSell = goldRefineSell;
            this.bankGoldBuy = bankGoldBuy;
            this.bankGoldSell = bankGoldSell;
        }

        // Existing getters
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

        // New getters for extended rates
        public String getGoldFutureRate() {
            return goldFutureRate;
        }

        public String getSilverFutureRate() {
            return silverFutureRate;
        }

        public String getGoldFutureLow() {
            return goldFutureLow;
        }

        public String getGoldFutureHigh() {
            return goldFutureHigh;
        }

        public String getSilverFutureLow() {
            return silverFutureLow;
        }

        public String getSilverFutureHigh() {
            return silverFutureHigh;
        }

        public String getGoldDollarRate() {
            return goldDollarRate;
        }

        public String getSilverDollarRate() {
            return silverDollarRate;
        }

        public String getInrRate() {
            return inrRate;
        }

        public String getGold99Buy() {
            return gold99Buy;
        }

        public String getGold99Sell() {
            return gold99Sell;
        }

        public String getGoldRefineBuy() {
            return goldRefineBuy;
        }

        public String getGoldRefineSell() {
            return goldRefineSell;
        }

        public String getBankGoldBuy() {
            return bankGoldBuy;
        }

        public String getBankGoldSell() {
            return bankGoldSell;
        }
    }
}