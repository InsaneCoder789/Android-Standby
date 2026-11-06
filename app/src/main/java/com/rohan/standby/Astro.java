package com.rohan.standby;

/** Sun and moon arithmetic for the Sun path and Moon frames. Pure, no Android types, unit-tested against known dates. */
final class Astro {
    static final double SYNODIC = 29.530588853;
    private static double jd(long ms) { return ms / 86400000.0 + 2440587.5; }

    /** {sunrise, sunset} in epoch milliseconds for the day around {@code nowMs} at the place (lat, lon east-positive), or null in polar day or night. */
    static long[] sun(double lat, double lon, long nowMs) {
        double n = Math.round(jd(nowMs) - 2451545.0 + 0.0008), jStar = n - lon / 360.0;
        double m = Math.toRadians((357.5291 + 0.98560028 * jStar) % 360.0), c = 1.9148 * Math.sin(m) + 0.02 * Math.sin(2 * m) + 0.0003 * Math.sin(3 * m);
        double lambda = Math.toRadians((Math.toDegrees(m) + c + 180.0 + 102.9372) % 360.0), transit = 2451545.0 + jStar + 0.0053 * Math.sin(m) - 0.0069 * Math.sin(2 * lambda);
        double sinD = Math.sin(lambda) * Math.sin(Math.toRadians(23.44)), cosD = Math.cos(Math.asin(sinD)), phi = Math.toRadians(lat);
        double cosW = (Math.sin(Math.toRadians(-0.833)) - Math.sin(phi) * sinD) / (Math.cos(phi) * cosD); if (cosW < -1 || cosW > 1 || Double.isNaN(cosW)) return null;
        double w = Math.toDegrees(Math.acos(cosW)) / 360.0;
        return new long[]{Math.round((transit - w - 2440587.5) * 86400000.0), Math.round((transit + w - 2440587.5) * 86400000.0)};
    }
    /** Days since the last new moon, 0 .. 29.53. */
    static double moonAge(long ms) { double a = (jd(ms) - 2451550.1) % SYNODIC; return a < 0 ? a + SYNODIC : a; }
    /** 0 new, .25 first quarter, .5 full, .75 last quarter. */
    static double moonPhase(long ms) { return moonAge(ms) / SYNODIC; }
    /** Fraction of the disc that is lit, 0..1. */
    static double illumination(double phase) { return (1 - Math.cos(2 * Math.PI * phase)) / 2; }
    static String phaseName(double p) {
        if (p < .03 || p >= .97) return "New Moon"; if (p < .22) return "Waxing Crescent"; if (p < .28) return "First Quarter"; if (p < .47) return "Waxing Gibbous";
        if (p < .53) return "Full Moon"; if (p < .72) return "Waning Gibbous"; if (p < .78) return "Last Quarter"; return "Waning Crescent";
    }
    /** Days until the next full moon from this phase, 0 .. 29.53. */
    static double daysToFull(double phase) { double d = (.5 - phase) * SYNODIC; return d < 0 ? d + SYNODIC : d; }
    static double daysToNew(double phase) { return (1 - phase) * SYNODIC; }
}
