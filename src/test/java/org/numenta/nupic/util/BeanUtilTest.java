package org.numenta.nupic.util;

import static org.junit.Assert.*;

import org.junit.Test;


public class BeanUtilTest {

    @Test
    public void testGetSimpleProperty() {
        BeanUtil beanUtil = BeanUtil.getInstance();
        
        @SuppressWarnings("unused")
        class Tester {
            private int testInt;
            public void setTestInt(int t) {
                this.testInt = t;
            }
        }
        
        try {
            beanUtil.getSimpleProperty(new Tester(), "testInt");
        }catch(Exception e) {
            assertEquals("Property 'testInt' of bean " +
                "org.numenta.nupic.util.BeanUtilTest$1Tester does not have getter method", e.getMessage());
        }
    }

    @Test
    public void testSetSimpleProperty() {
        BeanUtil beanUtil = BeanUtil.getInstance();
        
        @SuppressWarnings("unused")
        class Tester {
            private int testInt;
            public void setTestInt(int t) {
                this.testInt = t;
            }
        }
        
        try {
            beanUtil.setSimpleProperty(new Tester(), "testInt", 50);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        class TesterNoWrite {
            private int testInt;
            @SuppressWarnings("unused")
            public int getTestInt() {
                return this.testInt;
            }
        }
        
        try {
            beanUtil.setSimpleProperty(new TesterNoWrite(), "testInt", "50");
        }catch(Exception e) {
            assertEquals("Property 'testInt' of bean "+
                "org.numenta.nupic.util.BeanUtilTest$1TesterNoWrite does not have setter method", e.getMessage());
        }
    }
    
    @Test
    public void testGetPropertyInfoRequired() {
        BeanUtil beanUtil = BeanUtil.getInstance();
        
        class TesterNoWrite {
            @SuppressWarnings("unused")
            private int testInt;
        }
        
        try {
            beanUtil.getPropertyInfoRequired(new TesterNoWrite(), "testInt");
        }catch(Exception e) {
            assertEquals("Bean org.numenta.nupic.util.BeanUtilTest$2TesterNoWrite does not have property 'testInt'", e.getMessage());
        }
    }
}
