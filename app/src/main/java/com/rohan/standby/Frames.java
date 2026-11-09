package com.rohan.standby;

import java.util.ArrayList;
import java.util.List;

/**
 * The catalogue of frames. A frame is one page of the sideways carousel; each frame has one or more components that you move through by
 * swiping up or down (for example the Dial frame holds Analog, Widgets and Calendar, which share the same dial). Standby shows at most
 * {@link #MAX} frames. Selection helpers are pure so every edge case is unit-tested.
 */
final class Frames {
    static final int DIGITAL = 0, DIAL = 1, MUSIC = 2, CLOCK_MUSIC = 3, PHOTOS = 4, WORLD = 5, FLIP = 6, WORD = 7, DOTS = 8, NIXIE = 9, LED = 10, RAILWAY = 11, ORBIT = 12,
        SUN = 13, MOON = 14, PROGRESS = 15, BIGDATE = 16, WEATHER = 17, AURORA = 18, BREATHE = 19, TERMINAL = 20, SKY = 21, TIMER = 22;
    static final int MAX = 3, COUNT = 23;
    static final String[] NAMES = {"Digital", "Dial", "Music", "Clock and Music", "Photos", "World", "Flip", "Word clock", "Dot matrix", "Nixie", "Alarm LED", "Railway", "Orbit",
        "Sun path", "Moon", "Time left", "Big date", "Weather", "Aurora", "Breathe", "Terminal", "Sky", "Timer"};
    static final String[] INFO = {"Large time. Digital and Float", "One dial, three looks. Analog, Widgets and Calendar", "Now Bar style player", "Time with the player beside it",
        "Your photos, one at a time", "Clocks in other cities", "Mechanical flip cards", "The time written out in letters", "Glowing LED dots", "Warm glass tubes",
        "Seven segment bedside clock", "Swiss station clock", "Hours, minutes and seconds on orbits", "Sunrise and sunset along the day's arc", "Tonight's phase", "How much of the day, week, month and year is gone",
        "A poster of today's date", "Temperature and sky", "Slow drifting colour", "A paced breathing guide for winding down", "Green on black, for the curious", "A sky that follows the hour", "Quick timers, and the running one live"};
    /** How many components each frame has (swipe up or down to move through them). */
    /** The looks each frame had before the redesign. The redesign is one extra look after these, so every existing look stays exactly as it was. */
    static final int[] BASE = {2, 3, 1, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2};
    static final int[] COMPONENTS = new int[COUNT];
    static { for (int i = 0; i < COUNT; i++) COMPONENTS[i] = BASE[i] + 1; }
    /** Which frames use the full-bleed animation rate rather than once a second. */
    static boolean animated(int frame, int comp) { return frame == BREATHE || frame == AURORA; }
    /** Frames that show the Now Bar player (and so take part in the music touch handling). */
    static boolean hasPlayer(int frame) { return frame == MUSIC || frame == CLOCK_MUSIC; }

    static final int[] DEFAULT = {DIGITAL, DIAL, MUSIC};

    /** "0,1,2" -> a valid selection: in range, no repeats, at most {@link #MAX}, never empty. */
    static int[] parse(String saved) {
        List<Integer> out = new ArrayList<>();
        if (saved != null) for (String s : saved.split(",")) {
            try { int v = Integer.parseInt(s.trim()); if (v >= 0 && v < COUNT && !out.contains(v) && out.size() < MAX) out.add(v); } catch (NumberFormatException ignored) { }
        }
        if (out.isEmpty()) return DEFAULT.clone();
        int[] a = new int[out.size()]; for (int i = 0; i < a.length; i++) a[i] = out.get(i); return a;
    }
    static String format(int[] sel) { StringBuilder sb = new StringBuilder(); for (int i = 0; i < sel.length; i++) { if (i > 0) sb.append(','); sb.append(sel[i]); } return sb.toString(); }
    static boolean contains(int[] sel, int frame) { for (int f : sel) if (f == frame) return true; return false; }
    /** Adds a frame to the end. Returns the same array when it is already there or the carousel is full. */
    static int[] add(int[] sel, int frame) { if (contains(sel, frame) || sel.length >= MAX || frame < 0 || frame >= COUNT) return sel; int[] a = java.util.Arrays.copyOf(sel, sel.length + 1); a[sel.length] = frame; return a; }
    /** Removes a frame. The last remaining frame can't be removed. */
    static int[] remove(int[] sel, int frame) {
        if (!contains(sel, frame) || sel.length <= 1) return sel; int[] a = new int[sel.length - 1]; int n = 0; for (int f : sel) if (f != frame) a[n++] = f; return a;
    }
    /** Moves the frame at position {@code pos} one place earlier (-1) or later (+1). Out of range does nothing. */
    static int[] move(int[] sel, int pos, int dir) {
        int to = pos + dir; if (pos < 0 || pos >= sel.length || to < 0 || to >= sel.length) return sel; int[] a = sel.clone(); int t = a[pos]; a[pos] = a[to]; a[to] = t; return a;
    }
    /** Wraps around: the carousel has no end. */
    static int wrap(int index, int size) { return size <= 0 ? 0 : ((index % size) + size) % size; }
    /** Old single-face numbering (0 Digital, 1 Float, 2 Analog, 3 Widgets, 4 Calendar, 5 Music, 6 Photos, 7 World) as {frame, component}. Used by the preview tools. */
    static int[] fromLegacy(int face) {
        switch (face) { case 0: return new int[]{DIGITAL, 0}; case 1: return new int[]{DIGITAL, 1}; case 2: return new int[]{DIAL, 0}; case 3: return new int[]{DIAL, 1}; case 4: return new int[]{DIAL, 2};
            case 5: return new int[]{MUSIC, 0}; case 6: return new int[]{PHOTOS, 0}; default: return new int[]{WORLD, 0}; }
    }
    /** Frames that show only the time get no charging ring (Digital and Float), as on iOS. */
    static boolean hasRing(int frame, int comp) { return frame != DIGITAL; }
}
