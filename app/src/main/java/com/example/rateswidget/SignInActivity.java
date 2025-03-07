package com.example.rateswidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

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
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(view -> signIn());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if we're coming from a logout
        if (getIntent().getBooleanExtra("fromLogout", false)) {
            Log.d(TAG, "User logged out, requiring manual sign-in");
            
            // Clear widget when logged out
            clearWidget();
            return;
        }

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserAuthorization(currentUser);
        }
    }

    private void clearWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, RatesWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // Trigger update for all widgets to show sign-in message
        Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(updateIntent);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign-In successful, account: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed
                handleSignInError(e);
            }
        }
    }

    private void handleSignInError(ApiException e) {
        String errorMessage;
        switch (e.getStatusCode()) {
            case 12500: // SIGN_IN_CANCELLED
                errorMessage = "Sign-in was cancelled";
                break;
            case 12501: // SIGN_IN_CURRENTLY_IN_PROGRESS
                errorMessage = "Sign-in is already in progress";
                break;
            case 12502: // SIGN_IN_FAILED
                errorMessage = "Sign-in failed";
                break;
            case 10: // DEVELOPER_ERROR
                errorMessage = "Developer error: Check SHA-1 & package name in Firebase console";
                break;
            default:
                errorMessage = "Error code: " + e.getStatusCode();
        }
        Log.e(TAG, "Google sign-in failed: " + errorMessage, e);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    checkUserAuthorization(user);
                } else {
                    Log.e(TAG, "Firebase authentication failed", task.getException());
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void checkUserAuthorization(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = user.getEmail();
        if (userEmail == null) {
            Toast.makeText(this, "Email not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Checking authorization for email: " + userEmail);

        firestore.collection("authorized_users")
            .document(userEmail)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Log.d(TAG, "Document exists: " + documentSnapshot.exists());
                if (documentSnapshot.exists()) {
                    Boolean isAuthorized = documentSnapshot.getBoolean("is_authorized");
                    Log.d(TAG, "is_authorized value: " + isAuthorized);
                    
                    if (Boolean.TRUE.equals(isAuthorized)) {
                        Log.d(TAG, "User authorized, proceeding to main activity");
                        proceedToMainActivity();
                    } else {
                        Log.d(TAG, "User not authorized");
                        handleUnauthorizedUser();
                    }
                } else {
                    Log.d(TAG, "No document found for email: " + userEmail);
                    handleUnauthorizedUser();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking authorization", e);
                Toast.makeText(this, "Error checking authorization", Toast.LENGTH_LONG).show();
                handleUnauthorizedUser();
            });
    }

    private void handleUnauthorizedUser() {
        // Sign out the user
        mAuth.signOut();
        
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Show unauthorized message
            Toast.makeText(this, 
                "You are not authorized to access this app", 
                Toast.LENGTH_LONG).show();
            
            // Clear widget
            clearWidget();
        });
    }

    private void proceedToMainActivity() {
        // Update widgets to show rates
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, RatesWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // Trigger update for all widgets to fetch rates
        Intent updateIntent = new Intent(this, RatesWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(updateIntent);

        // Proceed to main activity
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}