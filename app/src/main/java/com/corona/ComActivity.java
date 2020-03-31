package com.corona;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.socket.client.Socket;

public abstract class ComActivity extends AppCompatActivity implements ComInterface {

    protected static class Motion {
        public static final String FORWARD = "FORWARD" ;
        public static final String BACKWARD = "BACKWARD" ;

        public static final String LEFT = "LEFT" ;
        public static final String RIGHT = "RIGHT" ;

        public static final String STOP = "STOP" ;

        public static final String AUTOPILOT = "AUTOPILOT" ;
    }

    protected Context context ;
    protected SharedPreferences sharedPref = null;
    protected RequestQueue requestQueue ;


    public abstract int getLayoutId() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( this.getLayoutId() );

        this.context = this.getApplicationContext();

        if( null == sharedPref ) {
            sharedPref = getSharedPreferences("mySettings", MODE_PRIVATE);
        }

        this.requestQueue = Volley.newRequestQueue(this);

    }

    public double prettyDegree( double degree ) {
        degree = degree % 360 ;

        if( 180 < degree ) {
            degree = degree - 360 ;
        }
        return degree ;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public <T extends View> T findViewById(@IdRes int id) {
        return (T) super.findViewById(id);
    }

    public void postDelayed(Runnable r, int delayMillis) {
        new Handler().postDelayed( r, delayMillis);
    }

    public void sleep( long millis ) {
        try {
            Thread.currentThread().sleep(millis);
        } catch ( Exception e ) {
            //
        }
    }

    public int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    // 와이 파이 이름을 반환한다.
    public String getWifiSsid() {
        String ssid = "" ;

        try {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

            WifiInfo info = wifiManager.getConnectionInfo();
            ssid = info.getSSID();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        if( ssid.startsWith( "\"") ) {
            ssid = ssid.substring( 1 );
        }

        if( ssid.endsWith( "\"")) {
            ssid = ssid.substring( 0, ssid.length() - 1 );
        }

        return ssid;
    }

    // 아이피 주소를 반환한다.
    public String getIpAddr() {
        String ipAddr = "" ;

        try {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

            WifiInfo info = wifiManager.getConnectionInfo();

            int ipInt = info.getIpAddress();
            ipAddr = InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array())
                    .getHostAddress();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return ipAddr;
    }

    public void hideActionBar() {
        // If the Android version is lower than Jellybean, use this call to hide
        // the status bar.
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
            ActionBar actionBar = getActionBar();
            if( null != actionBar ) {
                actionBar.hide();
            }
        }
    }

    protected boolean checkLocationPermissions() {
        boolean permission = false ;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            permission = true;
        }

        Log.d( TAG, "checkPermission = " + permission );
        return permission ;
    }

    protected void requestLocationPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_ID
        );

        Log.d( TAG, "requestPermissions " );

    }

    protected boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean locationEnabled =  locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );

        Log.d( TAG, "locationEnabled = " + locationEnabled );

        return locationEnabled;
    }

    // 두 위경도 사이의 거리를 m로 구한다.
    public float[] getDistance(LatLng from, LatLng to ) {
        float[] results = new float[2];
        Location.distanceBetween( from.latitude, from.longitude, to.latitude, to.longitude, results );

        return results ;
    }

    public double prettyAngle60(double angleDegDecimal ) {
        double angle = angleDegDecimal %360 ;

        int ang = (int) angle ;
        double deg = angle - ang ;
        deg = 60*deg;

        angle = angle + deg ;

        return angle ;
    }

    // 와이파이 선택창을 오픈한다.
    public void openWifiSelector() {
        Toast.makeText( context, "Wi-Fi 선택 화면으로 이동합니다.", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        }, 2_000);
    }

    public boolean isActivityAlive() {
        return getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
    }

    public void startLocationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, LocationService.class));
        } else {
            startService(new Intent(this, LocationService.class));
        }
    }

}
