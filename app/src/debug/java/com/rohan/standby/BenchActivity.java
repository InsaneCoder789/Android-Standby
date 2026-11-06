package com.rohan.standby;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.RenderNode;
import android.os.Bundle;
import android.util.Log;

/** Debug only. Records every face into a RenderNode (pure UI-thread cost, independent of the GPU) and logs timings (tag BENCH). Restores the user's settings afterwards. */
public class BenchActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); final java.util.Map<String, ?> saved = new java.util.HashMap<>(getSharedPreferences(Prefs.FILE, MODE_PRIVATE).getAll());
        SharedPreferences.Editor e = getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().clear().putBoolean("burnIn", false).putBoolean("autoMusic", false); for (int f = 0; f < Prefs.FACES; f++) e.putBoolean("face" + f, true); e.commit();
        final StandbyView v = new StandbyView(this); v.setActive(true); setContentView(v);
        v.postDelayed(() -> {
            int w = v.getWidth(), h = v.getHeight(); RenderNode node = new RenderNode("bench"); node.setPosition(0, 0, w, h);
            for (int pass = 0; pass < 2; pass++) { v.setNight(pass == 1);
                for (int fr = 0; fr < Frames.COUNT; fr++) for (int k = 0; k < Frames.COMPONENTS[fr]; k++) { v.lock(fr, k);
                    for (int i = 0; i < 40; i++) { Canvas c = node.beginRecording(w, h); v.draw(c); node.endRecording(); }
                    long[] t = new long[300]; for (int i = 0; i < 300; i++) { long s = System.nanoTime(); Canvas c = node.beginRecording(w, h); v.draw(c); node.endRecording(); t[i] = System.nanoTime() - s; }
                    java.util.Arrays.sort(t); long sum = 0; for (long x : t) sum += x;
                    Log.i("BENCH", String.format("%s %-18s %dx%d  avg=%.2fms p95=%.2fms max=%.2fms", pass == 1 ? "night" : "day  ", Frames.NAMES[fr] + "/" + k, w, h, sum / 300 / 1e6, t[284] / 1e6, t[299] / 1e6)); } }
            SharedPreferences.Editor r = getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().clear();
            for (java.util.Map.Entry<String, ?> en : saved.entrySet()) { Object x = en.getValue(); if (x instanceof Boolean) r.putBoolean(en.getKey(), (Boolean) x); else if (x instanceof Integer) r.putInt(en.getKey(), (Integer) x); else if (x instanceof Long) r.putLong(en.getKey(), (Long) x); else if (x instanceof String) r.putString(en.getKey(), (String) x); }
            r.commit(); Log.i("BENCH", "DONE"); finish();
        }, 2500);
    }
}
