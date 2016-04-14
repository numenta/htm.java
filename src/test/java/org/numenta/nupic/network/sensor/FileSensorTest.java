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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.SensorParams.Keys;


public class FileSensorTest {

    @Test
    public void testFileSensorCreation() {
        Object[] n = { "some name", ResourceLocator.path("rec-center-hourly.csv") };
        SensorParams parms = SensorParams.create(Keys::path, n);
        Sensor<File> sensor = Sensor.create(FileSensor::create, parms);
        
        assertNotNull(sensor);
        assertNotNull(sensor.getSensorParams());
        SensorParams sp = sensor.getSensorParams();
        assertEquals("some name", sp.get("FILE"));
        assertEquals(null, sp.get("NAME"));
        assertEquals(ResourceLocator.path("rec-center-hourly.csv"), sp.get("PATH"));
        
        Sensor<File> sensor2 = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, n)
        );
        
        assertNotNull(sensor2);
        assertNotNull(sensor2.getSensorParams());
        sp = sensor2.getSensorParams();
        assertEquals("some name", sp.get("FILE"));
        assertEquals(null, sp.get("NAME"));
        assertEquals(ResourceLocator.path("rec-center-hourly.csv"), sp.get("PATH"));
        
        try {
            String filepart = System.getProperty("user.dir") + "/src/test/resources/pathtest.jar";
            File f = new File(filepart);
            assertTrue(f.exists());
            String path = filepart + "!rec-center-hourly.csv";
            n = new Object[] { "some name", path };
            parms = SensorParams.create(Keys::path, n);
            sensor = Sensor.create(FileSensor::create, parms);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test
    public void testNegativeFileSensorCreation() {
        Object[] n = { "some name", ResourceLocator.path("rec-center-hourly.csv") };
        SensorParams parms = SensorParams.create(Keys::uri, n);
        try {
            Sensor.create(FileSensor::create, parms);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("Passed improperly formed Tuple: no key for \"PATH\"", e.getMessage());                
        }
        
        // Now test exception thrown in file existence check
        String badPath = "rec-nonexisting.csv";
        n = new Object[] { "some name", badPath };
        
        try {
            parms = SensorParams.create(Keys::path, n);
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("Passed improperly formed Tuple: invalid PATH: " + parms.get("PATH"), e.getMessage());
        }
    }
    
    @Test
    public void testJarFileEntryStreamFormation() {
        // Test code in the jar file parsing logic
        String filepart = System.getProperty("user.dir") + "/src/test/resources/pathtest.jar";
        
        File f = new File(filepart);
        assertTrue(f.exists());
        
        String path = filepart + "!rec-center-hourly.csv";
        Stream<String> stream = FileSensor.getJarEntryStream(path);
        
        assertEquals(10, 
            stream.limit(10)
            .filter(s -> s != null && !s.isEmpty())
            .collect(Collectors.toList())
            .size());
    }
}
