package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class AutoSwitchTest {
    private static final int DIGITAL = 0, ANALOG = 2, MUSIC = AutoSwitch.MUSIC; private static final long S = 1000;
    private static int step(AutoSwitch a, long t, boolean playing, int face) { return a.update(t, playing, face, true, true, true); }

    @Test public void goesToMusicWhenPlaybackStarts() { AutoSwitch a = new AutoSwitch(); assertEquals(-1, step(a, 0, false, DIGITAL)); assertEquals(MUSIC, step(a, S, true, DIGITAL)); }
    @Test public void switchesOnlyOncePerStart() { AutoSwitch a = new AutoSwitch(); assertEquals(MUSIC, step(a, 0, true, DIGITAL)); for (int i = 1; i < 50; i++) assertEquals(-1, step(a, i * S, true, MUSIC)); }
    @Test public void alreadyOnMusicDoesNothing() { AutoSwitch a = new AutoSwitch(); assertEquals(-1, step(a, 0, true, MUSIC)); assertFalse(a.waitingToReturn()); }
    @Test public void musicAlreadyPlayingWhenStandbyOpensGoesStraightToMusic() { AutoSwitch a = new AutoSwitch(); assertEquals(MUSIC, step(a, 0, true, ANALOG)); }
    @Test public void doesNotSwitchWhenDisabledOrMusicFaceIsOff() {
        assertEquals(-1, new AutoSwitch().update(0, true, DIGITAL, true, false, true)); assertEquals(-1, new AutoSwitch().update(0, true, DIGITAL, false, true, true));
    }
    @Test public void returnsToThePreviousFaceAfterTheDelay() {
        AutoSwitch a = new AutoSwitch(); step(a, 0, true, ANALOG); assertEquals(-1, step(a, 10 * S, true, MUSIC)); assertEquals(-1, step(a, 11 * S, false, MUSIC));
        assertEquals(-1, step(a, 11 * S + AutoSwitch.RETURN_DELAY_MS - 1, false, MUSIC)); assertEquals(ANALOG, step(a, 11 * S + AutoSwitch.RETURN_DELAY_MS, false, MUSIC));
    }
    @Test public void returnsOnlyOnce() { AutoSwitch a = new AutoSwitch(); step(a, 0, true, ANALOG); step(a, S, false, MUSIC); assertEquals(ANALOG, step(a, 40 * S, false, MUSIC)); assertEquals(-1, step(a, 41 * S, false, ANALOG)); assertEquals(-1, step(a, 90 * S, false, ANALOG)); }
    @Test public void aShortPauseDoesNotSendYouBack() { AutoSwitch a = new AutoSwitch(); step(a, 0, true, DIGITAL); step(a, 5 * S, false, MUSIC); assertEquals(-1, step(a, 20 * S, false, MUSIC)); assertEquals(-1, step(a, 21 * S, true, MUSIC)); assertEquals(-1, step(a, 200 * S, true, MUSIC)); }
    @Test public void resumingCancelsTheCountdown() { AutoSwitch a = new AutoSwitch(); step(a, 0, true, DIGITAL); step(a, S, false, MUSIC); step(a, 25 * S, true, MUSIC); step(a, 26 * S, false, MUSIC); assertEquals(-1, step(a, 50 * S, false, MUSIC)); assertEquals(DIGITAL, step(a, 56 * S, false, MUSIC)); }
    @Test public void neverMovesYouBackIfYouChoseAFaceYourself() { AutoSwitch a = new AutoSwitch(); step(a, 0, true, DIGITAL); a.userSwitched(); step(a, S, false, MUSIC); assertEquals(-1, step(a, 100 * S, false, MUSIC)); }
    @Test public void doesNotReturnIfYouAlreadyLeftTheMusicFace() { AutoSwitch a = new AutoSwitch(); step(a, 0, true, DIGITAL); step(a, S, false, ANALOG); assertEquals(-1, step(a, 100 * S, false, ANALOG)); }
    @Test public void aNewPlaybackSwitchesAgainAfterReturning() { AutoSwitch a = new AutoSwitch(); step(a, 0, true, DIGITAL); step(a, S, false, MUSIC); assertEquals(DIGITAL, step(a, 40 * S, false, MUSIC)); assertEquals(MUSIC, step(a, 50 * S, true, DIGITAL)); }
    @Test public void returnCanBeTurnedOff() { AutoSwitch a = new AutoSwitch(); a.update(0, true, DIGITAL, true, true, false); a.update(S, false, MUSIC, true, true, false); assertEquals(-1, a.update(100 * S, false, MUSIC, true, true, false)); }
    @Test public void idleNeverDoesAnything() { AutoSwitch a = new AutoSwitch(); for (int i = 0; i < 200; i++) assertEquals(-1, step(a, i * 10 * S, false, i % 3)); }
}
