package com.rohan.standby;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;

/** Bundled open fonts (SIL OFL): Nunito ~ SF Rounded, Barlow Condensed ~ SF Condensed, Inter ~ SF Pro. */
final class Fonts {
    static Typeface ROUNDED_BLACK, ROUNDED_BOLD, CONDENSED, UI_REG, UI_MED, UI_SEMI, UI_BOLD;
    static synchronized void init(Context c) {
        if (ROUNDED_BLACK != null) return;
        AssetManager a = c.getAssets();
        ROUNDED_BLACK = variable(a, "fonts/Nunito.ttf", "'wght' 1000");
        ROUNDED_BOLD = variable(a, "fonts/Nunito.ttf", "'wght' 800");
        UI_REG = variable(a, "fonts/Inter.ttf", "'wght' 400");
        UI_MED = variable(a, "fonts/Inter.ttf", "'wght' 500");
        UI_SEMI = variable(a, "fonts/Inter.ttf", "'wght' 600");
        UI_BOLD = variable(a, "fonts/Inter.ttf", "'wght' 700");
        Typeface cond;
        try { cond = Typeface.createFromAsset(a, "fonts/BarlowCondensed-ExtraBold.ttf"); } catch (RuntimeException e) { cond = Typeface.create("sans-serif-condensed", Typeface.BOLD); }
        CONDENSED = cond;
    }
    private static Typeface variable(AssetManager a, String path, String axes) {
        try { Typeface t = new Typeface.Builder(a, path).setFontVariationSettings(axes).build(); if (t != null) return t; } catch (RuntimeException ignored) {}
        return Typeface.create("sans-serif", Typeface.BOLD);
    }
}
