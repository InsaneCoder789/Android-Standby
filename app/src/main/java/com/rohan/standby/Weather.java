package com.rohan.standby;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

/** Current conditions from Open-Meteo (no API key) for the last known coarse location, cached for 20 minutes. */
final class Weather {
    static volatile String city = ""; static volatile int temp = Integer.MIN_VALUE, hi, lo, code = -1; static volatile long updated; static volatile boolean fahrenheit;
    /** Last known place (degrees, east-positive longitude), kept so the Sun path and Moon frames work without a network. NaN until a location has been read. */
    static volatile double lat = Double.NaN, lon = Double.NaN;
    static boolean hasPlace() { return !Double.isNaN(lat) && !Double.isNaN(lon); }
    private static boolean busy;
    static boolean ready() { return temp != Integer.MIN_VALUE; }
    /** Cached values are stored in the unit they were fetched in; convert to the unit the user wants. */
    static int shown(Context c, int value) { boolean want = Prefs.fahrenheit(c); if (want == fahrenheit) return value; return want ? Math.round(value * 9f / 5f + 32) : Math.round((value - 32) * 5f / 9f); }

    static void refresh(Context context) {
        final Context app = context.getApplicationContext(); SharedPreferences sp = app.getSharedPreferences("weather", Context.MODE_PRIVATE);
        if (!hasPlace() && sp.contains("lat")) { lat = Double.longBitsToDouble(sp.getLong("lat", 0)); lon = Double.longBitsToDouble(sp.getLong("lon", 0)); }
        if (!ready() && sp.contains("temp")) { city = sp.getString("city", ""); temp = sp.getInt("temp", 0); hi = sp.getInt("hi", 0); lo = sp.getInt("lo", 0); code = sp.getInt("code", 0); updated = sp.getLong("updated", 0); fahrenheit = sp.getBoolean("f", false); }
        if (busy || System.currentTimeMillis() - updated < 20 * 60000L) return;
        if (app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        busy = true;
        new Thread(() -> {
            try {
                LocationManager lm = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE); Location best = null;
                for (String provider : lm.getProviders(true)) { Location l = lm.getLastKnownLocation(provider); if (l != null && (best == null || l.getTime() > best.getTime())) best = l; }
                if (best == null) return;
                lat = best.getLatitude(); lon = best.getLongitude(); app.getSharedPreferences("weather", Context.MODE_PRIVATE).edit().putLong("lat", Double.doubleToLongBits(lat)).putLong("lon", Double.doubleToLongBits(lon)).apply();
                boolean f = Prefs.fahrenheit(app);
                URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=" + best.getLatitude() + "&longitude=" + best.getLongitude() + "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto&temperature_unit=" + (f ? "fahrenheit" : "celsius"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection(); conn.setConnectTimeout(8000); conn.setReadTimeout(8000);
                StringBuilder sb = new StringBuilder(); try (InputStream in = conn.getInputStream()) { byte[] buf = new byte[4096]; int n; while ((n = in.read(buf)) > 0) sb.append(new String(buf, 0, n, "UTF-8")); } finally { conn.disconnect(); }
                JSONObject root = new JSONObject(sb.toString()), cur = root.getJSONObject("current"), day = root.getJSONObject("daily");
                String name = ""; try { List<Address> a = new Geocoder(app, Locale.getDefault()).getFromLocation(best.getLatitude(), best.getLongitude(), 1); if (a != null && !a.isEmpty()) name = a.get(0).getLocality() != null ? a.get(0).getLocality() : a.get(0).getSubAdminArea() != null ? a.get(0).getSubAdminArea() : ""; } catch (Exception ignored) {}
                int t = (int) Math.round(cur.getDouble("temperature_2m")), c = cur.getInt("weather_code"), h = (int) Math.round(day.getJSONArray("temperature_2m_max").getDouble(0)), l = (int) Math.round(day.getJSONArray("temperature_2m_min").getDouble(0));
                app.getSharedPreferences("weather", Context.MODE_PRIVATE).edit().putString("city", name).putInt("temp", t).putInt("hi", h).putInt("lo", l).putInt("code", c).putLong("updated", System.currentTimeMillis()).putBoolean("f", f).apply();
                city = name; temp = t; hi = h; lo = l; code = c; fahrenheit = f; updated = System.currentTimeMillis();
            } catch (Exception ignored) { } finally { busy = false; }
        }, "standby-weather").start();
    }

    static String describe(int c) {
        if (c == 0) return "Clear"; if (c == 1) return "Mostly Sunny"; if (c == 2) return "Partly Cloudy"; if (c == 3) return "Cloudy"; if (c == 45 || c == 48) return "Foggy";
        if (c >= 51 && c <= 57) return "Drizzle"; if (c >= 61 && c <= 67 || c >= 80 && c <= 82) return "Rain"; if (c >= 71 && c <= 77 || c == 85 || c == 86) return "Snow"; if (c >= 95) return "Thunderstorms"; return "Weather";
    }

    /** Simple glyphs: sun, partly cloudy, cloud, rain, snow, storm. Centered at (x,y), s ~ overall width. */
    static void icon(Canvas c, Paint p, int code, float x, float y, float s) {
        p.setStyle(Paint.Style.FILL); p.setShader(null);
        boolean sun = code == 0 || code == 1, partly = code == 2;
        if (sun || partly) {
            float sx = partly ? x - s * .18f : x, sy = partly ? y - s * .16f : y, r = s * (partly ? .20f : .26f);
            p.setColor(0xffffc21a); c.drawCircle(sx, sy, r, p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(s * .065f); p.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; c.drawLine(sx + (float) Math.cos(a) * r * 1.4f, sy + (float) Math.sin(a) * r * 1.4f, sx + (float) Math.cos(a) * r * 1.85f, sy + (float) Math.sin(a) * r * 1.85f, p); }
            p.setStyle(Paint.Style.FILL);
        }
        if (!sun) {
            float cy = y + s * (partly ? .14f : .06f); p.setColor(code >= 95 ? 0xff8e8e93 : 0xffd1d1d6);
            c.drawCircle(x - s * .16f, cy, s * .17f, p); c.drawCircle(x + s * .04f, cy - s * .10f, s * .22f, p); c.drawCircle(x + s * .24f, cy, s * .16f, p); c.drawRoundRect(x - s * .30f, cy, x + s * .38f, cy + s * .17f, s * .08f, s * .08f, p);
            boolean rain = code >= 51 && code <= 67 || code >= 80 && code <= 82 || code >= 95, snow = code >= 71 && code <= 77 || code == 85 || code == 86;
            if (rain || snow) { p.setColor(snow ? 0xffffffff : 0xff5ac8fa); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(s * .05f); p.setStrokeCap(Paint.Cap.ROUND); for (int i = 0; i < 3; i++) { float dx = x - s * .18f + i * s * .2f; c.drawLine(dx, cy + s * .26f, dx - s * .05f, cy + s * .36f, p); } p.setStyle(Paint.Style.FILL); }
            if (code >= 95) { p.setColor(0xffffc21a); Path b = new Path(); b.moveTo(x + s * .02f, cy + s * .10f); b.lineTo(x - s * .08f, cy + s * .30f); b.lineTo(x, cy + s * .30f); b.lineTo(x - s * .05f, cy + s * .46f); b.lineTo(x + s * .12f, cy + s * .24f); b.lineTo(x + s * .03f, cy + s * .24f); b.close(); c.drawPath(b, p); }
        }
    }
}
