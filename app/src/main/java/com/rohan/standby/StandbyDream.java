package com.rohan.standby;

import android.content.pm.ActivityInfo;
import android.service.dreams.DreamService;
import android.view.WindowManager;

/** Standby as Android's screensaver: starts when the screen times out while charging. See also {@link StandbyActivity} for the instant route. */
public class StandbyDream extends DreamService {
    private StandbyView display; private StandbyController controller;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow(); setInteractive(true); setFullscreen(true); setScreenBright(false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE; params.screenBrightness = Prefs.brightness(this);
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES; getWindow().setAttributes(params);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        display = new StandbyView(this); setContentView(display);
        controller = new StandbyController(this, getWindow(), display, this::finish, -1, false);
    }
    @Override public void onDreamingStarted() { super.onDreamingStarted(); if (controller != null) controller.start(); }
    @Override public void onDreamingStopped() { if (controller != null) controller.stop(); super.onDreamingStopped(); }
    @Override public void onDetachedFromWindow() { if (controller != null) controller.stop(); super.onDetachedFromWindow(); }
}
