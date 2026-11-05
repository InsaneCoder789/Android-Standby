package com.rohan.standby;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** Instant mode plumbing: whether Standby is on screen, whether the permission is held, and starting or stopping the watcher service. */
final class Instant {
    /** True while {@link StandbyActivity} is on screen, so the watcher doesn't open a second one. */
    static volatile boolean showing;
    /** When {@link StandbyActivity} last went away (elapsedRealtime), so the watcher can tell a power press that closed Standby from one that locked the phone. */
    static volatile long closedAt = -100000;

    static boolean overlayAllowed(Context c) { return Settings.canDrawOverlays(c); }

    /** Brings the watcher in line with the setting. Safe to call from anywhere; a refusal from the system (background start limits) is ignored. */
    static void sync(Context c) {
        Context app = c.getApplicationContext(); Intent i = new Intent(app, InstantService.class);
        try { if (Prefs.instantMode(app)) app.startForegroundService(i); else app.stopService(i); } catch (RuntimeException ignored) { }
    }
}
