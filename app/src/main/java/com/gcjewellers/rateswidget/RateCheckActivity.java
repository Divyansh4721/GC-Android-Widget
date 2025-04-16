package com.gcjewellers.rateswidget;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class RateCheckActivity extends AppCompatActivity {

    // SharedPreferences keys
    private static final String PREFS_NAME = "RateAlertPrefs";
    private static final String CONDITIONS_KEY = "AlertConditions";

    // Default rates (in case real‑time fetch fails)
    private double currentGoldRate = 58000.0;
    private double currentSilverRate = 700.0;

    // UI references
    private RecyclerView rvConditions;
    private MaterialButton btnAddCondition;
    private MaterialButton btnSaveConditions;
    private ConditionAdapter conditionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_check);

        // Set up RecyclerView and adapter
        rvConditions = findViewById(R.id.rv_conditions);
        rvConditions.setLayoutManager(new LinearLayoutManager(this));
        conditionAdapter = new ConditionAdapter();
        rvConditions.setAdapter(conditionAdapter);

        // Bind buttons
        btnAddCondition = findViewById(R.id.btn_add_condition);
        btnSaveConditions = findViewById(R.id.btn_save_conditions);

        // Add a new blank condition on click
        btnAddCondition.setOnClickListener(view -> {
            conditionAdapter.addCondition(new JSONObject());
        });

        // Save conditions to SharedPreferences on click
        btnSaveConditions.setOnClickListener(view -> {
            JSONArray conditionsArray = new JSONArray();
            for (JSONObject condition : conditionAdapter.getConditions()) {
                conditionsArray.put(condition);
            }
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(CONDITIONS_KEY, conditionsArray.toString());
            editor.apply();
            Toast.makeText(RateCheckActivity.this, "Alert conditions saved.", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Load any saved conditions
        loadAlertSettings();

        // Fetch live rates from the API in the background
        fetchRealTimeRates();
    }

    /**
     * Loads previously saved alert conditions from SharedPreferences.
     */
    private void loadAlertSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String conditionsJson = prefs.getString(CONDITIONS_KEY, "[]");
        try {
            JSONArray conditionsArray = new JSONArray(conditionsJson);
            for (int i = 0; i < conditionsArray.length(); i++) {
                JSONObject condition = conditionsArray.getJSONObject(i);
                conditionAdapter.addCondition(condition);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Asynchronously fetches real‑time rates from a live endpoint and updates instance variables.
     * This method parses a JSON response and updates currentGoldRate and currentSilverRate.
     */
    private void fetchRealTimeRates() {
        new Thread(() -> {
            try {
                URL url = new URL("https://goldrate.divyanshbansal.com/api/live");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();
                    connection.disconnect();

                    JSONArray jsonArray = new JSONArray(responseBuilder.toString());
                    final int GOLD_ROW = 5;
                    final int SILVER_ROW = 4;
                    final int RATE_COLUMN = 1;
                    String goldRateStr = "0";
                    String silverRateStr = "0";

                    if (jsonArray.length() > Math.max(GOLD_ROW, SILVER_ROW)) {
                        JSONArray goldRow = jsonArray.getJSONArray(GOLD_ROW);
                        JSONArray silverRow = jsonArray.getJSONArray(SILVER_ROW);
                        if (goldRow.length() > RATE_COLUMN) {
                            goldRateStr = goldRow.getString(RATE_COLUMN);
                        }
                        if (silverRow.length() > RATE_COLUMN) {
                            silverRateStr = silverRow.getString(RATE_COLUMN);
                        }
                    }

                    double fetchedGoldRate = parseRate(goldRateStr);
                    double fetchedSilverRate = parseRate(silverRateStr);

                    // Update the rates on the UI thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        currentGoldRate = fetchedGoldRate;
                        currentSilverRate = fetchedSilverRate;
                        // Optionally, refresh the adapter to update thresholds
                        conditionAdapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Utility method to parse a string rate to a double.
     */
    private double parseRate(String rateStr) {
        try {
            rateStr = rateStr.replaceAll("[^\\d.]", "");
            return Double.parseDouble(rateStr);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Inner class serving as a RecyclerView adapter for alert condition rows.
     */
    class ConditionAdapter extends RecyclerView.Adapter<ConditionAdapter.ViewHolder> {

        private final List<JSONObject> conditionList = new ArrayList<>();

        /**
         * Adds a new condition entry.
         */
        public void addCondition(JSONObject condition) {
            conditionList.add(condition);
            notifyItemInserted(conditionList.size() - 1);
        }

        /**
         * Removes a condition entry at the specified position.
         */
        public void removeCondition(int position) {
            if (position >= 0 && position < conditionList.size()) {
                conditionList.remove(position);
                notifyItemRemoved(position);
            } else {
                Toast.makeText(RateCheckActivity.this, "Invalid condition removal", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Returns the list of conditions.
         */
        public List<JSONObject> getConditions() {
            return conditionList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_condition_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject conditionData = conditionList.get(position);
            // Retrieve existing values or assign defaults
            String metal = conditionData.optString("metal", "Gold");
            String condType = conditionData.optString("condition", "Match");
            String threshold = conditionData.optString("threshold", "");

            // Set spinner selections
            holder.spinnerMetal.setSelection(metal.equalsIgnoreCase("Silver") ? 1 : 0);
            ArrayAdapter<String> condAdapter = (ArrayAdapter<String>) holder.spinnerCondition.getAdapter();
            int pos = condAdapter.getPosition(condType);
            holder.spinnerCondition.setSelection(pos >= 0 ? pos : 0);

            // Set the threshold field (if already modified, user override remains)
            holder.edtThreshold.setText(threshold);

            // When the metal selection changes, update the threshold if it's empty.
            holder.spinnerMetal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long id) {
                    try {
                        conditionData.put("metal", (itemPosition == 0) ? "Gold" : "Silver");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // If threshold is empty, auto-populate with the current real‑time rate.
                    if (TextUtils.isEmpty(holder.edtThreshold.getText().toString())) {
                        if (itemPosition == 0) {
                            holder.edtThreshold.setText(String.valueOf(currentGoldRate));
                        } else {
                            holder.edtThreshold.setText(String.valueOf(currentSilverRate));
                        }
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // When the condition spinner changes, update JSON accordingly.
            holder.spinnerCondition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long id) {
                    String selectedCond = (String) holder.spinnerCondition.getItemAtPosition(itemPosition);
                    try {
                        conditionData.put("condition", selectedCond);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // When the threshold field loses focus, update the JSON.
            holder.edtThreshold.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String newThreshold = holder.edtThreshold.getText().toString().trim();
                    try {
                        conditionData.put("threshold", newThreshold);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // Wire up the Remove button.
            holder.btnRemove.setOnClickListener(v -> removeCondition(position));
        }

        @Override
        public int getItemCount() {
            return conditionList.size();
        }

        /**
         * ViewHolder for binding UI elements for each condition row.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            Spinner spinnerMetal;
            Spinner spinnerCondition;
            TextInputEditText edtThreshold;
            MaterialButton btnRemove;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_condition);
                spinnerMetal = itemView.findViewById(R.id.spinner_metal);
                spinnerCondition = itemView.findViewById(R.id.spinner_condition);
                edtThreshold = itemView.findViewById(R.id.edt_threshold);
                btnRemove = itemView.findViewById(R.id.btn_remove);

                // Initialize metal spinner (Gold, Silver)
                ArrayAdapter<String> metalAdapter = new ArrayAdapter<>(itemView.getContext(),
                        android.R.layout.simple_spinner_item, new String[]{"Gold", "Silver"});
                metalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerMetal.setAdapter(metalAdapter);

                // Initialize condition spinner (Match, Below, Above)
                ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(itemView.getContext(),
                        android.R.layout.simple_spinner_item, new String[]{"Match", "Below", "Above"});
                conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCondition.setAdapter(conditionAdapter);
            }
        }
    }
}
