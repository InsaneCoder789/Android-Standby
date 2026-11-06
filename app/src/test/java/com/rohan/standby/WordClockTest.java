package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class WordClockTest {
    @Test public void readsTheClassicTimes() {
        assertEquals("IT IS TEN OCLOCK", WordClock.sentence(10, 0)); assertEquals("IT IS FIVE PAST SIX", WordClock.sentence(6, 5)); assertEquals("IT IS TEN PAST THREE", WordClock.sentence(3, 10));
        assertEquals("IT IS A QUARTER PAST NINE", WordClock.sentence(9, 15)); assertEquals("IT IS TWENTY PAST ONE", WordClock.sentence(1, 20)); assertEquals("IT IS TWENTY FIVE PAST TWO", WordClock.sentence(2, 25));
        assertEquals("IT IS HALF PAST SEVEN", WordClock.sentence(7, 30)); assertEquals("IT IS TWENTY FIVE TO EIGHT", WordClock.sentence(7, 35)); assertEquals("IT IS TWENTY TO NINE", WordClock.sentence(8, 40));
        assertEquals("IT IS A QUARTER TO TEN", WordClock.sentence(9, 45)); assertEquals("IT IS TEN TO ELEVEN", WordClock.sentence(10, 50)); assertEquals("IT IS FIVE TO TWELVE", WordClock.sentence(11, 55));
    }
    @Test public void midnightAndNoonAreTwelve() { assertEquals("IT IS TWELVE OCLOCK", WordClock.sentence(0, 0)); assertEquals("IT IS TWELVE OCLOCK", WordClock.sentence(12, 0)); assertEquals("IT IS HALF PAST TWELVE", WordClock.sentence(0, 30)); }
    @Test public void roundsToTheNearestFiveMinutes() { assertEquals("IT IS TEN OCLOCK", WordClock.sentence(9, 58)); assertEquals("IT IS TEN OCLOCK", WordClock.sentence(10, 2)); assertEquals("IT IS FIVE PAST TEN", WordClock.sentence(10, 3)); assertEquals("IT IS TWELVE OCLOCK", WordClock.sentence(23, 58)); }
    @Test public void everyMinuteOfTheDayLightsAValidInBoundsSentence() {
        for (int h = 0; h < 24; h++) for (int m = 0; m < 60; m++) {
            java.util.List<int[]> lit = WordClock.lit(h, m); assertTrue(lit.size() >= 3);
            boolean[][] seen = new boolean[WordClock.ROWS][WordClock.COLS];
            for (int[] r : lit) { assertTrue(r[0] >= 0 && r[0] < WordClock.ROWS && r[1] >= 0 && r[1] + r[2] <= WordClock.COLS); for (int i = 0; i < r[2]; i++) { assertFalse("overlap at " + h + ":" + m, seen[r[0]][r[1] + i]); seen[r[0]][r[1] + i] = true; } }
        }
    }
    @Test public void gridIsRectangular() { assertEquals(WordClock.ROWS, WordClock.GRID.length); for (String r : WordClock.GRID) assertEquals(WordClock.COLS, r.length()); }
}
