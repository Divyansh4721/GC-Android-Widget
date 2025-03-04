package com.example.rateswidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        TextView devMessage = findViewById(R.id.developer_message);
        devMessage.setText("Hello Developer");
        
        // Request battery optimization exemption
        Button batteryOptButton = findViewById(R.id.battery_opt_button);
        batteryOptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestBatteryOptimizationExemption();
            }
        });
    }
    
    private void requestBatteryOptimizationExemption() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        Toast.makeText(this, "Please enable battery optimization exemption for reliable widget updates", 
                Toast.LENGTH_LONG).show();
    }
}