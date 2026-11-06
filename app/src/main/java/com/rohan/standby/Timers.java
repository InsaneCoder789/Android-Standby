package com.rohan.standby;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The countdown timer running in the phone's Clock app, as read from its ongoing notification. Pure maths so every edge case is unit-tested.
 * Samsung's notification says "45 m / 18:22" (length / the clock time it ends) and carries the time it was started; Google's uses a countdown chronometer.
 */
final class Timers {
    /** What the frame draws. {@code endAt} is wall-clock; while paused only {@code leftMs} is meaningful. */
    static final class State {
        final long totalMs, endAt, leftMs; final boolean paused; final String label;
        State(long totalMs, long endAt, long leftMs, boolean paused, String label) { this.totalMs = totalMs; this.endAt = endAt; this.leftMs = leftMs; this.paused = paused; this.label = label; }
        long left(long now) { return paused ? leftMs : Math.max(0, endAt - now); }
        boolean done(long now) { return !paused && now >= endAt; }
        float fraction(long now) { return totalMs <= 0 ? 0f : Math.max(0f, Math.min(1f, left(now) / (float) totalMs)); }
    }

    private static final Pattern PART = Pattern.compile("(\\d+)\\s*(h|hr|hrs|hour|hours|m|min|mins|minute|minutes|s|sec|secs|second|seconds)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLOCK = Pattern.compile("(\\d{1,2}):(\\d{2})(?::\\d{2})?\\s*([AaPp][Mm])?");

    /** "45 m / 18:22", "1 h 30 m / 20:05" or "90 s" -> the length in ms; 0 when there is none. Only the part before a slash counts. */
    static long parseTotal(String text) {
        if (text == null) return 0;
        String head = text.contains("/") ? text.substring(0, text.indexOf('/')) : text; long ms = 0; Matcher m = PART.matcher(head);
        while (m.find()) {
            long v = Long.parseLong(m.group(1)); char u = Character.toLowerCase(m.group(2).charAt(0));
            ms += u == 'h' ? v * 3_600_000L : u == 'm' ? v * 60_000L : v * 1000L;
        }
        return ms;
    }

    /** The "18:22" after the slash as minutes since midnight (12-hour text with AM or PM is handled); -1 when absent. */
    static int parseEndMinute(String text) {
        if (text == null || !text.contains("/")) return -1;
        Matcher m = CLOCK.matcher(text.substring(text.indexOf('/') + 1)); if (!m.find()) return -1;
        int h = Integer.parseInt(m.group(1)), min = Integer.parseInt(m.group(2)); String ap = m.group(3);
        if (h > 23 || min > 59) return -1;
        if (ap != null) { boolean pm = Character.toLowerCase(ap.charAt(0)) == 'p'; h = h % 12 + (pm ? 12 : 0); }
        return h * 60 + min;
    }

    /**
     * Works out when the timer ends. The start time plus the length is exact; but if the timer was paused and resumed the start time is stale, so when it
     * disagrees with the end clock time by more than a minute the clock time wins (to the half minute, the best the text allows).
     * {@code midnight} is today's 00:00 in ms and {@code now} the current time.
     */
    static long resolveEnd(long startMs, long totalMs, int endMinute, long midnight, long now) {
        long exact = startMs + totalMs;
        if (endMinute < 0) return exact;
        long clock = midnight + endMinute * 60_000L; if (clock < now - 12 * 3_600_000L) clock += 86_400_000L;
        return exact >= clock && exact < clock + 60_000L ? exact : clock + 30_000L;
    }

    /** A countdown chronometer (Google Clock): {@code when} is the moment it reaches zero. */
    static State fromCountdown(long whenMs, long now, String label) {
        if (whenMs <= 0) return null; long left = Math.max(0, whenMs - now); return new State(Math.max(left, 1000), whenMs, left, false, label);
    }

    /** Samsung style. Returns null when the text holds no length. A paused timer keeps the time it had left when it was last seen running. */
    static State fromText(String text, long startMs, boolean paused, long lastLeftMs, long midnight, long now, String label) {
        long total = parseTotal(text); if (total <= 0) return null;
        long end = resolveEnd(startMs, total, parseEndMinute(text), midnight, now);
        return new State(total, end, paused ? Math.min(total, Math.max(0, lastLeftMs)) : Math.max(0, end - now), paused, label);
    }

    /** 3725000 -> "1:02:05"; under an hour "02:05"; negative counts as zero. */
    static String format(long ms) {
        long s = Math.max(0, (ms + 999) / 1000), h = s / 3600, m = s % 3600 / 60, sec = s % 60;
        return h > 0 ? h + ":" + two(m) + ":" + two(sec) : two(m) + ":" + two(sec);
    }
    private static String two(long v) { return v < 10 ? "0" + v : Long.toString(v); }
}
