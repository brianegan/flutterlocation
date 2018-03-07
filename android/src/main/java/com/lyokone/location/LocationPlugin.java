package com.lyokone.location;

import android.Manifest;
import android.app.Activity;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * LocationPlugin
 */
public class LocationPlugin implements MethodCallHandler, StreamHandler, PluginRegistry.RequestPermissionsResultListener {
    private static final String STREAM_CHANNEL_NAME = "lyokone/locationstream";
    private static final String METHOD_CHANNEL_NAME = "lyokone/location";

    private static final int FUTURE_REQUEST_PERMISSIONS_REQUEST_CODE = 1259094578;
    private static final int STREAM_REQUEST_PERMISSIONS_REQUEST_CODE = 2080914098;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final String ERROR_CODE = "PERMISSION_ERROR";
    public static final String PERMISSIONS_NOT_GRANTED = "Permissions were not granted";

    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;

    private Result getLocationResult;
    private EventSink events;
    private final Activity activity;

    LocationPlugin(Activity activity) {
        this.activity = activity;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        mSettingsClient = LocationServices.getSettingsClient(activity);
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                HashMap<String, Double> loc = new HashMap<String, Double>();
                loc.put("latitude", location.getLatitude());
                loc.put("longitude", location.getLongitude());
                loc.put("accuracy", (double) location.getAccuracy());
                loc.put("altitude", location.getAltitude());
                if (events != null) {
                    events.success(loc);
                }

            }
        };
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions(int requestCode) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                requestCode);
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        LocationPlugin plugin = new LocationPlugin(registrar.activity());
        final MethodChannel channel = new MethodChannel(registrar.messenger(), METHOD_CHANNEL_NAME);
        channel.setMethodCallHandler(plugin);
        registrar.addRequestPermissionsResultListener(plugin);

        final EventChannel eventChannel = new EventChannel(registrar.messenger(), STREAM_CHANNEL_NAME);
        eventChannel.setStreamHandler(new LocationPlugin(registrar.activity()));
    }

    private void getLastLocation() {
        final Task<Location> lastLocation = mFusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    HashMap<String, Double> loc = new HashMap<String, Double>();
                    loc.put("latitude", location.getLatitude());
                    loc.put("longitude", location.getLongitude());
                    loc.put("accuracy", (double) location.getAccuracy());
                    loc.put("altitude", location.getAltitude());
                    getLocationResult.success(loc);
                } else {
                    getLocationResult.error(ERROR_CODE, "Failed to get location.", null);
                }
                getLocationResult = null;
            }
        });

        lastLocation.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                getLocationResult.error(ERROR_CODE, e.getMessage(), e);
                getLocationResult = null;
            }
        });
    }

    @Override
    public void onMethodCall(MethodCall call, final Result result) {
        if (call.method.equals("getLocation")) {
            getLocationResult = result;
            if (!checkPermissions()) {
                requestPermissions(FUTURE_REQUEST_PERMISSIONS_REQUEST_CODE);
            } else {
                getLastLocation();
            }
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onListen(Object arguments, final EventSink eventsSink) {
        events = eventsSink;

        if (!checkPermissions()) {
            requestPermissions(STREAM_REQUEST_PERMISSIONS_REQUEST_CODE);
            return;
        }

        /**
         * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
         * runtime permission has been granted.
         */
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback,
                                Looper.myLooper());
                    }
                }).addOnFailureListener(activity, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            ResolvableApiException rae = (ResolvableApiException) e;
                            rae.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sie) {
                            Log.i(METHOD_CHANNEL_NAME, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        String errorMessage = "Location settings are inadequate, and cannot be "
                                + "fixed here. Fix in Settings.";
                        Log.e(METHOD_CHANNEL_NAME, errorMessage);
                }
            }
        });
    }

    @Override
    public void onCancel(Object arguments) {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        events = null;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case STREAM_REQUEST_PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onListen(null, events);
                } else {
                    // TODO: Implement Stream Case where permissions are denied immediately.
                    events.error(ERROR_CODE, PERMISSIONS_NOT_GRANTED, "");
                }
                break;
            case FUTURE_REQUEST_PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                } else {
                    getLocationResult.error(ERROR_CODE, PERMISSIONS_NOT_GRANTED, null);
                    getLocationResult = null;
                }
                break;
        }

        return true;
    }
}
