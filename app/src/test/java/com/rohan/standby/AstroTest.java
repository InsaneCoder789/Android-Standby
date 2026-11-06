package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class AstroTest {
    private static long utc(int y, int mo, int d, int h, int mi) { java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")); c.clear(); c.set(y, mo - 1, d, h, mi); return c.getTimeInMillis(); }
    private static double hours(long ms) { return (ms % 86400000L) / 3600000.0; }

    @Test public void equatorAtTheMarchEquinoxHasASixAmSixPmSun() {
        long[] s = Astro.sun(0, 0, utc(2025, 3, 20, 12, 0)); assertNotNull(s);
        assertEquals(6.1, hours(s[0]), .25); assertEquals(18.1, hours(s[1]), .25);
    }
    @Test public void daylightIsLongerInSummerThanWinterInTheNorth() {
        long[] jun = Astro.sun(51.5, -0.1, utc(2025, 6, 21, 12, 0)), dec = Astro.sun(51.5, -0.1, utc(2025, 12, 21, 12, 0));
        double a = (jun[1] - jun[0]) / 3.6e6, b = (dec[1] - dec[0]) / 3.6e6; assertEquals(16.6, a, .4); assertEquals(7.8, b, .4);
    }
    @Test public void londonMidsummerSunriseIsAboutFourFortyFiveUtc() { long[] s = Astro.sun(51.5, -0.1, utc(2025, 6, 21, 12, 0)); assertEquals(3.7, hours(s[0]), .3); assertEquals(20.3, hours(s[1]), .3); }
    @Test public void polarDayAndNightReturnNull() { assertNull(Astro.sun(80, 0, utc(2025, 6, 21, 12, 0))); assertNull(Astro.sun(80, 0, utc(2025, 12, 21, 12, 0))); }
    @Test public void sunriseComesBeforeSunsetEverywhereItExists() {
        for (double lat = -60; lat <= 60; lat += 15) for (int month = 1; month <= 12; month++) { long[] s = Astro.sun(lat, 30, utc(2025, month, 15, 12, 0)); assertNotNull(s); assertTrue(s[0] < s[1]); assertTrue(s[1] - s[0] < 86400000L); }
    }
    @Test public void knownFullMoonIsHalfwayThroughTheCycle() { assertEquals(.5, Astro.moonPhase(utc(2025, 1, 13, 22, 27)), .02); assertEquals(1.0, Astro.illumination(Astro.moonPhase(utc(2025, 1, 13, 22, 27))), .02); }
    @Test public void knownNewMoonIsNearZero() { double p = Astro.moonPhase(utc(2025, 1, 29, 12, 36)); assertTrue(p < .02 || p > .98); assertTrue(Astro.illumination(p) < .01); }
    @Test public void firstQuarterIsHalfLit() { assertEquals(.5, Astro.illumination(.25), 1e-9); assertEquals(.5, Astro.illumination(.75), 1e-9); }
    @Test public void phaseStaysInRangeForAnyDate() { for (long t = utc(1990, 1, 1, 0, 0); t < utc(2060, 1, 1, 0, 0); t += 37L * 86400000L + 12345) { double p = Astro.moonPhase(t); assertTrue(p >= 0 && p < 1); } }
    @Test public void everyPhaseHasAName() { String last = ""; for (double p = 0; p < 1; p += .01) { String n = Astro.phaseName(p); assertFalse(n.isEmpty()); last = n; } assertEquals("Waning Crescent", Astro.phaseName(.9)); assertEquals("Full Moon", Astro.phaseName(.5)); assertEquals("New Moon", Astro.phaseName(.0)); }
    @Test public void daysToFullAndNewAreWithinOneCycle() { for (double p = 0; p < 1; p += .05) { assertTrue(Astro.daysToFull(p) >= 0 && Astro.daysToFull(p) <= Astro.SYNODIC + 1e-9); assertTrue(Astro.daysToNew(p) > 0 && Astro.daysToNew(p) <= Astro.SYNODIC + 1e-9); } assertEquals(0, Astro.daysToFull(.5), 1e-9); }
}
