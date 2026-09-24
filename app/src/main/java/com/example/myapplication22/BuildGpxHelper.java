package com.example.myapplication22;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

public class BuildGpxHelper {

    private static final String TAG = "BuildGpxHelper";
    private static final int REQUEST_CODE = 123;

    private String totalFilePath = "";

    public BuildGpxHelper(Activity activity, String trackName, List<Coordinate> coordinates) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
        } else {
            handleFileUriExposure();

            writeGpxFile(trackName, coordinates);
        }
    }

    private void handleFileUriExposure() {
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void writeGpxFile(String trackName, List<Coordinate> coordinates) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        trackName = trackName.trim();
        String fileName = trackName + ".gpx";
        File file = new File(directory, fileName);

        StringBuilder gpx = new StringBuilder();
        gpx.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n");
        gpx.append("<name>").append(trackName).append("</name><trkseg>\n");
        for (Coordinate coordinate : coordinates) {
            gpx.append("<trkpt lat=\"").append(coordinate.getLatitude())
                    .append("\" lon=\"").append(coordinate.getLongitude())
                    .append("\"><time>").append(coordinate.getTimestamp())
                    .append("</time></trkpt>\n");
        }
        gpx.append("</trkseg></trk></gpx>");

        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(gpx.toString());
            Log.d(TAG, "GPX file saved successfully in Download Folder" + directory.getPath());
        } catch (IOException e) {
            Log.e(TAG, "Error writing GPX file", e);
        }
        this.totalFilePath = directory.getPath() + "/" + fileName;
    }

    public String getTotalFilePath() {
        return this.totalFilePath;
    }
}
