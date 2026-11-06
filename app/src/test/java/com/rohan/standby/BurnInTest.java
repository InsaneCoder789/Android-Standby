package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class BurnInTest {
    @Test public void driftStaysWithinTheDocumentedMaximum() { for (long m = 0; m < 5000; m++) { float[] o = BurnIn.offset(m, 2340, 1080); assertTrue(Math.abs(o[0]) <= BurnIn.MAX_X * 2340 + .01f); assertTrue(Math.abs(o[1]) <= BurnIn.MAX_Y * 1080 + .01f); } }
    @Test public void overscanAlwaysCoversTheWorstCaseDrift() { assertTrue(BurnIn.OVERSCAN > BurnIn.MAX_X); assertTrue(BurnIn.OVERSCAN > BurnIn.MAX_Y); for (long m = 0; m < 400; m++) { float[] o = BurnIn.offset(m, 1000, 1000); assertTrue(Math.abs(o[0]) < BurnIn.OVERSCAN * 1000); assertTrue(Math.abs(o[1]) < BurnIn.OVERSCAN * 1000); } }
    @Test public void itActuallyMovesAndRepeatsEveryFortyNineMinutes() { assertNotEquals(BurnIn.offset(0, 100, 100)[0], BurnIn.offset(1, 100, 100)[0], 0); assertArrayEquals(BurnIn.offset(5, 100, 100), BurnIn.offset(54, 100, 100), 1e-4f); }
    @Test public void centreAtTheMiddleOfTheCycle() { float[] o = BurnIn.offset(3 + 7 * 3, 100, 100); assertEquals(0f, o[0], 1e-4f); assertEquals(0f, o[1], 1e-4f); }
    @Test public void badMinutesAreHarmless() { float[] o = BurnIn.offset(-5, 100, 100); assertNotNull(o); assertTrue(Math.abs(o[0]) < 5); }
}
