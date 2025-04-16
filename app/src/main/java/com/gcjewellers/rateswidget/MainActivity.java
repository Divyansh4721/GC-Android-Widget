package com.gcjewellers.rateswidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.GravityCompat;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity implements ProfileImageGenerator.ProfileImageCallback {
    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private SwitchMaterial batteryOptSwitch;
    private SwitchMaterial widgetRefreshSwitch;
    private MaterialButton logoutButton;
    private TextView userName;
    private TextView userEmail;
    private ImageView userProfileImage;
    private TextView goldRateText;
    private TextView silverPriceText;
    private TextView ratesUpdatedTimeText;
    private TextView goldPriceChange;
    private TextView silverPriceChange;
    private MaterialCardView ratesCard;
    private View loadingIndicator;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    private RatesViewModel ratesViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ratesViewModel = new ViewModelProvider(this).get(RatesViewModel.class);

        try {
            setupToolbar();
            setupAuthentication();
            initializeViews();
            setupUserInterface();
            observeRatesData();

            refreshRates();
        } catch (Exception e) {
            Log.e(TAG, "Critical error in MainActivity onCreate", e);
            handleFatalError("An error occurred. Please restart the app.");
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            // Enable home button
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            // Set the double chevron icon for the navigation drawer
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_double_chevron);
        }

        // Set custom title color
        int titleColor = ContextCompat.getColor(this, R.color.toolbar_title_color);
        toolbar.setTitleTextColor(titleColor);

        // Still setup the drawer toggle for functionality but we'll override its
        // appearance
        setupNavigationDrawer(toolbar);
        applyThemeAdjustments(toolbar);
    }

    private void setupAuthentication() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        if (currentUser == null) {
            Log.d(TAG, "No user logged in, redirecting to SignInActivity");
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            throw new RuntimeException("No authenticated user");
        }
    }

    private void initializeViews() {
        batteryOptSwitch = findViewById(R.id.battery_optimization_switch);
        widgetRefreshSwitch = findViewById(R.id.widget_refresh_switch);
        logoutButton = findViewById(R.id.logout_button);
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        userProfileImage = findViewById(R.id.user_profile_image);
        ratesUpdatedTimeText = findViewById(R.id.rates_updated_time);
        goldRateText = findViewById(R.id.gold_rate);
        silverPriceText = findViewById(R.id.silver_price);
        goldPriceChange = findViewById(R.id.gold_price_change);
        silverPriceChange = findViewById(R.id.silver_price_change);
        ratesCard = findViewById(R.id.rates_card);
        loadingIndicator = findViewById(R.id.loading_indicator);
    }

    private void setupUserInterface() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            setupUserProfile(currentUser);
        }

        setupBatteryOptimizationSwitch();
        setupWidgetRefreshSwitch();
        setupLogoutButton();
        setupNavigationButtons();
        setupGestureListeners();
    }

    private void observeRatesData() {
        ratesViewModel.getRatesData().observe(this, ratesData -> {
            if (ratesData != null) {
                // Always update UI with latest data
                updateRatesUI(
                        ratesData.getGoldRate(),
                        ratesData.getSilverRate(),
                        ratesData.getLastUpdated(),
                        ratesData.getGoldChangePercent(),
                        ratesData.getSilverChangePercent());

                // Don't animate for real-time updates since we're not using them for the UI
                // Only animate for manual refreshes
                if (!ratesData.isFromRealTimeUpdate()) {
                    ratesCard.startAnimation(AnimationUtils.loadAnimation(
                            MainActivity.this, R.anim.rates_update_animation));
                }

                if (loadingIndicator.getVisibility() == View.VISIBLE) {
                    loadingIndicator.setVisibility(View.GONE);
                }

                // Update widget with latest data, keeping its auto-refresh settings intact
                updateWidget(ratesData);
            }
        });

        ratesViewModel.getError().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                goldRateText.setText("Error");
                silverPriceText.setText("Check connection");
                ratesUpdatedTimeText.setText("Update Failed");

                if (loadingIndicator.getVisibility() == View.VISIBLE) {
                    loadingIndicator.setVisibility(View.GONE);
                }

                Snackbar.make(findViewById(R.id.drawer_layout),
                        errorMessage, Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> refreshRates())
                        .show();
            }
        });
    }

    private void updateWidget(RatesViewModel.RatesData ratesData) {
        // Update widget with latest data (keep auto-refresh settings intact)
        Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
        // Use standard AppWidget update action
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Add required AppWidgetManager extras
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, RatesWidgetProvider.class));
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        // Add our rates data
        updateIntent.putExtra("goldRate", ratesData.getGoldRate());
        updateIntent.putExtra("silverRate", ratesData.getSilverRate());
        updateIntent.putExtra("lastUpdated", ratesData.getLastUpdated());
        updateIntent.putExtra("goldChangePercent", ratesData.getGoldChangePercent());
        updateIntent.putExtra("silverChangePercent", ratesData.getSilverChangePercent());

        sendBroadcast(updateIntent);
    }

    private void applyThemeAdjustments(Toolbar toolbar) {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Always use custom color for toolbar title
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.toolbar_title_color));

        // Always make the navigation icon black regardless of theme
        Drawable homeIcon = toolbar.getNavigationIcon();
        if (homeIcon != null) {
            homeIcon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private void setupNavigationDrawer(Toolbar toolbar) {
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        // Set hamburger icon color to ensure visibility in dark theme
        drawerToggle.getDrawerArrowDrawable().setColor(
                ContextCompat.getColor(this, R.color.drawer_icon_color));

        // Use more moderate size adjustments
        drawerToggle.getDrawerArrowDrawable().setBarLength(30f); // Reduced from 100f
        drawerToggle.getDrawerArrowDrawable().setBarThickness(4f); // Reduced from 8f

        drawerLayout.addDrawerListener(drawerToggle);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                drawerView.startAnimation(AnimationUtils.loadAnimation(
                        MainActivity.this, R.anim.drawer_open_animation));
            }
        });
        drawerToggle.syncState();

        // Apply theme adjustments after toggle is created
        applyThemeAdjustments(toolbar);
    }

    private void setupGestureListeners() {
        ratesCard.setOnTouchListener(new SwipeGestureDetector(this, direction -> {
            if (direction == SwipeGestureDetector.SWIPE_DOWN) {
                refreshRates();
                return true;
            }
            return false;
        }));
    }

    private void refreshRates() {
        loadingIndicator.setVisibility(View.VISIBLE);
        ratesViewModel.refreshRates();
    }

    private void setupUserProfile(FirebaseUser user) {
        if (user == null)
            return;

        String displayName = (user.getDisplayName() != null) ? user.getDisplayName() : "User";
        userName.setText(displayName);

        String email = (user.getEmail() != null) ? user.getEmail() : "No email";
        userEmail.setText(email);

        if (user.getPhotoUrl() != null) {
            loadUserProfileImage(user.getPhotoUrl(), displayName);
        } else {
            ProfileImageGenerator.generateProfileImageAsync(displayName, this);
        }
    }

    @Override
    public void onImageGenerated(Bitmap bitmap) {
        userProfileImage.setImageBitmap(bitmap);
    }

    @Override
    public void onError(Exception e) {
        userProfileImage.setImageResource(R.drawable.ic_default_profile);
    }

    private void loadUserProfileImage(Uri photoUrl, String displayName) {
        try {
            Picasso.get()
                    .load(photoUrl)
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
                            ProfileImageGenerator.generateProfileImageAsync(displayName, MainActivity.this);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception loading profile image", e);
            ProfileImageGenerator.generateProfileImageAsync(displayName, MainActivity.this);
        }
    }

    private void setupBatteryOptimizationSwitch() {
        if (batteryOptSwitch == null) {
            Log.e(TAG, "Battery Optimization Switch not found. Skipping setup.");
            return;
        }

        boolean isIgnoringBattery = isIgnoringBatteryOptimization();
        batteryOptSwitch.setChecked(isIgnoringBattery);
        batteryOptSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestBatteryOptimizationExemption();
            } else {
                enableBatteryOptimization();
            }
        });
    }

    private void setupWidgetRefreshSwitch() {
        if (widgetRefreshSwitch == null) {
            Log.e(TAG, "Widget Refresh Switch not found. Skipping setup.");
            return;
        }

        RatesWidgetProvider ratesWidgetProvider = new RatesWidgetProvider();
        boolean isAutoRefreshEnabled = ratesWidgetProvider.isAutoRefreshEnabled(this);
        widgetRefreshSwitch.setChecked(isAutoRefreshEnabled);

        widgetRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent intent = new Intent(this, RatesWidgetProvider.class);
            intent.setAction(isChecked
                    ? RatesWidgetProvider.ACTION_START_UPDATES
                    : RatesWidgetProvider.ACTION_STOP_UPDATES);
            sendBroadcast(intent);

            // Show feedback with more engaging UI
            Snackbar.make(findViewById(R.id.drawer_layout),
                    isChecked ? "Auto-refresh enabled for widget" : "Auto-refresh disabled for widget",
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    private void setupNavigationButtons() {
        findViewById(R.id.switch_rate_alerts).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RateCheckActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        findViewById(R.id.btn_graph).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RatesGraphsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        findViewById(R.id.btn_graph_2).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RatesGraphsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        findViewById(R.id.btn_dashboard).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AnalyticsDashboardActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> logout());
    }

    private void updateRatesUI(String goldRate, String silverRate, String lastUpdated,
            double goldChangePercent, double silverChangePercent) {
        goldRateText.setText("₹" + goldRate);
        silverPriceText.setText("₹" + silverRate);
        ratesUpdatedTimeText.setText("Updated at " + lastUpdated);

        String goldChangeText = String.format(java.util.Locale.getDefault(), "%.2f%%", goldChangePercent);
        String silverChangeText = String.format(java.util.Locale.getDefault(), "%.2f%%", silverChangePercent);

        goldPriceChange.setText(goldChangeText);
        silverPriceChange.setText(silverChangeText);

        goldPriceChange.setTextColor(getChangeColor(goldChangePercent));
        silverPriceChange.setTextColor(getChangeColor(silverChangePercent));

        goldPriceChange.setCompoundDrawablesWithIntrinsicBounds(
                goldChangePercent >= 0 ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down,
                0, 0, 0);
        silverPriceChange.setCompoundDrawablesWithIntrinsicBounds(
                silverChangePercent >= 0 ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down,
                0, 0, 0);
    }

    private int getChangeColor(double changePercent) {
        if (changePercent > 0) {
            return ContextCompat.getColor(this, R.color.price_up);
        } else if (changePercent < 0) {
            return ContextCompat.getColor(this, R.color.price_down);
        } else {
            return ContextCompat.getColor(this, R.color.price_unchanged);
        }
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
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void enableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
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
        // getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // Handle the custom double chevron icon click
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }

        if (item.getItemId() == R.id.action_rates_graphs) {
            startActivity(new Intent(this, RatesGraphsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Just refresh rates once when app is resumed, without auto-refresh for the app
        // UI
        refreshRates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // We don't need to do anything here since we're not starting any updates for
        // the UI
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // We don't need to do anything here since we're not starting any updates for
        // the UI
    }

    private void handleFatalError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (mAuth != null) {
            mAuth.signOut();
        }
        startActivity(new Intent(this, SignInActivity.class));
        finish();
    }
}