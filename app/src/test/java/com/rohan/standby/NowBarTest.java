package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class NowBarTest {
    private static int[] fill(int w, int h, int argb) { int[] a = new int[w * h]; java.util.Arrays.fill(a, argb); return a; }
    private static int r(int c) { return (c >> 16) & 255; } private static int g(int c) { return (c >> 8) & 255; } private static int b(int c) { return c & 255; }

    // ── time text ──
    @Test public void clockFormatsMinutesAndSeconds() { assertEquals("0:00", NowBar.clock(0)); assertEquals("0:00", NowBar.clock(999)); assertEquals("0:01", NowBar.clock(1000)); assertEquals("0:59", NowBar.clock(59_999)); assertEquals("1:00", NowBar.clock(60_000)); assertEquals("3:25", NowBar.clock(205_000)); assertEquals("59:59", NowBar.clock(3_599_000)); }
    @Test public void clockFormatsHours() { assertEquals("1:00:00", NowBar.clock(3_600_000)); assertEquals("1:02:05", NowBar.clock(3_725_000)); assertEquals("10:00:00", NowBar.clock(36_000_000)); }
    @Test public void clockHandlesBadInput() { assertEquals("0:00", NowBar.clock(-5)); assertEquals("0:00", NowBar.clock(Long.MIN_VALUE)); assertEquals("0:00", NowBar.clock(Long.MAX_VALUE)); }
    @Test public void fractionIsClamped() { assertEquals(0f, NowBar.fraction(0, 0), 0); assertEquals(0f, NowBar.fraction(5, -1), 0); assertEquals(.5f, NowBar.fraction(50, 100), 1e-6f); assertEquals(1f, NowBar.fraction(500, 100), 0); assertEquals(0f, NowBar.fraction(-500, 100), 0); }

    // ── seek-bar wave ──
    @Test public void waveStartsAtZeroAndStaysWithinAmplitude() { assertEquals(0f, NowBar.waveY(0, 40, 6, 0), 1e-4f); for (float x = 0; x < 400; x += 3.7f) assertTrue(Math.abs(NowBar.waveY(x, 40, 6, .3f)) <= 6.0001f); }
    @Test public void waveRepeatsEveryWavelength() { for (float x = 0; x < 100; x += 7.3f) assertEquals(NowBar.waveY(x, 40, 6, 1.2f), NowBar.waveY(x + 40, 40, 6, 1.2f), 1e-3f); }
    @Test public void waveReachesItsPeak() { assertEquals(6f, NowBar.waveY(10, 40, 6, 0), 1e-3f); assertEquals(-6f, NowBar.waveY(30, 40, 6, 0), 1e-3f); }

    // ── album colours ──
    @Test public void colourResultIsAlwaysOpaqueAndTwoColours() { int[] c = NowBar.pickColors(fill(8, 8, 0xffcc3322), 8, 8); assertEquals(2, c.length); assertEquals(0xff, c[0] >>> 24); assertEquals(0xff, c[1] >>> 24); }
    @Test public void redArtGivesRedTopAndDarkerBottom() { int[] c = NowBar.pickColors(fill(16, 16, 0xffd02a20), 16, 16); assertTrue(r(c[0]) > g(c[0]) * 2 && r(c[0]) > b(c[0]) * 2); assertTrue(r(c[1]) + g(c[1]) + b(c[1]) < r(c[0]) + g(c[0]) + b(c[0])); }
    @Test public void twoColourArtUsesBothHues() {
        int[] px = new int[32 * 32]; for (int i = 0; i < px.length; i++) px[i] = (i % 32) < 20 ? 0xff2060e0 : 0xffe88020; // 62% blue, 38% orange
        int[] c = NowBar.pickColors(px, 32, 32); assertTrue("top should be blue-ish", b(c[0]) > r(c[0])); assertTrue("bottom should be orange-ish", r(c[1]) > b(c[1]));
    }
    @Test public void greyBlackAndWhiteArtFallBackToTheNeutralGradient() {
        for (int col : new int[]{0xff000000, 0xffffffff, 0xff808080, 0xff1a1a1a}) { int[] c = NowBar.pickColors(fill(10, 10, col), 10, 10); assertEquals(Integer.toHexString(col), NowBar.FALLBACK_TOP, c[0]); assertEquals(NowBar.FALLBACK_BOTTOM, c[1]); }
    }
    @Test public void transparentOrEmptyOrBrokenInputFallsBack() {
        assertArrayEquals(new int[]{NowBar.FALLBACK_TOP, NowBar.FALLBACK_BOTTOM}, NowBar.pickColors(null, 4, 4)); assertArrayEquals(new int[]{NowBar.FALLBACK_TOP, NowBar.FALLBACK_BOTTOM}, NowBar.pickColors(new int[0], 0, 0));
        assertArrayEquals(new int[]{NowBar.FALLBACK_TOP, NowBar.FALLBACK_BOTTOM}, NowBar.pickColors(new int[3], 4, 4)); assertArrayEquals(new int[]{NowBar.FALLBACK_TOP, NowBar.FALLBACK_BOTTOM}, NowBar.pickColors(fill(6, 6, 0x00ff0000), 6, 6));
    }
    @Test public void aSingleHueGetsAHueShiftedSecondColourInsteadOfTheSameOne() { int[] c = NowBar.pickColors(fill(16, 16, 0xff2a8fd0), 16, 16); assertNotEquals(c[0], c[1]); }
    @Test public void vividLiftsDullColoursButKeepsTheHue() { int dull = 0xff605850; int v = NowBar.vivid(dull); float[] hsv = NowBar.hsv(r(v), g(v), b(v)); assertTrue(hsv[1] >= .49f && hsv[2] >= .69f); float[] orig = NowBar.hsv(r(dull), g(dull), b(dull)); assertEquals(orig[0], hsv[0], 3f); }
    @Test public void hsvRoundTrips() { for (int col : new int[]{0xffff0000, 0xff00ff00, 0xff0000ff, 0xffff8800, 0xff12ab34, 0xff7f7fff}) { float[] h = NowBar.hsv(r(col), g(col), b(col)); int back = NowBar.fromHsv(h[0], h[1], h[2]); assertEquals(r(col), r(back), 1); assertEquals(g(col), g(back), 1); assertEquals(b(col), b(back), 1); } }
    @Test public void mixEndpointsAndMidpoint() { assertEquals(0xff102030, NowBar.mix(0xff102030, 0xffffffff, 0)); assertEquals(0xffffffff, NowBar.mix(0xff102030, 0xffffffff, 1)); assertEquals(0xff808080, NowBar.mix(0xff000000, 0xffffffff, .5f)); }
}
