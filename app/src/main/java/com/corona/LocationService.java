package com.corona;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

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
import java.util.ArrayList;

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
    static final int NOTIFICATION_ID = 100;

    static final String CHANNEL_ID = BuildConfig.APPLICATION_ID.concat("_notification_id");
    static final String CHANNEL_NAME = BuildConfig.APPLICATION_ID.concat("_notification_name");

    private void showNotificationAndStartForegroundService() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final NotificationCompat.Builder builder = this.createServiceNotificationBuilder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_NONE;
            assert notificationManager != null;
            NotificationChannel mChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (mChannel == null) {
                mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
                notificationManager.createNotificationChannel(mChannel);
            }
        }

        this.startForeground( NOTIFICATION_ID, builder.build());
    }
    // -- showNotificationAndStartForegroundService

    private NotificationCompat.Builder createServiceNotificationBuilder() {

        ArrayList<Corona> coronaList = dbHelper.getCoronaListInfected( 0, 1 );
        int infectedCountCnt = coronaList.size() ;
        Corona corona = 0 < infectedCountCnt ? coronaList.get( 0 ) : null ;

        String serviceName = getString(R.string.location_service_name);

        if( 0 < infectedCountCnt ) {
            serviceName = String.format( "코로나 확진자 동선 겹침 [%d] 건", infectedCountCnt );
        }

        String contentText = "핸드폰 위치와 확진자의 동선을 스캔중입니다.";
        if( 1 == infectedCountCnt ) {
            contentText = String.format( "%s에서 확진자[%s]와 동선이 겹쳤습니다.", corona.place, corona.patient );
        }else if( 1 < infectedCountCnt ) {
            contentText = String.format( "%s에서 확진자[%s]와 동선이 겹쳤습니다. 총 %d 건.", corona.place, corona.patient, infectedCountCnt );
        } else if( this.gpsInsCnt > 0 ) {
            contentText = String.format("%s [ %d ] [ %d ]", contentText, this.gpsInsCnt, this.coronaDbRecSuccCnt);
        }

        // Create an Intent for the activity you want to start
        Intent intent = new Intent(this, Activity_010_Splash.class);
        Bundle bundle = new Bundle();
        if( null != corona ) {
            bundle.putSerializable( corona_from_notification_click , corona );
        }
        intent.putExtras( bundle ) ;

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentIntent(resultPendingIntent);
        if( 0 == infectedCountCnt ) {
            builder.setSmallIcon(R.mipmap.ic_launcher);
        } else {
            builder.setSmallIcon( R.drawable.corona_alarm );
        }
        builder.setContentTitle(serviceName);
        builder.setContentText(contentText);

        if( 0 < infectedCountCnt ) {
            this.playNotificationSound();
        }

        return builder;
    }

    private void updateServiceNotificationTitleAndText() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder = this.createServiceNotificationBuilder();

        notificationManager.notify( NOTIFICATION_ID, builder.build());
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

        this.updateServiceNotificationTitleAndText();

    }
    // -- whenLocationUpdated

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

            if( true ) {
                String whereClause = " visit_tm < ? ";
                String[] args = {"" + two_weeks_ags};

                int updCnt = db.delete("gps", whereClause, args);

                Log.d(TAG, "gps two weeks ago del cnt = " + updCnt);
            }

            if( true ) {
                String whereClause = " visit_to < ? ";
                String[] args = {"" + two_weeks_ags};

                int updCnt = db.delete("corona", whereClause, args);

                Log.d(TAG, "corona two weeks ago del cnt = " + updCnt);
            }

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
                getCoronaDataFromServerImpl();
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            gettingCoronaDataFromServer = false ;
        }
    }
    // getCoronaDataFromServer

    private int coronaDbHandlerCnt = 0;
    protected RequestQueue requestQueue ;

    private void getCoronaDataFromServerImpl() {
        Log.d(TAG, String.format("Corona DbHandler[%d]:", coronaDbHandlerCnt));

        String url = SERVER_URL_HEADER + "/corona/data.json";

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
                        updateServiceNotificationTitleAndText();
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Response: Error Message " + error.getMessage());
                        Log.d(TAG, "Response: Error " + error.toString());

                        dbHelper.checkCoronaInfection();
                        showCoronaInfectionAlarmNotifications( );
                        updateServiceNotificationTitleAndText();
                    }
                }
        );

        // Access the RequestQueue through your singleton class.
        coronaDbHandlerCnt ++ ;

        this.requestQueue.add( jsonObjectRequest );
    }
    // -- getCoronaDataFromServerImpl

    private void showCoronaInfectionAlarmNotifications() {
        ArrayList<Corona> coronaList = this.dbHelper.getCoronaListInfected( 0 ) ;

        Log.d( TAG, "corona alarm count = " + coronaList.size() );

        for( Corona corona : coronaList ) {
            this.showColonaDetectionAlarmNotificationImpl(corona);

            this.dbHelper.updateCoronaNotification( corona, 1 );
        }

        boolean showAlertDialog = false ;
        if( showAlertDialog && 0 < coronaList.size() ) {
            this.showCoronaInfectionAlarmDialog( coronaList );
        }
    }

    private void showCoronaInfectionAlarmDialog( ArrayList<Corona> coronaList ) {
        final Corona corona = coronaList.get( 0 );
        String title = "코로나 알림";
        String message = "%s에서 코로나 확진다 동선 겹침이 발행하였습니다." ;
        int cnt = coronaList.size();
        if( 1 == cnt ) {
            message = "%s에서 코로나 확진다 동선 겹침이 발행하였습니다." ;
            message = String.format( message, corona.place ) ;
        } else {
            message = "%s외 %d건의 코로나 확진다 동선 겹침이 발행하였습니다." ;
            message = String.format( message, corona.place, cnt ) ;
        }

        AlertDialog alertDialog = new AlertDialog.Builder( this )
                .setTitle( title )
                .setMessage( message )
                .create();

        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL );
        alertDialog.setButton(Dialog.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                whenColonaAlarmDialogClicked( corona );
            }
        });
        alertDialog.setButton(Dialog.BUTTON_NEGATIVE,"Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.hide();
            }
        });

        alertDialog.show();
    }
    // -- showCoronaInfectionAlarmDialog

    private void whenColonaAlarmDialogClicked(Corona corona ) {
        // Create an Intent for the activity you want to start
        Intent intent = new Intent(this, Activity_010_Splash.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Bundle bundle = new Bundle();
        bundle.putSerializable( corona_from_notification_click , corona ) ;
        intent.putExtras(bundle);

        startActivity( intent );
    }
    // -- whenColonaAlarmDialogClicked

    private int coronaDbRecSuccCnt = 0 ;
    private void whenCoronaDbReceived(JSONArray response) {
        try {
            if( 0 < response.length() ) {
                coronaDbRecSuccCnt ++ ;

                this.dbHelper.whenCoronaDbReceived( response );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    int coronaDetectionAlarmNotificationId = 10_000 ;

    private void showColonaDetectionAlarmNotificationImpl( Corona corona ) {
        if( coronaDetectionAlarmNotificationId < 10_000 ) {
            coronaDetectionAlarmNotificationId = 10_000;
        }

        Log.d( TAG, "corona alarm id = " + corona.id );

        // Create an Intent for the activity you want to start
        Intent intent = new Intent(this, Activity_010_Splash.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Bundle bundle = new Bundle();
        bundle.putSerializable( corona_from_notification_click , corona ) ;
        intent.putExtras(bundle);

        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        String CHANNEL_ID = "999";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setContentIntent(resultPendingIntent);
        builder.setSmallIcon( R.drawable.corona_alarm );
        builder.setContentTitle( corona.getTitle() );
        builder.setContentText( corona.text );
        builder.setContentInfo( corona.content );
        builder.setPriority(Notification.PRIORITY_DEFAULT);
        builder.setAutoCancel( true );
        //Uri Emergency_sound_uri=Uri.parse("android.resource://"+getPackageName()+"/raw/emergency_sound");
        //builder.setSound( Emergency_sound_uri );
        builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        builder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
        builder.setDefaults(Notification.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(coronaDetectionAlarmNotificationId, builder.build());

        // this.playNotificationSound();

        coronaDetectionAlarmNotificationId ++;
    }

    public void playNotificationSound() {
        try {
            Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                    + "://" + this.getApplicationContext().getPackageName() + "/raw/notification");
            Ringtone r = RingtoneManager.getRingtone(this.getApplicationContext(), alarmSound);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}