package com.example.maptest1;

import com.google.android.gms.maps.model.Polyline;
import com.google.maps.model.DirectionsLeg;

public class PolyPair {
    private Polyline polyline;
    private DirectionsLeg directionsLeg;

    public PolyPair(Polyline p, DirectionsLeg d) {
        polyline = p;
        directionsLeg = d;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public DirectionsLeg getDirectionsLeg() {
        return directionsLeg;
    }

    public void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }

    public void setDirectionsLeg(DirectionsLeg directionsLeg) {
        this.directionsLeg = directionsLeg;
    }
}
