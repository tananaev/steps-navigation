package com.tananaev.stepsnavigation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, SensorEventListener {

    private SupportMapFragment fragment;
    private GoogleMap map;
    private Snackbar snackbar;

    private final List<Marker> markers = new ArrayList<>();

    private double lat;
    private double lon;

    private static final double step = 0.762;
    private static final double radius = 6378137;

    private float[] gravity;
    private float[] magnetic;

    private SensorManager sensorManager;

    private void showInfo(int message) {
        snackbar = Snackbar.make(fragment.getView(), message, Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sensorManager.unregisterListener(this);
    }

    @Override
    public synchronized void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            if (gravity != null && magnetic != null && !markers.isEmpty()) {

                float R[] = new float[9];
                float I[] = new float[9];

                if (SensorManager.getRotationMatrix(R, I, gravity, magnetic)) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    float azimut = orientation[0];

                    lat += step * Math.cos(azimut) * 180 / radius / Math.PI;
                    lon += step * Math.sin(azimut) * 180 / radius / Math.cos(Math.PI * lat / 180) / Math.PI;

                    LatLng latLng = new LatLng(lat, lon);
                    map.addMarker(new MarkerOptions().position(latLng));
                    map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravity = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetic = event.values.clone();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMapLongClickListener(this);

        showInfo(R.string.info_select_location);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        lat = latLng.latitude;
        lon = latLng.longitude;

        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();

        markers.add(map.addMarker(new MarkerOptions().position(latLng)));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.getMaxZoomLevel()));

        showInfo(R.string.info_navigation);
    }

}
