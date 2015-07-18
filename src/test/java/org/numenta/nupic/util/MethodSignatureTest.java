package org.numenta.nupic.util;

import static org.junit.Assert.*;

import org.junit.Test;


public class MethodSignatureTest {

    @Test
    public void testSetParam() {
        MethodSignature ms = new MethodSignature(2);
        
        int param1 = 5;
        ms.setParam(param1, 0);
        assertEquals(5, ms.get(0));
        
        String test = "test";
        ms.setParam(test, 1);
        assertEquals("test", ms.get(1));
        
        try {
            ms.setParam("fail", 2);
            fail();
        }catch(Exception e) {
            assertEquals(e.getClass(), ArrayIndexOutOfBoundsException.class);
        }
    }
    
    @Test
    public void testSetParams() {
        MethodSignature ms = new MethodSignature(2);
        
        int param1 = 5;
        String test = "test";
        ms.setParams(param1, test);
        assertEquals(5, ms.get(0));
        assertEquals("test", ms.get(1));
        
        try {
            ms.setParam("fail", 2);
            fail();
        }catch(Exception e) {
            assertEquals(e.getClass(), ArrayIndexOutOfBoundsException.class);
        }
    }

}
