package com.rohan.standby;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * Standby as a full-screen screen that opens itself over the lock screen and turns the display on (Instant mode). It closes when the phone is
 * lifted from its angle, unplugged, or the screen is turned off. Shares {@link StandbyController} with the screensaver, so it behaves the same.
 */
public class StandbyActivity extends Activity {
    static final int LAUNCH_NOTIFICATION = 72;
    private boolean closing; private StandbyView view; private StandbyController controller; private BroadcastReceiver receiver;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); Fonts.init(this);
        if (!Instant.charging(this)) { closing = true; finish(); return; }   // hard rule: never open off the charger
        setShowWhenLocked(true); setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams lp = getWindow().getAttributes(); lp.screenBrightness = Prefs.brightness(this);
        lp.layoutInDisplayCutoutMode = Build.VERSION.SDK_INT >= 30 ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES; getWindow().setAttributes(lp);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        view = new StandbyView(this); setContentView(view);
        controller = new StandbyController(this, getWindow(), view, () -> { closing = true; finish(); }, 0, true);
        receiver = new BroadcastReceiver() { @Override public void onReceive(Context c, Intent i) { closing = true; Instant.closedAt = android.os.SystemClock.elapsedRealtime(); finish(); } };
        IntentFilter f = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED); f.addAction(Intent.ACTION_SCREEN_OFF); registerReceiver(receiver, f);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE); if (nm != null) nm.cancel(LAUNCH_NOTIFICATION);
    }
    @Override protected void onStart() { super.onStart(); if (controller == null) return; Instant.showing = true; controller.start(); }
    @Override protected void onStop() { if (controller == null) { super.onStop(); return; } if (!closing && !isFinishing() && Instant.showing) Instant.coveredAt = android.os.SystemClock.elapsedRealtime(); controller.stop(); Instant.showing = false; Instant.closedAt = android.os.SystemClock.elapsedRealtime(); finish(); super.onStop(); }
    @Override protected void onDestroy() { if (controller == null) { super.onDestroy(); return; } Instant.showing = false; Instant.closedAt = android.os.SystemClock.elapsedRealtime(); controller.stop(); if (receiver != null) unregisterReceiver(receiver); super.onDestroy(); }
}
