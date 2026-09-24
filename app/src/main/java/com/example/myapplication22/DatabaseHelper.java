package com.example.myapplication22;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // column order must match Track constructor / trackFromCursor()
    private static final String[] TRACK_COLUMNS = {"ID", "name", "start", "finish", "elapsed"};

    public DatabaseHelper(Context context) {
        super(context, "example3.db", null, 4);
        getWritableDatabase(); // open (and create) the database right away
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE track (ID INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(255), start VARCHAR(255), finish VARCHAR(255), elapsed VARCHAR(255), distance VARCHAR(255), deleted VARCHAR(255))");
        db.execSQL("CREATE TABLE coordinates (ID INTEGER PRIMARY KEY AUTOINCREMENT,track_id INTEGER, longitude VARCHAR(255), latitude VARCHAR(255), timestamp VARCHAR(255), altitude REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // v4: altitude in meters per coordinate (NULL = unknown, e.g. for tracks recorded before)
            db.execSQL("ALTER TABLE coordinates ADD COLUMN altitude REAL");
        }
    }

    /** altitude in meters, NaN = unknown (stored as NULL) */
    long createSingleCoordinate(long trackId, String longitude, String latitude, String timestamp, double altitude) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("track_id", trackId);
        values.put("longitude", longitude);
        values.put("latitude", latitude);
        values.put("timestamp", timestamp);
        if (!Double.isNaN(altitude)) {
            values.put("altitude", altitude);
        }

        long id = db.insert("coordinates", null, values);
        db.close();
        return id;
    }

    List<Coordinate> getCoordinatesByTrackId(long trackId) {
        List<Coordinate> coordinates = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT longitude, latitude, timestamp, altitude FROM coordinates WHERE track_id = " + trackId, null)) {
            while (cursor.moveToNext()) {
                double altitude = cursor.isNull(3) ? Double.NaN : cursor.getDouble(3);
                coordinates.add(new Coordinate(cursor.getString(0), cursor.getString(1), cursor.getString(2), altitude));
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }

        db.close();
        return coordinates;
    }

    long createSingleTrack(String name, String start, String deleted) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("start", start);
        values.put("deleted", deleted);

        long id = db.insert("track", null, values);
        db.close();
        return id;
    }

    public Track getSingleTrack(long trackId) {
        Track track = null;
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query("track", TRACK_COLUMNS, "ID=?", new String[]{String.valueOf(trackId)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                track = trackFromCursor(cursor);
            }
        }

        db.close();
        return track;
    }

    public List<Track> getAllTracks() {
        List<Track> tracks = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query("track", TRACK_COLUMNS, "deleted = '0'", null, null, null, null)) {
            while (cursor.moveToNext()) {
                tracks.add(trackFromCursor(cursor));
            }
        }

        db.close();
        return tracks;
    }

    /** Updates only the columns whose new value is not null. */
    public void updateSingleTrackById(long trackId, String newName, String newStart, String newFinish, String newElapsed, String newDistance, String newDeleted) {
        ContentValues values = new ContentValues();
        putIfNotNull(values, "name", newName);
        putIfNotNull(values, "start", newStart);
        putIfNotNull(values, "finish", newFinish);
        putIfNotNull(values, "elapsed", newElapsed);
        putIfNotNull(values, "distance", newDistance);
        putIfNotNull(values, "deleted", newDeleted);

        SQLiteDatabase db = getWritableDatabase();
        db.update("track", values, "ID=?", new String[]{String.valueOf(trackId)});
        db.close();
    }

    private static Track trackFromCursor(Cursor cursor) {
        return new Track(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4));
    }

    private static void putIfNotNull(ContentValues values, String column, String value) {
        if (value != null) {
            values.put(column, value);
        }
    }
}
