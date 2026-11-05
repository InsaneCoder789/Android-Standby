package com.rohan.standby;

/**
 * Landscape faces drawn onto a portrait window. {@code turn} is 0, 90 or 270 and matches the canvas transform used in StandbyView:
 * 90 = translate(pw,0) then rotate(90); 270 = translate(0,ph) then rotate(-90).
 */
final class ViewTurn {
    /** Window (physical) point to the landscape space the faces are drawn in: {x, y, width, height}. */
    static float[] logical(int turn, float x, float y, float pw, float ph) {
        if (turn == 90) return new float[]{y, pw - x, ph, pw};
        if (turn == 270) return new float[]{ph - y, x, ph, pw};
        return new float[]{x, y, pw, ph};
    }
    /** Inverse of {@link #logical}: where a landscape-space point lands on the window. */
    static float[] physical(int turn, float u, float v, float pw, float ph) {
        if (turn == 90) return new float[]{pw - v, u};
        if (turn == 270) return new float[]{v, ph - u};
        return new float[]{u, v};
    }
    /** Which way to turn: none when the window is already landscape, otherwise toward the side the phone is lying on. */
    static int turn(float width, float height, float gravityX) { return width >= height ? 0 : gravityX >= 0 ? 90 : 270; }
}
