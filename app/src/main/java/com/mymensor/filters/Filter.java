package com.mymensor.filters;

import org.opencv.core.Mat;

public interface Filter {
    public abstract void dispose();
    public abstract void apply(final Mat src, final int isHudOn);
}
