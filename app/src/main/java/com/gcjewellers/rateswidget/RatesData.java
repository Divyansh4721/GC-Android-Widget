package com.gcjewellers.rateswidget;

import java.io.Serializable;

public class RatesData implements Serializable {
    private String goldRate;
    private String silverRate;
    private String lastUpdated;
    private String yesterdayGoldRate;
    private String yesterdaySilverRate;
    private String goldChangeValue;
    private String silverChangeValue;

    // Future rates
    private String goldFutureRate;
    private String silverFutureRate;
    private String goldFutureLow;
    private String goldFutureHigh;
    private String silverFutureLow;
    private String silverFutureHigh;

    // Dollar rates
    private String goldDollarRate;
    private String silverDollarRate;
    private String inrRate;

    // Buy/Sell rates
    private String gold99Buy;
    private String gold99Sell;
    private String goldRefineBuy;
    private String goldRefineSell;
    private String bankGoldBuy;
    private String bankGoldSell;

    // Constructor
    public RatesData(
        String goldRate, String silverRate, String lastUpdated,
        String yesterdayGoldRate, String yesterdaySilverRate,
        String goldChangeValue, String silverChangeValue
    ) {
        this.goldRate = goldRate;
        this.silverRate = silverRate;
        this.lastUpdated = lastUpdated;
        this.yesterdayGoldRate = yesterdayGoldRate;
        this.yesterdaySilverRate = yesterdaySilverRate;
        this.goldChangeValue = goldChangeValue;
        this.silverChangeValue = silverChangeValue;
    }

    // Extended constructor
    public RatesData(
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
        this(goldRate, silverRate, lastUpdated, yesterdayGoldRate, 
             yesterdaySilverRate, goldChangeValue, silverChangeValue);
        
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

    // Getters for all fields
    public String getGoldRate() { return goldRate; }
    public String getSilverRate() { return silverRate; }
    public String getLastUpdated() { return lastUpdated; }
    public String getYesterdayGoldRate() { return yesterdayGoldRate; }
    public String getYesterdaySilverRate() { return yesterdaySilverRate; }
    public String getGoldChangeValue() { return goldChangeValue; }
    public String getSilverChangeValue() { return silverChangeValue; }

    // Future rates getters
    public String getGoldFutureRate() { return goldFutureRate; }
    public String getSilverFutureRate() { return silverFutureRate; }
    public String getGoldFutureLow() { return goldFutureLow; }
    public String getGoldFutureHigh() { return goldFutureHigh; }
    public String getSilverFutureLow() { return silverFutureLow; }
    public String getSilverFutureHigh() { return silverFutureHigh; }

    // Dollar rates getters
    public String getGoldDollarRate() { return goldDollarRate; }
    public String getSilverDollarRate() { return silverDollarRate; }
    public String getInrRate() { return inrRate; }

    // Buy/Sell rates getters
    public String getGold99Buy() { return gold99Buy; }
    public String getGold99Sell() { return gold99Sell; }
    public String getGoldRefineBuy() { return goldRefineBuy; }
    public String getGoldRefineSell() { return goldRefineSell; }
    public String getBankGoldBuy() { return bankGoldBuy; }
    public String getBankGoldSell() { return bankGoldSell; }
}