package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class WeatherTest {
    @Test public void describesCommonCodes() {
        assertEquals("Clear", Weather.describe(0)); assertEquals("Mostly Sunny", Weather.describe(1)); assertEquals("Partly Cloudy", Weather.describe(2)); assertEquals("Cloudy", Weather.describe(3));
        assertEquals("Foggy", Weather.describe(45)); assertEquals("Drizzle", Weather.describe(53)); assertEquals("Rain", Weather.describe(63)); assertEquals("Rain", Weather.describe(81));
        assertEquals("Snow", Weather.describe(73)); assertEquals("Snow", Weather.describe(86)); assertEquals("Thunderstorms", Weather.describe(95)); assertEquals("Thunderstorms", Weather.describe(99));
    }
    @Test public void unknownCodesFallBackGracefully() { assertEquals("Weather", Weather.describe(-1)); assertEquals("Weather", Weather.describe(4)); assertEquals("Weather", Weather.describe(60)); }
}
