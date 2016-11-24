package com.mymensor.filters;

import java.io.IOException;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgcodecs.Imgcodecs;

import android.content.Context;

public final class ImageDetectionFilter implements ARFilter {

    // The address of the native object.
    private long mSelfAddr;

    // An adaptor that provides the camera's projection matrix.
    private final MatOfDouble mCameraMatrix;

    static {
        // Load the native library if it is not already loaded.
        System.loadLibrary("MyMensor");
    }

    public ImageDetectionFilter(final Context context,
                                final Object markerBuffer[],
                                final int qtyVps,
                                final MatOfDouble cameraMatrix,
                                final double realSize)
            throws IOException {
        mSelfAddr = newSelf(markerBuffer, qtyVps, realSize);
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
    public void apply(final Mat src, final int isHudOn, final int isSingleImage) {
        final Mat projection = mCameraMatrix;
        apply(mSelfAddr, src.getNativeObjAddr(), isHudOn, isSingleImage, projection.getNativeObjAddr());
    }

    private static native long newSelf(Object markerBuffer[], int qtyVps, double realSize);
    private static native void deleteSelf(long selfAddr);
    private static native float[] getPose(long selfAddr);
    private static native void apply(long selfAddr, long srcAddr, int isHudOn, int isSingleImage, long projectionAddr);
}