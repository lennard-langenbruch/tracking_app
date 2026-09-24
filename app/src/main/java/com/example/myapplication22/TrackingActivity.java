package com.example.myapplication22;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TrackingActivity extends AppCompatActivity {

    private static final String TAG = "TrackingActivity";

    private static final String LOCATION_SOURCE_ID = "current-location-src";
    private static final String LOCATION_LAYER_JSON =
            "{\"id\":\"current-location\",\"type\":\"circle\",\"source\":\"" + LOCATION_SOURCE_ID + "\",\"source-layer\":\"\","
                    + "\"paint\":{\"circle-radius\":8,\"circle-color\":\"#1976D2\","
                    + "\"circle-stroke-width\":3,\"circle-stroke-color\":\"#FFFFFF\"}}";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS");
    private final SimpleDateFormat gpxFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private MapView mapControl; // Mapbox map (token: res/values/mapbox_access_token.xml)
    private Style mapStyle; // null until the map style has finished loading
    private DatabaseHelper sqliteDao; // Database Access Object
    private GeoLocationClient geoLocation;
    private StopWatchHelper sw;
    private long currentTackId;
    private String currentTrackName;
    private TextView textLong;
    private TextView textLat;
    private TextView textAlt;
    private String lastSavedLongitude;
    private String lastSavedLatitude;
    private Date ds; // track start
    private boolean started = false;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tracking);
        UiUtils.applySystemBarPadding(findViewById(R.id.verticalLayout));
        UiUtils.warnIfMapboxTokenMissing(this);

        // Start Tracking Foreground Service
        ContextCompat.startForegroundService(this, new Intent(this, GeoLocationService.class));

        sqliteDao = new DatabaseHelper(this);

        currentTackId = sqliteDao.createSingleTrack("temp", "start", "0");
        currentTrackName = "Track " + currentTackId;
        ds = new Date();
        sqliteDao.updateSingleTrackById(currentTackId, currentTrackName, dateFormat.format(ds), null, null, null, null);

        TextView timeText = findViewById(R.id.time);
        sw = new StopWatchHelper(timeText);

        geoLocation = new GeoLocationClient(this);

        textLong = findViewById(R.id.longitude);
        textLat = findViewById(R.id.latitude);
        textAlt = findViewById(R.id.altitude);

        mapControl = findViewById(R.id.mapcontrol);
        MapboxHelper.bindZoomButtons(mapControl, findViewById(R.id.zoomInButton), findViewById(R.id.zoomOutButton));
        MapboxHelper.setZoom(mapControl.getMapboxMap(), 14.0);
        mapControl.getMapboxMap().loadStyle(MapboxHelper.STYLE_URI, style -> {
            // layer for the current position, its data is updated in updateLocation()
            MapboxHelper.addGeoJsonSource(style, LOCATION_SOURCE_ID, MapboxHelper.emptyFeatureCollection());
            MapboxHelper.addLayer(style, LOCATION_LAYER_JSON);
            mapStyle = style;
        });

        // The first location fix arrives asynchronously (see updateLocation()).
        timeText.setText("waiting for location to start timer");
        textLong.setText("loading ... ");
        textLat.setText("loading ...");
        textAlt.setText("loading ...");

        findViewById(R.id.stopTrackingButton).setOnClickListener(v -> {
            sw.stop();
            handler.removeCallbacksAndMessages(null);
            saveTrackEnd();

            GeoLocationService.staticInstance.stopSelf();

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("message", currentTrackName + " was saved successfully");
            startActivity(intent);
        });

        // Periodically update the location (every second, first run immediately)
        handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateLocation();
                handler.postDelayed(this, 1000);
            }
        });
    }

    // update Location by seconds passed
    private void updateLocation() {
        if (geoLocation.longitude == 0.0) { // no location yet
            textLong.setText("loading ... ");
            textLat.setText("loading ...");
            textAlt.setText("loading ...");
            return;
        }

        if (!started) {
            started = true;
            sw.start();
        }

        String longitudeString = String.valueOf(geoLocation.longitude);
        String latitudeString = String.valueOf(geoLocation.latitude);

        textLong.setText("Longitude: " + longitudeString);
        textLat.setText("Latitude: " + latitudeString);
        textAlt.setText("Altitude: " + formatAltitude(geoLocation.altitude));

        // Save coordinates and link to track id, but only if the location has changed
        if (!latitudeString.equals(lastSavedLatitude) || !longitudeString.equals(lastSavedLongitude)) {
            String gpx = gpxFormat.format(new Date());
            sqliteDao.createSingleCoordinate(currentTackId, longitudeString, latitudeString, gpx, geoLocation.altitude);
            Log.d(TAG, "Coordinate saved to track " + currentTackId + ": " + longitudeString + ", " + latitudeString + ", timestamp: " + gpx);
        }
        lastSavedLongitude = longitudeString;
        lastSavedLatitude = latitudeString;

        // move the position marker and center the map on it
        if (mapStyle != null) {
            MapboxHelper.setGeoJsonData(mapStyle, LOCATION_SOURCE_ID,
                    MapboxHelper.pointFeature(geoLocation.latitude, geoLocation.longitude));
        }
        MapboxHelper.centerOn(mapControl.getMapboxMap(), geoLocation.latitude, geoLocation.longitude);
    }

    /** "123 m", or "n/a" if the device delivers no altitude. */
    private static String formatAltitude(double altitude) {
        return Double.isNaN(altitude) ? "n/a" : Math.round(altitude) + " m";
    }

    /** Writes finish time and elapsed time (without stopwatch) of the current track to the database. */
    @SuppressLint("DefaultLocale")
    private void saveTrackEnd() {
        Date now = new Date();
        long differenceInMillis = now.getTime() - ds.getTime();

        long minutes = differenceInMillis / (1000 * 60);
        long seconds = (differenceInMillis / 1000) % 60;
        long milliseconds = differenceInMillis % 1000;
        String elapsedTimeWithoutStopwatch = String.format("%02d:%02d:%02d", minutes, seconds, milliseconds);

        sqliteDao.updateSingleTrackById(currentTackId, null, null, dateFormat.format(now), elapsedTimeWithoutStopwatch, "0", null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveTrackEnd();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy(); // necessary
        // stopTracking Button Action minus Intent & View switch
        sw.stop();
        handler.removeCallbacksAndMessages(null);
        GeoLocationService.staticInstance.stopSelf();
    }
}
