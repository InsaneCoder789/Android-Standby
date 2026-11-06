package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class TimersTest {
    private static final long MIN = 60_000L, HOUR = 3_600_000L;

    @Test public void parsesSamsungLengths() {
        assertEquals(45 * MIN, Timers.parseTotal("45 m / 18:22"));
        assertEquals(HOUR + 30 * MIN, Timers.parseTotal("1 h 30 m / 20:05"));
        assertEquals(90_000L, Timers.parseTotal("90 s"));
        assertEquals(HOUR + 5 * MIN + 10_000L, Timers.parseTotal("1 hr 5 min 10 sec"));
        assertEquals(0, Timers.parseTotal(null)); assertEquals(0, Timers.parseTotal("")); assertEquals(0, Timers.parseTotal("Timer"));
    }
    @Test public void onlyTheTextBeforeTheSlashIsALength() { assertEquals(10 * MIN, Timers.parseTotal("10 m / 5 m")); }

    @Test public void parsesTheEndClock() {
        assertEquals(18 * 60 + 22, Timers.parseEndMinute("45 m / 18:22"));
        assertEquals(18 * 60 + 22, Timers.parseEndMinute("45 m / 6:22 PM"));
        assertEquals(0, Timers.parseEndMinute("5 m / 12:00 AM")); assertEquals(12 * 60, Timers.parseEndMinute("5 m / 12:00 pm"));
        assertEquals(-1, Timers.parseEndMinute("45 m")); assertEquals(-1, Timers.parseEndMinute("45 m / soon")); assertEquals(-1, Timers.parseEndMinute("45 m / 25:99"));
        assertEquals(-1, Timers.parseEndMinute(null));
    }

    @Test public void startPlusLengthIsUsedWhenItAgreesWithTheClock() {
        long midnight = 1_000_000_000L, start = midnight + 17 * HOUR + 37 * MIN + 51_567L, now = start + 10 * MIN;
        assertEquals(start + 45 * MIN, Timers.resolveEnd(start, 45 * MIN, 18 * 60 + 22, midnight, now));
    }
    @Test public void theClockWinsWhenTheTimerWasResumed() {
        long midnight = 0, start = 17 * HOUR, now = 17 * HOUR + 30 * MIN;
        long end = Timers.resolveEnd(start, 45 * MIN, 18 * 60 + 40, midnight, now);   // would be 17:45 by start, but the clock says 18:40
        assertEquals(18 * HOUR + 40 * MIN + 30_000L, end);
    }
    @Test public void anEndClockPastMidnightRollsToTomorrow() {
        long midnight = 0, now = 23 * HOUR + 50 * MIN, start = now - MIN;
        long end = Timers.resolveEnd(start, 30 * MIN, 0 * 60 + 20, midnight, now);   // ends 00:20 tomorrow, text says 0:20
        assertEquals(24 * HOUR + 20 * MIN + 30_000L, end);
    }
    @Test public void noClockFallsBackToStartPlusLength() { assertEquals(5_000L + 60_000L, Timers.resolveEnd(5_000L, 60_000L, -1, 0, 0)); }

    @Test public void runningStateCountsDown() {
        long now = 10 * HOUR; Timers.State s = Timers.fromText("45 m / 10:40", now - 5 * MIN, false, 0, 0, now, "Timer");
        assertNotNull(s); assertFalse(s.paused); assertEquals(40 * MIN, s.left(now)); assertEquals(30 * MIN, s.left(now + 10 * MIN)); assertFalse(s.done(now));
        assertEquals(40 / 45f, s.fraction(now), .02f);
    }
    @Test public void finishedStateIsDoneAndNeverNegative() {
        long now = 10 * HOUR; Timers.State s = Timers.fromText("5 m", now - 10 * MIN, false, 0, 0, now, "Timer");
        assertTrue(s.done(now)); assertEquals(0, s.left(now)); assertEquals(0f, s.fraction(now), 0f);
    }
    @Test public void pausedStateFreezes() {
        long now = 10 * HOUR; Timers.State s = Timers.fromText("20 m", now - 5 * MIN, true, 12 * MIN, 0, now, "Timer");
        assertTrue(s.paused); assertEquals(12 * MIN, s.left(now)); assertEquals(12 * MIN, s.left(now + HOUR)); assertFalse(s.done(now + HOUR));
        assertEquals(20 * MIN, Timers.fromText("20 m", now, true, 99 * MIN, 0, now, "Timer").left(now), 0);   // never more than the length
    }
    @Test public void noLengthMeansNoTimer() { assertNull(Timers.fromText("Timer", 0, false, 0, 0, 0, "Timer")); }
    @Test public void countdownChronometer() {
        Timers.State s = Timers.fromCountdown(1000 + 90_000L, 1000, "Timer"); assertEquals(90_000L, s.left(1000)); assertEquals(0, s.left(1_000_000)); assertNull(Timers.fromCountdown(0, 5, "x"));
    }
    @Test public void formatsLikeAClock() {
        assertEquals("00:00", Timers.format(0)); assertEquals("00:01", Timers.format(1)); assertEquals("00:01", Timers.format(1000)); assertEquals("00:02", Timers.format(1001));
        assertEquals("45:00", Timers.format(45 * MIN)); assertEquals("1:02:05", Timers.format(HOUR + 2 * MIN + 5000)); assertEquals("00:00", Timers.format(-5));
    }
}
