package com.rohan.standby;

import static org.junit.Assert.*;
import java.util.TimeZone;
import org.junit.Test;

public class WorldCitiesTest {
    @Test public void cityNamesAreReadable() { assertEquals("New York", WorldCities.city("America/New_York")); assertEquals("Buenos Aires", WorldCities.city("America/Argentina/Buenos_Aires")); assertEquals("Tokyo", WorldCities.city("Asia/Tokyo")); assertEquals("UTC", WorldCities.city("UTC")); }
    @Test public void everyCuratedZoneExists() { for (String z : WorldCities.ZONES) assertTrue(z, java.util.Arrays.asList(TimeZone.getAvailableIDs()).contains(z)); for (String z : WorldCities.DEFAULTS) assertTrue(z, java.util.Arrays.asList(TimeZone.getAvailableIDs()).contains(z)); }
    @Test public void parseDropsUnknownDuplicatesAndCaps() {
        assertArrayEquals(new String[]{"Asia/Tokyo", "Europe/London"}, WorldCities.parse("Asia/Tokyo,Nope/Nowhere,Asia/Tokyo, Europe/London", 4));
        assertEquals(2, WorldCities.parse("Asia/Tokyo,Europe/London,America/New_York", 2).length); assertEquals(0, WorldCities.parse(null, 4).length); assertEquals(0, WorldCities.parse("", 4).length);
    }
    @Test public void relativeLabels() {
        TimeZone la = TimeZone.getTimeZone("America/Los_Angeles"), tokyo = TimeZone.getTimeZone("Asia/Tokyo"), kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        long noonUtc = 1_767_268_800_000L; // 2026-01-01 12:00 UTC: LA is 04:00 same day, Tokyo 21:00 same day, Kolkata 17:30 same day
        assertEquals("Today, +17h", WorldCities.relative(la, tokyo, noonUtc)); assertEquals("Today, +13h 30m", WorldCities.relative(la, kolkata, noonUtc));
        assertEquals("Today, same time", WorldCities.relative(la, la, noonUtc));
        long lateLa = 1_767_315_600_000L; // 2026-01-01 15:00 LA (23:00 UTC): Tokyo is already Jan 2
        assertEquals("Tomorrow, +17h", WorldCities.relative(la, tokyo, lateLa)); assertEquals("Yesterday, -17h", WorldCities.relative(tokyo, la, lateLa));
    }
}
