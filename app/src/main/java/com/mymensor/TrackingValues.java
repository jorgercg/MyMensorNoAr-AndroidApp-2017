package com.mymensor;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

public class TrackingValues {

    private int posX;
    private int posY;
    private int posXid;
    private int posYid;
    private int posZ;
    private int rotX;
    private int rotY;
    private int rotZ;
    private int vpNumber;
    private float rawX;
    private float rawY;
    private float rawZ;
    private float rawRotX;
    private float rawRotY;
    private float rawRotZ;

    public TrackingValues setTrackingValues(float[] rawTracking) {

        TrackingValues trackingValues = new TrackingValues();


        trackingValues.posX = Math.round(rawTracking[0]+Constants.xAxisTrackingCorrection);
        trackingValues.posY = Math.round(rawTracking[1]+Constants.yAxisTrackingCorrection);
        trackingValues.posXid = Math.round(rawTracking[0]);
        trackingValues.posYid = Math.round(rawTracking[1]);
        trackingValues.posZ = Math.round(rawTracking[2]);
        trackingValues.rotX = (int) Math.round(rawTracking[3]*(180.0f/Math.PI));
        trackingValues.rotY = (int) Math.round(rawTracking[4]*(180.0f/Math.PI));
        trackingValues.rotZ = (int) Math.round(rawTracking[5]*(180.0f/Math.PI));
        trackingValues.vpNumber = Math.round(rawTracking[6]);
        trackingValues.rawX = rawTracking[0];
        trackingValues.rawY = rawTracking[1];
        trackingValues.rawZ = rawTracking[2];
        trackingValues.rawRotX = rawTracking[3];
        trackingValues.rawRotY = rawTracking[4];
        trackingValues.rawRotZ = rawTracking[5];

        return trackingValues;
    }

    public int getVpNumberTrackedInPose(){
        return vpNumber;
    }

    public int getX(){
        return posX;
    }

    public int getY(){
        return posY;
    }

    public int getXid(){
        return posXid;
    }

    public int getYid(){
        return posYid;
    }

    public int getZ(){
        return posZ;
    }

    public int getEAX(){
        return rotX;
    }

    public int getEAY(){
        return rotY;
    }

    public int getEAZ(){
        return rotZ;
    }

    public float getRawRotX() {return rawRotX; }

    public float getRawRotY() {return rawRotY; }

    public float getRawRotZ() {return rawRotZ; }

    public float getRawX() {return rawX; }

    public float getRawY() {return rawY; }

    public float getRawZ() {return rawZ; }

}
