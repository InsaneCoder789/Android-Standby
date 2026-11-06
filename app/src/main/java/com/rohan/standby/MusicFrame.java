package com.rohan.standby;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;

/**
 * Now Bar style music. The full player (Music face): gradient from the album art, big rounded cover, title and artist, a wavy seek bar,
 * prev / play-pause / next. It is the one place music lives: Standby switches to it by itself when music starts (see AutoSwitch).
 * Geometry is shared by drawing and hit testing, so controls never drift.
 */
final class MusicFrame {
    private static final class Snap { boolean has, hasMeta; String title, artist, app; Bitmap art; long duration, position, stamp, actions; int state = PlaybackState.STATE_NONE; float speed = 1f; }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final RectF art = new RectF(), previous = new RectF(), toggle = new RectF(), next = new RectF(), seek = new RectF(), seekTrack = new RectF();
    private Snap snap = new Snap(); private long lastPoll; private MediaController controller; private Bitmap colorsFor; private int topColor = NowBar.FALLBACK_TOP, bottomColor = NowBar.FALLBACK_BOTTOM;
    private boolean seeking; private float seekFraction; private String appLabelFor, appLabel;

    // ── session snapshot: binder calls are slow, so refresh a few times a second and extrapolate the position in between ──
    private void poll(Context ctx) {
        long now = SystemClock.elapsedRealtime(); if (now - lastPoll < 450) return; lastPoll = now;
        MediaListener listener = MediaListener.instance; controller = listener == null ? null : listener.controller(); Snap s = new Snap();
        if (controller != null) {
            MediaMetadata m = controller.getMetadata(); PlaybackState st = controller.getPlaybackState();
            s.hasMeta = m != null; s.has = m != null || st != null; s.title = MediaListener.title(controller); s.artist = MediaListener.artist(controller); s.art = MediaListener.art(controller);
            s.duration = m == null ? 0 : m.getLong(MediaMetadata.METADATA_KEY_DURATION);
            if (st != null) { s.state = st.getState(); s.position = st.getPosition(); s.stamp = st.getLastPositionUpdateTime(); s.speed = st.getPlaybackSpeed(); s.actions = st.getActions(); }
            String pkg = controller.getPackageName(); if (pkg != null && !pkg.equals(appLabelFor)) { appLabelFor = pkg; appLabel = label(ctx, pkg); } s.app = appLabel;
        }
        snap = s; if (s.art != colorsFor) { colorsFor = s.art; recolor(s.art); }
    }
    private static String label(Context c, String pkg) { try { PackageManager pm = c.getPackageManager(); return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); } catch (Exception e) { return null; } }
    private void recolor(Bitmap b) {
        if (b == null || b.isRecycled()) { topColor = NowBar.FALLBACK_TOP; bottomColor = NowBar.FALLBACK_BOTTOM; return; }
        try { Bitmap small = Bitmap.createScaledBitmap(b, 32, 32, true); int[] px = new int[32 * 32]; small.getPixels(px, 0, 32, 0, 0, 32, 32); int[] c = NowBar.pickColors(px, 32, 32); topColor = c[0]; bottomColor = c[1]; if (small != b) small.recycle(); }
        catch (RuntimeException e) { topColor = NowBar.FALLBACK_TOP; bottomColor = NowBar.FALLBACK_BOTTOM; }
    }
    private long position(long now) {
        long pos = snap.position; if (snap.state == PlaybackState.STATE_PLAYING && snap.stamp > 0) pos += (long) ((now - snap.stamp) * snap.speed);
        return Math.max(0, snap.duration > 0 ? Math.min(snap.duration, pos) : pos);
    }
    boolean playing() { return snap.state == PlaybackState.STATE_PLAYING; }
    /** Music is on: playing or buffering a track that has a title. */
    boolean playingNow(Context ctx) { poll(ctx); return snap.hasMeta && snap.title != null && (snap.state == PlaybackState.STATE_PLAYING || snap.state == PlaybackState.STATE_BUFFERING); }
    /** True while the wave is moving, so the view redraws at animation rate. */
    boolean animating(Context ctx) { return playing() && Prefs.musicWave(ctx) && android.animation.ValueAnimator.areAnimatorsEnabled() && Prefs.animate(ctx); }

    // ───────────────────────────── full player ─────────────────────────────
    void draw(Canvas c, float w, float h, boolean night, Context ctx) {
        poll(ctx); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); paint.setStyle(Paint.Style.FILL); boolean idle = !snap.has || snap.title == null && snap.state == PlaybackState.STATE_NONE;
        // Background: the album's own colours, darkening toward the bottom.
        if (Prefs.musicBackground(ctx) == 0) {
            int top = idle ? NowBar.FALLBACK_TOP : topColor, bottom = idle ? NowBar.FALLBACK_BOTTOM : bottomColor;
            float ox = w * BurnIn.OVERSCAN, oy = h * BurnIn.OVERSCAN; paint.setShader(new LinearGradient(w * .15f, 0, w * .55f, h, top, bottom, Shader.TileMode.CLAMP)); c.drawRect(-ox, -oy, w + ox, h + oy, paint);
            paint.setShader(new RadialGradient(w * .26f, h * .5f, h * .95f, 0x26ffffff, 0x00ffffff, Shader.TileMode.CLAMP)); c.drawRect(-ox, -oy, w + ox, h + oy, paint); paint.setShader(null);
        } else { c.drawColor(Color.BLACK); }
        float size = Math.min(h * .74f, w * .355f), artX = w * .075f, artY = (h - size) / 2f, rad = size * .075f; art.set(artX, artY, artX + size, artY + size);
        paint.setColor(0xff1c1c1e); paint.setShadowLayer(h * .05f, 0, h * .018f, 0x88000000); c.drawRoundRect(art, rad, rad, paint); paint.setShadowLayer(0, 0, 0, 0);
        if (snap.art != null && !snap.art.isRecycled()) {
            c.save(); Path clip = new Path(); clip.addRoundRect(art, rad, rad, Path.Direction.CW); c.clipPath(clip); float crop = Math.min(snap.art.getWidth(), snap.art.getHeight());
            Rect src = new Rect((int) ((snap.art.getWidth() - crop) / 2), (int) ((snap.art.getHeight() - crop) / 2), (int) ((snap.art.getWidth() + crop) / 2), (int) ((snap.art.getHeight() + crop) / 2)); paint.setColor(-1); c.drawBitmap(snap.art, src, art, paint); c.restore();
        } else { paint.setShader(new LinearGradient(art.left, art.top, art.right, art.bottom, 0xff3a3a40, 0xff1c1c1e, Shader.TileMode.CLAMP)); c.drawRoundRect(art, rad, rad, paint); paint.setShader(null); note(c, art.centerX(), art.centerY(), size * .26f, 0x886e6e76); }

        float left = art.right + w * .07f, right = w - w * .075f, panelW = right - left;
        float ringR = h * .05f; ClockFrames.chargeRing(c, paint, ctx, right - ringR * 1.45f, h * .115f, ringR, h, true); paint.setStyle(Paint.Style.FILL);
        if (idle) {
            ClockFrames.text(c, paint, "Nothing playing", left, h * .43f, h * .075f, 0xffffffff, Fonts.UI_BOLD, Paint.Align.LEFT, panelW);
            ClockFrames.text(c, paint, "Start music on your phone and it shows up here", left, h * .53f, h * .045f, 0xb3ffffff, Fonts.UI_MED, Paint.Align.LEFT, panelW);
            toggle.setEmpty(); previous.setEmpty(); next.setEmpty(); seek.setEmpty(); return;
        }
        // Title, artist, source chip.
        paint.setTypeface(Fonts.UI_BOLD); paint.setTextSize(h * .072f); String title = TextUtils.ellipsize(snap.title == null ? "Unknown track" : snap.title, new TextPaint(paint), panelW, TextUtils.TruncateAt.END).toString();
        ClockFrames.text(c, paint, title, left, h * .275f, h * .072f, 0xffffffff, Fonts.UI_BOLD, Paint.Align.LEFT, 0);
        paint.setTypeface(Fonts.UI_MED); paint.setTextSize(h * .05f); String artist = TextUtils.ellipsize(snap.artist == null ? "" : snap.artist, new TextPaint(paint), panelW, TextUtils.TruncateAt.END).toString();
        ClockFrames.text(c, paint, artist, left, h * .362f, h * .05f, 0xb8ffffff, Fonts.UI_MED, Paint.Align.LEFT, 0);
        if (snap.app != null) chip(c, left, h * .455f, snap.app, h, panelW);
        // Wavy seek bar.
        long now = SystemClock.elapsedRealtime(), pos = position(now); float frac = seeking ? seekFraction : NowBar.fraction(pos, snap.duration);
        float sy = h * .585f, sl = left, sr = right; seekTrack.set(sl, sy, sr, sy); seek.set(sl - h * .02f, sy - h * .05f, sr + h * .02f, sy + h * .05f);
        float amp = playing() && !night ? h * .0105f : 0f, wl = h * .075f, phase = animating(ctx) ? (float) (-(now % 2200) / 2200.0 * 2 * Math.PI) : 0f, kx = sl + (sr - sl) * frac, stroke = h * .0095f;
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(stroke); paint.setColor(0x59ffffff); c.drawLine(kx, sy, sr, sy, paint);
        paint.setColor(0xffffffff); Path wave = new Path(); wave.moveTo(sl, sy + NowBar.waveY(0, wl, amp, phase)); for (float x = sl + 3; x < kx; x += 3) wave.lineTo(x, sy + NowBar.waveY(x - sl, wl, amp, phase)); wave.lineTo(kx, sy + NowBar.waveY(kx - sl, wl, amp, phase)); c.drawPath(wave, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xffffffff); c.drawCircle(kx, sy + NowBar.waveY(kx - sl, wl, amp, phase), h * (seeking ? .021f : .0165f), paint);
        ClockFrames.text(c, paint, NowBar.clock(seeking ? (long) (seekFraction * snap.duration) : pos), sl, h * .665f, h * .036f, 0xccffffff, Fonts.UI_MED, Paint.Align.LEFT, 0);
        if (snap.duration > 0) ClockFrames.text(c, paint, NowBar.clock(snap.duration), sr, h * .665f, h * .036f, 0xccffffff, Fonts.UI_MED, Paint.Align.RIGHT, 0);
        // Controls: prev, play/pause (larger), next.
        float cy = h * .795f, cx = (left + right) / 2f, step = Math.min(panelW * .27f, h * .24f), big = h * .078f, small = h * .062f;
        toggle.set(cx - big, cy - big, cx + big, cy + big); previous.set(cx - step - small, cy - small, cx - step + small, cy + small); next.set(cx + step - small, cy - small, cx + step + small, cy + small);
        paint.setStyle(Paint.Style.FILL); paint.setColor(0x29ffffff); c.drawCircle(cx, cy, big, paint); paint.setColor(0xffffffff);
        if (playing()) pauseGlyph(c, cx, cy, h * .044f); else playGlyph(c, cx + h * .004f, cy, h * .046f);
        skipGlyph(c, previous.centerX(), cy, h * .036f, true); skipGlyph(c, next.centerX(), cy, h * .036f, false);
    }
    // ───────────────────────────── Clock and Music: the player as a card beside a clock ─────────────────────────────
    /** comp 0: large digital time; comp 1: dial. The same player card either way. */
    void drawClockMusic(Canvas c, float w, float h, int comp, boolean night, Context ctx) {
        poll(ctx); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND); paint.setStyle(Paint.Style.FILL); paint.setShader(null);
        if (comp == 0) {
            int accent = ClockFrames.DIGITAL_COLORS[Prefs.digitalColor(ctx)]; boolean h24 = Prefs.is24(ctx); String t = new java.text.SimpleDateFormat(h24 ? "HH:mm" : "h:mm", java.util.Locale.getDefault()).format(new java.util.Date());
            ClockFrames.text(c, paint, t, w * .05f, h * .47f, h * .62f, accent, Fonts.CONDENSED, Paint.Align.LEFT, w * .41f);
            ClockFrames.text(c, paint, new java.text.SimpleDateFormat("EEE d MMM", java.util.Locale.getDefault()).format(new java.util.Date()).toUpperCase(java.util.Locale.getDefault()), w * .05f, h * .80f, h * .06f, 0xffd1d1d6, Fonts.UI_SEMI, Paint.Align.LEFT, w * .4f);
        } else ClockFrames.dial(c, paint, w * .235f, h * .5f, Math.min(h * .43f, w * .185f), Prefs.dialSeconds(ctx), Prefs.dialNumerals(ctx));
        card(c, new RectF(w * .50f, h * .16f, w * .955f, h * .90f), night, ctx);
        float r = h * .05f; ClockFrames.chargeRing(c, paint, ctx, w * .93f - r * .45f, h * .095f, r * .8f, h, false); paint.setStyle(Paint.Style.FILL);
    }
    private void card(Canvas c, RectF a, boolean night, Context ctx) {
        boolean idle = !snap.has || snap.title == null && snap.state == PlaybackState.STATE_NONE; float H = a.height(), pad = H * .07f, rad = H * .09f;
        int top = idle || Prefs.musicBackground(ctx) == 1 ? 0xff26272d : topColor, bottom = idle || Prefs.musicBackground(ctx) == 1 ? 0xff15161a : bottomColor;
        paint.setShader(new LinearGradient(a.left, a.top, a.right, a.bottom, top, bottom, Shader.TileMode.CLAMP)); c.drawRoundRect(a, rad, rad, paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(H * .004f); paint.setColor(0x22ffffff); c.drawRoundRect(a, rad, rad, paint); paint.setStyle(Paint.Style.FILL);
        if (idle) { note(c, a.centerX(), a.centerY() - H * .10f, H * .16f, 0x886e6e76); ClockFrames.text(c, paint, "Nothing playing", a.centerX(), a.centerY() + H * .14f, H * .07f, 0xffffffff, Fonts.UI_BOLD, Paint.Align.CENTER, a.width() * .8f); ClockFrames.text(c, paint, "Start music and it appears here", a.centerX(), a.centerY() + H * .24f, H * .045f, 0xb3ffffff, Fonts.UI_MED, Paint.Align.CENTER, a.width() * .86f); toggle.setEmpty(); previous.setEmpty(); next.setEmpty(); seek.setEmpty(); return; }
        float size = H * .38f, cr = size * .12f; art.set(a.left + pad, a.top + pad, a.left + pad + size, a.top + pad + size);
        paint.setColor(0xff1c1c1e); c.drawRoundRect(art, cr, cr, paint);
        if (snap.art != null && !snap.art.isRecycled()) { c.save(); Path clip = new Path(); clip.addRoundRect(art, cr, cr, Path.Direction.CW); c.clipPath(clip); float crop = Math.min(snap.art.getWidth(), snap.art.getHeight()); Rect src = new Rect((int) ((snap.art.getWidth() - crop) / 2), (int) ((snap.art.getHeight() - crop) / 2), (int) ((snap.art.getWidth() + crop) / 2), (int) ((snap.art.getHeight() + crop) / 2)); paint.setColor(-1); c.drawBitmap(snap.art, src, art, paint); c.restore(); }
        else { paint.setShader(new LinearGradient(art.left, art.top, art.right, art.bottom, 0xff3a3a40, 0xff1c1c1e, Shader.TileMode.CLAMP)); c.drawRoundRect(art, cr, cr, paint); paint.setShader(null); note(c, art.centerX(), art.centerY(), size * .26f, 0x886e6e76); }
        float left = art.right + pad, right = a.right - pad, textW = right - left;
        paint.setTypeface(Fonts.UI_BOLD); paint.setTextSize(H * .072f); String title = TextUtils.ellipsize(snap.title == null ? "Unknown track" : snap.title, new TextPaint(paint), textW, TextUtils.TruncateAt.END).toString();
        ClockFrames.text(c, paint, title, left, art.top + size * .30f, H * .072f, 0xffffffff, Fonts.UI_BOLD, Paint.Align.LEFT, 0);
        paint.setTypeface(Fonts.UI_MED); paint.setTextSize(H * .052f); String artist = TextUtils.ellipsize(snap.artist == null ? "" : snap.artist, new TextPaint(paint), textW, TextUtils.TruncateAt.END).toString();
        ClockFrames.text(c, paint, artist, left, art.top + size * .60f, H * .052f, 0xb8ffffff, Fonts.UI_MED, Paint.Align.LEFT, 0);
        if (snap.app != null) ClockFrames.text(c, paint, snap.app, left, art.top + size * .90f, H * .04f, 0x99ffffff, Fonts.UI_SEMI, Paint.Align.LEFT, textW);
        long now = SystemClock.elapsedRealtime(), pos = position(now); float frac = seeking ? seekFraction : NowBar.fraction(pos, snap.duration);
        float sl = a.left + pad, sr = a.right - pad, sy = a.top + H * .62f; seekTrack.set(sl, sy, sr, sy); seek.set(sl - H * .02f, sy - H * .05f, sr + H * .02f, sy + H * .05f);
        float amp = playing() && !night ? H * .0105f : 0f, wl = H * .075f, phase = animating(ctx) ? (float) (-(now % 2200) / 2200.0 * 2 * Math.PI) : 0f, kx = sl + (sr - sl) * frac;
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(H * .0095f); paint.setColor(0x59ffffff); c.drawLine(kx, sy, sr, sy, paint);
        paint.setColor(0xffffffff); Path wave = new Path(); wave.moveTo(sl, sy + NowBar.waveY(0, wl, amp, phase)); for (float x = sl + 3; x < kx; x += 3) wave.lineTo(x, sy + NowBar.waveY(x - sl, wl, amp, phase)); wave.lineTo(kx, sy + NowBar.waveY(kx - sl, wl, amp, phase)); c.drawPath(wave, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xffffffff); c.drawCircle(kx, sy + NowBar.waveY(kx - sl, wl, amp, phase), H * (seeking ? .021f : .0165f), paint);
        ClockFrames.text(c, paint, NowBar.clock(seeking ? (long) (seekFraction * snap.duration) : pos), sl, sy + H * .075f, H * .038f, 0xccffffff, Fonts.UI_MED, Paint.Align.LEFT, 0);
        if (snap.duration > 0) ClockFrames.text(c, paint, NowBar.clock(snap.duration), sr, sy + H * .075f, H * .038f, 0xccffffff, Fonts.UI_MED, Paint.Align.RIGHT, 0);
        float cy = a.top + H * .875f, cx = (sl + sr) / 2f, step = Math.min((sr - sl) * .27f, H * .30f), big = H * .078f, small = H * .06f;
        toggle.set(cx - big, cy - big, cx + big, cy + big); previous.set(cx - step - small, cy - small, cx - step + small, cy + small); next.set(cx + step - small, cy - small, cx + step + small, cy + small);
        paint.setStyle(Paint.Style.FILL); paint.setColor(0x29ffffff); c.drawCircle(cx, cy, big, paint); paint.setColor(0xffffffff);
        if (playing()) pauseGlyph(c, cx, cy, H * .042f); else playGlyph(c, cx + H * .004f, cy, H * .044f);
        skipGlyph(c, previous.centerX(), cy, H * .034f, true); skipGlyph(c, next.centerX(), cy, H * .034f, false);
    }
    private void chip(Canvas c, float x, float cy, String text, float h, float available) {
        paint.setTypeface(Fonts.UI_SEMI); paint.setTextSize(h * .034f); float pad = h * .024f, dot = h * .0085f, tw = Math.min(paint.measureText(text), Math.max(0f, Math.min(h * 1.1f, available - pad * 2.6f - dot * 2))), ch = h * .062f, cw = pad + dot * 2 + pad * .6f + tw + pad;
        text = TextUtils.ellipsize(text, new TextPaint(paint), tw, TextUtils.TruncateAt.END).toString();
        paint.setStyle(Paint.Style.FILL); paint.setColor(0x38000000); c.drawRoundRect(x, cy - ch / 2, x + cw, cy + ch / 2, ch / 2, ch / 2, paint);
        paint.setColor(0xe6ffffff); c.drawCircle(x + pad + dot, cy, dot, paint); ClockFrames.text(c, paint, text, x + pad + dot * 2 + pad * .6f, cy, h * .034f, 0xe6ffffff, Fonts.UI_SEMI, Paint.Align.LEFT, tw);
    }

    // ───────────────────────────── touch ─────────────────────────────
    boolean touch(float x, float y) {
        if (controller == null) return false; MediaController.TransportControls t = controller.getTransportControls(); PlaybackState st = controller.getPlaybackState(); long a = st == null ? 0 : st.getActions();
        if (toggle.contains(x, y)) { if (playing()) t.pause(); else t.play(); lastPoll = 0; return true; }
        if (previous.contains(x, y)) { if ((a & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0) t.skipToPrevious(); lastPoll = 0; return true; }
        if (next.contains(x, y)) { if ((a & PlaybackState.ACTION_SKIP_TO_NEXT) != 0) t.skipToNext(); lastPoll = 0; return true; }
        return false;
    }
    /** Seek bar: press on it to begin, drag to scrub, release to jump. Only when the app allows seeking. */
    boolean beginSeek(float x, float y) { if (seek.isEmpty() || !seek.contains(x, y) || snap.duration <= 0 || (snap.actions & PlaybackState.ACTION_SEEK_TO) == 0) return false; seeking = true; moveSeek(x); return true; }
    void moveSeek(float x) { float track = seekTrack.width(); if (track <= 0f) return; seekFraction = Math.max(0f, Math.min(1f, (x - seekTrack.left) / track)); }
    void endSeek() { if (seeking && controller != null) controller.getTransportControls().seekTo((long) (seekFraction * snap.duration)); seeking = false; lastPoll = 0; }

    // ───────────────────────────── glyphs ─────────────────────────────
    private void playGlyph(Canvas c, float x, float y, float r) { paint.setStyle(Paint.Style.FILL_AND_STROKE); paint.setStrokeWidth(r * .28f); Path p = new Path(); p.moveTo(x - r * .55f, y - r * .75f); p.lineTo(x + r * .8f, y); p.lineTo(x - r * .55f, y + r * .75f); p.close(); c.drawPath(p, paint); paint.setStyle(Paint.Style.FILL); paint.setStrokeWidth(0); }
    private void pauseGlyph(Canvas c, float x, float y, float r) { float bw = r * .34f, bh = r * 1.5f; paint.setStyle(Paint.Style.FILL); c.drawRoundRect(x - r * .62f, y - bh / 2, x - r * .62f + bw, y + bh / 2, bw * .4f, bw * .4f, paint); c.drawRoundRect(x + r * .62f - bw, y - bh / 2, x + r * .62f, y + bh / 2, bw * .4f, bw * .4f, paint); }
    private void skipGlyph(Canvas c, float x, float y, float r, boolean left) {
        float s = left ? -1f : 1f; paint.setStyle(Paint.Style.FILL_AND_STROKE); paint.setStrokeWidth(r * .26f); Path p = new Path(); p.moveTo(x - s * r * .75f, y - r * .72f); p.lineTo(x + s * r * .42f, y); p.lineTo(x - s * r * .75f, y + r * .72f); p.close(); c.drawPath(p, paint);
        paint.setStyle(Paint.Style.FILL); paint.setStrokeWidth(0); c.drawRoundRect(x + s * r * .56f - r * .13f, y - r * .72f, x + s * r * .56f + r * .13f, y + r * .72f, r * .1f, r * .1f, paint);
    }
    private void note(Canvas c, float x, float y, float s, int color) {
        paint.setColor(color); paint.setStyle(Paint.Style.FILL); c.drawOval(new RectF(x - s * .62f, y + s * .25f, x - s * .02f, y + s * .65f), paint); c.drawRect(x - s * .08f, y - s * .7f, x + s * .08f, y + s * .45f, paint);
        Path f = new Path(); f.moveTo(x, y - s * .7f); f.cubicTo(x + s * .3f, y - s * .55f, x + s * .62f, y - s * .45f, x + s * .55f, y - s * .05f); f.lineTo(x + s * .45f, y - s * .05f); f.cubicTo(x + s * .45f, y - s * .3f, x + s * .25f, y - s * .38f, x, y - s * .45f); f.close(); c.drawPath(f, paint);
    }
}
