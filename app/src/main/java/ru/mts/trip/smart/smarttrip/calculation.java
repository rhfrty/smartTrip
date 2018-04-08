package ru.mts.trip.smart.smarttrip;

/* 10:0.12:40:ford* */

public class calculation {
    public int radius, errCode;
    public static void main(String[] args) {

    }

    public int devided(String obdSend) {
        String[] readings = obdSend.split(":");
        float radiusFloat = (Float.parseFloat(readings[2]) * Float.parseFloat(readings[1]) * 100 * 1000) / Float.parseFloat(readings[0]);
        radius = (int) radiusFloat;
        errCode = Integer.parseInt(readings[3]);
        return radius;
    }


}