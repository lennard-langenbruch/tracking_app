package com.example.myapplication22;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.Point;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InspectActivity extends AppCompatActivity {

    private static final String TAG = "InspectActivity";

    private static final float MUTED_ALPHA = 0.44f; // 0x70 / 0xFF, as the previous Waypoints text color

    /** GPS height jitters by a few meters; smaller changes are not counted as ascent/descent. */
    private static final double ALTITUDE_NOISE_THRESHOLD_M = 3.0;

    private static final String TRACK_LINE_SOURCE = "track-line-src";
    private static final String TRACK_ENDS_SOURCE = "track-ends-src";
    private static final String WAYPOINTS_SOURCE = "waypoints-src";
    private static final String WAYPOINTS_LAYER = "waypoint-bubbles";

    private MapView mapControl; // Mapbox map (token: res/values/mapbox_access_token.xml)
    private Style mapStyle; // null until the map style has finished loading

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            onCreateInternal(savedInstanceState);
        } catch (Exception e) {
            // TEMPORARY diagnostic wrapper: logs the real crash instead of letting
            // the process die silently and the task restart on the old MainActivity intent.
            Log.e(TAG, "CRASH in onCreate", e);
        }
    }

    private void onCreateInternal(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inspect);
        UiUtils.applySystemBarPadding(findViewById(R.id.main));
        UiUtils.warnIfMapboxTokenMissing(this);

        /* Map initialisieren */
        mapControl = findViewById(R.id.mapcontrol);
        MapboxHelper.bindZoomButtons(mapControl, findViewById(R.id.zoomInButton), findViewById(R.id.zoomOutButton));

        /* Extra Wert überreicht (-1: not set) */
        long trackid = getIntent().getLongExtra("trackid", -1);

        /* Datenbankverbindung herstellen */
        DatabaseHelper sqliteDatabase = new DatabaseHelper(this);

        /* Inspezierter Track */
        Track inspectedTrack = sqliteDatabase.getSingleTrack(trackid);

        /* Titel */
        setText(R.id.inspectedTrackName, inspectedTrack.getName());

        /* Koordinaten beschaffen, Wegpunkte und Gesamtdistanz berechnen */
        List<Coordinate> coordinates = sqliteDatabase.getCoordinatesByTrackId(trackid);

        List<Point> points = new ArrayList<>();
        Location previousLocation = null;
        float totalDistance = 0f;

        for (Coordinate c : coordinates) {
            double latitude = Double.parseDouble(c.getLatitude());
            double longitude = Double.parseDouble(c.getLongitude());

            Location currentLocation = new Location("1");
            currentLocation.setLatitude(latitude);
            currentLocation.setLongitude(longitude);
            if (previousLocation != null) {
                totalDistance += currentLocation.distanceTo(previousLocation);
            }
            previousLocation = currentLocation;

            points.add(Point.fromLngLat(longitude, latitude));
        }

        /* Berechnete Distanz formatieren */
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        String format;
        if (totalDistance >= 1000) {
            format = decimalFormat.format(totalDistance / 1000) + " km"; // to Kilometers
        } else {
            format = decimalFormat.format(totalDistance) + " m";
        }

        /* Daten als Zeilen "Label ..... Wert" untereinander */
        LinearLayout dataRows = findViewById(R.id.dataRows);
        String[] start = splitDateTime(inspectedTrack.getStart());
        String[] finish = splitDateTime(inspectedTrack.getFinish());
        addDataRow(dataRows, "Start Date", start[0]);
        addDataRow(dataRows, "Start Time", start[1]);
        addDataRow(dataRows, "Finish Date", finish[0]);
        addDataRow(dataRows, "Finish Time", finish[1]);
        addDataRow(dataRows, "Elapsed Time", formatElapsed(inspectedTrack.getElapsedTime()));
        addDataRow(dataRows, "Distance", format);
        double[] ascentDescent = calculateAscentDescent(coordinates);
        addDataRow(dataRows, "Ascent", formatHeightDifference(ascentDescent[0], "+"));
        addDataRow(dataRows, "Descent", formatHeightDifference(ascentDescent[1], "-"));
        addDataRow(dataRows, "Waypoints", String.valueOf(points.size()), true); // greyed out (secondary info)

        /* Karte: einmal auf den Start zentrieren, Weg, Start & Ende zeichnen */
        Switch waypointSwitch = findViewById(R.id.switchButton);

        if (!points.isEmpty()) {
            MapboxHelper.centerOn(mapControl.getMapboxMap(), points.get(0).latitude(), points.get(0).longitude(), 14.0);
        }
        mapControl.getMapboxMap().loadStyle(MapboxHelper.STYLE_URI, style -> {
            addTrackToMap(style, points);
            MapboxHelper.setLayerVisible(style, WAYPOINTS_LAYER, waypointSwitch.isChecked());
            mapStyle = style;
        });

        // Draw a small circle "bubble" at every waypoint while the switch is on
        waypointSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mapStyle != null) {
                MapboxHelper.setLayerVisible(mapStyle, WAYPOINTS_LAYER, isChecked);
            }
        });

        /* "Go Back" Button */
        findViewById(R.id.clickableImageArrow).setOnClickListener(
                v -> startActivity(new Intent(this, ListingActivity.class)));

        /* Export Button */
        findViewById(R.id.exportButton).setOnClickListener(v -> {
            BuildGpxHelper buildGpx = new BuildGpxHelper(this, inspectedTrack.getName(), coordinates);
            String filePath = buildGpx.getTotalFilePath();
            Uri fileUri = Uri.fromFile(new File(filePath));

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.setType("text/gpx");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, null));

            Toast toast = Toast.makeText(this, "GPX saved locally at \n" + filePath, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
            toast.show();
        });
    }

    /** Adds the track line, the start/finish markers and the (initially hidden) waypoint bubbles. */
    private void addTrackToMap(Style style, List<Point> points) {
        if (points.isEmpty()) {
            return;
        }

        // Weg zeichnen (a line needs at least two points)
        if (points.size() > 1) {
            MapboxHelper.addGeoJsonSource(style, TRACK_LINE_SOURCE,
                    Feature.fromGeometry(LineString.fromLngLats(points)).toJson());
            MapboxHelper.addLayer(style,
                    "{\"id\":\"track-line\",\"type\":\"line\",\"source\":\"" + TRACK_LINE_SOURCE + "\",\"source-layer\":\"\","
                            + "\"layout\":{\"line-cap\":\"round\",\"line-join\":\"round\"},"
                            + "\"paint\":{\"line-color\":\"#283593\",\"line-width\":5}}");
        }

        // Start (S) & Finish (F)
        Feature start = Feature.fromGeometry(points.get(0));
        start.addStringProperty("label", "S");
        start.addStringProperty("color", "#2E7D32");
        Feature finish = Feature.fromGeometry(points.get(points.size() - 1));
        finish.addStringProperty("label", "F");
        finish.addStringProperty("color", "#C62828");
        MapboxHelper.addGeoJsonSource(style, TRACK_ENDS_SOURCE,
                FeatureCollection.fromFeatures(Arrays.asList(start, finish)).toJson());
        MapboxHelper.addLayer(style,
                "{\"id\":\"track-ends\",\"type\":\"circle\",\"source\":\"" + TRACK_ENDS_SOURCE + "\",\"source-layer\":\"\","
                        + "\"paint\":{\"circle-radius\":11,\"circle-color\":[\"get\",\"color\"],"
                        + "\"circle-stroke-width\":2,\"circle-stroke-color\":\"#FFFFFF\"}}");
        MapboxHelper.addLayer(style,
                "{\"id\":\"track-ends-label\",\"type\":\"symbol\",\"source\":\"" + TRACK_ENDS_SOURCE + "\",\"source-layer\":\"\","
                        + "\"layout\":{\"text-field\":[\"get\",\"label\"],\"text-size\":13,"
                        + "\"text-allow-overlap\":true,\"text-ignore-placement\":true},"
                        + "\"paint\":{\"text-color\":\"#FFFFFF\"}}");

        // Waypoint bubbles, shown/hidden with the switch
        MapboxHelper.addGeoJsonSource(style, WAYPOINTS_SOURCE,
                Feature.fromGeometry(MultiPoint.fromLngLats(points)).toJson());
        MapboxHelper.addLayer(style,
                "{\"id\":\"" + WAYPOINTS_LAYER + "\",\"type\":\"circle\",\"source\":\"" + WAYPOINTS_SOURCE + "\",\"source-layer\":\"\","
                        + "\"layout\":{\"visibility\":\"none\"},"
                        + "\"paint\":{\"circle-radius\":3,\"circle-color\":\"#FFFFFF\","
                        + "\"circle-stroke-width\":1,\"circle-stroke-color\":\"#283593\"}}");
    }

    private void setText(int viewId, String text) {
        ((TextView) findViewById(viewId)).setText(text);
    }

    /** Adds one row "label ..... value" (dotted leader in between) to the container. */
    private void addDataRow(ViewGroup parent, String label, String value) {
        addDataRow(parent, label, value, false);
    }

    /** Same, but a muted row is greyed out (same 44% black as the old "Waypoints" text, #70000000). */
    private void addDataRow(ViewGroup parent, String label, String value, boolean muted) {
        View row = getLayoutInflater().inflate(R.layout.item_data_row, parent, false);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.rowValue)).setText(value);
        if (muted) {
            row.setAlpha(MUTED_ALPHA);
        }
        parent.addView(row);
    }

    /**
     * Sums up ascent and descent {meters up, meters down}. A change only counts once it differs by at
     * least ALTITUDE_NOISE_THRESHOLD_M from the last counted height, so GPS noise while standing still
     * or walking on flat ground does not add up. Both values are NaN if the track has no altitude data.
     */
    static double[] calculateAscentDescent(List<Coordinate> coordinates) {
        double ascent = 0;
        double descent = 0;
        double reference = Double.NaN;

        for (Coordinate c : coordinates) {
            if (!c.hasAltitude()) {
                continue;
            }
            double altitude = c.getAltitude();
            if (Double.isNaN(reference)) {
                reference = altitude;
                continue;
            }
            double diff = altitude - reference;
            if (diff >= ALTITUDE_NOISE_THRESHOLD_M) {
                ascent += diff;
                reference = altitude;
            } else if (diff <= -ALTITUDE_NOISE_THRESHOLD_M) {
                descent -= diff;
                reference = altitude;
            }
        }

        if (Double.isNaN(reference)) {
            return new double[]{Double.NaN, Double.NaN}; // no altitude recorded (e.g. older tracks)
        }
        return new double[]{ascent, descent}; // 0/0 is a valid result: flat track
    }

    /** "+123 m" / "-45 m", or "-" if unknown. */
    private static String formatHeightDifference(double meters, String sign) {
        if (Double.isNaN(meters)) {
            return "-";
        }
        long rounded = Math.round(meters);
        return (rounded == 0 ? "" : sign) + rounded + " m";
    }

    /** Splits a timestamp "dd.MM.yy HH:mm:ss.SSS" into {date, time without milliseconds}. */
    private static String[] splitDateTime(String timestamp) {
        if (timestamp == null) {
            return new String[]{"-", "-"};
        }
        String[] parts = timestamp.split(" ", 2);
        String time = parts.length > 1 ? parts[1] : "-";
        int dot = time.indexOf('.');
        if (dot > 0) {
            time = time.substring(0, dot);
        }
        return new String[]{parts[0], time};
    }

    /** Elapsed time is stored as "mm:ss:SSS"; shown as "mm:ss min" (other formats stay unchanged). */
    private static String formatElapsed(String elapsed) {
        if (elapsed == null) {
            return "-";
        }
        if (elapsed.matches("\\d+:\\d+:\\d+")) {
            return elapsed.substring(0, elapsed.lastIndexOf(':')) + " min";
        }
        return elapsed;
    }
}
