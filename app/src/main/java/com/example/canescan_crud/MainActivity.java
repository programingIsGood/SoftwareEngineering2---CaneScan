package com.example.canescan_crud;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView btnGoogle;
    private com.google.android.material.button.MaterialButton btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("CaneScanPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // 0. Auto-Login Logic (Stay Signed In)
        boolean staySignedIn = prefs.getBoolean("stay_signed_in", false);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (staySignedIn && currentUser != null) {
            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        btnGoogle = findViewById(R.id.btn_google_signin);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> signIn());

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class)));

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user) {
        String userId = user.getUid();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", user.getDisplayName());
        userMap.put("email", user.getEmail());
        userMap.put("role", "user");
        userMap.put("email_verified_at", FieldValue.serverTimestamp());
        userMap.put("updated_at", FieldValue.serverTimestamp());

        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().exists()) {
                userMap.put("created_at", FieldValue.serverTimestamp());
            }

            db.collection("users").document(userId)
                    .set(userMap, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                        finish();
                    });
        });
    }
}