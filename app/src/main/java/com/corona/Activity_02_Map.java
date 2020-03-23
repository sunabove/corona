package com.corona;

import android.annotation.SuppressLint;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Activity_02_Map extends ComActivity implements OnMapReadyCallback {

    private static String TAG = "sun_above map" ;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private Marker myPhoneMarker;
    private Marker currCarMarker;
    private int currMarkerUpdCnt = 0 ;
    private Polyline gpsPathPoly = null ;
    private Polyline gpsLogPathPoly = null ;
    private GpsLog gpsLog = new GpsLog();
    private LatLng lastGpsLatLng ;

    private EditText status ;
    private TextView mapInfo ;
    private ImageView gpsLogo;

    private LocationResult lastLocationResult ;
    private float lastZoom ;

    protected FloatingActionButton showCalendar ;
    private CalendarView calendar ;
    private LinearLayout togglePane;
    private ImageButton hidePaneBtn ;

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
    }

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
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LOCATION_REQUEST_PRIORITY);
            locationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
            locationRequest.setFastestInterval(LOCATION_REQUEST_INTERVAL);
            locationRequest.setSmallestDisplacement(LOCATION_REQUEST_DISPLACEMENT);

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
    }

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

        Log.d( TAG, "onMapClick");
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
        long dd = now.get(Calendar.DAY_OF_MONTH);

        String sql = "SELECT id, yyyy, mm, dd, hh, mi, ss, zz, latitude, longitude FROM gps ";
        sql += " WHERE yyyy = ? AND mm = ? AND dd = ? " ;
        sql += " ORDER BY yyyy ASC, mm ASC, dd ASC, hh ASC, mi ASC, ss ASC, zz ASC ";

        String[] args = { "" + yyyy, "" + mm, "" + dd };
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

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex("id"));
            double latitude = cursor.getFloat(cursor.getColumnIndex("latitude"));
            double longitude = cursor.getFloat(cursor.getColumnIndex("longitude"));

            yyyy = cursor.getLong(cursor.getColumnIndex("yyyy"));
            mm = cursor.getLong(cursor.getColumnIndex("mm"));
            dd = cursor.getLong(cursor.getColumnIndex("dd"));

            long hh = cursor.getLong(cursor.getColumnIndex("hh"));
            long mi = cursor.getLong(cursor.getColumnIndex("mi"));
            long ss = cursor.getLong(cursor.getColumnIndex("ss"));

            long zz = cursor.getLong(cursor.getColumnIndex("zz"));

            String dateTime = "%04d-%02d-%02d %02d:%02d:%02d %d";
            dateTime = String.format(dateTime, yyyy, mm, dd, hh, mi, ss, zz);

            String info = "id = %d, lon = %f, lat = %f, upd = %s ";
            info = String.format(info, id, longitude, latitude, dateTime);
            Log.d(TAG, info);

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

        float zoom = this.getZoom();
        boolean isMapDetail = this.isMapDetail();

        if( true ) {
            Location location = locationResult.getLastLocation();
            LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );
            GpsLog gpsLog = this.gpsLog ;

            if( null == lastGpsLatLng ) {
                lastGpsLatLng = latLng;

                gpsLog.add( latLng );
            } else if( null != lastGpsLatLng ){
                float dists [] = getDistance( lastGpsLatLng, latLng );
                float dist = dists[ 0 ];

                Log.d( TAG , String.format("dist = %f", dist ) );

                if( 0.1f > dist ) {
                    Log.d( TAG , String.format("dist is small = %f", dist ) );
                    if( 0 < gpsLog.size() ) {
                        gpsLog.remove(gpsLog.size() - 1);
                    }
                    gpsLog.add( latLng );
                } else {
                    gpsLog.add( latLng );
                }

                lastGpsLatLng = latLng;
            }

            if( 1_000 < gpsLog.size() ) {
                while( 1_000 < gpsLog.size() ) {
                    gpsLog.remove( 0 );
                }
            }

            int color = Color.BLUE ;
            int width = isMapDetail ? 30: 15 ;

            PolylineOptions polyOptions = new PolylineOptions().width( width ).color( color ).geodesic(true);
            polyOptions.jointType(JointType.ROUND);

            //List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(20), new Gap(10));
            List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(10));
            polyOptions.pattern( pattern );

            for( LatLng log : gpsLog ) {
                polyOptions.add( log );
            }

            if( null != gpsPathPoly ) {
                gpsPathPoly.remove();
            }

            gpsPathPoly = googleMap.addPolyline( polyOptions );
        }

        if( true ){
            if( null != currCarMarker ) {
                currCarMarker.remove();
            }
            currMarkerUpdCnt += 1 ;

            Location location = locationResult.getLastLocation();
            LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(String.format("현재 위치 [%04d]", currMarkerUpdCnt ));

            currCarMarker = googleMap.addMarker(markerOptions);

            if( isMapDetail ) {
                currCarMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.smart_phone_icon_02_64));
            } else {
                currCarMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.smart_phone_icon_01_32));
            }

            double gpsHeading = gpsLog.getGpsHeading( 0 );

            currCarMarker.setRotation( (float) gpsHeading );

            Log.d( "heading" , "heading = " + gpsHeading );

            //currCarMarker.showInfoWindow();

            Projection projection = googleMap.getProjection();
            Point scrPos = projection.toScreenLocation(currCarMarker.getPosition());

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
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            }

            if( 0.35 < xr || 0.4 < yr ) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
            // --animate camera when current marker is out of screen
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
                        if (null != myPhoneMarker) {
                            myPhoneMarker.remove();
                        }

                        upCnt ++ ;

                        LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );

                        MarkerOptions options = new MarkerOptions();
                        options.position(latLng).title(String.format("현재 나의 위치 (%d)", upCnt));

                        myPhoneMarker = googleMap.addMarker(options);
                        myPhoneMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.smart_phone_icon_01_32));

                        myPhoneMarker.showInfoWindow();

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
