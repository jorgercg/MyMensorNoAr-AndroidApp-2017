package com.mymensor;

public class TrackingValues {

    private int posX;
    private int posY;
    private int posZ;
    private int rotX;
    private int rotY;
    private int rotZ;
    private int vpNumber;

    public TrackingValues setTrackingValues(float[] rawTracking) {

        TrackingValues trackingValues = new TrackingValues();
        trackingValues.posX = Math.round(rawTracking[0]+Constants.xAxisTrackingCorrection);
        trackingValues.posY = Math.round(rawTracking[1]+Constants.yAxisTrackingCorrection);
        trackingValues.posZ = Math.round(rawTracking[2]);
        trackingValues.rotX = (int) Math.round(rawTracking[3]*(180.0f/Math.PI));
        trackingValues.rotY = (int) Math.round(rawTracking[4]*(180.0f/Math.PI));
        trackingValues.rotZ = (int) Math.round(rawTracking[5]*(180.0f/Math.PI));
        trackingValues.vpNumber = Math.round(rawTracking[6]);

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

}
