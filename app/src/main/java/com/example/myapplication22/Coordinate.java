package com.example.myapplication22;

public class Coordinate {

    private final String longitude;
    private final String latitude;
    private final String timestamp;
    private final double altitude; // meters, NaN if unknown

    public Coordinate(String longitude, String latitude, String timestamp, double altitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.timestamp = timestamp;
        this.altitude = altitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public double getAltitude() {
        return altitude;
    }

    public boolean hasAltitude() {
        return !Double.isNaN(altitude);
    }
}
