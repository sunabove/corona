package com.corona;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Activity_02_Map extends ComActivity implements OnMapReadyCallback {

    private static String TAG = "sun_above map" ;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private Marker phoneMarker;
    private int phoneMarkerUpdCnt = 0 ;
    private Polyline gpsPathPoly = null ;
    private Polyline gpsLogPathPoly = null ;
    private GpsLog gpsLog = new GpsLog();
    private LatLng lastGpsLatLng ;

    private long coronaMaxUpDt = -1 ;
    private int coronaMarkerZIndex = 1;
    private HashMap<Long, Marker> coronaMarkers = new HashMap<>();
    private Handler coronaDbShowHandler ;

    private EditText status ;
    private TextView mapInfo ;
    private ImageView gpsLogo;

    private LocationResult lastLocationResult ;
    private float lastZoom ;

    protected FloatingActionButton showCalendar ;
    private CalendarView calendar ;
    private LinearLayout togglePane;
    private ImageButton hidePaneBtn ;

    private Proj projection = Proj.projection();

    @Override
    public final int getLayoutId() {
        return R.layout.activity_02_map;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.hideActionBar();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.status = this.findViewById(R.id.status);
        this.mapInfo = this.findViewById(R.id.mapInfo);
        this.gpsLogo = this.findViewById(R.id.gpsLogo);
        this.showCalendar = this.findViewById(R.id.showCalendar);
        this.calendar = this.findViewById(R.id.calendar);
        this.togglePane = this.findViewById(R.id.togglePane) ;
        this.hidePaneBtn = this.findViewById(R.id.hidePaneBtn);

        this.hidePaneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whenShowCalendarClicked();
            }
        });

        this.togglePane.setVisibility(View.INVISIBLE);

        this.status.setText("");

        // hide keyboard always
        this.status.setInputType(InputType.TYPE_NULL);

        this.showCalendar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                whenShowCalendarClicked();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v( TAG, "onResume");

        this.hideActionBar();

        this.startCoronaMarkerDbShowHandler();
    }

    private void startCoronaMarkerDbShowHandler() {
        if( null == this.googleMap ) {
            return ;
        }

        if( null != this.coronaDbShowHandler ) {
            return ;
        }

        this.coronaDbShowHandler = new Handler();

        this.coronaDbShowHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if( ! isActivityAlive() ) {
                    Log.d(TAG, "Running startCoronaDbShowHandler activity is not alive.");

                    return ;
                }

                Log.d(TAG, "Running startCoronaDbShowHandler");

                startCoronaMarkerDbShowImpl();

                if( isActivityAlive() ) {
                    final long delay = coronaMarkers.size() < 1 ? 5_000 : ComInterface.CORONA_DB_GET_INTERVAL ;
                    coronaDbShowHandler.postDelayed(this, delay );
                }
            }
        }, 1_000);

    }

    private void startCoronaMarkerDbShowImpl() {
        LocationDbHelper dbHelper = LocationDbHelper.getLocationDbHelper(context);

        SQLiteDatabase db = dbHelper.wdb;

        long coronaMaxUpDt = this.coronaMaxUpDt;

        String sql = "" ;
        sql += " SELECT id, deleted, up_dt, place, patient, visit_fr, visit_to " ;
        sql += " , latitude, longitude " ;
        sql += " FROM corona " ;
        sql += " WHERE up_dt > ? " ;
        sql += " ORDER BY up_dt ASC" ;
        ;

        String[] args = { "" + coronaMaxUpDt };
        Cursor cursor = db.rawQuery(sql, args);

        long id;
        long deleted;
        long up_dt;
        String place;
        String patient;
        long visit_fr;
        long visit_to;
        float latitude = 0;
        float longitude = 0 ;

        HashMap<Long, Marker> coronaMarkers = this.coronaMarkers;
        GoogleMap googleMap = this.googleMap;
        ArrayList<Long> deletedIds = new ArrayList<>();

        while ( this.isActivityAlive() && cursor.moveToNext()) {
            id = cursor.getLong(cursor.getColumnIndex("id"));
            deleted = cursor.getLong(cursor.getColumnIndex("deleted"));
            up_dt = cursor.getLong(cursor.getColumnIndex("up_dt"));
            place = cursor.getString(cursor.getColumnIndex("place"));
            patient = cursor.getString(cursor.getColumnIndex("patient"));
            visit_fr = cursor.getLong(cursor.getColumnIndex("visit_fr"));
            visit_to = cursor.getLong(cursor.getColumnIndex("visit_to"));
            latitude = cursor.getFloat(cursor.getColumnIndex("latitude"));
            longitude = cursor.getFloat(cursor.getColumnIndex("longitude"));

            SimpleDateFormat df = ComInterface.yyyMMdd_HHmmSS ;

            String title = String.format("[%d] %s / %s", id, place, patient );
            String snippet = String.format( "%s ~ %s", df.format( new Date( visit_fr) ) , df.format( new Date( visit_to ) ) );

            String info = String.format("corona marker title = %s, latitude = %f, longitude = %f", title, latitude, longitude ) ;
            Log.d( TAG, info );

            LatLng latLng = new LatLng( latitude, longitude );
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title( title );
            markerOptions.snippet( snippet );
            markerOptions.flat(true);
            markerOptions.icon(BitmapDescriptorFactory.fromResource( R.drawable.map_dot_yellow_64 ));
            markerOptions.zIndex( coronaMarkerZIndex );

            if( 1 == deleted ) {
                deletedIds.add( id );
                Marker marker = coronaMarkers.get( id );
                marker.remove();
                coronaMarkers.remove( id );
            } else {

                if (!this.isActivityAlive()) {
                    Log.d(TAG, "Activity is not alive skipped to add corona marker.");
                    break;
                }

                Marker marker = googleMap.addMarker(markerOptions);
                coronaMarkers.put(id, marker);
            }

            if( up_dt > this.coronaMaxUpDt ) {
                this.coronaMaxUpDt = up_dt ;
            }

            coronaMarkerZIndex ++ ;
        }

        cursor.close();

        for( Long delId : deletedIds ) {
            db.delete("corona", " WHERE ID = ?", new String[]{ "" + delId });
        }

    }
    // -- startCoronaDbShowImpl

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Add a marker in Sydney and move the camera
        if( false ) {
            this.googleMap.addMarker(new MarkerOptions().position(new LatLng(37.5866, 126.97)).title("청와대"));
        }

        Float lat = sharedPref.getFloat("lastPhoneLat", 37.5866f );
        Float lng = sharedPref.getFloat("lastPhoneLng", 126.97f );

        LatLng latlng = new LatLng(lat, lng);

        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));

        status.setText( "지도가 로드되었습니다.");

        boolean valid = checkPermissions();

        if( ! valid ) {
            requestPermissions();
        } else {
            valid = valid && isLocationEnabled();

            if( ! valid ) {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        }

        if( valid ) { // location updater
            LocationRequest locationRequest = LocationService.createLocationRequest();

            LocationCallback locationCallback = new LocationCallback() {

                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    Log.d(TAG, String.format("locationResult update[%d]: %s", gpsUpdCnt, locationResult));

                    lastLocationResult = locationResult ;

                    animateGpsLogoRotate();

                    showLastGpsData( locationResult );

                    gpsUpdCnt ++;
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
               whenMapClick( latLng );
            }
        });

        googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {

            @Override
            public void onCameraIdle() {
                whenCameraIdle();
            }
        });

        googleMap.setOnCameraMoveStartedListener( new GoogleMap.OnCameraMoveStartedListener() {

            @Override
            public void onCameraMoveStarted(int reasonCode) {
                if (reasonCode == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    // I will no longer keep updating the camera location because
                    // the user interacted with it. This is my field I check before
                    // snapping the camera location to the latest value.
                    lastMapMoveTime = System.currentTimeMillis();

                    Log.d( TAG, "onCameraMoveStarted. lastMapMoveTime = " + lastMapMoveTime);
                }
            }
        });

        this.startCoronaMarkerDbShowHandler();
    }

    private long lastMapMoveTime = 0 ;

    private void whenShowCalendarClicked() {
        if( View.INVISIBLE == togglePane.getVisibility() ) {
            this.setHighlightedDays();
        }

        togglePane.setVisibility( togglePane.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE );
    }

    int gpsUpdCnt = 0;

    private void whenCameraIdle() {
        if( this.isLocationEnabled() ) {
            this.gpsLogo.setImageResource(R.drawable.gps_recording_02);
        } else {
            this.gpsLogo.setImageResource(R.drawable.gps_recording_00);
        }

        TextView mapInfo = this.mapInfo;
        float zoom = this.getZoom();

        String info = "Zoom: %02.1f,  GPS: %d ";
        info = String.format(info, zoom, gpsUpdCnt );

        mapInfo.setText( info );

        if( zoom != this.lastZoom ) {
            whenCameraZoomChanged();
        }

        this.lastZoom = zoom ;
    }

    private float getZoom() {
        float zoom = googleMap.getCameraPosition().zoom ;

        return zoom;
    }

    private void whenCameraZoomChanged() {
        this.showGpsDb();

        this.showLastGpsData( this.lastLocationResult );

    }

    private void whenMapClick(LatLng latLng) {
        Log.d( TAG, "onMapClick. " );
    }

    private boolean isMapDetail() {
        float zoom = this.getZoom();
        return ( zoom > 17.0 );
    }

    private void showGpsDb() {
        LocationDbHelper dbHelper = LocationDbHelper.getLocationDbHelper(this.getApplicationContext() );

        SQLiteDatabase db = dbHelper.rdb;
        Calendar now = Calendar.getInstance();

        long yyyy = now.get(Calendar.YEAR);
        long mm = now.get(Calendar.MONTH) + 1; // Note: zero based!
        long dd = now.get(Calendar.DAY_OF_MONTH) - 2 ; // condition for yesterday
        long visit_tm = now.getTimeInMillis() - 24*60*60*1_000; // condition for yesterday

        String sql = "SELECT id, latitude, longitude, visit_tm FROM gps ";
        sql += " WHERE yyyy = ? AND mm = ? AND dd > ? AND visit_tm > ? " ;
        sql += " ORDER BY visit_tm ASC ";

        String[] args = { "" + yyyy, "" + mm, "" + dd, "" + visit_tm };
        Cursor cursor = db.rawQuery(sql, args);

        boolean  isMapDetail = this.isMapDetail() ;
        int color = Color.GRAY ;
        int width = isMapDetail ? 30: 15 ;

        PolylineOptions polyOptions = new PolylineOptions().width( width ).color( color ).geodesic(true);
        polyOptions.jointType(JointType.ROUND);

        //List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(20), new Gap(10));
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(10));
        polyOptions.pattern( pattern );

        int cnt = 0 ;
        long id;
        double latitude, longitude;
        String dateTime;
        SimpleDateFormat df = ComInterface.yyyMMdd_HHmmSS;

        while (cursor.moveToNext()) {
            id = cursor.getLong(cursor.getColumnIndex("id"));
            latitude = cursor.getFloat(cursor.getColumnIndex("latitude"));
            longitude = cursor.getFloat(cursor.getColumnIndex("longitude"));

            visit_tm = cursor.getLong(cursor.getColumnIndex("visit_tm"));

            dateTime = df.format( new Date( visit_tm ) ) ;

            String info = "Gps Log on DB: id = %d, lon = %f, lat = %f, visit_tm = %s ";
            info = String.format(info, id, longitude, latitude, dateTime);
            //Log.d(TAG, info);

            LatLng latLng = new LatLng( latitude, longitude );
            polyOptions.add( latLng );

            cnt ++ ;
        }
        cursor.close();

        if( cnt > 2 ) {
            if (null != gpsLogPathPoly) {
                gpsLogPathPoly.remove();
            }

            gpsLogPathPoly = googleMap.addPolyline(polyOptions);
        }

    }

    private void setHighlightedDays() {
        List<Calendar> dates = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set( Calendar.DAY_OF_MONTH, 22 );

        dates.add( cal );
        CalendarView calendar = this.calendar;
    }

    private void showLastGpsData(LocationResult locationResult ) {

        if( null == locationResult ) {
            return;
        }

        boolean isMapDetail = this.isMapDetail();

        Location location = locationResult.getLastLocation();
        LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );
        GpsLog gpsLog = this.gpsLog ;

        if( 100_000 < gpsLog.size() ) {
            while( 100_000 < gpsLog.size() ) {
                gpsLog.remove( 0 );
            }
        }

        gpsLog.add( latLng );

        lastGpsLatLng = latLng;

        int color = Color.BLUE ;
        int width = isMapDetail ? 30: 15 ;

        PolylineOptions polyOptions = new PolylineOptions().width( width ).color( color ).geodesic(true);
        polyOptions.jointType(JointType.ROUND);

        //List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(20), new Gap(10));
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(10));
        polyOptions.pattern( pattern );

        int insCnt = 0 ;
        for( LatLng log : gpsLog ) {
            polyOptions.add( log );
            insCnt += 1;
        }

        while( 3 > insCnt ) {
            polyOptions.add( latLng );
            insCnt += 1;
        }

        if( null != gpsPathPoly ) {
            gpsPathPoly.remove();
        }

        gpsPathPoly = googleMap.addPolyline( polyOptions );
        gpsPathPoly.setZIndex( 4 );

        this.showCurrentPositionMarker( locationResult );
    }

    private void showCurrentPositionMarker(LocationResult locationResult ) {
        float zoom = this.getZoom();
        boolean isMapDetail = this.isMapDetail();

        if( null != phoneMarker) {
            phoneMarker.remove();
            this.phoneMarker = null;
        }
        phoneMarkerUpdCnt += 1 ;

        Location location = locationResult.getLastLocation();
        LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(String.format("현재 위치 [%04d]", phoneMarkerUpdCnt));
        markerOptions.flat(true);

        int mapIconRscId = R.drawable.smart_phone_icon_01_64;
        if( zoom > 18.4 ) {
            mapIconRscId = R.drawable.smart_phone_icon_01_64;
        } else if( zoom > 13 ) {
            mapIconRscId = R.drawable.smart_phone_icon_02_32;
        } else {
            mapIconRscId = R.drawable.smart_phone_icon_03_16;
        }
        markerOptions.icon(BitmapDescriptorFactory.fromResource( mapIconRscId ));

        markerOptions.zIndex(1_000_000);

        phoneMarker = googleMap.addMarker(markerOptions);

        double gpsHeading = gpsLog.getGpsHeading( 0 );

        phoneMarker.setRotation( (float) gpsHeading );

        Log.d( "heading" , "heading = " + gpsHeading );

        //currCarMarker.showInfoWindow();

        Projection projection = googleMap.getProjection();
        Point scrPos = projection.toScreenLocation(phoneMarker.getPosition());

        // animate camera when current marker is out of screen
        double x = scrPos.x;
        double y = scrPos.y;

        int sw = getScreenWidth();
        int sh = getScreenHeight();

        double xr = Math.abs( sw/2.0 - x )/sw ;
        double yr = Math.abs( sh/2.0 - y )/sh ;

        if( false ) {
            Log.d("screen range", "xr = " + xr);
            Log.d("screen range", "yr = " + yr);
        }

        if( firstMove ) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, googleMap.getMaxZoomLevel() - 2));
            if( 0.35 < xr || 0.4 < yr ) {
                // animate camera when current marker is out of screen
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        } else {
            long now = System.currentTimeMillis();
            if( now - lastMapMoveTime < 6*1000 ) {
                Log.d( TAG, "Last click time is less than 6 seconds. Skipped moving map." );
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                if( 0.35 < xr || 0.4 < yr ) {
                    // animate camera when current marker is out of screen
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                }
            }
        }

        if( firstMove ) {
            firstMove = false ;
        }
    }

    private Animation gpsAnimation = null ;

    private void animateGpsLogoRotate() {
        if( null != this.gpsAnimation) {
            this.gpsLogo.clearAnimation();
        }

        this.gpsLogo.setImageResource(R.drawable.gps_recording_02 );

        int relative = Animation.RELATIVE_TO_SELF ;

        Animation animation = new RotateAnimation(
                30, -30,
                relative, 0.5f,
                relative,  0.5f);

        animation.setDuration( 2_500 );
        animation.setRepeatCount( 2 );

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                gpsLogo.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        this.gpsAnimation = animation ;

        this.gpsLogo.startAnimation( animation );
    }

    private boolean firstMove = true ;

    /*
    // 핸드폰의 최근 위치를 반환한다.
    @SuppressLint("MissingPermission")
    private void getPhoneLastLocation(){
        boolean valid = checkPermissions();

        if( ! valid ) {
            requestPermissions();
        } else {
            valid = valid && isLocationEnabled();

            if( ! valid ) {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        }

        if( ! valid ) {
            return ;
        }

        fusedLocationClient.getLastLocation().addOnCompleteListener(
            new OnCompleteListener<Location>() {
                int upCnt = 0 ;
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    if (location != null) {
                        if (null != phoneMarker) {
                            phoneMarker.remove();
                        }

                        upCnt ++ ;

                        LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );

                        MarkerOptions options = new MarkerOptions();
                        options.position(latLng).title(String.format("현재 나의 위치 (%d)", upCnt));

                        phoneMarker = googleMap.addMarker(options);
                        phoneMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.smart_phone_icon_02_32));

                        phoneMarker.showInfoWindow();

                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, googleMap.getMaxZoomLevel() - 2));

                        status.setText("지도를 핸드폰 현재 위치로 이동하였습니다.");

                        // 최신 위치 저장
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putFloat("lastPhoneLat", (float) latLng.latitude);
                        editor.putFloat("lastPhoneLng", (float) latLng.longitude);
                        editor.commit();
                        // -- 최신 위치 저장
                    }
                }
            }
        );
    }
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted. Start getting the location information
            }
        }
    }

}
