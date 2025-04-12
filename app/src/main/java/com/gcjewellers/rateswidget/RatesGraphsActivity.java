package com.gcjewellers.rateswidget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RatesGraphsActivity extends AppCompatActivity {

    private static final String TAG = "RatesGraphsActivity";
    private static final String API_BASE_URL = "https://goldrate.divyanshbansal.com/api/rates?startDate=2025-04-08&endDate=2025-04-11";

    // UI Components
    private Spinner spinnerSeries;
    private MaterialButtonToggleGroup toggleBuySell;
    private MaterialButton buttonBuy, buttonSell;
    private TextView textLow, textHigh;
    private LineChart lineChart;
    
    // New UI Components for Time Range Filtering
    private MaterialButtonToggleGroup toggleTimeRange;
    private MaterialButton buttonOneDay, buttonOneWeek, buttonOneYear;

    // Data from API
    private ApiResponse apiResponse;
    // List to hold Date objects corresponding to each DataItem's createdAt field.
    private final List<Date> dataPointDates = new ArrayList<>();

    // Series options in order
    private final String[] seriesOptions = {
            "golddollar", "silverdollar", "dollarinr", "goldfuture",
            "silverfuture", "gold", "goldrefine", "goldrtgs"
    };
    // Defaults
    private int selectedSeriesIndex = 0;
    private boolean isBuySelected = true; // true = Buy, false = Sell

    // For parsing ISO8601 date coming from API (assumed UTC)
    private final SimpleDateFormat isoFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());

    // New enum for time ranges
    private enum TimeRange {
        ONE_DAY, ONE_WEEK, ONE_YEAR
    }
    // Default time range selection
    private TimeRange selectedTimeRange = TimeRange.ONE_WEEK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rates_graphs);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        initializeViews();
        setupSpinner();
        setupToggleButtons();
        setupTimeRangeToggle(); // Setup the new time-range toggle functionality
        setupChart();
        // Set our custom marker view (now using a built-in system layout)
        lineChart.setMarker(new CustomMarkerView(this));
        fetchRatesData();
    }

    private void initializeViews() {
        spinnerSeries = findViewById(R.id.spinner_series);
        toggleBuySell = findViewById(R.id.toggle_buy_sell);
        buttonBuy = findViewById(R.id.button_buy);
        buttonSell = findViewById(R.id.button_sell);
        textLow = findViewById(R.id.text_low);
        textHigh = findViewById(R.id.text_high);
        lineChart = findViewById(R.id.line_chart);
        
        // Initialize new time range toggle controls
        toggleTimeRange = findViewById(R.id.toggle_time_range);
        buttonOneDay = findViewById(R.id.button_one_day);
        buttonOneWeek = findViewById(R.id.button_one_week);
        buttonOneYear = findViewById(R.id.button_one_year);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, seriesOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSeries.setAdapter(adapter);
        spinnerSeries.setSelection(0);
        spinnerSeries.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSeriesIndex = position;
                updateUI();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupToggleButtons() {
        toggleBuySell.check(R.id.button_buy); // Default selection
        toggleBuySell.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isBuySelected = (checkedId == R.id.button_buy);
                updateUI();
            }
        });
    }
    
    private void setupTimeRangeToggle() {
        // Set default selection to "One Week"
        toggleTimeRange.check(R.id.button_one_week);
        toggleTimeRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.button_one_day) {
                    selectedTimeRange = TimeRange.ONE_DAY;
                } else if (checkedId == R.id.button_one_week) {
                    selectedTimeRange = TimeRange.ONE_WEEK;
                } else if (checkedId == R.id.button_one_year) {
                    selectedTimeRange = TimeRange.ONE_YEAR;
                }
                // Refresh the UI (only the graph changes)
                updateUI();
            }
        });
    }

    private void setupChart() {
        // Disable the description text
        lineChart.getDescription().setEnabled(true);
        // Enable touch gestures (panning/zooming)
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        // Animate horizontal drawing
        lineChart.animateX(1000);
        
        // Remove any extra offsets so that zooming focuses on the data points.
        lineChart.setExtraOffsets(0f, 0f, 0f, 0f);
        
        // Enable auto-scale based solely on the data (optional but recommended).
        lineChart.setAutoScaleMinMaxEnabled(true);
    
        int axisTextColor = isDarkThemeActive() ? Color.WHITE : Color.BLACK;
        
        // X-Axis configuration
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(axisTextColor);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        // Remove additional spacing on the left/right of the x-axis.
        xAxis.setSpaceMin(0f);
        xAxis.setSpaceMax(0f);
        // Set a dummy formatter; it will be replaced later after the data loads.
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return "";
            }
        });
    
        // Y-Axis configuration
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(axisTextColor);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        lineChart.getAxisRight().setEnabled(false);
    }

    private void fetchRatesData() {
        new FetchRatesTask().execute(API_BASE_URL);
    }

    // Updates both the chart and high/low TextViews based on API data, selected series, toggle, and time range.
    private void updateUI() {
        if (apiResponse == null) return;
        updateChartWithData();
        updateHighLowText();
    }

    // Filters data according to the selected time range and updates the chart.
    private void updateChartWithData() {
        dataPointDates.clear();
        List<Entry> entries = new ArrayList<>();

        if (apiResponse == null || apiResponse.getData() == null || apiResponse.getData().isEmpty())
            return;

        // Determine the maximum date in the dataset and cache all parsed dates.
        Date maxDate = null;
        List<Date> allDates = new ArrayList<>();
        for (DataItem item : apiResponse.getData()) {
            try {
                Date d = isoFormat.parse(item.getCreatedAt());
                allDates.add(d);
                if (maxDate == null || d.after(maxDate)) {
                    maxDate = d;
                }
            } catch (ParseException e) {
                allDates.add(new Date());
                Log.e(TAG, "Error parsing createdAt", e);
            }
        }
        if (maxDate == null) {
            maxDate = new Date();
        }

        // Calculate threshold based on selected time range.
        long thresholdMillis = maxDate.getTime();
        switch (selectedTimeRange) {
            case ONE_DAY:
                thresholdMillis = maxDate.getTime() - 24 * 60 * 60 * 1000L;
                break;
            case ONE_WEEK:
                thresholdMillis = maxDate.getTime() - 7 * 24 * 60 * 60 * 1000L;
                break;
            case ONE_YEAR:
                thresholdMillis = maxDate.getTime() - 365 * 24 * 60 * 60 * 1000L;
                break;
        }

        // Loop through and add only data points whose date is within the threshold.
        int xIndex = 0;
        int i = 0;
        for (DataItem item : apiResponse.getData()) {
            Date currentDate = allDates.get(i);
            if (currentDate.getTime() >= thresholdMillis) {
                dataPointDates.add(currentDate);
                try {
                    JsonArray outerArray = JsonParser.parseString(item.getData()).getAsJsonArray();
                    JsonArray seriesRow = outerArray.get(selectedSeriesIndex).getAsJsonArray();
                    // Select Buy (index 0) or Sell (index 1) based on toggle.
                    String valueStr = isBuySelected ? seriesRow.get(0).getAsString() : seriesRow.get(1).getAsString();
                    float value = Float.parseFloat(valueStr);
                    entries.add(new Entry(xIndex, value));
                    xIndex++;
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing data at index " + i, e);
                }
            }
            i++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Rates");
        dataSet.setColor(Color.parseColor("#388E3C")); // Green line
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#388E3C"));
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setGradientColor(Color.parseColor("#E3F2FD"), Color.parseColor("#BBDEFB"));
        dataSet.setFillAlpha(200);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getXAxis().setValueFormatter(new DateTimeValueFormatter());
        lineChart.invalidate();
    }

    // Updates the High and Low TextViews based on the stats from the API.
    private void updateHighLowText() {
        if (apiResponse.getStats() == null) {
            textLow.setText("Lowest: N/A");
            textHigh.setText("Highest: N/A");
            return;
        }
        Price price;
        switch (selectedSeriesIndex) {
            case 0:
                price = isBuySelected ? apiResponse.getStats().getGolddollar().getBuy() : apiResponse.getStats().getGolddollar().getSell();
                break;
            case 1:
                price = isBuySelected ? apiResponse.getStats().getSilverdollar().getBuy() : apiResponse.getStats().getSilverdollar().getSell();
                break;
            case 2:
                price = isBuySelected ? apiResponse.getStats().getDollarinr().getBuy() : apiResponse.getStats().getDollarinr().getSell();
                break;
            case 3:
                price = isBuySelected ? apiResponse.getStats().getGoldfuture().getBuy() : apiResponse.getStats().getGoldfuture().getSell();
                break;
            case 4:
                price = isBuySelected ? apiResponse.getStats().getSilverfuture().getBuy() : apiResponse.getStats().getSilverfuture().getSell();
                break;
            case 5:
                price = isBuySelected ? apiResponse.getStats().getGold().getBuy() : apiResponse.getStats().getGold().getSell();
                break;
            case 6:
                price = isBuySelected ? apiResponse.getStats().getGoldrefine().getBuy() : apiResponse.getStats().getGoldrefine().getSell();
                break;
            case 7:
                price = isBuySelected ? apiResponse.getStats().getGoldrtgs().getBuy() : apiResponse.getStats().getGoldrtgs().getSell();
                break;
            default:
                price = null;
                break;
        }
        if (price != null) {
            textLow.setText(String.format(Locale.getDefault(), "Lowest: ₹%.2f", price.getLow()));
            textHigh.setText(String.format(Locale.getDefault(), "Highest: ₹%.2f", price.getHigh()));
        } else {
            textLow.setText("Lowest: N/A");
            textHigh.setText("Highest: N/A");
        }
    }

    // Custom ValueFormatter: by default shows date (dd/MM); when zoomed in, shows time (HH:mm).
    private class DateTimeValueFormatter extends ValueFormatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private final float zoomThreshold = 5f;
        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            int index = (int) value;
            if (index < 0 || index >= dataPointDates.size()) return "";
            Date d = dataPointDates.get(index);
            float scaleX = lineChart.getViewPortHandler().getScaleX();
            if (scaleX > zoomThreshold) {
                return timeFormat.format(d);
            } else {
                return dateFormat.format(d);
            }
        }
    }

    // AsyncTask to fetch JSON from the API.
    private class FetchRatesTask extends AsyncTask<String, Void, ApiResponse> {
        @Override
        protected ApiResponse doInBackground(String... params) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
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
                    Type responseType = new TypeToken<ApiResponse>() {}.getType();
                    return gson.fromJson(responseStr, responseType);
                } else {
                    Log.e(TAG, "HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching rates", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
            return null;
        }
        @Override
        protected void onPostExecute(ApiResponse result) {
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                Log.e(TAG, "No data received from API or data empty");
                return;
            }
            apiResponse = result;
            updateUI();
        }
    }

    // Model classes for JSON parsing
    private static class ApiResponse {
        private List<DataItem> data;
        private Stats stats;
        public List<DataItem> getData() { return data; }
        public Stats getStats() { return stats; }
    }
    private static class DataItem {
        private String data;       // JSON string representing an array of arrays
        private String createdAt;  // ISO date string
        public String getData() { return data; }
        public String getCreatedAt() { return createdAt; }
    }
    private static class Stats {
        private RateInfo golddollar;
        private RateInfo silverdollar;
        private RateInfo dollarinr;
        private RateInfo goldfuture;
        private RateInfo silverfuture;
        private RateInfo gold;
        private RateInfo goldrefine;
        private RateInfo goldrtgs;
        public RateInfo getGolddollar() { return golddollar; }
        public RateInfo getSilverdollar() { return silverdollar; }
        public RateInfo getDollarinr() { return dollarinr; }
        public RateInfo getGoldfuture() { return goldfuture; }
        public RateInfo getSilverfuture() { return silverfuture; }
        public RateInfo getGold() { return gold; }
        public RateInfo getGoldrefine() { return goldrefine; }
        public RateInfo getGoldrtgs() { return goldrtgs; }
    }
    private static class RateInfo {
        private Price buy;
        private Price sell;
        public Price getBuy() { return buy; }
        public Price getSell() { return sell; }
    }
    private static class Price {
        private float high;
        private float low;
        public float getHigh() { return high; }
        public float getLow() { return low; }
    }

    // Helper method to detect dark mode.
    private boolean isDarkThemeActive() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    // Custom MarkerView to display a box with details (rate, date, time) when the user taps a point.
    private class CustomMarkerView extends MarkerView {
        private final TextView tvContent;
        private final SimpleDateFormat markerFormat = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
        public CustomMarkerView(Context context) {
            // Use a built-in simple layout to avoid inflating our own XML.
            super(context, android.R.layout.simple_list_item_1);
            tvContent = findViewById(android.R.id.text1);
            tvContent.setTextColor(Color.WHITE);
            tvContent.setBackgroundColor(Color.DKGRAY);
            tvContent.setPadding(10, 10, 10, 10);
        }
        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            int index = (int) e.getX();
            String dateStr = "";
            if (index >= 0 && index < dataPointDates.size()) {
                dateStr = markerFormat.format(dataPointDates.get(index));
            }
            tvContent.setText("Rate: " + e.getY() + "\n" + dateStr);
            super.refreshContent(e, highlight);
        }
        @Override
        public MPPointF getOffset() {
            return new MPPointF(-(getWidth() / 2f), -getHeight());
        }
    }
}
