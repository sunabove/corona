package com.corona;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.SeekBar;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.locationtech.proj4j.ProjCoordinate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Activity_02_Map extends ComActivity implements OnMapReadyCallback {

    private static String TAG = "sun_above map" ;

    private GoogleMap googleMap;

    private EditText status ;
    private TextView mapInfo ;
    private ImageView gpsLogo;
    private ImageView coronaDownloadIcon ;

    private LocationResult lastLocationResult ;

    protected FloatingActionButton goBack ;
    protected FloatingActionButton showCalendar ;
    protected FloatingActionButton showCoronaDataListBtn ;

    private CalendarView calendarView;
    private LinearLayout togglePane;
    private ImageButton hideCalendarPaneBtn;

    private TextView gpsLogTimeFr ;
    private TextView gpsLogTimeTo ;
    private TextView gpsLogSeekBarProgress ;
    private SeekBar gpsLogSeekBar ;

    private FusedLocationProviderClient fusedLocationClient;

    private Marker phoneMarker;
    private int phoneMarkerUpdCnt = 0 ;
    private Polyline gpsPathPoly = null ;
    private Polyline gpsLogPathPoly = null ;
    private GpsLog gpsLog = new GpsLog();
    private LatLng lastGpsLatLng ;

    private float lastZoom ;
    private int coronaMarkerZIndex = 1;
    private HashMap<Long, Marker> coronaMarkers = new HashMap<>();
    private Handler coronaDbShowHandler ;

    private DbHelper dbHelper;
    private Proj projection = Proj.projection();
    private long mapReadyTime = System.currentTimeMillis();

    @Override
    public final int getLayoutId() {
        return R.layout.activity_02_map;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mapReadyTime = System.currentTimeMillis();

        this.hideActionBar();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.status = this.findViewById(R.id.status);
        this.mapInfo = this.findViewById(R.id.mapInfo);
        this.gpsLogo = this.findViewById(R.id.gpsLogo);
        this.showCalendar = this.findViewById(R.id.showCalendar);
        this.calendarView = this.findViewById(R.id.calendarView);
        this.togglePane = this.findViewById(R.id.togglePane) ;
        this.hideCalendarPaneBtn = this.findViewById(R.id.hideCalenearPaneBtn);
        this.gpsLogTimeFr = this.findViewById(R.id.gpsLogTimeFr);
        this.gpsLogTimeTo = this.findViewById(R.id.gpsLogTimeTo);
        this.gpsLogSeekBarProgress = this.findViewById(R.id.gpsLogSeekBarProgress);
        this.gpsLogSeekBar = this.findViewById(R.id.gpsLogSeekBar);

        this.coronaDownloadIcon = this.findViewById(R.id.coronaDownloadIcon);

        this.gpsLogSeekBar.setMax( 100 );
        this.gpsLogSeekBar.setProgress( 100 );

        this.goBack = this.findViewById(R.id.goBack);

        this.showCoronaDataListBtn = this.findViewById(R.id.showCoronaDataListBtn);

        this.showCoronaDataListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whenShowCoronaDataListBtnClicked();
            }
        });

        if( null != goBack ) {
            this.goBack.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        this.gpsLogSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                gpsLogSeekBarProgress.setText(String.format("%d%s", progress, "%" ));

                if( fromUser ) {
                    whenGpsLogSeekBarMoved();
                }else {
                    Log.d( TAG, "gpsLogSeekBar is not fromUser." );
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        this.togglePane.setVisibility(View.INVISIBLE);

        this.status.setText("");

        // hide keyboard always
        this.status.setInputType(InputType.TYPE_NULL);

        this.hideCalendarPaneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whenShowCalendarClicked();
            }
        });

        this.showCalendar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                whenShowCalendarClicked();
            }
        });

        this.calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                whenCalendarViewClicked( year, month, dayOfMonth );
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

    // whenShowCoronaDataListBtnClicked
    private void whenShowCoronaDataListBtnClicked () {
        Intent intent = new Intent(this, Activity_03_CoronaList.class );
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, ComInterface.INTENT_RESULT_CORONA_SELECTED );
    }
    // -- whenShowCoronaDataListBtnClicked

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

            showCoronaMarkerFromDbImpl( id );
        } catch( Exception e ) {
            e.printStackTrace();
        } finally {
            this.showingCoronaMarkerDb = false ;
        }
    }

    private void showCoronaMarkerFromDbImpl(final long spec_id) {
        this.animateCoronaDataDownloading();

        if( ! this.isActivityAlive() ) {
            return ;
        }

        HashMap<Long, Marker> coronaMarkers = this.coronaMarkers;

        GoogleMap googleMap = this.googleMap;
        LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng sw = bounds.southwest;
        LatLng ne = bounds.northeast;
        Proj proj = this.projection;
        ProjCoordinate swTm = proj.convertToUtmK( sw );
        ProjCoordinate neTm = proj.convertToUtmK( ne );

        double minX = swTm.x < neTm.x ? swTm.x : neTm.x ;
        double minY = swTm.y < neTm.y ? swTm.y : neTm.y ;
        double maxX = swTm.x > neTm.x ? swTm.x : neTm.x ;
        double maxY = swTm.y > neTm.y ? swTm.y : neTm.y ;

        DbHelper dbHelper = this.dbHelper;

        SQLiteDatabase db = dbHelper.wdb;

        String sql = "" ;
        sql += " SELECT id, deleted, checked, notification, up_dt, place, patient, visit_fr, visit_to " ;
        sql += " , latitude, longitude, y, x " ;
        sql += " FROM corona " ;

        sql += " WHERE  id = ? " ;
        sql += " OR ( x >= ? AND x <= ? AND y >= ? AND y <= ? )" ;
        sql += " ORDER BY up_dt ASC " ;
        ;

        String[] args = { "" + spec_id, "" + minX , "" + maxX , "" + minY , "" + maxY };
        //String[] args = { };
        Cursor cursor = db.rawQuery(sql, args);

        Corona corona ;

        String snippet, info ;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        ArrayList<Long> deletedIds = new ArrayList<>();
        final long now = System.currentTimeMillis();

        Log.d( TAG, "corona marker count = " + cursor.getCount() );

        while ( this.isActivityAlive() && cursor.moveToNext()) {
            corona = new Corona() ;
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
            corona.x = cursor.getFloat(cursor.getColumnIndex("x"));
            corona.y = cursor.getFloat(cursor.getColumnIndex("y"));

            String title = corona.getTitle();

            snippet = String.format( "%s ~ %s", df.format( corona.visit_fr ) , df.format( corona.visit_to ) );

            corona.up_dt_str = df.format( corona.up_dt ) ;

            info = "corona marker deleted = %d, checked = %d, notification = %d, title = %s, snippet = %s, latitude = %f, longitude = %f, up_dt = %s" ;
            info = String.format( info, corona.deleted, corona.checked, corona.notification, title, snippet,
                    corona.latitude, corona.longitude, corona.up_dt_str ) ;
            Log.d( TAG, info );

            Log.d( TAG, "corona marker x = " + corona.x + ", y = " + corona.y );

            int rscId = R.drawable.map_dot_corona_old_64; // old data
            if( 1 == corona.checked ) { // checked data
                if( corona.notification < 2 ) { // when notified
                    rscId = R.drawable.map_dot_corona_notifying_64;
                } else { // when notification accepted
                    rscId = R.drawable.map_dot_corona_notified_64;
                }
            } else if( Math.abs( now - corona.up_dt ) < 20*ComInterface.CORONA_DB_GET_INTERVAL ) {
                // latest data
                rscId = R.drawable.map_dot_corona_latest_data_64;
            }

            Bitmap b = BitmapFactory.decodeResource(getResources(), rscId );

            double ratio = 1.0;
            float zoom = this.getZoom();

            if( 18.0 < zoom ) {
                ratio = 1.0;
            } else if( 16.8 < zoom ) {
                ratio = 0.75;
            } else if( 13 < zoom ) {
                ratio = 0.5;
            } else {
                ratio = 0.38;
            }

            int width = (int)( b.getWidth()*ratio );
            int height = (int)( b.getHeight()*ratio );

            Bitmap markerIconResized = Bitmap.createScaledBitmap(b, width, height, false);
            BitmapDescriptor markerIcon = BitmapDescriptorFactory.fromBitmap( markerIconResized );

            if( 1 == corona.deleted ) {
                deletedIds.add( corona.id );
            }

            if (!this.isActivityAlive()) {
                Log.d(TAG, "Activity is not alive skipped to add corona marker.");
                break;
            }

            LatLng latLng = new LatLng( corona.latitude, corona.longitude );

            Marker marker = coronaMarkers.get( corona.id );
            if( null != marker ) {
                marker.setPosition(latLng);
            } else {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position( latLng );
                markerOptions.zIndex( coronaMarkerZIndex );

                marker = googleMap.addMarker(markerOptions);

                coronaMarkers.put( corona.id, marker);

                coronaMarkerZIndex ++ ;
            }

            marker.setTitle( title );
            marker.setSnippet( snippet );
            marker.setFlat( true );
            marker.setIcon(markerIcon) ;
            marker.setTag(corona);
        }

        cursor.close();

        for( Long delId : deletedIds ) {
            db.delete("corona", " WHERE ID = ?", new String[]{ "" + delId });
        }

    }
    // -- startCoronaDbShowImpl

    private void whenMarkerClicked( Marker marker ) {
        Object obj = marker.getTag();

        Corona corona = null ;

        if( obj instanceof Corona ) {
            corona = (Corona) obj;

            if ( 1 == corona.notification ) {
                DbHelper dbHelper = this.dbHelper;

                dbHelper.updateCoronaNotification(corona, 2);

                this.showCoronaMarkerFromDb( corona.id );
            }
        }

    }
    // whenMarkerClicked

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if( this.togglePane.getVisibility() == View.VISIBLE ) {
            this.whenShowCalendarClicked();
        } else {
            TextView status = findViewById(R.id.status);

            if( null != status ) {
                status.setText( "앱을 종료합니다." );
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 이전 화면으로 돌아감.
                    finish();
                }
            }, 300);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        final ComActivity activity = this;
        if(requestCode == PERMISSION_REQUEST_ID){
            if( grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED ) {
                this.whenPermissionGranted();
            } else {
                String info = "앱을 다시 실행하여 권한을 부여하여 주세요.";
                status.setText( info);
                Toast.makeText( activity, info, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d( TAG, "resultCode = " + resultCode );

        if(INTENT_RESULT_CORONA_SELECTED == resultCode ) {
            Bundle bundle = data.getExtras();
            if( null != bundle ) {
                Corona corona = (Corona) bundle.getSerializable("corona");
                if ( null != corona ) {
                    Log.d(TAG, "corona id = " + corona.id);
                    this.whenCoronaSelectedFromDataList( corona );
                }
            }
        }
    }

    private Corona coronaMoved;

    private void whenCoronaSelectedFromDataList( Corona corona ) {
        float zoom = this.getZoom() ;
        LatLng latLng = corona.getLatLng();

        this.coronaMoved = corona ;

        if( zoom > 16.5 ) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, googleMap.getMaxZoomLevel() - 2));
        }

        this.mapForceMoveTime = System.currentTimeMillis() ;
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

        boolean valid = checkLocationPermissions();

        if( ! valid ) {
            requestLocationPermissions();
        } else {
            valid = valid && isLocationEnabled();

            if( ! valid ) {
                buildAlertMessageNoGps();
            }
        }

        if( valid ) { // location updater
            this.startLocationService();

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

            this.mapReadyTime = System.currentTimeMillis();
        }

        Intent intent = getIntent();

        if( null != intent ) {
            this.whenNewIntentReceived( intent );
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void whenLocationUpdate( LocationResult locationResult ) {
        Log.d(TAG, String.format("locationResult update[%d]: %s", gpsUpdCnt, locationResult));

        this.whenMapLongIdle();

        this.animateGpsLogoRotate();

        this.showCoronaMarkerFromDb();

        lastLocationResult = locationResult ;

        this.showCurrentGpsData( locationResult );

        gpsUpdCnt ++;
    }

    private long lastMapMoveTime = 0 ;

    private long calendarTime = 0 ;

    private void whenShowCalendarClicked() {
        if( View.INVISIBLE == togglePane.getVisibility() ) {
            this.setHighlightedDays();
        }

        if( togglePane.getVisibility() == View.INVISIBLE ) {
            SQLiteDatabase db = this.dbHelper.rdb;

            long todayStartTime = this.getTodayStartTime() ;
            long now = System.currentTimeMillis();

            String sql = "SELECT IFNULL( MIN( visit_tm ) , ? ) AS visit_tm_min " ;
            sql += " , IFNULL( MAX( visit_tm ), ? ) AS visit_tm_max FROM gps ";
            sql += " LIMIT 1 ";

            String[] args = { "" + todayStartTime, "" + now };
            Cursor cursor = db.rawQuery(sql, args);

            SimpleDateFormat df = ComInterface.yyyyMMdd_HHmmSS;

            while( cursor.moveToNext() ) {
                long minTm = cursor.getLong( 0 );
                long maxTm = cursor.getLong( 1 );

                Log.d( TAG, "gps all min visit_tm = " + df.format( minTm ) );
                Log.d( TAG, "gps all max visit_tm = " + df.format( maxTm ) );

                // max date 을 먼저 설정하여야 제대로 UI에 반영된다. android 버그인 듯 함.
                this.calendarView.setMaxDate( maxTm );
                this.calendarView.setMinDate( minTm );
            }

            cursor.close();
        }

        togglePane.setVisibility( togglePane.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE );
    }

    private long mapForceMoveTime = 0 ;

    private void whenCalendarViewClicked( int year, int month, int dayOfMonth ) {
        Log.d( TAG, String.format("calendarView year = %d, month = %d, day = %d",  year, month, dayOfMonth ) );

        this.mapForceMoveTime = System.currentTimeMillis();

        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.YEAR, year );
        calendar.set( Calendar.MONTH , month );
        calendar.set( Calendar.DAY_OF_MONTH , dayOfMonth );
        calendar.set( Calendar.HOUR_OF_DAY, 0 );
        calendar.set( Calendar.MINUTE, 0 );
        calendar.set( Calendar.SECOND, 0 );
        calendar.set( Calendar.MILLISECOND, 0 );

        this.calendarTime = calendar.getTimeInMillis();

        this.gpsLogSeekBar.setProgress( 100 );

        this.showGpsLogFromDb(100, this.calendarTime );
    }

    private void whenMapLongIdle() {
        long now = System.currentTimeMillis() ;

        if( now > this.gpsLogSeekBarMoveTime + 30*1_000 ) {
            this.gpsLogSeekBar.setProgress( 100 );
            this.gpsLogSeekBarMovedCnt = 0 ;
        }

        if( this.calendarView.getVisibility() == View.INVISIBLE ) {
            if( mapForceMoveTime > 0 && now > mapForceMoveTime + 30_1000 ) {
                this.mapForceMoveTime = 0 ;
                this.calendarTime = 0 ;
            }
        }
    }

    int gpsUpdCnt = 0;

    private void whenCameraIdle() {
        boolean valid = checkLocationPermissions();

        if( ! valid ) {
            requestLocationPermissions();
            return ;
        }

        if (this.isLocationEnabled()) {
            this.gpsLogo.setImageResource(R.drawable.gps_recording_02);
        } else {
            this.gpsLogo.setImageResource(R.drawable.gps_recording_00);
        }

        final float zoom = this.getZoom();

        if( true ) {
            TextView mapInfo = this.mapInfo;
            String info = "Zoom: %02.1f,  GPS: %d ";
            info = String.format(info, zoom, gpsUpdCnt);

            mapInfo.setText(info);
        }

        whenMapLongIdle();

        if (zoom != this.lastZoom) {
            whenCameraZoomChanged();
        } else {
            this.showCoronaMarkerFromDb();
        }

        final Corona coronaMoved = this.coronaMoved ;

        if( null != coronaMoved) {
            long id = coronaMoved.id ;
            Marker marker = this.coronaMarkers.get( id ) ;
            if( null != marker ) {
                marker.showInfoWindow();
            }
        }

        long coronaInfectedCnt = this.dbHelper.getCoronaListInfectedCount( 1 );

        if( 1 > coronaInfectedCnt ) {
            this.showCoronaDataListBtn.setImageResource( R.drawable.corona_data_list_01_no_data);

            this.status.setText( "지도가 로드 되었습니다.");
        } else {
            this.showCoronaDataListBtn.setImageResource( R.drawable.corona_data_list_02_data );

            String info = String.format("%d건의 미확인 확진자 중첩 정보가 있습니다.", coronaInfectedCnt ) ;

            this.status.setText( info );

            if( null == coronaMoved ) {
                Snackbar snackbar = Snackbar.make(this.status, info, Snackbar.LENGTH_LONG);
                snackbar.setAction("No action", null);
                snackbar.show();
            }
        }

        this.removeAllMarkerOutOfBounds();

        if( coronaMoved == this.coronaMoved ) {
            this.coronaMoved = null ;
        }

        this.lastZoom = zoom;
    }

    private float getZoom() {
        float zoom = googleMap.getCameraPosition().zoom ;

        return zoom;
    }

    private void whenCameraZoomChanged() {
        this.showGpsLogFromDb( this.gpsLogSeekBarMovedCnt < 1 ? 100 : this.gpsLogSeekBar.getProgress() , this.calendarTime );

        this.showCoronaMarkerFromDb();

        this.showCurrentGpsData( this.lastLocationResult );
    }

    private void removeAllMarkerOutOfBounds() {
        HashMap<Long, Marker> coronaMarkers = this.coronaMarkers;

        Iterator<Map.Entry<Long, Marker>> it = coronaMarkers.entrySet().iterator();

        final GoogleMap googleMap = this.googleMap ;
        final LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds ;

        while ( this.isActivityAlive() && it.hasNext() ) {
            Marker marker = it.next().getValue();
            if( ! bounds.contains(marker.getPosition() ) ) {
                marker.remove();
                it.remove();
            }
        }
    }

    private void whenMapClick(LatLng latLng) {
        Log.d( TAG, "onMapClick. " );
    }

    private boolean isMapDetail() {
        float zoom = this.getZoom();
        return ( zoom > 17.0 );
    }

    class GpsLogPaintOption {
        long visitTimeFr;
        long visitTimeTo;
        long visitTimeToUi;

        int color = Color.GRAY ;
        int lineWidth = 30;
        int progress = 0 ;
    }

    private int gpsLogSeekBarMovedCnt = 0 ;

    private long gpsLogSeekBarMoveTime = 0 ;

    private void whenGpsLogSeekBarMoved( ) {
        gpsLogSeekBarMovedCnt ++ ;
        gpsLogSeekBarMoveTime = System.currentTimeMillis();

        //Log.d( TAG, "gpsLogSeekBarMoveTime = " + gpsLogSeekBarMoveTime );

        Runnable runnable = new Runnable() {
            private long reqTime = gpsLogSeekBarMoveTime ;
            @Override
            public void run() {
                if( this.reqTime == gpsLogSeekBarMoveTime ) {
                    int progress = gpsLogSeekBar.getProgress();

                    progress = 0 > progress ? 0 : progress;
                    progress = 100 < progress ? 100 : progress ;

                    if( isActivityAlive() ) {
                        showGpsLogFromDb(progress, calendarTime);
                    } else {
                        Log.d( TAG, "gpsLogSeekBar activity is not alive" );
                    }

                    Log.d( TAG, "gpsLogSeekBar progress = " + progress );
                } else {
                    Log.d( TAG, "reqTime is not valid." );
                }
            }
        };

        Handler handler = new Handler();
        handler.postDelayed(runnable, 300 );
    }

    private void showGpsLogFromDb(final int progress, final long calendarTime) {
        boolean  isMapDetail = this.isMapDetail() ;

        if( this.mapReadyTime < 1 ) {
            this.mapReadyTime = System.currentTimeMillis();
        }

        GpsLogPaintOption option = new GpsLogPaintOption();
        option.color = Color.GRAY;
        option.lineWidth = isMapDetail ? 30: 15 ;

        if( 0 < calendarTime ) { // when calendar time was selected
            SimpleDateFormat df = ComInterface.yyyyMMdd_HHmmSS;
            Log.d( TAG, "showGpsLogFromDb calendarTime = " + df.format( calendarTime ) );
            option.visitTimeFr = calendarTime;
            option.visitTimeTo = calendarTime + ONE_DAY_TIME -1 ;
            if( option.visitTimeTo > this.mapReadyTime ) {
                option.visitTimeTo = this.mapReadyTime ;
            }
        } else if( 1 > calendarTime ){ // when calendar time was not selected
            option.visitTimeFr = this.getTodayStartTime() ;
            option.visitTimeTo = this.mapReadyTime ;
        }

        option.visitTimeToUi = option.visitTimeTo ;

        option.progress = progress;

        if( progress < 100 ) {
            option.visitTimeTo = (long) ( option.visitTimeFr + Math.abs( option.visitTimeTo - option.visitTimeFr )*( progress + 0.0)/100.0 );
        }

        this.showGpsLogFromDbWithOption( option );
    }

    private int showGpsLogFromDbWithOptionCnt = 0 ;

    private void showGpsLogFromDbWithOption( GpsLogPaintOption option ) {

        SimpleDateFormat dfLog = ComInterface.yyyyMMdd_HHmmSS;

        Log.d( TAG, String.format("[%d] showGpsLogFromDbWithOption", showGpsLogFromDbWithOptionCnt) );
        Log.d( TAG, "visitTimeFr = " + dfLog.format( option.visitTimeFr) );
        Log.d( TAG, "visitTimeTo = " + dfLog.format( option.visitTimeTo) );
        showGpsLogFromDbWithOptionCnt ++ ;

        DbHelper dbHelper = this.dbHelper;

        SQLiteDatabase db = dbHelper.rdb;

        String sql = "SELECT id, latitude, longitude, visit_tm FROM gps ";
        sql += " WHERE visit_tm BETWEEN ? AND ? " ;
        sql += " ORDER BY visit_tm ASC ";

        String[] args = { "" + option.visitTimeFr, "" + option.visitTimeTo };
        Cursor cursor = db.rawQuery(sql, args);

        int color = option.color ;
        int lineWidth = option.lineWidth;

        PolylineOptions polyOptions = new PolylineOptions().width( lineWidth ).color( color ).geodesic(true);
        polyOptions.jointType(JointType.ROUND);

        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(10));
        polyOptions.pattern( pattern );

        int cnt = 0 ;
        long id;
        long visit_tm = 0 ;
        double latitude, longitude;

        String dateTimeTextLog;
        SimpleDateFormat dfUi = ComInterface.MMdd_HHmm;

        if( true ) {
            this.gpsLogTimeFr.setText( dfUi.format( option.visitTimeFr ));
            this.gpsLogSeekBarProgress.setText(String.format("%d%s", option.progress, "%" ));
        }

        int idx = 0 ;
        LatLng latLng = null ;

        Log.d( TAG, "gps log db cursor count = " + cursor.getCount() ) ;

        while (cursor.moveToNext()) {
            id = cursor.getLong(cursor.getColumnIndex("id"));
            latitude = cursor.getFloat(cursor.getColumnIndex("latitude"));
            longitude = cursor.getFloat(cursor.getColumnIndex("longitude"));

            visit_tm = cursor.getLong(cursor.getColumnIndex("visit_tm"));

            dateTimeTextLog = dfLog.format( visit_tm ) ;

            String info = "Gps Log on DB: id = %d, lon = %f, lat = %f, visit_tm = %s ";
            info = String.format(info, id, longitude, latitude, dateTimeTextLog );
            Log.d(TAG, info);

            latLng = new LatLng( latitude, longitude );
            polyOptions.add( latLng );

            cnt ++ ;
            idx ++ ;
        }
        cursor.close();

        SimpleDateFormat dfUiCurr = ComInterface.HHmmSS ;

        if( 99 < option.progress ) {
            this.gpsLogSeekBarProgress.setText( "100%" );
        } else {
            String visitTmToText = dfUiCurr.format( option.visitTimeTo );
            this.gpsLogSeekBarProgress.setText(String.format("%d%s", option.progress, "% " + visitTmToText));
        }

        this.gpsLogTimeTo.setText( dfUiCurr.format( option.visitTimeToUi ) );

        if (null != gpsLogPathPoly) {
            gpsLogPathPoly.remove();

            gpsLogPathPoly = null;
        }

        if( latLng != null ) {

            while( cnt < 2 ) {
                polyOptions.add( latLng );
                cnt ++ ;
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
        CalendarView calendar = this.calendarView;
    }

    private void showCurrentGpsData(LocationResult locationResult ) {
        if (null == locationResult) {
            return;
        }

        Location location = locationResult.getLastLocation();

        this.showCurrentGpsData( location );
    }

    private void showCurrentGpsData(Location location) {

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

        this.showCurrentPosMarker( location );
    }

    private void showCurrentPosMarker(Location location ) {
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

        int mapIconRscId = R.drawable.smart_phone_icon_03_16;
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

        //double gpsHeading = gpsLog.getGpsHeading( 0 );
        double gpsHeading = location.getBearing();

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

    private void animateCoronaDataDownloading() {
        ImageView coronaDownloadIcon = this.coronaDownloadIcon ;

        coronaDownloadIcon.clearAnimation();

        coronaDownloadIcon.setImageResource(R.drawable.corona_data_downloading );

        int relative = Animation.RELATIVE_TO_SELF ;

        Animation animation = new RotateAnimation(
                30, -30,
                relative, 0.5f,
                relative,  0.5f);

        animation.setDuration( 2_500 );
        animation.setRepeatCount( 1 );

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

        this.coronaDownloadIcon.startAnimation( animation );
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
                        showCurrentGpsData( location );
                    }
                }
            }
        );
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        this.whenNewIntentReceived( intent );
    }

    private void whenNewIntentReceived(Intent intent) {

        Bundle bundle = intent.getExtras();
        if( null != bundle ) {
            Corona corona = (Corona) bundle.getSerializable(corona_from_notification_click );
            if ( null != corona ) {
                Log.d(TAG, "corona_from_notification_click corona id = " + corona.id);
                this.whenCoronaSelectedFromDataList( corona );

                intent = new Intent(this, Activity_03_CoronaList.class );
                bundle = new Bundle();
                bundle.putSerializable( corona_from_notification_click , corona ) ;
                intent.putExtras(bundle);

                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityForResult(intent, ComInterface.INTENT_RESULT_CORONA_SELECTED );
            }
        }
    }

}
