package com.mymensor.filters;

import org.opencv.core.Mat;

public class NoneFilter implements Filter {

    @Override
    public void  dispose() {
        // Do nothing at all.
    }

    @Override
    public void apply(final Mat src, final int IsHudOn) {
        // Do nothing.
    }
}
