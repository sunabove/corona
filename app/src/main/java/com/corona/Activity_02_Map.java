package com.corona;

import android.annotation.SuppressLint;
import android.content.ContentValues;
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

import org.locationtech.proj4j.ProjCoordinate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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

    private LocationResult lastLocationResult ;

    protected FloatingActionButton showCalendar ;
    private CalendarView calendar ;
    private LinearLayout togglePane;
    private ImageButton hidePaneBtn ;
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
        this.calendar = this.findViewById(R.id.calendar);
        this.togglePane = this.findViewById(R.id.togglePane) ;
        this.hidePaneBtn = this.findViewById(R.id.hidePaneBtn);
        this.gpsLogTimeFr = this.findViewById(R.id.gpsLogTimeFr);
        this.gpsLogTimeTo = this.findViewById(R.id.gpsLogTimeTo);
        this.gpsLogSeekBarProgress = this.findViewById(R.id.gpsLogSeekBarProgress);
        this.gpsLogSeekBar = this.findViewById(R.id.gpsLogSeekBar);

        this.hidePaneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                whenShowCalendarClicked();
            }
        });

        this.gpsLogSeekBar.setMax( 100 );

        this.gpsLogSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                gpsLogSeekBarProgress.setText( progress + " %" );
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

            showCoronaMarkerFromDbImpl( id );
        } catch( Exception e ) {
            e.printStackTrace();
        } finally {
            this.showingCoronaMarkerDb = false ;
        }
    }

    private HashMap<Integer, Double> mapScales = null ;

    private double getRefScale( double zoom ) {
        if( null == mapScales ) {
            mapScales = new HashMap<>();
            mapScales.put( 20, 1128.497220 );
            mapScales.put( 19, 2256.994440 );
            mapScales.put( 18, 4513.988880 );
            mapScales.put( 17, 9027.977761 );
            mapScales.put( 16, 18055.955520 );
            mapScales.put( 15, 36111.911040 );
            mapScales.put( 14, 72223.822090 );
            mapScales.put( 13, 144447.644200 );
            mapScales.put( 12, 288895.288400 );
            mapScales.put( 11, 577790.576700 );
            mapScales.put( 10, 1155581.153000 );
            mapScales.put(  9, 2311162.307000 );
            mapScales.put(  8, 4622324.614000 );
            mapScales.put(  7, 9244649.227000 );
            mapScales.put(  6, 18489298.450000 );
            mapScales.put(  5, 36978596.910000 );
            mapScales.put(  4, 73957193.820000 );
            mapScales.put(  3, 147914387.600000 );
            mapScales.put(  2, 295828775.300000 );
            mapScales.put(  1, 591657550.500000 );

            /*
            20 : 1128.497220
            19 : 2256.994440
            18 : 4513.988880
            17 : 9027.977761
            16 : 18055.955520
            15 : 36111.911040
            14 : 72223.822090
            13 : 144447.644200
            12 : 288895.288400
            11 : 577790.576700
            10 : 1155581.153000
            9  : 2311162.307000
            8  : 4622324.614000
            7  : 9244649.227000
            6  : 18489298.450000
            5  : 36978596.910000
            4  : 73957193.820000
            3  : 147914387.600000
            2  : 295828775.300000
            1  : 591657550.500000
            */
        }
        double scale = mapScales.get( (int) zoom ) ;

        return scale;
    }

    private double getMapScaleRatio(double zoom) {
        if( zoom == (int) zoom ) {
            return this.getRefScale( zoom );
        }

        int zoomPrev = (int) zoom ;
        int zoomNext = (int) ( zoom - 1 );

        double scalePrev = this.getRefScale( zoomPrev ) ;
        double scaleNext = this.getRefScale( zoomNext );

        double scale = scalePrev + (scaleNext - scalePrev)*(zoom - zoomPrev);

        return scale;
    }

    private void showCoronaMarkerFromDbImpl(final long spec_id) {
        // remove at first
        HashMap<Long, Marker> coronaMarkers = this.coronaMarkers;
        Iterator<Map.Entry<Long, Marker>> it = coronaMarkers.entrySet().iterator();

        while ( this.isActivityAlive() && it.hasNext() ) {
            Marker marker = it.next().getValue();
            marker.remove();
            it.remove();
        }

        if( ! this.isActivityAlive() ) {
            return ;
        }

        DbHelper dbHelper = this.dbHelper;

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

        SQLiteDatabase db = dbHelper.wdb;

        String sql = "" ;
        sql += " SELECT id, deleted, checked, notification, up_dt, place, patient, visit_fr, visit_to " ;
        sql += " , latitude, longitude " ;
        sql += " FROM corona " ;
        sql += " WHERE  id = ? " ;
        sql += " OR ( x >= ? AND x <= ? AND y >= ? AND y <= ? )" ;
        sql += " ORDER BY up_dt ASC " ;
        ;

        String[] args = { "" + spec_id, "" + minX , "" + maxX , "" + minY , "" + maxY };
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

            int rscId = R.drawable.map_dot_corona_old_64; // old data
            if( 1 == checked ) { // checked data
                if( notification < 2 ) { // when notified
                    rscId = R.drawable.map_dot_corona_notifying_64;
                } else { // when notification accepted
                    rscId = R.drawable.map_dot_corona_notified_64;
                }
            } else if( Math.abs( now - up_dt ) < 20*ComInterface.CORONA_DB_GET_INTERVAL ) {
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

            LatLng latLng = new LatLng( latitude, longitude );
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position( latLng );
            markerOptions.title( title );
            markerOptions.snippet( snippet );
            markerOptions.flat(true);
            markerOptions.icon( markerIcon );
            markerOptions.zIndex( coronaMarkerZIndex );

            if( 1 == deleted ) {
                deletedIds.add( id );
            }

            if (!this.isActivityAlive()) {
                Log.d(TAG, "Activity is not alive skipped to add corona marker.");
                break;
            }

            Marker markerPrev = coronaMarkers.get( id );
            if( null != markerPrev ) {
                markerPrev.remove();
                coronaMarkers.remove(id);
            }

            Marker marker = googleMap.addMarker(markerOptions);
            String tag = String.format("%d:%d", id, notification);
            marker.setTag( tag );
            coronaMarkers.put(id, marker);

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

            this.mapReadyTime = System.currentTimeMillis();
        }
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

        togglePane.setVisibility( togglePane.getVisibility() == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE );

        if( togglePane.getVisibility() == View.VISIBLE ) {
            this.calendarTime = this.calendar.getDate();

            this.showGpsLogFromDb(100, this.calendarTime );
        }
    }

    private void whenMapLongIdle() {
        long now = System.currentTimeMillis() ;

        if( now > this.gpsLogSeekBarMoveTime + 30*1_000 ) {
            this.gpsLogSeekBar.setProgress( 0 );
            this.gpsLogSeekBarMovedCnt = 0 ;
        }

        if( this.calendar.getVisibility() == View.INVISIBLE ) {
            if( calendarTime > 0 && now > calendarTime + 30_1000 ) {
                this.calendarTime = 0 ;
            }
        }
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

        whenMapLongIdle();

        if( zoom != this.lastZoom ) {
            whenCameraZoomChanged();
        } else {
            this.showCoronaMarkerFromDb();
        }

        this.lastZoom = zoom ;
    }

    private float getZoom() {
        float zoom = googleMap.getCameraPosition().zoom ;

        return zoom;
    }

    private void whenCameraZoomChanged() {
        this.showGpsLogFromDb( this.gpsLogSeekBarMovedCnt < 1 ? 100 : this.gpsLogSeekBar.getProgress() , this.calendarTime );

        this.showCurrentGpsData( this.lastLocationResult );

        this.showCoronaMarkerFromDb();
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
        int color = Color.GRAY ;
        int lineWidth = 30;
        int progress = 0 ;
    }

    private int gpsLogSeekBarMovedCnt = 0 ;

    private long gpsLogSeekBarMoveTime = 0 ;

    private void whenGpsLogSeekBarMoved( ) {
        gpsLogSeekBarMovedCnt ++ ;
        gpsLogSeekBarMoveTime = System.currentTimeMillis();

        Log.d( TAG, "gpsLogSeekBarMoveTime = " + gpsLogSeekBarMoveTime );

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

        option.visitTimeFr = System.currentTimeMillis() - 24*60*60*1_000; // condition for yesterday;
        option.visitTimeTo = this.mapReadyTime ;

        if( 0 < calendarTime ) {
            option.visitTimeFr = calendarTime;
            option.visitTimeTo = calendarTime + 24*60*60+1_000 -1 ;
            if( option.visitTimeTo > this.mapReadyTime ) {
                option.visitTimeTo = this.mapReadyTime ;
            }
        }

        option.progress = progress;

        if( true ) {
            DbHelper dbHelper = this.dbHelper;

            SQLiteDatabase db = dbHelper.rdb;

            String sql = "SELECT MIN( visit_tm ) , MAX( visit_tm ) FROM gps ";
            sql += " WHERE visit_tm BETWEEN ? AND ? " ;
            sql += " LIMIT 1 ";

            String[] args = { "" + option.visitTimeFr, "" + option.visitTimeTo };
            Cursor cursor = db.rawQuery(sql, args);

            while( cursor.moveToNext() ) {
                option.visitTimeFr = cursor.getLong( 0 );
                option.visitTimeTo = cursor.getLong( 1 );
            }

            cursor.close();
        }

        if( progress < 100 ) {
            option.visitTimeTo = (long) ( option.visitTimeFr + Math.abs( option.visitTimeTo - option.visitTimeFr )*( progress + 0.0)/100.0 );
        }

        this.showGpsLogFromDbWithOption( option );
    }

    private int showGpsLogFromDbWithOptionCnt = 0 ;
    private void showGpsLogFromDbWithOption( GpsLogPaintOption option ) {

        Log.d( TAG, String.format("[%d] showGpsLogFromDbWithOption", showGpsLogFromDbWithOptionCnt) );
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

        //List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(20), new Gap(10));
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dot(), new Gap(10));
        polyOptions.pattern( pattern );

        int cnt = 0 ;
        long id;
        long visit_tm = 0 ;
        double latitude, longitude;
        String dateTimeTextLog;
        SimpleDateFormat dfLog = ComInterface.yyyMMdd_HHmmSS;
        String dateTimeTextUi;
        SimpleDateFormat dfUi = new SimpleDateFormat("HH:mm:ss");

        if( true ) {
            this.gpsLogTimeFr.setText( dfUi.format( new Date( option.visitTimeFr )));
            this.gpsLogSeekBarProgress.setText( option.progress + " %");
        }

        int idx = 0 ;
        LatLng latLng = null ;

        Log.d( TAG, "gps log db cursor count = " + cursor.getCount() ) ;

        while (cursor.moveToNext()) {
            id = cursor.getLong(cursor.getColumnIndex("id"));
            latitude = cursor.getFloat(cursor.getColumnIndex("latitude"));
            longitude = cursor.getFloat(cursor.getColumnIndex("longitude"));

            visit_tm = cursor.getLong(cursor.getColumnIndex("visit_tm"));

            dateTimeTextLog = dfLog.format( new Date( visit_tm ) ) ;

            String info = "Gps Log on DB: id = %d, lon = %f, lat = %f, visit_tm = %s ";
            info = String.format(info, id, longitude, latitude, dateTimeTextLog );
            Log.d(TAG, info);

            latLng = new LatLng( latitude, longitude );
            polyOptions.add( latLng );

            if( 0 == idx ) {
                this.gpsLogTimeFr.setText( dfUi.format( new Date( visit_tm )));
                this.gpsLogSeekBarProgress.setText( option.progress + " %");
            }

            cnt ++ ;
            idx ++ ;
        }
        cursor.close();

        this.gpsLogTimeTo.setText( dfUi.format( new Date( option.visitTimeTo ) ) );

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
        CalendarView calendar = this.calendar;
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

}
