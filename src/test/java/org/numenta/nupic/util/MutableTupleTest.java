package org.numenta.nupic.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class MutableTupleTest {

    @Test
    public void testSet() {
        MutableTuple mt = new MutableTuple("1", "2", 3, new int[] { 4 });
        assertEquals("1", mt.get(0));
        assertEquals("2", mt.get(1));
        assertEquals(3, mt.get(2));
        assertArrayEquals(new int[] { 4 }, (int[])mt.get(3));
        
        mt.set(1, 2);
        assertEquals(2, mt.get(1));
    }
    
    @Test
    public void testClear() {
        MutableTuple mt = new MutableTuple("1", "2", 3, new int[] { 4 });
        assertEquals("1", mt.get(0));
        assertEquals("2", mt.get(1));
        assertEquals(3, mt.get(2));
        assertArrayEquals(new int[] { 4 }, (int[])mt.get(3));
        
        mt.clear();
        assertTrue(mt.size() == 0);
        assertTrue(mt.all().isEmpty());
        
        try {
            mt.get(0);
            fail();
        }catch(Exception e) {
            assertEquals(ArrayIndexOutOfBoundsException.class, e.getClass());
        }
    }

}
