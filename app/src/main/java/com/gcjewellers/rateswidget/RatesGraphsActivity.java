package com.gcjewellers.rateswidget;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RatesGraphsActivity extends AppCompatActivity {

    private static final String TAG = "RatesGraphsActivity";
    // UI Components
    private Spinner spinnerSeries;
    private MaterialButtonToggleGroup toggleBuySell;
    private MaterialButton buttonBuy, buttonSell;
    private TextView textLow, textHigh;
    private LineChart lineChart;

    // UI Components for Time Range Filtering
    private MaterialButtonToggleGroup toggleTimeRange;
    private MaterialButton buttonDay, buttonWeek, buttonMonth, buttonYear, buttonCustom;

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
    private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.getDefault());

    // Extend the enum to support new time ranges
    private enum TimeRange {
        ONE_DAY, ONE_WEEK, ONE_MONTH, ONE_YEAR, CUSTOM
    }

    // Default time range selection
    private TimeRange selectedTimeRange = TimeRange.ONE_WEEK;

    // Variables to hold custom date selections
    private Date customStartDate;
    private Date customEndDate;

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
        // Set our custom marker view (using a built-in system layout)
        lineChart.setMarker(new CustomMarkerView(this));
        fetchRatesData();
    }

    private void setupChart() {
        // Determine colors based on dark mode.
        // In dark mode, we want white text; in light mode, black.
        int axisTextColor = isDarkThemeActive() ? Color.WHITE : Color.BLACK;
        // Use a medium gray for grid lines so they are visible but not overwhelming.
        int gridColor = isDarkThemeActive() ? Color.GRAY : Color.LTGRAY;
        // Set the chart background to match the theme (e.g., black for dark mode).
        int chartBackgroundColor = isDarkThemeActive() ? Color.BLACK : Color.WHITE;

        // Set the chart background.
        lineChart.setBackgroundColor(chartBackgroundColor);

        // Configure X-Axis.
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(axisTextColor);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(gridColor);
        xAxis.setGranularity(1f);
        xAxis.setSpaceMin(0f);
        xAxis.setSpaceMax(0f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                // Your existing formatting logic remains here.
                int index = (int) value;
                if (index < 0 || index >= dataPointDates.size())
                    return "";
                Date d = dataPointDates.get(index);
                float scaleX = lineChart.getViewPortHandler().getScaleX();
                // Return time format if zoomed in, date format if zoomed out.
                return scaleX > 5f ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
                        : new SimpleDateFormat("dd/MM", Locale.getDefault()).format(d);
            }
        });

        // Configure left Y-Axis.
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(axisTextColor);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(gridColor);
        leftAxis.setGranularity(1f);

        // Disable the right Y-Axis or configure it similarly.
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Set description text (if used) and its color.
        lineChart.getDescription().setEnabled(true);
        lineChart.getDescription().setText("Rate Chart");
        lineChart.getDescription().setTextColor(axisTextColor);

        // Configure legend appearance.
        if (lineChart.getLegend() != null) {
            lineChart.getLegend().setTextColor(axisTextColor);
        }

        // Additional chart customizations...
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.animateX(1000);
        lineChart.setExtraOffsets(0f, 0f, 0f, 0f);
        lineChart.setAutoScaleMinMaxEnabled(true);

        lineChart.invalidate();
    }

    private void initializeViews() {
        spinnerSeries = findViewById(R.id.spinner_series);
        toggleBuySell = findViewById(R.id.toggle_buy_sell);
        buttonBuy = findViewById(R.id.button_buy);
        buttonSell = findViewById(R.id.button_sell);
        textLow = findViewById(R.id.text_low);
        textHigh = findViewById(R.id.text_high);
        lineChart = findViewById(R.id.line_chart);

        // Initialize time range toggle controls with updated IDs
        toggleTimeRange = findViewById(R.id.toggle_time_range);
        buttonDay = findViewById(R.id.button_day);
        buttonWeek = findViewById(R.id.button_week);
        buttonMonth = findViewById(R.id.button_month);
        buttonYear = findViewById(R.id.button_year);
        buttonCustom = findViewById(R.id.button_custom);
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
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
        // Set default selection to "Week"
        toggleTimeRange.check(R.id.button_week);
        selectedTimeRange = TimeRange.ONE_WEEK;

        toggleTimeRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            for (int i = 0; i < group.getChildCount(); i++) {
                MaterialButton button = (MaterialButton) group.getChildAt(i);
                if (button.getId() == checkedId && isChecked) {
                    // Set the selected visual style (e.g., background color, text color, etc.)
                    button.setBackgroundColor(ContextCompat.getColor(this,R.color.gold_dark));
                    button.setTextColor(ContextCompat.getColor(this,R.color.dark_text));
                } else {
                    // Revert to default style
                    button.setBackgroundColor(ContextCompat.getColor(this,R.color.light_background));
                    button.setTextColor(ContextCompat.getColor(this,R.color.dark_text));
                }
            }

            // Existing logic for time range selection:
            if (isChecked) {
                if (checkedId == R.id.button_day) {
                    selectedTimeRange = TimeRange.ONE_DAY;
                    fetchRatesData();
                } else if (checkedId == R.id.button_week) {
                    selectedTimeRange = TimeRange.ONE_WEEK;
                    fetchRatesData();
                } else if (checkedId == R.id.button_month) {
                    selectedTimeRange = TimeRange.ONE_MONTH;
                    fetchRatesData();
                } else if (checkedId == R.id.button_year) {
                    selectedTimeRange = TimeRange.ONE_YEAR;
                    fetchRatesData();
                } else if (checkedId == R.id.button_custom) {
                    selectedTimeRange = TimeRange.CUSTOM;
                    showCustomDatePicker();
                }
            }
        });

    }

    /**
     * Opens a DatePickerDialog to allow the user to select custom start and end
     * dates.
     */
    private void showCustomDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        // First, prompt the user for the start date.
        DatePickerDialog startDatePicker = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar startCal = Calendar.getInstance();
                        startCal.set(year, month, dayOfMonth);
                        customStartDate = startCal.getTime();

                        // Next, prompt for the end date.
                        DatePickerDialog endDatePicker = new DatePickerDialog(RatesGraphsActivity.this,
                                new DatePickerDialog.OnDateSetListener() {
                                    @Override
                                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                        Calendar endCal = Calendar.getInstance();
                                        endCal.set(year, month, dayOfMonth);
                                        customEndDate = endCal.getTime();
                                        // Once both dates are selected, fetch data with the new range.
                                        fetchRatesData();
                                    }
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH));
                        endDatePicker.setTitle("Select End Date");
                        endDatePicker.show();
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        startDatePicker.setTitle("Select Start Date");
        startDatePicker.show();
    }

    /**
     * Dynamically builds the API URL using either preset or custom date ranges.
     */
    private String getApiUrl() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate;
        String endDate;

        if (selectedTimeRange == TimeRange.CUSTOM && customStartDate != null && customEndDate != null) {
            startDate = sdf.format(customStartDate);
            endDate = sdf.format(customEndDate);
        } else {
            Calendar calendar = Calendar.getInstance();
            // End date is set to today's date.
            endDate = sdf.format(calendar.getTime());
            switch (selectedTimeRange) {
                case ONE_DAY:
                    calendar.add(Calendar.DATE, -1);
                    break;
                case ONE_WEEK:
                    calendar.add(Calendar.DATE, -7);
                    break;
                case ONE_MONTH:
                    calendar.add(Calendar.MONTH, -1);
                    break;
                case ONE_YEAR:
                    calendar.add(Calendar.DATE, -365);
                    break;
                default:
                    calendar.add(Calendar.DATE, -7);
                    break;
            }
            startDate = sdf.format(calendar.getTime());
        }

        return "https://goldrate.divyanshbansal.com/api/rates?startDate=" + startDate + "&endDate=" + endDate;
    }

    private void fetchRatesData() {
        String apiUrl = getApiUrl();
        Log.d(TAG, "API being hit: " + apiUrl);
        new FetchRatesTask().execute(apiUrl);
    }

    private void updateUI() {
        if (apiResponse == null)
            return;
        updateChartWithData();
        updateHighLowText();
    }

    /**
     * Filters the API data based on the selected time range and updates the chart.
     */
    private void updateChartWithData() {
        dataPointDates.clear();
        List<Entry> entries = new ArrayList<>();

        if (apiResponse == null || apiResponse.getData() == null || apiResponse.getData().isEmpty())
            return;

        // Parse dates from API and store in allDates.
        List<Date> allDates = new ArrayList<>();
        for (DataItem item : apiResponse.getData()) {
            try {
                Date d = isoFormat.parse(item.getCreatedAt());
                allDates.add(d);
            } catch (ParseException e) {
                allDates.add(new Date());
                Log.e(TAG, "Error parsing createdAt", e);
            }
        }

        // Establish a filtering window for non-custom ranges.
        Calendar windowStart = Calendar.getInstance();
        if (selectedTimeRange == TimeRange.ONE_DAY) {
            windowStart.add(Calendar.DATE, -1);
        } else if (selectedTimeRange == TimeRange.ONE_WEEK) {
            windowStart.add(Calendar.DATE, -7);
        } else if (selectedTimeRange == TimeRange.ONE_MONTH) {
            windowStart.add(Calendar.MONTH, -1);
        } else if (selectedTimeRange == TimeRange.ONE_YEAR) {
            windowStart.add(Calendar.DATE, -365);
        }

        int xIndex = 0;
        int i = 0;
        for (Date currentDate : allDates) {
            boolean include = false;
            if (selectedTimeRange == TimeRange.CUSTOM) {
                if (customStartDate != null && customEndDate != null &&
                        !currentDate.before(customStartDate) && !currentDate.after(customEndDate)) {
                    include = true;
                }
            } else {
                if (currentDate.getTime() >= windowStart.getTimeInMillis()) {
                    include = true;
                }
            }
            if (include) {
                dataPointDates.add(currentDate);
                try {
                    DataItem item = apiResponse.getData().get(i);
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

    /**
     * Updates the High and Low TextViews based on the selected series and Buy/Sell
     * toggle.
     */
    private void updateHighLowText() {
        if (apiResponse.getStats() == null) {
            textLow.setText("Lowest: N/A");
            textHigh.setText("Highest: N/A");
            return;
        }
        Price price;
        switch (selectedSeriesIndex) {
            case 0:
                price = isBuySelected ? apiResponse.getStats().getGolddollar().getBuy()
                        : apiResponse.getStats().getGolddollar().getSell();
                break;
            case 1:
                price = isBuySelected ? apiResponse.getStats().getSilverdollar().getBuy()
                        : apiResponse.getStats().getSilverdollar().getSell();
                break;
            case 2:
                price = isBuySelected ? apiResponse.getStats().getDollarinr().getBuy()
                        : apiResponse.getStats().getDollarinr().getSell();
                break;
            case 3:
                price = isBuySelected ? apiResponse.getStats().getGoldfuture().getBuy()
                        : apiResponse.getStats().getGoldfuture().getSell();
                break;
            case 4:
                price = isBuySelected ? apiResponse.getStats().getSilverfuture().getBuy()
                        : apiResponse.getStats().getSilverfuture().getSell();
                break;
            case 5:
                price = isBuySelected ? apiResponse.getStats().getGold().getBuy()
                        : apiResponse.getStats().getGold().getSell();
                break;
            case 6:
                price = isBuySelected ? apiResponse.getStats().getGoldrefine().getBuy()
                        : apiResponse.getStats().getGoldrefine().getSell();
                break;
            case 7:
                price = isBuySelected ? apiResponse.getStats().getGoldrtgs().getBuy()
                        : apiResponse.getStats().getGoldrtgs().getSell();
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

        // Update text color based on dark mode
        int textColor = isDarkThemeActive() ? Color.WHITE : Color.BLACK;
        textLow.setTextColor(textColor);
        textHigh.setTextColor(textColor);
    }

    /**
     * Custom ValueFormatter that adapts the X-axis labels based on the zoom level.
     */
    private class DateTimeValueFormatter extends ValueFormatter {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private final float zoomThreshold = 5f;

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            int index = (int) value;
            if (index < 0 || index >= dataPointDates.size())
                return "";
            Date d = dataPointDates.get(index);
            float scaleX = lineChart.getViewPortHandler().getScaleX();
            if (scaleX > zoomThreshold) {
                return timeFormat.format(d);
            } else {
                return dateFormat.format(d);
            }
        }
    }

    /**
     * AsyncTask to fetch JSON from the API.
     */
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
                    Type responseType = new TypeToken<ApiResponse>() {
                    }.getType();
                    return gson.fromJson(responseStr, responseType);
                } else {
                    Log.e(TAG, "HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching rates", e);
            } finally {
                if (connection != null)
                    connection.disconnect();
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

        public List<DataItem> getData() {
            return data;
        }

        public Stats getStats() {
            return stats;
        }
    }

    private static class DataItem {
        private String data; // JSON string representing an array of arrays
        private String createdAt; // ISO date string

        public String getData() {
            return data;
        }

        public String getCreatedAt() {
            return createdAt;
        }
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

        public RateInfo getGolddollar() {
            return golddollar;
        }

        public RateInfo getSilverdollar() {
            return silverdollar;
        }

        public RateInfo getDollarinr() {
            return dollarinr;
        }

        public RateInfo getGoldfuture() {
            return goldfuture;
        }

        public RateInfo getSilverfuture() {
            return silverfuture;
        }

        public RateInfo getGold() {
            return gold;
        }

        public RateInfo getGoldrefine() {
            return goldrefine;
        }

        public RateInfo getGoldrtgs() {
            return goldrtgs;
        }
    }

    private static class RateInfo {
        private Price buy;
        private Price sell;

        public Price getBuy() {
            return buy;
        }

        public Price getSell() {
            return sell;
        }
    }

    private static class Price {
        private float high;
        private float low;

        public float getHigh() {
            return high;
        }

        public float getLow() {
            return low;
        }
    }

    /**
     * Custom MarkerView to display details (rate, date, time) when the user taps a
     * data point.
     */
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

    /**
     * Helper method to detect dark mode.
     */
    private boolean isDarkThemeActive() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}
