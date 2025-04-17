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
import android.view.animation.Animation;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    // Gold UI elements
    private TextView goldDate;
    private TextView goldRate;
    private TextView goldYesterdayPrice;
    private TextView goldPriceChange;

    private TextView silverPrice;
    private TextView silverYesterdayPrice;
    private TextView silverPriceChange;

    private MaterialCardView ratesCard;
    private FloatingActionButton fabRefresh;
    private View loadingIndicator;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    private RatesRepository ratesRepository;

    private TextView goldFutureRates;
    private TextView silverFutureRates;
    private TextView goldFutureLowHigh;
    private TextView silverFutureLowHigh;

    private TextView goldDollarRate;
    private TextView silverDollarRate;
    private TextView inrRate;

    private TextView gold99Buy;
    private TextView gold99Sell;
    private TextView goldRefineBuy;
    private TextView goldRefineSell;
    private TextView bankGoldBuy;
    private TextView bankGoldSell;

    private boolean isDrawerAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            setupToolbar();
            setupAuthentication();
            initializeViews();
            setupUserInterface();

            // Initialize the rates repository
            ratesRepository = new RatesRepository(this);

            // Load rates data
            refreshRates();

            // Setup refresh fab
            // In onCreate method, add this after initializing repository
            if (fabRefresh != null) {
                fabRefresh.setOnClickListener(v -> {
                    refreshRates();
                    // Add animation to the FAB
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_refresh));
                });
            }

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

        // Setup the drawer toggle for functionality but override its appearance
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
        try {
            // Navigation drawer elements
            batteryOptSwitch = findViewById(R.id.battery_optimization_switch);
            widgetRefreshSwitch = findViewById(R.id.widget_refresh_switch);
            logoutButton = findViewById(R.id.logout_button);
            userName = findViewById(R.id.user_name);
            userEmail = findViewById(R.id.user_email);
            userProfileImage = findViewById(R.id.user_profile_image);

            // Gold UI elements
            goldDate = findViewById(R.id.gold_date);
            goldRate = findViewById(R.id.gold_rate);
            goldYesterdayPrice = findViewById(R.id.gold_yesterday_price);
            goldPriceChange = findViewById(R.id.gold_price_change);

            // Silver UI elements
            // Silver UI elements
            TextView silverDate = findViewById(R.id.silver_date);
            silverPrice = findViewById(R.id.silver_price);
            silverYesterdayPrice = findViewById(R.id.silver_yesterday_price);
            silverPriceChange = findViewById(R.id.silver_price_change);

            ratesCard = findViewById(R.id.rates_card);
            fabRefresh = findViewById(R.id.fab_refresh);
            loadingIndicator = findViewById(R.id.loading_indicator);

            // Future Rates
            goldFutureRates = findViewById(R.id.gold_future_rates);
            silverFutureRates = findViewById(R.id.silver_future_rates);
            goldFutureLowHigh = findViewById(R.id.gold_future_low_high);
            silverFutureLowHigh = findViewById(R.id.silver_future_low_high);

            // Dollar Rates
            goldDollarRate = findViewById(R.id.gold_dollar_rate);
            silverDollarRate = findViewById(R.id.silver_dollar_rate);
            inrRate = findViewById(R.id.inr_rate);

            // Buy/Sell Rates
            gold99Buy = findViewById(R.id.gold_99_buy);
            gold99Sell = findViewById(R.id.gold_99_sell);
            goldRefineBuy = findViewById(R.id.gold_refine_buy);
            goldRefineSell = findViewById(R.id.gold_refine_sell);
            bankGoldBuy = findViewById(R.id.bank_gold_buy);
            bankGoldSell = findViewById(R.id.bank_gold_sell);

            // Set current date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String currentDate = dateFormat.format(new Date());

            if (goldDate != null) {
                goldDate.setText(currentDate);
            }

            if (silverDate != null) {
                silverDate.setText(currentDate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in initializeViews: " + e.getMessage());
        }
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

        // Setup swipe to refresh
        ratesCard.setOnTouchListener(new SwipeGestureDetector(this, direction -> {
            if (direction == SwipeGestureDetector.SWIPE_DOWN) {
                refreshRates();
                return true;
            }
            return false;
        }));
    }

    // Update the refreshRates() method in MainActivity
    private void refreshRates() {
        // Show loading indicator if available
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }

        // Fetch extended rates
        if (ratesRepository != null) {
            ratesRepository.fetchExtendedRates(new RatesRepository.ExtendedRatesFetchCallback() {
                @Override
                public void onSuccess(
                        String goldRate, String silverRate, String lastUpdated,
                        String yesterdayGoldRate, String yesterdaySilverRate,
                        String goldChangeValue, String silverChangeValue,

                        // New extended rate parameters
                        String goldFutureRate, String silverFutureRate,
                        String goldFutureLow, String goldFutureHigh,
                        String silverFutureLow, String silverFutureHigh,
                        String goldDollarRate, String silverDollarRate, String inrRate,
                        String gold99Buy, String gold99Sell,
                        String goldRefineBuy, String goldRefineSell,
                        String bankGoldBuy, String bankGoldSell) {
                    runOnUiThread(() -> {
                        // Update existing rates UI
                        updateRatesUI(
                                goldRate, silverRate, yesterdayGoldRate, yesterdaySilverRate,
                                goldChangeValue, silverChangeValue);

                        // Update new rates table UI
                        updateExtendedRatesUI(
                                goldFutureRate, silverFutureRate,
                                goldFutureLow, goldFutureHigh,
                                silverFutureLow, silverFutureHigh,
                                goldDollarRate, silverDollarRate, inrRate,
                                gold99Buy, gold99Sell,
                                goldRefineBuy, goldRefineSell,
                                bankGoldBuy, bankGoldSell);

                        // Hide loading indicator
                        if (loadingIndicator != null) {
                            loadingIndicator.setVisibility(View.GONE);
                        }

                        // Animate rates card
                        if (ratesCard != null) {
                            ratesCard.startAnimation(AnimationUtils.loadAnimation(
                                    MainActivity.this, R.anim.rates_update_animation));
                        }

                        // Update widgets with extended data
                        updateWidgetsWithExtendedData(
                                goldRate, silverRate, lastUpdated,
                                yesterdayGoldRate, yesterdaySilverRate,
                                goldChangeValue, silverChangeValue,
                                goldFutureRate, silverFutureRate,
                                goldDollarRate, silverDollarRate, inrRate);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        // Hide loading indicator
                        if (loadingIndicator != null) {
                            loadingIndicator.setVisibility(View.GONE);
                        }

                        // Show error Snackbar
                        if (findViewById(R.id.drawer_layout) != null) {
                            Snackbar.make(
                                    findViewById(R.id.drawer_layout),
                                    "Error: " + errorMessage,
                                    Snackbar.LENGTH_LONG)
                                    .setAction("Retry", v -> refreshRates())
                                    .show();
                        }
                    });
                }
            });
        }
    }

    // New method to update extended rates UI
    private void updateExtendedRatesUI(
            String goldFutureRate, String silverFutureRate,
            String goldFutureLow, String goldFutureHigh,
            String silverFutureLow, String silverFutureHigh,
            String goldDollarRate, String silverDollarRate, String inrRate,
            String gold99Buy, String gold99Sell,
            String goldRefineBuy, String goldRefineSell,
            String bankGoldBuy, String bankGoldSell) {
        // Update Future Rates Section
        if (this.goldFutureRates != null) {
            this.goldFutureRates.setText(goldFutureRate);
        }
        if (this.silverFutureRates != null) {
            this.silverFutureRates.setText(silverFutureRate);
        }

        // Update Future Low/High Section
        if (this.goldFutureLowHigh != null) {
            this.goldFutureLowHigh.setText(String.format("L: %s | H: %s", goldFutureLow, goldFutureHigh));
        }
        if (this.silverFutureLowHigh != null) {
            this.silverFutureLowHigh.setText(String.format("L: %s | H: %s", silverFutureLow, silverFutureHigh));
        }

        // Update Dollar Rates Section
        if (this.goldDollarRate != null) {
            this.goldDollarRate.setText(goldDollarRate);
        }
        if (this.silverDollarRate != null) {
            this.silverDollarRate.setText(silverDollarRate);
        }
        if (this.inrRate != null) {
            this.inrRate.setText(inrRate);
        }

        // Update Buy/Sell Rates Section
        if (this.gold99Buy != null) {
            this.gold99Buy.setText(gold99Buy);
        }
        if (this.gold99Sell != null) {
            this.gold99Sell.setText(gold99Sell);
        }
        if (this.goldRefineBuy != null) {
            this.goldRefineBuy.setText(goldRefineBuy);
        }
        if (this.goldRefineSell != null) {
            this.goldRefineSell.setText(goldRefineSell);
        }
        if (this.bankGoldBuy != null) {
            this.bankGoldBuy.setText(bankGoldBuy);
        }
        if (this.bankGoldSell != null) {
            this.bankGoldSell.setText(bankGoldSell);
        }
    }

    // Update widget method to include extended data
    private void updateWidgetsWithExtendedData(
            String goldRate, String silverRate, String lastUpdated,
            String yesterdayGoldRate, String yesterdaySilverRate,
            String goldChangeValue, String silverChangeValue,
            String goldFutureRate, String silverFutureRate,
            String goldDollarRate, String silverDollarRate, String inrRate) {
        // Create intent to update widgets
        Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Get all widget IDs
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, RatesWidgetProvider.class));
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        // Add extended rates data
        updateIntent.putExtra("goldRate", goldRate);
        updateIntent.putExtra("silverRate", silverRate);
        updateIntent.putExtra("lastUpdated", lastUpdated);
        updateIntent.putExtra("yesterdayGoldRate", yesterdayGoldRate);
        updateIntent.putExtra("yesterdaySilverRate", yesterdaySilverRate);
        updateIntent.putExtra("goldChangeValue", goldChangeValue);
        updateIntent.putExtra("silverChangeValue", silverChangeValue);

        // Add new extended data
        updateIntent.putExtra("goldFutureRate", goldFutureRate);
        updateIntent.putExtra("silverFutureRate", silverFutureRate);
        updateIntent.putExtra("goldDollarRate", goldDollarRate);
        updateIntent.putExtra("silverDollarRate", silverDollarRate);
        updateIntent.putExtra("inrRate", inrRate);

        // Send the broadcast
        sendBroadcast(updateIntent);
    }

    private void updateWidgets(String goldRate, String silverRate, String lastUpdated,
            String yesterdayGoldRate, String yesterdaySilverRate,
            String goldChangeValue, String silverChangeValue) {
        // Create intent to update widgets
        Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Get all widget IDs
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, RatesWidgetProvider.class));
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        // Add rates data
        updateIntent.putExtra("goldRate", goldRate);
        updateIntent.putExtra("silverRate", silverRate);
        updateIntent.putExtra("lastUpdated", lastUpdated);
        updateIntent.putExtra("yesterdayGoldRate", yesterdayGoldRate);
        updateIntent.putExtra("yesterdaySilverRate", yesterdaySilverRate);
        updateIntent.putExtra("goldChangeValue", goldChangeValue);
        updateIntent.putExtra("silverChangeValue", silverChangeValue);

        // Send the broadcast
        sendBroadcast(updateIntent);
    }

    private void updateRatesUI(String goldRateValue, String silverRateValue,
            String yesterdayGoldRateValue, String yesterdaySilverRateValue,
            String goldChangeValue, String silverChangeValue) {
        // Set gold values
        if (goldRate != null) {
            goldRate.setText("₹" + goldRateValue);
        }
        if (goldYesterdayPrice != null) {
            goldYesterdayPrice.setText("₹" + yesterdayGoldRateValue);
        }

        // Set silver values
        if (silverPrice != null) {
            silverPrice.setText("₹" + silverRateValue);
        }
        if (silverYesterdayPrice != null) {
            silverYesterdayPrice.setText("₹" + yesterdaySilverRateValue);
        }

        // Set price changes with proper sign and color
        try {
            int goldChange = Integer.parseInt(goldChangeValue);
            int silverChange = Integer.parseInt(silverChangeValue);

            // Format gold change with sign
            String formattedGoldChange = (goldChange >= 0 ? "+" : "") + goldChange;
            if (goldPriceChange != null) {
                goldPriceChange.setText(formattedGoldChange);
                goldPriceChange.setTextColor(ContextCompat.getColor(this,
                        goldChange >= 0 ? R.color.price_up : R.color.price_down));
                goldPriceChange.setCompoundDrawablesWithIntrinsicBounds(
                        goldChange >= 0 ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down,
                        0, 0, 0);
            }

            // Format silver change with sign
            String formattedSilverChange = (silverChange >= 0 ? "+" : "") + silverChange;
            if (silverPriceChange != null) {
                silverPriceChange.setText(formattedSilverChange);
                silverPriceChange.setTextColor(ContextCompat.getColor(this,
                        silverChange >= 0 ? R.color.price_up : R.color.price_down));
                silverPriceChange.setCompoundDrawablesWithIntrinsicBounds(
                        silverChange >= 0 ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down,
                        0, 0, 0);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error formatting price changes", e);

            // Default values if parsing fails
            if (goldPriceChange != null) {
                goldPriceChange.setText("+0");
                goldPriceChange.setTextColor(ContextCompat.getColor(this, R.color.price_unchanged));
                goldPriceChange.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_arrow_up, 0, 0, 0);
            }

            if (silverPriceChange != null) {
                silverPriceChange.setText("+0");
                silverPriceChange.setTextColor(ContextCompat.getColor(this, R.color.price_unchanged));
                silverPriceChange.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_arrow_up, 0, 0, 0);
            }
        }
    }

    private void updateWidgets(String goldRate, String silverRate,
            String yesterdayGoldRate, String yesterdaySilverRate,
            String goldChangeValue, String silverChangeValue) {
        // Create intent to update widgets
        Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Get all widget IDs
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(this, RatesWidgetProvider.class));
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        // Add rates data
        updateIntent.putExtra("goldRate", goldRate);
        updateIntent.putExtra("silverRate", silverRate);
        updateIntent.putExtra("yesterdayGoldRate", yesterdayGoldRate);
        updateIntent.putExtra("yesterdaySilverRate", yesterdaySilverRate);
        updateIntent.putExtra("goldChangeValue", goldChangeValue);
        updateIntent.putExtra("silverChangeValue", silverChangeValue);

        // Send the broadcast
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
        if (drawerLayout == null)
            return;

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    private void performDrawerOpenAnimation(View drawerView) {
        if (isDrawerAnimating)
            return;

        isDrawerAnimating = true;
        View navView = findViewById(R.id.nav_view);

        if (navView != null) {
            navView.clearAnimation();
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.drawer_open_animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    isDrawerAnimating = false;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            navView.startAnimation(animation);
        } else {
            isDrawerAnimating = false;
            Log.e(TAG, "Navigation view not found for animation");
        }
    }

    private void setupGestureListeners() {
        // Already setup in setupUserInterface for swipe-to-refresh
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

            // Show feedback
            Snackbar.make(findViewById(R.id.drawer_layout),
                    isChecked ? "Auto-refresh enabled for widget" : "Auto-refresh disabled for widget",
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    private void setupNavigationButtons() {
        try {
            // Check if each view exists before setting click listeners
            View switchRateAlertsBtn = findViewById(R.id.switch_rate_alerts);
            if (switchRateAlertsBtn != null) {
                switchRateAlertsBtn.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, RateCheckActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                });
            }

            View btnGraph = findViewById(R.id.btn_graph);
            if (btnGraph != null) {
                btnGraph.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, RatesGraphsActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                });
            }

            View btnGraph2 = findViewById(R.id.btn_graph_2);
            if (btnGraph2 != null) {
                btnGraph2.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, RatesGraphsActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                });
            }

            View btnDashboard = findViewById(R.id.btn_dashboard);
            if (btnDashboard != null) {
                btnDashboard.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, AnalyticsDashboardActivity.class));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupNavigationButtons: " + e.getMessage());
            // Continue gracefully without crashing
        }
    }

    private void setupLogoutButton() {
        logoutButton.setOnClickListener(v -> logout());
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh rates when app is resumed
        refreshRates();
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