package com.rohan.standby;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import java.util.Locale;

/** Every user-facing option in one place. Keys are stable strings so settings and renderers never drift apart. */
final class Prefs {
    static final String FILE = "standby";
    /** The original eight single faces, still the numbering the preview and audit tools use. See {@link Frames#fromLegacy}. */
    static final int FACES = 8;

    static SharedPreferences sp(Context c) { return c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    static int i(Context c, String key, int def) { return sp(c).getInt(key, def); }
    static boolean b(Context c, String key, boolean def) { return sp(c).getBoolean(key, def); }
    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    // ── frames ──
    /** The (at most three) frames Standby shows, in carousel order. See {@link Frames}. */
    static int[] frames(Context c) { return Frames.parse(sp(c).getString("frames", null)); }
    static void saveFrames(Context c, int[] sel) { sp(c).edit().putString("frames", Frames.format(sel)).apply(); }
    /** The component last shown for a frame (swipe up or down to change). */
    static int component(Context c, int frame) { return clamp(i(c, "comp" + frame, 0), 0, Frames.COMPONENTS[frame] - 1); }

    // ── quiet hours ──
    static boolean scheduleOn(Context c) { return b(c, "scheduleOn", false); }
    static int scheduleStart(Context c) { return clamp(i(c, "scheduleStart", 22 * 60), 0, 1439); }
    static int scheduleEnd(Context c) { return clamp(i(c, "scheduleEnd", 7 * 60), 0, 1439); }
    static boolean withinSchedule(Context c) { if (!scheduleOn(c)) return true; java.util.Calendar now = java.util.Calendar.getInstance(); return Schedule.isActive(now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE), scheduleStart(c), scheduleEnd(c)); }

    // ── activation ──
    static boolean requireTilt(Context c) { return b(c, "requireTilt", true); }
    static long activationDelayMs(Context c) { int v = clamp(i(c, "delay", 1), 0, 2); return v == 0 ? 900 : v == 1 ? 1800 : 3000; }

    // ── display ──
    static float brightness(Context c) { return clamp(i(c, "brightness", 12), 1, 100) / 100f; }
    static boolean autoNight(Context c) { return b(c, "autoNight", true); }
    static float nightLux(Context c) { int v = clamp(i(c, "nightLux", 1), 0, 2); return v == 0 ? 3f : v == 1 ? 8f : 20f; }
    /** 0 red, 1 dim, 2 off */
    static int nightStyle(Context c) { return clamp(i(c, "nightStyle", 0), 0, 2); }
    static boolean burnInShift(Context c) { return b(c, "burnIn", true); }
    static boolean showPicker(Context c) { return b(c, "showPicker", true); }
    static boolean animate(Context c) { return b(c, "animate", true); }
    static boolean showAlerts(Context c) { return b(c, "alerts", true); }

    // ── time and region ──
    static boolean is24(Context c) {
        int v = clamp(i(c, "clock24", 0), 0, 2);
        return v == 0 ? android.text.format.DateFormat.is24HourFormat(c) : v == 2;
    }
    /** Calendar.SUNDAY / MONDAY / locale default. */
    static int firstDow(Context c) { int v = clamp(i(c, "weekStart", 0), 0, 2); return v == 1 ? Calendar.SUNDAY : v == 2 ? Calendar.MONDAY : Calendar.getInstance().getFirstDayOfWeek(); }
    static boolean fahrenheit(Context c) {
        int v = clamp(i(c, "unit", 0), 0, 2); if (v != 0) return v == 1;
        String country = Locale.getDefault().getCountry(); return country.equals("US") || country.equals("LR") || country.equals("BS") || country.equals("BZ") || country.equals("KY");
    }

    // ── per face ──
    static int digitalColor(Context c) { return clamp(i(c, "digital", 0), 0, 3); }
    static boolean digitalDate(Context c) { return b(c, "digitalDate", true); }
    static boolean digitalTemp(Context c) { return b(c, "digitalTemp", true); }
    static boolean digitalAlarm(Context c) { return b(c, "digitalAlarm", true); }
    static int floatPalette(Context c) { return clamp(i(c, "float", 0), 0, 3); }
    static boolean floatColon(Context c) { return b(c, "floatColon", true); }
    static boolean dialSeconds(Context c) { return b(c, "dialSeconds", true); }
    static boolean dialNumerals(Context c) { return b(c, "dialNumerals", true); }
    /** 0 weather, 1 date */
    static int analogSide(Context c) { return clamp(i(c, "analogSide", 0), 0, 1); }
    /** The charging ring (green ring with a bolt) shown on every face except the two that show only the time. */
    static boolean chargeRing(Context c) { return b(c, "chargeRing", true); }
    static boolean dimWeekends(Context c) { return b(c, "dimWeekends", true); }
    static boolean photoClock(Context c) { return b(c, "photoClock", true); }
    static int photoDim(Context c) { return clamp(i(c, "photoDim", 0), 0, 70); }
    /** Slideshow list (newline separated); falls back to the single photo chosen by older versions. */
    static java.util.List<String> photoUris(Context c) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(); String list = sp(c).getString("photoList", null);
        if (list != null) for (String u : list.split("\n")) if (!u.trim().isEmpty()) out.add(u.trim());
        String single = sp(c).getString("photo", null); if (out.isEmpty() && single != null) out.add(single); return out;
    }
    static String photoUri(Context c) { java.util.List<String> l = photoUris(c); return l.isEmpty() ? null : l.get(0); }
    static long photoIntervalMs(Context c) { int v = clamp(i(c, "photoInterval", 1), 0, 3); return v == 0 ? 15_000L : v == 1 ? 60_000L : v == 2 ? 300_000L : 1_800_000L; }

    // ── new faces ──
    static String[] worldZones(Context c) { String saved = sp(c).getString("worldZones", null); return saved == null ? WorldCities.DEFAULTS.clone() : WorldCities.parse(saved, 4); }
    static void saveWorldZones(Context c, String[] zones) { sp(c).edit().putString("worldZones", android.text.TextUtils.join(",", zones)).apply(); }
    static boolean worldSeconds(Context c) { return b(c, "worldSeconds", false); }
    // ── instant mode ──
    /** Open Standby the moment the phone is charging at the landscape angle (a watcher service + a full-screen activity). */
    static boolean instantMode(Context c) { return b(c, "instantMode", true); }
    /** Only when the phone is locked or its screen is off, like StandBy on an iPhone, so it never interrupts you using the phone. */
    static boolean instantOnlyLocked(Context c) { return b(c, "instantOnlyLocked", true); }
    static long instantHoldMs(Context c) { int v = clamp(i(c, "instantHold", 0), 0, 2); return v == 0 ? 1000 : v == 1 ? 2000 : 3000; }
    /** Switch to the Music frame when music starts, and back when it stops. */
    static boolean autoMusic(Context c) { return b(c, "autoMusic", true); }
    static boolean autoReturn(Context c) { return b(c, "autoReturn", true); }
    static boolean musicWave(Context c) { return b(c, "musicWave", true); }
    /** 0 colours from the album art, 1 plain black */
    static int musicBackground(Context c) { return clamp(i(c, "musicBackground", 0), 0, 1); }
}
