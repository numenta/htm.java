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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;

import rx.Observer;


public class PublisherSupplierTest {

    @Test
    public void testPublisherCreation() {
        ////////////////
        //    Setup   //
        ////////////////
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        
        Supplier<Publisher> supplier = PublisherSupplier.builder()
            .addHeader("dayOfWeek")
            .addHeader("number")
            .addHeader("B")
            .build();
        
        // This line invokes all the Publisher creation underneath
        @SuppressWarnings("unused")
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", supplier}));
        
        
        ///////////////////////////////////////
        //   Now Test Publisher was created  //
        ///////////////////////////////////////
        
        Publisher pub = supplier.get();
        assertNotNull(pub);
        
        List<String> outputList = new ArrayList<>();
        pub.subscribe(new Observer<String>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(String s) {
                outputList.add(s);
            }
        });
        
        pub.onNext("" + 0);
        
        for(int i = 0;i < outputList.size();i++) {
            switch(i) {
                case 0: assertEquals("dayOfWeek", outputList.get(i));break;
                case 1: assertEquals("number", outputList.get(i));break;
                case 2: assertEquals("B", outputList.get(i));break;
                case 3: assertEquals("0", outputList.get(i));break;
            }
        }
        
        
        // Next test pessimistic path
        Network network2 = Network.create("testNetwork", p);
        Publisher nullPublisher = null;
        try {
            nullPublisher = network2.getPublisher();
            fail(); // Should not reach here
        }catch(Exception e) {
            assertEquals("A Supplier must be built first. " +
                "please see Network.getPublisherSupplier()", e.getMessage());
        }
        assertNull(nullPublisher);
    }

}
