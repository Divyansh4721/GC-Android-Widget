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
    private static final String API_URL = "https://goldrate.divyanshbansal.com/api/live";

    public interface RatesFetchCallback {
        void onSuccess(String goldRate, String silverRate, String lastUpdated,
                       String yesterdayGoldRate, String yesterdaySilverRate,
                       String goldChangeValue, String silverChangeValue);
        void onError(String errorMessage);
    }

    public interface ExtendedRatesFetchCallback {
        void onSuccess(
                String gold995Buy, String gold995Sell, String gold995High, String gold995Low,
                String silverFutureBuy, String silverFutureSell, String silverFutureHigh, String silverFutureLow,
                String goldFuturesBuy, String goldFuturesSell, String goldFuturesHigh, String goldFuturesLow,
                String goldRefineBuy, String goldRefineSell, String goldRefineHigh, String goldRefineLow,
                String goldRtgsBuy, String goldRtgsSell, String goldRtgsHigh, String goldRtgsLow,
                String goldDollarBuy, String goldDollarSell, String goldDollarHigh, String goldDollarLow,
                String silverDollarBuy, String silverDollarSell, String silverDollarHigh, String silverDollarLow,
                String dollarBuy, String dollarSell, String dollarHigh, String dollarLow);

        void onError(String errorMessage);
    }

    private Context context;
    private SharedPreferences prefs;

    public RatesRepository() {
    }

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

                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(
                            goldRate, silverRate, lastUpdated,
                            "8,725.25", "109,800.00",
                            goldChangeValue, silverChangeValue));
                } else {
                    throw new Exception("Server error: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching rates", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
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

                    JSONObject json = new JSONObject(response.toString());

                    String gold995Buy = json.getJSONObject("gold").getString("buy");
                    String gold995Sell = json.getJSONObject("gold").getString("sell");
                    String gold995High = json.getJSONObject("gold").getString("high");
                    String gold995Low = json.getJSONObject("gold").getString("low");

                    String silverFutureBuy = json.getJSONObject("silverfuture").getString("buy");
                    String silverFutureSell = json.getJSONObject("silverfuture").getString("sell");
                    String silverFutureHigh = json.getJSONObject("silverfuture").getString("high");
                    String silverFutureLow = json.getJSONObject("silverfuture").getString("low");

                    String goldFuturesBuy = json.getJSONObject("goldfuture").getString("buy");
                    String goldFuturesSell = json.getJSONObject("goldfuture").getString("sell");
                    String goldFuturesHigh = json.getJSONObject("goldfuture").getString("high");
                    String goldFuturesLow = json.getJSONObject("goldfuture").getString("low");

                    String goldRefineBuy = json.getJSONObject("goldrefine").getString("buy");
                    String goldRefineSell = json.getJSONObject("goldrefine").getString("sell");
                    String goldRefineHigh = json.getJSONObject("goldrefine").getString("high");
                    String goldRefineLow = json.getJSONObject("goldrefine").getString("low");

                    String goldRtgsBuy = json.getJSONObject("goldrtgs").getString("buy");
                    String goldRtgsSell = json.getJSONObject("goldrtgs").getString("sell");
                    String goldRtgsHigh = json.getJSONObject("goldrtgs").getString("high");
                    String goldRtgsLow = json.getJSONObject("goldrtgs").getString("low");

                    String goldDollarBuy = json.getJSONObject("golddollar").getString("buy");
                    String goldDollarSell = json.getJSONObject("golddollar").getString("sell");
                    String goldDollarHigh = json.getJSONObject("golddollar").getString("high");
                    String goldDollarLow = json.getJSONObject("golddollar").getString("low");

                    String silverDollarBuy = json.getJSONObject("silverdollar").getString("buy");
                    String silverDollarSell = json.getJSONObject("silverdollar").getString("sell");
                    String silverDollarHigh = json.getJSONObject("silverdollar").getString("high");
                    String silverDollarLow = json.getJSONObject("silverdollar").getString("low");

                    String dollarBuy = json.getJSONObject("dollarinr").getString("buy");
                    String dollarSell = json.getJSONObject("dollarinr").getString("sell");
                    String dollarHigh = json.getJSONObject("dollarinr").getString("high");
                    String dollarLow = json.getJSONObject("dollarinr").getString("low");

                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(
                            gold995Buy, gold995Sell, gold995High, gold995Low,
                            silverFutureBuy, silverFutureSell, silverFutureHigh, silverFutureLow,
                            goldFuturesBuy, goldFuturesSell, goldFuturesHigh, goldFuturesLow,
                            goldRefineBuy, goldRefineSell, goldRefineHigh, goldRefineLow,
                            goldRtgsBuy, goldRtgsSell, goldRtgsHigh, goldRtgsLow,
                            goldDollarBuy, goldDollarSell, goldDollarHigh, goldDollarLow,
                            silverDollarBuy, silverDollarSell, silverDollarHigh, silverDollarLow,
                            dollarBuy, dollarSell, dollarHigh, dollarLow));
                } else {
                    throw new Exception("Server error: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching rates", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }
}