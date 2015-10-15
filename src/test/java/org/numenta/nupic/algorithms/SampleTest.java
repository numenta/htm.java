package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.joda.time.DateTime;
import org.junit.Test;


public class SampleTest {

    @Test
    public void testEquals() {
        Sample s1 = new Sample(new DateTime(2015, 7, 12, 12, 0, 0), 0.75, 0.3);
        Sample s2 = new Sample(new DateTime(2015, 7, 12, 12, 0, 0), 0.75, 0.3);
        Sample s3 = new Sample(new DateTime(2015, 7, 12, 12, 0, 0), 0.85, 0.3);
        Sample s4 = new Sample(new DateTime(2015, 7, 12, 12, 0, 0), 0.75, 0.5);
        Sample s5 = new Sample(new DateTime(2015, 7, 12, 23, 0, 0), 0.75, 0.3);
        
        Sample s6 = new Sample(new DateTime(2015, 7, 12, 23, 0, 0), 0.75, 0.3);
        assertFalse(s6.equals(this));
        
        assertTrue(s1.equals(s2));
        assertFalse(s1.equals(s3));
        assertFalse(s1.equals(s4));
        assertFalse(s1.equals(s5));
        
        assertTrue(s1.timeStamp().equals(s3.timeStamp()));
        assertTrue(s1.timeStamp().equals(s4.timeStamp()));
        
        assertTrue(s1.score == s5.score);
        assertTrue(s1.value == s5.value);
        
        assertFalse(s1.hashCode() == s5.hashCode());
        assertTrue(s1.hashCode() == s2.hashCode());
    }

    @Test
    public void testBadInitialization() {
        Sample s1 = null;
        try {
            s1 = new Sample(null, 0.75, 0.3);
            fail();
           
            //Not reached...
            assertTrue(s1 != null);
        }catch(Exception e) {
            assertEquals("Sample must have a valid date", e.getMessage());
        }
    }
    
    @Test
    public void testStringRepresentationEqual() {
        Sample s1 = new Sample(new DateTime(2015, 7, 12, 12, 0, 0), 0.75, 0.3);
        Sample s2 = new Sample(new DateTime(2015, 7, 12, 12, 0, 0), 0.75, 0.3);
        
        assertEquals(s1.toString(), s2.toString());
    }
}
