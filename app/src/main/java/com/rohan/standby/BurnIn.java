package com.rohan.standby;

/** OLED burn-in protection: the picture drifts a few pixels, changing every minute. Pure, so the range and the coverage margin are tested. */
final class BurnIn {
    static final float MAX_X = .0075f, MAX_Y = .012f;  // largest drift as a fraction of width / height
    /** Full-bleed backgrounds are drawn this much larger than the screen on every side, so no drift can expose a bare edge. */
    static final float OVERSCAN = .02f;
    /** {dx, dy} in pixels for the given minute since the epoch. */
    static float[] offset(long minute, float w, float h) {
        long m = Math.max(0, minute);
        return new float[]{((m % 7) - 3) * (MAX_X / 3f) * w, (((m / 7) % 7) - 3) * (MAX_Y / 3f) * h};
    }
}
