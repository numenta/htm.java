/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.Ignore;
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
    @Ignore
    public void test() {
        Object[] n = { "optional", "http://www.metaware.us/nupic/rec-center-hourly.csv" };
        SensorParams parms = SensorParams.create(Keys::uri, n);
        Sensor<URI> sensor = Sensor.create(URISensor::create, parms);
        
        assertNotNull(sensor);
        assertNotNull(sensor.getParams());
        
        assertEquals(4391, sensor.getInputStream().count());
    }

}
