package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class ViewTurnTest {
    private static final float PW = 1280, PH = 2856;
    private static void near(float[] a, float x, float y) { assertEquals(x, a[0], .01f); assertEquals(y, a[1], .01f); }

    @Test public void landscapeWindowIsLeftAlone() { near(ViewTurn.logical(0, 100, 200, 2856, 1280), 100, 200); assertEquals(0, ViewTurn.turn(2856, 1280, 9f)); assertEquals(0, ViewTurn.turn(2856, 1280, -9f)); }
    @Test public void portraitWindowTurnsTowardTheLowSide() { assertEquals(90, ViewTurn.turn(PW, PH, 9.4f)); assertEquals(270, ViewTurn.turn(PW, PH, -9.4f)); assertEquals(90, ViewTurn.turn(PW, PH, 0f)); }
    @Test public void landscapeSpaceHasSwappedDimensions() { float[] l = ViewTurn.logical(90, 0, 0, PW, PH); assertEquals(PH, l[2], 0); assertEquals(PW, l[3], 0); l = ViewTurn.logical(270, 0, 0, PW, PH); assertEquals(PH, l[2], 0); assertEquals(PW, l[3], 0); }
    @Test public void turn90MapsLandscapeCornersOntoTheWindow() {
        near(ViewTurn.physical(90, 0, 0, PW, PH), PW, 0);       // landscape top-left lands on the window's top-right
        near(ViewTurn.physical(90, PH, 0, PW, PH), PW, PH);     // landscape top-right lands bottom-right
        near(ViewTurn.physical(90, 0, PW, PW, PH), 0, 0);       // landscape bottom-left lands top-left
        near(ViewTurn.physical(90, PH, PW, PW, PH), 0, PH);     // landscape bottom-right lands bottom-left
    }
    @Test public void turn270MapsLandscapeCornersOntoTheWindow() {
        near(ViewTurn.physical(270, 0, 0, PW, PH), 0, PH);      // landscape top-left lands on the window's bottom-left
        near(ViewTurn.physical(270, PH, 0, PW, PH), 0, 0);
        near(ViewTurn.physical(270, 0, PW, PW, PH), PW, PH);
        near(ViewTurn.physical(270, PH, PW, PW, PH), PW, 0);
    }
    @Test public void touchMappingIsTheExactInverseOfDrawing() {
        for (int turn : new int[]{0, 90, 270}) {
            float w = turn == 0 ? PH : PH, h = PW; // faces always see a 2856 x 1280 landscape space
            for (float u = 0; u <= w; u += w / 7) for (float v = 0; v <= h; v += h / 5) {
                float[] ph = ViewTurn.physical(turn, u, v, turn == 0 ? PH : PW, turn == 0 ? PW : PH);
                float[] back = ViewTurn.logical(turn, ph[0], ph[1], turn == 0 ? PH : PW, turn == 0 ? PW : PH);
                assertEquals("u turn=" + turn, u, back[0], .01f); assertEquals("v turn=" + turn, v, back[1], .01f);
            }
        }
    }
    @Test public void aTapAtTheCentreStaysAtTheCentre() { for (int t : new int[]{90, 270}) { float[] l = ViewTurn.logical(t, PW / 2, PH / 2, PW, PH); assertEquals(PH / 2, l[0], .01f); assertEquals(PW / 2, l[1], .01f); } }
    @Test public void slidingAlongThePhysicalLongEdgeIsLandscapeLeftRight() {
        // Moving down the window (increasing y) must move right in landscape space for turn 90, and left for turn 270.
        assertTrue(ViewTurn.logical(90, 600, 2000, PW, PH)[0] > ViewTurn.logical(90, 600, 1000, PW, PH)[0]);
        assertTrue(ViewTurn.logical(270, 600, 2000, PW, PH)[0] < ViewTurn.logical(270, 600, 1000, PW, PH)[0]);
    }
}
