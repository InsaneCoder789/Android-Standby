package com.rohan.standby;
import org.junit.Test;
import static org.junit.Assert.*;
public class TimeCellsTest {
    @Test public void everyMinuteKeepsTheColonAndFourDigitSlots() {
        for(int hour=0;hour<24;hour++)for(int minute=0;minute<60;minute++){
            String twelve=(hour%12==0?12:hour%12)+":"+String.format(java.util.Locale.ROOT,"%02d",minute);
            String value=TimeCells.padded(twelve);
            assertEquals(5,value.length());assertEquals(':',value.charAt(2));
            assertEquals(200f,TimeCells.slotLeft(2,100f),0f);
            assertEquals(250f,TimeCells.slotLeft(3,100f),0f);
            assertEquals(450f,TimeCells.width(100f),0f);
        }
    }
    @Test public void oneDigitHourLeavesAnEmptySlotRatherThanMovingMinutes() {
        assertEquals(" 9:59",TimeCells.padded("9:59"));
        assertEquals("10:00",TimeCells.padded("10:00"));
        assertEquals("23:59",TimeCells.padded("23:59"));
    }
    @Test public void elapsedHoursRetainAllSlotsWithoutThrowing() { assertEquals(7f,TimeCells.units("12:34:56"),0f);assertEquals(600f,TimeCells.slotLeft("12:34:56",7,100f),0f); }
    @Test(expected=IllegalArgumentException.class) public void rejectsInvalidSlot(){TimeCells.slotLeft(5,100f);}
}
