/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
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
