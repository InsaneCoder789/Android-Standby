package com.rohan.standby;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Exact ink bounds of drawn text. {@code Paint.getTextBounds} over-reports by several pixels at large sizes (up to 9 px at 590 px in testing),
 * which silently throws off centring and alignment of big numerals. The glyph outline is exact to under a pixel at every size.
 * Bounds are relative to the draw origin (x, baseline), for a Paint whose alignment is LEFT.
 */
final class Ink {
    private static final Path SCRATCH = new Path();
    static synchronized void bounds(Paint p, String s, RectF out) { SCRATCH.rewind(); if (s.isEmpty()) { out.setEmpty(); return; } p.getTextPath(s, 0, s.length(), 0, 0, SCRATCH); SCRATCH.computeBounds(out, true); }
    static RectF bounds(Paint p, String s) { RectF r = new RectF(); bounds(p, s, r); return r; }
}
