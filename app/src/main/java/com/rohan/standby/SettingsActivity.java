package com.rohan.standby;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/** Settings app: home, faces (with order), one page per face, display, activation (quiet hours, tilt test) and region. Controls write {@link Prefs} immediately; the preview updates live. */
public class SettingsActivity extends Activity {
    private static final int CARD = 0xff1c1c1e, MUTED = 0xff8e8e93, GREEN = 0xff30d158, ORANGE = 0xffff9f0a, RED = 0xffff453a;
    private static final int[] FLOAT_SWATCH = {0xfff06cb4, 0xff6fdc8c, 0xff5ac8fa, 0xffffb347};
    private static final int REQ_PHOTOS = 12, REQ_LOCATION = 13, REQ_NOTIFY = 15;

    private final ArrayDeque<String> stack = new ArrayDeque<>();
    private ScrollView scroll; private LinearLayout column; private StandbyView preview;
    private final List<Runnable> refreshers = new ArrayList<>();
    private SensorManager sensorManager; private SensorEventListener tiltListener; private TextView tiltStatus, tiltHint, tiltValues;

    /** Settings content never grows wider than a comfortable column, so landscape and tablets stay tidy. */
    private int contentWidth() { return Math.min(getResources().getDisplayMetrics().widthPixels, dp(560)); }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); Fonts.init(this);
        getWindow().setStatusBarColor(Color.BLACK); getWindow().setNavigationBarColor(Color.BLACK);
        scroll = new ScrollView(this); scroll.setBackgroundColor(Color.BLACK); scroll.setFillViewport(true); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v, in) -> { v.setPadding(0, in.getSystemWindowInsetTop(), 0, in.getSystemWindowInsetBottom()); return in; }); scroll.setClipToPadding(true);
        String start = getIntent().getStringExtra("page"); go(start == null ? "home" : start);

    }
    @Override protected void onResume() { super.onResume(); Instant.sync(this); for (Runnable r : refreshers) r.run(); if (preview != null) preview.reload(); if (stack.peek() != null && stack.peek().equals("tilt")) startTilt(); }
    @Override protected void onPause() { stopTilt(); super.onPause(); }
    @Override public void onBackPressed() { if (stack.size() > 1) { stack.pop(); render(stack.peek()); } else super.onBackPressed(); }
    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) { super.onRequestPermissionsResult(code, perms, results); Weather.refresh(this); for (Runnable r : refreshers) r.run(); if (preview != null) preview.reload(); if (code == 7) grantAll(); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == REQ_PHOTOS && result == RESULT_OK && data != null) {
            List<android.net.Uri> picked = new ArrayList<>();
            ClipData clip = data.getClipData(); if (clip != null) for (int i = 0; i < clip.getItemCount(); i++) picked.add(clip.getItemAt(i).getUri()); else if (data.getData() != null) picked.add(data.getData());
            StringBuilder sb = new StringBuilder(); int saved = 0;
            for (android.net.Uri u : picked) { try { getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); if (saved++ > 0) sb.append('\n'); sb.append(u); } catch (SecurityException e) { saved--; } }
            if (saved == 0) Toast.makeText(this, "Could not save access to those photos", Toast.LENGTH_LONG).show();
            else { Prefs.sp(this).edit().putString("photoList", sb.toString()).remove("photo").apply(); changed(); rebuild(); }
        }
    }

    // ───────────────────────── navigation ─────────────────────────
    private void go(String page) { stack.push(page); render(page); }
    private void rebuild() { if (stack.peek() != null) { int y = scroll.getScrollY(); render(stack.peek()); scroll.post(() -> scroll.scrollTo(0, y)); } }
    private void render(String page) {
        stopTilt(); refreshers.clear(); preview = null;
        column = new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL); column.setPadding(dp(20), dp(16), dp(20), dp(40));
        scroll.removeAllViews(); scroll.addView(column, new FrameLayout.LayoutParams(contentWidth(), -2, Gravity.CENTER_HORIZONTAL)); scroll.scrollTo(0, 0);
        if (page.equals("home")) home(); else if (page.equals("faces")) faces(); else if (page.startsWith("face")) face(Integer.parseInt(page.substring(4)));
        else if (page.equals("display")) display(); else if (page.equals("activation")) activation(); else if (page.equals("tilt")) tilt(); else if (page.equals("region")) region();
    }
    private void header(String title, String subtitle, boolean back) {
        if (back) { TextView b = text("‹  Back", 16, Fonts.UI_SEMI, GREEN, 0, 0, 14); b.setOnClickListener(v -> onBackPressed()); b.setPadding(0, dp(4), dp(20), dp(8)); b.setMinHeight(dp(48)); b.setGravity(Gravity.CENTER_VERTICAL); b.setContentDescription("Back"); }
        text(title, back ? 32 : 38, Fonts.ROUNDED_BLACK, Color.WHITE, 0, 0, 0); if (subtitle != null) text(subtitle, 16, Fonts.UI_MED, MUTED, 4, 0, 18); else text(" ", 4, Fonts.UI_REG, MUTED, 0, 0, 14);
    }
    private void changed() { Instant.sync(this); if (preview != null) preview.reload(); }

    // ───────────────────────── pages ─────────────────────────
    private void home() {
        header("Standby", "Make time your own", false); previewCard(-1);
        TextView yours = nav("Your frames", "Choose up to three of " + Frames.COUNT + ", their order and their looks", v -> go("faces"));
        refreshers.add(() -> { yours.setText(Prefs.frames(this).length + " of " + Frames.MAX); yours.setTextColor(GREEN); });
        section("SET UP");
        TextView s1 = nav("Screensaver", "Choose Standby there. Turn Low light mode off, or Android shows its own clock in the dark.", v -> startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)));
        TextView s2 = nav("Music and calls", "Now-playing controls and incoming calls", v -> startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
        TextView s4 = nav("Keep Standby ready", "Lets it start by itself, even after the phone has been idle. Also set Battery to never sleep this app.", v -> { try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:" + getPackageName()))); } catch (RuntimeException e) { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); } });
        TextView s5 = nav("Standby call screen", "Keeps Standby's call frame in front when the phone app opens its own call screen. Turn on Standby under Installed apps.", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        TextView s3 = nav("Weather", "Uses your approximate location", v -> { if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || !shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION) && Prefs.b(this, "askedLocation", false)) startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + getPackageName()))); else { Prefs.sp(this).edit().putBoolean("askedLocation", true).apply(); requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION); } });
        refreshers.add(() -> {
            String dream = Settings.Secure.getString(getContentResolver(), "screensaver_components"), l = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
            status(s1, dream != null && dream.contains(getPackageName())); status(s2, l != null && l.contains(getPackageName())); status(s3, checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED); status(s4, Instant.unrestricted(this)); String acc = Settings.Secure.getString(getContentResolver(), "enabled_accessibility_services"); status(s5, acc != null && acc.contains(getPackageName() + "/"));
        });
        section("CUSTOMISE");
        nav("Display and night", "Brightness, night mode, burn-in protection", v -> go("display"));
        nav("Activation", "Start rules, quiet hours, tilt test", v -> go("activation"));
        nav("Time and region", "12 or 24 hour, week start, temperature unit", v -> go("region"));
        section("RESET");
        LinearLayout reset = row(); reset.addView(label("Reset all settings", 17, Fonts.UI_SEMI, RED), new LinearLayout.LayoutParams(0, -2, 1f)); add(reset);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Reset all settings?").setMessage("Faces, order, colours, photos and every option go back to their defaults.")
            .setNegativeButton("Cancel", null).setPositiveButton("Reset", (d, w) -> { Prefs.sp(this).edit().clear().apply(); Toast.makeText(this, "Settings reset", Toast.LENGTH_SHORT).show(); render("home"); }).show());
        text("Standby appears when the phone rests sideways on a charger. It uses Android's screensaver, so it can't replace Always On Display.", 12, Fonts.UI_REG, 0xff636366, 24, 0, 0);
        for (Runnable r : refreshers) r.run();
    }

    private void faces() {
        header("Frames", "Standby shows up to three frames. In Standby, swipe sideways to move between them and up or down to change a frame's look.", true);
        int[] sel = Prefs.frames(this);
        section("ON YOUR STANDBY · " + sel.length + " OF " + Frames.MAX);
        for (int i = 0; i < Frames.MAX; i++) { if (i < sel.length) slotRow(sel, i); else emptySlot(i); }
        section("ALL " + Frames.COUNT + " FRAMES");
        text("Tap a frame to see its looks and options. Tap + to add it straight away.", 13, Fonts.UI_REG, MUTED, 0, 4, 12);
        LinearLayout rowView = null;
        for (int f = 0; f < Frames.COUNT; f++) {
            if (f % 2 == 0) { rowView = new LinearLayout(this); rowView.setOrientation(LinearLayout.HORIZONTAL); add(rowView); }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f); if (f % 2 == 1) lp.setMargins(dp(10), 0, 0, 0); rowView.addView(tile(f, sel), lp);
        }
        if (Frames.COUNT % 2 == 1) { View spacer = new View(this); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f); lp.setMargins(dp(10), 0, 0, 0); rowView.addView(spacer, lp); }
    }
    /** One of the three places on Standby: position number, live thumbnail, name and look, and move / remove buttons. */
    private void slotRow(final int[] sel, final int pos) {
        final int frame = sel[pos]; LinearLayout r = row(); r.setPadding(dp(12), dp(12), dp(8), dp(12));
        TextView num = label(Integer.toString(pos + 1), 22, Fonts.ROUNDED_BLACK, GREEN); num.setGravity(Gravity.CENTER); r.addView(num, new LinearLayout.LayoutParams(dp(26), -2));
        int tw = dp(104); LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(tw, Math.round(tw / 2.2f)); tlp.setMargins(dp(6), 0, dp(12), 0); r.addView(new Thumb(frame, Prefs.component(this, frame)), tlp);
        LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL); TextView nm = label(Frames.NAMES[frame], 17, Fonts.UI_SEMI, Color.WHITE); nm.setSingleLine(true); nm.setEllipsize(android.text.TextUtils.TruncateAt.END); texts.addView(nm);
        TextView look = label(Frames.COMPONENTS[frame] > 1 ? lookName(frame, Prefs.component(this, frame)) : "One look", 13, Fonts.UI_REG, MUTED); look.setSingleLine(true); look.setEllipsize(android.text.TextUtils.TruncateAt.END); look.setPadding(0, dp(3), 0, 0); texts.addView(look);
        LinearLayout icons = new LinearLayout(this); icons.setOrientation(LinearLayout.HORIZONTAL); icons.setPadding(0, dp(4), 0, 0);
        icons.addView(icon("↑", "Move " + Frames.NAMES[frame] + " earlier", pos > 0, Color.WHITE, v -> { Prefs.saveFrames(this, Frames.move(Prefs.frames(this), pos, -1)); changed(); rebuild(); }));
        icons.addView(icon("↓", "Move " + Frames.NAMES[frame] + " later", pos < sel.length - 1, Color.WHITE, v -> { Prefs.saveFrames(this, Frames.move(Prefs.frames(this), pos, 1)); changed(); rebuild(); }));
        icons.addView(icon("✕", "Remove " + Frames.NAMES[frame], sel.length > 1, RED, v -> removeFrame(frame)));
        texts.addView(icons); r.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));
        r.setOnClickListener(v -> go("face" + frame)); r.setContentDescription("Position " + (pos + 1) + ", " + Frames.NAMES[frame] + ". Open"); add(r);
    }
    private void emptySlot(int pos) {
        LinearLayout r = row(); GradientDrawable g = new GradientDrawable(); g.setCornerRadius(dp(18)); g.setStroke(dp(1), 0xff3a3a3c, dp(6), dp(5)); g.setColor(Color.BLACK); r.setBackground(g); r.setMinimumHeight(dp(72));
        TextView num = label(Integer.toString(pos + 1), 22, Fonts.ROUNDED_BLACK, 0xff48484a); num.setGravity(Gravity.CENTER); r.addView(num, new LinearLayout.LayoutParams(dp(26), -2));
        TextView t = label("Empty. Add a frame from the list below", 15, Fonts.UI_MED, MUTED); t.setPadding(dp(14), 0, 0, 0); r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f)); add(r);
    }
    /** A square-ish button for the slot rows; 48dp so it is easy to hit. */
    private TextView icon(String glyph, String description, boolean enabled, int color, View.OnClickListener click) {
        TextView v = label(glyph, 20, Fonts.UI_SEMI, color); v.setGravity(Gravity.CENTER); v.setContentDescription(description); v.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(44)));
        v.setEnabled(enabled); v.setAlpha(enabled ? 1f : .25f); if (enabled) { v.setOnClickListener(click); v.setFocusable(true); } return v;
    }
    /** Grid tile in the collection. Shows the slot number when the frame is on Standby, otherwise a + that adds it. */
    private View tile(final int frame, int[] sel) {
        int pos = -1; for (int i = 0; i < sel.length; i++) if (sel[i] == frame) pos = i;
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8), dp(8), dp(8), dp(10));
        GradientDrawable g = card(); if (pos >= 0) g.setStroke(dp(2), GREEN); box.setBackground(g);
        int tw = (contentWidth() - dp(40) - dp(10)) / 2 - dp(16); box.addView(new Thumb(frame, Prefs.component(this, frame)), new LinearLayout.LayoutParams(-1, Math.round(tw / 2.2f)));
        LinearLayout bottom = new LinearLayout(this); bottom.setOrientation(LinearLayout.HORIZONTAL); bottom.setGravity(Gravity.CENTER_VERTICAL); bottom.setPadding(dp(4), dp(8), 0, 0);
        LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL);
        TextView name = label(Frames.NAMES[frame], 15, Fonts.UI_SEMI, Color.WHITE); name.setSingleLine(true); name.setEllipsize(android.text.TextUtils.TruncateAt.END); texts.addView(name);
        TextView sub = label(pos >= 0 ? "On Standby · " + (pos + 1) : Frames.COMPONENTS[frame] + (Frames.COMPONENTS[frame] == 1 ? " look" : " looks"), 12, Fonts.UI_MED, pos >= 0 ? GREEN : MUTED); sub.setPadding(0, dp(2), 0, 0); texts.addView(sub);
        bottom.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));
        if (pos < 0) { TextView plus = label("+", 22, Fonts.UI_SEMI, Color.BLACK); plus.setGravity(Gravity.CENTER); GradientDrawable pg = new GradientDrawable(); pg.setShape(GradientDrawable.OVAL); pg.setColor(Color.WHITE); plus.setBackground(pg);
            plus.setContentDescription("Add " + Frames.NAMES[frame] + " to Standby"); plus.setOnClickListener(v -> addFrame(frame)); bottom.addView(plus, new LinearLayout.LayoutParams(dp(40), dp(40))); }
        box.addView(bottom); box.setOnClickListener(v -> go("face" + frame)); box.setContentDescription(Frames.NAMES[frame] + (pos >= 0 ? ", on Standby" : "") + ". Open");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); box.setLayoutParams(lp); return box;
    }
    private TextView action(String title, View.OnClickListener click){
        TextView v=label(title,16,Fonts.UI_SEMI,Color.WHITE);v.setGravity(Gravity.CENTER);v.setMinHeight(dp(52));v.setPadding(dp(14),dp(12),dp(14),dp(12));v.setBackground(card());v.setOnClickListener(click);v.setFocusable(true);v.setContentDescription(title);return v;
    }
    private void addFrame(final int frame) {
        int[] sel = Prefs.frames(this); if (Frames.contains(sel, frame)) return;
        if (sel.length < Frames.MAX) { Prefs.saveFrames(this, Frames.add(sel, frame)); changed(); Toast.makeText(this, Frames.NAMES[frame] + " added to Standby", Toast.LENGTH_SHORT).show(); rebuild(); return; }
        String[] names = new String[sel.length]; for (int i = 0; i < names.length; i++) names[i] = (i + 1) + ".  " + Frames.NAMES[sel[i]];
        new AlertDialog.Builder(this).setTitle("Standby is full. Replace which frame with " + Frames.NAMES[frame] + "?").setItems(names, (d, index) -> {
            int[] next = Prefs.frames(this).clone(); next[index] = frame; Prefs.saveFrames(this, next); changed(); rebuild(); }).setNegativeButton("Cancel", null).show();
    }
    private void removeFrame(int frame) {
        int[] sel = Prefs.frames(this); if (sel.length <= 1) { Toast.makeText(this, "Standby needs at least one frame", Toast.LENGTH_SHORT).show(); return; }
        Prefs.saveFrames(this, Frames.remove(sel, frame)); changed(); rebuild();
    }
    /** A still render of a frame, refreshed every 30 seconds while on screen. Never shows calls or alarms, never takes touches. */
    private final class Thumb extends View {
        private final StandbyView renderer; private android.graphics.Bitmap bitmap; private long drawnAt;
        Thumb(int frame, int look) {
            super(SettingsActivity.this); renderer = new StandbyView(SettingsActivity.this); renderer.previewMode = true; renderer.setActive(true); renderer.lock(frame, look);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            setOutlineProvider(new ViewOutlineProvider() { @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(10)); } }); setClipToOutline(true);
        }
        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight(); if (w <= 0 || h <= 0) return; long now = System.currentTimeMillis();
            if (bitmap == null || bitmap.getWidth() != w || bitmap.getHeight() != h || now - drawnAt > 30_000) {
                if (bitmap == null || bitmap.getWidth() != w || bitmap.getHeight() != h) bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.BLACK); Canvas bc = new Canvas(bitmap); int rw = 1100, rh = 500; renderer.layout(0, 0, rw, rh); bc.scale(w / (float) rw, h / (float) rh); renderer.draw(bc); drawnAt = now;
            }
            c.drawBitmap(bitmap, 0, 0, null); if (isShown()) postInvalidateDelayed(30_000);
        }
    }
    private String lookName(int frame,int look){
        if(look>=Frames.BASE[frame])return "Redesign";
        if(frame==Frames.DIGITAL)return look==0?"Digital":"Float";
        if(frame==Frames.DIAL)return new String[]{"Analog","Widgets","Calendar"}[look];
        if(frame==Frames.CLOCK_MUSIC)return look==0?"Analog and player":"Digital and player";
        if(Frames.COMPONENTS[frame]==1)return Frames.NAMES[frame];
        String[][] names={{"Cards","Cards and date"},{"Letters","Letters and date"},{"Time","Time and date"},{"Four tubes","Six tubes"},{"Time","Time and date"},{"Dial","Dial and date"},{"Orbits","Orbits and date"},{"Daylight","Clock and path"},{"Phase","Clock and phase"},{"Progress bars","Year of dots"},{"Date poster","Split date"},{"Weather","Clock and weather"},{"Time","Time and date"},{"4–7–8 breathing","Box breathing"},{"Command line","System information"},{"Time and date","Clock and sidebar"},{"Ring timer","Digital timer"}};
        return frame>=Frames.FLIP && frame<=Frames.TIMER?names[frame-Frames.FLIP][look]:"Look "+(look+1);
    }

    private void face(int f) {
        header(Frames.NAMES[f], Frames.INFO[f], true); previewCard(f);
        int[] sel = Prefs.frames(this); int pos = -1; for (int i = 0; i < sel.length; i++) if (sel[i] == f) pos = i;
        if (pos >= 0) {
            TextView on = label("✓  On your Standby, position " + (pos + 1) + " of " + sel.length, 15, Fonts.UI_SEMI, GREEN); on.setPadding(dp(4), dp(14), 0, dp(10)); column.addView(on);
            TextView remove = action("Remove from Standby", v -> removeFrame(f)); remove.setTextColor(sel.length > 1 ? RED : MUTED); add(remove);
        } else { TextView addB = action(sel.length < Frames.MAX ? "Add to Standby" : "Add to Standby (replace one)", v -> addFrame(f)); GradientDrawable g = new GradientDrawable(); g.setCornerRadius(dp(18)); g.setColor(GREEN); addB.setBackground(g); addB.setTextColor(Color.BLACK); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(14), 0, dp(10)); column.addView(addB, lp); }
        if (Frames.COMPONENTS[f] > 1) {
            section("LOOKS · " + Frames.COMPONENTS[f]);
            text("In Standby, swipe up or down to switch. The one you pick here is where it starts.", 13, Fonts.UI_REG, MUTED, 0, 4, 12);
            int cur = Prefs.component(this, f); LinearLayout rowView = null;
            for (int i = 0; i < Frames.COMPONENTS[f]; i++) {
                if (i % 2 == 0) { rowView = new LinearLayout(this); rowView.setOrientation(LinearLayout.HORIZONTAL); add(rowView); }
                final int look = i; LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8), dp(8), dp(8), dp(10));
                GradientDrawable g = card(); if (i == cur) g.setStroke(dp(2), GREEN); box.setBackground(g);
                int tw = (contentWidth() - dp(40) - dp(10)) / 2 - dp(16); box.addView(new Thumb(f, i), new LinearLayout.LayoutParams(-1, Math.round(tw / 2.2f)));
                TextView name = label((i == cur ? "✓  " : "") + lookName(f, i), 14, Fonts.UI_SEMI, i == cur ? GREEN : Color.WHITE); name.setPadding(dp(4), dp(8), 0, 0); name.setSingleLine(true); name.setEllipsize(android.text.TextUtils.TruncateAt.END); box.addView(name);
                box.setContentDescription(lookName(f, i) + (i == cur ? ", selected" : ""));
                box.setOnClickListener(v -> { Prefs.sp(this).edit().putInt("comp" + f, look).apply(); changed(); rebuild(); });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f); if (i % 2 == 1) lp.setMargins(dp(10), 0, 0, 0); rowView.addView(box, lp);
            }
            if (Frames.COMPONENTS[f] % 2 == 1) { View spacer = new View(this); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f); lp.setMargins(dp(10), 0, 0, 0); rowView.addView(spacer, lp); }
        }
        if (f != Frames.DIGITAL) section("OPTIONS");
        if (f == Frames.DIGITAL) { section("DIGITAL LOOK"); swatches("Colour", "digital", ClockFrames.DIGITAL_COLORS); toggle("Show date", null, "digitalDate", true); toggle("Show temperature", "Needs weather set up", "digitalTemp", true); toggle("Show next alarm", null, "digitalAlarm", true); section("FLOAT LOOK"); swatches("Colours", "float", FLOAT_SWATCH); toggle("Show colon bubbles", null, "floatColon", true); }
        else if (f == Frames.DIAL) { choice("Analog right side", "analogSide", new String[]{"Weather", "Date"}, 0); toggle("Seconds hand", "On every dial", "dialSeconds", true); toggle("Numerals", "On every dial", "dialNumerals", true); toggle("Dim weekends", "On the Widgets and Calendar looks", "dimWeekends", true); }
        else if (f == Frames.MUSIC || f == Frames.CLOCK_MUSIC) {
            choice("Background", "musicBackground", new String[]{"Album colours", "Black"}, 0); toggle("Animated seek bar", "The wave moves while music plays", "musicWave", true); toggle("Switch to the player when music plays", "Standby moves to Music or Clock and Music by itself when music starts", "autoMusic", true); toggle("Go back when it stops", "Returns to the frame you were on after 30 seconds of silence", "autoReturn", true);
            if (f == Frames.CLOCK_MUSIC) { swatches("Digital colour", "digital", ClockFrames.DIGITAL_COLORS); toggle("Seconds hand", "On the dial look", "dialSeconds", true); toggle("Numerals", "On the dial look", "dialNumerals", true); }
            text("Needs Music and calls access on the home screen.", 13, Fonts.UI_REG, MUTED, 4, 4, 0);
        }
        else if (f == Frames.PHOTOS) photos();
        else if (f == Frames.WORLD) world();
        else {
            if (NewFrames.PAL[f] != null) swatches("Colour", "accent" + f, NewFrames.PAL[f]);
            if (f == Frames.SUN || f == Frames.MOON || f == Frames.WEATHER) text("Uses your approximate location. Allow it on the home screen.", 13, Fonts.UI_REG, MUTED, 4, 4, 0);
            if (f == Frames.PROGRESS || f == Frames.WORD) text("Week start is in Time and region.", 13, Fonts.UI_REG, MUTED, 4, 4, 0);
        }
    }

    private void photos() {
        TextView pick = nav("Choose photos", "Pick one or many. Several play as a slideshow.", v -> { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); startActivityForResult(i, REQ_PHOTOS); });
        refreshers.add(() -> { int n = Prefs.photoUris(this).size(); pick.setText(n == 0 ? "None" : n + (n == 1 ? " photo" : " photos")); pick.setTextColor(n == 0 ? MUTED : GREEN); });
        LinearLayout clear = row(); clear.addView(label("Clear photos", 17, Fonts.UI_SEMI, RED), new LinearLayout.LayoutParams(0, -2, 1f)); if (!Prefs.photoUris(this).isEmpty()) add(clear);
        clear.setOnClickListener(v -> { Prefs.sp(this).edit().remove("photoList").remove("photo").apply(); changed(); rebuild(); });
        choice("Change every", "photoInterval", new String[]{"15 sec", "1 min", "5 min", "30 min"}, 1);
        toggle("Show clock over photo", null, "photoClock", true); slider("Dim photo", "photoDim", 0, 70, 0, "%");
        for (Runnable r : refreshers) r.run();
    }
    private void world() {
        String[] zones = Prefs.worldZones(this); long now = System.currentTimeMillis(); TimeZone local = TimeZone.getDefault();
        for (final String z : zones) {
            LinearLayout r = row(); LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL); texts.addView(label(WorldCities.city(z), 17, Fonts.UI_SEMI, Color.WHITE));
            TextView sub = label(WorldCities.relative(local, TimeZone.getTimeZone(z), now), 13, Fonts.UI_REG, MUTED); sub.setPadding(0, dp(3), 0, 0); texts.addView(sub); r.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView remove = label("Remove", 15, Fonts.UI_SEMI, RED); remove.setPadding(dp(16), dp(14), dp(4), dp(14)); remove.setMinWidth(dp(48)); remove.setContentDescription("Remove " + WorldCities.city(z)); r.addView(remove); add(r);
            remove.setOnClickListener(v -> { List<String> keep = new ArrayList<>(); for (String o : Prefs.worldZones(this)) if (!o.equals(z)) keep.add(o); Prefs.saveWorldZones(this, keep.toArray(new String[0])); changed(); rebuild(); });
        }
        if (zones.length < 4) {
            TextView add = nav("Add a city", "Up to four", v -> {
                List<String> ids = new ArrayList<>(), names = new ArrayList<>(); List<String> have = java.util.Arrays.asList(Prefs.worldZones(this));
                for (String z : WorldCities.ZONES) if (!have.contains(z)) { ids.add(z); names.add(WorldCities.city(z)); }
                new AlertDialog.Builder(this).setTitle("Add a city").setItems(names.toArray(new String[0]), (d, which) -> { List<String> next = new ArrayList<>(have); next.add(ids.get(which)); Prefs.saveWorldZones(this, next.toArray(new String[0])); changed(); rebuild(); }).show();
            });
            add.setText("+");
        } else text("Four cities is the most that fit. Remove one to add another.", 13, Fonts.UI_REG, MUTED, 4, 4, 8);
        toggle("Seconds hands", null, "worldSeconds", false);
    }

    private void display() {
        header("Display and night", "How the screen looks in the dark", true); previewCard(-1);
        section("BRIGHTNESS"); slider("Screen brightness", "brightness", 1, 100, 12, "%");
        section("ALARM AND TIMER"); choice("Alarm look", "alarmTheme", new String[]{"Auto", "Morning", "Sunset", "Evening", "Night", "Midnight"}, 0);
        section("NIGHT MODE"); toggle("Automatic night mode", "Uses the light sensor", "autoNight", true); choice("Switches on when", "nightLux", new String[]{"Very dark", "Dark", "Dim"}, 1); choice("Night style", "nightStyle", new String[]{"Red", "Dim", "Off"}, 0);
        LinearLayout r = row(); r.addView(label("Preview night mode", 17, Fonts.UI_SEMI, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1f)); Switch sw = new Switch(this); r.addView(sw); add(r);
        sw.setOnCheckedChangeListener((b, on) -> { if (preview != null) preview.setNight(on); });
        section("SCREEN CARE"); toggle("Charging ring", "On every frame except Digital", "chargeRing", true); toggle("Burn-in protection", "Shifts the picture slightly every minute", "burnIn", true);
        section("BEHAVIOUR"); toggle("Animations", "Slides between frames and looks", "animate", true); toggle("Calls and alarms", "Take over the screen when they ring", "alerts", true);
    }

    /** Asks for every runtime permission at once, then walks through the special switches Android only lets the user flip: overlay, full-screen alerts and battery. Safe to tap again; it goes to the next missing one. */
    private void grantAll() {
        java.util.ArrayList<String> need = new java.util.ArrayList<>();
        for (String p : new String[]{"android.permission.POST_NOTIFICATIONS", "android.permission.READ_PHONE_STATE", "android.permission.ANSWER_PHONE_CALLS", "android.permission.READ_CONTACTS", "android.permission.READ_CALL_LOG", "android.permission.READ_PHONE_NUMBERS", "android.permission.BLUETOOTH_CONNECT", "android.permission.ACCESS_COARSE_LOCATION"})
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) need.add(p);
        if (!need.isEmpty()) { requestPermissions(need.toArray(new String[0]), 7); return; }
        if (!Instant.overlayAllowed(this)) { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + getPackageName()))); return; }
        if (android.os.Build.VERSION.SDK_INT >= 34 && !((android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE)).canUseFullScreenIntent()) { startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, android.net.Uri.parse("package:" + getPackageName()))); return; }
        if (!Instant.unrestricted(this)) { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:" + getPackageName()))); return; }
        android.widget.Toast.makeText(this, "Every permission is on. Notification access and the call-screen accessibility switch are under their own rows.", android.widget.Toast.LENGTH_LONG).show();
    }

    private void activation() {
        header("Activation", "When Standby starts", true);
        section("INSTANT MODE");
        toggle("Open instantly", "Opens by itself the moment the phone is charging at the landscape angle, even from the always-on display", "instantMode", true);
        nav("Grant all permissions", "One tap: asks for notifications, phone, contacts, call log and location, then opens each special switch Android keeps for itself (overlay, full-screen alerts, battery)", v -> grantAll());
        TextView overlay = nav("Display over other apps", "The one permission Instant mode needs, so Standby can open over your lock screen", v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:" + getPackageName()))));
        refreshers.add(() -> status(overlay, Instant.overlayAllowed(this)));
        TextView notify = nav("Notifications", "Lets Android show the small “Standby is ready” notice that keeps Instant mode running", v -> { if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY); else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())); });
        refreshers.add(() -> status(notify, android.os.Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED));
        toggle("Only when locked", "Like StandBy on an iPhone: it won't interrupt you while you are using the phone", "instantOnlyLocked", true);
        choice("Hold still for", "instantHold", new String[]{"1 second", "2 seconds", "3 seconds"}, 0);
        for (Runnable r : refreshers) r.run();
        section("SCREENSAVER ROUTE");
        toggle("Only when sideways", "The screensaver starts only when the phone rests on its side at an angle", "requireTilt", true); choice("Start delay", "delay", new String[]{"Quick", "Normal", "Slow"}, 1);
        nav("Tilt test", "See live what your phone's sensors report", v -> go("tilt"));
        section("QUIET HOURS");
        toggle("Only during set hours", "Outside them Standby stays out of the way", "scheduleOn", false);
        timeRow("From", "scheduleStart", 22 * 60); timeRow("Until", "scheduleEnd", 7 * 60);
        text("If the phone leaves its sideways position, Standby closes itself. Turn the first option off to show Standby whenever Android starts the screensaver.", 13, Fonts.UI_REG, MUTED, 10, 4, 0);
    }
    private void timeRow(String title, final String key, final int def) {
        final TextView value = nav(title, null, null); final View row = (View) value.getParent();
        Runnable upd = () -> { value.setText(Schedule.format(Prefs.i(this, key, def), Prefs.is24(this))); value.setTextColor(Color.WHITE); }; refreshers.add(upd); upd.run();
        row.setOnClickListener(v -> { int m = Prefs.i(this, key, def); new TimePickerDialog(this, (view, h, min) -> { Prefs.sp(this).edit().putInt(key, h * 60 + min).apply(); upd.run(); }, m / 60, m % 60, Prefs.is24(this)).show(); });
    }

    private void tilt() {
        header("Tilt test", "Rest your phone on its stand and watch this screen.", true);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20), dp(22), dp(20), dp(22)); box.setBackground(card());
        tiltStatus = label("Reading sensors…", 26, Fonts.ROUNDED_BLACK, Color.WHITE); box.addView(tiltStatus);
        tiltHint = label(" ", 15, Fonts.UI_MED, MUTED); tiltHint.setPadding(0, dp(8), 0, dp(16)); box.addView(tiltHint);
        tiltValues = label(" ", 14, Fonts.UI_MED, 0xffd1d1d6); tiltValues.setLineSpacing(dp(4), 1f); box.addView(tiltValues); add(box);
        text("Standby starts once this says Ready and holds for the start delay. It closes about a second after the phone leaves the range.", 13, Fonts.UI_REG, MUTED, 6, 4, 0);
        startTilt();
    }
    private void startTilt() {
        if (tiltStatus == null || tiltListener != null) return;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); Sensor s = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY); if (s == null) s = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (s == null) { tiltStatus.setText("No motion sensor"); tiltHint.setText("This phone has no accelerometer, so Standby can't tell when it is sideways. Turn off “Only when sideways”."); return; }
        tiltListener = new SensorEventListener() {
            @Override public void onSensorChanged(SensorEvent e) { showTilt(e.values[0], e.values[1], e.values[2]); }
            @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        sensorManager.registerListener(tiltListener, s, SensorManager.SENSOR_DELAY_UI);
    }
    private void stopTilt() { if (sensorManager != null && tiltListener != null) sensorManager.unregisterListener(tiltListener); tiltListener = null; }
    private void showTilt(float x, float y, float z) {
        boolean ok = TiltGate.inside(x, y, z, false);
        tiltStatus.setText(ok ? "Ready" : "Not yet"); tiltStatus.setTextColor(ok ? GREEN : ORANGE); tiltHint.setText(TiltGate.hint(x, y, z)); tiltValues.setText(TiltGate.report(x, y, z));
    }

    private void region() {
        header("Time and region", null, true);
        choice("Clock", "clock24", new String[]{"System", "12-hour", "24-hour"}, 0); choice("Week starts on", "weekStart", new String[]{"System", "Sunday", "Monday"}, 0); choice("Temperature", "unit", new String[]{"Auto", "°F", "°C"}, 0);
    }

    // ───────────────────────── building blocks ─────────────────────────
    private void previewCard(int lockFace) {
        preview = new StandbyView(this); preview.setActive(true); preview.previewMode = true; if (lockFace >= 0) preview.lock(lockFace, Prefs.component(this, lockFace));
        FrameLayout frame = new FrameLayout(this); frame.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        frame.setOutlineProvider(new ViewOutlineProvider() { @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(22)); } }); frame.setClipToOutline(true);
        GradientDrawable edge = new GradientDrawable(); edge.setCornerRadius(dp(22)); edge.setStroke(dp(1), 0xff3a3a3c); edge.setColor(Color.TRANSPARENT); frame.setForeground(edge);
        int w = contentWidth() - dp(40); column.addView(frame, new LinearLayout.LayoutParams(-1, Math.round(w / 2.2f)));
        if (lockFace < 0) text("Live preview. Swipe it like Standby: sideways for frames, up or down for looks.", 13, Fonts.UI_REG, MUTED, 8, 4, 14);
    }
    private TextView text(String t, float sp, Typeface face, int color, int top, int left, int bottom) { TextView v = label(t, sp, face, color); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.setMargins(dp(left), dp(top), 0, dp(bottom)); column.addView(v, lp); return v; }
    private TextView label(String t, float sp, Typeface face, int color) { TextView v = new TextView(this); v.setText(t); v.setTextSize(sp); v.setTypeface(face); v.setTextColor(color); v.setIncludeFontPadding(false); return v; }
    private void section(String t) { TextView v = text(t, 12, Fonts.UI_SEMI, MUTED, 28, 4, 10); v.setLetterSpacing(.12f); }
    private GradientDrawable card() { GradientDrawable g = new GradientDrawable(); g.setColor(CARD); g.setCornerRadius(dp(18)); return g; }
    private LinearLayout row() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(18), dp(15), dp(18), dp(15)); r.setBackground(card()); return r; }
    private void add(View v) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, dp(10)); column.addView(v, lp); }
    private void status(TextView v, boolean on) { v.setText(on ? "On" : "Set up"); v.setTextColor(on ? GREEN : ORANGE); }

    /** Tappable row with a value on the right; returns the value label. */
    private TextView nav(String title, String sub, View.OnClickListener click) {
        LinearLayout r = row(); if (click != null) r.setOnClickListener(click);
        LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL); texts.addView(label(title, 17, Fonts.UI_SEMI, Color.WHITE));
        if (sub != null) { TextView s = label(sub, 13, Fonts.UI_REG, MUTED); s.setPadding(0, dp(3), 0, 0); texts.addView(s); }
        r.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f)); TextView value = label("", 15, Fonts.UI_SEMI, MUTED); value.setPadding(dp(12), 0, dp(6), 0); r.addView(value);
        r.addView(label("›", 24, Typeface.DEFAULT, 0xff636366)); add(r); return value;
    }
    private void toggle(String title, String sub, final String key, boolean def) {
        LinearLayout r = row(); LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL); texts.addView(label(title, 17, Fonts.UI_SEMI, Color.WHITE));
        if (sub != null) { TextView s = label(sub, 13, Fonts.UI_REG, MUTED); s.setPadding(0, dp(3), 0, 0); texts.addView(s); }
        r.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f)); Switch sw = new Switch(this); sw.setContentDescription(title);sw.setMinHeight(dp(48)); sw.setChecked(Prefs.b(this, key, def)); sw.setOnCheckedChangeListener((b, on) -> { Prefs.sp(this).edit().putBoolean(key, on).apply(); changed(); }); r.addView(sw);
        r.setOnClickListener(v -> sw.toggle()); add(r);
    }
    /** Segmented control. */
    private void choice(String title, final String key, String[] options, int def) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18), dp(15), dp(18), dp(15)); box.setBackground(card());
        box.addView(label(title, 17, Fonts.UI_SEMI, Color.WHITE)); LinearLayout chips = new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL); chips.setPadding(0, dp(12), 0, 0);
        final TextView[] views = new TextView[options.length];
        Runnable paint = () -> { int sel = Prefs.i(this, key, def); for (int i = 0; i < views.length; i++) { boolean on = i == sel; GradientDrawable g = new GradientDrawable(); g.setCornerRadius(dp(12)); g.setColor(on ? Color.WHITE : 0xff2c2c2e); views[i].setBackground(g); views[i].setTextColor(on ? Color.BLACK : 0xffd1d1d6); } };
        for (int i = 0; i < options.length; i++) {
            final int index = i; TextView c = label(options[i], 14, Fonts.UI_SEMI, Color.WHITE); c.setGravity(Gravity.CENTER); c.setPadding(dp(4), dp(10), dp(4), dp(10)); views[i] = c;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f); lp.setMargins(i == 0 ? 0 : dp(6), 0, 0, 0); chips.addView(c, lp);
            c.setOnClickListener(v -> { Prefs.sp(this).edit().putInt(key, index).apply(); paint.run(); changed(); });
        }
        box.addView(chips); paint.run(); add(box);
    }
    private void slider(String title, final String key, final int min, int max, int def, final String suffix) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18), dp(15), dp(18), dp(10)); box.setBackground(card());
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.addView(label(title, 17, Fonts.UI_SEMI, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1f));
        int cur = Prefs.clamp(Prefs.i(this, key, def), min, max); final TextView value = label(cur + suffix, 15, Fonts.UI_SEMI, MUTED); top.addView(value); box.addView(top);
        SeekBar bar = new SeekBar(this);bar.setContentDescription(title);bar.setMinimumHeight(dp(48)); bar.setMax(max - min); bar.setProgress(cur - min); bar.setPadding(0, dp(10), 0, dp(4));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) { value.setText((p + min) + suffix); if (user) { Prefs.sp(SettingsActivity.this).edit().putInt(key, p + min).apply(); changed(); } }
            @Override public void onStartTrackingTouch(SeekBar s) {} @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        box.addView(bar); add(box);
    }
    private void swatches(String title, final String key, final int[] colors) {
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(15),dp(18),dp(15));box.setBackground(card());box.addView(label(title,17,Fonts.UI_SEMI,Color.WHITE));LinearLayout r=new LinearLayout(this);r.setPadding(0,dp(10),0,0);box.addView(r);
        final View[] views = new View[colors.length];
        for (int i = 0; i < colors.length; i++) {
            final int index = i, color = colors[i];
            views[i] = new View(this) {
                private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                @Override protected void onDraw(Canvas c) { float rad = getWidth() / 2f; boolean on = Prefs.i(SettingsActivity.this, key, 0) == index; p.setStyle(Paint.Style.FILL); p.setColor(color); c.drawCircle(rad, rad, on ? rad * .66f : rad * .9f, p); if (on) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(Color.WHITE); c.drawCircle(rad, rad, rad - dp(1), p); } }
            };
            views[i].setContentDescription(title+", colour "+(i+1));views[i].setFocusable(true);
            views[i].setOnClickListener(v -> { Prefs.sp(this).edit().putInt(key, index).apply(); for (View o : views) o.invalidate(); changed(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48)); lp.setMargins(dp(8), 0, 0, 0); r.addView(views[i], lp);
        }
        add(box);
    }
}
