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
        Network network = Network.create("testNetwork", p);
        
        Supplier<Publisher> supplier = network.getPublisherSupplier()
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
        
        Publisher pub = network.getNewPublisher();
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
            nullPublisher = network2.getNewPublisher();
            fail(); // Should not reach here
        }catch(Exception e) {
            assertEquals("A Supplier must be built first. " +
                "please see Network.getPublisherSupplier()", e.getMessage());
        }
        assertNull(nullPublisher);
        
        // Test that the previous Publisher is still available and that the Network
        // returns the same one as before.
        assertEquals(pub, network.getNewPublisher());
    }

}
