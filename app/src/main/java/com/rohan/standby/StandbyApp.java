package com.rohan.standby;

import android.app.Application;

/** Loads fonts off the main thread as soon as the process starts, so the first frame never waits for them. */
public class StandbyApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        final Application app = this;
        Thread t = new Thread(() -> Fonts.init(app), "standby-fonts"); t.setPriority(Thread.MAX_PRIORITY); t.start();
        Instant.sync(this); // keep the Instant mode watcher running whenever the app process starts
    }
}
