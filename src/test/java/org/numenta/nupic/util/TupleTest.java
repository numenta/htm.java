package org.numenta.nupic.util;

import static org.junit.Assert.*;

import org.junit.Test;


public class TupleTest {

    @Test
    public void testEquality() {
        Tuple t1 = new Tuple("1", new Double(1));
        Tuple t2 = new Tuple("1", new Double(1));
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertTrue(t1.equals(t2));
        
        t1 = new Tuple("1", new Double(1));
        t2 = new Tuple("2", new Double(1));
        assertNotEquals(t1, t2);
        assertNotEquals(t1.hashCode(), t2.hashCode());
        assertFalse(t1.equals(t2));
        
        t1 = new Tuple("1", new Double(1));
        t2 = new Tuple("1", new Double(1), 1);
        assertNotEquals(t1, t2);
        assertNotEquals(t1.hashCode(), t2.hashCode());
        assertFalse(t1.equals(t2));
    }

}
