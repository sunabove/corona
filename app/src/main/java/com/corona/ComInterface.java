package com.corona;

import android.graphics.Color;

import com.google.android.gms.location.LocationRequest;

import java.text.SimpleDateFormat;

/**
 * Created by sunabove on 2016-02-19.
 */
public interface ComInterface {

    public static final String TAG = "sun_above";

    public static final long HANDLER_DELAY_SPINNER_ITEM = 200;

    public static final long HANDLER_DELAY_05_MAX = 4000;
    public static final long HANDLER_DELAY_04_LONG = 2000;
    public static final long HANDLER_DELAY_03_NORMAL = 1000;
    public static final long HANDLER_DELAY_02_SHORT = 500;
    public static final long HANDLER_DELAY_01_MIN = 0;

    public static final int gray = Color.parseColor("#d3d3d3") ;
    public static final int yellow = Color.parseColor("#ffff00") ;
    public static final int green = Color.parseColor("#00FF00") ;
    public static final int black = Color.parseColor("#FFFFFF") ;
    public static final int red = Color.parseColor("#FF0000") ;

    /*
    public static final long LOCATION_REQUEST_INTERVAL = 0 ;
    public static final float LOCATION_REQUEST_DISPLACEMENT_METERS = 1.0f;
    public static final int LOCATION_REQUEST_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY ;
    */

    public static final long LOCATION_REQUEST_INTERVAL = 10_000;
    public static final float LOCATION_REQUEST_DISPLACEMENT_METERS = 5.0f;
    public static final int LOCATION_REQUEST_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY ;

    public static final SimpleDateFormat yyyMMdd_HHmmSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final long CORONA_DB_GET_INTERVAL = 1*60*1_000;
    //public static final long CORONA_DB_GET_INTERVAL = 10*1_000;

}
