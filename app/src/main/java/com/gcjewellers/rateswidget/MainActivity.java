package com.gcjewellers.rateswidget;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 9001;

    // UI Components for Main Content
    private LinearLayout mainContent; // Container for main UI elements
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

    // Sign-In UI (assumed to be part of the same layout)
    private FrameLayout signInContainer; // Container for sign-in UI (visible if not signed in)
    private SignInButton signInButton; // Google Sign-In button

    // Firebase and Google Sign-In Client
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure activity_main.xml includes both mainContent and
                                                // signInContainer

        // Initialize UI elements that are common to both signed-in and sign-in layouts
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.black));
        }

        // Initialize Firebase Auth and GoogleSignInClient
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize view containers (make sure these IDs exist in your
        // activity_main.xml)
        mainContent = findViewById(R.id.main_content);
        // signInContainer = findViewById(R.id.sign_in_container);
        signInButton = findViewById(R.id.sign_in_button);

        // Check current authentication state
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showSignInUI();
        } else {
            hideSignInUI();
            initMainUI(currentUser);
        }
    }

    // Initialize main UI components when the user is signed in.
    private void initMainUI(FirebaseUser currentUser) {
        // Initialize main content views
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

        setupDateAndTime();
        setupUserProfile(currentUser);
        setupBatteryOptimizationSwitch();
        setupWidgetRefreshSwitch();
        setupLogoutButton();
        fetchCurrentRates();
    }

    // Show the sign-in container and hide the main content.
    private void showSignInUI() {
        signInContainer.setVisibility(View.VISIBLE);
        mainContent.setVisibility(View.GONE);
        // Set up sign-in button functionality
        signInButton.setOnClickListener(v -> signIn());
    }

    // Hide the sign-in container and show the main content.
    private void hideSignInUI() {
        signInContainer.setVisibility(View.GONE);
        mainContent.setVisibility(View.VISIBLE);
    }

    private void signIn() {
        // Disable sign-in button to prevent duplicate clicks.
        signInButton.setEnabled(false);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            // FUTURE ENHANCEMENT: Consider migrating to the Activity Result API instead of
            // startActivityForResult.
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    // Handle the result from Google Sign-In
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        signInButton.setEnabled(true);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                        .getResult(ApiException.class);
                Log.d(TAG, "Google Sign-In successful, account: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Google sign-in failed: " + e.getStatusCode(), e);
                Toast.makeText(this, "Sign-in failed", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        signInButton.setEnabled(false);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        hideSignInUI();
                        initMainUI(user);
                    } else {
                        signInButton.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupDateAndTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy | hh:mm a", Locale.getDefault());
        String currentDateTime = dateFormat.format(new Date());
        dateTimeText.setText(currentDateTime);
    }

    private void setupUserProfile(FirebaseUser user) {
        if (user != null) {
            String displayName = (user.getDisplayName() != null) ? user.getDisplayName() : "User";
            userName.setText(displayName);
            String email = (user.getEmail() != null) ? user.getEmail() : "No email";
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
            if (width <= 0)
                width = 100;
            if (height <= 0)
                height = 100;
            Bitmap profileBitmap = ProfileImageGenerator.generateCircularProfileImage(displayName, width, height);
            userProfileImage.setImageBitmap(profileBitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error generating profile image", e);
            userProfileImage.setImageResource(R.drawable.ic_default_profile);
        }
    }

    private void setupBatteryOptimizationSwitch() {
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
        WidgetManager.RatesWidgetProvider ratesWidgetProvider = new WidgetManager.RatesWidgetProvider();
        boolean isAutoRefreshEnabled = ratesWidgetProvider.isAutoRefreshEnabled(this);
        widgetRefreshSwitch.setChecked(isAutoRefreshEnabled);
        widgetRefreshSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent intent = new Intent(this, WidgetManager.RatesWidgetProvider.class);
            intent.setAction(isChecked
                    ? WidgetManager.RatesWidgetProvider.ACTION_START_UPDATES
                    : WidgetManager.RatesWidgetProvider.ACTION_STOP_UPDATES);
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
            // After logout, show sign-in UI again
            showSignInUI();
        });
    }

    // -------------------------------------------------------------------------
    // Integrated static inner class: ProfileImageGenerator
    // -------------------------------------------------------------------------
    public static class ProfileImageGenerator {
        public static Bitmap generateCircularProfileImage(String name, int width, int height) {
            if (width <= 0)
                width = 100;
            if (height <= 0)
                height = 100;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint backgroundPaint = new Paint();
            backgroundPaint.setStyle(Paint.Style.FILL);
            backgroundPaint.setColor(generateColorFromName(name));
            canvas.drawCircle(width / 2f, height / 2f, Math.min(width, height) / 2f, backgroundPaint);
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setTextSize(width * 0.5f);
            String initials = getInitials(name);
            Rect textBounds = new Rect();
            textPaint.getTextBounds(initials, 0, initials.length(), textBounds);
            canvas.drawText(initials, width / 2f, height / 2f + textBounds.height() / 2f - textBounds.bottom,
                    textPaint);
            return bitmap;
        }

        private static String getInitials(String name) {
            if (name == null || name.trim().isEmpty())
                return "?";
            String[] nameParts = name.trim().split("\\s+");
            if (nameParts.length == 1) {
                return nameParts[0].substring(0, 1).toUpperCase();
            }
            return (nameParts[0].substring(0, 1) + nameParts[nameParts.length - 1].substring(0, 1)).toUpperCase();
        }

        private static int generateColorFromName(String name) {
            if (name == null || name.isEmpty())
                return Color.GRAY;
            int[] goldenColors = {
                    Color.rgb(255, 215, 0), // Gold
                    Color.rgb(218, 165, 32), // Goldenrod
                    Color.rgb(238, 232, 170), // Pale Goldenrod
                    Color.rgb(189, 183, 107), // Dark Khaki
                    Color.rgb(240, 230, 140) // Khaki
            };
            return goldenColors[Math.abs(name.hashCode()) % goldenColors.length];
        }
    }

    // -------------------------------------------------------------------------
    // Integrated static inner class: RatesFetcher
    // -------------------------------------------------------------------------
    public static class RatesFetcher {
        private static final String TAG = "RatesFetcher";
        private static final String API_URL = "https://goldrate.divyanshbansal.com/api/live";
        private static final int GOLD_ROW = 5;
        private static final int SILVER_ROW = 4;
        private static final int RATE_COLUMN = 1;

        public interface RatesFetchListener {
            void onRatesFetched(String goldRate, String silverRate, String lastUpdated);

            void onError(String errorMessage);
        }

        public static void fetchRates(final Context context, final RatesFetchListener listener) {
            new Thread(() -> {
                String[] rates = new String[2];
                boolean hasError = false;
                String errorMessage = "";
                try {
                    Log.d(TAG, "Making API request to: " + API_URL);
                    URL url = new URL(API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    int responseCode = connection.getResponseCode();
                    Log.d(TAG, "API response code: " + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        reader.close();
                        JSONArray jsonArray = new JSONArray(responseBuilder.toString());
                        if (jsonArray.length() > Math.max(GOLD_ROW, SILVER_ROW)) {
                            JSONArray goldRowArray = jsonArray.getJSONArray(GOLD_ROW);
                            JSONArray silverRowArray = jsonArray.getJSONArray(SILVER_ROW);
                            if (goldRowArray.length() > RATE_COLUMN && silverRowArray.length() > RATE_COLUMN) {
                                rates[0] = goldRowArray.getString(RATE_COLUMN);
                                rates[1] = silverRowArray.getString(RATE_COLUMN);
                                Log.d(TAG, "Gold rate: " + rates[0] + ", Silver rate: " + rates[1]);
                            } else {
                                hasError = true;
                                errorMessage = "Invalid response format: rate column not found";
                            }
                        } else {
                            hasError = true;
                            errorMessage = "Invalid response format: required rows not found";
                        }
                    } else {
                        hasError = true;
                        errorMessage = "Server returned code: " + responseCode;
                        Log.e(TAG, errorMessage);
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    hasError = true;
                    errorMessage = "Error: " + e.getMessage();
                    Log.e(TAG, "API request failed", e);
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String currentTime = dateFormat.format(new Date());
                final String goldRate = (rates[0] != null) ? rates[0] : "N/A";
                final String silverRate = (rates[1] != null) ? rates[1] : "N/A";
                final boolean finalHasError = hasError;
                final String finalErrorMessage = errorMessage;
                ((MainActivity) context).runOnUiThread(() -> {
                    if (finalHasError) {
                        listener.onError(finalErrorMessage);
                    } else {
                        listener.onRatesFetched(goldRate, silverRate, currentTime);
                    }
                });
            }).start();
        }
    }
}
