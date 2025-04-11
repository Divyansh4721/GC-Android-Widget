package com.gcjewellers.rateswidget;

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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
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

    // UI
    private Spinner spinnerSeries;
    private MaterialButtonToggleGroup toggleBuySell;
    private MaterialButton buttonBuy, buttonSell;
    private TextView textLow, textHigh;
    private LineChart lineChart;

    // Full response from the API
    private ApiResponse apiResponse;
    // Holds the Date objects for each data point (parsed from createdAt).
    private final List<Date> dataPointDates = new ArrayList<>();

    // The available series (matching the JSON structure)
    private final String[] seriesOptions = {
            "golddollar", "silverdollar", "dollarinr", "goldfuture",
            "silverfuture", "gold", "goldrefine", "goldrtgs"
    };
    // Which series is currently selected in the Spinner
    private int selectedSeriesIndex = 0;
    // True => Buy is selected; False => Sell
    private boolean isBuySelected = true;

    // For parsing the "createdAt" in ISO8601 format (UTC).
    private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rates_graphs);

        // If your API times are in UTC, set that here.
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        initializeViews();
        setupSpinner();
        setupToggleButtons();
        setupChart();
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
        // Ensure "Buy" is initially selected
        toggleBuySell.check(R.id.button_buy);
        toggleBuySell.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isBuySelected = (checkedId == R.id.button_buy);
                updateUI();
            }
        });
    }

    private void setupChart() {
        // Remove the default description text
        lineChart.getDescription().setEnabled(false);
        // Allow user interactions (scrolling, zooming)
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        // Simple animation
        lineChart.animateX(1000);

        // Hide background grid if desired
        lineChart.setDrawGridBackground(false);

        // X-axis style
        int axisTextColor = isDarkThemeActive() ? Color.WHITE : Color.BLACK;
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(axisTextColor);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        // We'll assign a real ValueFormatter once we load data (in updateChartWithData).

        // Y-axis style
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(axisTextColor);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        lineChart.getAxisRight().setEnabled(false);
    }

    private void fetchRatesData() {
        new FetchRatesTask().execute(API_BASE_URL);
    }

    // Called whenever data changes or the user modifies the series/toggle.
    private void updateUI() {
        if (apiResponse == null) return;
        updateChartWithData();
        updateHighLowText();
    }

    // Build the chart entries from the "data" array, picking the selected row & buy/sell index.
    private void updateChartWithData() {
        dataPointDates.clear();
        List<Entry> entries = new ArrayList<>();
        List<DataItem> dataList = apiResponse.getData();

        for (int i = 0; i < dataList.size(); i++) {
            DataItem item = dataList.get(i);

            // Parse out the date/time
            try {
                Date d = isoFormat.parse(item.getCreatedAt());
                dataPointDates.add(d);
            } catch (ParseException e) {
                // If there's an error, fallback to "now"
                dataPointDates.add(new Date());
                Log.e(TAG, "Error parsing createdAt", e);
            }

            // Parse the "data" field
            try {
                JsonArray outerArray = JsonParser.parseString(item.getData()).getAsJsonArray();
                // We select the row corresponding to the user’s spinner selection
                JsonArray seriesRow = outerArray.get(selectedSeriesIndex).getAsJsonArray();
                // For the graph, index=0 => Buy, index=1 => Sell
                String valueStr = isBuySelected ? seriesRow.get(0).getAsString() : seriesRow.get(1).getAsString();
                float value = Float.parseFloat(valueStr);
                entries.add(new Entry(i, value));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing data array at index " + i, e);
            }
        }

        // Create the line dataset
        LineDataSet dataSet = new LineDataSet(entries, "Rates");
        // Make the line green, slightly thick
        dataSet.setColor(Color.parseColor("#388E3C")); // a shade of green
        dataSet.setLineWidth(2f);

        // Show circles at each data point
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#388E3C"));
        dataSet.setCircleRadius(3f);

        // Draw gradient fill under the line
        dataSet.setDrawFilled(true);
        // The gradient fill can be done with two colors: top -> bottom
        // This will create a gentle fade from a lighter blue down to a slightly different shade.
        dataSet.setGradientColor(
                Color.parseColor("#E3F2FD"), // top color (very light blue)
                Color.parseColor("#BBDEFB")  // bottom color (light blue)
        );
        dataSet.setFillAlpha(200);

        // Hide the point labels
        dataSet.setDrawValues(false);

        // Optionally, a smooth (cubic) line
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Create final LineData and attach to chart
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Set up a custom X-axis formatter that displays date by default,
        // and shows time if the user zooms in. (Similar to your earlier code.)
        lineChart.getXAxis().setValueFormatter(new DateTimeValueFormatter());
        lineChart.invalidate();
    }

    // Display "Lowest: ___" and "Highest: ___" from the stats object
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

    // Custom ValueFormatter that changes to time if user zooms in
    private class DateTimeValueFormatter extends ValueFormatter {
        // Two formatters: date by default, time if zoomed
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        // If the scaleX is greater than this threshold, we show "HH:mm"
        private final float ZOOM_THRESHOLD = 5f;

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            int index = (int) value;
            if (index < 0 || index >= dataPointDates.size()) return "";
            Date d = dataPointDates.get(index);

            // Check how far the user has zoomed horizontally
            float scaleX = lineChart.getViewPortHandler().getScaleX();
            if (scaleX > ZOOM_THRESHOLD) {
                // If zoomed in, show time
                return timeFormat.format(d);
            } else {
                // Else show day/month
                return dateFormat.format(d);
            }
        }
    }

    // Simple AsyncTask to fetch the JSON
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

    // Model classes

    private static class ApiResponse {
        private List<DataItem> data;
        private Stats stats;
        public List<DataItem> getData() { return data; }
        public Stats getStats() { return stats; }
    }

    private static class DataItem {
        // "data" is a JSON string (an array of arrays)
        private String data;
        // "createdAt" is the date/time in ISO8601
        private String createdAt;
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

    // Helper to detect dark mode
    private boolean isDarkThemeActive() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}
