package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class ScheduleTest {
    private static final int H = 60;
    @Test public void daytimeWindow() { assertTrue(Schedule.isActive(9 * H, 8 * H, 17 * H)); assertFalse(Schedule.isActive(7 * H + 59, 8 * H, 17 * H)); assertFalse(Schedule.isActive(17 * H, 8 * H, 17 * H)); assertTrue(Schedule.isActive(8 * H, 8 * H, 17 * H)); }
    @Test public void overnightWindowWrapsPastMidnight() {
        int start = 22 * H, end = 7 * H;
        assertTrue(Schedule.isActive(23 * H, start, end)); assertTrue(Schedule.isActive(0, start, end)); assertTrue(Schedule.isActive(6 * H + 59, start, end)); assertTrue(Schedule.isActive(22 * H, start, end));
        assertFalse(Schedule.isActive(7 * H, start, end)); assertFalse(Schedule.isActive(12 * H, start, end)); assertFalse(Schedule.isActive(21 * H + 59, start, end));
    }
    @Test public void equalStartAndEndMeansAlways() { assertTrue(Schedule.isActive(0, 5 * H, 5 * H)); assertTrue(Schedule.isActive(13 * H, 5 * H, 5 * H)); }
    @Test public void formatsBothClockStyles() {
        assertEquals("22:05", Schedule.format(22 * H + 5, true)); assertEquals("10:05 PM", Schedule.format(22 * H + 5, false)); assertEquals("12:00 AM", Schedule.format(0, false)); assertEquals("12:30 PM", Schedule.format(12 * H + 30, false)); assertEquals("00:00", Schedule.format(0, true));
    }
}
