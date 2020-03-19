package com.corona;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class GpsLog extends ArrayList<LatLng> {

    // 두 위경도 사이의 거리를 m로 구한다.
    private float[] getDistance(LatLng from, LatLng to, float [] results ) {
        Location.distanceBetween( from.latitude, from.longitude, to.latitude, to.longitude, results );

        return results ;
    }

    public double getHeading( double def ) {
        double heading = 0.0;

        int size = this.size();

        if( 1 > size ) {
            return def ;
        }

        LatLng lastLatLng = this.get( size - 1 );

        LatLng latLng ;
        float[] results = new float[2];

        for( int i = 2 ; i < size ; i ++ ) {
            latLng = this.get( i );

            this.getDistance( latLng, lastLatLng, results );

            if( 2.0f < results[0] ) {
                return results[1];
            }
        }

        return results[1];
    }
}
