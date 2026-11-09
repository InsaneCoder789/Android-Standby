package com.rohan.standby;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * Keeps Standby's call screen in front. When a call arrives while Standby is showing, the phone app opens its own screen over it and Android
 * stops Standby. This service notices that the phone app's window appeared and brings Standby back, so the call is handled on Standby's frame.
 * It never reads screen content (see res/xml/call_guard.xml) and only acts while a call is ringing or active and Standby was the screen just covered.
 */
public class CallGuardService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int tries;
    private final Runnable bringBack = new Runnable() { @Override public void run() {
        if (!callActive()) { tries = 0; return; }
        if (Instant.showing) {
            // The phone app's window usually appears a moment before Android stops Standby, so Standby still looks visible here. Wait for it to be covered instead of giving up.
            if (++tries < 20) handler.postDelayed(this, 300); else tries = 0;
            return;
        }
        if (SystemClock.elapsedRealtime() - Instant.coveredAt > 20_000) { tries = 0; return; }
        try { startActivity(new Intent(CallGuardService.this, StandbyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); } catch (RuntimeException ignored) { }
        // The phone app may open a second window a moment later; check again a few times so Standby really ends up on top.
        if (++tries < 6) handler.postDelayed(this, 400); else tries = 0;
    } };
    /** A call is ringing or active, or an alarm / timer is ringing on Standby's own frame. */
    private boolean callActive() {
        try {
            if (MediaListener.instance != null && MediaListener.instance.alert() != null) return true;
            TelephonyManager t = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            return t != null && checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED && t.getCallState() != TelephonyManager.CALL_STATE_IDLE;
        } catch (RuntimeException e) { return false; }
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (SystemClock.elapsedRealtime() - Instant.coveredAt > 20_000 && !Instant.showing) return; // Standby wasn't what the call covered: leave the phone app alone
        handler.removeCallbacks(bringBack); tries = 0; handler.postDelayed(bringBack, 150);
    }
    @Override public void onInterrupt() { handler.removeCallbacks(bringBack); }
    @Override public boolean onUnbind(Intent i) { handler.removeCallbacks(bringBack); return super.onUnbind(i); }
}
