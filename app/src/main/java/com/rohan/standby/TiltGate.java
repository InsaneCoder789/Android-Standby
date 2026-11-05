package com.rohan.standby;

/**
 * Decides when the phone is resting sideways at an angle (landscape on a stand). Pure logic with an injected
 * clock so it can be unit-tested. Feed it gravity readings; it says when to show and when to close.
 */
final class TiltGate {
    enum State { WAITING, ACTIVE, CLOSED }

    private static final long LOSE_GRACE_MS = 1200, GIVE_UP_MS = 3500;
    private final long delayMs; private long goodSince = -1, badSince = -1; private boolean active, closed;

    TiltGate(long delayMs) { this.delayMs = delayMs; }

    /** Axes are fixed to the hardware, so this is independent of rotation lock. Hysteresis: easier to stay than to start. */
    static boolean inside(float x, float y, float z, boolean alreadyActive) {
        double n = Math.sqrt(x * x + y * y + z * z); if (n < 1e-3) return false;
        return n > 7 && n < 12 && Math.abs(x) / n > (alreadyActive ? 0.64 : 0.72) && Math.abs(y) / n < (alreadyActive ? 0.42 : 0.32) && z / n > (alreadyActive ? 0.12 : 0.18) && z / n < (alreadyActive ? 0.80 : 0.72);
    }

    /** Plain-language reason the pose is or isn't accepted, for the Tilt test screen. */
    static String hint(float x, float y, float z) {
        double n = Math.sqrt(x * x + y * y + z * z); if (!(n > 1e-3)) return "Hold the phone still";
        double ax = Math.abs(x) / n, ay = Math.abs(y) / n, az = z / n;
        if (n < 7 || n > 12) return "Hold the phone still";
        if (az < -0.1) return "The screen is facing down";
        if (ay > .5) return "Turn the phone sideways, into landscape";
        if (az > .72) return "Too flat. Prop it up a little more";
        if (az < .18) return "Too upright. Lean it back a little";
        if (ax < .72) return "Turn it a bit further onto its side";
        return "In the right position";
    }
    /** Live numbers shown under the status. Kept free of Android so it can be tested. */
    static String report(float x, float y, float z) {
        double n = Math.sqrt(x * x + y * y + z * z); if (!(n > 1e-3)) return "No reading yet";
        double lean = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, z / n)))), side = Math.abs(x) / n * 100;
        return String.format(java.util.Locale.US, "Leaning back  %.0f\u00b0   (works from 10\u00b0 to 46\u00b0)\nSideways  %.0f%%   (needs 72%% or more)\nx %.1f    y %.1f    z %.1f", lean, side, x, y, z);
    }

    State update(float x, float y, float z, long now) {
        if (closed) return State.CLOSED;
        if (inside(x, y, z, active)) {
            badSince = -1; if (goodSince < 0) goodSince = now;
            if (!active && now - goodSince >= delayMs) active = true;
        } else {
            goodSince = -1; if (badSince < 0) badSince = now;
            if (active && now - badSince > LOSE_GRACE_MS) closed = true; else if (!active && now - badSince > GIVE_UP_MS) closed = true;
        }
        return closed ? State.CLOSED : active ? State.ACTIVE : State.WAITING;
    }
}
