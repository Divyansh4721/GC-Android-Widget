package com.gcjewellers.rateswidget;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always proceed to MainActivity
        startActivity(new Intent(this, MainActivity.class));
        
        // Close the SplashActivity so it doesn't remain in the back stack
        finish();
    }
}