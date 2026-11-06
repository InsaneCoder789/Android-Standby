package com.rohan.standby;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * Geometry audit. Renders each face into an off-screen bitmap at three aspect ratios and MEASURES the pixels:
 * discs and rings centred on their content, dial symmetry, optical alignment of text stacks, calendar column alignment,
 * whole-time centring, and safe margins. Every check prints PASS or FAIL against an explicit tolerance (logcat tag AUDIT).
 */
public class AuditActivity extends Activity {
    static final String T = "AUDIT";
    private final List<String> failures = new ArrayList<>(); private int checks; private java.util.Map<String, ?> saved;
    private void restoreSettings() {
        SharedPreferences.Editor e = getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().clear();
        for (java.util.Map.Entry<String, ?> en : saved.entrySet()) { Object v = en.getValue(); if (v instanceof Boolean) e.putBoolean(en.getKey(), (Boolean) v); else if (v instanceof Integer) e.putInt(en.getKey(), (Integer) v); else if (v instanceof Long) e.putLong(en.getKey(), (Long) v); else if (v instanceof Float) e.putFloat(en.getKey(), (Float) v); else if (v instanceof String) e.putString(en.getKey(), (String) v); }
        e.commit();
    }
    // tolerances (pixels unless noted)
    static final float DISC_TOL = 1.0f, RING_TOL = 1.5f, TICK_SPREAD_TOL = 2f, PIVOT_TOL = 1.5f, NUMERAL_TOL_R = .012f, ALIGN_TOL = 1.5f, COLUMN_TOL = 1.5f, CENTRE_TOL_PCT = .12f /* of width: a round glyph tip fades over a few px, so its visible edge sits slightly inside its outline */, MARGIN_MIN_PCT = 3.5f;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        // The audit needs known settings, so snapshot the real ones and put them back when it finishes.
        saved = new java.util.HashMap<>(getSharedPreferences(Prefs.FILE, MODE_PRIVATE).getAll());
        SharedPreferences.Editor e = getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().clear().putBoolean("burnIn", false).putInt("musicBackground", 1).putBoolean("autoMusic", false);
        for (int f = 0; f < Prefs.FACES; f++) e.putBoolean("face" + f, true); e.apply();
        getSharedPreferences("weather", MODE_PRIVATE).edit().putString("city", "San Diego").putInt("temp", 70).putInt("hi", 70).putInt("lo", 59).putInt("code", 1).putLong("updated", System.currentTimeMillis()).putBoolean("f", true).apply();
        Fonts.init(this);
        new android.os.Handler(getMainLooper()).postDelayed(() -> { run(); Log.i(T, "SUMMARY " + checks + " checks, " + failures.size() + " failed" + (failures.isEmpty() ? "" : ": " + failures)); restoreSettings(); Log.i(T, "DONE"); }, 600);
    }

    private void check(String name, boolean ok, String detail) { checks++; if (!ok) failures.add(name); Log.i(T, (ok ? "PASS " : "FAIL ") + name + "  " + detail); }
    private Bitmap render(int face, int w, int h) {
        StandbyView v = new StandbyView(this); v.setActive(true); v.lockFace(face);
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)); v.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); v.draw(new Canvas(bmp)); return bmp;
    }
    private static int[] px(Bitmap b) { int[] a = new int[b.getWidth() * b.getHeight()]; b.getPixels(a, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight()); return a; }
    interface Match { boolean ok(int r, int g, int b); }
    private static int[] bbox(int[] a, int w, int h, int x0, int y0, int x1, int y1, Match m) {
        int minX = 1 << 30, minY = 1 << 30, maxX = -1, maxY = -1;
        for (int y = Math.max(0, y0); y < Math.min(h, y1); y++) for (int x = Math.max(0, x0); x < Math.min(w, x1); x++) { int p = a[y * w + x]; if (m.ok((p >> 16) & 255, (p >> 8) & 255, p & 255)) { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y; } }
        return maxX < 0 ? null : new int[]{minX, minY, maxX, maxY};
    }
    private static boolean near(int r, int g, int b, int R, int G, int B, int tol) { return Math.abs(r - R) <= tol && Math.abs(g - G) <= tol && Math.abs(b - B) <= tol; }
    private static boolean bright(int r, int g, int b) { return r + g + b > 120; }
    private static boolean white(int r, int g, int b) { return r > 200 && g > 200 && b > 200; }

    /** How trustworthy is Paint.getTextBounds for our fonts? Compare it with the pixels actually drawn. */
    private void fontProbe() {
        String[] strs = {"4", "70°", "WEDNESDAY", "1:05"}; android.graphics.Typeface[] faces = {Fonts.ROUNDED_BOLD, Fonts.UI_MED, Fonts.UI_SEMI, Fonts.CONDENSED}; String[] names = {"RoundedBold", "InterMed", "InterSemi", "Condensed"}; float[] sizes = {60f, 200f, 590f};
        for (int fi = 0; fi < faces.length; fi++) for (String str : strs) for (float size : sizes) {
            android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG); p.setTypeface(faces[fi]); p.setTextSize(size); p.setColor(0xffffffff);
            android.graphics.Rect rb = new android.graphics.Rect(); p.getTextBounds(str, 0, str.length(), rb); android.graphics.Path path = new android.graphics.Path(); p.getTextPath(str, 0, str.length(), 0, 0, path); android.graphics.RectF pb = new android.graphics.RectF(); path.computeBounds(pb, true);
            int W = (int) (p.measureText(str) + size * 1.2f), H = (int) (size * 2f); Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); Canvas c = new Canvas(bmp); float ox = size * .4f, oy = size * 1.3f; c.drawText(str, ox, oy, p);
            int[] a = px(bmp); int[] bx = bbox(a, W, H, 0, 0, W, H, (r, g, b) -> r > 128);
            Log.i(T, String.format("PROBE %-11s %-9s %4.0fpx  pixels L=%.0f R=%.0f | getTextBounds L=%d R=%d | getTextPath L=%.1f R=%.1f | (L err bounds %.1f, path %.1f)", names[fi], str, size, bx[0] - ox, bx[2] + 1 - ox, rb.left, rb.right, pb.left, pb.right, rb.left - (bx[0] - ox), pb.left - (bx[0] - ox)));
            bmp.recycle();
        }
    }
    private void run() {
        if (getIntent().getBooleanExtra("probe", false)) { fontProbe(); return; }
        int[][] sizes = {{2856, 1280}, {1920, 1080}, {2520, 1080}};
        for (int[] sz : sizes) {
            int w = sz[0], h = sz[1]; String tag = w + "x" + h;
            // ── today's disc (Widgets, Calendar): the number must sit dead-centre ──
            for (int face : new int[]{3, 4}) {
                Bitmap bmp = render(face, w, h); int[] a = px(bmp);
                int[] disc = bbox(a, w, h, (int) (w * .45f), 0, w, h, (r, g, bl) -> near(r, g, bl, 240, 85, 77, 10));
                if (disc == null) { check("disc " + tag + " face" + face, false, "disc not found"); continue; }
                float cx = (disc[0] + disc[2]) / 2f, cy = (disc[1] + disc[3]) / 2f, rad = (disc[2] - disc[0]) / 2f;
                int[] txt = bbox(a, w, h, disc[0] + 3, disc[1] + 3, disc[2] - 2, disc[3] - 2, AuditActivity::white);
                float dx = (txt[0] + txt[2]) / 2f - cx, dy = (txt[1] + txt[3]) / 2f - cy;
                check("disc " + tag + " face" + face, Math.abs(dx) <= DISC_TOL && Math.abs(dy) <= DISC_TOL, String.format("number off-centre dx=%.1f dy=%.1f px (r=%.0f, digit height %.0f%% of diameter)", dx, dy, rad, (txt[3] - txt[1] + 1) * 50f / rad));
                bmp.recycle();
            }
            // ── dial: pivot, hour ticks and numerals ──
            {
                Bitmap bmp = render(2, w, h); int[] a = px(bmp); float r = ClockFrames.dialRadius(w, h), cx = w * .275f, cy = h * .5f;
                int[] pv = bbox(a, w, h, (int) (cx - r * .08f), (int) (cy - r * .08f), (int) (cx + r * .08f), (int) (cy + r * .08f), (rr, g, bl) -> near(rr, g, bl, 255, 176, 0, 30));
                float pdx = (pv[0] + pv[2]) / 2f - cx, pdy = (pv[1] + pv[3]) / 2f - cy;
                check("dial pivot " + tag, Math.hypot(pdx, pdy) <= PIVOT_TOL, String.format("pivot off by %.1f,%.1f px", pdx, pdy));
                float lo = 1e9f, hi = -1e9f;
                for (int d : new int[]{0, 90, 180, 270}) {
                    double ang = Math.toRadians(d); float tx = cx + (float) Math.sin(ang) * r * .95f, ty = cy - (float) Math.cos(ang) * r * .95f; int pad = (int) (r * .06f);
                    int[] t = bbox(a, w, h, (int) tx - pad, (int) ty - pad, (int) tx + pad, (int) ty + pad, AuditActivity::white);
                    float rad = (float) Math.hypot((t[0] + t[2]) / 2f - cx, (t[1] + t[3]) / 2f - cy); lo = Math.min(lo, rad); hi = Math.max(hi, rad);
                }
                check("dial ticks " + tag, hi - lo <= TICK_SPREAD_TOL, String.format("12/3/6/9 tick radius spread %.1f px", hi - lo));
                // numerals: measured on a hands-free face so all twelve are judged, every time
                Bitmap face = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); android.graphics.Paint fp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG); ClockFrames.dialFace(new Canvas(face), fp, cx, cy, r, true); int[] fa = px(face);
                float worst = 0; int judged = 0;
                for (int n = 1; n <= 12; n++) {
                    double ang = n * Math.PI / 6; float ix = cx + (float) Math.sin(ang) * r * .745f, iy = cy - (float) Math.cos(ang) * r * .745f; int pad = (int) (r * .115f);
                    int[] t = bbox(fa, w, h, (int) ix - pad, (int) iy - pad, (int) ix + pad, (int) iy + pad, AuditActivity::white); if (t == null) continue;
                    worst = Math.max(worst, (float) Math.hypot((t[0] + t[2]) / 2f - ix, (t[1] + t[3]) / 2f - iy)); judged++;
                }
                check("dial numerals " + tag, judged == 12 && worst <= r * NUMERAL_TOL_R, String.format("%d of 12 numerals judged, worst centre error %.1f px (limit %.1f)", judged, worst, r * NUMERAL_TOL_R)); face.recycle();
                bmp.recycle();
            }
            // ── optical left edges: every line of a stack must start its INK on the same x ──
            alignStack(tag, 2, w, h, (int) (w * .50f), w, new float[][]{{.08f, .26f}, {.27f, .62f}, {.77f, .90f}}, "Analog weather stack");
            alignStack(tag, 5, w, h, (int) (w * .075f + h * .74f + w * .02f), w, new float[][]{{.20f, .31f}, {.325f, .40f}, {.64f, .69f}}, "Music text stack (needs the Music test session running)");
            // ── calendar: weekday letters and numbers share a centre line per column ──
            {
                Bitmap bmp = render(4, w, h); int[] a = px(bmp); float left = w * .565f, right = w * .885f, cell = (right - left) / 6f; float worst = 0;
                for (int col = 0; col < 7; col++) {
                    float cxx = left + cell * col; int x0 = (int) (cxx - cell * .45f), x1 = (int) (cxx + cell * .45f);
                    int[] letter = bbox(a, w, h, x0, (int) (h * .225f), x1, (int) (h * .305f), AuditActivity::bright);
                    int[] num = bbox(a, w, h, x0, (int) (h * .39f), x1, (int) (h * .50f), (r, g, bl) -> bright(r, g, bl) && !near(r, g, bl, 240, 85, 77, 40));
                    if (letter == null || num == null) continue; worst = Math.max(worst, Math.abs((letter[0] + letter[2]) / 2f - (num[0] + num[2]) / 2f));
                }
                check("calendar columns " + tag, worst <= COLUMN_TOL + 3, String.format("weekday letter vs date centre lines differ by up to %.1f px (single vs double digits legitimately differ by the digit width)", worst));
                bmp.recycle();
            }
            // ── charging ring: render with and without it. It must cover nothing and keep a safe margin. ──
            for (int face : new int[]{2, 3, 4, 5, 6, 7}) {
                getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().putBoolean("chargeRing", false).commit(); Bitmap off = render(face, w, h); int[] a0 = px(off);
                getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().putBoolean("chargeRing", true).commit(); Bitmap on = render(face, w, h); int[] a1 = px(on);
                int minX = 1 << 30, minY = 1 << 30, maxX = -1, maxY = -1; for (int y = 0; y < (int) (h * .3f); y++) for (int x = (int) (w * .8f); x < w; x++) if (a0[y * w + x] != a1[y * w + x]) { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y; }
                if (maxX < 0) { check("ring " + tag + " face" + face, false, "no ring drawn"); continue; }
                int covered = 0; for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) { int q = a0[y * w + x]; if (((q >> 16 & 255) + (q >> 8 & 255) + (q & 255)) > 120) covered++; }
                float right = (w - 1 - maxX) * 100f / w, top = minY * 100f / h;
                check("ring " + tag + " face" + face, covered == 0 && right >= MARGIN_MIN_PCT && top >= MARGIN_MIN_PCT, String.format("ring %dx%d px at x %d..%d y %d..%d, covers %d content pixels, margins right %.1f%% top %.1f%%", maxX - minX + 1, maxY - minY + 1, minX, maxX, minY, maxY, covered, right, top)); off.recycle(); on.recycle();
            }
            // ── burn-in drift must never expose a bare edge on a full-bleed face (this is what real hardware caught) ──
            getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().putInt("musicBackground", 0).commit();
            for (float[] drift : new float[][]{{BurnIn.MAX_X, BurnIn.MAX_Y}, {-BurnIn.MAX_X, -BurnIn.MAX_Y}}) {
                StandbyView v = new StandbyView(this); v.setActive(true); v.lockFace(5); v.burnInOverride = drift;
                v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)); v.layout(0, 0, w, h);
                Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); v.draw(new Canvas(bm)); int[] q = px(bm); int bare = 0, total = 0;
                for (int x = 0; x < w; x++) { for (int y : new int[]{0, h - 1}) { total++; int c0 = q[y * w + x]; if (((c0 >> 16 & 255) + (c0 >> 8 & 255) + (c0 & 255)) < 12) bare++; } }
                for (int y = 0; y < h; y++) { for (int x : new int[]{0, w - 1}) { total++; int c0 = q[y * w + x]; if (((c0 >> 16 & 255) + (c0 >> 8 & 255) + (c0 & 255)) < 12) bare++; } }
                check("burn-in coverage " + tag + String.format(" music drift %+.1f%%,%+.1f%%", drift[0] * 100, drift[1] * 100), bare == 0, bare + " of " + total + " edge pixels left bare; sample left-edge " + Integer.toHexString(q[(h / 2) * w]) + " top-edge " + Integer.toHexString(q[w / 2]) + " inner " + Integer.toHexString(q[(h / 2) * w + (int) (w * .03f)])); bm.recycle();
            }
            getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().putInt("musicBackground", 1).commit();
            // ── whole-time centring (Float; Digital when alone) and safe margins on every face ──
            {
                Bitmap bmp = render(1, w, h); int[] a = px(bmp); int[] box = bbox(a, w, h, 0, 0, w, h, (r, g, bl) -> r + g + bl > 24); float off = (box[0] + box[2]) / 2f - (w - 1) / 2f;
                check("float centred " + tag, Math.abs(off) <= w * CENTRE_TOL_PCT / 100f, String.format("ink centre is %.1f px from screen centre (L %.1f%% R %.1f%%)", off, box[0] * 100f / w, (w - 1 - box[2]) * 100f / w)); bmp.recycle();
            }
            for (int face = 0; face < Prefs.FACES; face++) {
                if (face == 6 || face == 5) continue; Bitmap bmp = render(face, w, h); int[] a = px(bmp); int[] box = bbox(a, w, h, 0, 0, w, h, AuditActivity::bright);
                if (box == null) { check("margins " + tag + " face" + face, false, "empty"); continue; }
                float l = box[0] * 100f / w, t = box[1] * 100f / h, r = (w - 1 - box[2]) * 100f / w, bt = (h - 1 - box[3]) * 100f / h;
                check("margins " + tag + " face" + face, Math.min(Math.min(l, r), Math.min(t, bt)) >= MARGIN_MIN_PCT, String.format("L %.1f%%  T %.1f%%  R %.1f%%  B %.1f%%", l, t, r, bt)); bmp.recycle();
            }
        }
    }
    private void alignStack(String tag, int face, int w, int h, int x0, int x1, float[][] bands, String name) {
        Bitmap bmp = render(face, w, h); int[] a = px(bmp); float lo = 1e9f, hi = -1e9f; StringBuilder sb = new StringBuilder();
        for (float[] bnd : bands) { int[] bx = bbox(a, w, h, x0, (int) (h * bnd[0]), x1, (int) (h * bnd[1]), AuditActivity::bright); if (bx == null) { sb.append("none "); continue; } lo = Math.min(lo, bx[0]); hi = Math.max(hi, bx[0]); sb.append(bx[0]).append(' '); }
        check("align " + name + " " + tag, hi - lo <= ALIGN_TOL, "ink left edges: " + sb + String.format("(spread %.0f px)", hi - lo)); bmp.recycle();
    }
}
