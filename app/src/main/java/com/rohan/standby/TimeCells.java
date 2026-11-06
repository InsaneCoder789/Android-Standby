package com.rohan.standby;

/** Four reserved digit positions and a colon, including the unused first 12-hour position. */
final class TimeCells {
    static final float COLON_UNITS = .5f;
    static final float TOTAL_UNITS = 4f + COLON_UNITS;
    private TimeCells() { }
    static String padded(String value) {
        int colon = value.indexOf(':');
        return colon == 1 ? " " + value : value;
    }
    static float units(String value) { float n=0;for(int i=0;i<value.length();i++)n+=value.charAt(i)==':'?COLON_UNITS:1f;return n; }
    static float slotLeft(String value,int index,float digitWidth) { float n=0;for(int i=0;i<index;i++)n+=value.charAt(i)==':'?COLON_UNITS:1f;return n*digitWidth; }
    static float width(float digitWidth) { return digitWidth * TOTAL_UNITS; }
    static float slotLeft(int index, float digitWidth) {
        if(index < 0 || index > 4) throw new IllegalArgumentException("Five time slots only");
        return digitWidth * (index <= 2 ? index : index - .5f);
    }
}
