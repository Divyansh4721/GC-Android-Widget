package com.gcjewellers.rateswidget;

import android.content.Intent;
import android.content.res.Configuration;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.gcjewellers.rateswidget.RatesGraphsActivity;
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

public class MainActivity extends AppCompatActivity {
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
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Initialize the Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // Ensure the Toolbar displays the app name from strings.xml
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.app_name);
                getSupportActionBar().setDisplayShowTitleEnabled(true);
            }

            // Explicitly set the title text color based on the current UI mode
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                // In dark mode, set the title color to white for optimal contrast
                toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));
            } else {
                // In light mode, set the title color to black or as desired
                toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.black));
            }

            // Initialize Firebase Auth and check for a signed-in user
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            // Configure Google Sign-In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            googleSignInClient = GoogleSignIn.getClient(this, gso);

            // If no user is logged in, redirect to SignInActivity
            if (currentUser == null) {
                Log.d(TAG, "No user logged in, redirecting to SignInActivity");
                Intent intent = new Intent(this, SignInActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            // Initialize views
            initializeViews();
            setupDateAndTime();
            setupUserProfile(currentUser);
            setupBatteryOptimizationSwitch();
            setupWidgetRefreshSwitch();
            setupLogoutButton();
            fetchCurrentRates();

            // Schedule widget updates for currency rates
            WidgetUpdateService.scheduleNextUpdate(this);

        } catch (Exception e) {
            Log.e(TAG, "Critical error in MainActivity onCreate", e);
            Toast.makeText(this, "An error occurred. Please restart the app.", Toast.LENGTH_LONG).show();

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
        dateTimeText = findViewById(R.id.date_time_text);
        ratesUpdatedTimeText = findViewById(R.id.rates_updated_time);
        goldRateText = findViewById(R.id.gold_rate);
        silverPriceText = findViewById(R.id.silver_price);
    }

    private void setupDateAndTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy | hh:mm a", Locale.getDefault());
        String currentDateTime = dateFormat.format(new Date());
        dateTimeText.setText(currentDateTime);
    }

    private void setupUserProfile(FirebaseUser user) {
        if (user != null) {
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "User";
            userName.setText(displayName);
            String email = user.getEmail() != null ? user.getEmail() : "No email";
            userEmail.setText(email);

            if (user.getPhotoUrl() != null) {
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
                                generateProfileImage(displayName);
                            }
                        });
                } catch (Exception e) {
                    Log.e(TAG, "Exception loading profile image", e);
                    generateProfileImage(displayName);
                }
            } else {
                generateProfileImage(displayName);
            }
        }
    }

    private void generateProfileImage(String displayName) {
        try {
            int width = userProfileImage.getWidth();
            int height = userProfileImage.getHeight();
            if (width <= 0) width = 100;
            if (height <= 0) height = 100;
            Bitmap profileBitmap = ProfileImageGenerator.generateCircularProfileImage(displayName, width, height);
            userProfileImage.setImageBitmap(profileBitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error generating profile image", e);
            userProfileImage.setImageResource(R.drawable.ic_default_profile);
        }
    }

    private void setupBatteryOptimizationSwitch() {
        boolean isIgnoringBatteryOptimizations = isIgnoringBatteryOptimization();
        batteryOptSwitch.setChecked(isIgnoringBatteryOptimizations);
        batteryOptSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestBatteryOptimizationExemption();
            } else {
                enableBatteryOptimization();
            }
        });
    }

    private void setupWidgetRefreshSwitch() {
        boolean isAutoRefreshEnabled = RatesWidgetProvider.isAutoRefreshEnabled(this);
        widgetRefreshSwitch.setChecked(isAutoRefreshEnabled);
        widgetRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent intent = new Intent(this, RatesWidgetProvider.class);
            intent.setAction(isChecked
                    ? RatesWidgetProvider.ACTION_START_UPDATES
                    : RatesWidgetProvider.ACTION_STOP_UPDATES);
            Log.d(TAG, "Sending " + intent.getAction() + " broadcast");
            sendBroadcast(intent);
            Toast.makeText(this, isChecked ? "Widget auto-refresh enabled" : "Widget auto-refresh disabled", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> logout());
    }

    private void fetchCurrentRates() {
        goldRateText.setText("Loading...");
        silverPriceText.setText("Loading...");
        RatesFetcher.fetchRates(this, new RatesFetcher.RatesFetchListener() {
            @Override
            public void onRatesFetched(String goldRate, String silverRate, String lastUpdated) {
                goldRateText.setText("₹" + goldRate);
                silverPriceText.setText("₹" + silverRate);
                ratesUpdatedTimeText.setText("Updated at " + lastUpdated);
            }
            @Override
            public void onError(String errorMessage) {
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
        mAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.putExtra("fromLogout", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_rates_graphs) {
            Intent intent = new Intent(this, RatesGraphsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (batteryOptSwitch != null) {
            batteryOptSwitch.setChecked(isIgnoringBatteryOptimization());
        }
        setupDateAndTime();
        fetchCurrentRates();
    }
}
