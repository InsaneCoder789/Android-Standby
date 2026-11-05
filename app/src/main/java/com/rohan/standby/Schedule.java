package com.rohan.standby;

/** Quiet-hours window in minutes after midnight. Handles windows that cross midnight (22:00 to 07:00). */
final class Schedule {
    static boolean isActive(int nowMinute, int startMinute, int endMinute) {
        if (startMinute == endMinute) return true;                       // empty window means always
        if (startMinute < endMinute) return nowMinute >= startMinute && nowMinute < endMinute;
        return nowMinute >= startMinute || nowMinute < endMinute;        // wraps past midnight
    }
    static String format(int minute, boolean is24) {
        int h = (minute / 60) % 24, m = minute % 60;
        if (is24) return String.format(java.util.Locale.US, "%02d:%02d", h, m);
        return String.format(java.util.Locale.US, "%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, h < 12 ? "AM" : "PM");
    }
}
