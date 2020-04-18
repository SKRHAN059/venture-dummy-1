package com.example.maptest1;

import java.util.Random;

public class Trip implements Destination {
    private String tripID;
    private int travelers;
    private String destAddress;
    private double tLat;
    private double tLong;

    public Trip(double aLat, double aLong, String aAddress) {
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
        tripID = "<br /><font color=#2B97EB>"+tripID+"</font>";
        addTraveler();
        this.tLat = aLat;
        this.tLong = aLong;
        this.destAddress = aAddress;
    }

    public String getTripID() {
        System.out.println("\n\n\nTRIP ID: " + tripID + "\n\n\n");
        return this.tripID;
    }

    public String getDestAddress() {
        return this.destAddress;
    }

    public double getTripLat() {
        return this.tLat;
    }

    public double getTripLong() {
        return this.tLong;
    }

    public void addTraveler(){
        this.travelers++;
    }

    public void removeTraveler(){
        this.travelers--;
        checkTravelers();
    }

    public void checkTravelers(){
        if (this.travelers <= 0) {
            deleteDestination();
        }
    }

    public void deleteDestination(){
        //TODO: delete trip
    }
}
