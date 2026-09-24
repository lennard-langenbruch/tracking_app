package com.example.myapplication22;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class GeoLocationClient {

    private static final String TAG = "GeoLocationHelper";

    private final FusedLocationProviderClient mFusedLocationClient;
    private Location mLastUpdatedLocation;
    public double longitude;
    public double latitude;
    public double altitude = Double.NaN; // meters, NaN = unknown
    private boolean isUpdatingLocation;

    public GeoLocationClient(Context context) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!isUpdatingLocation) {
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1000
            ).build();

            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            isUpdatingLocation = true;
            Log.d(TAG, "Location updates started.");
        }
    }

    public void stopLocationUpdates() {
        if (isUpdatingLocation) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
            isUpdatingLocation = false;
            Log.d(TAG, "Location updates stopped.");
        }
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                // ignore movements of 1m or less
                if (mLastUpdatedLocation == null || mLastUpdatedLocation.distanceTo(location) > 1.0) {
                    updateLocationVariables(location);
                }
            }
        }
    };

    private void updateLocationVariables(Location location) {
        mLastUpdatedLocation = location;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitude = readAltitude(location);
    }

    /**
     * Altitude in meters. Prefers the height above mean sea level (provided by Android 14+ when the
     * phone can compute it). Otherwise Location.getAltitude() is used, which Android defines as the
     * height above the WGS84 ellipsoid, and that is roughly 40-50 m higher than sea level in Germany.
     */
    private static double readAltitude(Location location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && location.hasMslAltitude()) {
            return location.getMslAltitudeMeters();
        }
        return location.hasAltitude() ? location.getAltitude() : Double.NaN;
    }
}
