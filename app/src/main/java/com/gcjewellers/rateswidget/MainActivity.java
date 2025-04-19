package com.gcjewellers.rateswidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private SwitchMaterial widgetRefreshSwitch;
    private MaterialButton logoutButton;
    private TextView userName;
    private TextView userEmail;
    private ImageView userProfileImage;

    private android.os.Handler autoRefreshHandler;
    private static final long AUTO_REFRESH_INTERVAL = 2000;
    private boolean isAutoRefreshEnabled = true;
    private Runnable autoRefreshRunnable;

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

    private RatesRepository ratesRepository;

    // Extended UI elements for all rates
    private TextView gold995Buy;
    private TextView gold995Sell;
    private TextView gold995High;
    private TextView gold995Low;

    private TextView silverFutureBuy;
    private TextView silverFutureSell;
    private TextView silverFutureHigh;
    private TextView silverFutureLow;

    private TextView goldFuturesBuy;
    private TextView goldFuturesSell;
    private TextView goldFuturesHigh;
    private TextView goldFuturesLow;

    private TextView goldRefineBuy;
    private TextView goldRefineSell;
    private TextView goldRefineHigh;
    private TextView goldRefineLow;

    private TextView goldRtgsBuy;
    private TextView goldRtgsSell;
    private TextView goldRtgsHigh;
    private TextView goldRtgsLow;

    private TextView goldDollarBuy;
    private TextView goldDollarSell;
    private TextView goldDollarHigh;
    private TextView goldDollarLow;

    private TextView silverDollarBuy;
    private TextView silverDollarSell;
    private TextView silverDollarHigh;
    private TextView silverDollarLow;

    private TextView dollarBuy;
    private TextView dollarSell;
    private TextView dollarHigh;
    private TextView dollarLow;

    private boolean isDrawerAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            setupToolbar();
            initializeViews();
            setupUserInterface();

            // Initialize the rates repository
            ratesRepository = new RatesRepository(this);

            // Initialize auto refresh handler
            autoRefreshHandler = new android.os.Handler(Looper.getMainLooper());
            autoRefreshRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing() && isAutoRefreshEnabled) {
                        refreshRates();
                        autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL);
                    }
                }
            };

            // Load rates data
            refreshRates();

            // Start auto refresh
            startAutoRefresh();

            // Setup refresh fab
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

    private void startAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            // Remove any existing callbacks to avoid duplicate refreshes
            stopAutoRefresh();

            // Start periodic refresh if enabled
            if (isAutoRefreshEnabled) {
                autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL);
            }
        }
    }

    private void stopAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    public void toggleAutoRefresh(boolean enabled) {
        isAutoRefreshEnabled = enabled;
        if (isAutoRefreshEnabled) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRates();

        isAutoRefreshEnabled = true;
        startAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoRefresh();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // Setup drawer with custom toggle
        drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            drawerToggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close) {

                @Override
                public void onDrawerSlide(View drawerView, float slideOffset) {
                    super.onDrawerSlide(drawerView, 0);
                }
            };

            drawerToggle.setDrawerIndicatorEnabled(false);
            drawerToggle.setHomeAsUpIndicator(R.drawable.gc_logo);

            drawerToggle.setToolbarNavigationClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });

            drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();
        }

        applyThemeAdjustments(toolbar);
    }

    private void initializeViews() {
        try {
            // Navigation drawer elements
            widgetRefreshSwitch = findViewById(R.id.widget_refresh_switch);
            ratesCard = findViewById(R.id.rates_card);
            fabRefresh = findViewById(R.id.fab_refresh);
            loadingIndicator = findViewById(R.id.loading_indicator);

            // Set default user information
            if (userName != null) {
                userName.setText("Guest User");
            }
            if (userEmail != null) {
                userEmail.setText("guest@example.com");
            }
            if (userProfileImage != null) {
                userProfileImage.setImageResource(R.drawable.ic_default_profile);
            }

            // Initialize all extended rate UI elements
            gold995Buy = findViewById(R.id.gold_995_buy);
            gold995Sell = findViewById(R.id.gold_995_sell);
            gold995High = findViewById(R.id.gold_995_high);
            gold995Low = findViewById(R.id.gold_995_low);

            silverFutureBuy = findViewById(R.id.silver_future_buy);
            silverFutureSell = findViewById(R.id.silver_future_sell);
            silverFutureHigh = findViewById(R.id.silver_future_high);
            silverFutureLow = findViewById(R.id.silver_future_low);

            goldFuturesBuy = findViewById(R.id.gold_futures_buy);
            goldFuturesSell = findViewById(R.id.gold_futures_sell);
            goldFuturesHigh = findViewById(R.id.gold_futures_high);
            goldFuturesLow = findViewById(R.id.gold_futures_low);

            goldRefineBuy = findViewById(R.id.gold_refine_buy);
            goldRefineSell = findViewById(R.id.gold_refine_sell);
            goldRefineHigh = findViewById(R.id.gold_refine_high);
            goldRefineLow = findViewById(R.id.gold_refine_low);

            goldRtgsBuy = findViewById(R.id.gold_rtgs_buy);
            goldRtgsSell = findViewById(R.id.gold_rtgs_sell);
            goldRtgsHigh = findViewById(R.id.gold_rtgs_high);
            goldRtgsLow = findViewById(R.id.gold_rtgs_low);

            goldDollarBuy = findViewById(R.id.gold_dollar_buy);
            goldDollarSell = findViewById(R.id.gold_dollar_sell);
            goldDollarHigh = findViewById(R.id.gold_dollar_high);
            goldDollarLow = findViewById(R.id.gold_dollar_low);

            silverDollarBuy = findViewById(R.id.silver_dollar_buy);
            silverDollarSell = findViewById(R.id.silver_dollar_sell);
            silverDollarHigh = findViewById(R.id.silver_dollar_high);
            silverDollarLow = findViewById(R.id.silver_dollar_low);

            dollarBuy = findViewById(R.id.dollar_buy);
            dollarSell = findViewById(R.id.dollar_sell);
            dollarHigh = findViewById(R.id.dollar_high);
            dollarLow = findViewById(R.id.dollar_low);

            // Set current date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String currentDate = dateFormat.format(new Date());

            if (goldDate != null) {
                goldDate.setText(currentDate);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in initializeViews: " + e.getMessage());
        }
    }

    private void setupUserInterface() {
        setupWidgetRefreshSwitch();
        setupNavigationButtons();
        setupSwipeToRefresh();
    }

    private void setupSwipeToRefresh() {
        ratesCard.setOnTouchListener(new SwipeGestureDetector(this, direction -> {
            if (direction == SwipeGestureDetector.SWIPE_DOWN) {
                refreshRates();
                return true;
            }
            return false;
        }));
    }

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
                        String gold995Buy, String gold995Sell, String gold995High, String gold995Low,
                        String silverFutureBuy, String silverFutureSell, String silverFutureHigh,
                        String silverFutureLow,
                        String goldFuturesBuy, String goldFuturesSell, String goldFuturesHigh, String goldFuturesLow,
                        String goldRefineBuy, String goldRefineSell, String goldRefineHigh, String goldRefineLow,
                        String goldRtgsBuy, String goldRtgsSell, String goldRtgsHigh, String goldRtgsLow,
                        String goldDollarBuy, String goldDollarSell, String goldDollarHigh, String goldDollarLow,
                        String silverDollarBuy, String silverDollarSell, String silverDollarHigh,
                        String silverDollarLow,
                        String dollarBuy, String dollarSell, String dollarHigh, String dollarLow) {

                    runOnUiThread(() -> {
                        // Update main rates UI
                        updateMainRatesUI(
                                gold995Sell, silverFutureSell,
                                calculateChangeValue(gold995Sell, gold995Low),
                                calculateChangeValue(silverFutureSell, silverFutureLow));

                        // Update all extended rates UI
                        updateExtendedRatesUI(
                                gold995Buy, gold995Sell, gold995High, gold995Low,
                                silverFutureBuy, silverFutureSell, silverFutureHigh, silverFutureLow,
                                goldFuturesBuy, goldFuturesSell, goldFuturesHigh, goldFuturesLow,
                                goldRefineBuy, goldRefineSell, goldRefineHigh, goldRefineLow,
                                goldRtgsBuy, goldRtgsSell, goldRtgsHigh, goldRtgsLow,
                                goldDollarBuy, goldDollarSell, goldDollarHigh, goldDollarLow,
                                silverDollarBuy, silverDollarSell, silverDollarHigh, silverDollarLow,
                                dollarBuy, dollarSell, dollarHigh, dollarLow);

                        // Hide loading indicator
                        if (loadingIndicator != null) {
                            loadingIndicator.setVisibility(View.GONE);
                        }

                        // Update widgets with the data
                        // Update widgets with the data

                        updateWidgetsWithExtendedData(
                                gold995Sell,
                                silverFutureSell,
                                new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()),
                                gold995Low,
                                silverFutureLow,
                                calculateChangeValue(gold995Sell, gold995Low),
                                calculateChangeValue(silverFutureSell, silverFutureLow),
                                goldFuturesSell,
                                silverFutureSell,
                                goldDollarSell,
                                silverDollarSell,
                                dollarSell);
                    }

                    );
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

    // Helper method to calculate change value
    private String calculateChangeValue(String currentRate, String previousRate) {
        try {
            float current = Float.parseFloat(currentRate.replace(",", ""));
            float previous = Float.parseFloat(previousRate.replace(",", ""));
            return String.valueOf(Math.round(current - previous));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error calculating change value", e);
            return "0";
        }
    }

    // Method to update the main rates UI (gold and silver primary displays)
    private void updateMainRatesUI(String goldRateValue, String silverRateValue,
            String goldChangeValue, String silverChangeValue) {
        // Set gold values
        if (goldRate != null) {
            goldRate.setText("₹" + goldRateValue);
        }
        if (goldYesterdayPrice != null) {
            goldYesterdayPrice.setText("₹" + goldRateValue);
        }

        // Set silver values
        if (silverPrice != null) {
            silverPrice.setText("₹" + silverRateValue);
        }
        if (silverYesterdayPrice != null) {
            silverYesterdayPrice.setText("₹" + silverRateValue);
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

    // New method to update all extended rates UI
    private void setRateStyle(TextView textView, String currentRate, String previousRate) {
        if (textView == null)
            return;

        try {
            // Parse rates, removing any commas
            double current = Double.parseDouble(currentRate.replace(",", ""));
            double previous = Double.parseDouble(previousRate.replace(",", ""));

            // Calculate change
            double change = current - previous;

            // Set base text
            textView.setText(currentRate);

            // Apply styling based on change
            if (change > 0) {
                // Rate increased - red color, up arrow
                textView.setTextColor(ContextCompat.getColor(this, R.color.price_up_red));
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_up, 0);
            } else if (change < 0) {
                // Rate decreased - green color, down arrow
                textView.setTextColor(ContextCompat.getColor(this, R.color.price_down_green));
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down, 0);
            } else {
                // No change - default color, no arrow
                textView.setTextColor(ContextCompat.getColor(this, R.color.price_unchanged));
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing rate: " + currentRate, e);
            // Default styling if parsing fails
            textView.setTextColor(ContextCompat.getColor(this, R.color.price_unchanged));
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    private void updateExtendedRatesUI(
            String gold995Buy, String gold995Sell, String gold995High, String gold995Low,
            String silverFutureBuy, String silverFutureSell, String silverFutureHigh, String silverFutureLow,
            String goldFuturesBuy, String goldFuturesSell, String goldFuturesHigh, String goldFuturesLow,
            String goldRefineBuy, String goldRefineSell, String goldRefineHigh, String goldRefineLow,
            String goldRtgsBuy, String goldRtgsSell, String goldRtgsHigh, String goldRtgsLow,
            String goldDollarBuy, String goldDollarSell, String goldDollarHigh, String goldDollarLow,
            String silverDollarBuy, String silverDollarSell, String silverDollarHigh, String silverDollarLow,
            String dollarBuy, String dollarSell, String dollarHigh, String dollarLow) {

        // Method to set text color and drawable based on rate change

        // Retrieve previous rates from SharedPreferences or your preferred storage
        SharedPreferences prefs = getSharedPreferences("RateTracker", MODE_PRIVATE);

        // Gold 995 Rates
        setRateStyle(this.gold995Buy, gold995Buy,
                prefs.getString("previous_gold_995_buy", gold995Buy));
        setRateStyle(this.gold995Sell, gold995Sell,
                prefs.getString("previous_gold_995_sell", gold995Sell));

        // Silver Future Rates
        setRateStyle(this.silverFutureBuy, silverFutureBuy,
                prefs.getString("previous_silver_future_buy", silverFutureBuy));
        setRateStyle(this.silverFutureSell, silverFutureSell,
                prefs.getString("previous_silver_future_sell", silverFutureSell));

        // Gold Futures Rates
        setRateStyle(this.goldFuturesBuy, goldFuturesBuy,
                prefs.getString("previous_gold_futures_buy", goldFuturesBuy));
        setRateStyle(this.goldFuturesSell, goldFuturesSell,
                prefs.getString("previous_gold_futures_sell", goldFuturesSell));

        // Gold Refine Section
        setRateStyle(this.goldRefineBuy, goldRefineBuy,
                prefs.getString("previous_gold_refine_buy", goldRefineBuy));
        setRateStyle(this.goldRefineSell, goldRefineSell,
                prefs.getString("previous_gold_refine_sell", goldRefineSell));

        // Gold RTGS Section
        setRateStyle(this.goldRtgsBuy, goldRtgsBuy,
                prefs.getString("previous_gold_rtgs_buy", goldRtgsBuy));
        setRateStyle(this.goldRtgsSell, goldRtgsSell,
                prefs.getString("previous_gold_rtgs_sell", goldRtgsSell));

        // Gold Dollar Section
        setRateStyle(this.goldDollarBuy, goldDollarBuy,
                prefs.getString("previous_gold_dollar_buy", goldDollarBuy));
        setRateStyle(this.goldDollarSell, goldDollarSell,
                prefs.getString("previous_gold_dollar_sell", goldDollarSell));

        // Silver Dollar Section
        setRateStyle(this.silverDollarBuy, silverDollarBuy,
                prefs.getString("previous_silver_dollar_buy", silverDollarBuy));
        setRateStyle(this.silverDollarSell, silverDollarSell,
                prefs.getString("previous_silver_dollar_sell", silverDollarSell));

        // Dollar Section
        setRateStyle(this.dollarBuy, dollarBuy,
                prefs.getString("previous_dollar_buy", dollarBuy));
        setRateStyle(this.dollarSell, dollarSell,
                prefs.getString("previous_dollar_sell", dollarSell));

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("previous_gold_995_buy", gold995Buy);
        editor.putString("previous_gold_995_sell", gold995Sell);
        editor.putString("previous_silver_future_buy", silverFutureBuy);
        editor.putString("previous_silver_future_sell", silverFutureSell);
        editor.putString("previous_gold_futures_buy", goldFuturesBuy);
        editor.putString("previous_gold_futures_sell", goldFuturesSell);
        editor.putString("previous_gold_refine_buy", goldRefineBuy);
        editor.putString("previous_gold_refine_sell", goldRefineSell);
        editor.putString("previous_gold_rtgs_buy", goldRtgsBuy);
        editor.putString("previous_gold_rtgs_sell", goldRtgsSell);
        editor.putString("previous_gold_dollar_buy", goldDollarBuy);
        editor.putString("previous_gold_dollar_sell", goldDollarSell);
        editor.putString("previous_silver_dollar_buy", silverDollarBuy);
        editor.putString("previous_silver_dollar_sell", silverDollarSell);
        editor.putString("previous_dollar_buy", dollarBuy);
        editor.putString("previous_dollar_sell", dollarSell);

        editor.apply();

    }

    // Helper method to set text if TextView is not null
    private void setTextIfNotNull(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text);
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

    private void setupWidgetRefreshSwitch() {
        if (widgetRefreshSwitch == null) {
            Log.e(TAG, "Widget Refresh Switch not found. Skipping setup.");
            return;
        }

        RatesWidgetProvider ratesWidgetProvider = new RatesWidgetProvider();
        boolean isAutoRefreshEnabled = ratesWidgetProvider.isAutoRefreshEnabled(this);
        widgetRefreshSwitch.setChecked(isAutoRefreshEnabled);

        widgetRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleAutoRefresh(isChecked);

            // Show feedback
            Snackbar.make(findViewById(R.id.drawer_layout),
                    isChecked ? "Auto-refresh enabled" : "Auto-refresh disabled",
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
        }
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

    private void handleFatalError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // Redirect to the same activity to restart
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}