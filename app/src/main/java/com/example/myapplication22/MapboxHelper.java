package com.example.myapplication22;

import android.util.Log;
import android.view.View;

import com.mapbox.bindgen.Expected;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;

/**
 * Small Java-friendly wrapper around the Mapbox Maps SDK (v11).
 *
 * Sources and layers are added through the "raw" style API (JSON), because the typed
 * Kotlin DSL extensions (style.addSource / style.addLayer) cannot be called from Java.
 *
 * The access token is NOT set here: the SDK reads it from the string resource
 * "mapbox_access_token" in res/values/mapbox_access_token.xml.
 */
final class MapboxHelper {

    private static final String TAG = "MapboxHelper";

    /** Map style used by all screens. */
    static final String STYLE_URI = Style.MAPBOX_STREETS;

    private MapboxHelper() {}

    // ---------- style: sources & layers ----------

    /** Adds a GeoJSON source; geoJson is a GeoJSON object (Feature, FeatureCollection, ...) as text. */
    static void addGeoJsonSource(Style style, String sourceId, String geoJson) {
        Expected<String, Value> source = Value.fromJson("{\"type\":\"geojson\",\"data\":" + geoJson + "}");
        if (source.isError()) {
            Log.e(TAG, "Invalid GeoJSON for source " + sourceId + ": " + source.getError());
            return;
        }
        Expected<String, ?> added = style.addStyleSource(sourceId, source.getValue());
        if (added.isError()) {
            Log.e(TAG, "Could not add source " + sourceId + ": " + added.getError());
        }
    }

    /** Adds a layer on top of all existing layers; layerJson is a layer definition in style-spec JSON. */
    static void addLayer(Style style, String layerJson) {
        Expected<String, Value> layer = Value.fromJson(layerJson);
        if (layer.isError()) {
            Log.e(TAG, "Invalid layer JSON: " + layer.getError());
            return;
        }
        Expected<String, ?> added = style.addStyleLayer(layer.getValue(), null);
        if (added.isError()) {
            Log.e(TAG, "Could not add layer: " + added.getError());
        }
    }

    /** Replaces the data of an already added GeoJSON source. */
    static void setGeoJsonData(Style style, String sourceId, String geoJson) {
        Expected<String, Value> data = Value.fromJson(geoJson);
        if (data.isError()) {
            Log.e(TAG, "Invalid GeoJSON for source " + sourceId + ": " + data.getError());
            return;
        }
        Expected<String, ?> result = style.setStyleSourceProperty(sourceId, "data", data.getValue());
        if (result.isError()) {
            Log.e(TAG, "Could not update source " + sourceId + ": " + result.getError());
        }
    }

    static void setLayerVisible(Style style, String layerId, boolean visible) {
        style.setStyleLayerProperty(layerId, "visibility", Value.valueOf(visible ? "visible" : "none"));
    }

    // ---------- GeoJSON helpers ----------

    static String emptyFeatureCollection() {
        return "{\"type\":\"FeatureCollection\",\"features\":[]}";
    }

    static String pointFeature(double latitude, double longitude) {
        return Feature.fromGeometry(Point.fromLngLat(longitude, latitude)).toJson();
    }

    // ---------- camera ----------

    static void setZoom(MapboxMap map, double zoom) {
        map.setCamera(new CameraOptions.Builder().zoom(zoom).build());
    }

    static void centerOn(MapboxMap map, double latitude, double longitude) {
        map.setCamera(new CameraOptions.Builder().center(Point.fromLngLat(longitude, latitude)).build());
    }

    static void centerOn(MapboxMap map, double latitude, double longitude, double zoom) {
        map.setCamera(new CameraOptions.Builder().center(Point.fromLngLat(longitude, latitude)).zoom(zoom).build());
    }

    /** Changes the zoom level by delta, e.g. +1 (zoom in) or -1 (zoom out). */
    static void zoomBy(MapboxMap map, double delta) {
        map.setCamera(new CameraOptions.Builder().zoom(map.getCameraState().getZoom() + delta).build());
    }

    /** Wires the "+" / "-" buttons of layout view_zoom_buttons to the map. */
    static void bindZoomButtons(MapView mapView, View zoomInButton, View zoomOutButton) {
        zoomInButton.setOnClickListener(v -> zoomBy(mapView.getMapboxMap(), 1.0));
        zoomOutButton.setOnClickListener(v -> zoomBy(mapView.getMapboxMap(), -1.0));
    }
}
