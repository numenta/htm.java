package org.numenta.nupic;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;
import org.numenta.nupic.algorithms.BitHistory;


public class BitHistoryTest {

    @Test
    public void testPFormatArray() {
        BitHistory history = new BitHistory(null, 1, 1);
        Method m = null;
        try {
            m = BitHistory.class.getDeclaredMethod("pFormatArray", new Class[] {double[].class});
            m.setAccessible(true);
            double[] da = { 2.0, 4.0 };
            String output = (String)m.invoke(history, da);
            assertEquals("[ 2.00 4.00 ]", output);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

}
