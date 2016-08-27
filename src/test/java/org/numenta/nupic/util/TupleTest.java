package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Comparator;

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
    
    @Test
    public void testCompare() {
        Comparator<Tuple> comp = (tOne, tTwo) -> ((String)tOne.get(0)).compareTo((String)tTwo.get(0));
        Tuple t1 = new Tuple(comp, "1", new Double(1));
        Tuple t2 = new Tuple(comp, "2", new Double(1));
        
        assertEquals(-1, t1.compareTo(t2));
        assertNotEquals(1, t1.compareTo(t2));
    }
    
    @Test
    public void testCompareAttemptThrowsException() {
        Tuple t1 = new Tuple("1", new Double(1));
        Tuple t2 = new Tuple("2", new Double(1));
        try {
            t1.compareTo(t2);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("Tuples used for comparison should be " +
                "instantiated using the constructor taking a Comparator", e.getMessage());
        }
    }

}
