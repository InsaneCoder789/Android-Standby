package com.rohan.standby;

/**
 * One frame per activity, no overlays: when music starts playing the screen goes to the Music frame, and when it stops it goes back to where
 * you were. Pure logic with an injected clock so every case is unit-tested. Feed it once per frame; it says which face to switch to, or -1.
 */
final class AutoSwitch {
    static final int MUSIC = 5;
    static final long RETURN_DELAY_MS = 30_000;
    private boolean wasPlaying, auto; private int returnFace = -1; private long notPlayingSince = -1;

    int update(long now, boolean playing, int face, boolean musicFaceOn, boolean enabled, boolean returnEnabled) {
        int result = -1;
        if (enabled && musicFaceOn && playing && !wasPlaying && face != MUSIC) { returnFace = face; auto = true; result = MUSIC; }
        if (playing) notPlayingSince = -1; else if (wasPlaying || notPlayingSince < 0) { if (wasPlaying) notPlayingSince = now; }
        if (!playing && auto && returnEnabled && face == MUSIC && notPlayingSince >= 0 && now - notPlayingSince >= RETURN_DELAY_MS && returnFace >= 0) { result = returnFace; auto = false; returnFace = -1; }
        wasPlaying = playing;
        return result;
    }
    /** The person chose a face themselves: never move them back automatically. */
    void userSwitched() { auto = false; returnFace = -1; }
    boolean waitingToReturn() { return auto; }
}
