package com.corona;

import android.graphics.Color;
import com.google.android.gms.location.LocationRequest;
import java.text.SimpleDateFormat;

public interface ComInterface {

    String TAG = "sun_above";

    int PERMISSION_REQUEST_ID = 44;

    long ONE_DAY_TIME = 24*60*60*1000;
    long ONE_HOUR_TIME = 60*60*1000;

    long HANDLER_DELAY_SPINNER_ITEM = 200;

    long HANDLER_DELAY_05_MAX = 4000;
    long HANDLER_DELAY_04_LONG = 2000;
    long HANDLER_DELAY_03_NORMAL = 1000;
    long HANDLER_DELAY_02_SHORT = 500;
    long HANDLER_DELAY_01_MIN = 0;

    int gray = Color.parseColor("#d3d3d3") ;
    int yellow = Color.parseColor("#ffff00") ;
    int green = Color.parseColor("#00FF00") ;
    int black = Color.parseColor("#FFFFFF") ;
    int red = Color.parseColor("#FF0000") ;

    /*
    long LOCATION_REQUEST_INTERVAL = 0 ;
    float LOCATION_REQUEST_DISPLACEMENT_METERS = 1.0f;
    int LOCATION_REQUEST_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY ;
    */

    long LOCATION_REQUEST_INTERVAL = 10_000;
    float LOCATION_REQUEST_DISPLACEMENT_METERS = 5.0f;
    int LOCATION_REQUEST_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY ;

    SimpleDateFormat yyyyMMdd_HHmmSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat MMdd_HHmmSS = new SimpleDateFormat("MM-dd HH:mm:ss");
    SimpleDateFormat MMdd_HHmm = new SimpleDateFormat("MM-dd HH:mm");
    SimpleDateFormat HHmmSS = new SimpleDateFormat("HH:mm:ss");

    long CORONA_DB_GET_INTERVAL = 1*60*1_000;
    //long CORONA_DB_GET_INTERVAL = 10*1_000;

}
