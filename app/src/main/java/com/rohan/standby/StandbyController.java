package com.rohan.standby;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Window;
import android.view.WindowManager;

/**
 * Everything Standby does around its view that is the same whether it runs as the screensaver or as the instant screen: the tilt gate,
 * automatic night mode, quiet hours and the brightness control. The host supplies the window and says how to close itself.
 */
final class StandbyController implements SensorEventListener {
    private final Context ctx; private final Window window; private final StandbyView view; private final Runnable close; private final long delayOverride; private final boolean activeAtOnce;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable scheduleCheck = new Runnable() { @Override public void run() { if (!Prefs.withinSchedule(ctx)) { close.run(); return; } handler.postDelayed(this, 30_000); } };
    /** Hard rule: Standby only runs on the charger. Checked every second while showing, so unplugging closes it even if the broadcast was missed or a call screen was on top. */
    private final Runnable powerCheck = new Runnable() { @Override public void run() { if (!Instant.charging(ctx)) { close.run(); return; } handler.postDelayed(this, 1_000); } };
    private float lastLux; private boolean leaving;
    private final Runnable leaveNight = new Runnable() { @Override public void run() { leaving = false; if (night && lastLux > nightLux * 1.6f + 2) { night = false; view.setNight(false); } } };
    private SensorManager sensors; private Sensor motion, light; private TiltGate gate; private boolean gated, autoNight, night, running; private float nightLux;

    /** @param delayMs how long the pose must hold before showing (-1 = the user's Start delay); @param activeAtOnce show immediately, using the gate only to close. */
    StandbyController(Context ctx, Window window, StandbyView view, Runnable close, long delayMs, boolean activeAtOnce) {
        this.ctx = ctx; this.window = window; this.view = view; this.close = close; this.delayOverride = delayMs; this.activeAtOnce = activeAtOnce;
        view.setBrightnessControl(new StandbyView.BrightnessControl() {
            @Override public float get() { return window.getAttributes().screenBrightness; }
            @Override public void set(float v) { WindowManager.LayoutParams lp = window.getAttributes(); lp.screenBrightness = v; window.setAttributes(lp); Prefs.sp(StandbyController.this.ctx).edit().putInt("brightness", Math.round(v * 100)).apply(); }
        });
        sensors = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE); light = sensors.getDefaultSensor(Sensor.TYPE_LIGHT);
        motion = sensors.getDefaultSensor(Sensor.TYPE_GRAVITY); if (motion == null) motion = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    void start() {
        if (running) return; running = true;
        if (!Instant.charging(ctx)) { close.run(); return; } // not on the charger: never show
        handler.postDelayed(powerCheck, 1_000);
        if (!Prefs.withinSchedule(ctx)) { close.run(); return; } // quiet hours: outside the window Standby stays out of the way
        if (Prefs.scheduleOn(ctx)) handler.postDelayed(scheduleCheck, 30_000);
        gated = Prefs.requireTilt(ctx) && motion != null; autoNight = Prefs.autoNight(ctx); nightLux = Prefs.nightLux(ctx);
        gate = new TiltGate(delayOverride >= 0 ? delayOverride : Prefs.activationDelayMs(ctx));
        if (motion != null) sensors.registerListener(this, motion, SensorManager.SENSOR_DELAY_NORMAL); // also tells the view which way to turn the picture
        if (!gated || activeAtOnce) view.setActive(true);
        if (autoNight && light != null) sensors.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
    }
    void stop() { if (!running) return; running = false; handler.removeCallbacks(scheduleCheck); handler.removeCallbacks(powerCheck); handler.removeCallbacks(leaveNight); leaving = false; sensors.unregisterListener(this); }

    @Override public void onSensorChanged(SensorEvent e) {
        if (!running) return;
        if (e.sensor.getType() == Sensor.TYPE_LIGHT) {
            // Hysteresis: enter night below the threshold, leave it well above, so the face doesn't flicker.
            float lux = e.values[0]; lastLux = lux;
            if (!night && lux < nightLux) { night = true; view.setNight(true); }
            else if (night) {
                // One stray bright reading (a sensor spike, a passing glow) used to flip the whole screen to day mode for a split second. Leave night only once it stays bright.
                if (lux > nightLux * 1.6f + 2) { if (!leaving) { leaving = true; handler.postDelayed(leaveNight, 4_000); } }
                else if (leaving) { leaving = false; handler.removeCallbacks(leaveNight); }
            }
            return;
        }
        view.setGravityX(e.values[0]);
        if (!gated) return;
        TiltGate.State s = gate.update(e.values[0], e.values[1], e.values[2], SystemClock.elapsedRealtime());
        if (s == TiltGate.State.ACTIVE) view.setActive(true); else if (s == TiltGate.State.CLOSED) { view.setActive(false); close.run(); }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
