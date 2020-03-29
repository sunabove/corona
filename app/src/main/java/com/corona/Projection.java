package com.corona;

import com.google.android.gms.maps.model.LatLng;

import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

public class Projection {

    private static final CRSFactory factory = new CRSFactory();
    private static final String wgs84 = "EPSG:4326" ;
    private static final String utm_k = "EPSG:5179" ;
    private static final CoordinateReferenceSystem srcCrs = factory.createFromName( wgs84 );
    private static final CoordinateReferenceSystem dstCrs = factory.createFromName( utm_k );

    private static final BasicCoordinateTransform transform = new BasicCoordinateTransform(srcCrs, dstCrs);

    public static ProjCoordinate convertToUtmK(LatLng latLng) {
        return convertToUtmK( latLng.latitude, latLng.longitude );
    }

    public static ProjCoordinate convertToUtmK(double latitude, double longitude) {
        ProjCoordinate src = new ProjCoordinate( longitude, latitude ) ;
        ProjCoordinate dst = new ProjCoordinate();

        transform.transform( src, dst);
        System.out.println( "src x = " + src.x + ", y = " + src.y + ", dst x = " + dst.x + ", y = " + dst.y);

        return dst;
    }
}
