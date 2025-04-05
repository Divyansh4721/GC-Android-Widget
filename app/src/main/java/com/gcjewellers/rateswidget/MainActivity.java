package com.gcjewellers.rateswidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
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
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private SwitchMaterial batteryOptSwitch;
    private SwitchMaterial widgetRefreshSwitch;
    private MaterialButton logoutButton;
    private TextView userName;
    private TextView userEmail;
    private ImageView userProfileImage;
    private TextView goldRateText;
    private TextView silverPriceText;
    private TextView dateTimeText;
    private TextView ratesUpdatedTimeText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);

            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            // Verify user is logged in
            if (currentUser == null) {
                Log.d(TAG, "No user logged in, redirecting to SignInActivity");
                Intent intent = new Intent(this, SignInActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            // Initialize views
            initializeViews();

            // Setup current date and time
            setupDateAndTime();

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

            // Schedule widget updates to ensure widgets stay current
            WidgetUpdateService.scheduleNextUpdate(this);

        } catch (Exception e) {
            Log.e(TAG, "Critical error in MainActivity onCreate", e);
            Toast.makeText(this, "An error occurred. Please restart the app.", Toast.LENGTH_LONG).show();
            
            // Force logout and redirect to sign-in
            if (mAuth != null) {
                mAuth.signOut();
            }
            Intent intent = new Intent(this, SignInActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void initializeViews() {
        batteryOptSwitch = findViewById(R.id.battery_optimization_switch);
        widgetRefreshSwitch = findViewById(R.id.widget_refresh_switch);
        logoutButton = findViewById(R.id.logout_button);
        
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        userProfileImage = findViewById(R.id.user_profile_image);
        
        // Date and rates views
        dateTimeText = findViewById(R.id.date_time_text);
        ratesUpdatedTimeText = findViewById(R.id.rates_updated_time);
        
        // Rates views
        goldRateText = findViewById(R.id.gold_rate);
        silverPriceText = findViewById(R.id.silver_price);
    }

    private void setupDateAndTime() {
        // Get current date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy | hh:mm a", Locale.getDefault());
        String currentDateTime = dateFormat.format(new Date());
        
        // Set date and time in the TextView
        dateTimeText.setText(currentDateTime);
    }

    private void setupUserProfile(FirebaseUser user) {
        if (user != null) {
            // Set user name
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "User";
            userName.setText(displayName);

            // Set user email
            String email = user.getEmail() != null ? user.getEmail() : "No email";
            userEmail.setText(email);

            // Handle profile image
            if (user.getPhotoUrl() != null) {
                // Attempt to load photo from Google account
                try {
                    Picasso.get()
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(userProfileImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Profile image loaded successfully");
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error loading profile image", e);
                                // Fallback to generated profile image
                                generateProfileImage(displayName);
                            }
                        });
                } catch (Exception e) {
                    Log.e(TAG, "Exception loading profile image", e);
                    // Fallback to generated profile image
                    generateProfileImage(displayName);
                }
            } else {
                // No photo URL, generate profile image
                generateProfileImage(displayName);
            }
        }
    }

    private void generateProfileImage(String displayName) {
        try {
            // Ensure non-zero dimensions
            int width = userProfileImage.getWidth();
            int height = userProfileImage.getHeight();
            
            if (width <= 0) width = 100;
            if (height <= 0) height = 100;

            Bitmap profileBitmap = ProfileImageGenerator.generateCircularProfileImage(
                    displayName,
                    width,
                    height
            );
            userProfileImage.setImageBitmap(profileBitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error generating profile image", e);
            // Fallback to default drawable
            userProfileImage.setImageResource(R.drawable.ic_default_profile);
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
        // Check current auto-refresh state from RatesWidgetProvider preferences
        boolean isAutoRefreshEnabled = RatesWidgetProvider.isAutoRefreshEnabled(this);
        widgetRefreshSwitch.setChecked(isAutoRefreshEnabled);

        widgetRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Send broadcast to widget provider to start/stop updates
            Intent intent = new Intent(this, RatesWidgetProvider.class);
            intent.setAction(isChecked
                    ? RatesWidgetProvider.ACTION_START_UPDATES
                    : RatesWidgetProvider.ACTION_STOP_UPDATES);
            
            Log.d(TAG, "Sending " + intent.getAction() + " broadcast");
            sendBroadcast(intent);
            
            // Show confirmation toast
            Toast.makeText(this, 
                    isChecked ? "Widget auto-refresh enabled" : "Widget auto-refresh disabled", 
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> logout());
    }

    private void fetchCurrentRates() {
        // Show loading state
        goldRateText.setText("Loading...");
        silverPriceText.setText("Loading...");
        
        // Use the RatesFetcher utility
        RatesFetcher.fetchRates(this, new RatesFetcher.RatesFetchListener() {
            @Override
            public void onRatesFetched(String goldRate, String silverRate, String lastUpdated) {
                // Update the UI with fetched rates
                goldRateText.setText("₹" + goldRate);
                silverPriceText.setText("₹" + silverRate);
                
                // Update the rates updated time
                ratesUpdatedTimeText.setText("Updated at " + lastUpdated);
            }
            
            @Override
            public void onError(String errorMessage) {
                // Handle error
                goldRateText.setText("Error");
                silverPriceText.setText("Check connection");
                ratesUpdatedTimeText.setText("Update Failed");
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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
        
        // Refresh date and time
        setupDateAndTime();
        
        // Refresh rates when returning to the activity
        fetchCurrentRates();
    }
}