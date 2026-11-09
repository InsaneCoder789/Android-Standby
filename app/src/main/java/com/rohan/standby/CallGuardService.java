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
 * It only looks at the phone app's call screen, and only for the Speaker / Bluetooth button when you tap it on Standby; it acts only while charging and a call is ringing or active.
 */
public class CallGuardService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int tries;
    static volatile CallGuardService instance;
    private volatile boolean tapping; private String tapLabel; private int tapTries;
    @Override protected void onServiceConnected() { super.onServiceConnected(); instance = this; }
    /**
     * Presses the phone app's own Speaker / Bluetooth button (Android only lets the call-owning app change call audio). Charger-gated like everything else.
     * The caller has already opened the phone app; this waits for its call screen, presses the button, then puts Standby back. Returns false if it cannot.
     */
    static boolean tapAudio(String label) {
        CallGuardService s = instance; if (s == null || !Instant.charging(s)) return false;
        s.handler.post(() -> { s.tapping = true; s.tapLabel = label; s.tapTries = 0; s.handler.removeCallbacks(s.tap); s.handler.postDelayed(s.tap, 350); });
        return true;
    }
    private final Runnable tap = new Runnable() { @Override public void run() {
        if (!Instant.charging(CallGuardService.this) || !callActive()) { tapping = false; return; }
        boolean done = false; StringBuilder seen = new StringBuilder();
        try {
            java.util.List<android.view.accessibility.AccessibilityNodeInfo> roots = new java.util.ArrayList<>();
            for (android.view.accessibility.AccessibilityWindowInfo w : getWindows()) { android.view.accessibility.AccessibilityNodeInfo r = w.getRoot(); if (r != null) roots.add(r); }
            android.view.accessibility.AccessibilityNodeInfo ar = getRootInActiveWindow(); if (ar != null) roots.add(ar);
            for (android.view.accessibility.AccessibilityNodeInfo r : roots) {
                if (r.getPackageName() == null || r.getPackageName().toString().equals(getPackageName())) continue;   // never press Standby's own window
                done = press(r, tapLabel.toLowerCase(java.util.Locale.ROOT), seen); if (done) break;
            }
        } catch (RuntimeException ignored) { }
        if (!done && tapTries == 19) android.util.Log.i("StandbyTap", "no '" + tapLabel + "' button found; buttons seen: " + seen);
        if (done || ++tapTries >= 20) {
            tapping = false;
            handler.postDelayed(() -> { try { startActivity(new Intent(CallGuardService.this, StandbyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); } catch (RuntimeException ignored) { } }, 350);
        } else handler.postDelayed(this, 250);
    } };
    /** Depth-first over a window: presses the first clickable control whose text, description or id says the label. */
    private boolean press(android.view.accessibility.AccessibilityNodeInfo n, String label, StringBuilder seen) {
        if (n == null) return false;
        String t = String.valueOf(n.getText()).toLowerCase(java.util.Locale.ROOT), d = String.valueOf(n.getContentDescription()).toLowerCase(java.util.Locale.ROOT), id = String.valueOf(n.getViewIdResourceName()).toLowerCase(java.util.Locale.ROOT);
        if (n.isClickable() && seen.length() < 600) seen.append('[').append(n.getPackageName()).append(' ').append(t).append('|').append(d).append('|').append(id).append(']');
        if (t.contains(label) || d.contains(label) || id.contains(label)) {
            android.view.accessibility.AccessibilityNodeInfo c = n; int up = 0;
            while (c != null && !c.isClickable() && up++ < 4) c = c.getParent();
            if (c != null && c.isEnabled() && c.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) return true;
        }
        for (int i = 0; i < n.getChildCount(); i++) if (press(n.getChild(i), label, seen)) return true;
        return false;
    }
    private final Runnable bringBack = new Runnable() { @Override public void run() {
        if (tapping) { handler.postDelayed(this, 300); return; }   // pressing the phone app's button: don't pull Standby back over it yet
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
            if (!Instant.charging(this)) return false;   // hard rule: never bring Standby back unless the phone is on the charger
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
    @Override public void onDestroy() { if (instance == this) instance = null; super.onDestroy(); }
    @Override public void onInterrupt() { handler.removeCallbacks(bringBack); }
    @Override public boolean onUnbind(Intent i) { handler.removeCallbacks(bringBack); return super.onUnbind(i); }
}
