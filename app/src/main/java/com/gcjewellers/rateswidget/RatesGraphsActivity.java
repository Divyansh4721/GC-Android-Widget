package com.gcjewellers.rateswidget;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RatesGraphsActivity extends AppCompatActivity {
    private static final String TAG = "RatesGraphsActivity";
    private static final String API_BASE_URL = "https://goldrate.divyanshbansal.com/api/rates";

    // UI Components
    private SwitchMaterial autoRefreshSwitch;
    private MaterialButton refreshButton;
    private TextView goldLowestRate;
    private TextView goldHighestRate;
    private TextView silverLowestRate;
    private TextView silverHighestRate;
    private LineChart goldChart;
    private LineChart silverChart;

    // Refresh Handling
    private Handler refreshHandler;
    private Runnable autoRefreshRunnable;

    // Rate Indices and historical data constant
    private static final int GOLD_ROW_INDEX = 5;
    private static final int SILVER_ROW_INDEX = 4;
    private static final int RATE_VALUE_INDEX = 3;
    private static final int HISTORICAL_DAYS = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rates_graphs);

        initializeViews();
        setupCharts();
        setupRefreshButton();
        setupAutoRefresh();
        fetchHistoricalRatesData();
    }

    private void initializeViews() {
        autoRefreshSwitch = findViewById(R.id.auto_refresh_switch);
        refreshButton = findViewById(R.id.refresh_button);
        goldLowestRate = findViewById(R.id.gold_lowest_rate);
        goldHighestRate = findViewById(R.id.gold_highest_rate);
        silverLowestRate = findViewById(R.id.silver_lowest_rate);
        silverHighestRate = findViewById(R.id.silver_highest_rate);
        goldChart = findViewById(R.id.gold_graph);
        silverChart = findViewById(R.id.silver_graph);
        refreshHandler = new Handler(Looper.getMainLooper());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void setupCharts() {
        // Configure both charts with the designated color themes.
        setupChart(goldChart, "#FFD700"); // Gold
        setupChart(silverChart, "#C0C0C0"); // Silver

    }

    private boolean isDarkThemeActive() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupChart(LineChart chart, String lineColor) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setExtraOffsets(10, 10, 10, 10);
        chart.animateX(1000);

        int axisTextColor = isDarkThemeActive() ? Color.WHITE : Color.BLACK;
        int gridColor = isDarkThemeActive() ? Color.DKGRAY : Color.LTGRAY;

        // X Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(axisTextColor);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(gridColor);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(axisTextColor);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());

            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return formatDateFromIndex((int) value);
            }
        });

        // Y Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(axisTextColor);
        leftAxis.setGridColor(gridColor);
        leftAxis.setAxisLineColor(axisTextColor);
        leftAxis.setGranularity(500f);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
    }

    private String formatDateFromIndex(int index) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -(HISTORICAL_DAYS - 1 - index));
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    private void setupRefreshButton() {
        refreshButton.setOnClickListener(v -> fetchHistoricalRatesData());
    }

    private void setupAutoRefresh() {
        autoRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                scheduleAutoRefresh();
            } else {
                cancelAutoRefresh();
            }
        });
    }

    private void scheduleAutoRefresh() {
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (autoRefreshSwitch.isChecked()) {
                    fetchHistoricalRatesData();
                    refreshHandler.postDelayed(this, 60000);
                }
            }
        };
        refreshHandler.postDelayed(autoRefreshRunnable, 60000);
    }

    private void cancelAutoRefresh() {
        if (refreshHandler != null && autoRefreshRunnable != null) {
            refreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    private void fetchHistoricalRatesData() {
        if (!isNetworkAvailable()) {
            showErrorToast("No network connection available");
            return;
        }
        resetRateViews();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        String endDate = dateFormat.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -HISTORICAL_DAYS + 1);
        String startDate = dateFormat.format(cal.getTime());
        String apiUrl = API_BASE_URL + "?startDate=" + startDate + "&endDate=" + endDate;
        Log.d(TAG, "Fetching historical rates from URL: " + apiUrl);
        new FetchHistoricalRatesTask().execute(apiUrl);
    }

    private void resetRateViews() {
        goldLowestRate.setText("Loading...");
        goldHighestRate.setText("Loading...");
        silverLowestRate.setText("Loading...");
        silverHighestRate.setText("Loading...");
    }

    private void updateRatesAndGraphs(List<RateData> historicalData) {
        try {
            if (historicalData == null || historicalData.isEmpty()) {
                throw new Exception("No historical data received");
            }

            List<Float> goldRates = new ArrayList<>();
            List<Float> silverRates = new ArrayList<>();
            Log.d(TAG, "Number of historical data points: " + historicalData.size());

            for (RateData dayData : historicalData) {
                try {
                    Log.d(TAG, "Processing data: " + dayData.toString());
                    float goldRate = dayData.getGoldRate();
                    float silverRate = dayData.getSilverRate();
                    goldRates.add(goldRate);
                    silverRates.add(silverRate);
                    Log.d(TAG, "Gold Rate: " + goldRate + ", Silver Rate: " + silverRate);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing individual data point", e);
                }
            }

            if (goldRates.isEmpty() || silverRates.isEmpty()) {
                throw new Exception("No valid rates extracted");
            }

            final float goldLowestFinal = findMin(goldRates);
            final float goldHighestFinal = findMax(goldRates);
            final float silverLowestFinal = findMin(silverRates);
            final float silverHighestFinal = findMax(silverRates);

            runOnUiThread(() -> {
                goldLowestRate.setText(String.format("Lowest: ₹%.2f", goldLowestFinal));
                goldHighestRate.setText(String.format("Highest: ₹%.2f", goldHighestFinal));
                silverLowestRate.setText(String.format("Lowest: ₹%.2f", silverLowestFinal));
                silverHighestRate.setText(String.format("Highest: ₹%.2f", silverHighestFinal));

                List<Entry> goldEntries = new ArrayList<>();
                List<Entry> silverEntries = new ArrayList<>();
                for (int i = 0; i < goldRates.size(); i++) {
                    goldEntries.add(new Entry(i, goldRates.get(i)));
                }
                for (int i = 0; i < silverRates.size(); i++) {
                    silverEntries.add(new Entry(i, silverRates.get(i)));
                }

                updateGraphWithData(goldChart, goldEntries, "#FFD700");
                updateGraphWithData(silverChart, silverEntries, "#C0C0C0");
            });
        } catch (Exception e) {
            Log.e(TAG, "Error parsing historical rates", e);
            showErrorToast("Error parsing rates: " + e.getMessage());
        }
    }

    private float findMin(List<Float> rates) {
        if (rates == null || rates.isEmpty()) {
            return 0f;
        }
        float min = rates.get(0);
        for (float rate : rates) {
            min = Math.min(min, rate);
        }
        return min;
    }

    private float findMax(List<Float> rates) {
        if (rates == null || rates.isEmpty()) {
            return 0f;
        }
        float max = rates.get(0);
        for (float rate : rates) {
            max = Math.max(max, rate);
        }
        return max;
    }

    private void updateGraphWithData(LineChart chart, List<Entry> entries, String lineColor) {
        if (entries.isEmpty()) {
            chart.clear();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Rates");
        dataSet.setColor(android.graphics.Color.parseColor(lineColor));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(android.graphics.Color.parseColor(lineColor));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false); // Remove distracting number labels

        // Enable a filled area under the curve for enhanced visual appeal.
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(80);
        // Option 1: Use a solid fill color matching the line.

        // dataSet.setFillColor(android.graphics.Color.parseColor(lineColor));
        // Option 2: Uncomment below to use a gradient fill (ensure drawable exists in
        // res/drawable).

        Drawable gradientDrawable = null;
        if (lineColor.equals("#FFD700")) {
            gradientDrawable = ContextCompat.getDrawable(this, R.drawable.fade_gold);
        } else if (lineColor.equals("#C0C0C0")) {
            gradientDrawable = ContextCompat.getDrawable(this, R.drawable.fade_silver);
        }
        if (gradientDrawable != null) {
            dataSet.setFillDrawable(gradientDrawable);
        }

        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Render a smooth, curved line.

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.animateX(1000);
        chart.invalidate();
    }

    private void showErrorToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(RatesGraphsActivity.this, message, Toast.LENGTH_LONG).show();
            goldLowestRate.setText("Error");
            goldHighestRate.setText("Error");
            silverLowestRate.setText("Error");
            silverHighestRate.setText("Error");
        });
    }

    private class FetchHistoricalRatesTask extends AsyncTask<String, Void, List<RateData>> {
        @Override
        protected List<RateData> doInBackground(String... params) {
            List<RateData> historicalData = new ArrayList<>();
            HttpURLConnection connection = null;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String responseStr = response.toString();
                    Log.d(TAG, "Full Response: " + responseStr);

                    Gson gson = new Gson();
                    Type listType = new TypeToken<ApiResponse>() {
                    }.getType();
                    ApiResponse apiResponse = gson.fromJson(responseStr, listType);

                    if (apiResponse != null && apiResponse.getData() != null) {
                        for (DataItem dataItem : apiResponse.getData()) {
                            if (dataItem != null && dataItem.getData() != null) {
                                RateData rateData = parseRateData(dataItem.getData());
                                if (rateData != null) {
                                    historicalData.add(rateData);
                                }
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "HTTP error code: " + responseCode);
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    Log.e(TAG, "Error response: " + errorResponse.toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching historical rates", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return historicalData;
        }

        private RateData parseRateData(String dataString) {
            try {
                JsonArray dataArray = JsonParser.parseString(dataString).getAsJsonArray();
                JsonArray goldRow = dataArray.get(GOLD_ROW_INDEX).getAsJsonArray();
                JsonArray silverRow = dataArray.get(SILVER_ROW_INDEX).getAsJsonArray();
                float goldRate = Float.parseFloat(goldRow.get(RATE_VALUE_INDEX).getAsString());
                float silverRate = Float.parseFloat(silverRow.get(RATE_VALUE_INDEX).getAsString());
                return new RateData(goldRate, silverRate);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing rate data", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<RateData> historicalData) {
            if (historicalData == null || historicalData.isEmpty()) {
                showErrorToast("No historical data found");
                return;
            }
            try {
                updateRatesAndGraphs(historicalData);
            } catch (Exception e) {
                Log.e(TAG, "Error processing historical rates", e);
                showErrorToast("Error processing rates data: " + e.getMessage());
            }
        }
    }

    // Supporting classes for JSON parsing.
    private static class ApiResponse {
        private List<DataItem> data;

        public List<DataItem> getData() {
            return data;
        }
    }

    private static class DataItem {
        private String data;

        public String getData() {
            return data;
        }
    }

    private static class RateData {
        private final float goldRate;
        private final float silverRate;

        public RateData(float goldRate, float silverRate) {
            this.goldRate = goldRate;
            this.silverRate = silverRate;
        }

        public float getGoldRate() {
            return goldRate;
        }

        public float getSilverRate() {
            return silverRate;
        }

        @Override
        public String toString() {
            return "RateData{" + "goldRate=" + goldRate + ", silverRate=" + silverRate + '}';
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelAutoRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (autoRefreshSwitch != null && autoRefreshSwitch.isChecked()) {
            scheduleAutoRefresh();
        }
    }
}
