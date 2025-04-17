package com.gcjewellers.rateswidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RatesRepository {
    private static final String TAG = "RatesRepository";
    private static final String API_URL = "https://goldrate.divyanshbansal.com/api/LIVE";

    public interface RatesFetchCallback {
        void onSuccess(String goldRate, String silverRate, String lastUpdated,
                       String yesterdayGoldRate, String yesterdaySilverRate,
                       String goldChangeValue, String silverChangeValue);
        void onError(String errorMessage);
    }

    public interface ExtendedRatesFetchCallback {
        void onSuccess(
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
        );
        void onError(String errorMessage);
    }

    private Context context;
    private SharedPreferences prefs;

    public RatesRepository() {}

    public RatesRepository(Context context) {
        this.context = context;
        if (context != null) {
            this.prefs = context.getSharedPreferences("RatesData", Context.MODE_PRIVATE);
        }
    }

    public void fetchRates(RatesFetchCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject jsonObject = new JSONObject(response.toString());
                    
                    String goldRate = jsonObject.getJSONObject("gold").getString("sell");
                    String silverRate = jsonObject.getJSONObject("silverfuture").getString("sell");
                    String goldChangeValue = "0"; 
                    String silverChangeValue = "0"; 
                    
                    String lastUpdated = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                    
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onSuccess(
                            goldRate, silverRate, lastUpdated, 
                            "8,725.25", "109,800.00", 
                            goldChangeValue, silverChangeValue
                        )
                    );
                } else {
                    throw new Exception("Server error: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching rates", e);
                new Handler(Looper.getMainLooper()).post(() -> 
                    callback.onError(e.getMessage())
                );
            }
        }).start();
    }

    public void fetchExtendedRates(ExtendedRatesFetchCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject jsonObject = new JSONObject(response.toString());
                    
                    String goldRate = jsonObject.getJSONObject("gold").getString("sell");
                    String silverRate = jsonObject.getJSONObject("silverfuture").getString("sell");
                    String goldChangeValue = "0"; 
                    String silverChangeValue = "0"; 
                    
                    String goldDollarRate = jsonObject.getJSONObject("golddollar").getString("sell");
                    String silverDollarRate = jsonObject.getJSONObject("silverdollar").getString("sell");
                    String inrRate = jsonObject.getJSONObject("dollarinr").getString("sell");
                    
                    String gold99Buy = jsonObject.getJSONObject("gold").getString("buy");
                    String gold99Sell = jsonObject.getJSONObject("gold").getString("sell");
                    
                    String goldFutureRate = jsonObject.getJSONObject("goldfuture").getString("sell");
                    String silverFutureRate = jsonObject.getJSONObject("silverfuture").getString("sell");
                    
                    String goldFutureLow = jsonObject.getJSONObject("goldfuture").getString("low");
                    String goldFutureHigh = jsonObject.getJSONObject("goldfuture").getString("high");
                    
                    String silverFutureLow = jsonObject.getJSONObject("silverfuture").getString("low");
                    String silverFutureHigh = jsonObject.getJSONObject("silverfuture").getString("high");
                    
                    String lastUpdated = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                    
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onSuccess(
                            goldRate, silverRate, lastUpdated, 
                            "8,725.25", "109,800.00", 
                            goldChangeValue, silverChangeValue,
                            
                            goldFutureRate, silverFutureRate,
                            goldFutureLow, goldFutureHigh,
                            silverFutureLow, silverFutureHigh,
                            
                            goldDollarRate, silverDollarRate, inrRate,
                            
                            gold99Buy, gold99Sell,
                            gold99Buy, gold99Sell,
                            "96600", "97600"
                        )
                    );
                } else {
                    throw new Exception("Server error: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching extended rates", e);
                new Handler(Looper.getMainLooper()).post(() -> 
                    callback.onError(e.getMessage())
                );
            }
        }).start();
    }
}