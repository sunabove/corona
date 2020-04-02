package com.corona;

public class Corona {
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
    String title, text, content ;
    String up_dt_str ;

    public Corona() {
    }

    public Corona(String place, String patient, long visit_fr, long visit_to) {
        this.place = place;
        this.patient = patient;
        this.visit_fr = visit_fr;
        this.visit_to = visit_to;
    }
}
