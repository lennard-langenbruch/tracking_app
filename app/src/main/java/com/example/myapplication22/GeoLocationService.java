package com.example.myapplication22;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class GeoLocationService extends Service {

    private static final String CHANNEL_ID = "Foreground Service ID";

    public static GeoLocationService staticInstance = null;
    private GeoLocationClient locationClient = null;

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        staticInstance = this;
        locationClient = new GeoLocationClient(this); // starts location updates itself

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager.class).createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW));
        }

        startForeground(1001, new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentText("Activity Recognition is running....")
                .setContentTitle("Mobile Tracking: currently tracking :)")
                .build());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationClient.stopLocationUpdates();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
