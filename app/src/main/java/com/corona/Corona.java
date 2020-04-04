package com.corona;

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

    String infection ;
    String title;
    String text;
    String content ;
    String up_dt_str ;

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
}
