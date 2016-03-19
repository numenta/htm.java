package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.network.NetworkSerializer.Scheme;


public class SerialConfigTest {

    @Test
    public void testEquals() {
        SerialConfig config1 = new SerialConfig("test.ser", Scheme.FST);
        SerialConfig config2 = new SerialConfig("test.ser", Scheme.FST);
        assertEquals(config1, config2);
        
        config2 = new SerialConfig("test", Scheme.FST);
        assertNotEquals(config1, config2);
        
        config2 = new SerialConfig("test.ser", Scheme.KRYO);
        assertNotEquals(config1, config2);
        
        config1 = new SerialConfig("test.ser", Scheme.KRYO);
        assertEquals(config1, config2);
        
        config2 = new SerialConfig("test.ser", Scheme.KRYO, Arrays.asList(Network.class, Connections.class));
        assertNotEquals(config1, config2);
        
        config1 = new SerialConfig("test.ser", Scheme.KRYO, Arrays.asList(Network.class, Connections.class));
        assertEquals(config1, config2);
        
        config2 = new SerialConfig("test.ser", Scheme.KRYO, Arrays.asList(Network.class, SerialConfig.class));
        assertNotEquals(config1, config2);
        
        config2 = new SerialConfig("test.ser", Scheme.KRYO, Arrays.asList(Network.class, Connections.class));
        assertEquals(config1.getFileName(), config2.getFileName());
        assertEquals(config1.getScheme(), config2.getScheme());
        assertEquals(config1.getRegistry(), config2.getRegistry());
        
        config1 = new SerialConfig(Scheme.FST);
        config2 = new SerialConfig(Scheme.KRYO);
        assertNotEquals(config1, config2);
        
        config1 = new SerialConfig(Scheme.KRYO);
        config2 = new SerialConfig(Scheme.KRYO);
        assertEquals(config1, config2);
        
        assertTrue(config1.getOpenOptions() != null);
        assertTrue(Arrays.equals(config1.getOpenOptions(),SerialConfig.PRODUCTION_OPTIONS));
    }

}
