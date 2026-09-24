package com.example.myapplication22;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_PERMISSIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (!hasLocationPermissions() || !hasFilePermissions()) {
            requestPermissions();
        }

        UiUtils.applySystemBarPadding(findViewById(R.id.verticalLayout));

        try {
            new DatabaseHelper(this); // creates the database on first start
        } catch (Exception e) {
            Log.d(TAG, "sqlite database creation caused : " + e);
        }

        findViewById(R.id.startTrackingButton).setOnClickListener(
                v -> startActivity(new Intent(this, TrackingActivity.class)));
        findViewById(R.id.viewTracksButton).setOnClickListener(
                v -> startActivity(new Intent(this, ListingActivity.class)));

        if (getIntent().hasExtra("message")) {
            displayDialog(getIntent().getStringExtra("message"));
        }
    }

    private void displayDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", (dialog, id) -> dialog.cancel())
                .show();
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasFilePermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_PERMISSIONS) {
            return;
        }
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                // Permissions denied, close the app
                new AlertDialog.Builder(this)
                        .setTitle("Permission Denied")
                        .setMessage("Location and file permissions are required for this app. Please grant them in settings.")
                        .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                        .setCancelable(false)
                        .show();
                return;
            }
        }
    }
}
