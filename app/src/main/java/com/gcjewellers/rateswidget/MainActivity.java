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
import android.widget.ScrollView;
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
            // goldDate = findViewById(R.id.gold_date);
            // goldRate = findViewById(R.id.gold_rate);
            // goldYesterdayPrice = findViewById(R.id.gold_yesterday_price);
            // goldPriceChange = findViewById(R.id.gold_price_change);
            //
            // // Silver UI elements
            // TextView silverDate = findViewById(R.id.silver_date);
            // silverPrice = findViewById(R.id.silver_price);
            // silverYesterdayPrice = findViewById(R.id.silver_yesterday_price);
            // silverPriceChange = findViewById(R.id.silver_price_change);

            ratesCard = findViewById(R.id.rates_card);
            fabRefresh = findViewById(R.id.fab_refresh);
            loadingIndicator = findViewById(R.id.loading_indicator);

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

            // if (silverDate != null) {
            // silverDate.setText(currentDate);
            // }
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

    // Update the refreshRates() method in MainActivity to use the updated
    // ExtendedRatesFetchCallback
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
                        // Update main rates UI (gold and silver primary rates)
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

                        // Animate rates card
                        if (ratesCard != null) {
                            ratesCard.startAnimation(AnimationUtils.loadAnimation(
                                    MainActivity.this, R.anim.rates_update_animation));
                        }

                        // Update widgets with the data
                        updateWidgetsWithExtendedData(
                                gold995Sell, silverFutureSell,
                                new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()),
                                gold995Low, silverFutureLow,
                                calculateChangeValue(gold995Sell, gold995Low),
                                calculateChangeValue(silverFutureSell, silverFutureLow),
                                goldFuturesSell, silverFutureSell,
                                goldDollarSell, silverDollarSell, dollarSell);
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

    // New method to update all extended rates UI
    private void updateExtendedRatesUI(
            String gold995Buy, String gold995Sell, String gold995High, String gold995Low,
            String silverFutureBuy, String silverFutureSell, String silverFutureHigh, String silverFutureLow,
            String goldFuturesBuy, String goldFuturesSell, String goldFuturesHigh, String goldFuturesLow,
            String goldRefineBuy, String goldRefineSell, String goldRefineHigh, String goldRefineLow,
            String goldRtgsBuy, String goldRtgsSell, String goldRtgsHigh, String goldRtgsLow,
            String goldDollarBuy, String goldDollarSell, String goldDollarHigh, String goldDollarLow,
            String silverDollarBuy, String silverDollarSell, String silverDollarHigh, String silverDollarLow,
            String dollarBuy, String dollarSell, String dollarHigh, String dollarLow) {

        // Update Gold 995 Section
        setTextIfNotNull(this.gold995Buy, gold995Buy);
        setTextIfNotNull(this.gold995Sell, gold995Sell);
        setTextIfNotNull(this.gold995High, gold995High);
        setTextIfNotNull(this.gold995Low, gold995Low);

        // Update Silver Future Section
        setTextIfNotNull(this.silverFutureBuy, silverFutureBuy);
        setTextIfNotNull(this.silverFutureSell, silverFutureSell);
        setTextIfNotNull(this.silverFutureHigh, silverFutureHigh);
        setTextIfNotNull(this.silverFutureLow, silverFutureLow);

        // Update Gold Futures Section
        setTextIfNotNull(this.goldFuturesBuy, goldFuturesBuy);
        setTextIfNotNull(this.goldFuturesSell, goldFuturesSell);
        setTextIfNotNull(this.goldFuturesHigh, goldFuturesHigh);
        setTextIfNotNull(this.goldFuturesLow, goldFuturesLow);

        // Update Gold Refine Section
        setTextIfNotNull(this.goldRefineBuy, goldRefineBuy);
        setTextIfNotNull(this.goldRefineSell, goldRefineSell);
        setTextIfNotNull(this.goldRefineHigh, goldRefineHigh);
        setTextIfNotNull(this.goldRefineLow, goldRefineLow);

        // Update Gold RTGS Section
        setTextIfNotNull(this.goldRtgsBuy, goldRtgsBuy);
        setTextIfNotNull(this.goldRtgsSell, goldRtgsSell);
        setTextIfNotNull(this.goldRtgsHigh, goldRtgsHigh);
        setTextIfNotNull(this.goldRtgsLow, goldRtgsLow);

        // Update Gold Dollar Section
        setTextIfNotNull(this.goldDollarBuy, goldDollarBuy);
        setTextIfNotNull(this.goldDollarSell, goldDollarSell);
        setTextIfNotNull(this.goldDollarHigh, goldDollarHigh);
        setTextIfNotNull(this.goldDollarLow, goldDollarLow);

        // Update Silver Dollar Section
        setTextIfNotNull(this.silverDollarBuy, silverDollarBuy);
        setTextIfNotNull(this.silverDollarSell, silverDollarSell);
        setTextIfNotNull(this.silverDollarHigh, silverDollarHigh);
        setTextIfNotNull(this.silverDollarLow, silverDollarLow);

        // Update Dollar Section
        setTextIfNotNull(this.dollarBuy, dollarBuy);
        setTextIfNotNull(this.dollarSell, dollarSell);
        setTextIfNotNull(this.dollarHigh, dollarHigh);
        setTextIfNotNull(this.dollarLow, dollarLow);
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