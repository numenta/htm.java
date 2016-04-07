/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.network.sensor.SensorParams.Keys;

/**
 * This is a *MANUAL ONLY* test due to the dependency on network resources.
 * The URL's in the test should actually point to available resources (and 
 * should be kept up in that order) - however we don't want this test run
 * on any continuous integration server.
 * 
 * @author David Ray
 */
public class URISensorTest {

    /**
     * Ignored - but may be run manually for testing and regression
     * To run this test change the "@Ignore" annotation to "@Test"
     */
    @Test
    public void testURISensorConstruction() {
        Object [] n = { "optional name", new StringReader(makeStream().collect(Collectors.joining("\n"))) }; 
        SensorParams parms = SensorParams.create(Keys::uri, n);
        Sensor<URI> sensor = Sensor.create(URISensor::create, parms);
        
        assertNotNull(sensor);
        assertNotNull(sensor.getSensorParams());
        
        assertEquals(21, sensor.getInputStream().count());
        
        // Test header is present
        ValueList list = sensor.getMetaInfo();
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("\'timestamp\':\'consumption\'", list.getRow(0).toString());
        assertEquals("\'datetime\':\'float\'", list.getRow(1).toString());
        assertEquals("\'T\':\'B\'", list.getRow(2).toString());
        
        try {
            parms = SensorParams.create(Keys::path, new Object[] { "", new Object() });
            Sensor.create(URISensor::create, parms);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("Passed improperly formed Tuple: no key for \"URI\"", e.getMessage());
        }
    }

    public Stream<String> makeStream() {
        return Stream.of(
            "timestamp, consumption",
            "datetime, float",
            "T,B",
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,16.4",
            "7/2/10 2:00,4.7",
            "7/2/10 3:00,4.7",
            "7/2/10 4:00,4.6",
            "7/2/10 5:00,23.5",
            "7/2/10 6:00,47.5",
            "7/2/10 7:00,45.4",
            "7/2/10 8:00,46.1",
            "7/2/10 9:00,41.5",
            "7/2/10 10:00,43.4",
            "7/2/10 11:00,43.8",
            "7/2/10 12:00,37.8",
            "7/2/10 13:00,36.6",
            "7/2/10 14:00,35.7",
            "7/2/10 15:00,38.9",
            "7/2/10 16:00,36.2",
            "7/2/10 17:00,36.6",
            "7/2/10 18:00,37.2",
            "7/2/10 19:00,38.2",
            "7/2/10 20:00,14.1");
    }
}
