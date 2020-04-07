package com.corona;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

public class Corona implements Serializable {
    long id;
    long deleted, checked, notification ;
    long up_dt;

    String place;
    String patient;

    long visit_fr;
    long visit_to;
    float latitude = 0;
    float longitude = 0 ;
    float x ;
    float y ;

    String text;
    String content ;
    //String up_dt_str ;

    public Corona() {
    }

    public Corona(String place, String patient, long visit_fr, long visit_to) {
        this.place = place;
        this.patient = patient;
        this.visit_fr = visit_fr;
        this.visit_to = visit_to;
    }

    public LatLng getLatLng() {
        return new LatLng( latitude, longitude ) ;
    }

    public String getTitle() {
        String infection = 1 == this.checked ? "동선 겹침" : "" ;

        String title = String.format("%s / %s / %s  [%d] ", this.place, this.patient , infection, this.id );

        return title;
    }

    public int getItemRowBackgroundColor() {
        int color = Color.YELLOW ;
        if( 0 == checked ) {
            color =  Color.YELLOW ;
        } else {
            if( 0 == notification ) {
                color = Color.YELLOW ;
            } else if( 1 == notification ) {
                color = Color.parseColor( "#FF5722" ) ;
            } else if( 2 == notification ) {
                color = Color.parseColor( "#DF58B0" ) ;
            }
        }

        return color ;
    }
}
