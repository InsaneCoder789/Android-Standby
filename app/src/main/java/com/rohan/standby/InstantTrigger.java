package com.rohan.standby;

/**
 * Decides the moment Instant mode should open Standby: the phone is charging AND resting at the landscape angle for a short hold, and the screen
 * situation allows it (locked or off). Fires once, then stays disarmed until the phone has left the position for a moment, so closing Standby
 * (power button, swiping away) is respected instead of reopening at once. Pure logic with an injected clock; fully unit-tested.
 */
final class InstantTrigger {
    static final long LEAVE_MS = 1500;
    private final long holdMs; private boolean armed = true; private long goodSince = -1, awaySince = -1;
    InstantTrigger(long holdMs) { this.holdMs = holdMs; }

    /** @param allowed screen/lock/schedule conditions are met right now. Returns true exactly when Standby should open. */
    boolean update(long now, boolean charging, boolean allowed, float x, float y, float z) {
        if (!charging) { armed = true; goodSince = -1; awaySince = -1; return false; }       // unplugging resets everything
        if (!armed) {
            if (!TiltGate.inside(x, y, z, true)) { if (awaySince < 0) awaySince = now; if (now - awaySince >= LEAVE_MS) { armed = true; awaySince = -1; goodSince = -1; } }
            else awaySince = -1;
            return false;
        }
        if (allowed && TiltGate.inside(x, y, z, false)) {
            if (goodSince < 0) goodSince = now;
            if (now - goodSince >= holdMs) { armed = false; goodSince = -1; awaySince = -1; return true; }
        } else goodSince = -1;
        return false;
    }
    /** The screen was switched off (power button): the user is locking the phone, so Standby may open again at the angle. */
    void rearm() { armed = true; goodSince = -1; awaySince = -1; }
    boolean armed() { return armed; }
}
