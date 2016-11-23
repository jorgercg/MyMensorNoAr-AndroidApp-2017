package com.mymensor.filters;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

import java.io.IOException;

public final class IdMarkerDetectionFilter implements ARFilter {

    // The address of the native object.
    private long mSelfAddr;

    // An adaptor that provides the camera's projection matrix.
    private final MatOfDouble mCameraMatrix;

    static {
        // Load the native library if it is not already loaded.
        System.loadLibrary("MyMensor");
    }

    public IdMarkerDetectionFilter(final Context context,
                                   final int qtyVps,
                                   final MatOfDouble cameraMatrix,
                                   final float realSize)
            throws IOException {
        mSelfAddr = newSelf(qtyVps, realSize);
        mCameraMatrix = cameraMatrix;
    }

    @Override
    public void dispose() {
        deleteSelf(mSelfAddr);
        mSelfAddr = 0;
    }

    @Override
    protected void finalize() throws Throwable {
        dispose();
    }

    @Override
    public float[] getPose() {
        return getPose(mSelfAddr);
    }

    @Override
    public void apply(final Mat src, final int isHudOn) {
        final Mat projection = mCameraMatrix;
        apply(mSelfAddr, src.getNativeObjAddr(), isHudOn, projection.getNativeObjAddr());
    }

    private static native long newSelf(int qtyVps, double realSize);
    private static native void deleteSelf(long selfAddr);
    private static native float[] getPose(long selfAddr);
    private static native void apply(long selfAddr, long srcAddr, int isHudOn, long projectionAddr);
}