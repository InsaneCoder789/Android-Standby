package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class InstantTriggerTest {
    private static final float[] SIDE = {9.4f, 0.3f, 3.2f}, FLAT = {0f, 0f, 9.8f}, UPRIGHT = {0f, 9.8f, 0f}; private static final long HOLD = 1000;
    private static boolean step(InstantTrigger t, long ms, boolean charging, boolean allowed, float[] v) { return t.update(ms, charging, allowed, v[0], v[1], v[2]); }

    @Test public void firesOnceAfterTheHoldWhileChargingAtTheAngle() { InstantTrigger t = new InstantTrigger(HOLD); assertFalse(step(t, 0, true, true, SIDE)); assertFalse(step(t, 999, true, true, SIDE)); assertTrue(step(t, 1000, true, true, SIDE)); }
    @Test public void neverFiresWhenNotCharging() { InstantTrigger t = new InstantTrigger(HOLD); for (long ms = 0; ms < 60_000; ms += 200) assertFalse(step(t, ms, false, true, SIDE)); }
    @Test public void neverFiresAtTheWrongAngle() { InstantTrigger t = new InstantTrigger(HOLD); for (long ms = 0; ms < 20_000; ms += 200) { assertFalse(step(t, ms, true, true, FLAT)); assertFalse(step(t, ms + 1, true, true, UPRIGHT)); } }
    @Test public void doesNotFireWhileTheScreenSituationDisallowsIt() { InstantTrigger t = new InstantTrigger(HOLD); for (long ms = 0; ms < 30_000; ms += 200) assertFalse(step(t, ms, true, false, SIDE)); }
    @Test public void firesAsSoonAsItBecomesAllowedAndHeldLongEnough() { InstantTrigger t = new InstantTrigger(HOLD); for (long ms = 0; ms < 5000; ms += 200) step(t, ms, true, false, SIDE); assertFalse(step(t, 5000, true, true, SIDE)); assertTrue(step(t, 6000, true, true, SIDE)); }
    @Test public void aWobbleBeforeTheHoldRestartsIt() { InstantTrigger t = new InstantTrigger(HOLD); step(t, 0, true, true, SIDE); step(t, 600, true, true, FLAT); assertFalse(step(t, 700, true, true, SIDE)); assertFalse(step(t, 1600, true, true, SIDE)); assertTrue(step(t, 1700, true, true, SIDE)); }
    @Test public void doesNotRepeatWhileThePhoneStaysPut() { InstantTrigger t = new InstantTrigger(HOLD); step(t, 0, true, true, SIDE); assertTrue(step(t, 1000, true, true, SIDE)); for (long ms = 1200; ms < 600_000; ms += 500) assertFalse(step(t, ms, true, true, SIDE)); }
    @Test public void closingStandbyIsRespectedUntilThePhoneIsLiftedAway() {
        InstantTrigger t = new InstantTrigger(HOLD); step(t, 0, true, true, SIDE); assertTrue(step(t, 1000, true, true, SIDE));
        for (long ms = 2000; ms < 20_000; ms += 200) assertFalse("still in place after being closed", step(t, ms, true, true, SIDE));
        assertFalse(t.armed());
    }
    @Test public void liftedAwayThenPlacedAgainFiresAgain() {
        InstantTrigger t = new InstantTrigger(HOLD); step(t, 0, true, true, SIDE); assertTrue(step(t, 1000, true, true, SIDE));
        for (long ms = 3000; ms <= 5000; ms += 200) step(t, ms, true, true, FLAT);   // picked up for two seconds
        assertTrue(t.armed()); assertFalse(step(t, 5200, true, true, SIDE)); assertTrue(step(t, 6200, true, true, SIDE));
    }
    @Test public void aQuickBumpDoesNotReArm() { InstantTrigger t = new InstantTrigger(HOLD); step(t, 0, true, true, SIDE); assertTrue(step(t, 1000, true, true, SIDE)); step(t, 2000, true, true, FLAT); step(t, 2800, true, true, FLAT); step(t, 2900, true, true, SIDE); assertFalse(t.armed()); for (long ms = 3000; ms < 9000; ms += 200) assertFalse(step(t, ms, true, true, SIDE)); }
    @Test public void unpluggingResetsAndRearms() { InstantTrigger t = new InstantTrigger(HOLD); step(t, 0, true, true, SIDE); assertTrue(step(t, 1000, true, true, SIDE)); assertFalse(step(t, 1500, false, true, SIDE)); assertTrue(t.armed()); assertFalse(step(t, 2000, true, true, SIDE)); assertTrue(step(t, 3000, true, true, SIDE)); }
    @Test public void bothLandscapeDirectionsWork() { InstantTrigger t = new InstantTrigger(HOLD); step(t, 0, true, true, new float[]{-9.4f, 0.3f, 3.2f}); assertTrue(step(t, 1000, true, true, new float[]{-9.4f, 0.3f, 3.2f})); }
    @Test public void zeroHoldFiresOnTheFirstGoodReading() { InstantTrigger t = new InstantTrigger(0); assertTrue(step(t, 5, true, true, SIDE)); }
    @Test public void noisyButSteadyPlacementStillFires() {
        InstantTrigger t = new InstantTrigger(HOLD); java.util.Random r = new java.util.Random(7); boolean fired = false;
        for (long ms = 0; ms < 5000 && !fired; ms += 200) fired = t.update(ms, true, true, 9.4f + (float) r.nextGaussian() * .2f, .3f + (float) r.nextGaussian() * .2f, 3.2f + (float) r.nextGaussian() * .2f); assertTrue(fired);
    }
    @Test public void screenOffRearmsAfterFiring() {
        InstantTrigger t = new InstantTrigger(1000); float[] p = SIDE;
        long now = 0; boolean fired = false; for (; now <= 1500 && !fired; now += 100) fired = t.update(now, true, true, p[0], p[1], p[2]);
        assertTrue(fired); assertFalse(t.armed());
        t.rearm(); assertTrue(t.armed());
    }
}
