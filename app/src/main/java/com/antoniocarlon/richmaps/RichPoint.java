/*
 * Copyright 2016 ANTONIO CARLON
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.antoniocarlon.richmaps;

import com.google.android.gms.maps.model.LatLng;

/**
 * Represents a point in a shape.
 */
public class RichPoint {
    private LatLng position;
    private Integer color = null;

    public RichPoint(final LatLng position) {
        this.position = position;
    }

    public RichPoint color(final Integer color) {
        this.color = color;
        return this;
    }

    public LatLng getPosition() {
        return position;
    }

    public Integer getColor() {
        return color;
    }
}