package com.example.maptest1;

public class Car {
    private String carID; //device's phone number
    private double cLat;
    private double cLong;
    private String tripID;

    public Car(String aID, double aLat, double aLong){
        this.carID = aID;
        this.cLat = aLat;
        this.cLong = aLong;
    }

    public String getCarID(){
        return carID;
    }

    public double getCarLat(){
        return cLat;
    }

    public double getCarLong(){
        return cLong;
    }
}
