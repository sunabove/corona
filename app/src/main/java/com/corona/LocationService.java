package com.corona;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationService extends Service implements ComInterface, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = ComInterface.TAG + " " + LocationService.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    public LocationService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        buildGoogleApiClient();

        showNotificationAndStartForegroundService();

        locationCallback = new LocationCallback() {
            int saveCnt = 0 ;

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //here you get the continues location updated based on the interval defined in
                //location request

                LocationDbHelper.insertGpsLog( getApplicationContext(), locationResult.getLastLocation() );

                Log.d(TAG, String.format("locationResult gps data saved[%d]: %s", saveCnt, locationResult) );

                saveCnt ++ ;
            }
        };

        this.showColonaAlarmNotification();
    }

    private void showColonaAlarmNotification() {
        int NOTIFICATION_ID = 888;
        String CHANNEL_ID = "999";

        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(this, Activity_02_Map.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentIntent(resultPendingIntent);
        builder.setSmallIcon(R.drawable.corona_alarm);
        builder.setContentTitle( "강남역 사거리 / 확진자 A");
        builder.setContentText( "동선 겹침 / 2020년 3월 24일  오후 2시 22분경 " );
        builder.setContentInfo( "자가 격리 요망" );
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        return START_STICKY;
    }

    /**
     * Method used for building GoogleApiClient and add connection callback
     */
    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleApiClient.connect();
    }

    /**
     * Method used for creating location request
     * After successfully connection of the GoogleClient ,
     * This method used for to request continues location
     */
    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LOCATION_REQUEST_PRIORITY);
        locationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_REQUEST_INTERVAL);
        locationRequest.setSmallestDisplacement(LOCATION_REQUEST_DISPLACEMENT);

        requestLocationUpdate();
    }

    /**
     * Method used for the request new location using Google FusedLocation Api
     */
    private void requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //get the last location of the device
            }
        });

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void removeLocationUpdate() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    /**
     * This Method shows notification for ForegroundService
     * Start Foreground Service and Show Notification to user for android all version
     */
    final int NOTIFICATION_ID = 100;

    private void showNotificationAndStartForegroundService() {

        final String CHANNEL_ID = BuildConfig.APPLICATION_ID.concat("_notification_id");
        final String CHANNEL_NAME = BuildConfig.APPLICATION_ID.concat("_notification_name");


        NotificationCompat.Builder builder;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String serviceName = getString(R.string.location_service_name);
        String contentText = "핸드폰 위치와 호가진자의 동선을 스캔중입니다.";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_NONE;
            assert notificationManager != null;
            NotificationChannel mChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (mChannel == null) {
                mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
                notificationManager.createNotificationChannel(mChannel);
            }
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setContentTitle( serviceName );
            builder.setContentText( contentText );
            startForeground(NOTIFICATION_ID, builder.build());
        } else {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setContentTitle( serviceName );
            builder.setContentText( contentText );
            startForeground(NOTIFICATION_ID, builder.build());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "GoogleApi Client Connected");
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApi Client Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApi Client Failed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeLocationUpdate();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }
}