package com.corona;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.BuildConfig;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LocationService extends Service implements ComInterface, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = ComInterface.TAG + " " + LocationService.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private Proj projection = Proj.projection();
    private DbHelper dbHelper;

    int gpsInsCnt = 0;

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

        this.requestQueue = Volley.newRequestQueue(this);
        this.dbHelper = DbHelper.getLocationDbHelper( this );

        this.buildGoogleApiClient();

        this.showNotificationAndStartForegroundService();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //here you get the continues location updated based on the interval defined in
                //location request
                whenLocationUpdated(locationResult);
            }
        };

        this.startCoronaDataFromServerHandler();
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
    public static LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LOCATION_REQUEST_PRIORITY);
        locationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
        locationRequest.setFastestInterval(LOCATION_REQUEST_INTERVAL);
        locationRequest.setSmallestDisplacement(LOCATION_REQUEST_DISPLACEMENT_METERS);

        return locationRequest;
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "GoogleApi Client Connected");

        this.locationRequest = createLocationRequest();

        this.requestLocationUpdate();
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
        String contentText = "핸드폰 위치와 확진자의 동선을 스캔중입니다.";

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
            builder.setContentTitle(serviceName);
            builder.setContentText(contentText);
            startForeground(NOTIFICATION_ID, builder.build());
        } else {
            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setContentTitle(serviceName);
            builder.setContentText(contentText);
            startForeground(NOTIFICATION_ID, builder.build());
        }
    }

    private void whenLocationUpdated(LocationResult locationResult) {
        if( null == locationResult || null == locationResult.getLastLocation() ) {
            return;
        }

        this.dbHelper.insertGpsLog( locationResult.getLastLocation());

        Log.d(TAG, String.format("locationResult gps data inserted[%d]: %s", gpsInsCnt, locationResult));

        this.gpsInsCnt++;

        this.getCoronaDataFromServer();

        this.checkCurrDataAndRemoveOldGpsData();
    }

    private long prevCheckDataTime = 0 ;

    private long timeCheckCnt = 0 ;
    private static final long TWO_WEEK_TIME = 14*ONE_DAY_TIME;

    private void checkCurrDataAndRemoveOldGpsData() {
        if( timeCheckCnt%10_000 != 0) {
            timeCheckCnt ++ ;
            return;
        }

        long now = System.currentTimeMillis();

        if( 0 == prevCheckDataTime || now - prevCheckDataTime > ONE_DAY_TIME ) {

            long two_weeks_ags = now - TWO_WEEK_TIME ;

            DbHelper dbHelper = this.dbHelper;
            SQLiteDatabase db = dbHelper.wdb ;
            String whereClause = " visit_tm < ? ";
            String [] args = { "" + two_weeks_ags };

            int updCnt = db.delete( "gps", whereClause, args );

            Log.d( TAG, "gps two weeks ago del cnt = " + updCnt );

            prevCheckDataTime = now ;
        }

        timeCheckCnt ++ ;
    }

    private void startCoronaDataFromServerHandler() {
        final long delay = CORONA_DB_GET_INTERVAL;

        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                getCoronaDataFromServer( );
                handler.postDelayed( this, delay);
            }
        };

        handler.postDelayed( runnable, 2_000);
    }

    private boolean gettingCoronaDataFromServer = false ;

    private void getCoronaDataFromServer() {
        try {
            if( ! gettingCoronaDataFromServer ) {
                gettingCoronaDataFromServer = true ;
                getCoronaDataFromServerImpl2();
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            gettingCoronaDataFromServer = false ;
        }

    }

    private int coronaDbHandlerCnt = 0;
    protected RequestQueue requestQueue ;

    private void getCoronaDataFromServerImpl2( ) {
        Log.d(TAG, String.format("Corona DbHandler[%d]:", coronaDbHandlerCnt));

        String url = "http://sunabove.iptime.org:8080/corona_map-1/corona/data.json";

        String up_dt = "";
        long coronaMaxUpDt = DbHelper.getLocationDbHelper( this ).getCoronaMaxUpDt();

        if( coronaMaxUpDt < 1 ) {
            up_dt = "" ;
        } else {
            up_dt = "" + ( coronaMaxUpDt + 1 );
        }

        try {
            url += "?up_dt=" + URLEncoder.encode( up_dt, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Response: url = " + url );

        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, "Response: Success " + response.toString());
                        whenCoronaDbReceived( response );

                        dbHelper.checkCoronaInfection();

                        showCoronaInfectionAlarmNotifications( );
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Response: Error Message " + error.getMessage());
                        Log.d(TAG, "Response: Error " + error.toString());

                        dbHelper.checkCoronaInfection();
                        showCoronaInfectionAlarmNotifications( );
                    }
                }
        );

        // Access the RequestQueue through your singleton class.
        coronaDbHandlerCnt ++ ;
        this.requestQueue.add( jsonObjectRequest );
    }

    private void showCoronaInfectionAlarmNotifications( ) {

        if( true ) {
            SQLiteDatabase db = this.dbHelper.wdb;
            String sql = "";
            sql += " SELECT id, deleted, checked, notification, up_dt, place, patient, visit_fr, visit_to ";
            sql += " , latitude, longitude ";
            sql += " FROM corona ";
            sql += " WHERE deleted = 0 AND checked = 1 AND notification = 0 ";
            sql += " ORDER BY up_dt ASC ";
            ;

            String[] args = { };
            Cursor cursor = db.rawQuery(sql, args);

            SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm");

            while (cursor.moveToNext()) {
                Corona corona = new Corona();
                corona.id = cursor.getLong(cursor.getColumnIndex("id"));

                corona.deleted = cursor.getLong(cursor.getColumnIndex("deleted"));
                corona.checked = cursor.getLong(cursor.getColumnIndex("checked"));
                corona.notification = cursor.getLong(cursor.getColumnIndex("notification"));

                corona.up_dt = cursor.getLong(cursor.getColumnIndex("up_dt"));
                corona.place = cursor.getString(cursor.getColumnIndex("place"));
                corona.patient = cursor.getString(cursor.getColumnIndex("patient"));

                corona.visit_fr = cursor.getLong(cursor.getColumnIndex("visit_fr"));
                corona.visit_to = cursor.getLong(cursor.getColumnIndex("visit_to"));

                corona.latitude = cursor.getFloat(cursor.getColumnIndex("latitude"));
                corona.longitude = cursor.getFloat(cursor.getColumnIndex("longitude"));

                corona.title = String.format("[%d] %s / %s / 동선 겹침", corona.id, corona.place, corona.patient);
                corona.text = String.format("%s ~ %s / 자가 격리 요망", df.format(new Date(corona.visit_fr)), df.format(new Date(corona.visit_to)));
                corona.content = "동선 겹침 / 자가 격리 요망";

                corona.up_dt_str = df.format(new Date(corona.up_dt));

                String info = String.format("corona notification notification = %d, title = %s, text = %s, latitude = %f, longitude = %f, up_dt = %s",
                        corona.notification, corona.title, corona.text, corona.latitude, corona.longitude, corona.up_dt_str);
                Log.d(TAG, info);

                this.showColonaDetectionAlarmNotificationImpl(corona);

                if( true ) {
                    String table = "corona" ;
                    String whereClause =  " id = ? ";

                    ContentValues values = new ContentValues();
                    values.put( "notification", 1 );

                    args = new String[] { "" + corona.id };

                    int updCnt = db.update( table , values, whereClause, args );

                    Log.d( TAG, "notification updCnt = " + updCnt );
                }
            }

            cursor.close();
        }
    }

    private void whenCoronaDbReceived(JSONArray response) {
        try {
            this.dbHelper.whenCoronaDbReceived( response );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showColonaDetectionAlarmNotificationImpl( Corona corona) {
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
        builder.setContentTitle( corona.title );
        builder.setContentText( corona.text );
        builder.setContentInfo( corona.content );

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

}