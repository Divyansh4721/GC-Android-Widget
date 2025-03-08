package com.example.rateswidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

public class MainActivity extends Activity {
    
    private SwitchMaterial batteryOptSwitch;
    private SwitchMaterial widgetRefreshSwitch;
    private MaterialButton logoutButton;
    private TextView userName;
    private TextView userEmail;
    private ImageView userProfileImage;
    private TextView goldPriceText;
    private TextView silverPriceText;
    
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Initialize views
        initializeViews();
        
        // Setup user profile
        setupUserProfile(currentUser);
        
        // Setup battery optimization switch
        setupBatteryOptimizationSwitch();
        
        // Setup widget refresh switch
        setupWidgetRefreshSwitch();
        
        // Setup logout button
        setupLogoutButton();
        
        // Fetch and display current rates
        fetchCurrentRates();
    }
    
    private void initializeViews() {
        batteryOptSwitch = findViewById(R.id.battery_optimization_switch);
        widgetRefreshSwitch = findViewById(R.id.widget_refresh_switch);
        logoutButton = findViewById(R.id.logout_button);
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        userProfileImage = findViewById(R.id.user_profile_image);
        goldPriceText = findViewById(R.id.gold_price);
        silverPriceText = findViewById(R.id.silver_price);
    }
    
    private void setupUserProfile(FirebaseUser user) {
        if (user != null) {
            // Set user name
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "User";
            userName.setText(displayName);
            
            // Set user email
            String email = user.getEmail() != null ? user.getEmail() : "No email";
            userEmail.setText(email);
            
            // Generate profile image from name or use photo URL
            if (user.getPhotoUrl() != null) {
                // Use Firebase photo URL if available
                Picasso.get()
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(userProfileImage);
            } else {
                // Generate profile image from name
                Bitmap profileBitmap = ProfileImageGenerator.generateCircularProfileImage(
                    displayName, 
                    userProfileImage.getWidth(), 
                    userProfileImage.getHeight()
                );
                userProfileImage.setImageBitmap(profileBitmap);
            }
        }
    }

    
    private void setupBatteryOptimizationSwitch() {
        // Check current battery optimization status
        boolean isIgnoringBatteryOptimizations = isIgnoringBatteryOptimization();
        batteryOptSwitch.setChecked(isIgnoringBatteryOptimizations);
        
        batteryOptSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Request to disable battery optimization
                requestBatteryOptimizationExemption();
            } else {
                // Enable battery optimization
                enableBatteryOptimization();
            }
        });
    }
    
    private void setupWidgetRefreshSwitch() {
        // Default to on
        widgetRefreshSwitch.setChecked(true);
        
        widgetRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Send broadcast to widget provider to start/stop updates
            Intent intent = new Intent(this, RatesWidgetProvider.class);
            intent.setAction(isChecked ? 
                "com.example.rateswidget.ACTION_START_UPDATES" : 
                "com.example.rateswidget.ACTION_STOP_UPDATES");
            sendBroadcast(intent);
        });
    }
    
    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> logout());
    }
    
    private void fetchCurrentRates() {
        // TODO: Implement actual rate fetching logic
        // For now, using placeholder values
        goldPriceText.setText("₹6,250/10g");
        silverPriceText.setText("₹75/10g");
    }
    
    private boolean isIgnoringBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            return powerManager.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }
    
    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
    
    private void enableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
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

    @Override
    protected void onResume() {
        super.onResume();
        // Recheck battery optimization status when returning to the activity
        if (batteryOptSwitch != null) {
            batteryOptSwitch.setChecked(isIgnoringBatteryOptimization());
        }
    }
}