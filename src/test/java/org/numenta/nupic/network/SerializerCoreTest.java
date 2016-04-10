package org.numenta.nupic.network;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.numenta.nupic.network.Persistence;
import org.numenta.nupic.network.SerializerCore;


public class SerializerCoreTest {

    @Test
    public void testGetSerializer() {
        SerializerCore serializer = Persistence.get().serializer();
        assertNotNull(serializer);
        
        SerializerCore  serializer2 = Persistence.get().serializer();
        assertTrue(serializer == serializer2);
    }

    
}
