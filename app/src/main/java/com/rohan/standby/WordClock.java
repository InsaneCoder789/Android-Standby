package com.rohan.standby;

import java.util.ArrayList;
import java.util.List;

/** The Word clock grid and which letters light for a time. Pure and unit-tested for every five-minute step of the day. */
final class WordClock {
    static final String[] GRID = {"ITLISASAMPM", "ACQUARTERDC", "TWENTYFIVEX", "HALFSTENFTO", "PASTERUNINE", "ONESIXTHREE", "FOURFIVETWO", "EIGHTELEVEN", "SEVENTWELVE", "TENSEOCLOCK"};
    static final int ROWS = 10, COLS = 11;
    // {row, firstColumn, length}
    private static final int[] IT = {0, 0, 2}, IS = {0, 3, 2}, QUARTER = {1, 2, 7}, TWENTY = {2, 0, 6}, FIVE_MIN = {2, 6, 4}, HALF = {3, 0, 4}, TEN_MIN = {3, 5, 3}, TO = {3, 9, 2}, PAST = {4, 0, 4}, OCLOCK = {9, 5, 6}, A = {1, 0, 1};
    private static final int[][] HOURS = {{8, 5, 6}, {5, 0, 3}, {6, 8, 3}, {5, 6, 5}, {6, 0, 4}, {6, 4, 4}, {5, 3, 3}, {8, 0, 5}, {7, 0, 5}, {4, 7, 4}, {9, 0, 3}, {7, 5, 6}};   // twelve, one ... eleven

    /** Letter runs to light for the given time. Minutes are rounded to the nearest five. */
    static List<int[]> lit(int hour24, int minute) {
        int total = (hour24 * 60 + minute + 2) / 5 % 288, step = total % 12, hour = total / 12;   // step 0..11 within the hour, rounded
        List<int[]> out = new ArrayList<>(); out.add(IT); out.add(IS);
        int shownHour = hour;
        switch (step) {
            case 0: out.add(OCLOCK); break;
            case 1: out.add(FIVE_MIN); out.add(PAST); break;
            case 2: out.add(TEN_MIN); out.add(PAST); break;
            case 3: out.add(A); out.add(QUARTER); out.add(PAST); break;
            case 4: out.add(TWENTY); out.add(PAST); break;
            case 5: out.add(TWENTY); out.add(FIVE_MIN); out.add(PAST); break;
            case 6: out.add(HALF); out.add(PAST); break;
            case 7: out.add(TWENTY); out.add(FIVE_MIN); out.add(TO); shownHour = hour + 1; break;
            case 8: out.add(TWENTY); out.add(TO); shownHour = hour + 1; break;
            case 9: out.add(A); out.add(QUARTER); out.add(TO); shownHour = hour + 1; break;
            case 10: out.add(TEN_MIN); out.add(TO); shownHour = hour + 1; break;
            default: out.add(FIVE_MIN); out.add(TO); shownHour = hour + 1; break;
        }
        out.add(HOURS[shownHour % 12]); return out;
    }
    /** The words spelled by the lit runs, in reading order. */
    static String sentence(int hour24, int minute) {
        List<int[]> runs = lit(hour24, minute); runs.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]); StringBuilder sb = new StringBuilder();
        for (int[] r : runs) { if (sb.length() > 0) sb.append(' '); sb.append(GRID[r[0]], r[1], r[1] + r[2]); } return sb.toString();
    }
}
