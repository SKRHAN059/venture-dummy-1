package com.example.maptest1;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class Car {
    private int mCarID; //device's phone number
    private double mLat;
    private double mLong;
    private String tripID;

    public Car() {

    }
    public Car(int aID, LatLng aLatLng, String aTripID){
        this.mCarID = aID;
        this.mLat = aLatLng.latitude;
        this.mLong = aLatLng.longitude;
        this.tripID = aTripID;
    }

    public int getCarID(){
        return mCarID;
    }

    public double getCarLat(){
        return mLat;
    }

    public double getCarLong(){
        return mLong;
    }

    public String getTripID() {
        return tripID;
    }

    public String getCarName() {
        return Character.toString((char) mCarID);
    }

    public void setCarID(int aCarID) {
        mCarID = aCarID;
    }

    public void setLat(int aLat) {
        mLat = aLat;
    }

    public void setLong(int aLong) {
        mLong = aLong;
    }
}
