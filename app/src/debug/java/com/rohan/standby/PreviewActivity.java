package com.rohan.standby;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Debug only. Shows one face full screen. Extras: --ei page N, --es p "key=value,key=value" (applies Prefs), --ez night true. Prefs are reset on each launch. */
public class PreviewActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); getWindow().getDecorView().setSystemUiVisibility(5894);
        if (getIntent().getBooleanExtra("wipe", false)) { getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit().clear().commit(); getSharedPreferences("weather", MODE_PRIVATE).edit().clear().commit(); finish(); return; } // --ez wipe true: back to factory defaults
        SharedPreferences.Editor e = getSharedPreferences(Prefs.FILE, MODE_PRIVATE).edit(); if (getIntent().getBooleanExtra("reset", true)) e.clear(); e.putInt("page", getIntent().getIntExtra("page", 0)).putBoolean("burnIn", false);
        String kv = getIntent().getStringExtra("p");
        if (kv != null) for (String s : kv.split(",")) { String[] a = s.split("=", 2); if (a.length < 2) continue; String val = a[1]; if (val.equals("true") || val.equals("false")) e.putBoolean(a[0], Boolean.parseBoolean(val)); else if (val.matches("-?\\d+")) e.putInt(a[0], Integer.parseInt(val)); else e.putString(a[0], val); }
        e.apply();
        getSharedPreferences("weather", MODE_PRIVATE).edit().putString("city", "San Diego").putInt("temp", 70).putInt("hi", 70).putInt("lo", 59).putInt("code", 1).putLong("updated", System.currentTimeMillis()).putBoolean("f", true).putLong("lat", Double.doubleToLongBits(32.72)).putLong("lon", Double.doubleToLongBits(-117.16)).apply();
        StandbyView v = new StandbyView(this); v.setBrightnessControl(new StandbyView.BrightnessControl() { float cur = .55f; public float get() { return cur; } public void set(float x) { cur = x; } }); if (getIntent().hasExtra("frame")) v.lock(getIntent().getIntExtra("frame", 0), getIntent().getIntExtra("comp", 0)); v.setActive(true); v.setNight(getIntent().getBooleanExtra("night", false)); setContentView(v);
    }
}
