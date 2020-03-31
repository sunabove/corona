package com.corona;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.annotation.NonNull;

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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

    private DbHelper dbHelper;
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

        this.dbHelper = DbHelper.getLocationDbHelper(this.getApplicationContext() );

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
    }

    /*
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

    }*/

    private boolean showingCoronaMarkerDb = false ;

    private void showCoronaMarkerFromDb( ) {
        showCoronaMarkerFromDb( -1 );
    }

    private void showCoronaMarkerFromDb( long id ) {
        try {
            if( this.showingCoronaMarkerDb ) {
                return ;
            }

            this.showingCoronaMarkerDb = true;

            showCoronaMarkerFromDbImpl2( id );
        } catch( Exception e ) {
            e.printStackTrace();
        } finally {
            this.showingCoronaMarkerDb = false ;
        }
    }

    private void showCoronaMarkerFromDbImpl2(final long spec_id) {

        DbHelper dbHelper = this.dbHelper;

        SQLiteDatabase db = dbHelper.wdb;

        long coronaMaxUpDt = this.coronaMaxUpDt;

        String sql = "" ;
        sql += " SELECT id, deleted, checked, notification, up_dt, place, patient, visit_fr, visit_to " ;
        sql += " , latitude, longitude " ;
        sql += " FROM corona " ;
        sql += " WHERE up_dt >= ? or id = ? " ;
        sql += " ORDER BY up_dt ASC " ;
        ;

        String[] args = { "" + coronaMaxUpDt , "" + spec_id };
        Cursor cursor = db.rawQuery(sql, args);

        long id;
        long deleted, checked, notification ;
        long up_dt;
        String place;
        String patient;
        long visit_fr;
        long visit_to;
        float latitude = 0;
        float longitude = 0 ;
        String title, snippet, info ;
        String up_dt_str ;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        HashMap<Long, Marker> coronaMarkers = this.coronaMarkers;
        GoogleMap googleMap = this.googleMap;
        ArrayList<Long> deletedIds = new ArrayList<>();
        final long now = System.currentTimeMillis();

        while ( this.isActivityAlive() && cursor.moveToNext()) {
            id = cursor.getLong(cursor.getColumnIndex("id"));

            deleted = cursor.getLong(cursor.getColumnIndex("deleted"));
            checked = cursor.getLong(cursor.getColumnIndex("checked"));
            notification = cursor.getLong(cursor.getColumnIndex("notification"));

            up_dt = cursor.getLong(cursor.getColumnIndex("up_dt"));
            place = cursor.getString(cursor.getColumnIndex("place"));
            patient = cursor.getString(cursor.getColumnIndex("patient"));

            visit_fr = cursor.getLong(cursor.getColumnIndex("visit_fr"));
            visit_to = cursor.getLong(cursor.getColumnIndex("visit_to"));

            latitude = cursor.getFloat(cursor.getColumnIndex("latitude"));
            longitude = cursor.getFloat(cursor.getColumnIndex("longitude"));

            String infection = 1 == checked ? "동선 겹침" : "" ;

            title = String.format("[%d] %s / %s / %s", id, place, patient , infection );
            snippet = String.format( "%s ~ %s", df.format( new Date( visit_fr) ) , df.format( new Date( visit_to ) ) );

            up_dt_str = df.format( new Date( up_dt ) ) ;

            info = String.format("corona marker deleted = %d, checked = %d, notification = %d, title = %s, snippet = %s, latitude = %f, longitude = %f, up_dt = %s",
                    deleted, checked, notification, title, snippet, latitude, longitude, up_dt_str ) ;
            Log.d( TAG, info );

            int rscId = R.drawable.map_dot_cyan_64 ; // old data
            if( 1 == checked ) { // checked data
                if( notification < 2 ) { // when notified
                    rscId = R.drawable.map_dot_red_64 ;
                } else { // when notification accepted
                    rscId = R.drawable.map_dot_pink_64 ;
                }

            } else if( Math.abs( now - up_dt ) < 20*ComInterface.CORONA_DB_GET_INTERVAL ) {
                // latest data
                rscId = R.drawable.map_dot_yellow_64 ;
            }

            LatLng latLng = new LatLng( latitude, longitude );
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title( title );
            markerOptions.snippet( snippet );
            markerOptions.flat(true);
            markerOptions.icon(BitmapDescriptorFactory.fromResource( rscId ));
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
                String tag = String.format("%d:%d", id, notification);
                marker.setTag( tag );
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

    private void whenMarkerClicked( Marker marker ) {
        Object obj = marker.getTag();
        String id = null ;
        String notification = null ;
        if( obj instanceof String ) {
            String tag = (String) obj;
            if( 0 < tag.length() ) {
                String [] infos = tag.split( ":" );
                if( null != infos && 1 < infos.length ) {
                    id = infos[0];
                    notification = infos[1];
                }
            }
        }

        if( null == id || null == notification  || "2".equals( notification )) {
            return;
        }

        DbHelper dbHelper = this.dbHelper;

        SQLiteDatabase db = dbHelper.wdb;
        String table = "corona";

        ContentValues values = new ContentValues();
        values.put( "notification", 2 );

        String whereClause = " id = ? ";
        String [] args = { id };

        int updCnt = db.update( table, values, whereClause, args );

        Log.d( TAG, String.format("Corona marker [%s] notification update cnt = %d", id, updCnt ) );

        this.showCoronaMarkerFromDb( Long.valueOf( id ).longValue() );
    }
    // whenMarkerClicked

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        final ComActivity activity = this;
        if(requestCode == PERMISSION_ID ){
            if( grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED ) {
                this.whenPermissionGranted();
            } else {
                String info = "앱을 다시 실행하여 권한을 부여하여 주세요.";
                status.setText( info);
                Toast.makeText( activity, info, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void whenPermissionGranted() {
        this.onMapReady( this.googleMap );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        Float lat = sharedPref.getFloat("lastPhoneLat", 37.5866f );
        Float lng = sharedPref.getFloat("lastPhoneLng", 126.97f );

        LatLng latlng = new LatLng(lat, lng);

        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));

        status.setText( "지도가 로드되었습니다.");

        boolean valid = checkPermissions();

        if( ! valid ) {
            requestPermissions();

            return ;
        } else {
            valid = valid && isLocationEnabled();

            if( ! valid ) {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG  ).show();
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

                    whenLocationUpdate( locationResult );
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

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                whenMarkerClicked( marker );
                return false;
            }
        });

        if( valid ) {
            this.getPhoneLastLocation();
        }
    }

    private void whenLocationUpdate( LocationResult locationResult ) {
        Log.d(TAG, String.format("locationResult update[%d]: %s", gpsUpdCnt, locationResult));

        lastLocationResult = locationResult ;

        animateGpsLogoRotate();

        showCoronaMarkerFromDb();

        showLastGpsData( locationResult );

        gpsUpdCnt ++;
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
        this.showGpsLogFromDb();

        this.showLastGpsData( this.lastLocationResult );

    }

    private void whenMapClick(LatLng latLng) {
        Log.d( TAG, "onMapClick. " );
    }

    private boolean isMapDetail() {
        float zoom = this.getZoom();
        return ( zoom > 17.0 );
    }

    private void showGpsLogFromDb() {
        DbHelper dbHelper = this.dbHelper;

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
    // showGpsLogFromDb

    private void setHighlightedDays() {
        List<Calendar> dates = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set( Calendar.DAY_OF_MONTH, 22 );

        dates.add( cal );
        CalendarView calendar = this.calendar;
    }

    private void showLastGpsData(LocationResult locationResult ) {
        if (null == locationResult) {
            return;
        }

        Location location = locationResult.getLastLocation();

        this.showLastGpsData( location );
    }

    private void showLastGpsData(Location location) {

        if( null == location ) {
            return;
        }

        boolean isMapDetail = this.isMapDetail();

        LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );

        // 최신 위치 저장
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat("lastPhoneLat", (float) latLng.latitude);
        editor.putFloat("lastPhoneLng", (float) latLng.longitude);
        editor.commit();
        // -- 최신 위치 저장

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

        this.showCurrentPositionMarker( location );
    }

    private void showCurrentPositionMarker( Location location ) {
        float zoom = this.getZoom();
        boolean isMapDetail = this.isMapDetail();

        if( null != phoneMarker) {
            phoneMarker.remove();
            this.phoneMarker = null;
        }
        phoneMarkerUpdCnt += 1 ;

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

    // 핸드폰의 최근 위치를 반환한다.
    @SuppressLint("MissingPermission")
    private void getPhoneLastLocation(){

        fusedLocationClient.getLastLocation().addOnCompleteListener(
            new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    if( null != location ) {
                        showLastGpsData( location );
                    }
                }
            }
        );
    }

}
