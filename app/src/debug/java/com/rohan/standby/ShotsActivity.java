package com.rohan.standby;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;

/** Debug only. Renders every frame and component off-screen to PNGs in files/shots (pull with run-as). Extras: --ei w, --ei h, --ez night. Uses fake weather and a fixed place. */
public class ShotsActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); Fonts.init(this);
        getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().clear().putBoolean("burnIn", false).commit();
        getSharedPreferences("weather", MODE_PRIVATE).edit().clear().putString("city", "San Diego").putInt("temp", 70).putInt("hi", 74).putInt("lo", 59).putInt("code", 1).putLong("updated", System.currentTimeMillis()).putBoolean("f", true).putLong("lat", Double.doubleToLongBits(32.72)).putLong("lon", Double.doubleToLongBits(-117.16)).commit();
        final int w = getIntent().getIntExtra("w", 2340), h = getIntent().getIntExtra("h", 1080); final boolean night = getIntent().getBooleanExtra("night", false);
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            File dir = new File(getFilesDir(), "shots"); dir.mkdirs();
            for (int f = 0; f < Frames.COUNT; f++) for (int k = 0; k < Frames.COMPONENTS[f]; k++) {
                try {
                    StandbyView v = new StandbyView(this); v.setActive(true); v.setNight(night); v.lock(f, k);
                    v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)); v.layout(0, 0, w, h);
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); v.draw(new Canvas(bmp));
                    try (FileOutputStream out = new FileOutputStream(new File(dir, String.format("%02d_%d.png", f, k)))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, out); } bmp.recycle();
                } catch (Throwable t) { Log.e("SHOTS", "frame " + f + "/" + k + " failed", t); }
            }
            try {   // the call screen in its states, drawn from test data
                CallFrame cf = new CallFrame(this); long now = System.currentTimeMillis();
                for (int i = 0; i < 6; i++) {
                    CallFrame.Call k = new CallFrame.Call(); k.name = i == 3 ? "Unknown caller" : i == 4 ? "Dr. Priyadarshini Venkataraghavan-Subramaniam" : i == 5 ? "+91 98765 43210" : "Aarav Sharma"; k.number = i == 3 ? "" : "+91 98765 43210"; k.typeLabel = i == 3 ? null : "Mobile"; k.sub = "SIM 1  ·  Airtel";
                    k.state = i == 0 || i == 5 ? 0 : i == 4 ? 2 : i == 1 || i == 2 ? 1 : 3; if (i == 4) k.sub = "SIM 2  ·  Reliance Jio  ·  Call forwarding is on for this number"; k.since = now - 134_000; k.btAvailable = i != 1; k.speaker = i == 2; k.bluetooth = i == 2; k.btName = i == 2 ? "Pixel Buds" : null; k.muted = i == 2;
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); Canvas cv = new Canvas(bmp); cv.drawColor(0xff000000); cf.render(cv, w, h, k, null, null, null, true);
                    try (FileOutputStream out = new FileOutputStream(new File(dir, "call_" + i + ".png"))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, out); } bmp.recycle();
                }
                for (int i = 0; i < 2; i++) {   // voicemail, with and without a Call back button
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); Canvas cv = new Canvas(bmp); cv.drawColor(0xff000000);
                    cf.renderVoicemail(cv, w, h, now - 120_000, "New voicemail", i == 0 ? "Aarav Sharma  ·  0:42" : "+91 98765 43210", null, null, "Call back", i == 1);
                    try (FileOutputStream out = new FileOutputStream(new File(dir, "voicemail_" + i + ".png"))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, out); } bmp.recycle();
                }
                for (int th : new int[]{0, 3, 4}) {   // calls over the Morning, Night and Midnight scenes
                    CallFrame.themeOverride = th;
                    for (int st = 0; st < 3; st++) {
                        CallFrame.Call k = new CallFrame.Call(); k.name = "Aarav Sharma"; k.number = "+91 98765 43210"; k.typeLabel = "Mobile"; k.sub = "SIM 1  \u00b7  Airtel"; k.state = st == 0 ? 0 : st == 1 ? 1 : 2; k.since = now - 134_000; k.btAvailable = true; k.speaker = st == 1;
                        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); Canvas cv = new Canvas(bmp); cv.drawColor(0xff000000); cf.render(cv, w, h, k, null, null, null, true);
                        try (FileOutputStream out = new FileOutputStream(new File(dir, "scene_" + th + "_" + st + ".png"))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, out); } bmp.recycle();
                    }
                    Bitmap bv = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); Canvas cvv = new Canvas(bv); cvv.drawColor(0xff000000); cf.renderVoicemail(cvv, w, h, now - 120_000, "New voicemail", "Aarav Sharma  \u00b7  0:42", null, null, "Call back", true);
                    try (FileOutputStream out = new FileOutputStream(new File(dir, "scene_vm_" + th + ".png"))) { bv.compress(Bitmap.CompressFormat.PNG, 100, out); } bv.recycle();
                }
                CallFrame.themeOverride = -1;
                {   // the player with real-looking artwork: old look and redesign, full player and clock-and-music
                    Bitmap art = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888); Canvas ac = new Canvas(art); android.graphics.Paint ap = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                    ap.setShader(new android.graphics.LinearGradient(0, 0, 512, 512, 0xffd9395a, 0xff2a4bd7, android.graphics.Shader.TileMode.CLAMP)); ac.drawRect(0, 0, 512, 512, ap); ap.setShader(null); ap.setColor(0xffffc83d); ac.drawCircle(360, 150, 90, ap); ap.setColor(0xff1b1030); ac.drawCircle(150, 380, 170, ap);
                    String[] names = {"music_old", "music_new", "cm_old", "cm_new"};
                    for (int i = 0; i < 4; i++) {
                        MusicFrame mf = new MusicFrame(); mf.debugPlay("Midnight City (Official Video Remastered)", "M83", art, 243_000, 92_000, true);
                        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); Canvas cv = new Canvas(bmp); cv.drawColor(0xff000000);
                        if (i == 0) mf.draw(cv, w, h, false, this); else if (i == 1) mf.drawRedesign(cv, w, h, false, this); else if (i == 2) mf.drawClockMusic(cv, w, h, 0, false, this); else mf.drawClockMusicRedesign(cv, w, h, false, this);
                        try (FileOutputStream out = new FileOutputStream(new File(dir, names[i] + ".png"))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, out); } bmp.recycle();
                    }
                }
                String[] times = {"06:45", "17:30", "20:15", "22:50", "03:05"}, labels = {"Gym  ·  Weekdays", "Sunset walk", "Wind down", "Sleep", "Night shift"};
                for (int i = 0; i < 5; i++) {   // alarm: Morning, Sunset, Evening, Night, Midnight
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); Canvas cv = new Canvas(bmp); cv.drawColor(0xff000000);
                    cf.renderAlarm(cv, w, h, i, labels[i], times[i], "Wednesday  ·  12 Nov", "Dismiss", "Snooze  ·  10 min");
                    try (FileOutputStream out = new FileOutputStream(new File(dir, "alarm_" + i + ".png"))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, out); } bmp.recycle();
                }
                { Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); Canvas cv = new Canvas(bmp); cv.drawColor(0xff000000); cf.renderTimerDone(cv, w, h, "Pasta", "Stop");
                  try (FileOutputStream out = new FileOutputStream(new File(dir, "timerdone.png"))) { bmp.compress(Bitmap.CompressFormat.PNG, 100, out); } bmp.recycle(); }
            } catch (Throwable t) { Log.e("SHOTS", "call failed", t); }
            Log.i("SHOTS", "DONE"); finish();
        }, 1500);
    }
}
