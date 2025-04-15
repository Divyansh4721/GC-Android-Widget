package com.gcjewellers.rateswidget;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Provides comprehensive analytics and insights on gold and silver rates
 */
public class AnalyticsDashboardActivity extends AppCompatActivity {
    private static final String TAG = "AnalyticsDashboard";
    
    // UI Components
    private Toolbar toolbar;
    private TabLayout periodTabLayout;
    private Spinner dataTypeSpinner;
    private LineChart trendLineChart;
    private BarChart monthlyComparisonChart;
    private PieChart marketShareChart;
    private RecyclerView newsRecyclerView;
    private TextView insightsSummaryText;
    
    // Time period options
    private static final int PERIOD_WEEK = 0;
    private static final int PERIOD_MONTH = 1;
    private static final int PERIOD_YEAR = 2;
    private int currentPeriod = PERIOD_MONTH;
    
    // Data type options (Gold or Silver)
    private static final int DATA_GOLD = 0;
    private static final int DATA_SILVER = 1;
    private static final int DATA_BOTH = 2;
    private int currentDataType = DATA_BOTH;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics_dashboard);
        
        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Analytics Dashboard");
        }
        
        // Initialize UI components
        initializeViews();
        
        // Set up listeners
        setupTabLayoutListener();
        setupSpinnerListener();
        
        // Load initial data
        loadDashboardData();
    }
    
    private void initializeViews() {
        periodTabLayout = findViewById(R.id.period_tab_layout);
        dataTypeSpinner = findViewById(R.id.data_type_spinner);
        trendLineChart = findViewById(R.id.trend_line_chart);
        monthlyComparisonChart = findViewById(R.id.monthly_comparison_chart);
        marketShareChart = findViewById(R.id.market_share_chart);
        newsRecyclerView = findViewById(R.id.news_recycler_view);
        insightsSummaryText = findViewById(R.id.insights_summary_text);
        
        // Set up the spinner with options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.data_type_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataTypeSpinner.setAdapter(adapter);
        
        // Set up RecyclerView
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newsRecyclerView.setAdapter(new MarketNewsAdapter(generateSampleNewsItems()));
        
        // Initialize charts
        setupLineChart();
        setupBarChart();
        setupPieChart();
    }
    
    private void setupTabLayoutListener() {
        periodTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Update selected period and refresh data
                currentPeriod = tab.getPosition();
                loadDashboardData();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }
    
    private void setupSpinnerListener() {
        dataTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Update selected data type and refresh data
                currentDataType = position;
                loadDashboardData();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not needed
            }
        });
    }
    
    private void setupLineChart() {
        // Configure the appearance and behavior of the line chart
        trendLineChart.getDescription().setEnabled(false);
        trendLineChart.setTouchEnabled(true);
        trendLineChart.setDragEnabled(true);
        trendLineChart.setScaleEnabled(true);
        trendLineChart.setPinchZoom(true);
        trendLineChart.setDrawGridBackground(false);
        
        // Customize axis
        XAxis xAxis = trendLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        YAxis leftAxis = trendLineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        
        trendLineChart.getAxisRight().setEnabled(false);
        
        // Customize legend
        Legend legend = trendLineChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }
    
    private void setupBarChart() {
        // Configure the appearance and behavior of the bar chart
        monthlyComparisonChart.getDescription().setEnabled(false);
        monthlyComparisonChart.setDrawGridBackground(false);
        monthlyComparisonChart.setDrawBarShadow(false);
        monthlyComparisonChart.setHighlightFullBarEnabled(false);
        
        // Customize axis
        XAxis xAxis = monthlyComparisonChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        
        YAxis leftAxis = monthlyComparisonChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        
        monthlyComparisonChart.getAxisRight().setEnabled(false);
        
        // Customize legend
        Legend legend = monthlyComparisonChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }
    
    private void setupPieChart() {
        // Configure the appearance and behavior of the pie chart
        marketShareChart.getDescription().setEnabled(false);
        marketShareChart.setUsePercentValues(true);
        marketShareChart.setExtraOffsets(5, 10, 5, 5);
        marketShareChart.setDragDecelerationFrictionCoef(0.95f);
        marketShareChart.setCenterText("Market\nDistribution");
        marketShareChart.setCenterTextSize(16f);
        marketShareChart.setDrawHoleEnabled(true);
        marketShareChart.setHoleColor(Color.WHITE);
        marketShareChart.setTransparentCircleRadius(61f);
        
        // Customize legend
        Legend legend = marketShareChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }
    
    private void loadDashboardData() {
        // Load data based on selected period and data type
        updateTrendLineChart();
        updateMonthlyComparisonChart();
        updateMarketShareChart();
        updateInsightsSummary();
    }
    
    private void updateTrendLineChart() {
        ArrayList<Entry> goldEntries = new ArrayList<>();
        ArrayList<Entry> silverEntries = new ArrayList<>();
        
        // Generate sample data points based on selected period
        int dataPoints = getDataPointsForPeriod();
        Random random = new Random();
        
        // Baseline values for gold and silver
        float goldBaseValue = 50000f;
        float silverBaseValue = 70000f;
        
        for (int i = 0; i < dataPoints; i++) {
            // Add some random variations to create realistic looking data
            goldEntries.add(new Entry(i, goldBaseValue + random.nextFloat() * 5000 - 2500));
            silverEntries.add(new Entry(i, silverBaseValue + random.nextFloat() * 10000 - 5000));
        }
        
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        
        // Add datasets based on selected data type
        if (currentDataType == DATA_GOLD || currentDataType == DATA_BOTH) {
            LineDataSet goldDataSet = new LineDataSet(goldEntries, "Gold Rate (per 10g)");
            goldDataSet.setColor(Color.rgb(255, 215, 0));
            goldDataSet.setLineWidth(2f);
            goldDataSet.setCircleColor(Color.rgb(255, 215, 0));
            goldDataSet.setCircleRadius(3f);
            goldDataSet.setDrawCircleHole(false);
            goldDataSet.setValueTextSize(9f);
            goldDataSet.setDrawFilled(true);
            goldDataSet.setFillColor(Color.rgb(255, 215, 0));
            goldDataSet.setFillAlpha(50);
            dataSets.add(goldDataSet);
        }
        
        if (currentDataType == DATA_SILVER || currentDataType == DATA_BOTH) {
            LineDataSet silverDataSet = new LineDataSet(silverEntries, "Silver Rate (per kg)");
            silverDataSet.setColor(Color.rgb(192, 192, 192));
            silverDataSet.setLineWidth(2f);
            silverDataSet.setCircleColor(Color.rgb(192, 192, 192));
            silverDataSet.setCircleRadius(3f);
            silverDataSet.setDrawCircleHole(false);
            silverDataSet.setValueTextSize(9f);
            silverDataSet.setDrawFilled(true);
            silverDataSet.setFillColor(Color.rgb(192, 192, 192));
            silverDataSet.setFillAlpha(50);
            dataSets.add(silverDataSet);
        }
        
        // Create LineData object and set up X-axis labels
        LineData lineData = new LineData(dataSets);
        trendLineChart.setData(lineData);
        
        // Set X-axis labels based on period
        setXAxisLabelsForPeriod(trendLineChart.getXAxis());
        
        // Refresh the chart
        trendLineChart.invalidate();
    }
    
    private void updateMonthlyComparisonChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        Random random = new Random();
        
        // Generate sample data for each month or quarter depending on period
        int groupCount = currentPeriod == PERIOD_YEAR ? 4 : 12; // quarters or months
        
        for (int i = 0; i < groupCount; i++) {
            // For simplicity, generate random data
            if (currentDataType == DATA_BOTH) {
                // Grouped bars for both gold and silver
                entries.add(new BarEntry(
                        i, new float[]{
                        45000 + random.nextFloat() * 10000, // Gold
                        65000 + random.nextFloat() * 10000  // Silver
                }));
            } else {
                // Single bar for either gold or silver
                float value = currentDataType == DATA_GOLD ?
                        45000 + random.nextFloat() * 10000 :
                        65000 + random.nextFloat() * 10000;
                entries.add(new BarEntry(i, value));
            }
        }
        
        BarDataSet barDataSet;
        if (currentDataType == DATA_BOTH) {
            barDataSet = new BarDataSet(entries, "Monthly Comparison");
            barDataSet.setColors(
                    Color.rgb(255, 215, 0), // Gold
                    Color.rgb(192, 192, 192) // Silver
            );
            barDataSet.setStackLabels(new String[]{"Gold", "Silver"});
        } else {
            String label = currentDataType == DATA_GOLD ? "Gold" : "Silver";
            barDataSet = new BarDataSet(entries, label + " Rate Comparison");
            barDataSet.setColor(currentDataType == DATA_GOLD ?
                    Color.rgb(255, 215, 0) : // Gold
                    Color.rgb(192, 192, 192)); // Silver
        }
        
        BarData barData = new BarData(barDataSet);
        barData.setValueTextSize(10f);
        monthlyComparisonChart.setData(barData);
        
        // Set X-axis labels based on period
        XAxis xAxis = monthlyComparisonChart.getXAxis();
        if (currentPeriod == PERIOD_YEAR) {
            xAxis.setValueFormatter(new IndexAxisValueFormatter(
                    new String[]{"Q1", "Q2", "Q3", "Q4"}));
        } else {
            xAxis.setValueFormatter(new IndexAxisValueFormatter(
                    new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"}));
        }
        xAxis.setLabelCount(groupCount);
        
        // Refresh the chart
        monthlyComparisonChart.invalidate();
    }
    
    private void updateMarketShareChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        
        // Sample data for market distribution
        entries.add(new PieEntry(40f, "Gold"));
        entries.add(new PieEntry(30f, "Silver"));
        entries.add(new PieEntry(20f, "Platinum"));
        entries.add(new PieEntry(10f, "Others"));
        
        PieDataSet dataSet = new PieDataSet(entries, "Market Distribution");
        dataSet.setColors(
                Color.rgb(255, 215, 0),   // Gold
                Color.rgb(192, 192, 192), // Silver
                Color.rgb(229, 228, 226), // Platinum
                Color.rgb(150, 150, 150)  // Others
        );
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        
        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.WHITE);
        
        marketShareChart.setData(data);
        marketShareChart.invalidate();
    }
    
    private void updateInsightsSummary() {
        // Generate some insight text based on the selected period and data type
        StringBuilder insights = new StringBuilder();
        
        String periodName = "";
        switch (currentPeriod) {
            case PERIOD_WEEK:
                periodName = "week";
                break;
            case PERIOD_MONTH:
                periodName = "month";
                break;
            case PERIOD_YEAR:
                periodName = "year";
                break;
        }
        
        String metalType = "";
        switch (currentDataType) {
            case DATA_GOLD:
                metalType = "Gold";
                insights.append("Gold prices have shown a steady increase over the past ")
                       .append(periodName)
                       .append(", with a 3.2% growth rate. Market analysts attribute this to ")
                       .append("increased global economic uncertainty and higher demand in Asian markets.\n\n");
                break;
            case DATA_SILVER:
                metalType = "Silver";
                insights.append("Silver has experienced notable volatility this ")
                       .append(periodName)
                       .append(", with prices fluctuating between ₹68,000 and ₹72,000 per kg. ")
                       .append("Industrial demand remains a key driver, with the electronics sector ")
                       .append("showing increased consumption.\n\n");
                break;
            case DATA_BOTH:
                metalType = "Precious metals";
                insights.append("Both gold and silver have demonstrated positive trends this ")
                       .append(periodName)
                       .append(". Gold has increased by approximately 2.8%, while silver ")
                       .append("has seen a more modest 1.5% growth. The gold-to-silver ratio ")
                       .append("remains at historically high levels of around 70:1.\n\n");
                break;
        }
        
        // Add predictive analysis
        insights.append("Forecast: ")
               .append(metalType)
               .append(" prices are expected to rise by 1.5-2.5% in the coming weeks ")
               .append("based on current market indicators and seasonal trends.");
        
        insightsSummaryText.setText(insights.toString());
    }
    
    private int getDataPointsForPeriod() {
        switch (currentPeriod) {
            case PERIOD_WEEK:
                return 7; // Daily data for a week
            case PERIOD_MONTH:
                return 30; // Daily data for a month
            case PERIOD_YEAR:
                return 12; // Monthly data for a year
            default:
                return 30;
        }
    }
    
    private void setXAxisLabelsForPeriod(XAxis xAxis) {
        final String[] labels;
        
        switch (currentPeriod) {
            case PERIOD_WEEK:
                // Show day names for week view
                labels = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                break;
            case PERIOD_MONTH:
                // Show dates for month view (just show every 5 days for clarity)
                labels = new String[30];
                for (int i = 0; i < 30; i++) {
                    labels[i] = String.valueOf(i + 1);
                }
                break;
            case PERIOD_YEAR:
                // Show month names for year view
                labels = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                break;
            default:
                labels = new String[]{};
                break;
        }
        
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.length);
    }
    
    private List<MarketNewsItem> generateSampleNewsItems() {
        List<MarketNewsItem> newsItems = new ArrayList<>();
        
        // Add some sample news items
        newsItems.add(new MarketNewsItem(
                "Gold hits new high as global tensions rise",
                "Increasing geopolitical uncertainties drive investors towards safe haven assets",
                "2 hours ago"));
        
        newsItems.add(new MarketNewsItem(
                "Silver demand surges in industrial sector",
                "Growing electronics and renewable energy sectors boost silver consumption",
                "5 hours ago"));
        
        newsItems.add(new MarketNewsItem(
                "Central bank policies impact precious metals",
                "Interest rate decisions and inflation concerns influence market sentiment",
                "Yesterday"));
        
        newsItems.add(new MarketNewsItem(
                "Import duties revised for gold and silver",
                "Government announces changes to customs tariffs on precious metals",
                "2 days ago"));
                
        newsItems.add(new MarketNewsItem(
                "Festival season expected to boost jewelry demand",
                "Analysts predict increased consumer buying during upcoming celebrations",
                "3 days ago"));
                
        return newsItems;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button in toolbar
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Model class for market news items
     */
    static class MarketNewsItem {
        private final String title;
        private final String description;
        private final String timestamp;
        
        public MarketNewsItem(String title, String description, String timestamp) {
            this.title = title;
            this.description = description;
            this.timestamp = timestamp;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Adapter for displaying market news items
     */
    class MarketNewsAdapter extends RecyclerView.Adapter<MarketNewsAdapter.NewsViewHolder> {
        private final List<MarketNewsItem> newsItems;
        
        public MarketNewsAdapter(List<MarketNewsItem> newsItems) {
            this.newsItems = newsItems;
        }
        
        @NonNull
        @Override
        public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_market_news, parent, false);
            return new NewsViewHolder(view);
        }

        @NonNull


        @Override
        public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
            MarketNewsItem item = newsItems.get(position);
            holder.titleTextView.setText(item.getTitle());
            holder.descriptionTextView.setText(item.getDescription());
            holder.timestampTextView.setText(item.getTimestamp());
            
            // Add ripple effect on click
            holder.itemView.setOnClickListener(v -> {
                // Open detailed news view or external link
                // For now, just show a toast
                Toast.makeText(AnalyticsDashboardActivity.this,
                        "Opening news: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            });
        }
        
        @Override
        public int getItemCount() {
            return newsItems.size();
        }
        
        class NewsViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            TextView descriptionTextView;
            TextView timestampTextView;
            
            public NewsViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.news_title);
                descriptionTextView = itemView.findViewById(R.id.news_description);
                timestampTextView = itemView.findViewById(R.id.news_timestamp);
            }
        }
    }
}