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
            Log.i("SHOTS", "DONE"); finish();
        }, 1500);
    }
}
