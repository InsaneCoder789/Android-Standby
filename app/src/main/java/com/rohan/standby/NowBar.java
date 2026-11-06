package com.rohan.standby;

/** Pure helpers for the Now Bar style player: album colours, time text, seek-bar wave. No Android types, so all of it is unit-tested. */
final class NowBar {
    static final int FALLBACK_TOP = 0xff3b3f5c, FALLBACK_BOTTOM = 0xff15161f;

    /** "m:ss", or "h:mm:ss" from one hour up. Negative or absurd input reads 0:00. */
    static String clock(long ms) {
        long s = Math.max(0, ms) / 1000; if (s > 99L * 3600) s = 0;
        return s >= 3600 ? String.format(java.util.Locale.US, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60) : String.format(java.util.Locale.US, "%d:%02d", s / 60, s % 60);
    }
    /** Playback fraction clamped to 0..1; unknown duration reads 0. */
    static float fraction(long position, long duration) { return duration <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) position / duration)); }
    /** Height of the seek-bar wave at x. */
    static float waveY(float x, float wavelength, float amplitude, float phase) { return (float) (amplitude * Math.sin(2 * Math.PI * x / wavelength + phase)); }

    // ── colour extraction ──
    /** {top, bottom} gradient colours for the player background, from album-art pixels (ARGB, row-major). Never null; opaque. */
    static int[] pickColors(int[] argb, int w, int h) {
        if (argb == null || w <= 0 || h <= 0 || argb.length < w * h) return new int[]{FALLBACK_TOP, FALLBACK_BOTTOM};
        int stepX = Math.max(1, w / 32), stepY = Math.max(1, h / 32); double[] weight = new double[12], sr = new double[12], sg = new double[12], sb = new double[12]; double total = 0;
        for (int y = 0; y < h; y += stepY) for (int x = 0; x < w; x += stepX) {
            int p = argb[y * w + x]; if ((p >>> 24) < 200) continue; int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255; float[] hsv = hsv(r, g, b);
            if (hsv[2] < .14f || hsv[1] < .12f) continue;                       // near-black and near-grey pixels carry no usable hue
            double wgt = hsv[1] * hsv[1] * hsv[2]; int bin = Math.min(11, (int) (hsv[0] / 30f));
            weight[bin] += wgt; sr[bin] += r * wgt; sg[bin] += g * wgt; sb[bin] += b * wgt; total += wgt;
        }
        if (total < 1e-3) return new int[]{FALLBACK_TOP, FALLBACK_BOTTOM};
        int first = 0; for (int i = 1; i < 12; i++) if (weight[i] > weight[first]) first = i;
        int second = -1; for (int i = 0; i < 12; i++) { int d = Math.min((i - first + 12) % 12, (first - i + 12) % 12); if (d >= 2 && weight[i] > weight[first] * .22 && (second < 0 || weight[i] > weight[second])) second = i; }
        int c1 = vivid(avg(sr, sg, sb, weight, first)); int c2 = second >= 0 ? vivid(avg(sr, sg, sb, weight, second)) : rotate(c1, 38f);
        return new int[]{mix(c1, 0xff000000, .18f), mix(c2, 0xff000000, .62f)};
    }
    private static int avg(double[] sr, double[] sg, double[] sb, double[] w, int i) { return 0xff000000 | ((int) (sr[i] / w[i]) << 16) | ((int) (sg[i] / w[i]) << 8) | (int) (sb[i] / w[i]); }
    /** Lift a colour toward something that reads well on a dark screen: saturation at least .5, brightness at least .7. */
    static int vivid(int c) { float[] hsv = hsv((c >> 16) & 255, (c >> 8) & 255, c & 255); return fromHsv(hsv[0], Math.max(.5f, hsv[1]), Math.max(.7f, hsv[2])); }
    static int rotate(int c, float degrees) { float[] hsv = hsv((c >> 16) & 255, (c >> 8) & 255, c & 255); return fromHsv((hsv[0] + degrees + 360f) % 360f, hsv[1], hsv[2]); }
    static int mix(int a, int b, float t) { int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bl = Math.round((a & 255) * (1 - t) + (b & 255) * t); return 0xff000000 | (r << 16) | (g << 8) | bl; }

    static float[] hsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f, max = Math.max(rf, Math.max(gf, bf)), min = Math.min(rf, Math.min(gf, bf)), d = max - min, h;
        if (d == 0) h = 0; else if (max == rf) h = 60f * (((gf - bf) / d) % 6f); else if (max == gf) h = 60f * ((bf - rf) / d + 2f); else h = 60f * ((rf - gf) / d + 4f);
        if (h < 0) h += 360f; return new float[]{h, max == 0 ? 0 : d / max, max};
    }
    static int fromHsv(float h, float s, float v) {
        float c = v * s, x = c * (1 - Math.abs((h / 60f) % 2f - 1)), m = v - c, r, g, b; int sector = ((int) (h / 60f)) % 6;
        switch (sector) { case 0: r = c; g = x; b = 0; break; case 1: r = x; g = c; b = 0; break; case 2: r = 0; g = c; b = x; break; case 3: r = 0; g = x; b = c; break; case 4: r = x; g = 0; b = c; break; default: r = c; g = 0; b = x; }
        return 0xff000000 | (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
}
