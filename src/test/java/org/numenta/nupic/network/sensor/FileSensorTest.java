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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.datagen.ResourceLocator;
<<<<<<< HEAD
import org.numenta.nupic.network.NetworkTestHarness;
=======
>>>>>>> master
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.MersenneTwister;


public class FileSensorTest {

    @Test
    public void testFileSensorCreation() {
        Object[] n = { "some name", ResourceLocator.path("rec-center-hourly.csv") };
        SensorParams parms = SensorParams.create(Keys::path, n);
        Sensor<File> sensor = Sensor.create(FileSensor::create, parms);
        
        assertNotNull(sensor);
        assertNotNull(sensor.getParams());
        SensorParams sp = sensor.getParams();
        assertEquals("some name", sp.get("FILE"));
        assertEquals(null, sp.get("NAME"));
        assertEquals(ResourceLocator.path("rec-center-hourly.csv"), sp.get("PATH"));
        
        Sensor<File> sensor2 = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, n)
        );
        
        assertNotNull(sensor2);
        assertNotNull(sensor2.getParams());
        sp = sensor2.getParams();
        assertEquals("some name", sp.get("FILE"));
        assertEquals(null, sp.get("NAME"));
        assertEquals(ResourceLocator.path("rec-center-hourly.csv"), sp.get("PATH"));
    }
    
    @SuppressWarnings("unused")
    @Ignore
    private void testReadIntegerArray() {
        Publisher manual = Publisher.builder()
            .addHeader("sdr_in")
            .addHeader("int")
            .addHeader("B")
            .build();
        
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, 
                SensorParams.create(
                    Keys::obs, new Object[] {"name", manual}));
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        
        try {
            Stream<String> s = Files.lines(Paths.get(getClass().getResource("1_100.csv").getPath()));
            int[] SDR = s.mapToInt(b -> (int)(s.equals("0") ? 0 : 1)).toArray();
            s.close();
            System.out.println("len = " + Arrays.toString(SDR));
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

}
