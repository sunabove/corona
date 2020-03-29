package com.corona;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

public class Proj implements ComInterface {

    private final CRSFactory factory = new CRSFactory();
    private final String wgs84 = "EPSG:4326" ;
    private final String utm_k = "EPSG:5179" ;
    private final CoordinateReferenceSystem srcCrs = factory.createFromName( wgs84 );
    private final CoordinateReferenceSystem dstCrs = factory.createFromName( utm_k );

    private final BasicCoordinateTransform transform = new BasicCoordinateTransform(srcCrs, dstCrs);

    private static final Proj projection = new Proj();

    public static Proj projection() {
        return projection;
    }

    private Proj() {
        // do nothing.
    }

    public ProjCoordinate convertToUtmK(LatLng latLng) {
        return convertToUtmK( latLng.latitude, latLng.longitude );
    }

    public ProjCoordinate convertToUtmK(double latitude, double longitude) {
        ProjCoordinate src = new ProjCoordinate( longitude, latitude ) ;
        ProjCoordinate dst = new ProjCoordinate();

        transform.transform( src, dst);
        Log.d( TAG, "projection src x = " + src.x + ", y = " + src.y + ", dst x = " + dst.x + ", y = " + dst.y);

        return dst;
    }
}
