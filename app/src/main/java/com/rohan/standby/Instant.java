package com.rohan.standby;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** Instant mode plumbing: whether Standby is on screen, whether the permission is held, and starting or stopping the watcher service. */
final class Instant {
    /** True while {@link StandbyActivity} is on screen, so the watcher doesn't open a second one. */
    static volatile boolean showing;
    /** When Standby was last stopped by something else drawing over it (not by the user closing it); the call guard uses this. */
    static volatile long coveredAt = -100000;
    /** When {@link StandbyActivity} last went away (elapsedRealtime), so the watcher can tell a power press that closed Standby from one that locked the phone. */
    static volatile long closedAt = -100000;

    /** True when Android is told not to put Standby to sleep, which is what keeps the watcher alive on phones with aggressive battery managers. */
    static boolean unrestricted(Context c) { android.os.PowerManager pm = (android.os.PowerManager) c.getSystemService(Context.POWER_SERVICE); return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName()); }

    /**
     * The hard rule: Standby exists only while the phone is plugged in (cable or wireless, including when full). Every way of opening Standby, every call/alarm
     * wake-up and the call guard check this first, and a showing Standby re-checks it every second and closes itself the moment the charger is removed.
     */
    static boolean charging(Context c) {
        try { android.content.Intent i = c.getApplicationContext().registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)); return i != null && i.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) != 0; }
        catch (RuntimeException e) { return false; }
    }

    static boolean overlayAllowed(Context c) { return Settings.canDrawOverlays(c); }

    /** Brings the watcher in line with the setting. Safe to call from anywhere; a refusal from the system (background start limits) is ignored. */
    static void sync(Context c) {
        Context app = c.getApplicationContext(); Intent i = new Intent(app, InstantService.class);
        if (Prefs.instantMode(app)) InstantJob.schedule(app); else InstantJob.cancel(app);
        try { if (Prefs.instantMode(app)) app.startForegroundService(i); else app.stopService(i); } catch (RuntimeException ignored) { }
    }
}
