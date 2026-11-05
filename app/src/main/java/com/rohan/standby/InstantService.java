package com.rohan.standby;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

/**
 * Instant mode. Runs as a quiet foreground service; listens to the motion sensor only while the phone is charging. When it is charging and
 * resting at the landscape angle for a moment (see {@link InstantTrigger}), and the phone is locked or its screen is off, it opens
 * {@link StandbyActivity} over the lock screen. The screensaver route stays in place as a fallback.
 */
public class InstantService extends Service implements SensorEventListener {
    private static final String CHANNEL = "instant", CHANNEL_LAUNCH = "instant_launch"; private static final int ID = 71;
    private SensorManager sensors; private Sensor accel; private boolean plugged, listening, haveGravity; private InstantTrigger trigger; private long triggerHold = -1;
    private final float[] g = new float[3];
    private final BroadcastReceiver screen = new BroadcastReceiver() { @Override public void onReceive(Context c, Intent i) { onScreenOff(); } };
    private final BroadcastReceiver battery = new BroadcastReceiver() { @Override public void onReceive(Context c, Intent i) { onPlug(i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0); } };

    @Override public void onCreate() {
        super.onCreate(); NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel quiet = new NotificationChannel(CHANNEL, "Instant mode", NotificationManager.IMPORTANCE_LOW); quiet.setShowBadge(false); nm.createNotificationChannel(quiet);
        NotificationChannel launch = new NotificationChannel(CHANNEL_LAUNCH, "Opening Standby", NotificationManager.IMPORTANCE_HIGH); launch.setShowBadge(false); nm.createNotificationChannel(launch);
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, SettingsActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_stat_standby).setContentTitle("Standby is ready").setContentText("Opens by itself when the phone is charging at an angle")
            .setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).setContentIntent(open).setShowWhen(false).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE); else startForeground(ID, n);
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true);                         // wake-up variant: delivers events while the screen is off
        if (accel == null) accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        registerReceiver(screen, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        Intent sticky = registerReceiver(battery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); onPlug(sticky != null && sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { unregisterReceiver(battery); unregisterReceiver(screen); handler.removeCallbacksAndMessages(null); stopListening(); super.onDestroy(); }

    private void onPlug(boolean now) {
        if (now != plugged) android.util.Log.i("StandbyInstant", now ? "charging: watching the angle" : "unplugged: stopped watching");
        plugged = now; if (!now && trigger != null) trigger.update(SystemClock.elapsedRealtime(), false, false, 0, 0, 0);   // unplugging re-arms
        if (now && accel != null && !listening) { sensors.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL); listening = true; haveGravity = false; } else if (!now) stopListening();
    }
    private void stopListening() { if (listening) { sensors.unregisterListener(this); listening = false; } }

    @Override public void onSensorChanged(SensorEvent e) {
        if (!haveGravity) { System.arraycopy(e.values, 0, g, 0, 3); haveGravity = true; } else for (int i = 0; i < 3; i++) g[i] = .8f * g[i] + .2f * e.values[i];   // smooth the raw reading into a steady gravity estimate
        long hold = Prefs.instantHoldMs(this); if (trigger == null || hold != triggerHold) { trigger = new InstantTrigger(hold); triggerHold = hold; }
        if (trigger.update(SystemClock.elapsedRealtime(), plugged && Prefs.instantMode(this), allowed(), g[0], g[1], g[2])) launch();
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable openAfterLock = new Runnable() { @Override public void run() { if (plugged && Prefs.instantMode(InstantService.this) && !Instant.showing && haveGravity && TiltGate.inside(g[0], g[1], g[2], true) && allowed()) launch(); } };
    /**
     * The phone was locked with the power button. Samsung then shows its always-on display, which is not a screensaver, so waiting for the
     * motion sensor is too slow (and sleeps with the CPU). If it is charging at the angle right now, open Standby straight away. A power
     * press that merely closed Standby is respected and does not bring it back.
     */
    private void onScreenOff() {
        handler.removeCallbacks(openAfterLock);
        boolean closedStandby = Instant.showing || SystemClock.elapsedRealtime() - Instant.closedAt < 1500;
        if (!closedStandby) { if (trigger != null) trigger.rearm(); handler.postDelayed(openAfterLock, 600); }
    }

    /** The screen situation allows opening: not already showing, inside quiet hours, and (by default) the phone is locked or its screen is off. */
    private boolean allowed() {
        if (Instant.showing || !Prefs.withinSchedule(this)) return false; if (!Prefs.instantOnlyLocked(this)) return true;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE); KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return !pm.isInteractive() || km.isKeyguardLocked();
    }
    private void launch() {
        android.util.Log.i("StandbyInstant", "charging + landscape angle held: opening Standby (overlay permission " + (Instant.overlayAllowed(this) ? "granted" : "NOT granted") + ")");
        Intent i = new Intent(this, StandbyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (Instant.overlayAllowed(this)) { try { startActivity(i); return; } catch (RuntimeException ignored) { } }
        // Without "Display over other apps" the system may still allow a full-screen notification.
        PendingIntent pi = PendingIntent.getActivity(this, 1, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, CHANNEL_LAUNCH).setSmallIcon(R.drawable.ic_stat_standby).setContentTitle("Standby").setCategory(Notification.CATEGORY_ALARM).setFullScreenIntent(pi, true).setAutoCancel(true).build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(StandbyActivity.LAUNCH_NOTIFICATION, n);
    }
}
