package org.numenta.nupic.util;

import static org.junit.Assert.*;

import org.junit.Test;


public class ConditionTest {

    @Test
    public void testRqwAdapterReturnsFalse() {
        Condition.Adapter<Object> adapter = new Condition.Adapter<>();
        assertFalse(adapter.eval(1.0d));
        assertFalse(adapter.eval(1));
        assertFalse(adapter.eval(new Object()));
    }

}
