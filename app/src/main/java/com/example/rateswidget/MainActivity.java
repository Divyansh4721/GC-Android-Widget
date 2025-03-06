package com.example.rateswidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class MainActivity extends Activity {
    
    private Button batteryOptButton;
    private Button logoutButton;
    private TextView statusText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        TextView devMessage = findViewById(R.id.developer_message);
        statusText = findViewById(R.id.status_text);

        if (currentUser != null) {
            String userName = currentUser.getDisplayName();
            String greeting = getTimeBasedGreeting();
            devMessage.setText(greeting + " " + (userName != null ? userName : "User"));
        } else {
            devMessage.setText(getTimeBasedGreeting() + " User");
        }
        
        // Find the battery optimization button
        batteryOptButton = findViewById(R.id.battery_opt_button);
        
        // Set up logout button
        logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
        
        // Check if we already have battery optimization exemption
        if (isIgnoringBatteryOptimization()) {
            // Already has permission, disable the button
            batteryOptButton.setEnabled(false);
            batteryOptButton.setText("Battery Optimization Disabled");
            statusText.setText("Widget Status: Active (Battery Optimized)");
            statusText.setTextColor(Color.parseColor("#8FFC8F"));
        } else {
            // Enable the button with click listener
            batteryOptButton.setEnabled(true);
            batteryOptButton.setText("Disable Battery Optimization");
            statusText.setText("Widget Status: Limited (Battery Restricted !)");
            statusText.setTextColor(Color.parseColor("#FF0105"));
            batteryOptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestBatteryOptimizationExemption();
                }
            });
        }
    }
    
    // Method to get time-based greeting
    private String getTimeBasedGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            return "Good morning,";
        } else if (hour >= 12 && hour < 17) {
            return "Good afternoon,";
        } else   {
            return "Good evening,";
        } 
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check permissions again when returning to the activity
        if (isIgnoringBatteryOptimization()) {
            batteryOptButton.setEnabled(false);
            batteryOptButton.setText("Battery Optimization Disabled");
            statusText.setText("Widget Status: Active (Battery Optimized)");
            statusText.setTextColor(Color.parseColor("#8FFC8F"));
        } else {
            batteryOptButton.setEnabled(true);
            batteryOptButton.setText("Disable Battery Optimization");
            statusText.setText("Widget Status: Limited (Battery Restricted)");
            statusText.setTextColor(Color.parseColor("#FFA500"));
        }
    }
    
    private boolean isIgnoringBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            return powerManager.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true; // For older Android versions, return true (no need for exemption)
    }
    
    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Please enable battery optimization exemption for reliable widget updates", 
                    Toast.LENGTH_LONG).show();
        }
    }
    
    private void logout() {
        // Sign out from Firebase
        mAuth.signOut();
        
        // Sign out from GoogleSignIn as well
        try {
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, 
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build());
            
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                // Return to the SignInActivity with flag to prevent auto-login
                Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                intent.putExtra("fromLogout", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        } catch (Exception e) {
            // Fallback if there's any issue with Google Sign-out
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.putExtra("fromLogout", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}