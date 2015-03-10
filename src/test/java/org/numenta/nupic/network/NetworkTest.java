package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.network.Network.Mode;


public class NetworkTest {

    /**
     * Test that a null {@link Assembly.Mode} results in exception
     */
    @Test
    public void testNullModeUninstantiable() {
        Parameters p1 = Parameters.getAllDefaultParameters();
        
        try {
            Network n1 = Assembly.create(p1, Mode.MANUAL);
            n1 = Assembly.create(p1, Mode.AUTO);
            n1 = Assembly.create(p1, Mode.REACTIVE);
            //Null parameters are allowed at the Network level (not Region)
            n1 = Assembly.create(null, Mode.MANUAL);
            //Should throw exception
            n1 = Assembly.create(p1, null);
            fail();
        }catch(Exception e) {
            assertTrue(IllegalArgumentException.class.isAssignableFrom(e.getClass()));
            assertEquals("Mode cannot be null and must be one of: { MANUAL, AUTO, REACTIVE }", e.getMessage());
        }
   }

}
