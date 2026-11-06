package com.rohan.standby;

import java.util.TimeZone;

/** Curated city list for the World clock face, plus label helpers that depend only on time zones. */
final class WorldCities {
    static final String[] ZONES = {
        "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "America/Denver", "America/Chicago", "America/New_York", "America/Toronto", "America/Mexico_City", "America/Sao_Paulo", "America/Argentina/Buenos_Aires",
        "Atlantic/Reykjavik", "Europe/London", "Europe/Dublin", "Europe/Lisbon", "Europe/Paris", "Europe/Berlin", "Europe/Madrid", "Europe/Rome", "Europe/Stockholm", "Europe/Athens", "Europe/Istanbul", "Europe/Moscow",
        "Africa/Cairo", "Africa/Lagos", "Africa/Nairobi", "Africa/Johannesburg", "Asia/Dubai", "Asia/Karachi", "Asia/Kolkata", "Asia/Dhaka", "Asia/Bangkok", "Asia/Singapore", "Asia/Hong_Kong", "Asia/Shanghai", "Asia/Seoul", "Asia/Tokyo",
        "Australia/Perth", "Australia/Sydney", "Pacific/Auckland"};
    static final String[] DEFAULTS = {"America/New_York", "Europe/London", "Asia/Kolkata", "Asia/Tokyo"};

    /** "America/Argentina/Buenos_Aires" becomes "Buenos Aires". */
    static String city(String zoneId) { int i = zoneId.lastIndexOf('/'); return (i < 0 ? zoneId : zoneId.substring(i + 1)).replace('_', ' '); }

    /** Parses saved ids, dropping unknown ones and duplicates, capped at max. */
    static String[] parse(String saved, int max) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (saved != null) for (String s : saved.split(",")) { s = s.trim(); if (!s.isEmpty() && !out.contains(s) && isKnown(s) && out.size() < max) out.add(s); }
        return out.toArray(new String[0]);
    }
    private static boolean isKnown(String id) { for (String z : TimeZone.getAvailableIDs()) if (z.equals(id)) return true; return false; }

    /** "Today", "Tomorrow", "Yesterday" relative to the local zone, then the hour difference, e.g. "Tomorrow, +5h". */
    static String relative(TimeZone local, TimeZone other, long nowMillis) {
        java.util.Calendar a = java.util.Calendar.getInstance(local), b = java.util.Calendar.getInstance(other); a.setTimeInMillis(nowMillis); b.setTimeInMillis(nowMillis);
        int dayDiff = (b.get(java.util.Calendar.YEAR) * 366 + b.get(java.util.Calendar.DAY_OF_YEAR)) - (a.get(java.util.Calendar.YEAR) * 366 + a.get(java.util.Calendar.DAY_OF_YEAR));
        String day = dayDiff == 0 ? "Today" : dayDiff > 0 ? "Tomorrow" : "Yesterday";
        int diffMin = (other.getOffset(nowMillis) - local.getOffset(nowMillis)) / 60000;
        if (diffMin == 0) return day + ", same time";
        int abs = Math.abs(diffMin); String amount = abs % 60 == 0 ? (abs / 60) + "h" : (abs / 60) + "h " + (abs % 60) + "m";
        return day + ", " + (diffMin > 0 ? "+" : "-") + amount;
    }
}
