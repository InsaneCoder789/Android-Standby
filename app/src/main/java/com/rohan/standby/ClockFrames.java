package com.rohan.standby;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.*;
import android.os.BatteryManager;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * StandBy faces modelled on iOS 17: pure black, no cards, big confident type.
 * Layout fractions come from the reference screenshots; the bottom 8% stays free for the picker gesture.
 */
final class ClockFrames {
    static final int INK = 0xffffffff, MUTED = 0xff8e8e93, FAINT = 0xff3a3a3c, RED = 0xffff453a, ORANGE = 0xffff9f0a, GREEN = 0xff30d158, GREEN_TIME = 0xff6cf55c, SECOND = 0xffffb000;

    private static String date(String pattern) { return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date()); }
    private static boolean is24(Context c) { return Prefs.is24(c); }
    private static String time(Context c) { return date(is24(c) ? "HH:mm" : "h:mm"); }
    private static Paint paint() { Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); return p; }

    /**
     * Text placed by its visible ink, not its advance box: centred text has equal ink either side of x, left-aligned text starts its ink exactly at x
     * (so a big numeral and a small label above it share one true left edge), right-aligned ends its ink at x. Vertically the cap height is centred on cy.
     * Returns the advance width, for callers that chain pieces of text.
     */
    static float text(Canvas c, Paint p, String v, float x, float cy, float size, int color, Typeface face, Paint.Align align, float maxWidth) {
        p.setStyle(Paint.Style.FILL); p.setShader(null); p.setTypeface(face); p.setTextAlign(Paint.Align.LEFT); p.setTextSize(size); p.setTextScaleX(1f);
        if (maxWidth > 0 && p.measureText(v) > maxWidth) p.setTextSize(size * maxWidth / p.measureText(v));
        RectF ink = Ink.bounds(p, v), cap = Ink.bounds(p, "H0");
        float dx = align == Paint.Align.CENTER ? x - (ink.left + ink.right) / 2f : align == Paint.Align.RIGHT ? x - ink.right : x - ink.left;
        p.setColor(color); c.drawText(v, dx, cy - (cap.top + cap.bottom) / 2f, p); return p.measureText(v);
    }

    private static AlarmManager.AlarmClockInfo alarm(Context c) { AlarmManager m = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE); return m == null ? null : m.getNextAlarmClock(); }
    static int battery(Context c, boolean[] charging) {
        Intent i = c == null ? null : c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); if (i == null) return -1;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1), status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        charging[0] = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL; return level >= 0 && scale > 0 ? Math.round(level * 100f / scale) : -1;
    }

    /** Fixed-width digit cells so the time never shifts. Returns the cell width. */
    /** Time text with every digit in a cell as wide as the widest digit (sized from "00:00"), left-aligned, so it never shifts as it ticks. Returns the width used. */
    static float tabular(Canvas c, Paint p, String t, float x, float cy, float size, int color, Typeface face, float maxW) {
        p.setStyle(Paint.Style.FILL); p.setShader(null); p.setTypeface(face); p.setTextScaleX(1f); p.setTextSize(size);
        t = TimeCells.padded(t); float dw = inkCell(p), full = Math.max(TimeCells.TOTAL_UNITS,TimeCells.units(t)) * dw, k = maxW > 0 && full > maxW ? maxW / full : 1f; p.setTextSize(size * k); dw = inkCell(p); float colonW = dw * .5f;
        RectF cap = Ink.bounds(p, "0"); float base = cy - (cap.top + cap.bottom) / 2f, total = 0; p.setColor(color);
        for (int i = 0; i < t.length(); i++) { char ch = t.charAt(i); float cw = ch == ':' ? colonW : dw; if(ch != ' ') inCell(c, p, ch, x + TimeCells.slotLeft(t, i, dw), cw, base); total += cw; }
        p.setTextAlign(Paint.Align.LEFT); return total;
    }
    private static float cell(Paint p) { float m = 0; for (char d = '0'; d <= '9'; d++) m = Math.max(m, p.measureText(String.valueOf(d))); return m; }
    /** Width of the widest digit's ink plus a hairline gap: a tight, even pitch for the tall condensed time. */
    private static float inkCell(Paint p) { RectF r = new RectF(); float m = 0; for (char d = '0'; d <= '9'; d++) { Ink.bounds(p, String.valueOf(d), r); m = Math.max(m, r.width()); } return m + p.getTextSize() * .035f; }
    /** Draws one character with its ink centred in a cell starting at x. */
    private static void inCell(Canvas c, Paint p, char ch, float x, float cw, float base) { RectF r = new RectF(); String v = String.valueOf(ch); Ink.bounds(p, v, r); p.setTextAlign(Paint.Align.LEFT); c.drawText(v, x + cw / 2f - (r.left + r.right) / 2f, base, p); }

    // ───────────────────────────── themes ─────────────────────────────
    static final int[] DIGITAL_COLORS = {0xff6cf55c, 0xff7cadf8, 0xffffa30a, 0xffff6fa5};
    private static final int[][] FLOAT_PALETTES = {{0xfff06cb4, 0xffb98cf3, 0xffd1008a}, {0xff6fdc8c, 0xff2f8f57, 0xff6fdc8c}, {0xff5ac8fa, 0xff7b8cff, 0xff3a4fd6}, {0xffffb347, 0xffff7a59, 0xffe8344e}};
    private static int pref(Context c, String key) { return c == null ? 0 : Math.max(0, Math.min(3, c.getSharedPreferences("standby", Context.MODE_PRIVATE).getInt(key, 0))); }
    private static int mix(int a, int b, float t) { return (Integer) new android.animation.ArgbEvaluator().evaluate(t, a, b); }

    // ───────────────────────────── DIGITAL: tall condensed time, date/temperature/alarm column ─────────────────────────────
    static void drawDigital(Canvas c, float w, float h, Context ctx) {
        Paint p = paint(); String t = time(ctx); int accent = DIGITAL_COLORS[Prefs.digitalColor(ctx)];
        AlarmManager.AlarmClockInfo a = Prefs.digitalAlarm(ctx) ? alarm(ctx) : null;
        boolean column = Prefs.digitalDate(ctx) || (Prefs.digitalTemp(ctx) && Weather.ready()) || a != null;
        float x0 = w * .04f, colW = h * .56f, rx = w * .94f - colW, maxW = column ? rx - x0 - w * .03f : w * .92f;
        // Reserve all four digits even at 1..9 o'clock. Width, scale and baseline depend
        // only on the font/window, never the current minute's glyph advances.
        t = TimeCells.padded(t);
        p.setTypeface(Fonts.CONDENSED); p.setTextSize(h); RectF b = Ink.bounds(p, "0");
        float size = h * .90f * h / b.height(); p.setTextSize(size);
        float full = TimeCells.width(inkCell(p)), sx = Math.min(1f, maxW / full);
        if (sx < .66f) { size *= sx / .66f; p.setTextSize(size); full = TimeCells.width(inkCell(p)); sx = Math.min(1f, maxW / full); }
        float drawX = column ? x0 : (w - full * sx) / 2f;
        c.save(); c.translate(drawX, 0); c.scale(sx, 1f);
        tabular(c, p, t, 0, h * .50f, size, accent, Fonts.CONDENSED, 0);
        c.restore(); p.setTextScaleX(1f);
        float fs = h * .125f, ty = h * .155f; String dow = date("EEE").toUpperCase(Locale.getDefault());
        if (Prefs.digitalDate(ctx)) { float dowW = text(c, p, dow, rx, ty, fs, accent, Fonts.ROUNDED_BOLD, Paint.Align.LEFT, 0); text(c, p, date("d"), rx + dowW + fs * .22f, ty, fs, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT, 0); }
        if (Prefs.digitalTemp(ctx) && Weather.ready()) text(c, p, Weather.shown(ctx, Weather.temp) + "°", rx, Prefs.digitalDate(ctx) ? h * .31f : h * .155f, fs, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT, 0);
        if (a != null) {
            Date when = new Date(a.getTriggerTime()); String s = new SimpleDateFormat(is24(ctx) ? "HH:mm" : "h:mm", Locale.getDefault()).format(when), ap = is24(ctx) ? "" : new SimpleDateFormat(" a", Locale.getDefault()).format(when).toUpperCase(Locale.getDefault());
            float as = h * .092f, icon = as * 1.05f, cy = h * .84f; alarmIcon(c, p, rx, cy, icon, accent);
            float tx = rx + icon * 1.35f, tw = text(c, p, s, tx, cy, as, accent, Fonts.ROUNDED_BOLD, Paint.Align.LEFT, 0); if (!ap.isEmpty()) text(c, p, ap, tx + tw, cy, as * .68f, accent, Fonts.ROUNDED_BOLD, Paint.Align.LEFT, 0);
        }
    }
    private static void alarmIcon(Canvas c, Paint p, float x, float cy, float s, int color) {
        p.setStyle(Paint.Style.STROKE); p.setColor(color); p.setStrokeWidth(s * .11f); float cx = x + s / 2, r = s * .34f; c.drawCircle(cx, cy + s * .04f, r, p);
        c.drawLine(cx, cy + s * .04f, cx, cy - s * .12f, p); c.drawLine(cx, cy + s * .04f, cx + s * .12f, cy + s * .10f, p);
        c.drawArc(new RectF(x, cy - s * .52f, x + s * .42f, cy - s * .10f), 180, 110, false, p); c.drawArc(new RectF(x + s * .58f, cy - s * .52f, x + s, cy - s * .10f), 250, 110, false, p);
        c.drawLine(cx - r * .7f, cy + s * .42f, cx - r * .95f, cy + s * .52f, p); c.drawLine(cx + r * .7f, cy + s * .42f, cx + r * .95f, cy + s * .52f, p); p.setStyle(Paint.Style.FILL);
    }

    // ───────────────────────────── FLOAT: overlapping translucent rounded-black digits, grey bubble colon ─────────────────────────────
    private static float span(Paint p, String t, float f, float colon) { float total = 0; for (int i = 0; i < t.length(); i++) total += t.charAt(i) == ':' ? colon : cell(p) * f; return total; }
    static void drawColour(Canvas c, float w, float h, Context ctx) {
        Paint p = paint(); String t = TimeCells.padded(time(ctx)); p.setTypeface(Fonts.ROUNDED_BLACK); p.setTextSize(h); RectF b = Ink.bounds(p, "0");
        float size = h * .88f * h / b.height(), f = .84f; p.setTextSize(size); float colon = size * .26f, total = span(p, "00:00", f, colon), maxW = w * .86f;
        if (total > maxW) { size *= maxW / total; p.setTextSize(size); colon = size * .26f; total = span(p, "00:00", f, colon); }
        Ink.bounds(p, "0", b); float base = h * .50f - (b.top + b.bottom) / 2f;
        int digits = 0; for (int i = 0; i < t.length(); i++) if (t.charAt(i) != ':') digits++;
        int[] pal = FLOAT_PALETTES[Prefs.floatPalette(ctx)], cols = digits >= 4 ? new int[]{pal[0], pal[1], mix(pal[1], pal[2], .3f), pal[2]} : pal;
        // Fixed cells: every digit advances by the widest digit, so the overlapping glyphs never creep as the minutes change.
        float adv = cell(p) * f; float[] gx = new float[t.length()]; float x = 0, colonX = 0; p.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < t.length(); i++) { gx[i] = x; if (t.charAt(i) == ':') { colonX = x + colon / 2; x += colon; } else x += adv; }
        float shift = w / 2f - x / 2f; int di = 0;
        for (int i = 0; i < t.length(); i++) {
            if (t.charAt(i) == ':') continue;
            if (t.charAt(i) == ' ') { di++; continue; }
            p.setColor(cols[Math.min(di, cols.length - 1)]); p.setAlpha(224); c.drawText(t.substring(i, i + 1), gx[i] + adv / 2f + shift, base, p); di++;
        }
        p.setTextAlign(Paint.Align.LEFT);
        if (Prefs.floatColon(ctx)) { p.setColor(0xffd9d9db); p.setAlpha(176); float r = h * .09f; c.drawCircle(colonX + shift, h * .36f, r, p); c.drawCircle(colonX + shift, h * .70f, r, p); }
    }

    /** Dial radius that never reaches the right-hand widgets, from 16:9 up to 21:9. */
    static float dialRadius(float w, float h) { return Math.min(h * .44f, w * .20f); }

    // ───────────────────────────── ANALOG dial (shared) ─────────────────────────────
    static void dial(Canvas c, Paint p, float cx, float cy, float r, boolean seconds, boolean numerals) { dial(c, p, cx, cy, r, seconds, numerals, null); }
    /** tz == null means the device time zone. */
    static void dial(Canvas c, Paint p, float cx, float cy, float r, boolean seconds, boolean numerals, TimeZone tz) { dialFace(c, p, cx, cy, r, numerals); dialHands(c, p, cx, cy, r, seconds, tz); }
    /** Ticks and numerals only (also lets the geometry audit measure the face without hands in the way). */
    static void dialFace(Canvas c, Paint p, float cx, float cy, float r, boolean numerals) {
        for (int i = 0; i < 60; i++) {
            double a = i * Math.PI / 30; boolean major = i % 5 == 0; float in = major ? .90f : .955f;
            p.setStyle(Paint.Style.STROKE); p.setColor(major ? INK : 0xff9a9a9f); p.setStrokeWidth(r * (major ? .042f : .018f)); p.setStrokeCap(major ? Paint.Cap.BUTT : Paint.Cap.ROUND);
            c.drawLine(cx + (float) Math.sin(a) * r * in, cy - (float) Math.cos(a) * r * in, cx + (float) Math.sin(a) * r, cy - (float) Math.cos(a) * r, p);
        }
        p.setStrokeCap(Paint.Cap.ROUND);
        if (numerals) for (int i = 1; i <= 12; i++) { double a = i * Math.PI / 6; text(c, p, Integer.toString(i), cx + (float) Math.sin(a) * r * .745f, cy - (float) Math.cos(a) * r * .745f, r * .205f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, 0); }
    }
    static void dialHands(Canvas c, Paint p, float cx, float cy, float r, boolean seconds, TimeZone tz) {
        Calendar now = tz == null ? Calendar.getInstance() : Calendar.getInstance(tz); float sec = now.get(Calendar.SECOND), min = now.get(Calendar.MINUTE) + sec / 60f, hr = now.get(Calendar.HOUR) + min / 60f;
        hand(c, p, cx, cy, r * .52f, hr * Math.PI / 6, r * .075f, INK, r * .12f);
        hand(c, p, cx, cy, r * .82f, min * Math.PI / 30, r * .060f, INK, r * .12f);
        if (seconds) hand(c, p, cx, cy, r * .90f, sec * Math.PI / 30, r * .016f, SECOND, r * .24f);
        p.setStyle(Paint.Style.FILL); p.setColor(seconds ? SECOND : INK); c.drawCircle(cx, cy, r * .045f, p); p.setColor(Color.BLACK); c.drawCircle(cx, cy, r * .016f, p);
    }
    private static void hand(Canvas c, Paint p, float x, float y, float len, double a, float width, int color, float tail) {
        p.setStyle(Paint.Style.STROKE); p.setColor(color); p.setStrokeWidth(width); float sx = (float) Math.sin(a), cy = -(float) Math.cos(a);
        c.drawLine(x - sx * tail, y - cy * tail, x + sx * len, y + cy * len, p); p.setStyle(Paint.Style.FILL);
    }

    // ───────────────────────────── ANALOG + WEATHER ─────────────────────────────
    static void drawAnalog(Canvas c, float w, float h, Context ctx) {
        Paint p = paint(); boxedDial(c, p, w, h, ctx);
        float x = w * .565f, ringX = w * .905f, ringR = h * .075f, maxW = w * .38f, headW = ringX - ringR * 1.3f - w * .02f - x;
        chargeRing(c, p, ctx, ringX, h * .155f, ringR, h, false);
        if (Prefs.analogSide(ctx) == 0 && Weather.ready()) {
            text(c, p, Weather.city.isEmpty() ? "Weather" : Weather.city, x, h * .15f, h * .075f, INK, Fonts.UI_SEMI, Paint.Align.LEFT, headW);
            text(c, p, dayMonth(), x, h * .255f, h * .06f, RED, Fonts.UI_SEMI, Paint.Align.LEFT, headW);
            text(c, p, Weather.shown(ctx, Weather.temp) + "\u00b0", x, h * .50f, h * .27f, INK, Fonts.UI_MED, Paint.Align.LEFT, maxW);
            Weather.icon(c, p, Weather.code, x + h * .055f, h * .72f, h * .12f); text(c, p, Weather.describe(Weather.code), x + h * .16f, h * .72f, h * .08f, INK, Fonts.UI_MED, Paint.Align.LEFT, maxW - h * .16f);
            text(c, p, "H:" + Weather.shown(ctx, Weather.hi) + "\u00b0  L:" + Weather.shown(ctx, Weather.lo) + "\u00b0", x, h * .86f, h * .075f, INK, Fonts.UI_MED, Paint.Align.LEFT, maxW);
        } else {
            text(c, p, date("EEEE"), x, h * .20f, h * .10f, RED, Fonts.UI_SEMI, Paint.Align.LEFT, headW);
            float dw = text(c, p, date("d"), x, h * .55f, h * .34f, INK, Fonts.UI_MED, Paint.Align.LEFT, w * .17f);
            text(c, p, date("MMM").toUpperCase(Locale.getDefault()), x + dw + h * .05f, h * .55f + h * .075f, h * .12f, RED, Fonts.UI_SEMI, Paint.Align.LEFT, maxW - dw - h * .05f);
            text(c, p, Prefs.analogSide(ctx) == 1 ? date("EEEE  \u00b7  d MMMM yyyy") : "Allow location in Android settings for weather", x, h * .84f, h * .05f, MUTED, Fonts.UI_MED, Paint.Align.LEFT, maxW);
        }
    }
    /** "4 NOV", shown next to the weather so the Analog frame always carries the full date. */
    private static String dayMonth() { return date("d") + " " + date("MMM").toUpperCase(Locale.getDefault()); }

    // ───────────────────────────── WIDGETS: dial + month calendar + battery ring ─────────────────────────────
    private static void boxedDial(Canvas c, Paint p, float w, float h, Context ctx) {
        RectF box = new RectF(w * .045f, h * .075f, w * .455f, h * .925f);
        p.setStyle(Paint.Style.FILL); p.setColor(0xff111113);
        c.drawRoundRect(box, h * .08f, h * .08f, p);
        float radius = Math.min(box.width(), box.height()) * .455f;
        dial(c, p, box.centerX(), box.centerY(), radius, Prefs.dialSeconds(ctx), Prefs.dialNumerals(ctx));
    }
    static void drawWidgets(Canvas c, float w, float h, Context ctx) {
        Paint p = paint(); boxedDial(c, p, w, h, ctx);
        RectF box = new RectF(w * .475f, h * .075f, w * .955f, h * .925f);
        p.setStyle(Paint.Style.FILL); p.setColor(0xff111113); c.drawRoundRect(box, h * .08f, h * .08f, p);
        float left = w * .51f, right = w * .91f;
        text(c, p, date("EEEE, d MMMM"), left, h * .17f, h * .062f, INK, Fonts.UI_MED, Paint.Align.LEFT, right - left - h * .16f);
        chargeRing(c, p, ctx, w * .92f, h * .17f, h * .045f, h, false);
        grid(c, p, ctx, h, left + w * .025f, right - w * .005f, h * .295f, h * .39f, h * .09f, h * .050f, true);
    }
    /** The shared charging indicator: ring that fills with the charge, a bolt while charging, the percentage otherwise. */
    /** 1 = fully visible. The view fades the ring out while the picker (and its brightness bar) is open, so the two never overlap. */
    static float ringFade = 1f;
    static void chargeRing(Canvas c, Paint p, Context ctx, float cx, float cy, float r, float h, boolean backing) {
        if (ctx == null || !Prefs.chargeRing(ctx) || ringFade <= .02f) return; boolean[] ch = {false}; int lv = battery(ctx, ch); if (lv < 0) return;
        int layer = ringFade < .98f ? c.saveLayerAlpha(cx - r * 2f, cy - r * 2f, cx + r * 2f, cy + r * 2f, (int) (255 * ringFade)) : -1;
        if (backing) { p.setStyle(Paint.Style.FILL); p.setColor(0xb3000000); c.drawCircle(cx, cy, r * 1.45f, p); }
        batteryRing(c, p, cx, cy, r, lv / 100f, ch[0], h);
        if (layer >= 0) c.restoreToCount(layer);
    }
    static void batteryRing(Canvas c, Paint p, float x, float y, float r, float frac, boolean charging, float h) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(r * .22f); p.setColor(0xff1f3a26); c.drawCircle(x, y, r, p);
        p.setColor(frac <= .2f && !charging ? RED : GREEN); c.drawArc(new RectF(x - r, y - r, x + r, y + r), -90, 360 * frac, false, p); p.setStyle(Paint.Style.FILL);
        if (charging) { Path b = new Path(); b.moveTo(x + r * .12f, y - r * .50f); b.lineTo(x - r * .30f, y + r * .08f); b.lineTo(x - r * .02f, y + r * .08f); b.lineTo(x - r * .10f, y + r * .52f); b.lineTo(x + r * .32f, y - r * .10f); b.lineTo(x + r * .04f, y - r * .10f); b.close(); p.setColor(GREEN); c.drawPath(b, p); }
        else text(c, p, Integer.toString(Math.round(frac * 100)), x, y, r * .62f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, r * 1.3f);
    }
    /** Month grid. Column centres run left..right; weekends are greyed, today is a red disc. */
    private static void grid(Canvas c, Paint p, Context ctx, float h, float left, float right, float headY, float row0, float rowH, float fs, boolean adjacent) {
        Calendar first = Calendar.getInstance(); int today = first.get(Calendar.DAY_OF_MONTH); first.set(Calendar.DAY_OF_MONTH, 1);
        int fdow = Prefs.firstDow(ctx), off = (first.get(Calendar.DAY_OF_WEEK) - fdow + 7) % 7, days = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int rows = (off + days + 6) / 7; float cell = (right - left) / 6f;
        float bottom = h * .845f; rowH = rows > 1 ? Math.min(rowH, (bottom - row0) / (rows - 1)) : rowH;
        String[] names = new DateFormatSymbols().getShortWeekdays();
        for (int i = 0; i < 7; i++) {
            String name = names[(fdow - 1 + i) % 7 + 1].toUpperCase(Locale.getDefault());
            text(c, p, name, left + cell * i, headY, fs * .68f, MUTED, Fonts.UI_SEMI, Paint.Align.CENTER, cell * .86f);
        }
        Calendar before = (Calendar) first.clone(); before.add(Calendar.MONTH, -1); int previousDays = before.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int slot = 0; slot < rows * 7; slot++) {
            int d = slot - off + 1, col = slot % 7; boolean inMonth = d >= 1 && d <= days;
            if (!inMonth && !adjacent) continue;
            int shown = d < 1 ? previousDays + d : d > days ? d - days : d;
            int dow = (fdow - 1 + col) % 7 + 1; float x = left + cell * col, y = row0 + rowH * (slot / 7);
            boolean current = inMonth && d == today, weekend = Prefs.dimWeekends(ctx) && (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY);
            if (current) { p.setStyle(Paint.Style.FILL); p.setColor(0xffff8975); float halfW = Math.min(cell * .37f, rowH * .55f); c.drawRoundRect(x - halfW, y - rowH * .35f, x + halfW, y + rowH * .35f, rowH * .17f, rowH * .17f, p); }
            text(c, p, Integer.toString(shown), x, y, fs, current ? 0xff181313 : !inMonth ? 0xff626268 : weekend ? MUTED : INK, current ? Fonts.UI_BOLD : Fonts.UI_MED, Paint.Align.CENTER, cell * .86f);
        }
    }

    // ───────────────────────────── CALENDAR: grouped date and measured full month ─────────────────────────────
    static void drawCalendar(Canvas c, float w, float h, Context ctx) {
        Paint p = paint(); float dateX = w * .205f;
        text(c, p, date("EEE").toUpperCase(Locale.getDefault()), dateX, h * .215f, h * .12f, 0xffff8975, Fonts.ROUNDED_BOLD, Paint.Align.CENTER, w * .26f);
        text(c, p, date("MMM").toUpperCase(Locale.getDefault()), dateX, h * .34f, h * .10f, INK, Fonts.UI_MED, Paint.Align.CENTER, w * .26f);
        p.setTypeface(Fonts.ROUNDED_BOLD); p.setTextSize(h * .43f);
        float dateSize = h * .43f * Math.min(1f, w * .24f / (2f * cell(p)));
        text(c, p, date("d"), dateX, h * .63f, dateSize, INK, Fonts.ROUNDED_BOLD, Paint.Align.CENTER, 0);
        p.setStyle(Paint.Style.FILL); p.setColor(0xff303034); c.drawRect(w * .385f, h * .12f, w * .386f, h * .88f, p);
        text(c, p, date("MMMM yyyy"), w * .435f, h * .18f, h * .085f, INK, Fonts.UI_SEMI, Paint.Align.LEFT, w * .42f);
        chargeRing(c, p, ctx, w * .925f, h * .17f, h * .045f, h, false);
        grid(c, p, ctx, h, w * .46f, w * .91f, h * .30f, h * .405f, h * .10f, h * .057f, false);
    }

    // ───────────────────────────── WORLD: mini dials for your chosen cities ─────────────────────────────
    static void drawWorld(Canvas c, float w, float h, Context ctx) {
        Paint p = paint(); String[] zones = Prefs.worldZones(ctx);
        if (zones.length == 0) { text(c, p, "No cities yet", w * .5f, h * .45f, h * .08f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, w * .8f); text(c, p, "Add some in Standby settings", w * .5f, h * .56f, h * .05f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, w * .8f); return; }
        chargeRing(c, p, ctx, w * .92f, h * .098f, h * .042f, h, false);
        int n = zones.length; float cell = w * .90f / n, r = Math.min(cell * .40f, h * .25f); TimeZone local = TimeZone.getDefault(); long now = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            float cx = w * .05f + cell * (i + .5f); TimeZone tz = TimeZone.getTimeZone(zones[i]);
            dial(c, p, cx, h * .41f, r, Prefs.worldSeconds(ctx), false, tz);
            text(c, p, WorldCities.city(zones[i]), cx, h * .735f, h * .065f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, cell * .92f);
            SimpleDateFormat f = new SimpleDateFormat(is24(ctx) ? "HH:mm" : "h:mm a", Locale.getDefault()); f.setTimeZone(tz); String clock = f.format(new Date(now));
            text(c, p, clock, cx, h * .815f, h * .05f, INK, Fonts.UI_MED, Paint.Align.CENTER, cell * .92f);
            text(c, p, WorldCities.relative(local, tz, now), cx, h * .885f, h * .038f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, cell * .94f);
        }
    }
    static void drawCalendar(Canvas c, float w, float h) { drawCalendar(c, w, h, null); }
}
