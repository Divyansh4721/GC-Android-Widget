package com.example.rateswidget;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_BATTERY_OPTIMIZATION = 9002;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(getString(R.string.default_web_client_id))
        .requestEmail()
        .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if we're coming from a logout
        if (getIntent().getBooleanExtra("fromLogout", false)) {
            // Do not auto-login, let the user choose to sign in again
            Log.d(TAG, "User logged out, requiring manual sign-in");
        } else {
            // Normal app start - check if user is already signed in
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // User is already signed in, proceed to MainActivity
                proceedToMainOrCheckBattery();
            }
        }
    }

    private void signIn() {
        Log.d(TAG, "Starting Google Sign-In process");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // Handle Google Sign In result
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign-In successful, account: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed - provide detailed error information
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
        } else if (requestCode == RC_BATTERY_OPTIMIZATION) {
            // After returning from battery optimization settings, proceed to MainActivity
            startMainActivity();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle: starting Firebase authentication");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "Firebase authentication successful");
                            FirebaseUser user = mAuth.getCurrentUser();
                            proceedToMainOrCheckBattery();
                        } else {
                            // Sign in fails
                            Log.e(TAG, "Firebase authentication failed", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void proceedToMainOrCheckBattery() {
        // Check if we have battery optimization permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                // Ask for battery optimization exemption
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, RC_BATTERY_OPTIMIZATION);
                Toast.makeText(this, "Please enable battery optimization exemption for reliable widget updates", 
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        // If we already have permission or it's handled, proceed to MainActivity
        startMainActivity();
    }

    private void startMainActivity() {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}