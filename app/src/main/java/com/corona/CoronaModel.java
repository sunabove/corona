package com.corona;

import lombok.Getter;
import lombok.Setter;

public class CoronaModel {

    @Getter @Setter private String place;
    @Getter @Setter private String patient;
    @Getter @Setter private String visitTimeFr;
    @Getter @Setter private String visitTimeTo;

    public CoronaModel() {
    }

    public CoronaModel(String place, String patient, String visitTimeFr, String visitTimeTo) {
        this.place = place;
        this.patient = patient;
        this.visitTimeFr = visitTimeFr;
        this.visitTimeTo = visitTimeTo;
    }

}
