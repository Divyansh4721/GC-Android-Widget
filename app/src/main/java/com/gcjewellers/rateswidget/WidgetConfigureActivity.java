package com.gcjewellers.rateswidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class WidgetConfigureActivity extends Activity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    
    // Width and height seek bars
    private SeekBar widthSeekBar;
    private SeekBar heightSeekBar;
    
    // Text views to show current width and height
    private TextView widthValueText;
    private TextView heightValueText;
    
    // Buttons to save or cancel
    private Button saveButton;
    private Button cancelButton;
    
    // Constants for width and height ranges
    private static final int MIN_WIDGET_WIDTH = 180;  // Minimum width in dp
    private static final int MAX_WIDGET_WIDTH = 400;  // Maximum width in dp
    private static final int MIN_WIDGET_HEIGHT = 60;  // Minimum height in dp
    private static final int MAX_WIDGET_HEIGHT = 200; // Maximum height in dp

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_configure);
        
        // Set result to CANCELED in case user backs out
        setResult(RESULT_CANCELED);
        
        // Find the widget id from the intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, 
                AppWidgetManager.INVALID_APPWIDGET_ID
            );
        }
        
        // If no valid widget id, exit
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        
        // Initialize views
        widthSeekBar = findViewById(R.id.width_seek_bar);
        heightSeekBar = findViewById(R.id.height_seek_bar);
        widthValueText = findViewById(R.id.width_value_text);
        heightValueText = findViewById(R.id.height_value_text);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);
        
        // Set up seek bar ranges
        widthSeekBar.setMax(MAX_WIDGET_WIDTH - MIN_WIDGET_WIDTH);
        heightSeekBar.setMax(MAX_WIDGET_HEIGHT - MIN_WIDGET_HEIGHT);
        
        // Retrieve current widget size
        int[] currentSize = RatesWidgetProvider.getWidgetSize(this, appWidgetId);
        
        // Set initial seek bar progress
        widthSeekBar.setProgress(currentSize[0] - MIN_WIDGET_WIDTH);
        heightSeekBar.setProgress(currentSize[1] - MIN_WIDGET_HEIGHT);
        
        // Update text views
        updateWidthText(currentSize[0]);
        updateHeightText(currentSize[1]);
        
        // Set up seek bar listeners
        widthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int width = progress + MIN_WIDGET_WIDTH;
                updateWidthText(width);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        heightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int height = progress + MIN_WIDGET_HEIGHT;
                updateHeightText(height);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Save button listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Calculate selected width and height
                int width = widthSeekBar.getProgress() + MIN_WIDGET_WIDTH;
                int height = heightSeekBar.getProgress() + MIN_WIDGET_HEIGHT;
                
                // Save widget configuration
                RatesWidgetProvider.saveWidgetSize(WidgetConfigureActivity.this, appWidgetId, width, height);
                
                // Update the widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetConfigureActivity.this);
                RatesWidgetProvider.updateWidgetWithSize(WidgetConfigureActivity.this, appWidgetManager, appWidgetId);
                
                // Set result and finish
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
        
        // Cancel button listener
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Simply finish the activity
                finish();
            }
        });
    }
    
    // Helper method to update width text
    private void updateWidthText(int width) {
        widthValueText.setText(String.format("Width: %d dp", width));
    }
    
    // Helper method to update height text
    private void updateHeightText(int height) {
        heightValueText.setText(String.format("Height: %d dp", height));
    }
}