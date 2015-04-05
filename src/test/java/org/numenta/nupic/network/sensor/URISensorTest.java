package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.Ignore;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.URISensor;
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
     */
    @Ignore
    public void test() {
        Object[] n = { "optional", "http://www.metaware.us/nupic/rec-center-hourly.csv" };
        SensorParams parms = SensorParams.create(Keys::uri, n);
        Sensor<URI> sensor = Sensor.create(URISensor::create, parms);
        
        assertNotNull(sensor);
        assertNotNull(sensor.getParams());
    }

}
