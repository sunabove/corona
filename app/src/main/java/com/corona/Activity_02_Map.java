package com.corona;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Lifecycle;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class Activity_02_Map extends ComActivity implements OnMapReadyCallback {

    private static String TAG = "sunabove map" ;

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;

    private Marker myPhoneMarker;
    private Marker currCarMarker;
    private int currMarkerUpdCnt = 0 ;
    private Polyline gpsPath = null ;
    private GpsLog gpsLog = new GpsLog();
    private LatLng lastGpsLatLng ;

    private EditText status ;

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

        this.motionEnabled = false;

        this.status.setText("");

        // hide keyboard always
        this.status.setInputType(InputType.TYPE_NULL);

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
        map = googleMap;

        // Add a marker in Sydney and move the camera
        if( false ) {
            map.addMarker(new MarkerOptions().position(new LatLng(37.5866, 126.97)).title("청와대"));
        }

        Float lat = sharedPref.getFloat("lastPhoneLat", 37.5866f );
        Float lng = sharedPref.getFloat("lastPhoneLng", 126.97f );

        LatLng latlng = new LatLng(lat, lng);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));

        status.setText( "지도가 로드되었습니다.");

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                status.setText( "현재 위치를 체크중입니다.");
                getPhoneLastLocation();
            }
        }, 1_000);

        if( true ) { // location updater
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(LOCATION_REQUEST_INTERVAL);
            locationRequest.setSmallestDisplacement(LOCATION_REQUEST_DISPLACEMENT);

            LocationCallback locationCallback = new LocationCallback() {
                int updCnt = 0;

                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    updCnt++;
                    Log.d(TAG, String.format("locationResult update[%d]: %s", updCnt, locationResult));

                    showGpsData( locationResult );
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

    }

    private void whenMapClick(LatLng latLng) {
        final String tag = "google map";

        Log.d( tag, "onMapClick");
    }

    private void showGpsData( LocationResult locationResult ) {

        if( null != currCarMarker ) {
            currCarMarker.remove();
        }

        if( null !=gpsPath ) {
            gpsPath.remove();

        }

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
            int width = 10 ;

            PolylineOptions polyOptions = new PolylineOptions().width( width ).color( color ).geodesic(true);

            List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(20), new Gap(10));
            polyOptions.pattern( pattern );

            for( LatLng log : gpsLog ) {
                polyOptions.add( log );
            }

            gpsPath = map.addPolyline( polyOptions );
        }

        if( true ){
            currMarkerUpdCnt += 1 ;

            Location location = locationResult.getLastLocation();
            LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(String.format("현재 위치 [%04d]", currMarkerUpdCnt ));

            currCarMarker = map.addMarker(markerOptions);

            currCarMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.car_map_icon_02));

            double heading = 0 ;

            if( 1 > gpsLog.size() ) {
                currCarMarker.setRotation( (float) heading );
            } else {
                double gpsHeading = gpsLog.getLatestHeading( heading );

                currCarMarker.setRotation( (float) gpsHeading );

                Log.d( "heading" , "heading = " + gpsHeading );
            }

            //currCarMarker.showInfoWindow();

            Projection projection = map.getProjection();
            Point scrPos = projection.toScreenLocation(currCarMarker.getPosition());

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

            if( 0.35 < xr || 0.4 < yr ) {
                map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }

        }
    }

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

                        myPhoneMarker = map.addMarker(options);
                        myPhoneMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.smart_phone_icon_01));

                        myPhoneMarker.showInfoWindow();

                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.getMaxZoomLevel() - 2));

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
