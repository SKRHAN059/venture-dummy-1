package com.example.maptest1;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Trip implements Destination {
    private String tripID;
    private int travelers;
    private String destAddress;
    private double tLat;
    private double tLong;
    private List<Car> mCars;

    public Trip(LatLng aLatLng, String aAddress, String aID) {
        tLat = aLatLng.latitude;
        tLong = aLatLng.longitude;
        destAddress = aAddress;
        tripID = aID;
        mCars = new ArrayList<>();
    }
    public Trip(LatLng aLatLng, String aAddress) {
        Random rand = new Random();
        tripID = "";
        for (int i = 0; i < 4; i++) {
            int num = rand.nextInt(101 - 65 + 1) + 65;
            if (num > 90) {
                tripID += num % 10;
            } else {
                tripID += (char) num;
            }
        }
        addTraveler();
        tLat = aLatLng.latitude;
        tLong = aLatLng.longitude;
        destAddress = aAddress;
        mCars = new ArrayList<>();
    }

    public String getTripID() {
        return tripID;
    }

    public String getDestAddress() {
        return destAddress;
    }

    public double getTripLat() {
        return tLat;
    }

    public double getTripLong() {
        return tLong;
    }

    public List<Car> getCars() {
        return mCars;
    }

    public void addTraveler(){
        travelers++;
    }

    public void removeTraveler(){
        travelers--;
        checkTravelers();
    }

    public void checkTravelers(){
        if (travelers <= 0) {
            deleteDestination();
        }
    }

    public void deleteDestination(){
        //TODO: delete trip
    }

    public void addCar(Car aCar) {
        mCars.add(aCar);
    }
}
