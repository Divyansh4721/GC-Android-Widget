package com.gcjewellers.rateswidget;

import java.util.HashMap;
import java.util.Map;

public class RateTracker {
    private static final RateTracker instance = new RateTracker();
    
    private final Map<String, String> previousRates = new HashMap<>();
    
    private RateTracker() {}
    
    public static RateTracker getInstance() {
        return instance;
    }
    
    public void trackRate(String rateKey, String rateValue) {
        previousRates.put(rateKey, rateValue);
    }
    
    public String getPreviousRate(String rateKey) {
        return previousRates.getOrDefault(rateKey, null);
    }
    
    public RateChange getChangeInfo(String rateKey, String currentRate) {
        String previousRate = getPreviousRate(rateKey);
        
        if (previousRate == null) {
            trackRate(rateKey, currentRate);
            return new RateChange(0, RateChange.CHANGE_NONE);
        }
        
        try {
            float current = Float.parseFloat(currentRate.replace(",", ""));
            float previous = Float.parseFloat(previousRate.replace(",", ""));
            
            float change = current - previous;
            int changeType;
            
            if (change > 0) {
                changeType = RateChange.CHANGE_UP;
            } else if (change < 0) {
                changeType = RateChange.CHANGE_DOWN;
            } else {
                changeType = RateChange.CHANGE_NONE;
            }
            
            trackRate(rateKey, currentRate);
            return new RateChange(change, changeType);
            
        } catch (NumberFormatException e) {
            trackRate(rateKey, currentRate);
            return new RateChange(0, RateChange.CHANGE_NONE);
        }
    }
    
    public static class RateChange {
        public static final int CHANGE_NONE = 0;
        public static final int CHANGE_UP = 1;
        public static final int CHANGE_DOWN = 2;
        
        private final float changeAmount;
        private final int changeType;
        
        public RateChange(float changeAmount, int changeType) {
            this.changeAmount = changeAmount;
            this.changeType = changeType;
        }
        
        public float getChangeAmount() {
            return changeAmount;
        }
        
        public int getChangeType() {
            return changeType;
        }
        
        public String getFormattedChange() {
            if (changeType == CHANGE_NONE) {
                return "0";
            } else if (changeType == CHANGE_UP) {
                return "+" + Math.round(changeAmount);
            } else {
                return String.valueOf(Math.round(changeAmount));
            }
        }
    }
}