package com.mymensor.filters;

import com.mymensor.filters.NoneFilter;

public class NoneARFilter extends NoneFilter implements ARFilter {
    @Override
    public float[] getPose() {
        return null;
    }
}
