package com.corona;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams;
import androidx.lifecycle.Lifecycle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
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

public class Activity_03_Map extends ComActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;

    private Marker myPhoneMarker;
    private Marker currCarMarker;
    private int currMarkerUpdCnt = 0 ;
    private Polyline gpsPath = null ;
    private GpsLog gpsLog = new GpsLog();
    private LatLng lastGpsLatLng ;

    private WebView videoView ;
    private FloatingActionButton stop ;
    private FloatingActionButton autopilot ;
    private EditText status ;
    private EditText log ;

    private EditText pitch ;
    private EditText roll ;

    private ImageView carAni ;
    private Animation carAnimation = null ;

    private String currMotion = "";
    private long motionTime = System.currentTimeMillis();

    private int moveCnt = 0 ;
    private String motionCurr = "" ;

    private boolean videoFullWidth = false ;

    // auto pilot
    private Marker pathStart ;
    private Marker pathEnd ;
    private Polyline autoPilotPath ;
    private boolean isAutopilot = false;
    // -- auto pilot

    public int getLayoutId() {
        return R.layout.activity_maps;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.hideActionBar();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.videoView = this.findViewById(R.id.videoView);
        this.stop = this.findViewById(R.id.stop );
        this.status = this.findViewById(R.id.status);
        this.log = this.findViewById(R.id.log);

        this.autopilot = this.findViewById(R.id.autopilot);

        this.pitch = this.findViewById(R.id.pitch);
        this.roll = this.findViewById(R.id.roll);

        this.carAni = this.findViewById(R.id.carAni);

        this.motionEnabled = false ;

        this.stop.setEnabled( false );
        this.autopilot.setEnabled( false );

        this.status.setText( "" );

        // hide keyboard always
        this.status.setInputType(InputType.TYPE_NULL);

        //setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        Log.v( TAG, "onResume");

        this.hideActionBar();

        this.getCarLocationByHttp( 500 );

        this.isAutopilot = false ;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    private void whenMapClick(LatLng latLng) {
        final String tag = "google map";

        Log.d( tag, "onMapClick");

        if( ! isAutopilot ) {
            Log.d( tag, "Autopilot is not enabled." );
        } else if( isAutopilot ) {

            if (null != pathEnd) {
                pathEnd.remove();
            }

            if (null != autoPilotPath) {
                autoPilotPath.remove();
            }

            // clear gps log
            this.gpsLog = new GpsLog();

            if (null != gpsPath) {
                gpsPath.remove();
            }

            // 도착 지점 추가
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("목적지");

            pathEnd = map.addMarker(markerOptions);
            pathEnd.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.path_end));

            if (null != pathEnd) {
                pathEnd.hideInfoWindow();
            }

            pathEnd.showInfoWindow();
            // -- 도착 지점 추가

            // 출발지 -> 도착지 경로 표시

            if (null != pathStart) {
                List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(20), new Gap(10));
                //List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(20), new Dash(30), new Gap(20));

                PolylineOptions polyOptions = new PolylineOptions().width(20).color(Color.GREEN).geodesic(true);
                polyOptions.add(pathStart.getPosition());
                polyOptions.add(pathEnd.getPosition());
                polyOptions.pattern(pattern);

                autoPilotPath = map.addPolyline(polyOptions);
            }

            Toast.makeText(getApplicationContext(), "목적지가 설정되었습니다.", Toast.LENGTH_SHORT).show();

            // -- 출발지 -> 도착지 경로 표시
        }

    }

    // 차량의 최근 위치를 반환한다.
    private void getCarLocationByHttp( final long delay ) {
        final Handler handler = new Handler() ;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getCarLocationByHttpImpl( delay );
            }
        }, delay );
    }

    private int carLocCnt = 0 ;
    private void getCarLocationByHttpImpl( final long delay ) {
        String url = "http://10.3.141.1/send_me_curr_pos.json";
        final String tag = "car location" ;

        carLocCnt += 1;
        Log.d( tag, String.format("[%04d] Getting car location by http ...", carLocCnt ) );

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        setCarLocationByJsonString( response );

                        if( Activity_03_Map.this.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            getCarLocationByHttp( delay );
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();

                        Log.d( tag, "Error has occured" );

                        getCarLocationByHttp( 3*delay );
                    }
                });

        this.requestQueue.add( jsonObjectRequest );
    }

    private void setCarLocationByJsonString( Object obj ) {
        try {
            JSONObject response = null ;

            if( obj instanceof JSONObject) {
                response = (JSONObject) obj ;
            }

            Object test = null;

            try {
                test = response.get("latitude");
            } catch ( Exception e ) {
                test = null;
            }

            if( null == test || 1 > test.toString().trim().length() ) {
                Log.d( "sunabove", "There is no gps data.");
                return ;
            }

            boolean isAutopilot = this.isAutopilot;

            double latitude = Double.parseDouble(response.get("latitude").toString().trim());
            double longitude = Double.parseDouble(response.get("longitude").toString().trim());
            double heading = Double.parseDouble(response.get("heading").toString().trim());
            double altitude = Double.parseDouble(response.get("altitude").toString().trim());
            String timestamp = response.get( "timestamp" ).toString().trim();

            String text = "" ;
            text += String.format(  "Lat     :  %3.6f °", prettyAngle60( latitude ) );
            text += String.format("   Lon   : %3.6f °", prettyAngle60( longitude ) ) ;
            text += String.format("\nHead : %3.6f °", prettyAngle60( heading ) ) ;
            text += String.format("   Alt    :  %3.6f m", altitude ) ;

            log.setText( text );

            if( null != currCarMarker ) {
                currCarMarker.remove();
            }

            if( null !=gpsPath ) {
                gpsPath.remove();

            }

            final String tag = "car location" ;

            if( true ) {
                LatLng latLng = new LatLng(latitude, longitude);
                GpsLog gpsLog = Activity_03_Map.this.gpsLog ;

                if( null == lastGpsLatLng ) {
                    lastGpsLatLng = latLng;

                    gpsLog.add( latLng );
                } else if( null != lastGpsLatLng ){
                    float dists [] = getDistance( lastGpsLatLng, latLng );
                    float dist = dists[ 0 ];

                    Log.d( tag , String.format("dist = %f", dist ) );

                    if( 0.1f > dist ) {
                        Log.d( tag , String.format("dist is small = %f", dist ) );
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

                int color = isAutopilot ? Color.RED : Color.BLUE ;
                int width = isAutopilot ? 12 : 10 ;

                PolylineOptions polyOptions = new PolylineOptions().width( width ).color( color ).geodesic(true);

                if( isAutopilot ) {
                    List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(20), new Gap(10));
                    polyOptions.pattern( pattern );
                }

                for( LatLng log : gpsLog ) {
                    polyOptions.add( log );
                }

                gpsPath = map.addPolyline( polyOptions );
            }

            if( true ){
                currMarkerUpdCnt += 1 ;

                LatLng latLng = new LatLng(latitude, longitude);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(String.format("현재 차량 위치 [%04d]", currMarkerUpdCnt ));

                currCarMarker = map.addMarker(markerOptions);

                currCarMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.car_map_icon_02));

                if( 1 > gpsLog.size() ) {
                    currCarMarker.setRotation( (float) heading );
                } else {
                    double gpsHeading = gpsLog.getHeading( heading );

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

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    // 핸드폰의 최근 위치를 반환한다.
    @SuppressLint("MissingPermission")
    private void getPhoneLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location != null) {
                                    if( null != myPhoneMarker ) {
                                        myPhoneMarker.remove();
                                    }

                                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                                    MarkerOptions options = new MarkerOptions();
                                    options.position(latLng).title("현재 나의 위치") ;

                                    myPhoneMarker = map.addMarker(options);
                                    myPhoneMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.smart_phone_icon_01));

                                    myPhoneMarker.showInfoWindow();

                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.getMaxZoomLevel() - 2 ));

                                    status.setText( "지도를 핸드폰 현재 위치로 이동하였습니다.");

                                    stop.setEnabled( true );
                                    autopilot.setEnabled( true );

                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putFloat( "lastPhoneLat", (float) latLng.latitude );
                                    editor.putFloat( "lastPhoneLng", (float) latLng.longitude);
                                    editor.commit();
                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        final Activity_03_Map activity = this;

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
        }, 5_000);

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                activity.whenMapClick( latLng );
            }
        });

    }

    public void paintUI() {
        // do nothing.
    }

}
