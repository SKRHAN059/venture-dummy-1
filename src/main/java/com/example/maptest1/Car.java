package com.example.maptest1;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.List;

public class Car {
    private int mCarID; //device's phone number
    private double mLat;
    private double mLong;
    private String tripID;
    private List<PolyPair> mPolylineData;
    private PolyPair mCarPolylineData;
    private Marker mMarker;

    public Car() {

    }
    public Car(int aID, LatLng aLatLng, String aTripID){
        this.mCarID = aID;
        this.mLat = aLatLng.latitude;
        this.mLong = aLatLng.longitude;
        this.tripID = aTripID;
        this.mPolylineData = new ArrayList<>();
        this.mCarPolylineData = null;
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

    public LatLng getLatLng() {
        return new LatLng(mLat, mLong);
    }

    public Marker getMarker() {
        return mMarker;
    }

    public List<PolyPair> getPolyineData() {
        return mPolylineData;
    }

    public PolyPair getCarPolylineData() {
        return mCarPolylineData;
    }

    public void setCarID(int aCarID) {
        mCarID = aCarID;
    }

    public void setLat(double aLat) {
        mLat = aLat;
    }

    public void setLong(double aLong) {
        mLong = aLong;
    }

    public void setMarker(Marker aMarker) {
        mMarker = aMarker;
    }

    public void setPolylineData(List<PolyPair> aList) {
        mPolylineData = aList;
    }

    public void setCarPolylineData(PolyPair aPolylineData) {
        mCarPolylineData = aPolylineData;
    }
}
