package com.gcjewellers.rateswidget;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immediately check for an existing authenticated user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Forward-thinking decision: direct the user based on authentication status
        if (currentUser != null) {
            // User is authenticated; proceed to MainActivity
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // User is not authenticated; proceed to SignInActivity
            startActivity(new Intent(this, SignInActivity.class));
        }
        // Close the SplashActivity so it doesn't remain in the back stack
        finish();
    }
}
