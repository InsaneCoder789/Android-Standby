package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class FramesTest {
    @Test public void parseKeepsAtMostThreeInOrder() { assertArrayEquals(new int[]{5, 1, 9}, Frames.parse("5,1,9,3,4")); }
    @Test public void parseDropsRepeatsAndOutOfRange() { assertArrayEquals(new int[]{2, 7}, Frames.parse("2,2,99,-1,x,7")); }
    @Test public void parseFallsBackToTheDefaultWhenEmptyOrMissing() { assertArrayEquals(Frames.DEFAULT, Frames.parse(null)); assertArrayEquals(Frames.DEFAULT, Frames.parse("")); assertArrayEquals(Frames.DEFAULT, Frames.parse("junk")); }
    @Test public void formatRoundTrips() { int[] s = {4, 0, 12}; assertArrayEquals(s, Frames.parse(Frames.format(s))); }
    @Test public void addStopsAtThree() { int[] s = {0, 1, 2}; assertSame(s, Frames.add(s, 5)); assertArrayEquals(new int[]{0, 1}, Frames.add(new int[]{0}, 1)); }
    @Test public void addIgnoresRepeatsAndBadIds() { int[] s = {0, 1}; assertSame(s, Frames.add(s, 1)); assertSame(s, Frames.add(s, Frames.COUNT)); assertSame(s, Frames.add(s, -1)); }
    @Test public void removeNeverEmptiesTheCarousel() { int[] one = {3}; assertSame(one, Frames.remove(one, 3)); assertArrayEquals(new int[]{0, 2}, Frames.remove(new int[]{0, 1, 2}, 1)); }
    @Test public void removeOfAFrameThatIsNotThereChangesNothing() { int[] s = {0, 1}; assertSame(s, Frames.remove(s, 7)); }
    @Test public void moveSwapsNeighboursAndIgnoresTheEnds() { assertArrayEquals(new int[]{1, 0, 2}, Frames.move(new int[]{0, 1, 2}, 0, 1)); int[] s = {0, 1}; assertSame(s, Frames.move(s, 0, -1)); assertSame(s, Frames.move(s, 1, 1)); assertSame(s, Frames.move(s, 5, 1)); }
    @Test public void wrapHasNoEnd() { assertEquals(0, Frames.wrap(3, 3)); assertEquals(2, Frames.wrap(-1, 3)); assertEquals(1, Frames.wrap(-5, 3)); assertEquals(0, Frames.wrap(7, 0)); }
    @Test public void catalogueIsConsistent() {
        assertEquals(Frames.COUNT, Frames.NAMES.length); assertEquals(Frames.COUNT, Frames.INFO.length); assertEquals(Frames.COUNT, Frames.COMPONENTS.length);
        for (int c : Frames.COMPONENTS) assertTrue(c >= 1 && c <= 3);
        assertTrue("at least fifteen frames beyond the original six", Frames.COUNT - 6 >= 15);
        java.util.HashSet<String> seen = new java.util.HashSet<>(); for (String n : Frames.NAMES) assertTrue(n, seen.add(n));
    }
    @Test public void sharedDialIsOneFrameWithThreeLooks() { assertEquals(3, Frames.COMPONENTS[Frames.DIAL]); assertEquals(2, Frames.COMPONENTS[Frames.DIGITAL]); }
    @Test public void legacyFacesMapOntoFramesAndComponents() {
        int[][] want = {{0, 0}, {0, 1}, {1, 0}, {1, 1}, {1, 2}, {2, 0}, {4, 0}, {5, 0}};
        for (int i = 0; i < 8; i++) { int[] got = Frames.fromLegacy(i); assertArrayEquals("legacy " + i, want[i], got); assertTrue(got[1] < Frames.COMPONENTS[got[0]]); }
    }
    @Test public void onlyDigitalHasNoRing() { assertFalse(Frames.hasRing(Frames.DIGITAL, 0)); assertFalse(Frames.hasRing(Frames.DIGITAL, 1)); for (int f = 1; f < Frames.COUNT; f++) assertTrue(Frames.hasRing(f, 0)); }
    @Test public void defaultSelectionIsThreeValidFrames() { assertEquals(3, Frames.DEFAULT.length); for (int f : Frames.DEFAULT) assertTrue(f >= 0 && f < Frames.COUNT); }
}
