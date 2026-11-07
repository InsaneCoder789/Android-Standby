package com.rohan.standby;

import android.app.AlarmManager;
import android.content.Context;
import android.graphics.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The extra frames: Flip, Word clock, Dot matrix, Nixie, Alarm LED, Railway, Orbit, Sun path, Moon, Time left, Big date, Weather, Aurora, Breathe,
 * Terminal and Sky. Each has two components (swipe up or down). All geometry is a fraction of the landscape window, like the original frames.
 */
final class NewFrames {
    /** Accent colours the user can pick for frames that have one; null means the frame has no colour option. */
    static final int[][] PAL = new int[Frames.COUNT][];
    static {
        PAL[Frames.DOTS] = new int[]{0xffff9f0a, 0xff30d158, 0xff5ac8fa, 0xffff453a};
        PAL[Frames.LED] = new int[]{0xffff3b30, 0xff30d158, 0xffffb000, 0xff5ac8fa};
        PAL[Frames.TERMINAL] = new int[]{0xff30d158, 0xffffb000, 0xff5ac8fa, 0xfff06cb4};
        PAL[Frames.BREATHE] = new int[]{0xff5ac8fa, 0xff30d158, 0xffb98cf3, 0xffffb347};
        PAL[Frames.AURORA] = new int[]{0xff2ee6a6, 0xff6a7bff, 0xffff7a59, 0xfff06cb4};
        PAL[Frames.TIMER] = new int[]{0xffff9f0a, 0xff30d158, 0xff5ac8fa, 0xfff06cb4};
        PAL[Frames.ORBIT] = new int[]{0xffff9f0a, 0xff0a84ff, 0xff30d158, 0xfff06cb4};
    }
    static int accent(Context ctx, int frame) { int[] pal = PAL[frame]; int i = ctx == null ? 0 : Prefs.clamp(Prefs.i(ctx, "accent" + frame, 0), 0, pal.length - 1); return pal[i]; }

    private static final int INK = ClockFrames.INK, MUTED = ClockFrames.MUTED, RED = ClockFrames.RED;
    private static Paint paint() { Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); return p; }
    private static String date(String pattern) { return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date()); }
    private static String up(String s) { return s.toUpperCase(Locale.getDefault()); }
    private static float T(Canvas c, Paint p, String v, float x, float cy, float size, int color, Typeface f, Paint.Align a, float maxW) { return ClockFrames.text(c, p, v, x, cy, size, color, f, a, maxW); }
    /**
     * Stable numeric text: every digit gets a cell as wide as the widest digit, so a value never shifts or resizes as its digits change.
     * The layout follows the value's own shape ("1:23" is three cells, "12:34" four), so it only moves when the number of digits changes, such as 9:59 to 10:00.
     */
    private static float NT(Canvas c, Paint p, String value, float x, float cy, float size, int color, Typeface face, Paint.Align align, float maxW) {
        p.setStyle(Paint.Style.FILL); p.setShader(null); p.setTypeface(face); p.setTextAlign(Paint.Align.LEFT); p.setTextScaleX(1f); p.setTextSize(size);
        float total = cells(p, value, size), scale = maxW > 0 ? Math.min(1f, maxW / total) : 1f; p.setTextSize(size * scale); total = cells(p, value, size * scale);
        float digit = 0; for (char d = '0'; d <= '9'; d++) digit = Math.max(digit, p.measureText(String.valueOf(d))); float unit = digit + size * scale * .015f;
        float left = align == Paint.Align.CENTER ? x - total / 2 : align == Paint.Align.RIGHT ? x - total : x;
        RectF cap = Ink.bounds(p, "H0"); float base = cy - (cap.top + cap.bottom) / 2; p.setColor(color);
        for (char ch : value.toCharArray()) {
            String s1 = String.valueOf(ch); float cell = Character.isDigit(ch) ? unit : ch == ':' ? unit * .45f : p.measureText(s1);
            if (ch != ' ') { if (Character.isDigit(ch) || ch == ':') { RectF ink = Ink.bounds(p, s1); c.drawText(s1, left + (cell - ink.width()) / 2 - ink.left, base, p); } else c.drawText(s1, left, base, p); }
            left += cell;
        }
        return total;
    }
    /** Width of {@link #NT} text at the paint's current typeface and the given size. */
    private static float cells(Paint p, String value, float size) {
        p.setTextSize(size); float digit = 0; for (char d = '0'; d <= '9'; d++) digit = Math.max(digit, p.measureText(String.valueOf(d))); float unit = digit + size * .015f, total = 0;
        for (char ch : value.toCharArray()) total += Character.isDigit(ch) ? unit : ch == ':' ? unit * .45f : p.measureText(String.valueOf(ch));
        return total;
    }
    /** Spaced capitals for small labels. */
    private static void tracked(Canvas c, Paint p, String v, float x, float cy, float size, int color, Typeface f, Paint.Align a, float em, float maxW) { p.setLetterSpacing(em); T(c, p, v, x, cy, size, color, f, a, maxW); p.setLetterSpacing(0f); }
    private static int mix(int a, int b, float t) { return NowBar.mix(a, b, Math.max(0f, Math.min(1f, t))); }
    private static int alpha(int color, float a) { return (color & 0x00ffffff) | ((int) (255 * Math.max(0f, Math.min(1f, a))) << 24); }
    private static void ring(Canvas c, Paint p, Context ctx, float w, float h, boolean backing) { float r = h * .05f; ClockFrames.chargeRing(c, p, ctx, w * .93f - r * .45f, h * .115f, r, h, backing); p.setStyle(Paint.Style.FILL); }
    /** Fills the screen plus the burn-in overscan, so shifting the picture never exposes an edge. */
    private static void bleed(Canvas c, Paint p, float w, float h) { c.drawRect(-w * BurnIn.OVERSCAN, -h * BurnIn.OVERSCAN, w * (1 + BurnIn.OVERSCAN), h * (1 + BurnIn.OVERSCAN), p); }
    private static String clock(Context ctx, boolean pad) { return date(Prefs.is24(ctx) ? "HH:mm" : pad ? "hh:mm" : "h:mm"); }
    private static AlarmManager.AlarmClockInfo alarm(Context c) { AlarmManager m = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE); return m == null ? null : m.getNextAlarmClock(); }
    private static String alarmText(Context ctx) { AlarmManager.AlarmClockInfo a = alarm(ctx); return a == null ? null : new SimpleDateFormat(Prefs.is24(ctx) ? "HH:mm" : "h:mm a", Locale.getDefault()).format(new Date(a.getTriggerTime())); }

    /** True while a frame is mid-animation and wants frequent redraws (the Flip cards turning over, the Railway second hand sweeping). */
    static boolean busy(int frame) { long ms = System.currentTimeMillis(); return frame == Frames.FLIP ? ms % 60000 < FLIP_MS : frame == Frames.RAILWAY || frame == Frames.ORBIT; }
    /** The Clock app's running timer, or null when none is running or notification access is off. */
    static Timers.State runningTimer() { MediaListener l = MediaListener.instance; return l == null ? null : l.timer(); }
    private static final long FLIP_MS = 650;

    static void draw(Canvas c, float w, float h, Context ctx, int frame, int comp) {
        switch (frame) {
            case Frames.FLIP: flip(c, w, h, ctx, comp); break;
            case Frames.WORD: word(c, w, h, ctx, comp); break;
            case Frames.DOTS: dots(c, w, h, ctx, comp); break;
            case Frames.NIXIE: nixie(c, w, h, ctx, comp); break;
            case Frames.LED: led(c, w, h, ctx, comp); break;
            case Frames.RAILWAY: railway(c, w, h, ctx, comp); break;
            case Frames.ORBIT: orbit(c, w, h, ctx, comp); break;
            case Frames.SUN: sun(c, w, h, ctx, comp); break;
            case Frames.MOON: moon(c, w, h, ctx, comp); break;
            case Frames.PROGRESS: progress(c, w, h, ctx, comp); break;
            case Frames.BIGDATE: bigDate(c, w, h, ctx, comp); break;
            case Frames.WEATHER: weather(c, w, h, ctx, comp); break;
            case Frames.AURORA: aurora(c, w, h, ctx, comp); break;
            case Frames.BREATHE: breathe(c, w, h, ctx, comp); break;
            case Frames.TERMINAL: terminal(c, w, h, ctx, comp); break;
            case Frames.TIMER: timer(c, w, h, ctx, comp); break;
            default: sky(c, w, h, ctx, comp);
        }
    }

    // ───────────────────────────── FLIP ─────────────────────────────
    private static void flip(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); String t = clock(ctx, true), hh = t.substring(0, 2), mm = t.substring(3, 5); boolean info = comp == 1;
        float ch = Math.min(info ? h * .58f : h * .68f, (w * .86f - h * .05f) / 2.32f), cw = ch * 1.16f, gap = h * .05f, x0 = (w - (cw * 2 + gap)) / 2f, y0 = info ? h * .12f : (h - ch) / 2f;
        long ms = System.currentTimeMillis(); float turn = ms % 60000 < FLIP_MS ? (ms % 60000) / (float) FLIP_MS : 1f; String prev = clockAt(ctx, ms - 60000), ph = prev.substring(0, 2), pm = prev.substring(3, 5);
        card(c, p, x0, y0, cw, ch, hh, ph, hh.equals(ph) ? 1f : turn); card(c, p, x0 + cw + gap, y0, cw, ch, mm, pm, mm.equals(pm) ? 1f : turn);
        if (!Prefs.is24(ctx)) tracked(c, p, date("a").toUpperCase(Locale.getDefault()), x0 + cw * .09f, y0 + ch * .10f, ch * .06f, MUTED, Fonts.UI_BOLD, Paint.Align.LEFT, .08f, 0);
        if (info) { T(c, p, up(date("EEEE")) + "   " + date("d") + " " + up(date("MMM")), w / 2f, h * .865f, h * .062f, 0xffd1d1d6, Fonts.UI_SEMI, Paint.Align.CENTER, w * .8f); }
        ring(c, p, ctx, w, h, false);
    }
    private static String clockAt(Context ctx, long ms) { return new SimpleDateFormat(Prefs.is24(ctx) ? "HH:mm" : "hh:mm", Locale.getDefault()).format(new Date(ms)); }
    /** A card that turns over: the top half of the old digits folds down, then the bottom half of the new ones. turn 0..1; 1 is at rest. */
    private static void card(Canvas c, Paint p, float x, float y, float cw, float ch, String digits, String before, float turn) {
        if (turn >= 1f) { card(c, p, x, y, cw, ch, digits); return; }
        float mid = y + ch / 2f, ease = turn * turn * (3 - 2 * turn);
        card(c, p, x, y, cw, ch, ease < .5f ? before : digits);      // the static card behind shows the old digits first, then the new
        c.save(); c.clipRect(x, ease < .5f ? y : mid, x + cw, ease < .5f ? mid : y + ch);
        if (ease < .5f) { float k = 1f - ease * 2f; c.scale(1f, Math.max(.001f, k), x, mid); card(c, p, x, y, cw, ch, before); } else { float k = (ease - .5f) * 2f; c.scale(1f, Math.max(.001f, k), x, mid); card(c, p, x, y, cw, ch, digits); }
        c.restore();
    }
    private static void card(Canvas c, Paint p, float x, float y, float cw, float ch, String digits) {
        float r = ch * .075f, mid = y + ch / 2f; p.setStyle(Paint.Style.FILL); p.setShader(null); p.setColor(0xff0c0c0d); p.setShadowLayer(ch * .04f, 0, ch * .02f, 0xaa000000); c.drawRoundRect(x, y, x + cw, y + ch, r, r, p); p.setShadowLayer(0, 0, 0, 0);
        c.save(); Path clip = new Path(); clip.addRoundRect(x, y, x + cw, y + ch, r, r, Path.Direction.CW); c.clipPath(clip);
        p.setShader(new LinearGradient(0, y, 0, mid, 0xff2b2b2e, 0xff202022, Shader.TileMode.CLAMP)); c.drawRect(x, y, x + cw, mid, p);
        p.setShader(new LinearGradient(0, mid, 0, y + ch, 0xff19191b, 0xff131315, Shader.TileMode.CLAMP)); c.drawRect(x, mid, x + cw, y + ch, p); p.setShader(null);
        NT(c, p, digits, x + cw / 2f, mid, ch * 1.02f, 0xfff2f2f7, Fonts.CONDENSED, Paint.Align.CENTER, cw * .86f);
        p.setColor(0xff000000); c.drawRect(x, mid - ch * .006f, x + cw, mid + ch * .006f, p);
        p.setColor(0x14ffffff); c.drawRect(x, y, x + cw, y + ch * .004f, p);
        c.restore();
        p.setColor(0xff000000); float nw = cw * .022f, nh = ch * .05f; c.drawRoundRect(x - nw / 2, mid - nh / 2, x + nw, mid + nh / 2, nw / 2, nw / 2, p); c.drawRoundRect(x + cw - nw, mid - nh / 2, x + cw + nw / 2, mid + nh / 2, nw / 2, nw / 2, p);
    }

    // ───────────────────────────── WORD CLOCK ─────────────────────────────
    private static void word(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); Calendar now = Calendar.getInstance(); List<int[]> lit = WordClock.lit(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        boolean[][] on = new boolean[WordClock.ROWS][WordClock.COLS]; for (int[] r : lit) for (int i = 0; i < r[2]; i++) on[r[0]][r[1] + i] = true;
        float cell = h * .080f, cellX = comp == 1 ? Math.min(cell * 1.25f, w * .46f / WordClock.COLS) : Math.min(cell * 1.5f, w * .76f / WordClock.COLS), gx = comp == 1 ? w * .07f : (w - cellX * WordClock.COLS) / 2f, gy = (h - cell * WordClock.ROWS) / 2f;
        p.setTypeface(Fonts.UI_BOLD); p.setTextSize(cell * .60f); p.setTextAlign(Paint.Align.CENTER); RectF cap = Ink.bounds(p, "H"); float base = -(cap.top + cap.bottom) / 2f;
        for (int r = 0; r < WordClock.ROWS; r++) for (int col = 0; col < WordClock.COLS; col++) {
            boolean lightIt = on[r][col]; p.setColor(lightIt ? INK : 0xff2a2a2e); p.setTextAlign(Paint.Align.CENTER);
            if (lightIt) p.setShadowLayer(cell * .25f, 0, 0, 0x66ffffff);
            c.drawText(WordClock.GRID[r].substring(col, col + 1), gx + cellX * (col + .5f), gy + cell * (r + .5f) + base, p); p.setShadowLayer(0, 0, 0, 0);
        }
        int extra = Calendar.getInstance().get(Calendar.MINUTE) % 5; float gw0 = cellX * WordClock.COLS, m = cell * .45f; p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 4; i++) { float dx = i == 0 || i == 3 ? gx - m : gx + gw0 + m, dy = i < 2 ? gy - m * .55f : gy + cell * WordClock.ROWS + m * .55f; if (i == 1 || i == 2) dx = gx + gw0 + m; if (i == 0 || i == 3) dx = gx - m; p.setColor(i < extra ? INK : 0xff2a2a2e); c.drawCircle(dx, dy, cell * .075f, p); }
        if (comp == 1) {
            float x = w * .60f; T(c, p, up(date("EEEE")), x, h * .24f, h * .085f, RED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .3f);
            NT(c, p, date("d"), x, h * .52f, h * .34f, INK, Fonts.UI_MED, Paint.Align.LEFT, w * .3f); T(c, p, up(date("MMMM yyyy")), x, h * .80f, h * .05f, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .3f);
        }
        ring(c, p, ctx, w, h, false);
    }

    // ───────────────────────────── DOT MATRIX ─────────────────────────────
    private static final String[][] DOT_FONT = {
        {"01110", "10001", "10011", "10101", "11001", "10001", "01110"}, {"00100", "01100", "00100", "00100", "00100", "00100", "01110"},
        {"01110", "10001", "00001", "00010", "00100", "01000", "11111"}, {"11110", "00001", "00001", "01110", "00001", "00001", "11110"},
        {"00010", "00110", "01010", "10010", "11111", "00010", "00010"}, {"11111", "10000", "11110", "00001", "00001", "10001", "01110"},
        {"00110", "01000", "10000", "11110", "10001", "10001", "01110"}, {"11111", "00001", "00010", "00100", "01000", "01000", "01000"},
        {"01110", "10001", "10001", "01110", "10001", "10001", "01110"}, {"01110", "10001", "10001", "01111", "00001", "00010", "01100"}};
    private static void dots(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); int accent = accent(ctx, Frames.DOTS); String t = clock(ctx, false); if (t.length() == 4) t = " " + t;       // fixed layout: a blank first cell for 1..9 o'clock
        int cols = 27; float pitch = Math.min(w * .86f / cols, h * .13f), x0 = (w - pitch * cols) / 2f, y0 = comp == 1 ? h * .21f : (h - pitch * 7) / 2f;
        int[] starts = {0, 6, -1, 16, 22};
        for (int i = 0; i < 5; i++) {
            char ch = t.charAt(i); if (i == 2) { for (int row : new int[]{2, 4}) dot(c, p, x0 + pitch * (13 + .5f), y0 + pitch * (row + .5f), pitch, true, accent); continue; }
            for (int row = 0; row < 7; row++) for (int col = 0; col < 5; col++) { boolean lit = ch != ' ' && DOT_FONT[ch - '0'][row].charAt(col) == '1'; dot(c, p, x0 + pitch * (starts[i] + col + .5f), y0 + pitch * (row + .5f), pitch, lit, accent); }
        }
        if (comp == 1) { tracked(c, p, up(date("EEEE d MMMM")), w / 2f, h * .80f, h * .065f, accent, Fonts.UI_SEMI, Paint.Align.CENTER, .14f, w * .8f); String a = alarmText(ctx); if (a != null) T(c, p, "Alarm  " + a, w / 2f, h * .86f, h * .045f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, w * .6f); }
        ring(c, p, ctx, w, h, false);
    }
    private static void dot(Canvas c, Paint p, float x, float y, float pitch, boolean lit, int accent) {
        p.setStyle(Paint.Style.FILL);
        if (lit) { p.setColor(alpha(accent, .22f)); c.drawCircle(x, y, pitch * .62f, p); p.setColor(accent); c.drawCircle(x, y, pitch * .38f, p); p.setColor(alpha(0xffffffff, .35f)); c.drawCircle(x - pitch * .09f, y - pitch * .09f, pitch * .14f, p); }
        else { p.setColor(0xff1b1b1e); c.drawCircle(x, y, pitch * .28f, p); }
    }

    // ───────────────────────────── NIXIE ─────────────────────────────
    private static void nixie(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); boolean secs = comp == 1; String t = (Prefs.is24(ctx) ? date("HHmm") : date("hhmm")) + (secs ? date("ss") : ""); int n = t.length(), colons = secs ? 2 : 1;
        float th = h * .64f, tw = Math.min(th * .46f, w * .86f / (n + colons * .75f + (n - 1) * .22f)), gap = tw * .22f, colonW = tw * .6f, total = tw * n + gap * (n - 1) + colonW * colons;
        float x = (w - total) / 2f, y = (h - th) / 2f - h * .02f;
        for (int i = 0; i < n; i++) {
            tube(c, p, x, y, tw, th, t.charAt(i)); x += tw;
            if ((i == 1 && n > 2) || (i == 3 && secs)) { x += gap / 2f; colonDots(c, p, x + colonW / 2f, y + th * .42f, th); x += colonW + gap / 2f; } else x += gap;
        }
        ring(c, p, ctx, w, h, false);
    }
    private static void colonDots(Canvas c, Paint p, float cx, float cy, float th) {
        for (float dy : new float[]{-th * .07f, th * .07f}) { p.setStyle(Paint.Style.FILL); p.setColor(0x44ff7a1a); c.drawCircle(cx, cy + dy, th * .035f, p); p.setColor(0xffffa24d); c.drawCircle(cx, cy + dy, th * .017f, p); }
    }
    private static void tube(Canvas c, Paint p, float x, float y, float tw, float th, char digit) {
        float r = tw * .5f, baseH = th * .09f; RectF glass = new RectF(x, y, x + tw, y + th - baseH);
        Path body = new Path(); body.addRoundRect(glass, new float[]{r, r, r, r, tw * .08f, tw * .08f, tw * .08f, tw * .08f}, Path.Direction.CW);
        p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(x, y, x + tw, y, 0xff241913, 0xff0a0706, Shader.TileMode.CLAMP)); c.drawPath(body, p); p.setShader(null);
        c.save(); c.clipPath(body); p.setShader(new RadialGradient(x + tw / 2f, y + th * .46f, tw * .95f, 0x55ff7a1a, 0x00ff7a1a, Shader.TileMode.CLAMP)); c.drawRect(glass, p); p.setShader(null);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(tw * .008f); p.setColor(0x1aff9a4d);
        for (float d = -th; d < tw + th; d += tw * .12f) { c.drawLine(x + d, y, x + d - th * .5f, y + th, p); c.drawLine(x + d - th * .5f, y, x + d, y + th, p); }
        c.restore();
        p.setStyle(Paint.Style.FILL); p.setShadowLayer(tw * .10f, 0, 0, 0xccff6a00);
        p.setTypeface(Fonts.UI_MED); p.setTextSize(th * .50f); float widest=0; for(char d='0';d<='9';d++) widest=Math.max(widest,p.measureText(String.valueOf(d)));
        T(c, p, String.valueOf(digit), x + tw / 2f, y + th * .46f, th * .50f * Math.min(1f,tw*.8f/widest), 0xffffb25e, Fonts.UI_MED, Paint.Align.CENTER, 0); p.setShadowLayer(0, 0, 0, 0);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(tw * .014f); p.setColor(0x44ffb066); c.drawPath(body, p);
        p.setColor(0x26ffffff); p.setStrokeWidth(tw * .02f); c.drawLine(x + tw * .17f, y + r * .9f, x + tw * .17f, y + th * .55f, p);
        p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(0, y + th - baseH, 0, y + th, 0xff8a5b30, 0xff3a2512, Shader.TileMode.CLAMP)); c.drawRoundRect(x + tw * .04f, y + th - baseH, x + tw * .96f, y + th, tw * .05f, tw * .05f, p); p.setShader(null);
        p.setColor(0x55ffb066); for (int i = 0; i < 5; i++) c.drawRect(x + tw * (.18f + i * .16f), y + th, x + tw * (.18f + i * .16f) + tw * .02f, y + th + baseH * .55f, p);
        p.setShader(new RadialGradient(x + tw / 2f, y + th + baseH, tw * .9f, 0x33ff7a1a, 0x00ff7a1a, Shader.TileMode.CLAMP)); c.drawRect(x - tw * .4f, y + th - baseH, x + tw * 1.4f, y + th + baseH * 2.4f, p); p.setShader(null);
    }

    // ───────────────────────────── ALARM LED (seven segment) ─────────────────────────────
    private static final int[] SEG = {0x3f, 0x06, 0x5b, 0x4f, 0x66, 0x6d, 0x7d, 0x07, 0x7f, 0x6f};   // bits: a b c d e f g
    private static void led(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); int accent = accent(ctx, Frames.LED); boolean info = comp == 1; String t = clock(ctx, false); if (t.length() == 4) t = " " + t;
        float H = Math.min(info ? h * .50f : h * .66f, w * .84f / 2.84f), W = H * .47f, T = H * .105f, gap = W * .30f, colonW = W * .34f, total = W * 4 + gap * 3 + colonW + gap * 2, x0 = (w - total) / 2f, y0 = info ? h * .12f : (h - H) / 2f;
        c.save(); c.translate(w / 2f, y0 + H); c.skew(-.09f, 0); c.translate(-w / 2f, -(y0 + H));
        float x = x0;
        for (int i = 0; i < 5; i++) {
            if (i == 2) { float cx = x + colonW / 2f; p.setStyle(Paint.Style.FILL); p.setColor(accent); p.setShadowLayer(T * .9f, 0, 0, alpha(accent, .8f)); c.drawRect(cx - T / 2, y0 + H * .30f, cx + T / 2, y0 + H * .30f + T, p); c.drawRect(cx - T / 2, y0 + H * .66f, cx + T / 2, y0 + H * .66f + T, p); p.setShadowLayer(0, 0, 0, 0); x += colonW + gap; continue; }
            char ch = t.charAt(i); sevenSeg(c, p, x, y0, W, H, T, ch == ' ' ? 0 : SEG[ch - '0'], accent); x += W + gap;
        }
        c.restore();
        float ty = info ? h * .74f : h * .93f;
        if (!Prefs.is24(ctx)) tracked(c, p, date("a").toUpperCase(Locale.getDefault()), x0 + total, y0 + H + h * .045f, h * .045f, accent, Fonts.UI_BOLD, Paint.Align.RIGHT, .1f, 0);
        if (info) {
            tracked(c, p, up(date("EEE d MMM")), x0, ty + h * .05f, h * .065f, accent, Fonts.UI_BOLD, Paint.Align.LEFT, .1f, w * .4f); String a = alarmText(ctx);
            if (a != null) { T(c, p, "ALARM  " + a, x0 + total, ty + h * .05f, h * .065f, alpha(accent, .75f), Fonts.UI_BOLD, Paint.Align.RIGHT, w * .4f); }
        }
        ring(c, p, ctx, w, h, false);
    }
    private static void sevenSeg(Canvas c, Paint p, float x, float y, float W, float H, float T, int mask, int accent) {
        float g = T * .24f, lenH = W - T - 2 * g, lenV = H / 2f - T / 2f - 2 * g; Path[] seg = new Path[7];
        seg[0] = hex(x + T / 2f + g, y, lenH, T); seg[6] = hex(x + T / 2f + g, y + H / 2f - T / 2f, lenH, T); seg[3] = hex(x + T / 2f + g, y + H - T, lenH, T);
        seg[5] = vex(x, y + T / 2f + g, lenV, T); seg[1] = vex(x + W - T, y + T / 2f + g, lenV, T); seg[4] = vex(x, y + H / 2f + g, lenV, T); seg[2] = vex(x + W - T, y + H / 2f + g, lenV, T);
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 7; i++) {
            boolean lit = (mask >> i & 1) != 0;
            if (lit) { p.setColor(accent); p.setShadowLayer(T * .9f, 0, 0, alpha(accent, .75f)); } else { p.setColor(alpha(accent, .075f)); p.setShadowLayer(0, 0, 0, 0); }
            c.drawPath(seg[i], p);
        }
        p.setShadowLayer(0, 0, 0, 0);
    }
    private static Path hex(float x, float y, float len, float t) { Path q = new Path(); q.moveTo(x + t / 2, y); q.lineTo(x + len - t / 2, y); q.lineTo(x + len, y + t / 2); q.lineTo(x + len - t / 2, y + t); q.lineTo(x + t / 2, y + t); q.lineTo(x, y + t / 2); q.close(); return q; }
    private static Path vex(float x, float y, float len, float t) { Path q = new Path(); q.moveTo(x + t / 2, y); q.lineTo(x + t, y + t / 2); q.lineTo(x + t, y + len - t / 2); q.lineTo(x + t / 2, y + len); q.lineTo(x, y + len - t / 2); q.lineTo(x, y + t / 2); q.close(); return q; }

    // ───────────────────────────── RAILWAY ─────────────────────────────
    private static void railway(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); boolean info = comp == 1; float cx = info ? w * .275f : w / 2f, cy = h / 2f, r = info ? ClockFrames.dialRadius(w, h) : Math.min(h * .41f, w * .40f);
        p.setStyle(Paint.Style.FILL); p.setColor(0xff111113); c.drawCircle(cx, cy, r, p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(r * .028f); p.setColor(0xff2d2d31); c.drawCircle(cx, cy, r * .985f, p);
        for (int i = 0; i < 60; i++) {
            double a = i * Math.PI / 30; boolean hour = i % 5 == 0; float in = hour ? .78f : .925f, out = .955f; p.setStrokeCap(Paint.Cap.BUTT); p.setStrokeWidth(r * (hour ? .055f : .016f)); p.setColor(hour ? INK : 0xffb4b4b8);
            c.drawLine(cx + (float) Math.sin(a) * r * in, cy - (float) Math.cos(a) * r * in, cx + (float) Math.sin(a) * r * out, cy - (float) Math.cos(a) * r * out, p);
        }
        Calendar now = Calendar.getInstance(); float sec = now.get(Calendar.SECOND), min = now.get(Calendar.MINUTE) + sec / 60f, hr = now.get(Calendar.HOUR) + min / 60f;
        bar(c, p, cx, cy, hr * 30f, r * .52f, r * .09f, r * .10f, INK); bar(c, p, cx, cy, min * 6f, r * .86f, r * .06f, r * .12f, INK);
        float sweep = Math.min(sec + now.get(Calendar.MILLISECOND) / 1000f, 58.5f); float sa = sweep / 58.5f * 360f; p.setStyle(Paint.Style.FILL); bar(c, p, cx, cy, sa, r * .63f, r * .02f, r * .22f, 0xffe5322d);
        double ra = Math.toRadians(sa); p.setColor(0xffe5322d); c.drawCircle(cx + (float) Math.sin(ra) * r * .63f, cy - (float) Math.cos(ra) * r * .63f, r * .075f, p); c.drawCircle(cx, cy, r * .035f, p);
        if (info) { float x = w * .565f; T(c, p, up(date("EEEE")), x, h * .22f, h * .10f, 0xffe5322d, Fonts.UI_SEMI, Paint.Align.LEFT, w * .3f); NT(c, p, date("d"), x, h * .55f, h * .34f, INK, Fonts.UI_MED, Paint.Align.LEFT, w * .3f); T(c, p, up(date("MMMM")), x, h * .82f, h * .06f, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .3f); }
        ring(c, p, ctx, w, h, false);
    }
    private static void bar(Canvas c, Paint p, float cx, float cy, float deg, float len, float width, float tail, int color) {
        c.save(); c.rotate(deg, cx, cy); p.setStyle(Paint.Style.FILL); p.setColor(color); c.drawRoundRect(cx - width / 2, cy - len, cx + width / 2, cy + tail, width * .25f, width * .25f, p); c.restore();
    }

    // ───────────────────────────── ORBIT ─────────────────────────────
    private static void orbit(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); boolean info = comp == 1; int accent = accent(ctx, Frames.ORBIT); float cx = info ? w * .27f : w / 2f, cy = h * .5f, k = info ? Math.min(h * .98f, w * .44f) : Math.min(h * .98f, w * .86f);
        Calendar now = Calendar.getInstance(); float ms = now.get(Calendar.MILLISECOND), sec = now.get(Calendar.SECOND), min = now.get(Calendar.MINUTE) + sec / 60f, hr = now.get(Calendar.HOUR) + min / 60f;
        float[] radius = {.20f * k, .315f * k, .43f * k}, frac = {hr / 12f, min / 60f, sec / 60f}; int[] col = {accent, 0xff0a84ff, 0xffd1d1d6}; if (accent == 0xff0a84ff) col[1] = 0xffff9f0a;
        float sw = k * .026f;
        for (int i = 0; i < 3; i++) {
            RectF oval = new RectF(cx - radius[i], cy - radius[i], cx + radius[i], cy + radius[i]); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sw); p.setStrokeCap(Paint.Cap.ROUND); p.setColor(0xff1c1c1f); c.drawCircle(cx, cy, radius[i], p);
            if (frac[i] > 0) { p.setColor(alpha(col[i], .85f)); c.drawArc(oval, -90, 360f * frac[i], false, p); }
            double a = frac[i] * 2 * Math.PI; float px = cx + (float) Math.sin(a) * radius[i], py = cy - (float) Math.cos(a) * radius[i];
            p.setStyle(Paint.Style.FILL); p.setColor(alpha(col[i], .28f)); c.drawCircle(px, py, k * .052f, p); p.setColor(col[i]); c.drawCircle(px, py, k * .031f, p);
        }
        String t = date(Prefs.is24(ctx) ? "HH:mm" : "h:mm"); NT(c, p, t, cx, cy, k * .115f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, radius[0] * 1.55f);
        if (info) {
            float x = w * .575f; T(c, p, up(date("EEEE")), x, h * .22f, h * .085f, accent, Fonts.UI_SEMI, Paint.Align.LEFT, w * .3f); NT(c, p, date("d"), x, h * .55f, h * .34f, INK, Fonts.UI_MED, Paint.Align.LEFT, w * .16f);
            T(c, p, up(date("MMMM")), x, h * .82f, h * .06f, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .3f);
        }
        ring(c, p, ctx, w, h, false);
    }

    // ───────────────────────────── SUN PATH ─────────────────────────────
    private static void sun(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); ring(c, p, ctx, w, h, false);
        if (!Weather.hasPlace()) { T(c, p, "Sun path needs your location", w / 2f, h * .46f, h * .075f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, w * .8f); T(c, p, "Allow location for Standby in Android settings", w / 2f, h * .57f, h * .045f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, w * .8f); return; }
        long now = System.currentTimeMillis(); long[] s = Astro.sun(Weather.lat, Weather.lon, now); if (s == null) { T(c, p, Weather.lat > 0 == (Calendar.getInstance().get(Calendar.MONTH) >= 3 && Calendar.getInstance().get(Calendar.MONTH) <= 8) ? "Midnight sun" : "Polar night", w / 2f, h * .5f, h * .09f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, w * .8f); return; }
        SimpleDateFormat f = new SimpleDateFormat(Prefs.is24(ctx) ? "HH:mm" : "h:mm a", Locale.getDefault());
        float cx = w / 2f, base = h * .74f, rx = w * .37f, ry = h * .56f; float t = (now - s[0]) / (float) (s[1] - s[0]); boolean day = t >= 0 && t <= 1; float tc = Math.max(0f, Math.min(1f, t));
        RectF oval = new RectF(cx - rx, base - ry, cx + rx, base + ry);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * .007f); p.setColor(0xff2a2a2e); c.drawArc(oval, 180, 180, false, p);
        p.setStrokeWidth(h * .012f); p.setColor(0xffffb340); if (tc > 0) c.drawArc(oval, 180, 180f * tc, false, p);
        p.setStrokeWidth(h * .004f); p.setColor(0xff3a3a3e); c.drawLine(w * .07f, base, w * .93f, base, p);
        double a = Math.PI * tc; float sx = cx - (float) Math.cos(a) * rx, sy = base - (float) Math.sin(a) * ry;
        p.setStyle(Paint.Style.FILL);
        if (day) { p.setShader(new RadialGradient(sx, sy, h * .13f, 0x66ffb340, 0x00ffb340, Shader.TileMode.CLAMP)); c.drawCircle(sx, sy, h * .13f, p); p.setShader(null); p.setColor(0xffffc04d); c.drawCircle(sx, sy, h * .042f, p); }
        else { p.setColor(0xff48484d); c.drawCircle(t < 0 ? cx - rx : cx + rx, base, h * .028f, p); }
        T(c, p, "SUNRISE", cx - rx, base + h * .07f, h * .035f, MUTED, Fonts.UI_SEMI, Paint.Align.CENTER, w * .2f); T(c, p, f.format(new Date(s[0])), cx - rx, base + h * .15f, h * .07f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, w * .2f);
        T(c, p, "SUNSET", cx + rx, base + h * .07f, h * .035f, MUTED, Fonts.UI_SEMI, Paint.Align.CENTER, w * .2f); T(c, p, f.format(new Date(s[1])), cx + rx, base + h * .15f, h * .07f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, w * .2f);
        if (comp == 1) NT(c, p, clock(ctx, false), cx, h * .58f, h * .20f, INK, Fonts.UI_MED, Paint.Align.CENTER, rx * 1.2f);
        else { long left = day ? s[1] - now : now < s[0] ? s[0] - now : 0; String line = day ? "Sunset in " + span(left) : now < s[0] ? "Sunrise in " + span(left) : "The sun has set"; T(c, p, line, cx, h * .55f, h * .08f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, rx * 1.5f); T(c, p, span(s[1] - s[0]) + " of daylight", cx, h * .645f, h * .045f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, rx * 1.5f); }
    }
    private static String span(long ms) { long m = Math.max(0, ms / 60000); return m >= 60 ? (m / 60) + " h " + (m % 60) + " min" : m + " min"; }

    // ───────────────────────────── MOON ─────────────────────────────
    private static void moon(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); long now = System.currentTimeMillis(); double phase = Astro.moonPhase(now), lit = Astro.illumination(phase);
        Random rnd = new Random(11); p.setStyle(Paint.Style.FILL); for (int i = 0; i < 46; i++) { float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h, s = h * (.002f + rnd.nextFloat() * .003f); p.setColor(alpha(0xffffffff, .10f + rnd.nextFloat() * .30f)); c.drawCircle(x, y, s, p); }
        float cx = w * .26f, cy = h * .5f, r = h * .37f; moonDisc(c, p, cx, cy, r, (float) phase);
        float x = w * .54f;
        if (comp == 0) {
            T(c, p, Astro.phaseName(phase), x, h * .33f, h * .085f, INK, Fonts.UI_BOLD, Paint.Align.LEFT, w * .4f); T(c, p, Math.round(lit * 100) + "% illuminated", x, h * .45f, h * .05f, MUTED, Fonts.UI_MED, Paint.Align.LEFT, w * .4f);
            double toFull = Astro.daysToFull(phase), toNew = Astro.daysToNew(phase); boolean full = toFull <= toNew; double d = full ? toFull : toNew; long when = now + (long) (d * 86400000L);
            T(c, p, (full ? "Full moon " : "New moon ") + date2(when), x, h * .60f, h * .055f, 0xffd1d1d6, Fonts.UI_SEMI, Paint.Align.LEFT, w * .4f); T(c, p, "in " + Math.max(1, (int) Math.round(d)) + (Math.round(d) == 1 ? " day" : " days"), x, h * .69f, h * .045f, MUTED, Fonts.UI_MED, Paint.Align.LEFT, w * .4f);
        } else {
            NT(c, p, clock(ctx, false), x, h * .42f, h * .30f, INK, Fonts.UI_MED, Paint.Align.LEFT, w * .38f); T(c, p, Astro.phaseName(phase) + "  ·  " + Math.round(lit * 100) + "%", x, h * .70f, h * .055f, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .4f);
        }
        ring(c, p, ctx, w, h, false);
    }
    private static String date2(long ms) { return new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date(ms)); }
    /** Phase 0 new .. .5 full .. 1 new. Waxing lights the right side, waning the left. */
    private static void moonDisc(Canvas c, Paint p, float cx, float cy, float r, float phase) {
        p.setStyle(Paint.Style.FILL); p.setShader(null); p.setColor(0xff161618); c.drawCircle(cx, cy, r, p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(r * .012f); p.setColor(0xff26262a); c.drawCircle(cx, cy, r, p); p.setStyle(Paint.Style.FILL);
        boolean waxing = phase <= .5f; float k = (float) Math.abs(Math.cos(2 * Math.PI * phase)); boolean crescent = phase < .25f || phase > .75f;
        c.save(); if (!waxing) c.scale(-1f, 1f, cx, cy);
        Path lit = new Path(); RectF disc = new RectF(cx - r, cy - r, cx + r, cy + r), term = new RectF(cx - k * r, cy - r, cx + k * r, cy + r);
        lit.moveTo(cx, cy - r); lit.arcTo(disc, -90, 180); lit.arcTo(term, 90, crescent ? -180 : 180); lit.close();
        p.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r, 0xfff7f4ea, 0xffc9c4b6, Shader.TileMode.CLAMP)); c.drawPath(lit, p); p.setShader(null);
        c.save(); c.clipPath(lit); p.setColor(0x22000000); Random rnd = new Random(5); for (int i = 0; i < 9; i++) { float a = rnd.nextFloat() * 6.283f, d = rnd.nextFloat() * r * .72f, rr = r * (.05f + rnd.nextFloat() * .09f); c.drawCircle(cx + (float) Math.cos(a) * d, cy + (float) Math.sin(a) * d, rr, p); } c.restore();
        c.restore();
    }

    // ───────────────────────────── TIME LEFT ─────────────────────────────
    private static void progress(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); Calendar n = Calendar.getInstance(); float dayFrac = (n.get(Calendar.HOUR_OF_DAY) * 3600f + n.get(Calendar.MINUTE) * 60f + n.get(Calendar.SECOND)) / 86400f;
        int fdow = Prefs.firstDow(ctx), wd = (n.get(Calendar.DAY_OF_WEEK) - fdow + 7) % 7, dom = n.get(Calendar.DAY_OF_MONTH), dim = n.getActualMaximum(Calendar.DAY_OF_MONTH), doy = n.get(Calendar.DAY_OF_YEAR), diy = n.getActualMaximum(Calendar.DAY_OF_YEAR);
        float[] frac = {dayFrac, (wd + dayFrac) / 7f, (dom - 1 + dayFrac) / dim, (doy - 1 + dayFrac) / diy};
        if (comp == 0) {
            String[] names = {"Today", "This week", "This month", "This year"}; int[] cols = {0xff30d158, 0xff5ac8fa, 0xffff9f0a, 0xfff06cb4};
            T(c, p, up(date("EEEE d MMMM")), w * .07f, h * .115f, h * .05f, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .5f);
            for (int i = 0; i < 4; i++) {
                float y0 = h * (.25f + .17f * i), left = w * .07f, right = w * .93f;
                T(c, p, names[i], left, y0, h * .06f, INK, Fonts.UI_SEMI, Paint.Align.LEFT, w * .4f); T(c, p, Math.round(frac[i] * 100) + "%", right, y0, h * .075f, cols[i], Fonts.ROUNDED_BOLD, Paint.Align.RIGHT, w * .2f);
                float by = y0 + h * .075f, bh = h * .034f; p.setStyle(Paint.Style.FILL); p.setColor(0xff1c1c1f); c.drawRoundRect(left, by - bh / 2, right, by + bh / 2, bh / 2, bh / 2, p);
                p.setColor(cols[i]); float fr = Math.max(bh, (right - left) * frac[i]); c.drawRoundRect(left, by - bh / 2, left + fr, by + bh / 2, bh / 2, bh / 2, p);
            }
        } else {
            Calendar first = Calendar.getInstance(); first.set(Calendar.DAY_OF_YEAR, 1); int off = (first.get(Calendar.DAY_OF_WEEK) - fdow + 7) % 7, cols = (off + diy + 6) / 7;
            float pitch = Math.min(w * .86f / cols, h * .105f), gw = pitch * cols, gx = (w - gw) / 2f, gy = h * .56f;
            T(c, p, Integer.toString(n.get(Calendar.YEAR)), gx, h * .20f, h * .17f, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT, w * .3f);
            T(c, p, "Day " + doy + " of " + diy + "   ·   " + Math.round(frac[3] * 100) + "%   ·   " + (diy - doy) + " left", gx + gw, h * .21f, h * .05f, MUTED, Fonts.UI_SEMI, Paint.Align.RIGHT, w * .5f);
            for (int d = 0; d < diy; d++) {
                int slot = off + d, col = slot / 7, row = slot % 7; float x = gx + pitch * (col + .5f), y = gy - pitch * 3.5f + pitch * (row + .5f);
                boolean past = d + 1 < doy, today = d + 1 == doy; p.setStyle(Paint.Style.FILL); p.setColor(today ? INK : past ? 0xff30d158 : 0xff252528); c.drawCircle(x, y, pitch * (today ? .46f : .34f), p);
            }
            float gap = h * .05f; T(c, p, "Each dot is a day. Columns are weeks.", gx, gy + pitch * 3.5f + gap + h * .02f, h * .04f, 0xff636366, Fonts.UI_MED, Paint.Align.LEFT, w * .6f);
        }
        ring(c, p, ctx, w, h, false);
    }

    // ───────────────────────────── BIG DATE ─────────────────────────────
    private static void bigDate(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); Calendar n = Calendar.getInstance();
        if (comp == 0) {
            NT(c, p, date("d"), w * .27f, h * .52f, h * 1.02f, RED, Fonts.CONDENSED, Paint.Align.CENTER, w * .40f);
            float x = w * .55f; T(c, p, up(date("EEEE")), x, h * .31f, h * .115f, INK, Fonts.UI_BOLD, Paint.Align.LEFT, w * .38f); T(c, p, up(date("MMMM yyyy")), x, h * .44f, h * .062f, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .38f);
            p.setColor(0xff2a2a2e); c.drawRect(x, h * .535f, x + w * .30f, h * .54f, p);
            T(c, p, "Week " + n.get(Calendar.WEEK_OF_YEAR), x, h * .63f, h * .05f, 0xffd1d1d6, Fonts.UI_SEMI, Paint.Align.LEFT, w * .38f); T(c, p, "Day " + n.get(Calendar.DAY_OF_YEAR) + " of " + n.getActualMaximum(Calendar.DAY_OF_YEAR), x, h * .71f, h * .05f, MUTED, Fonts.UI_MED, Paint.Align.LEFT, w * .38f);
        } else {
            T(c, p, up(date("EEE")), w * .27f, h * .50f, h * 1.0f, RED, Fonts.CONDENSED, Paint.Align.CENTER, w * .42f);
            p.setColor(0xff2a2a2e); c.drawRect(w * .5f - h * .004f, h * .17f, w * .5f + h * .004f, h * .83f, p);
            NT(c, p, date("d"), w * .745f, h * .46f, h * .92f, INK, Fonts.CONDENSED, Paint.Align.CENTER, w * .40f); tracked(c, p, up(date("MMMM")), w * .745f, h * .85f, h * .06f, MUTED, Fonts.UI_SEMI, Paint.Align.CENTER, .2f, w * .4f);
        }
        ring(c, p, ctx, w, h, false);
    }

    // ───────────────────────────── WEATHER ─────────────────────────────
    private static void weather(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); int code = Weather.ready() ? Weather.code : 0; int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); boolean night = hour >= 20 || hour < 6;
        int top, bottom; if (code >= 95) { top = 0xff2a2145; bottom = 0xff08060e; } else if (code >= 71 && code <= 77 || code == 85 || code == 86) { top = 0xff2d3649; bottom = 0xff0a0c11; } else if (code >= 51 && code <= 67 || code >= 80 && code <= 82) { top = 0xff143842; bottom = 0xff050c0f; }
        else if (code == 3 || code == 45 || code == 48) { top = 0xff2c313c; bottom = 0xff090a0d; } else { top = 0xff123a74; bottom = 0xff040a18; }
        if (night) { top = mix(top, 0xff000000, .55f); bottom = mix(bottom, 0xff000000, .3f); }
        p.setShader(new LinearGradient(0, 0, w * .35f, h, top, bottom, Shader.TileMode.CLAMP)); bleed(c, p, w, h); p.setShader(null); ring(c, p, ctx, w, h, true);
        if (!Weather.ready()) { T(c, p, "No weather yet", w / 2f, h * .46f, h * .08f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, w * .8f); T(c, p, "Allow location for Standby in Android settings", w / 2f, h * .57f, h * .045f, 0xb3ffffff, Fonts.UI_MED, Paint.Align.CENTER, w * .8f); return; }
        String temp = Weather.shown(ctx, Weather.temp) + "°";
        if (comp == 0) {
            Weather.icon(c, p, code, w * .21f, h * .47f, h * .52f);
            float x = w * .40f; NT(c, p, temp, x, h * .43f, h * .46f, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT, w * .30f); T(c, p, Weather.describe(code), x, h * .70f, h * .075f, INK, Fonts.UI_SEMI, Paint.Align.LEFT, w * .32f);
            T(c, p, "H:" + Weather.shown(ctx, Weather.hi) + "°   L:" + Weather.shown(ctx, Weather.lo) + "°", x, h * .81f, h * .06f, 0xccffffff, Fonts.UI_MED, Paint.Align.LEFT, w * .32f);
            if (!Weather.city.isEmpty()) T(c, p, Weather.city, w * .07f, h * .115f, h * .06f, 0xe6ffffff, Fonts.UI_SEMI, Paint.Align.LEFT, w * .4f);
        } else {
            NT(c, p, clock(ctx, false), w * .07f, h * .47f, h * .48f, INK, Fonts.UI_MED, Paint.Align.LEFT, w * .52f);
            Weather.icon(c, p, code, w * .715f, h * .36f, h * .24f); NT(c, p, temp, w * .79f, h * .38f, h * .21f, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT, w * .16f);
            T(c, p, Weather.describe(code), w * .78f, h * .60f, h * .06f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, w * .28f); T(c, p, up(date("EEE d MMM")), w * .07f, h * .82f, h * .06f, 0xccffffff, Fonts.UI_SEMI, Paint.Align.LEFT, w * .4f);
        }
    }

    // ───────────────────────────── AURORA ─────────────────────────────
    private static final int[][] AURORA_SETS = {{0xff1fd1a0, 0xff3a7bff, 0xff8a5cff}, {0xff3a6bff, 0xff8a5cff, 0xff22c1ff}, {0xffff7a59, 0xffff4f8b, 0xff8a5cff}, {0xffff5fa2, 0xffffa14a, 0xff8a5cff}};
    private static void aurora(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); int set = Prefs.clamp(Prefs.i(ctx, "accent" + Frames.AURORA, 0), 0, 3); long t = System.currentTimeMillis(); double ts = t / 1000.0; c.drawColor(0xff000000);
        for (int i = 0; i < 3; i++) {
            float x = w * (.5f + .32f * (float) Math.sin(ts / (41f + i * 13f) + i * 2.1f)), y = h * (.5f + .30f * (float) Math.cos(ts / (57f + i * 11f) + i * 1.3f)), rad = h * (.85f + .18f * (float) Math.sin(ts / (23f + i * 7f)));
            p.setShader(new RadialGradient(x, y, rad, alpha(AURORA_SETS[set][i], .50f), alpha(AURORA_SETS[set][i], 0f), Shader.TileMode.CLAMP)); bleed(c, p, w, h);
        }
        p.setShader(null);
        if (comp == 0) NT(c, p, clock(ctx, false), w / 2f, h * .47f, h * .46f, 0xfff5f5f7, Fonts.UI_REG, Paint.Align.CENTER, w * .8f);
        else { NT(c, p, clock(ctx, false), w * .06f, h * .47f, h * .46f, 0xfff5f5f7, Fonts.UI_REG, Paint.Align.LEFT, w * .56f); T(c, p, up(date("EEEE")), w * .93f, h * .38f, h * .07f, 0xe6ffffff, Fonts.UI_SEMI, Paint.Align.RIGHT, w * .3f); T(c, p, up(date("d MMMM")), w * .93f, h * .50f, h * .05f, 0xb3ffffff, Fonts.UI_SEMI, Paint.Align.RIGHT, w * .3f); }
        ring(c, p, ctx, w, h, true);
    }

    // ───────────────────────────── BREATHE ─────────────────────────────
    private static void breathe(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); int accent = accent(ctx, Frames.BREATHE); long ms = System.currentTimeMillis();
        // comp 0: 4-7-8 (in, hold, out); comp 1: box 4-4-4-4 (in, hold, out, hold). Radius grows while breathing in and shrinks while breathing out.
        float[] lens = comp == 0 ? new float[]{4, 7, 8} : new float[]{4, 4, 4, 4}; String[] names = comp == 0 ? new String[]{"Breathe in", "Hold", "Breathe out"} : new String[]{"Breathe in", "Hold", "Breathe out", "Hold"};
        float total = 0; for (float l : lens) total += l; float pos = (ms % (long) (total * 1000)) / 1000f; int phase = 0; float acc = 0; while (phase < lens.length - 1 && pos >= acc + lens[phase]) { acc += lens[phase]; phase++; }
        float local = (pos - acc) / lens[phase], eased = local * local * (3 - 2 * local); boolean boxOut = comp == 1 && phase == 3; float grow = phase == 0 ? eased : phase == 1 ? 1f : phase == 2 ? 1f - eased : 0f;
        if (comp == 0 && phase == 1) grow = 1f; float rmin = h * .12f, rmax = h * .29f, r = rmin + (rmax - rmin) * grow, cx = w / 2f, cy = h * .53f;
        p.setStyle(Paint.Style.FILL); p.setShader(new RadialGradient(cx, cy, r * 1.9f, alpha(accent, .26f), alpha(accent, 0f), Shader.TileMode.CLAMP)); c.drawCircle(cx, cy, r * 1.9f, p); p.setShader(null);
        p.setColor(alpha(accent, .16f)); c.drawCircle(cx, cy, r, p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * .012f); p.setColor(accent); c.drawCircle(cx, cy, r, p);
        p.setStrokeWidth(h * .005f); p.setColor(alpha(accent, .35f)); c.drawCircle(cx, cy, rmax * 1.0f, p); c.drawCircle(cx, cy, rmin, p);
        int remaining = (int) Math.ceil(lens[phase] * (1 - local)); T(c, p, Integer.toString(Math.max(1, remaining)), cx, cy, h * .14f, INK, Fonts.ROUNDED_BOLD, Paint.Align.CENTER, r * 1.2f);
        T(c, p, names[phase], cx, h * .105f, h * .075f, INK, Fonts.UI_SEMI, Paint.Align.CENTER, w * .5f);
        T(c, p, comp == 0 ? "4 - 7 - 8" : "Box breathing   4 - 4 - 4 - 4", cx, h * .925f, h * .045f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, w * .6f); NT(c, p, clock(ctx, false), w * .07f, h * .115f, h * .06f, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .2f);
        ring(c, p, ctx, w, h, false);
    }

    // ───────────────────────────── TERMINAL ─────────────────────────────
    private static void terminal(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); int accent = accent(ctx, Frames.TERMINAL); Typeface mono = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD), monoReg = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL); float x = w * .07f; boolean cursor = (System.currentTimeMillis() / 600) % 2 == 0;
        int dim = alpha(accent, .62f); String pr = "standby@phone ~ % ";
        if (comp == 0) {
            T(c, p, pr + "date +%T", x, h * .20f, h * .055f, dim, monoReg, Paint.Align.LEFT, w * .8f);
            p.setShadowLayer(h * .035f, 0, 0, alpha(accent, .55f)); NT(c, p, date(Prefs.is24(ctx) ? "HH:mm:ss" : "hh:mm:ss"), x, h * .44f, h * .27f, accent, mono, Paint.Align.LEFT, w * .86f); p.setShadowLayer(0, 0, 0, 0);
            T(c, p, pr + "date +\"%A, %d %B\"", x, h * .64f, h * .055f, dim, monoReg, Paint.Align.LEFT, w * .8f); T(c, p, date("EEEE, dd MMMM"), x, h * .75f, h * .09f, accent, mono, Paint.Align.LEFT, w * .86f);
            T(c, p, pr + (cursor ? "█" : " "), x, h * .86f, h * .055f, dim, monoReg, Paint.Align.LEFT, w * .8f);
        } else {
            boolean[] ch = {false}; int lv = ClockFrames.battery(ctx, ch); String a = alarmText(ctx); long up = android.os.SystemClock.elapsedRealtime() / 60000; String upS = up >= 1440 ? (up / 1440) + "d " + (up / 60 % 24) + "h" : (up / 60) + "h " + (up % 60) + "m";
            String[][] rows = {{"time", date(Prefs.is24(ctx) ? "HH:mm:ss" : "hh:mm:ss a")}, {"date", date("EEE d MMM yyyy")}, {"battery", lv < 0 ? "n/a" : lv + "%" + (ch[0] ? "  charging" : "")}, {"alarm", a == null ? "none" : a},
                {"weather", Weather.ready() ? Weather.shown(ctx, Weather.temp) + "°  " + Weather.describe(Weather.code) : "n/a"}, {"uptime", upS}};
            T(c, p, "standby@phone", x, h * .14f, h * .075f, accent, mono, Paint.Align.LEFT, w * .8f); p.setColor(dim); c.drawRect(x, h * .205f, x + w * .30f, h * .21f, p);
            for (int i = 0; i < rows.length; i++) { float y = h * (.29f + .095f * i); T(c, p, rows[i][0], x, y, h * .06f, dim, monoReg, Paint.Align.LEFT, w * .2f); T(c, p, rows[i][1], x + w * .22f, y, h * .07f, accent, mono, Paint.Align.LEFT, w * .6f); }
            T(c, p, pr + (cursor ? "█" : " "), x, h * .90f, h * .045f, dim, monoReg, Paint.Align.LEFT, w * .7f);
        }
        ring(c, p, ctx, w, h, false);
    }

    // ───────────────────────────── SKY ─────────────────────────────
    private static final float[] SKY_HOUR = {0f, 5f, 6.5f, 9f, 13f, 17f, 19f, 20.5f, 24f};
    private static final int[] SKY_TOP = {0xff03040b, 0xff0b1128, 0xff2a3a70, 0xff1b5099, 0xff1c5aa8, 0xff233f70, 0xff2c1f4c, 0xff0b0f26, 0xff03040b};
    private static final int[] SKY_BOTTOM = {0xff0a1020, 0xff3a2c4c, 0xffff9a62, 0xff6fb1e0, 0xff7fc0ee, 0xffe89a4f, 0xffd8643a, 0xff2a2145, 0xff0a1020};
    private static int sky(float hour, int[] stops) { int i = 0; while (i < SKY_HOUR.length - 2 && hour >= SKY_HOUR[i + 1]) i++; float t = (hour - SKY_HOUR[i]) / (SKY_HOUR[i + 1] - SKY_HOUR[i]); return mix(stops[i], stops[i + 1], t); }
    private static void sky(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); Calendar n = Calendar.getInstance(); float hour = n.get(Calendar.HOUR_OF_DAY) + n.get(Calendar.MINUTE) / 60f;
        int top = mix(sky(hour, SKY_TOP), 0xff000000, .38f), bottom = mix(sky(hour, SKY_BOTTOM), 0xff000000, .38f);
        p.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP)); bleed(c, p, w, h); p.setShader(null);
        if (hour < 6.5f || hour > 19.5f) { Random rnd = new Random(3); p.setStyle(Paint.Style.FILL); float fade = hour < 5 || hour > 20.5f ? 1f : hour < 6.5f ? (6.5f - hour) / 1.5f : (hour - 19.5f); for (int i = 0; i < 70; i++) { float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h * .7f, s = h * (.0015f + rnd.nextFloat() * .0025f); p.setColor(alpha(0xffffffff, fade * (.2f + rnd.nextFloat() * .5f))); c.drawCircle(x, y, s, p); } }
        if (comp == 0) { NT(c, p, clock(ctx, false), w / 2f, h * .46f, h * .46f, 0xfff5f5f7, Fonts.UI_REG, Paint.Align.CENTER, w * .8f); T(c, p, up(date("EEEE d MMMM")), w / 2f, h * .76f, h * .055f, 0xccffffff, Fonts.UI_SEMI, Paint.Align.CENTER, w * .8f); }
        else { NT(c, p, clock(ctx, false), w * .06f, h * .47f, h * .46f, 0xfff5f5f7, Fonts.UI_REG, Paint.Align.LEFT, w * .6f); T(c, p, up(date("EEEE")), w * .93f, h * .40f, h * .065f, 0xe6ffffff, Fonts.UI_SEMI, Paint.Align.RIGHT, w * .3f); T(c, p, up(date("d MMMM")), w * .93f, h * .51f, h * .05f, 0xb3ffffff, Fonts.UI_SEMI, Paint.Align.RIGHT, w * .3f); }
        ring(c, p, ctx, w, h, true);
    }

    // ───────────────────────────── TIMER ─────────────────────────────
    /** Draws a time string with every character in a fixed-width cell so the digits don't jiggle as they change. */
    private static void cells(Canvas c, Paint p, String s, float cx, float cy, float size, int color) {
        float dw = size * .60f, cw = size * .30f, total = 0; for (char ch : s.toCharArray()) total += ch == ':' ? cw : dw;
        float x = cx - total / 2f; p.setTypeface(Fonts.ROUNDED_BOLD); p.setTextSize(size); p.setTextScaleX(1f); p.setTextAlign(Paint.Align.LEFT); p.setStyle(Paint.Style.FILL); p.setShader(null); p.setColor(color); RectF cap=Ink.bounds(p,"H0"); float base=cy-(cap.top+cap.bottom)/2; for (char ch : s.toCharArray()) { float cell = ch == ':' ? cw : dw; RectF ink=Ink.bounds(p,String.valueOf(ch)); c.drawText(String.valueOf(ch),x+(cell-ink.width())/2-ink.left,base,p); x += cell; }
    }

    /** Quick-start lengths in seconds. Tapping one starts a real timer in the Clock app, so it rings like any other and shows here as it counts down. */
    static final int[] PRESETS = {60, 180, 300, 600, 900, 1800, 2700, 3600};
    private static final RectF[] CHIPS = new RectF[PRESETS.length];
    static { for (int i = 0; i < CHIPS.length; i++) CHIPS[i] = new RectF(); }
    private static long startedAt; private static int startedIdx = -1;

    private static void presets(Canvas c, Paint p, float w, float h, Context ctx, int accent, long now) {
        tracked(c, p, "START A TIMER", w / 2f, h * .19f, h * .055f, accent, Fonts.UI_SEMI, Paint.Align.CENTER, .16f, w * .6f);
        float cw = w * .205f, ch = h * .27f, gap = w * .018f, x0 = w / 2f - (4 * cw + 3 * gap) / 2f;
        for (int i = 0; i < PRESETS.length; i++) {
            float x = x0 + (i % 4) * (cw + gap), y = h * (i < 4 ? .27f : .58f); CHIPS[i].set(x, y, x + cw, y + ch);
            boolean hot = i == startedIdx && now - startedAt < 1800;
            p.setStyle(Paint.Style.FILL); p.setColor(hot ? accent : alpha(accent, .15f)); c.drawRoundRect(CHIPS[i], ch * .3f, ch * .3f, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * .006f); p.setColor(alpha(accent, hot ? 1f : .45f)); c.drawRoundRect(CHIPS[i], ch * .3f, ch * .3f, p);
            int m = PRESETS[i] / 60; boolean hour = m >= 60; String n = Integer.toString(hour ? m / 60 : m), u = hour ? (m / 60 == 1 ? "hour" : "hours") : "min";
            T(c, p, n, x + cw / 2f, y + ch * .42f, ch * .44f, hot ? 0xff000000 : INK, Fonts.ROUNDED_BOLD, Paint.Align.CENTER, cw * .8f);
            T(c, p, u, x + cw / 2f, y + ch * .80f, ch * .17f, hot ? 0xff000000 : MUTED, Fonts.UI_SEMI, Paint.Align.CENTER, cw * .8f);
        }
        NT(c, p, clock(ctx, false), w * .07f, h * .115f, h * .06f, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .2f);
        T(c, p, MediaListener.instance == null ? "Turn on notification access in the Standby app to watch it count down" : "Starts in the Clock app, so it rings as usual", w / 2f, h * .93f, h * .045f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, w * .8f);
        ring(c, p, ctx, w, h, false);
    }

    /** A tap on the Timer frame while no timer runs. Returns true when it hit a preset (and started it). */
    static boolean timerTap(Context ctx, float x, float y) {
        if (runningTimer() != null) return false;
        for (int i = 0; i < CHIPS.length; i++) if (CHIPS[i].contains(x, y)) { startedIdx = i; startedAt = System.currentTimeMillis(); startTimer(ctx, PRESETS[i]); return true; }
        return false;
    }
    static void startTimer(Context ctx, int seconds) {
        try {
            ctx.startActivity(new android.content.Intent(android.provider.AlarmClock.ACTION_SET_TIMER).putExtra(android.provider.AlarmClock.EXTRA_LENGTH, seconds)
                .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException ignored) { }
    }

    private static void timer(Canvas c, float w, float h, Context ctx, int comp) {
        Paint p = paint(); int accent = accent(ctx, Frames.TIMER); long now = System.currentTimeMillis(); Timers.State t = runningTimer();
        boolean done = t != null && t.done(now), paused = t != null && t.paused; float frac = t == null ? 0f : done ? 0f : t.fraction(now);
        int col = done ? mix(RED, 0xffffffff, .5f * (float) Math.abs(Math.sin(now / 350.0))) : paused ? MUTED : accent;
        if (t == null) { presets(c, p, w, h, ctx, accent, now); return; }
        long seconds = Math.max(0, (t.left(now) + 999) / 1000); boolean hours = t.totalMs >= 3_600_000L;
        String time = hours ? String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60) : String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
        NT(c, p, clock(ctx, false), w * .07f, h * .115f, h * .06f, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT, w * .2f);
        String status = t == null ? (MediaListener.instance == null ? "ALLOW NOTIFICATION ACCESS" : "NO TIMER RUNNING") : done ? "TIME'S UP" : paused ? "PAUSED" : "TIMER";
        if (comp == 0) {
            float cx = w / 2f, cy = h * .52f, r = h * .335f, sw = h * .05f;
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sw); p.setColor(alpha(col, .17f)); c.drawCircle(cx, cy, r, p);
            if (t != null && !done) {
                RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r); p.setColor(col); c.drawArc(oval, -90f, 360f * frac, false, p);
                double a = Math.toRadians(-90 + 360 * frac); float hx = cx + (float) Math.cos(a) * r, hy = cy + (float) Math.sin(a) * r;
                p.setStyle(Paint.Style.FILL); p.setShader(new RadialGradient(hx, hy, sw * 1.7f, alpha(col, .55f), alpha(col, 0f), Shader.TileMode.CLAMP)); c.drawCircle(hx, hy, sw * 1.7f, p); p.setShader(null);
                p.setColor(0xffffffff); c.drawCircle(hx, hy, sw * .22f, p);
            } else if (done) { p.setColor(col); c.drawCircle(cx, cy, r, p); }
            cells(c, p, time, cx, cy - h * .01f, hours ? h * .15f : h * .19f, t == null ? MUTED : done ? RED : paused ? MUTED : INK);
            tracked(c, p, status, cx, cy + h * .135f, h * .04f, col, Fonts.UI_SEMI, Paint.Align.CENTER, .12f, r * 1.5f);
            if (t != null && !done) {
                String end = new SimpleDateFormat(Prefs.is24(ctx) ? "HH:mm" : "h:mm a", Locale.getDefault()).format(new Date(paused ? now + t.left(now) : t.endAt));
                T(c, p, (paused ? "Would end " : "Ends ") + end, cx, h * .85f, h * .05f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, w * .5f);
            } else if (t == null) T(c, p, MediaListener.instance == null ? "Turn it on in Standby settings" : "Start one in the Clock app", cx, h * .85f, h * .05f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, w * .6f);
            else T(c, p, "Open the Clock app to stop it", cx, h * .85f, h * .05f, MUTED, Fonts.UI_MED, Paint.Align.CENTER, w * .6f);
        } else {
            tracked(c, p, status, w / 2f, h * .24f, h * .06f, col, Fonts.UI_SEMI, Paint.Align.CENTER, .16f, w * .6f);
            cells(c, p, time, w / 2f, h * .5f, hours ? h * .34f : h * .42f, t == null ? MUTED : done ? RED : paused ? MUTED : INK);
            float bx0 = w * .12f, bx1 = w * .88f, by = h * .8f, bh = h * .03f; p.setStyle(Paint.Style.FILL); p.setColor(alpha(col, .18f)); c.drawRoundRect(bx0, by - bh / 2, bx1, by + bh / 2, bh / 2, bh / 2, p);
            if (t != null && !done) { p.setColor(col); c.drawRoundRect(bx0, by - bh / 2, bx0 + Math.max(bh, (bx1 - bx0) * frac), by + bh / 2, bh / 2, bh / 2, p); }
            if (t != null) { T(c, p, Math.max(0, Math.round(100 * frac)) + "% left", bx0, h * .86f, h * .05f, MUTED, Fonts.UI_MED, Paint.Align.LEFT, w * .3f);
                T(c, p, (t.totalMs >= 3_600_000L ? (t.totalMs / 3_600_000L) + " h " + (t.totalMs % 3_600_000L / 60_000L) + " min" : Math.max(1, t.totalMs / 60_000L) + " min") + " timer", bx1, h * .86f, h * .05f, MUTED, Fonts.UI_MED, Paint.Align.RIGHT, w * .4f); }
        }
        ring(c, p, ctx, w, h, false);
    }
}
