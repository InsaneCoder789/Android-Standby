package com.rohan.standby;

import android.app.AlarmManager;
import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The redesigned look of every frame: dark screen, one accent, one big element, letter-spaced labels, details in glass pills, 24-hour time, and the
 * charging ring in the top-right corner. Each frame gets it as one extra look (the last swipe up or down), so the existing looks are untouched.
 * Drawn in a 480 x 222 design space that is scaled to the screen, so every number below can be read straight off the concept sheet.
 */
final class ConceptFrames {
    private static final int INK = 0xffffffff, MUTED = 0xff8e8e93, GLASS = 0x17ffffff;
    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DITHER_FLAG);

    static void draw(Canvas c, float w, float h, Context ctx, int frame) {
        int save = c.save(); c.scale(w / 480f, h / 222f); Paint p = P; p.reset(); p.setAntiAlias(true); p.setSubpixelText(true); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        int accent = 0xff30d158;
        try {
            Calendar now = Calendar.getInstance(); long ms = System.currentTimeMillis();
            switch (frame) {
                case Frames.DIGITAL: accent = digital(c, p, ctx, now); break;
                case Frames.DIAL: accent = dial(c, p, ctx, now, ms); break;
                case Frames.MUSIC: accent = music(c, p, ctx, false, now); break;
                case Frames.CLOCK_MUSIC: accent = music(c, p, ctx, true, now); break;
                case Frames.WORLD: accent = world(c, p, ctx, ms); break;
                case Frames.FLIP: accent = flip(c, p, now); break;
                case Frames.WORD: accent = word(c, p, now); break;
                case Frames.DOTS: accent = dots(c, p, now); break;
                case Frames.NIXIE: accent = nixie(c, p, now); break;
                case Frames.LED: accent = led(c, p, ctx, now); break;
                case Frames.RAILWAY: accent = railway(c, p, now); break;
                case Frames.ORBIT: accent = orbit(c, p, now); break;
                case Frames.SUN: accent = sunPath(c, p, ctx, now, ms); break;
                case Frames.MOON: accent = moon(c, p, ms); break;
                case Frames.PROGRESS: accent = left(c, p, now); break;
                case Frames.BIGDATE: accent = bigDate(c, p, now); break;
                case Frames.WEATHER: accent = weather(c, p, ctx); break;
                case Frames.AURORA: accent = aurora(c, p, now, ms); break;
                case Frames.BREATHE: accent = breathe(c, p, now, ms); break;
                case Frames.TERMINAL: accent = terminal(c, p, ctx, now); break;
                case Frames.SKY: accent = sky(c, p, now, ms); break;
                case Frames.TIMER: accent = timer(c, p, ctx, ms); break;
                default: break;
            }
            charge(c, p, ctx, accent);
        } catch (RuntimeException e) { android.util.Log.e("CONCEPT", "frame " + frame, e); }
        c.restoreToCount(save);
    }

    /** The photo frame's redesign keeps your photo full-bleed; this adds the glass pill with the time. */
    static void photoPill(Canvas c, float w, float h, Context ctx) {
        int save = c.save(); c.scale(w / 480f, h / 222f); Paint p = P; p.reset(); p.setAntiAlias(true); Calendar now = Calendar.getInstance();
        rr(c, p, 26, 150, 190, 198, 24, 0x80000000); tx(c, p, hm(now), 44, 184, 28, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT); tx(c, p, new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date()), 128, 180, 13, 0xccffffff, Fonts.UI_MED, Paint.Align.LEFT);
        c.restoreToCount(save);
    }

    // ───────────────────────── helpers ─────────────────────────
    private static String two(int v) { return v < 10 ? "0" + v : Integer.toString(v); }
    private static String hm(Calendar c) { return two(c.get(Calendar.HOUR_OF_DAY)) + ":" + two(c.get(Calendar.MINUTE)); }
    private static String dayShort(Calendar c) { return new SimpleDateFormat("EEE d", Locale.getDefault()).format(c.getTime()); }
    private static void tx(Canvas c, Paint p, String s, float x, float y, float size, int color, Typeface f, Paint.Align a) { p.setStyle(Paint.Style.FILL); p.setShader(null); p.setTypeface(f); p.setTextSize(size); p.setColor(color); p.setTextAlign(a); p.setLetterSpacing(0f); c.drawText(s, x, y, p); }
    private static void lab(Canvas c, Paint p, String s, float x, float y, int color, Paint.Align a) { p.setLetterSpacing(.24f); p.setStyle(Paint.Style.FILL); p.setTypeface(Fonts.UI_SEMI); p.setTextSize(10); p.setColor(color); p.setTextAlign(a); c.drawText(s.toUpperCase(Locale.ROOT), x, y, p); p.setLetterSpacing(0f); }
    private static void rr(Canvas c, Paint p, float l, float t, float r, float b, float rad, int color) { p.setStyle(Paint.Style.FILL); p.setShader(null); p.setColor(color); c.drawRoundRect(l, t, r, b, rad, rad, p); }
    private static void dot(Canvas c, Paint p, float x, float y, float r, int color) { p.setStyle(Paint.Style.FILL); p.setShader(null); p.setColor(color); c.drawCircle(x, y, r, p); }
    private static void ring(Canvas c, Paint p, float x, float y, float r, float sw, int color) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sw); p.setColor(color); p.setShader(null); c.drawCircle(x, y, r, p); p.setStyle(Paint.Style.FILL); }
    private static void line(Canvas c, Paint p, float x1, float y1, float x2, float y2, float sw, int color) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(sw); p.setColor(color); c.drawLine(x1, y1, x2, y2, p); p.setStyle(Paint.Style.FILL); }
    private static void pill(Canvas c, Paint p, float x, float y, float w, float h, String s, float size, int color) { rr(c, p, x, y, x + w, y + h, h / 2, GLASS); tx(c, p, s, x + w / 2, y + h / 2 + size * .36f, size, color, Fonts.UI_SEMI, Paint.Align.CENTER); }
    /** A number with every character in a fixed cell, so the digits never jiggle as they change. Centred on cx. */
    private static void num(Canvas c, Paint p, String s, float cx, float base, float size, int color, Typeface f) {
        p.setTypeface(f); p.setTextSize(size); p.setStyle(Paint.Style.FILL); p.setShader(null); p.setColor(color); p.setTextAlign(Paint.Align.LEFT); p.setLetterSpacing(0f);
        float dw = 0; for (char d = '0'; d <= '9'; d++) dw = Math.max(dw, p.measureText(String.valueOf(d))); float cw = dw * .5f, total = 0; for (char ch : s.toCharArray()) total += ch == ':' ? cw : dw;
        float x = cx - total / 2f; Rect b = new Rect(); for (char ch : s.toCharArray()) { String v = String.valueOf(ch); float cell = ch == ':' ? cw : dw; p.getTextBounds(v, 0, 1, b); c.drawText(v, x + cell / 2f - (b.left + b.right) / 2f, base, p); x += cell; }
    }
    private static Paint opaque(Paint p) { p.setStyle(Paint.Style.FILL); p.setAlpha(255); return p; }
    private static float hash(int i) { float v = (float) Math.sin(i * 12.9898) * 43758.545f; return v - (float) Math.floor(v); }
    private static void stars(Canvas c, Paint p, int n, int seed, float x0, float x1, float y0, float y1, float op) {
        for (int i = 0; i < n; i++) dot(c, p, x0 + (x1 - x0) * hash(i + seed), y0 + (y1 - y0) * hash(i + seed + 30), .7f + 1.1f * hash(i + seed + 60), ((int) (255 * op * (.4f + .6f * hash(i + seed + 90))) << 24) | 0xffffff);
    }
    private static int mixc(int a, int b, float t) { t = Math.max(0, Math.min(1, t)); int r = (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t), g = (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t), bl = (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t); return Color.rgb(r, g, bl); }
    private static String nextAlarm(Context ctx) { try { AlarmManager m = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE); AlarmManager.AlarmClockInfo i = m == null ? null : m.getNextAlarmClock(); if (i == null) return null; Calendar c = Calendar.getInstance(); c.setTimeInMillis(i.getTriggerTime()); return hm(c); } catch (RuntimeException e) { return null; } }
    private static String temp(Context ctx) { return Weather.ready() ? Weather.shown(ctx, Weather.temp) + "°" : "--°"; }

    /** The app's own charging ring (same drawing, same "Charge ring" setting) in the same top-right spot as the other frames: the percentage inside, a bolt in a notch when charging. */
    private static void charge(Canvas c, Paint p, Context ctx, int accent) {
        p.reset(); p.setAntiAlias(true); p.setStrokeCap(Paint.Cap.ROUND); ClockFrames.chargeRing(c, p, ctx, 480 * .92f, 222 * .098f, 222 * .042f, 222, false);
    }

    // ───────────────────────── frames ─────────────────────────
    private static int digital(Canvas c, Paint p, Context ctx, Calendar now) {
        int a = 0xff6cf55c; num(c, p, hm(now), 190, 148, 112, INK, Fonts.ROUNDED_BOLD); rr(c, p, 40, 168, 340, 173, 2.5f, 0x18ffffff); rr(c, p, 40, 168, 40 + 300f * now.get(Calendar.SECOND) / 60f, 173, 2.5f, a);
        lab(c, p, new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(now.getTime()), 40, 58, a, Paint.Align.LEFT); pill(c, p, 372, 84, 84, 34, dayShort(now).toUpperCase(Locale.ROOT), 15, INK); pill(c, p, 372, 128, 84, 34, temp(ctx), 15, INK); return a;
    }
    private static void hands(Canvas c, Paint p, float cx, float cy, float hl, float ml, Calendar n, int sec, boolean secLollipop) {
        float ha = (float) Math.toRadians(((n.get(Calendar.HOUR_OF_DAY) % 12) + n.get(Calendar.MINUTE) / 60f) * 30f), ma = (float) Math.toRadians((n.get(Calendar.MINUTE) + n.get(Calendar.SECOND) / 60f) * 6f), sa = (float) Math.toRadians(n.get(Calendar.SECOND) * 6f);
        line(c, p, cx, cy, cx + (float) Math.sin(ha) * hl, cy - (float) Math.cos(ha) * hl, 6, 0xfff2f2f7); line(c, p, cx, cy, cx + (float) Math.sin(ma) * ml, cy - (float) Math.cos(ma) * ml, 4, 0xfff2f2f7);
        float sx = cx + (float) Math.sin(sa) * ml * .96f, sy = cy - (float) Math.cos(sa) * ml * .96f; line(c, p, cx - (float) Math.sin(sa) * 14, cy + (float) Math.cos(sa) * 14, sx, sy, 2, sec); if (secLollipop) dot(c, p, sx, sy, 7, sec);
    }
    private static int dial(Canvas c, Paint p, Context ctx, Calendar now, long ms) {
        int a = 0xffff9f0a; float cx = 130, cy = 113, r = 86;
        ring(c, p, cx, cy, r + 8, 5, 0xff1d2a4a); float rise = 6f, set = 18f; if (Weather.hasPlace()) { long[] s = Astro.sun(Weather.lat, Weather.lon, ms); if (s != null && s.length >= 2 && s[0] > 0 && s[1] > 0) { Calendar r1 = Calendar.getInstance(), s1 = Calendar.getInstance(); r1.setTimeInMillis(s[0]); s1.setTimeInMillis(s[1]); rise = r1.get(Calendar.HOUR_OF_DAY) + r1.get(Calendar.MINUTE) / 60f; set = s1.get(Calendar.HOUR_OF_DAY) + s1.get(Calendar.MINUTE) / 60f; } }
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5); p.setColor(0x80ffd36b); c.drawArc(new RectF(cx - r - 8, cy - r - 8, cx + r + 8, cy + r + 8), rise * 15f - 90f, Math.max(1f, (((set - rise) + 24f) % 24f) * 15f), false, p); p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 60; i++) { double an = Math.toRadians(i * 6); float l = i % 5 == 0 ? 9 : 4; line(c, p, cx + (float) Math.sin(an) * (r - l), cy - (float) Math.cos(an) * (r - l), cx + (float) Math.sin(an) * r, cy - (float) Math.cos(an) * r, i % 5 == 0 ? 2 : 1, i % 5 == 0 ? INK : 0x44ffffff); }
        hands(c, p, cx, cy, 44, 66, now, a, false); dot(c, p, cx, cy, 5, a);
        lab(c, p, Weather.ready() ? Weather.describe(Weather.code) : "Today", 270, 62, a, Paint.Align.LEFT); tx(c, p, dayShort(now), 270, 108, 44, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT);
        pill(c, p, 270, 128, 70, 32, temp(ctx), 13, INK); String al = nextAlarm(ctx); pill(c, p, 348, 128, 96, 32, al == null ? "No alarm" : "Alarm " + al, 12, INK); return a;
    }
    private static String clockOf(long ms) { long s = Math.max(0, ms / 1000); return (s / 60) + ":" + two((int) (s % 60)); }
    private static int music(Canvas c, Paint p, Context ctx, boolean withClock, Calendar now) {
        int a = withClock ? 0xff64d2ff : 0xffff5e8a; MediaController mc = null; try { if (MediaListener.instance != null) mc = MediaListener.instance.controller(); } catch (RuntimeException ignored) { }
        String title = mc == null ? (withClock ? "Not playing" : "Nothing playing") : MediaListener.title(mc), artist = mc == null ? (withClock ? "Start a song" : "Play something to see it here") : MediaListener.artist(mc); if (title == null) title = "Unknown"; if (artist == null) artist = "";
        Bitmap art = mc == null ? null : MediaListener.art(mc); float frac = 0; long pos = 0, dur = 0; boolean playing = false;
        if (mc != null) { MediaMetadata m = mc.getMetadata(); dur = m == null ? 0 : m.getLong(MediaMetadata.METADATA_KEY_DURATION); PlaybackState s = mc.getPlaybackState(); if (s != null) { playing = s.getState() == PlaybackState.STATE_PLAYING; pos = s.getPosition(); if (playing) pos += (long) ((android.os.SystemClock.elapsedRealtime() - s.getLastPositionUpdateTime()) * s.getPlaybackSpeed()); } if (dur > 0) frac = Math.max(0, Math.min(1, pos / (float) dur)); }
        float l = withClock ? 276 : 36, t = withClock ? 76 : 36, sz = withClock ? 50 : 150;
        if (withClock) { num(c, p, hm(now), 130, 138, 76, INK, Fonts.ROUNDED_BOLD); lab(c, p, new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(now.getTime()), 46, 52, a, Paint.Align.LEFT); rr(c, p, 262, 62, 442, 174, 20, 0x14ffffff); }
        Path clip = new Path(); clip.addRoundRect(new RectF(l, t, l + sz, t + sz), sz * .15f, sz * .15f, Path.Direction.CW); int s0 = c.save(); c.clipPath(clip);
        if (art != null) c.drawBitmap(art, null, new RectF(l, t, l + sz, t + sz), p); else { opaque(p).setShader(new LinearGradient(l, t, l + sz, t + sz, 0xffff5e8a, 0xff5e5ce6, Shader.TileMode.CLAMP)); c.drawRect(l, t, l + sz, t + sz, p); p.setShader(null); } c.restoreToCount(s0);
        if (withClock) { tx(c, p, ell(p, title, 100, 15), 336, 96, 15, INK, Fonts.UI_SEMI, Paint.Align.LEFT); tx(c, p, ell(p, artist, 100, 12), 336, 114, 12, MUTED, Fonts.UI_MED, Paint.Align.LEFT); rr(c, p, 276, 140, 428, 145, 2.5f, 0x22ffffff); rr(c, p, 276, 140, 276 + 152 * frac, 145, 2.5f, a); tx(c, p, clockOf(pos), 276, 164, 10, MUTED, Fonts.UI_MED, Paint.Align.LEFT); tx(c, p, dur > 0 ? clockOf(dur) : "", 428, 164, 10, MUTED, Fonts.UI_MED, Paint.Align.RIGHT); return a; }
        lab(c, p, playing ? "Now playing" : mc == null ? "Standby" : "Paused", 212, 64, a, Paint.Align.LEFT); tx(c, p, ell(p, title, 224, 26), 212, 92, 26, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT); tx(c, p, ell(p, artist, 224, 14), 212, 114, 14, MUTED, Fonts.UI_MED, Paint.Align.LEFT);
        for (int i = 0; i < 30; i++) { float amp = 4 + 14 * Math.abs((float) (Math.sin(i * 1.7) * Math.cos(i * .5 + (playing ? System.currentTimeMillis() / 600.0 : 0)))); rr(c, p, 212 + i * 8, 144 - amp, 216 + i * 8, 144 + amp, 2, i < 30 * frac ? a : 0x30ffffff); }
        tx(c, p, clockOf(pos) + (dur > 0 ? " / " + clockOf(dur) : ""), 212, 190, 13, MUTED, Fonts.UI_MED, Paint.Align.LEFT); return a;
    }
    private static String ell(Paint p, String s, float maxW, float size) { p.setTextSize(size); p.setTypeface(Fonts.UI_SEMI); return p.measureText(s) <= maxW ? s : android.text.TextUtils.ellipsize(s, new android.text.TextPaint(p), maxW, android.text.TextUtils.TruncateAt.END).toString(); }
    private static int world(Canvas c, Paint p, Context ctx, long ms) {
        int a = 0xff5ac8fa; String[] zones = Prefs.worldZones(ctx); lab(c, p, "Local " + hm(Calendar.getInstance()), 36, 38, MUTED, Paint.Align.LEFT);
        for (int i = 0; i < Math.min(4, zones.length); i++) {
            float x = 34 + (i % 2) * 212, y = 60 + (i / 2) * 78; TimeZone tz = TimeZone.getTimeZone(zones[i]); Calendar cal = Calendar.getInstance(tz); cal.setTimeInMillis(ms); float hr = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f;
            rr(c, p, x, y, x + 200, y + 66, 16, 0x12ffffff); lab(c, p, WorldCities.city(zones[i]), x + 16, y + 22, a, Paint.Align.LEFT); tx(c, p, hm(cal), x + 16, y + 52, 28, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT);
            rr(c, p, x + 120, y + 40, x + 184, y + 46, 3, 0xff1d2a4a); boolean day = hr >= 6 && hr < 18; dot(c, p, x + 120 + 64 * hr / 24f, y + 43, 6, day ? 0xffffd36b : 0xff9fb4ff);
        }
        if (zones.length == 0) tx(c, p, "Add cities in Standby settings", 240, 120, 16, MUTED, Fonts.UI_MED, Paint.Align.CENTER); return a;
    }
    private static int flip(Canvas c, Paint p, Calendar now) {
        int a = 0xffff9f0a; String t = hm(now).replace(":", ""); for (int i = 0; i < 4; i++) {
            float x = 42 + i * 98 + (i > 1 ? 14 : 0); rr(c, p, x, 42, x + 86, 180, 14, 0xff1c1c20); rr(c, p, x, 42, x + 86, 111, 14, 0xff25252a); tx(c, p, t.substring(i, i + 1), x + 43, 140, 110, 0xfff2f2f7, Fonts.ROUNDED_BOLD, Paint.Align.CENTER);
            rr(c, p, x, 109.5f, x + 86, 112.5f, 0, 0xff000000); dot(c, p, x + 6, 111, 3, 0xff000000); dot(c, p, x + 80, 111, 3, 0xff000000);
        }
        dot(c, p, 232, 95, 4, a); dot(c, p, 232, 125, 4, a); return a;
    }
    private static int word(Canvas c, Paint p, Calendar now) {
        int a = 0xffffd60a; boolean[][] on = new boolean[WordClock.ROWS][WordClock.COLS]; List<int[]> lit = WordClock.lit(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)); for (int[] s : lit) for (int k = 0; k < s[2]; k++) on[s[0]][s[1] + k] = true;
        float dx = 38, dy = 18.2f, x0 = 240 - dx * (WordClock.COLS - 1) / 2f, y0 = 40; for (int r = 0; r < WordClock.ROWS; r++) for (int k = 0; k < WordClock.COLS; k++) tx(c, p, WordClock.GRID[r].substring(k, k + 1), x0 + k * dx, y0 + r * dy + 6, 17, on[r][k] ? a : 0xff2c2c30, on[r][k] ? Fonts.ROUNDED_BOLD : Fonts.UI_SEMI, Paint.Align.CENTER);
        return a;
    }
    private static final String[][] DOT = {{"01110", "10001", "10011", "10101", "11001", "10001", "01110"}, {"00100", "01100", "00100", "00100", "00100", "00100", "01110"}, {"01110", "10001", "00001", "00010", "00100", "01000", "11111"}, {"11110", "00001", "00001", "01110", "00001", "00001", "11110"}, {"00010", "00110", "01010", "10010", "11111", "00010", "00010"},
        {"11111", "10000", "11110", "00001", "00001", "10001", "01110"}, {"00110", "01000", "10000", "11110", "10001", "10001", "01110"}, {"11111", "00001", "00010", "00100", "01000", "01000", "01000"}, {"01110", "10001", "10001", "01110", "10001", "10001", "01110"}, {"01110", "10001", "10001", "01111", "00001", "00010", "01100"}};
    private static int dots(Canvas c, Paint p, Calendar now) {
        int a = 0xffffa30a; String t = hm(now).replace(":", ""); float pitch = 15f; for (int k = 0; k < 4; k++) { float ox = 52 + k * 96 + (k > 1 ? 24 : 0); String[] g = DOT[t.charAt(k) - '0']; for (int r = 0; r < 7; r++) for (int col = 0; col < 5; col++) { boolean on = g[r].charAt(col) == '1'; dot(c, p, ox + col * pitch, 62 + r * pitch, on ? 6f : 1.8f, on ? a : 0x2affffff); } }
        dot(c, p, 238, 92, 5, a); dot(c, p, 238, 122, 5, a); return a;
    }
    private static int nixie(Canvas c, Paint p, Calendar now) {
        int a = 0xffff8a3c; String t = hm(now).replace(":", ""); for (int i = 0; i < 4; i++) { float x = 40 + i * 100 + (i > 1 ? 20 : 0); rr(c, p, x, 34, x + 74, 184, 37, 0xff2a1408); rr(c, p, x + 3, 37, x + 71, 181, 34, 0xff120803); p.setShadowLayer(8, 0, 0, 0xffff6a1a); tx(c, p, t.substring(i, i + 1), x + 37, 138, 92, a, Fonts.ROUNDED_BOLD, Paint.Align.CENTER); p.clearShadowLayer(); rr(c, p, x + 20, 184, x + 54, 194, 0, 0xff5a3418); }
        dot(c, p, 238, 96, 5, a); dot(c, p, 238, 124, 5, a); return a;
    }
    private static final String[] SEG = {"abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg"};
    private static void seven(Canvas c, Paint p, int d, float x, float y, float w, float h, float t, int on, int off) {
        float[][] m = {{x + t, y, w - 2 * t, t}, {x + w - t, y + t, t, h / 2 - t * 1.5f}, {x + w - t, y + h / 2 + t / 2, t, h / 2 - t * 1.5f}, {x + t, y + h - t, w - 2 * t, t}, {x, y + h / 2 + t / 2, t, h / 2 - t * 1.5f}, {x, y + t, t, h / 2 - t * 1.5f}, {x + t, y + h / 2 - t / 2, w - 2 * t, t}};
        for (int i = 0; i < 7; i++) rr(c, p, m[i][0], m[i][1], m[i][0] + m[i][2], m[i][1] + m[i][3], t / 2.2f, SEG[d].indexOf((char) ('a' + i)) >= 0 ? on : off);
    }
    private static int led(Canvas c, Paint p, Context ctx, Calendar now) {
        int a = 0xffff3b30; String t = hm(now).replace(":", ""); float[] xs = {34, 126, 240, 332}; for (int i = 0; i < 4; i++) seven(c, p, t.charAt(i) - '0', xs[i], 40, 76, 128, 14, a, 0xff1d0707);
        dot(c, p, 224, 76, 7, a); dot(c, p, 224, 132, 7, a); String al = nextAlarm(ctx); pill(c, p, 34, 184, 150, 28, al == null ? "NO ALARM SET" : "ALARM " + al, 12, 0xffff6b60); return a;
    }
    private static int railway(Canvas c, Paint p, Calendar now) {
        int red = 0xffe5252a; float cx = 150, cy = 113, r = 92; dot(c, p, cx, cy, r, 0xfff4f1ea);
        for (int i = 0; i < 60; i++) { double an = Math.toRadians(i * 6); float l = i % 5 == 0 ? 14 : 5; line(c, p, cx + (float) Math.sin(an) * (r - l), cy - (float) Math.cos(an) * (r - l), cx + (float) Math.sin(an) * (r - 2), cy - (float) Math.cos(an) * (r - 2), i % 5 == 0 ? 4 : 1.5f, 0xff111111); }
        float ha = (float) Math.toRadians(((now.get(Calendar.HOUR_OF_DAY) % 12) + now.get(Calendar.MINUTE) / 60f) * 30f), ma = (float) Math.toRadians((now.get(Calendar.MINUTE) + now.get(Calendar.SECOND) / 60f) * 6f), sa = (float) Math.toRadians(now.get(Calendar.SECOND) * 6f);
        line(c, p, cx, cy, cx + (float) Math.sin(ha) * 52, cy - (float) Math.cos(ha) * 52, 8, 0xff111111); line(c, p, cx, cy, cx + (float) Math.sin(ma) * 76, cy - (float) Math.cos(ma) * 76, 5, 0xff111111);
        float sx = cx + (float) Math.sin(sa) * 72, sy = cy - (float) Math.cos(sa) * 72; line(c, p, cx - (float) Math.sin(sa) * 18, cy + (float) Math.cos(sa) * 18, sx, sy, 2.5f, red); dot(c, p, sx, sy, 7, red); dot(c, p, cx, cy, 4, red);
        lab(c, p, "Platform", 290, 86, red, Paint.Align.LEFT); tx(c, p, hm(now), 290, 130, 46, 0xfff4f1ea, Fonts.ROUNDED_BOLD, Paint.Align.LEFT); tx(c, p, new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(now.getTime()), 290, 156, 14, MUTED, Fonts.UI_MED, Paint.Align.LEFT); return red;
    }
    private static int orbit(Canvas c, Paint p, Calendar now) {
        int a = 0xff5e5ce6; float cx = 130, cy = 113; ring(c, p, cx, cy, 94, 1, 0x1affffff); ring(c, p, cx, cy, 64, 1, 0x1affffff); ring(c, p, cx, cy, 34, 1, 0x1affffff);
        double ha = Math.toRadians(((now.get(Calendar.HOUR_OF_DAY) % 12) + now.get(Calendar.MINUTE) / 60f) * 30f), ma = Math.toRadians(now.get(Calendar.MINUTE) * 6f), sa = Math.toRadians(now.get(Calendar.SECOND) * 6f);
        dot(c, p, cx + (float) Math.sin(ha) * 94, cy - (float) Math.cos(ha) * 94, 9, a); dot(c, p, cx + (float) Math.sin(ma) * 64, cy - (float) Math.cos(ma) * 64, 7, 0xff64d2ff); dot(c, p, cx + (float) Math.sin(sa) * 34, cy - (float) Math.cos(sa) * 34, 5, INK); dot(c, p, cx, cy, 10, 0xffff9f0a);
        lab(c, p, "Orbit", 270, 70, a, Paint.Align.LEFT); num(c, p, hm(now), 350, 124, 58, INK, Fonts.ROUNDED_BOLD); tx(c, p, ":" + two(now.get(Calendar.SECOND)) + " seconds", 270, 152, 14, MUTED, Fonts.UI_MED, Paint.Align.LEFT); return a;
    }
    private static int sunPath(Canvas c, Paint p, Context ctx, Calendar now, long ms) {
        int a = 0xffff9f0a; float rise = 6f, set = 18f; String rs = "06:00", ss = "18:00";
        if (Weather.hasPlace()) { long[] s = Astro.sun(Weather.lat, Weather.lon, ms); if (s != null && s.length >= 2 && s[0] > 0 && s[1] > 0) { Calendar r1 = Calendar.getInstance(), s1 = Calendar.getInstance(); r1.setTimeInMillis(s[0]); s1.setTimeInMillis(s[1]); rise = r1.get(Calendar.HOUR_OF_DAY) + r1.get(Calendar.MINUTE) / 60f; set = s1.get(Calendar.HOUR_OF_DAY) + s1.get(Calendar.MINUTE) / 60f; rs = hm(r1); ss = hm(s1); } }
        if (set < rise) set += 24f; float hr = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60f; if (hr < rise && set >= 24f) hr += 24f; float f = Math.max(0, Math.min(1, (hr - rise) / Math.max(.1f, set - rise))); boolean up = hr >= rise && hr <= set;
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(0x22ffffff); c.drawArc(new RectF(50, 20, 430, 320), 180, 180, false, p);
        if (up) { p.setStrokeWidth(4); p.setColor(a); c.drawArc(new RectF(50, 20, 430, 320), 180, 180 * f, false, p); } p.setStyle(Paint.Style.FILL);
        line(c, p, 30, 170, 450, 170, 1, 0x30ffffff);
        if (up) { double an = Math.PI + Math.PI * f; float sx = 240 + 190 * (float) Math.cos(an), sy = 170 + 150 * (float) Math.sin(an); p.setShadowLayer(10, 0, 0, a); dot(c, p, sx, sy, 14, 0xffffd36b); p.clearShadowLayer(); }
        lab(c, p, "Sunrise", 50, 194, MUTED, Paint.Align.LEFT); tx(c, p, rs, 50, 212, 16, INK, Fonts.UI_SEMI, Paint.Align.LEFT); lab(c, p, "Sunset", 430, 194, MUTED, Paint.Align.RIGHT); tx(c, p, ss, 430, 212, 16, INK, Fonts.UI_SEMI, Paint.Align.RIGHT);
        num(c, p, hm(now), 240, 132, 44, INK, Fonts.ROUNDED_BOLD); float left = set - hr; tx(c, p, !up ? "The sun is down" : (int) left + " h " + two((int) ((left % 1) * 60)) + " min of daylight left", 240, 154, 13, MUTED, Fonts.UI_MED, Paint.Align.CENTER); return a;
    }
    private static int moon(Canvas c, Paint p, long ms) {
        int a = 0xffb4c4ff; double ph = Astro.moonPhase(ms), il = Astro.illumination(ph); float cx = 130, cy = 111, r = 76; boolean waxing = ph < .5;
        dot(c, p, cx, cy, r, 0xff1a1d2b); ring(c, p, cx, cy, r, 1.5f, 0xff3a4060); Path half = new Path(); RectF o = new RectF(cx - r, cy - r, cx + r, cy + r); half.addArc(o, waxing ? -90 : 90, 180); half.close(); p.setColor(0xffe8ecf8); c.drawPath(half, p);
        float rx = r * (float) Math.abs(1 - 2 * il); Path e = new Path(); e.addOval(new RectF(cx - rx, cy - r, cx + rx, cy + r), Path.Direction.CW); p.setColor(il > .5 ? 0xffe8ecf8 : 0xff1a1d2b); c.drawPath(e, p);
        lab(c, p, "Tonight", 250, 76, a, Paint.Align.LEFT); tx(c, p, Math.round(il * 100) + "%", 250, 124, 54, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT); tx(c, p, Astro.phaseName(ph), 250, 152, 15, MUTED, Fonts.UI_MED, Paint.Align.LEFT);
        int full = (int) Math.round(Astro.daysToFull(ph)); pill(c, p, 250, 164, 150, 30, full <= 0 ? "Full moon tonight" : "Full moon in " + full + " days", 12, INK); stars(c, p, 16, 7, 200, 460, 20, 200, .5f); return a;
    }
    private static int left(Canvas c, Paint p, Calendar now) {
        int[] col = {0xffff9f0a, 0xff30d158, 0xff64d2ff, 0xffbf5af2}; String[] nm = {"DAY", "WEEK", "MONTH", "YEAR"};
        float day = (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)) / 1440f, wd = ((now.get(Calendar.DAY_OF_WEEK) + 5) % 7 + day) / 7f, mon = (now.get(Calendar.DAY_OF_MONTH) - 1 + day) / now.getActualMaximum(Calendar.DAY_OF_MONTH), yr = (now.get(Calendar.DAY_OF_YEAR) - 1 + day) / now.getActualMaximum(Calendar.DAY_OF_YEAR);
        float[] v = {day, wd, mon, yr}; for (int i = 0; i < 4; i++) { float y = 70 + i * 36; lab(c, p, nm[i], 40, y + 4, MUTED, Paint.Align.LEFT); rr(c, p, 112, y - 8, 372, y + 8, 8, 0x14ffffff); rr(c, p, 112, y - 8, 112 + Math.max(16, 260 * v[i]), y + 8, 8, col[i]); tx(c, p, Math.round(v[i] * 100) + "%", 392, y + 6, 18, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT); }
        return col[0];
    }
    private static int bigDate(Canvas c, Paint p, Calendar now) {
        int a = 0xffff453a; p.setLetterSpacing(-.03f); tx(c, p, two(now.get(Calendar.DAY_OF_MONTH)), 34, 186, 178, INK, Fonts.ROUNDED_BLACK, Paint.Align.LEFT); p.setLetterSpacing(0f);
        lab(c, p, new SimpleDateFormat("EEEE", Locale.getDefault()).format(now.getTime()), 290, 92, a, Paint.Align.LEFT); tx(c, p, new SimpleDateFormat("MMMM", Locale.getDefault()).format(now.getTime()), 290, 130, 30, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT);
        tx(c, p, "Week " + now.get(Calendar.WEEK_OF_YEAR) + "  ·  Day " + now.get(Calendar.DAY_OF_YEAR), 290, 156, 14, MUTED, Fonts.UI_MED, Paint.Align.LEFT); pill(c, p, 290, 170, 96, 30, hm(now), 14, INK); return a;
    }
    private static int weather(Canvas c, Paint p, Context ctx) {
        int a = 0xff5ac8fa; lab(c, p, Weather.ready() ? Weather.describe(Weather.code) : "Weather", 44, 36, a, Paint.Align.LEFT); tx(c, p, temp(ctx), 36, 122, 88, INK, Fonts.ROUNDED_BOLD, Paint.Align.LEFT);
        if (!Weather.ready()) { tx(c, p, Weather.hasPlace() ? "Fetching the weather" : "Allow location in Standby settings", 40, 160, 14, MUTED, Fonts.UI_MED, Paint.Align.LEFT); return a; }
        Weather.icon(c, P, Weather.code, 400, 92, 44); int hi = Weather.shown(ctx, Weather.hi), lo = Weather.shown(ctx, Weather.lo), t = Weather.shown(ctx, Weather.temp); float f = hi == lo ? .5f : Math.max(0, Math.min(1, (t - lo) / (float) (hi - lo)));
        rr(c, p, 40, 172, 440, 180, 4, 0x18ffffff); opaque(p).setShader(new LinearGradient(40, 0, 440, 0, 0xff5ac8fa, 0xffffb340, Shader.TileMode.CLAMP)); c.drawRoundRect(40, 172, 440, 180, 4, 4, p); p.setShader(null); dot(c, p, 40 + 400 * f, 176, 8, INK);
        tx(c, p, "L " + lo + "°", 40, 204, 15, MUTED, Fonts.UI_SEMI, Paint.Align.LEFT); tx(c, p, "H " + hi + "°", 440, 204, 15, MUTED, Fonts.UI_SEMI, Paint.Align.RIGHT); return a;
    }
    private static int aurora(Canvas c, Paint p, Calendar now, long ms) {
        float ph = (ms % 31416L) / 5000f; int[] cols = {0xff30d158, 0xff5ac8fa, 0xffbf5af2}; for (int k = 0; k < 3; k++) { Path path = new Path(); float base = 100 + k * 36, amp = 34 - k * 6; path.moveTo(0, base); for (int x = 0; x <= 480; x += 12) path.lineTo(x, base + amp * (float) Math.sin(x / 60f + ph + k * 1.4f)); path.lineTo(480, 222); path.lineTo(0, 222); path.close();
            p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(0, 0, 480, 0, new int[]{cols[k], cols[(k + 1) % 3], cols[(k + 2) % 3]}, null, Shader.TileMode.CLAMP)); p.setAlpha(80 + k * 40); c.drawPath(path, p); p.setShader(null); p.setAlpha(255); }
        stars(c, p, 20, 3, 0, 480, 10, 100, .6f); num(c, p, hm(now), 240, 108, 68, INK, Fonts.ROUNDED_BOLD); return 0xff5ac8fa;
    }
    private static int breathe(Canvas c, Paint p, Calendar now, long ms) {
        int a = 0xff64d2ff; float t = (ms % 19000) / 1000f, scale, count; String word; if (t < 4) { scale = t / 4f; word = "Breathe in"; count = 4 - t; } else if (t < 11) { scale = 1; word = "Hold"; count = 11 - t; } else { scale = 1 - (t - 11) / 8f; word = "Breathe out"; count = 19 - t; }
        float e = scale * scale * (3 - 2 * scale); dot(c, p, 240, 113, 92 * (.6f + .4f * e), 0x1464d2ff); dot(c, p, 240, 113, 70 * (.6f + .4f * e), 0x2964d2ff); dot(c, p, 240, 113, 48 * (.6f + .5f * e), 0xe664d2ff);
        tx(c, p, Integer.toString((int) Math.ceil(count)), 240, 121, 26, 0xff032030, Fonts.ROUNDED_BOLD, Paint.Align.CENTER); lab(c, p, word, 240, 36, a, Paint.Align.CENTER); tx(c, p, "4 in  ·  7 hold  ·  8 out  ·  " + hm(now), 240, 212, 13, MUTED, Fonts.UI_MED, Paint.Align.CENTER); return a;
    }
    private static int terminal(Canvas c, Paint p, Context ctx, Calendar now) {
        int g = 0xff30d158; Typeface m = Typeface.MONOSPACE; boolean[] ch = new boolean[1]; int pct = ClockFrames.battery(ctx, ch);
        tx(c, p, "standby@phone ~ $ date", 36, 62, 14, g, m, Paint.Align.LEFT); tx(c, p, hm(now) + ":" + two(now.get(Calendar.SECOND)), 36, 106, 44, g, Typeface.create(Typeface.MONOSPACE, Typeface.BOLD), Paint.Align.LEFT);
        tx(c, p, new SimpleDateFormat("EEE d MMM yyyy  'UTC'XXX", Locale.getDefault()).format(now.getTime()), 36, 132, 14, 0xff2a9d47, m, Paint.Align.LEFT);
        tx(c, p, "battery  " + (pct < 0 ? "--" : pct + "%") + (ch[0] ? "  charging" : ""), 36, 166, 14, g, m, Paint.Align.LEFT); tx(c, p, "weather  " + (Weather.ready() ? Weather.shown(ctx, Weather.temp) + "  " + Weather.describe(Weather.code).toLowerCase(Locale.ROOT) : "--"), 36, 188, 14, g, m, Paint.Align.LEFT);
        if ((System.currentTimeMillis() / 500) % 2 == 0) rr(c, p, 36, 198, 45, 214, 0, g); return g;
    }
    private static int sky(Canvas c, Paint p, Calendar now, long ms) {
        float hr = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) / 60f; int top, bot; String name; boolean night = hr < 5.5f || hr >= 20f;
        if (hr < 5.5f) { top = 0xff03060f; bot = 0xff0a1730; name = "Night"; } else if (hr < 8) { top = 0xff2a3a6a; bot = 0xffe8a070; name = "Dawn"; } else if (hr < 16.5f) { top = 0xff3a7bd5; bot = 0xff9fd0f5; name = "Day"; } else if (hr < 19) { top = 0xff2a2a5a; bot = 0xffe0705a; name = "Golden hour"; } else if (hr < 20.5f) { top = 0xff0a0f2e; bot = 0xff8a3a4a; name = "Dusk"; } else { top = 0xff03060f; bot = 0xff0a1730; name = "Night"; }
        opaque(p).setShader(new LinearGradient(0, 0, 0, 222, top, bot, Shader.TileMode.CLAMP)); c.drawRect(0, 0, 480, 222, p); p.setShader(null); if (night || hr >= 19) stars(c, p, 30, 11, 0, 480, 6, 130, .8f);
        if (night) { Path m = new Path(), s = new Path(); m.addCircle(340, 56, 20, Path.Direction.CW); s.addCircle(348, 52, 17, Path.Direction.CW); m.op(s, Path.Op.DIFFERENCE); p.setColor(0xffe8e0ff); c.drawPath(m, p); } else dot(c, p, 340, hr < 12 ? 86 - (hr - 6) * 8 : 40 + (hr - 12) * 8, 20, 0xffffe0a8);
        Path hill = new Path(); hill.moveTo(0, 200); hill.lineTo(60, 174); hill.lineTo(130, 194); hill.lineTo(220, 164); hill.lineTo(300, 188); hill.lineTo(390, 170); hill.lineTo(480, 192); hill.lineTo(480, 222); hill.lineTo(0, 222); hill.close(); p.setColor(0xff0d0a18); c.drawPath(hill, p);
        num(c, p, hm(now), 130, 128, 72, INK, Fonts.ROUNDED_BOLD); lab(c, p, name, 44, 60, 0xffffd0d8, Paint.Align.LEFT); return 0xffffd0d8;
    }
    private static int timer(Canvas c, Paint p, Context ctx, long ms) {
        int a = 0xff64d2ff; Timers.State t = NewFrames.runningTimer(); float cx = 130, cy = 113, r = 78; for (int i = 0; i < 60; i++) { double an = Math.toRadians(i * 6); float l = i % 5 == 0 ? 8 : 4; line(c, p, cx + (float) Math.sin(an) * (r + 12), cy - (float) Math.cos(an) * (r + 12), cx + (float) Math.sin(an) * (r + 12 + l), cy - (float) Math.cos(an) * (r + 12 + l), 1, 0x40ffffff); }
        ring(c, p, cx, cy, r, 14, 0x14ffffff); String time = "--:--", of = ""; float frac = 0; boolean done = false;
        if (t != null) { done = t.done(ms); frac = done ? 1 : t.fraction(ms); long s = Math.max(0, (t.left(ms) + 999) / 1000); time = s >= 3600 ? (s / 3600) + ":" + two((int) (s / 60 % 60)) + ":" + two((int) (s % 60)) : two((int) (s / 60)) + ":" + two((int) (s % 60)); long tot = t.totalMs / 1000; of = "of " + (tot >= 3600 ? (tot / 3600) + ":" + two((int) (tot / 60 % 60)) + ":" + two((int) (tot % 60)) : two((int) (tot / 60)) + ":" + two((int) (tot % 60))); }
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(14); p.setColor(t != null && t.paused ? MUTED : a); c.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), -90, 360f * frac, false, p); p.setStyle(Paint.Style.FILL);
        tx(c, p, time, cx, cy + 10, 36, INK, Fonts.ROUNDED_BOLD, Paint.Align.CENTER); tx(c, p, of, cx, cy + 30, 12, MUTED, Fonts.UI_MED, Paint.Align.CENTER);
        lab(c, p, t == null ? "Timer" : done ? "Time's up" : t.paused ? "Paused" : "Timer" + (t.label != null && !t.label.isEmpty() && !t.label.toLowerCase(Locale.ROOT).startsWith("timer") ? "  ·  " + t.label : ""), 250, 78, a, Paint.Align.LEFT);
        if (t == null) { tx(c, p, "No timer running", 250, 112, 22, INK, Fonts.UI_SEMI, Paint.Align.LEFT); tx(c, p, "Start one in the Clock app", 250, 136, 13, MUTED, Fonts.UI_MED, Paint.Align.LEFT); String[] ps = {"1 min", "5 min", "10 min"}; for (int i = 0; i < 3; i++) pill(c, p, 250 + i * 70, 156, 64, 30, ps[i], 12, INK); }
        else { Calendar e = Calendar.getInstance(); e.setTimeInMillis(t.paused ? ms + t.left(ms) : t.endAt); tx(c, p, (t.paused ? "Would end " : "Ends ") + hm(e), 250, 118, 22, INK, Fonts.UI_SEMI, Paint.Align.LEFT); tx(c, p, Math.round(100 * frac) + "% left", 250, 142, 13, MUTED, Fonts.UI_MED, Paint.Align.LEFT); }
        return a;
    }
}
