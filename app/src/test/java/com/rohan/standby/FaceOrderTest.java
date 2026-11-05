package com.rohan.standby;

import static org.junit.Assert.*;
import org.junit.Test;

public class FaceOrderTest {
    @Test public void emptyOrNullGivesDefaultOrder() { assertArrayEquals(new int[]{0, 1, 2, 3}, FaceOrder.parse(null, 4)); assertArrayEquals(new int[]{0, 1, 2, 3}, FaceOrder.parse("", 4)); }
    @Test public void keepsSavedOrder() { assertArrayEquals(new int[]{2, 0, 3, 1}, FaceOrder.parse("2,0,3,1", 4)); }
    @Test public void appendsFacesAddedLater() { assertArrayEquals(new int[]{1, 0, 2, 3}, FaceOrder.parse("1,0", 4)); }
    @Test public void dropsDuplicatesOutOfRangeAndGarbage() { assertArrayEquals(new int[]{2, 0, 1, 3}, FaceOrder.parse("2,2,9,-1,x,0, ,1", 4)); }
    @Test public void formatRoundTrips() { int[] o = {3, 1, 0, 2}; assertArrayEquals(o, FaceOrder.parse(FaceOrder.format(o), 4)); }
    @Test public void moveUpAndDown() { assertArrayEquals(new int[]{1, 0, 2}, FaceOrder.move(new int[]{0, 1, 2}, 1, -1)); assertArrayEquals(new int[]{0, 2, 1}, FaceOrder.move(new int[]{0, 1, 2}, 1, 1)); }
    @Test public void moveAtTheEndsDoesNothingAndNeverMutatesInput() {
        int[] o = {0, 1, 2}; assertArrayEquals(new int[]{0, 1, 2}, FaceOrder.move(o, 0, -1)); assertArrayEquals(new int[]{0, 1, 2}, FaceOrder.move(o, 2, 1)); assertArrayEquals(new int[]{0, 1, 2}, FaceOrder.move(o, 5, 1)); assertArrayEquals(new int[]{0, 1, 2}, o);
    }
}
