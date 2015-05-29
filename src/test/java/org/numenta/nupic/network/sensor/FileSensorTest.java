package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;


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

}
