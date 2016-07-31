package org.numenta.nupic.util;

import static org.junit.Assert.*;

import org.junit.Test;


public class MethodSignatureTest {

    @Test
    public void testSetParam() {
        MethodSignature ms = new MethodSignature();
        
        int param1 = 5;
        ms.addParam(param1, "five");
        assertEquals(5, ms.get("five"));
        
        String test = "test";
        ms.addParam(test, "name");
        assertEquals("test", ms.get("name"));
    }
    
    @Test
    public void testSetParams() {
        MethodSignature ms = new MethodSignature();
        
        String test = "test";
        ms.setParams(new String[] { "arg0", "arg1" }, test, "value1");
        assertEquals(test, ms.values().iterator().next());
        assertEquals("test", ms.get("arg0"));
        assertEquals("value1", ms.get("arg1"));
        
        try {
            ms.setParams(new String[] { "arg0", "arg1" }, test);
            fail();
        }catch(Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
            assertEquals("Keys and values must be same length.", e.getMessage());
        }
    }

}
