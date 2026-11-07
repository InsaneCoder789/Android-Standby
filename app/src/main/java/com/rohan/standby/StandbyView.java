package com.rohan.standby;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.ExifInterface;
import android.service.notification.StatusBarNotification;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Renders the carousel: up to three frames side by side (swipe left or right, it wraps with no end) and, inside each frame, components stacked
 * top to bottom (swipe up or down, or tap). No menu is drawn; touch the screen and hold to bring up the brightness bar. The incoming frame follows the finger.
 */
public final class StandbyView extends View {
    private static final ColorMatrixColorFilter RED_NIGHT = new ColorMatrixColorFilter(new float[]{.32f,.32f,.32f,0,0, 0,0,0,0,0, 0,0,0,0,0, 0,0,0,1,0});
    private static final ColorMatrixColorFilter DIM_NIGHT = new ColorMatrixColorFilter(new ColorMatrix(new float[]{.4f,0,0,0,0, 0,.4f,0,0,0, 0,0,.4f,0,0, 0,0,0,1,0}));

    private final CallFrame callFrame;
    private final MusicFrame musicFrame = new MusicFrame();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG), layerPaint = new Paint();
    private boolean active, picker, night; private int frame, comp; private int[] frames; private int lockedFrame = -1; private float downX, downY;
    private Bitmap photo, photoPrev; private String loadedPhoto; private float photoFade = 1f; private ValueAnimator photoAnim; private volatile boolean photoLoading; private boolean photoFailed;
    private ValueAnimator settle, pickerAnimator; private float pickerAmount = 0f;
    // Drag state: axis 0 moves between frames, axis 1 between components. dragOff is in landscape pixels; negative means the content is moving left or up.
    private int dragAxis = -1; private float dragOff; private boolean ignoreDrag, moved, longPressed;
    private final Runnable dismissPicker = () -> showPicker(false);
    private final Runnable longPress = () -> { if (!this.moved && !this.musicSeek && this.dragAxis < 0 && this.brightnessControl != null) { longPressed = true; showPicker(true); } };

    /** Lets the host (screensaver or activity), which owns the window, apply brightness changes. */
    interface BrightnessControl { float get(); void set(float value); }
    private BrightnessControl brightnessControl; private boolean sliderDrag, musicSeek;
    private final AutoSwitch autoSwitch = new AutoSwitch(); private boolean internalSwitch;
    /** Test hook: force the burn-in drift (fractions of width and height) instead of the clock-based one. */
    float[] burnInOverride;
    void setBrightnessControl(BrightnessControl control) { brightnessControl = control; }

    StandbyView(Context c) {
        super(c); Fonts.init(c); Weather.refresh(c); callFrame = new CallFrame(c); setLayerType(View.LAYER_TYPE_NONE, null);
        frames = Prefs.frames(c); int saved = Prefs.i(c, "page", frames[0]); frame = Frames.contains(frames, saved) ? saved : frames[0]; comp = Prefs.component(c, frame);
    }
    private static int indexOf(int[] a, int v) { for (int i = 0; i < a.length; i++) if (a[i] == v) return i; return 0; }

    /** Re-read the chosen frames; call after settings change. */
    void reload() { finishSettle(); frames = Prefs.frames(getContext()); if (lockedFrame < 0 && !Frames.contains(frames, frame)) { frame = frames[0]; comp = Prefs.component(getContext(), frame); } comp = Math.min(comp, Frames.COMPONENTS[frame] - 1); loadedPhoto = null; photoFailed = false; invalidate(); }
    /** Settings preview: pin one frame and component regardless of the chosen three. -1 releases it. */
    void lock(int f, int component) { finishSettle(); lockedFrame = f; if (f >= 0) { frame = f; comp = Math.max(0, Math.min(component, Frames.COMPONENTS[f] - 1)); } else reload(); invalidate(); }
    /** Old single-face numbering, for the preview and audit tools. */
    void lockFace(int legacy) { if (legacy < 0) lock(-1, 0); else { int[] fc = Frames.fromLegacy(legacy); lock(fc[0], fc[1]); } }
    void setNight(boolean value) { if (night != value) { night = value; invalidate(); } }
    /** Settings previews: never show a ringing call or alarm, never drift for burn-in. */
    boolean previewMode;
    void setActive(boolean value) { active = value; if (!value) { picker = false; pickerAmount = 0; removeCallbacks(dismissPicker); removeCallbacks(longPress); } invalidate(); }

    private int[] navigable() { return lockedFrame >= 0 ? new int[]{lockedFrame} : frames; }

    private void showPicker(boolean show) {
        picker = show; removeCallbacks(dismissPicker); if (pickerAnimator != null) pickerAnimator.cancel();
        if (!ValueAnimator.areAnimatorsEnabled() || !Prefs.animate(getContext())) { pickerAmount = show ? 1 : 0; invalidate(); }
        else { pickerAnimator = ValueAnimator.ofFloat(pickerAmount, show ? 1 : 0); pickerAnimator.setDuration(200); pickerAnimator.setInterpolator(new DecelerateInterpolator(1.5f)); pickerAnimator.addUpdateListener(a -> { pickerAmount = (float) a.getAnimatedValue(); postInvalidateOnAnimation(); }); pickerAnimator.start(); }
        if (show) postDelayed(dismissPicker, 5000);
    }

    // ── moving between frames and components ──
    /** The frame and component that sit next to the current one: sign +1 is after (frame to the right, component below), -1 before. Null when there is nothing to move to. */
    private int[] neighbor(int axis, int sign) {
        if (axis == 0) { int[] list = navigable(); if (list.length < 2) return null; int nf = list[Frames.wrap(indexOf(list, frame) + sign, list.length)]; return new int[]{nf, Prefs.component(getContext(), nf)}; }
        int n = Frames.COMPONENTS[frame]; if (n < 2) return null; return new int[]{frame, Frames.wrap(comp + sign, n)};
    }
    private float extent(int axis, float w, float h) { return axis == 0 ? w : h; }
    /** Animates a move without a finger (tap on a component, auto switch). */
    private void slide(int axis, int sign) {
        finishSettle(); int[] nb = neighbor(axis, sign); if (nb == null) return; if (!internalSwitch) autoSwitch.userSwitched();
        if (!ValueAnimator.areAnimatorsEnabled() || !Prefs.animate(getContext())) { commit(nb); return; }
        dragAxis = axis; dragOff = 0; animateTo(-sign * extent(axis, logicalW(), logicalH()), nb);
    }
    private float logicalW() { return turn() == 0 ? getWidth() : getHeight(); }
    private float logicalH() { return turn() == 0 ? getHeight() : getWidth(); }
    private void animateTo(final float target, final int[] commitTo) {
        if (settle != null) settle.cancel(); final float from = dragOff; settle = ValueAnimator.ofFloat(from, target); settle.setDuration(commitTo != null ? 260 : 200); settle.setInterpolator(new DecelerateInterpolator(1.6f));
        settle.addUpdateListener(a -> { dragOff = (float) a.getAnimatedValue(); postInvalidateOnAnimation(); });
        settle.addListener(new android.animation.AnimatorListenerAdapter() { boolean cancelled; @Override public void onAnimationCancel(android.animation.Animator a) { cancelled = true; } @Override public void onAnimationEnd(android.animation.Animator a) { if (cancelled) return; settle = null; if (commitTo != null) commit(commitTo); else { dragAxis = -1; dragOff = 0; } invalidate(); } });
        settle.start();
    }
    /** Ends a running settle at once, landing where it was heading, so a new touch never fights an animation. */
    private void finishSettle() {
        if (settle != null) { float end = (Float) settle.getAnimatedValue(); ValueAnimator s = settle; settle = null; s.cancel(); if (Math.abs(end) > 0.5f && dragAxis >= 0) { int[] nb = neighbor(dragAxis, end < 0 ? 1 : -1); if (nb != null) commit(nb); } }
        dragAxis = -1; dragOff = 0;
    }
    private void commit(int[] nb) {
        frame = nb[0]; comp = nb[1]; dragAxis = -1; dragOff = 0;
        if (!internalSwitch && lockedFrame < 0) Prefs.sp(getContext()).edit().putInt("page", frame).putInt("comp" + frame, comp).apply();   // only YOUR choice is remembered; an automatic switch (e.g. to Music) must not become the frame Standby opens on next time
        invalidate();
    }
    /** Jumps to a frame by the shortest slide through the carousel (used by the automatic Music switch). */
    private void goTo(int target) {
        int[] list = navigable(); if (!Frames.contains(list, target) || target == frame) return; int from = indexOf(list, frame), to = indexOf(list, target), fwd = Frames.wrap(to - from, list.length);
        finishSettle(); int[] nb = {target, Prefs.component(getContext(), target)}; if (!ValueAnimator.areAnimatorsEnabled() || !Prefs.animate(getContext())) { commit(nb); return; }
        // The neighbour drawn during the slide is the one in that direction, so a non-adjacent target simply lands after the slide.
        dragAxis = 0; dragOff = 0; boolean forward = fwd <= list.length - fwd; animateTo((forward ? -1 : 1) * logicalW(), nb);
    }
    @Override protected void onDetachedFromWindow() {
        removeCallbacks(dismissPicker); removeCallbacks(longPress); if (settle != null) settle.cancel(); if (pickerAnimator != null) pickerAnimator.cancel();
        settle = null; dragAxis = -1; dragOff = 0; photo = null; photoPrev = null; if (photoAnim != null) photoAnim.cancel(); loadedPhoto = null; super.onDetachedFromWindow();
    }

    private void text(Canvas c, String value, float x, float y, float size, int color, boolean bold, Paint.Align align) { p.setColor(color); p.setTypeface(bold ? Fonts.UI_SEMI : Fonts.UI_MED); p.setTextSize(size); p.setTextAlign(align); p.setStyle(Paint.Style.FILL); c.drawText(value, x, y, p); }
    private String date(String pattern) { return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date()); }
    private String clockTime() { return date(Prefs.is24(getContext()) ? "HH:mm" : "h:mm"); }
    private ColorMatrixColorFilter nightFilter() { if (!night) return null; int s = Prefs.nightStyle(getContext()); return s == 0 ? RED_NIGHT : s == 1 ? DIM_NIGHT : null; }

    // ── orientation independence ──
    // The system decides the screensaver window's orientation and (with rotation lock on) it can be portrait even when the phone lies on its
    // side. Frames are designed for landscape, so in a portrait window the picture is rotated onto it, toward whichever side is down.
    private float gravityX = 9f;
    /** Feed raw gravity x; the sign says which way the phone is turned. Small values are ignored so it doesn't flip near flat. */
    void setGravityX(float x) { if (Math.abs(x) > 3f) { int before = turn(); gravityX = x; if (turn() != before) invalidate(); } }
    private int turn() { return ViewTurn.turn(getWidth(), getHeight(), gravityX); }
    /** Maps a touch point to the landscape space the frames are drawn in: {x, y, width, height}. */
    private float[] logical(float x, float y) { return ViewTurn.logical(turn(), x, y, getWidth(), getHeight()); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.drawColor(Color.BLACK); int turn = turn(); float pw = getWidth(), ph = getHeight();
        int saved = c.save(); if (turn == 90) { c.translate(pw, 0); c.rotate(90); } else if (turn == 270) { c.translate(0, ph); c.rotate(-90); }
        drawContent(c, turn == 0 ? pw : ph, turn == 0 ? ph : pw); c.restoreToCount(saved);
    }
    private void drawContent(Canvas c, float w, float h) {
        Context ctx = getContext(); ColorMatrixColorFilter filter = nightFilter(); p.setColorFilter(filter);
        if (!active) { postInvalidateDelayed(1000); return; }
        StatusBarNotification alert = !previewMode && Prefs.showAlerts(ctx) && MediaListener.instance != null ? MediaListener.instance.alert() : null;
        if (alert != null) { callFrame.draw(c, w, h, alert); postInvalidateDelayed(callFrame.animated() ? 33 : 500); return; }
        int layer = -1; if (filter != null) { layerPaint.setColorFilter(filter); layer = c.saveLayer(0, 0, w, h, layerPaint); }
        int shift = c.save();
        if (burnInOverride != null) c.translate(burnInOverride[0] * w, burnInOverride[1] * h); else if (!previewMode && Prefs.burnInShift(ctx)) { float[] o = BurnIn.offset(System.currentTimeMillis() / 60000, w, h); c.translate(o[0], o[1]); }
        ClockFrames.ringFade = 1f - Math.min(1f, pickerAmount * 1.6f);
        drawStack(c, w, h, ctx);
        c.restoreToCount(shift); if (layer >= 0) c.restoreToCount(layer);
        if (lockedFrame < 0) {
            int current = Frames.hasPlayer(frame) ? AutoSwitch.MUSIC : frame, target = Frames.contains(frames, Frames.MUSIC) ? Frames.MUSIC : Frames.contains(frames, Frames.CLOCK_MUSIC) ? Frames.CLOCK_MUSIC : -1;
            int to = autoSwitch.update(android.os.SystemClock.elapsedRealtime(), musicFrame.playingNow(ctx), current, target >= 0, Prefs.autoMusic(ctx), Prefs.autoReturn(ctx));
            if (to >= 0 && to != current && dragAxis < 0) { internalSwitch = true; goTo(to == AutoSwitch.MUSIC ? target : to); internalSwitch = false; }
        }
        if (pickerAmount > 0) drawPicker(c, w, h);
        boolean fast = dragAxis >= 0 || Frames.animated(frame, comp) || NewFrames.busy(frame) || (Frames.hasPlayer(frame) && !night && musicFrame.animating(ctx));
        postInvalidateDelayed(fast ? (Frames.animated(frame, comp) ? 40 : 33) : 1005 - System.currentTimeMillis() % 1000);
    }

    /** The current frame, and while a drag or slide is running its neighbour, offset along the drag axis so the two move together. */
    private void drawStack(Canvas c, float w, float h, Context ctx) {
        if (dragAxis < 0) { drawFrame(c, w, h, frame, comp, ctx); return; }
        float size = extent(dragAxis, w, h), off = Math.max(-size, Math.min(size, dragOff)); int sign = off < 0 ? 1 : -1; int[] nb = neighbor(dragAxis, sign);
        if (nb == null || off == 0) { drawFrame(c, w, h, frame, comp, ctx); return; }
        float ox = w * BurnIn.OVERSCAN, oy = h * BurnIn.OVERSCAN;
        for (int pass = 0; pass < 2; pass++) {
            float shiftBy = pass == 0 ? off : off + sign * size; int f = pass == 0 ? frame : nb[0], k = pass == 0 ? comp : nb[1];
            int s = c.save(); if (dragAxis == 0) c.translate(shiftBy, 0); else c.translate(0, shiftBy); c.clipRect(-ox, -oy, w + ox, h + oy); c.drawColor(Color.BLACK); drawFrame(c, w, h, f, k, ctx); c.restoreToCount(s);
        }
    }
    private void drawFrame(Canvas c, float w, float h, int f, int k, Context ctx) {
        switch (f) {
            case Frames.DIGITAL: if (k == 0) ClockFrames.drawDigital(c, w, h, ctx); else ClockFrames.drawColour(c, w, h, ctx); break;
            case Frames.DIAL: if (k == 0) ClockFrames.drawAnalog(c, w, h, ctx); else if (k == 1) ClockFrames.drawWidgets(c, w, h, ctx); else ClockFrames.drawCalendar(c, w, h, ctx); break;
            case Frames.MUSIC: musicFrame.draw(c, w, h, night, ctx); break;
            case Frames.CLOCK_MUSIC: musicFrame.drawClockMusic(c, w, h, k, night, ctx); break;
            case Frames.PHOTOS: drawPhoto(c, w, h); break;
            case Frames.WORLD: ClockFrames.drawWorld(c, w, h, ctx); break;
            default: NewFrames.draw(c, w, h, ctx, f, k);
        }
    }

    // ── photo slideshow ──
    private void loadPhoto(final String uri) {
        if (photoLoading) return; photoLoading = true; final Context app = getContext().getApplicationContext(); final int target = Math.max(getWidth(), 1280);
        new Thread(() -> {
            Bitmap result = null;
            try {
                BitmapFactory.Options o = new BitmapFactory.Options(); o.inJustDecodeBounds = true;
                try (InputStream in = app.getContentResolver().openInputStream(android.net.Uri.parse(uri))) { BitmapFactory.decodeStream(in, null, o); }
                int sample = 1; while (o.outWidth / (sample * 2) >= target && o.outHeight / (sample * 2) >= target) sample *= 2;
                o = new BitmapFactory.Options(); o.inSampleSize = sample;
                try (InputStream in = app.getContentResolver().openInputStream(android.net.Uri.parse(uri))) { result = BitmapFactory.decodeStream(in, null, o); }
                int rotation = 0; try (InputStream in = app.getContentResolver().openInputStream(android.net.Uri.parse(uri))) { int ori = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL); rotation = ori == ExifInterface.ORIENTATION_ROTATE_90 ? 90 : ori == ExifInterface.ORIENTATION_ROTATE_180 ? 180 : ori == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0; } catch (Exception ignored) {}
                if (result != null && rotation != 0) { Matrix m = new Matrix(); m.postRotate(rotation); Bitmap r = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), m, true); if (r != result) result.recycle(); result = r; }
            } catch (Exception | OutOfMemoryError ignored) { result = null; }
            final Bitmap done = result; post(() -> { acceptPhoto(done, uri); });
        }, "standby-photo").start();
    }
    /** Cross-fades from the previous picture to the new one. A failed load keeps the current picture. */
    private void acceptPhoto(Bitmap done, String uri) {
        photoLoading = false; loadedPhoto = uri;
        if (done == null) { photoFailed = photo == null; invalidate(); return; }
        photoFailed = false;
        if (photoAnim != null) photoAnim.cancel();
        Bitmap old = photo; photo = done; photoPrev = null;
        if (old != null && ValueAnimator.areAnimatorsEnabled() && Prefs.animate(getContext())) {
            photoPrev = old; photoFade = 0; photoAnim = ValueAnimator.ofFloat(0, 1); photoAnim.setDuration(700);
            photoAnim.addUpdateListener(a -> { photoFade = (float) a.getAnimatedValue(); if (photoFade >= 1f) photoPrev = null; postInvalidateOnAnimation(); }); photoAnim.start();
        } else photoFade = 1f;
        invalidate();
    }
    private void drawCover(Canvas c, Bitmap b, float w, float h, int alpha) {
        float scale = Math.max(w / b.getWidth(), h / b.getHeight()) * (1f + BurnIn.OVERSCAN * 2f), pw = b.getWidth() * scale, ph = b.getHeight() * scale; p.setColor(Color.WHITE); p.setAlpha(alpha); p.setFilterBitmap(true);
        c.drawBitmap(b, null, new RectF((w - pw) / 2, (h - ph) / 2, (w + pw) / 2, (h + ph) / 2), p); p.setAlpha(255);
    }
    private void drawPhoto(Canvas c, float w, float h) {
        Context ctx = getContext(); java.util.List<String> list = Prefs.photoUris(ctx);
        String uri = null; if (!list.isEmpty()) { long every = Prefs.photoIntervalMs(ctx); uri = list.get(list.size() == 1 ? 0 : (int) ((System.currentTimeMillis() / every) % list.size())); }
        if (uri != null && !uri.equals(loadedPhoto)) loadPhoto(uri);
        if (uri == null || photo == null) {
            p.setShader(new RadialGradient(w * .5f, h * .5f, w * .55f, 0xff1c1c1f, Color.BLACK, Shader.TileMode.CLAMP)); c.drawRect(-w * BurnIn.OVERSCAN, -h * BurnIn.OVERSCAN, w * (1 + BurnIn.OVERSCAN), h * (1 + BurnIn.OVERSCAN), p); p.setShader(null);
            float cx = w * .5f, cy = h * .42f, s = h * .11f; p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(h * .008f); p.setColor(0xff5a5a60); p.setStrokeJoin(Paint.Join.ROUND);
            c.drawRoundRect(cx - s * 1.4f, cy - s, cx + s * 1.4f, cy + s, s * .3f, s * .3f, p); c.drawCircle(cx + s * .7f, cy - s * .35f, s * .2f, p);
            Path hill = new Path(); hill.moveTo(cx - s * 1.3f, cy + s * .9f); hill.lineTo(cx - s * .4f, cy - s * .1f); hill.lineTo(cx + s * .2f, cy + s * .5f); hill.lineTo(cx + s * .6f, cy + s * .1f); hill.lineTo(cx + s * 1.3f, cy + s * .9f); c.drawPath(hill, p); p.setStyle(Paint.Style.FILL);
            text(c, uri == null ? "Add a photo" : photoFailed ? "Couldn't load that photo" : "Loading photo", w / 2, h * .67f, h * .065f, ClockFrames.INK, true, Paint.Align.CENTER);
            if (uri == null || photoFailed) text(c, uri == null ? "Open the Standby app and choose photos" : "Choose another one in the Standby app", w / 2, h * .76f, h * .04f, ClockFrames.MUTED, false, Paint.Align.CENTER);
            float pr = h * .05f; ClockFrames.chargeRing(c, p, ctx, w * .93f - pr * .45f, h * .115f, pr, h, true); p.setStyle(Paint.Style.FILL); return;
        }
        if (photoPrev != null && photoFade < 1f) { drawCover(c, photoPrev, w, h, 255); drawCover(c, photo, w, h, (int) (255 * photoFade)); } else drawCover(c, photo, w, h, 255);
        int dim = Prefs.photoDim(ctx); if (dim > 0) { p.setColor(Color.argb(dim * 255 / 100, 0, 0, 0)); c.drawRect(-w * BurnIn.OVERSCAN, -h * BurnIn.OVERSCAN, w * (1 + BurnIn.OVERSCAN), h * (1 + BurnIn.OVERSCAN), p); }
        float rr = h * .05f; ClockFrames.chargeRing(c, p, ctx, w * .93f - rr * .45f, h * .115f, rr, h, true); p.setStyle(Paint.Style.FILL);
        if (Prefs.photoClock(ctx)) {
            p.setShader(new LinearGradient(0, h * .55f, 0, h, 0x00000000, 0xcc000000, Shader.TileMode.CLAMP)); c.drawRect(-w * BurnIn.OVERSCAN, h * .55f, w * (1 + BurnIn.OVERSCAN), h * (1 + BurnIn.OVERSCAN), p); p.setShader(null);
            p.setShadowLayer(h * .03f, 0, 0, 0x99000000); ClockFrames.tabular(c, p, clockTime(), w * .06f, h * .765f, h * .20f, Color.WHITE, Fonts.UI_MED, w * .75f); ClockFrames.text(c, p, date("EEEE d MMMM").toUpperCase(Locale.getDefault()), w * .062f, h * .63f, h * .04f, 0xe6ffffff, Fonts.UI_SEMI, Paint.Align.LEFT, w * .84f); p.setShadowLayer(0, 0, 0, 0);
        }
    }

    // ── brightness (hold the screen) ──
    private void drawPicker(Canvas c, float w, float h) {
        // Dim the frame behind the bar, as video players do, so nothing underneath competes with it.
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(Color.argb((int) (150 * pickerAmount), 0, 0, 0)); c.drawRect(0, 0, w, h, p);
        int save = c.saveLayerAlpha(0, 0, w, h, (int) (255 * pickerAmount)); c.translate(0, (1 - pickerAmount) * h * .05f);
        if (brightnessControl != null) drawBrightnessSlider(c, w, h);
        c.restoreToCount(save);
    }
    private static final float BAR_X = .945f, BAR_TOP = .185f, BAR_BOTTOM = .675f;
    private boolean inSlider(float x, float y, float w, float h) { return Math.abs(x - w * BAR_X) <= h * .09f && y >= h * (BAR_TOP - .08f) && y <= h * (BAR_BOTTOM + .10f); }
    private void setSliderFromY(float y, float h) { float v = Math.max(.01f, Math.min(1f, (h * BAR_BOTTOM - y) / (h * (BAR_BOTTOM - BAR_TOP)))); brightnessControl.set(v); removeCallbacks(dismissPicker); postDelayed(dismissPicker, 5000); invalidate(); }
    private void drawBrightnessSlider(Canvas c, float w, float h) {
        float cx = w * BAR_X, top = h * BAR_TOP, bottom = h * BAR_BOTTOM, bw = h * .082f, v = Math.max(.01f, Math.min(1f, brightnessControl.get())), fillTop = bottom - (bottom - top) * v;
        RectF track = new RectF(cx - bw / 2, top, cx + bw / 2, bottom);
        p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(0xd91c1c1e); c.drawRoundRect(new RectF(track.left - h * .012f, track.top - h * .012f, track.right + h * .012f, track.bottom + h * .012f), bw / 2 + h * .012f, bw / 2 + h * .012f, p);
        c.save(); Path clip = new Path(); clip.addRoundRect(track, bw / 2, bw / 2, Path.Direction.CW); c.clipPath(clip); p.setColor(0x33ffffff); c.drawRect(track, p); p.setColor(0xfff5f5f7); c.drawRect(track.left, fillTop, track.right, track.bottom, p); c.restore();
        sun(c, cx, top - h * .062f, h * .022f, 1f);
        text(c, Math.round(v * 100) + "%", cx, bottom + h * .055f, h * .034f, 0xfff5f5f7, true, Paint.Align.CENTER);
    }
    private void sun(Canvas c, float cx, float cy, float rad, float alpha) {
        int col = Color.argb((int) (255 * alpha), 245, 245, 247); p.setStyle(Paint.Style.FILL); p.setColor(col); c.drawCircle(cx, cy, rad, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(rad * .30f); p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 8; i++) { double a = i * Math.PI / 4; c.drawLine(cx + (float) Math.cos(a) * rad * 1.5f, cy + (float) Math.sin(a) * rad * 1.5f, cx + (float) Math.cos(a) * rad * 2.0f, cy + (float) Math.sin(a) * rad * 2.0f, p); }
        p.setStyle(Paint.Style.FILL);
    }

    // ── touch ──
    @Override public boolean onTouchEvent(MotionEvent e) {
        if (!active) return true; Context ctx = getContext(); float[] pt = logical(e.getX(), e.getY()); final float ex = pt[0], ey = pt[1], lw = pt[2], lh = pt[3], slop = lh * .03f;
        StatusBarNotification alert = !previewMode && Prefs.showAlerts(ctx) && MediaListener.instance != null ? MediaListener.instance.alert() : null;
        if (alert != null) { if (e.getAction() == MotionEvent.ACTION_UP) callFrame.touch(ex, ey, alert); return true; }
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                finishSettle(); downX = ex; downY = ey; sliderDrag = false; moved = false; longPressed = false; ignoreDrag = false; removeCallbacks(longPress);
                musicSeek = Frames.hasPlayer(frame) && !picker && musicFrame.beginSeek(ex, ey); if (musicSeek) { invalidate(); return true; }
                sliderDrag = picker && brightnessControl != null && inSlider(ex, ey, lw, lh); if (sliderDrag) { setSliderFromY(ey, lh); return true; }
                if (!picker) postDelayed(longPress, 650); return true;
            case MotionEvent.ACTION_MOVE: {
                if (musicSeek) { musicFrame.moveSeek(ex); invalidate(); return true; } if (sliderDrag) { setSliderFromY(ey, lh); return true; }
                float dx = ex - downX, dy = ey - downY; if (!moved && Math.hypot(dx, dy) > slop) { moved = true; removeCallbacks(longPress); }
                if (picker || longPressed || ignoreDrag) return true;
                if (dragAxis < 0 && moved) { int axis = Math.abs(dx) >= Math.abs(dy) ? 0 : 1; if (neighbor(axis, 1) == null) ignoreDrag = true; else dragAxis = axis; }
                if (dragAxis >= 0) { float d = dragAxis == 0 ? dx : dy, size = extent(dragAxis, lw, lh); dragOff = Math.max(-size, Math.min(size, d)); invalidate(); }
                return true;
            }
            case MotionEvent.ACTION_CANCEL: removeCallbacks(longPress); sliderDrag = false; if (musicSeek) { musicSeek = false; musicFrame.endSeek(); }
                if (dragAxis >= 0 && settle == null) animateTo(0, null); return true;
            case MotionEvent.ACTION_UP: {
                removeCallbacks(longPress);
                if (musicSeek) { musicSeek = false; musicFrame.endSeek(); invalidate(); return true; }
                if (sliderDrag) { sliderDrag = false; return true; }
                if (longPressed) return true;
                if (dragAxis >= 0) {
                    float size = extent(dragAxis, lw, lh); int sign = dragOff < 0 ? 1 : -1; int[] nb = neighbor(dragAxis, sign);
                    if (nb != null && Math.abs(dragOff) > size * .20f) { if (!internalSwitch) autoSwitch.userSwitched(); animateTo(-sign * size, nb); } else animateTo(0, null); return true;
                }
                if (picker) { showPicker(false); return true; }
                if (moved) return true;
                if (Frames.hasPlayer(frame) && musicFrame.touch(ex, ey)) { invalidate(); return true; }
                if (frame == Frames.TIMER && NewFrames.timerTap(ctx, ex, ey)) { invalidate(); postDelayed(this::invalidate, 900); return true; }
                if (Frames.COMPONENTS[frame] > 1 && !(frame == Frames.TIMER && NewFrames.runningTimer() == null)) slide(1, 1);
                return true;
            }
            default: return true;
        }
    }
}
