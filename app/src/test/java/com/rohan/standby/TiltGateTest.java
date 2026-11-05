package com.rohan.standby;

import static org.junit.Assert.*;
import static com.rohan.standby.TiltGate.State.*;
import org.junit.Test;

public class TiltGateTest {
    private static final float[] SIDEWAYS = {9.4f, 0.3f, 3.2f}, FLAT = {0f, 0f, 9.8f}, PORTRAIT = {0f, 9.8f, 0f};

    private static TiltGate.State feed(TiltGate g, float[] v, long t) { return g.update(v[0], v[1], v[2], t); }

    // ── poses ──
    @Test public void sidewaysLeftAndRightAreBothAccepted() { assertTrue(TiltGate.inside(9.4f, .3f, 3.2f, false)); assertTrue(TiltGate.inside(-9.4f, .3f, 3.2f, false)); }
    @Test public void flatOnTableIsRejected() { assertFalse(TiltGate.inside(0, 0, 9.8f, false)); }
    @Test public void portraitIsRejected() { assertFalse(TiltGate.inside(0, 9.8f, 0, false)); assertFalse(TiltGate.inside(0, -9.8f, 0, false)); }
    @Test public void faceDownIsRejected() { assertFalse(TiltGate.inside(0, 0, -9.8f, false)); assertFalse(TiltGate.inside(9.4f, .3f, -3.2f, false)); }
    @Test public void perfectlyVerticalLandscapeIsRejectedToStart() { assertFalse(TiltGate.inside(9.8f, 0, 0, false)); }
    @Test public void leaningTooFarBackIsRejected() { assertFalse(TiltGate.inside(4f, 0, 9f, false)); }
    @Test public void zeroVectorDoesNotBlowUp() { assertFalse(TiltGate.inside(0, 0, 0, false)); assertFalse(TiltGate.inside(0, 0, 0, true)); }
    @Test public void freeFallAndShakingAreRejected() { assertFalse(TiltGate.inside(1, 1, 1, false)); assertFalse(TiltGate.inside(15, 1, 5, false)); assertFalse(TiltGate.inside(Float.NaN, 0, 0, false)); }
    @Test public void hysteresisIsEasierToStayThanToStart() {
        float x = 9.7f, z = 1.4f; // z/n ~ 0.14: below the start threshold (0.18) but above the stay threshold (0.12)
        assertFalse(TiltGate.inside(x, 0, z, false)); assertTrue(TiltGate.inside(x, 0, z, true));
    }

    // ── timing ──
    @Test public void activatesOnlyAfterTheDelay() {
        TiltGate g = new TiltGate(1800);
        assertEquals(WAITING, feed(g, SIDEWAYS, 0)); assertEquals(WAITING, feed(g, SIDEWAYS, 1799)); assertEquals(ACTIVE, feed(g, SIDEWAYS, 1800));
    }
    @Test public void respectsQuickAndSlowDelays() {
        TiltGate quick = new TiltGate(900); feed(quick, SIDEWAYS, 0); assertEquals(ACTIVE, feed(quick, SIDEWAYS, 900));
        TiltGate slow = new TiltGate(3000); feed(slow, SIDEWAYS, 0); assertEquals(WAITING, feed(slow, SIDEWAYS, 2999)); assertEquals(ACTIVE, feed(slow, SIDEWAYS, 3000));
    }
    @Test public void interruptedHoldRestartsTheDelay() {
        TiltGate g = new TiltGate(1800); feed(g, SIDEWAYS, 0); feed(g, SIDEWAYS, 1000); feed(g, FLAT, 1200);
        assertEquals(WAITING, feed(g, SIDEWAYS, 1400)); assertEquals(WAITING, feed(g, SIDEWAYS, 3000)); assertEquals(ACTIVE, feed(g, SIDEWAYS, 3200));
    }
    @Test public void givesUpIfNeverPlacedSideways() {
        TiltGate g = new TiltGate(1800); assertEquals(WAITING, feed(g, FLAT, 0)); assertEquals(WAITING, feed(g, FLAT, 3500)); assertEquals(CLOSED, feed(g, FLAT, 3501));
    }
    @Test public void closesShortlyAfterBeingPickedUp() {
        TiltGate g = new TiltGate(900); feed(g, SIDEWAYS, 0); assertEquals(ACTIVE, feed(g, SIDEWAYS, 900));
        assertEquals(ACTIVE, feed(g, FLAT, 1000)); assertEquals(ACTIVE, feed(g, FLAT, 2200)); assertEquals(CLOSED, feed(g, FLAT, 2201));
    }
    @Test public void aBriefWobbleDoesNotClose() {
        TiltGate g = new TiltGate(900); feed(g, SIDEWAYS, 0); feed(g, SIDEWAYS, 900);
        assertEquals(ACTIVE, feed(g, PORTRAIT, 1000)); assertEquals(ACTIVE, feed(g, PORTRAIT, 2000)); assertEquals(ACTIVE, feed(g, SIDEWAYS, 2100));
        assertEquals(ACTIVE, feed(g, FLAT, 3000)); assertEquals(ACTIVE, feed(g, FLAT, 4100)); // grace timer restarted when the pose came back
    }
    @Test public void onceClosedItStaysClosed() {
        TiltGate g = new TiltGate(900); feed(g, SIDEWAYS, 0); feed(g, SIDEWAYS, 900); feed(g, FLAT, 1000); assertEquals(CLOSED, feed(g, FLAT, 2300));
        assertEquals(CLOSED, feed(g, SIDEWAYS, 2400)); assertEquals(CLOSED, feed(g, SIDEWAYS, 9000));
    }
    @Test public void staysActiveWhileSidewaysForALongTime() {
        TiltGate g = new TiltGate(1800); for (long t = 0; t <= 600000; t += 200) { TiltGate.State s = feed(g, SIDEWAYS, t); if (t >= 1800) assertEquals(ACTIVE, s); }
    }
    @Test public void survivesSensorNoiseAroundTheStandingAngle() {
        TiltGate g = new TiltGate(900); java.util.Random r = new java.util.Random(42); long t = 0; TiltGate.State s = WAITING;
        for (int i = 0; i < 3000; i++, t += 200) s = g.update(9.4f + (float) r.nextGaussian() * .25f, .3f + (float) r.nextGaussian() * .25f, 3.2f + (float) r.nextGaussian() * .25f, t);
        assertEquals(ACTIVE, s);
    }
    @Test public void worksWithTheClockStartingAtAnyValue() {
        TiltGate g = new TiltGate(900); long base = 5_000_000_000L; feed(g, SIDEWAYS, base); assertEquals(ACTIVE, feed(g, SIDEWAYS, base + 900));
    }

    // ── Tilt test screen text (a bad format string here once crashed the screen) ──
    @Test public void reportNeverThrowsForAnyReading() {
        float[] vals = {-20f, -9.8f, -1f, 0f, 0.3f, 3.2f, 9.4f, 9.8f, 15f, Float.NaN, Float.POSITIVE_INFINITY};
        for (float a : vals) for (float b : vals) for (float c : vals) { assertNotNull(TiltGate.report(a, b, c)); assertNotNull(TiltGate.hint(a, b, c)); }
    }
    @Test public void reportMentionsTheRealNumbers() { String r = TiltGate.report(9.4f, .3f, 3.2f); assertTrue(r, r.contains("Leaning back  19")); assertTrue(r, r.contains("Sideways  95%")); assertTrue(r, r.contains("72% or more")); assertTrue(r, r.contains("x 9.4")); }
    @Test public void hintsExplainEachWrongPose() {
        assertEquals("In the right position", TiltGate.hint(9.4f, .3f, 3.2f)); assertEquals("Too flat. Prop it up a little more", TiltGate.hint(0, 0, 9.8f)); assertEquals("Too upright. Lean it back a little", TiltGate.hint(9.8f, 0, 0));
        assertEquals("Turn the phone sideways, into landscape", TiltGate.hint(0, 9.8f, 0)); assertEquals("The screen is facing down", TiltGate.hint(0, 0, -9.8f)); assertEquals("Hold the phone still", TiltGate.hint(0, 0, 0));
    }
    @Test public void statusAgreesWithTheHint() { for (float z = -9; z <= 9; z += 1.5f) for (float x = -9; x <= 9; x += 1.5f) { boolean ok = TiltGate.inside(x, 0.3f, z, false); assertEquals("x=" + x + " z=" + z, ok, TiltGate.hint(x, 0.3f, z).equals("In the right position")); } }
}
