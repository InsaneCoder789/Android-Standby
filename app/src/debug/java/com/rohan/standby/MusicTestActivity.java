package com.rohan.standby;

import android.app.Activity;
import android.graphics.*;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

/** Debug only. Publishes a real Android media session (3 tracks, generated artwork) so the music faces can be tested without a music app. Logs every transport command (tag MUSICTEST). */
public class MusicTestActivity extends Activity {
    private static final String T = "MUSICTEST";
    private static final String[][] TRACKS = {{"Solo Two", "ImanAganj", "205000"}, {"Golden Hour", "The Lantern Club", "251000"}, {"Northern Lights Over a Very Long Winter Night", "Aurora Borealis Ensemble", "312000"}};
    private static final int[][] ART = {{0xff3a2c8f, 0xff0e0a2e, 0xffd94fa0}, {0xffee7a1c, 0xff7a1d12, 0xffffd166}, {0xff12a58a, 0xff053b45, 0xff7be0c3}};
    private MediaSession session; private int track; private int state = PlaybackState.STATE_PLAYING; private long base = 42_000, stamp; private TextView label; private boolean withArt = true;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); withArt = getIntent().getBooleanExtra("art", true); track = getIntent().getIntExtra("track", 0); base = getIntent().getLongExtra("pos", 42_000);
        if (getIntent().getBooleanExtra("paused", false)) state = PlaybackState.STATE_PAUSED;
        label = new TextView(this); label.setVisibility(android.view.View.INVISIBLE); setContentView(label); // transparent screen: whatever is behind stays visible
        session = new MediaSession(this, "StandbyTest");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { base = now(); stamp = SystemClock.elapsedRealtime(); state = PlaybackState.STATE_PLAYING; Log.i(T, "onPlay"); publish(); }
            @Override public void onPause() { base = now(); state = PlaybackState.STATE_PAUSED; Log.i(T, "onPause at " + base); publish(); }
            @Override public void onSkipToNext() { track = (track + 1) % TRACKS.length; base = 0; Log.i(T, "onSkipToNext -> " + TRACKS[track][0]); loadTrack(); }
            @Override public void onSkipToPrevious() { track = (track + TRACKS.length - 1) % TRACKS.length; base = 0; Log.i(T, "onSkipToPrevious -> " + TRACKS[track][0]); loadTrack(); }
            @Override public void onSeekTo(long pos) { base = pos; Log.i(T, "onSeekTo " + pos); publish(); }
        });
        session.setActive(true); loadTrack();
        // Remote control for tests: adb shell am broadcast -a com.rohan.standby.TEST_PAUSE|TEST_PLAY|TEST_STOP -p com.rohan.standby
        android.content.IntentFilter f = new android.content.IntentFilter(); f.addAction("com.rohan.standby.TEST_PAUSE"); f.addAction("com.rohan.standby.TEST_PLAY"); f.addAction("com.rohan.standby.TEST_STOP");
        registerReceiver(new android.content.BroadcastReceiver() { @Override public void onReceive(android.content.Context c, android.content.Intent i) {
            String a = i.getAction(); if (a.endsWith("PAUSE")) { base = now(); state = PlaybackState.STATE_PAUSED; } else if (a.endsWith("PLAY")) { base = now(); stamp = SystemClock.elapsedRealtime(); state = PlaybackState.STATE_PLAYING; } else { state = PlaybackState.STATE_STOPPED; }
            Log.i(T, "test command " + a); publish(); } }, f, android.content.Context.RECEIVER_EXPORTED);
    }
    private long now() { return state == PlaybackState.STATE_PLAYING ? base + (SystemClock.elapsedRealtime() - stamp) : base; }
    private void loadTrack() {
        MediaMetadata.Builder m = new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, TRACKS[track][0]).putString(MediaMetadata.METADATA_KEY_ARTIST, TRACKS[track][1]).putLong(MediaMetadata.METADATA_KEY_DURATION, Long.parseLong(TRACKS[track][2]));
        if (withArt) m.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art(ART[track]));
        session.setMetadata(m.build()); stamp = SystemClock.elapsedRealtime(); publish();
    }
    private void publish() {
        stamp = SystemClock.elapsedRealtime();
        session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO).setState(state, base, state == PlaybackState.STATE_PLAYING ? 1f : 0f, stamp).build());
        label.setText("Test media session\n\n" + TRACKS[track][0] + "\n" + TRACKS[track][1] + "\n" + (state == PlaybackState.STATE_PLAYING ? "playing" : "paused") + " at " + base / 1000 + "s");
    }
    /** Colourful square cover with a soft diagonal gradient and a few shapes, so palette extraction has something to chew on. */
    private static Bitmap art(int[] c) {
        Bitmap bmp = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888); Canvas cv = new Canvas(bmp); Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 600, 600, c[0], c[1], Shader.TileMode.CLAMP)); cv.drawRect(0, 0, 600, 600, p); p.setShader(null);
        p.setColor(c[2]); p.setAlpha(210); cv.drawCircle(420, 190, 120, p); p.setAlpha(90); cv.drawCircle(160, 430, 170, p);
        p.setColor(0xffffffff); p.setAlpha(60); for (int i = 0; i < 9; i++) cv.drawRect(40 + i * 60, 470, 70 + i * 60, 600, p);
        return bmp;
    }
    @Override protected void onDestroy() { if (session != null) { session.setActive(false); session.release(); } super.onDestroy(); }
}
