package com.gcjewellers.rateswidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private SignInButton signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Setup Sign In Button
        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(view -> {
            // Disable button during sign-in process to prevent multiple clicks
            signInButton.setEnabled(false);
            signIn();
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if we're coming from a logout
        if (getIntent().getBooleanExtra("fromLogout", false)) {
            Log.d(TAG, "User logged out, requiring manual sign-in");
            clearWidget();
            return;
        }

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Attempt to check user authorization
            checkUserAuthorization(currentUser);
        }
    }

    private void signIn() {
        // Clear any previous sign-in attempts
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Launch the Google Sign-In intent
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Re-enable sign-in button
        signInButton.setEnabled(true);

        if (requestCode == RC_SIGN_IN) {
            try {
                // Google Sign In was successful, authenticate with Firebase
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign-In successful, account: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                handleSignInError(e);
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
                        checkUserAuthorization(user);
                    } else {
                        handleAuthenticationFailure(task.getException());
                    }
                });
    }

    private void checkUserAuthorization(FirebaseUser user) {
        if (user == null) {
            Log.e(TAG, "No user found during authorization check");
            showErrorAndSignOut("No user found");
            return;
        }

        String userEmail = user.getEmail();
        if (userEmail == null) {
            Log.e(TAG, "User email is null during authorization check");
            showErrorAndSignOut("Email not available");
            return;
        }

        Log.d(TAG, "Checking authorization for email: " + userEmail);

        // Check authorization in Firestore
        firestore.collection("authorized_users")
                .document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Document exists: " + documentSnapshot.exists());
                    if (documentSnapshot.exists()) {
                        Boolean isAuthorized = documentSnapshot.getBoolean("is_authorized");
                        Log.d(TAG, "is_authorized value: " + isAuthorized);
                        if (Boolean.TRUE.equals(isAuthorized)) {
                            proceedToMainActivity();
                        } else {
                            handleUnauthorizedUser();
                        }
                    } else {
                        handleUnauthorizedUser();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking authorization", e);
                    handleAuthorizationFailure(e);
                });
    }

    private void handleUnauthorizedUser() {
        signOutAndClearWidget("You are not authorized to access this app");
    }

    private void handleAuthenticationFailure(Exception exception) {
        signInButton.setEnabled(true);
        if (exception != null) {
            Log.e(TAG, "Authentication failed", exception);
            String errorMessage = "Authentication Failed: ";
            if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                errorMessage += "Invalid credentials";
            } else if (exception instanceof FirebaseNetworkException) {
                errorMessage += "Network error";
            } else {
                errorMessage += exception.getMessage();
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
        signOutAndClearWidget(null);
    }

    private void handleAuthorizationFailure(Exception e) {
        String errorMessage = "Error checking authorization: " + (e != null ? e.getMessage() : "Unknown error");
        Log.e(TAG, errorMessage, e);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        signOutAndClearWidget(errorMessage);
    }

    private void signOutAndClearWidget(String message) {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            clearWidget();
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
            signInButton.setEnabled(true);
        });
    }

    private void clearWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, RatesWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(updateIntent);
    }

    private void proceedToMainActivity() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, RatesWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(updateIntent);
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void handleSignInError(ApiException e) {
        signInButton.setEnabled(true);
        String errorMessage;
        switch (e.getStatusCode()) {
            case 12500:
                errorMessage = "Sign-in was cancelled";
                break;
            case 12501:
                errorMessage = "Sign-in is already in progress";
                break;
            case 12502:
                errorMessage = "Sign-in failed";
                break;
            case 10:
                errorMessage = "Network error during sign-in";
                break;
            default:
                errorMessage = "Error code: " + e.getStatusCode();
        }
        Log.e(TAG, "Google sign-in failed: " + errorMessage, e);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void showErrorAndSignOut(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        signOutAndClearWidget(message);
    }
}
