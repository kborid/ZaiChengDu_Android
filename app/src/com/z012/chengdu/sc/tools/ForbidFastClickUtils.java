package com.z012.chengdu.sc.tools;

public class ForbidFastClickUtils {

    private static final long MAX_DURATION = 500;

    private static long mStart = 0;

    public static boolean isFastClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - mStart < MAX_DURATION) {
            return true;
        }
        mStart = currentTime;
        return false;
    }
}
