package com.gcjewellers.rateswidget;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;

public class CalculatorActivity extends Activity {
    private TextView currentGoldRateText;
    private TextView currentSilverRateText;
    private TextInputEditText goldWeightInput;
    private TextInputEditText silverWeightInput;
    private TextView goldTotalPriceText;
    private TextView silverTotalPriceText;

    private double currentGoldRate = 0;
    private double currentSilverRate = 0;
    private DecimalFormat priceFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        // Initialize views
        initializeViews();

        // Setup decimal format
        priceFormat = new DecimalFormat("₹#,##0.00");

        // Fetch and display current rates
        fetchCurrentRates();

        // Setup text change listeners for weight inputs
        setupWeightInputListeners();
    }

    private void initializeViews() {
        currentGoldRateText = findViewById(R.id.current_gold_rate);
        currentSilverRateText = findViewById(R.id.current_silver_rate);
        goldWeightInput = findViewById(R.id.gold_weight_input);
        silverWeightInput = findViewById(R.id.silver_weight_input);
        goldTotalPriceText = findViewById(R.id.gold_total_price);
        silverTotalPriceText = findViewById(R.id.silver_total_price);
    }

    private void fetchCurrentRates() {
        RatesFetcher.fetchRates(this, new RatesFetcher.RatesFetchListener() {
            @Override
            public void onRatesFetched(String goldRate, String silverRate, String lastUpdated) {
                try {
                    // Parse gold rate (price per 10g)
                    currentGoldRate = Double.parseDouble(goldRate);
                    currentGoldRateText.setText("₹" + goldRate + "/10g");

                    // Parse silver rate (price per 1 kg = 1000g)
                    currentSilverRate = Double.parseDouble(silverRate);
                    currentSilverRateText.setText("₹" + silverRate + "/kg");

                    // Recalculate prices if inputs are already filled
                    calculateGoldPrice();
                    calculateSilverPrice();
                } catch (NumberFormatException e) {
                    currentGoldRateText.setText("Error");
                    currentSilverRateText.setText("Error");
                }
            }

            @Override
            public void onError(String errorMessage) {
                currentGoldRateText.setText("N/A");
                currentSilverRateText.setText("N/A");
            }
        });
    }

    private void setupWeightInputListeners() {
        goldWeightInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateGoldPrice();
            }
        });

        silverWeightInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateSilverPrice();
            }
        });
    }

    private void calculateGoldPrice() {
        try {
            // Get weight input
            String weightStr = goldWeightInput.getText().toString().trim();
            if (weightStr.isEmpty()) {
                goldTotalPriceText.setText("₹0");
                return;
            }

            // Parse weight
            double weight = Double.parseDouble(weightStr);

            // Calculate price (rate is per 10g, so convert weight to per 10g units)
            double pricePerWeight = currentGoldRate * (weight / 10.0);

            // Update price text
            goldTotalPriceText.setText(priceFormat.format(pricePerWeight));
        } catch (NumberFormatException e) {
            goldTotalPriceText.setText("₹0");
        }
    }

    private void calculateSilverPrice() {
        try {
            // Get weight input
            String weightStr = silverWeightInput.getText().toString().trim();
            if (weightStr.isEmpty()) {
                silverTotalPriceText.setText("₹0");
                return;
            }

            // Parse weight
            double weight = Double.parseDouble(weightStr);

            // Calculate price (rate is per kg, so convert weight to kg)
            double pricePerWeight = currentSilverRate * (weight / 1000.0);

            // Update price text
            silverTotalPriceText.setText(priceFormat.format(pricePerWeight));
        } catch (NumberFormatException e) {
            silverTotalPriceText.setText("₹0");
        }
    }
}