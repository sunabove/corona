package com.corona;

import lombok.Getter;
import lombok.Setter;

public class CoronaModel {

    @Getter @Setter private String name;
    @Getter @Setter private String type;
    @Getter @Setter private String version_number;
    @Getter @Setter private String feature;


    public CoronaModel() {
    }

    public CoronaModel(String name, String type, String version_number, String feature ) {
        this.name=name;
        this.type=type;
        this.version_number=version_number;
        this.feature=feature;
    }

}
