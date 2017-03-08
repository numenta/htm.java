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
package org.numenta.nupic.network;

import static org.numenta.nupic.network.NetworkTestHarness.getInferredFieldsMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_USE_MOVING_AVG;
import static org.numenta.nupic.algorithms.Anomaly.KEY_WINDOW_SIZE;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.SDRClassifier;
import org.numenta.nupic.algorithms.Classifier;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.encoders.RandomDistributedScalarEncoder;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.SDR;
import org.numenta.nupic.network.Layer.FunctionFactory;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.NamedTuple;
import org.numenta.nupic.util.UniversalRandom;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.TestObserver;
import rx.subjects.PublishSubject;

/**
 * Tests the "heart and soul" of the Network API
 * 
 * @author DavidRay
 *
 */
public class LayerTest extends ObservableTestBase {

    /** Total used for spatial pooler priming tests */
    private int TOTAL = 0;

    @Test
    public void testMasking() {
        byte algo_content_mask = 0;

        // -- Build up mask
        algo_content_mask |= Layer.CLA_CLASSIFIER;
        assertEquals(4, algo_content_mask);

        algo_content_mask |= Layer.SPATIAL_POOLER;
        assertEquals(5, algo_content_mask);

        algo_content_mask |= Layer.TEMPORAL_MEMORY;
        assertEquals(7, algo_content_mask);

        algo_content_mask |= Layer.ANOMALY_COMPUTER;
        assertEquals(15, algo_content_mask);

        // -- Now Peel Off
        algo_content_mask ^= Layer.ANOMALY_COMPUTER;
        assertEquals(7, algo_content_mask);

        assertEquals(0, algo_content_mask & Layer.ANOMALY_COMPUTER);
        assertEquals(2, algo_content_mask & Layer.TEMPORAL_MEMORY);

        algo_content_mask ^= Layer.TEMPORAL_MEMORY;
        assertEquals(5, algo_content_mask);

        algo_content_mask ^= Layer.SPATIAL_POOLER;
        assertEquals(4, algo_content_mask);

        algo_content_mask ^= Layer.CLA_CLASSIFIER;
        assertEquals(0, algo_content_mask);
    }
    
    @Test
    public void callsOnClosedLayer() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new UniversalRandom(42));
        
        Network n = new Network("AlreadyClosed", p)
            .add(Network.createRegion("AlreadyClosed")
                .add(Network.createLayer("AlreadyClosed", p)));
        
        Layer<?> l = n.lookup("AlreadyClosed").lookup("AlreadyClosed");
        l.using(new Connections());
        l.using(p);
        
        l.close();
        
        try {
            l.using(new Connections());
            
            fail(); // Should fail here, disallowing "using" call on closed layer
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("Layer already \"closed\"", e.getMessage());
        }
        
        try {
            l.using(p);
            
            fail(); // Should fail here, disallowing "using" call on closed layer
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("Layer already \"closed\"", e.getMessage());
        }
    }
    
    @Test
    public void testNoName() {
        Parameters p = Parameters.getAllDefaultParameters();
        
        try {
            new Network("", p)
                .add(Network.createRegion("")
                    .add(Network.createLayer("", p)
                        .add(Sensor.create(
                            FileSensor::create, 
                            SensorParams.create(
                                Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv"))))));
            
            fail(); // Fails due to no name...
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("All Networks must have a name. Increases digestion, and overall happiness!",
                e.getMessage());
        }
        
        try {
            new Network("Name", p)
                .add(Network.createRegion("")
                    .add(Network.createLayer("", p)
                        .add(Sensor.create(
                            FileSensor::create, 
                            SensorParams.create(
                                Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv"))))));
            
            fail(); // Fails due to no name on Region...
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("Name may not be null or empty. ...not that anyone here advocates name calling!",
                e.getMessage());
        }
    }
    
    @Test
    public void testAddSensor() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new UniversalRandom(42));
        
        try {
            PublisherSupplier supplier = PublisherSupplier.builder()
                .addHeader("dayOfWeek")
                .addHeader("int")
                .addHeader("B").build();
                        
            Network n = new Network("Name", p)
                .add(Network.createRegion("Name")
                    .add(Network.createLayer("Name", p)));
            
            Layer<?> l = n.lookup("Name").lookup("Name"); 
            l.add(Sensor.create(
                ObservableSensor::create, 
                SensorParams.create(
                    Keys::obs, "", supplier)));
            
            assertEquals(n, l.getNetwork());
            assertTrue(l.getRegion() != null);
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetAllValues() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new UniversalRandom(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));

        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);

        // Test that we get the expected exception if there hasn't been any processing.
        try {
            l.getAllValues("dayOfWeek", 1);
            fail();
        }catch(Exception e) {
            assertEquals("Predictions not available. Either classifiers unspecified or inferencing has not yet begun.", e.getMessage());
        }

        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                assertEquals(42, i.getSDR().length);
            }
        });

        // Now push some fake data through so that "onNext" is called above
        Map<String, Object> multiInput = new HashMap<>();
        multiInput.put("dayOfWeek", 0.0);
        l.compute(multiInput);

        Object[] values = l.getAllValues("dayOfWeek", 1);
        assertNotNull(values);
        assertTrue(values.length == 1);
        assertEquals(0.0D, values[0]);
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    @Test
    public void testResetMethod() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        Layer<?> l = Network.createLayer("l1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, false)
                .add(new TemporalMemory());
        try {
            l.reset();
            assertTrue(l.hasTemporalMemory());
        }catch(Exception e) {
            fail();
        }

        Layer<?> l2 = Network.createLayer("l2", p).add(new SpatialPooler());
        try {
            l2.reset();
            assertFalse(l2.hasTemporalMemory());
        }catch(Exception e) {
            fail();
        }
    }

    @Test
    public void testResetRecordNum() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        @SuppressWarnings("unchecked")
        Layer<int[]> l = (Layer<int[]>)Network.createLayer("l1", p).add(new TemporalMemory());
        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
//                System.out.println("output = " + Arrays.toString(output.getSDR()));
            }
        });

        l.compute(new int[] { 2,3,4 });
        l.compute(new int[] { 2,3,4 });
        assertEquals(1, l.getRecordNum());

        l.resetRecordNum();
        assertEquals(0, l.getRecordNum());
    }

    boolean isHalted = false;
    @Test
    public void testHalt() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, SensorParams.create(
                Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv")));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {
                assertTrue(l.isHalted());
                isHalted = true;
            }
            @Override public void onNext(Inference output) {}
        });
        
        l.start();

        try {
            l.halt();
            l.getLayerThread().join();
            assertTrue(isHalted);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    int trueCount = 0;
    @Test
    public void testReset() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
                SensorParams.create(
                    Keys::path, "", ResourceLocator.path("rec-center-hourly-4reset.csv")));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                if(l.getSensor().getMetaInfo().isReset()) {
                    trueCount++;
                }
            }
        });
        
        l.start();

        try {
            l.getLayerThread().join();
            assertEquals(3, trueCount);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    int seqResetCount = 0;
    @Test
    public void testSequenceChangeReset() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
                SensorParams.create(
                    Keys::path, "", ResourceLocator.path("rec-center-hourly-4seqReset.csv")));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                if(l.getSensor().getMetaInfo().isReset()) {
                    seqResetCount++;
                }
            }
        });
        
        l.start();

        try {
            l.getLayerThread().join();
            assertEquals(3, seqResetCount);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testLayerWithObservableInput() {
        PublisherSupplier manual = PublisherSupplier.builder()
            .addHeader("timestamp, consumption")
            .addHeader("datetime, float")
            .addHeader("B").build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(
                Keys::obs, new Object[] {"name", manual}));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));

        HTMSensor<ObservableSensor<String[]>> htmSensor = (HTMSensor<ObservableSensor<String[]>>)sensor;

        Network n = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("2/3", p)
                    .add(htmSensor)));
                
        final Layer<?> l = n.lookup("r1").lookup("2/3");
        
        TestObserver<Inference> tester;
        l.observe().subscribe(tester = new TestObserver<Inference>() {
            int idx = 0;
            @Override public void onCompleted() {}
            @Override public void onNext(Inference output) {
                switch(idx) {
                    case 0: assertEquals("[0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1]", Arrays.toString(output.getSDR()));
                    break;
                    case 1: assertEquals("[1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]", Arrays.toString(output.getSDR()));
                    break;
                    case 2: assertEquals("[0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", Arrays.toString(output.getSDR()));
                    break;
                }
                ++idx;
            }
        });
        
        l.start();
        
        Publisher pub = n.getPublisher();
        
        try {
            String[] entries = { 
                "7/2/10 0:00,21.2",
                "7/2/10 1:00,34.0",
                "7/2/10 2:00,40.4",
            };
            
            // Send inputs through the observable
            for(String s : entries) {
                pub.onNext(s);
            }
            
            ////////////////////
            // Very Important //
            ////////////////////
            pub.onComplete();

            try {
                l.getLayerThread().join();
            }catch(Exception e) {e.printStackTrace();}
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }
    
    public Stream<String> makeStream() {
        return Stream.of(
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,34.0",
            "7/2/10 2:00,40.4",
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
    
    @Test
    public void testLayerWithObservableInputIntegerArray() {
        Publisher manual = Publisher.builder()
            .addHeader("sdr_in")
            .addHeader("darr")
            .addHeader("B")
            .build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, 
                SensorParams.create(
                    Keys::obs, new Object[] {"name", manual}));

        Parameters p = Parameters.getAllDefaultParameters();
        p = p.union(getArrayTestParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        HTMSensor<ObservableSensor<String[]>> htmSensor = (HTMSensor<ObservableSensor<String[]>>)sensor;

        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        String input = "[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, "
                        + "1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, "
                        + "0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, "
                        + "1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, "
                        + "1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                        + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0]";

        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onNext(Inference output) {
                assertEquals(input, Arrays.toString((int[])output.getLayerInput()));
            }
        });
        
        l.start();

        try {
            String[] entries = { 
                  input
            };

            // Send inputs through the observable
            for(String s : entries) {
                manual.onNext(s);
                manual.onComplete();
            }

            l.getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    @Test 
    public void testLayerWithGenericObservable() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.set(KEY.RANDOM, new UniversalRandom(42));

        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        final int[] expected0 = new int[] { 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] expected1 = new int[] { 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0 };

        Func1<ManualInput, ManualInput> addedFunc = l -> { 
            return l.customObject("Interposed: " + Arrays.toString(l.getSDR()));
        };

        Network n = Network.create("Generic Test", p)
            .add(Network.createRegion("R1")
                .add(Network.createLayer("L1", p)
                    .add(addedFunc)
                    .add(new SpatialPooler())));

        @SuppressWarnings("unchecked")
        Layer<int[]> l = (Layer<int[]>)n.lookup("R1").lookup("L1");
        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                if(test == 0) {
                    assertTrue(Arrays.equals(expected0, i.getSDR()));
                    assertEquals("Interposed: [1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0]", i.getCustomObject());
                }
                if(test == 1) {
                    assertTrue(Arrays.equals(expected1, i.getSDR()));
                    assertEquals("Interposed: [1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0]", i.getCustomObject());
                }
                ++test; 
            }
        });

        // SHOULD RECEIVE BOTH
        // Now push some fake data through so that "onNext" is called above
        l.compute(inputs[0]);
        l.compute(inputs[1]);
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    @Test
    public void testBasicSetupEncoder_UsingSubscribe() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, null, null, null, null);

        final int[][] expected = new int[7][8];
        expected[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        expected[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        expected[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        expected[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        expected[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        expected[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        expected[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {}
            @Override public void onNext(Inference output) {
                assertTrue(Arrays.equals(expected[seq++], output.getSDR()));
            }
        });

        Map<String, Object> inputs = new HashMap<String, Object>();
        for(double i = 0;i < 7;i++) {
            inputs.put("dayOfWeek", i);
            l.compute(inputs);
        }
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    @Test
    public void testBasicSetupEncoder_UsingObserve() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, null, null, null, null);

        final int[][] expected = new int[7][8];
        expected[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        expected[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        expected[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        expected[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        expected[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        expected[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        expected[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        Observable<Inference> o = l.observe();
        TestObserver<Inference> tester;
        o.subscribe(tester = new TestObserver<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {}
            @Override public void onNext(Inference output) {
                assertTrue(Arrays.equals(expected[seq++], output.getSDR()));
            }
        });

        Map<String, Object> inputs = new HashMap<String, Object>();
        for(double i = 0;i < 7;i++) {
            inputs.put("dayOfWeek", i);
            l.compute(inputs);
        }
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    @Test
    public void testBasicSetupEncoder_AUTO_MODE() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv")));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new UniversalRandom(42));
        p.set(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        final int[][] expected = new int[][] {
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0}
        };

        ///////////////////////////////////////////////////////
        //              Test with 2 subscribers              //
        ///////////////////////////////////////////////////////
        TestObserver<Inference> tester;
        l.observe().subscribe(tester = new TestObserver<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {}
            @Override public void onNext(Inference output) {
//                System.out.println("  seq = " + seq + ",    recNum = " + output.getRecordNum() + ",  expected = " + Arrays.toString(expected[seq]));
//                System.out.println("  seq = " + seq + ",    recNum = " + output.getRecordNum() + ",    output = " + Arrays.toString(output.getSDR()));
                assertTrue(Arrays.equals(expected[seq], output.getSDR()));
                seq++;
            }
        });

        TestObserver<Inference> tester2;
        l.observe().subscribe(tester2 = new TestObserver<Inference>() {
            int seq2 = 0;
            @Override public void onCompleted() {}
            @Override public void onNext(Inference output) {
//                System.out.println("  seq = " + seq2 + ",    recNum = " + output.getRecordNum() + ",  expected = " + Arrays.toString(expected[seq2]));
//                System.out.println("  seq = " + seq2 + ",    recNum = " + output.getRecordNum() + ",    output = " + Arrays.toString(output.getSDR()));
                assertTrue(Arrays.equals(expected[seq2], output.getSDR()));
                seq2++;
            }
        });
        
        l.start();

        try {
            l.getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
        checkObserver(tester2);
    }

    /**
     * Temporary test to test basic sequence mechanisms
     */
    @Test
    public void testBasicSetup_SpatialPooler_MANUAL_MODE() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.set(KEY.RANDOM, new UniversalRandom(42));

        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        final int[] expected0 = new int[] { 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] expected1 = new int[] { 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0 };

        Layer<int[]> l = new Layer<>(p, null, new SpatialPooler(), null, null, null);

        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference spatialPoolerOutput) {
                if(test == 0) {
                    assertTrue(Arrays.equals(expected0, spatialPoolerOutput.getSDR()));
                }
                if(test == 1) {
                    assertTrue(Arrays.equals(expected1, spatialPoolerOutput.getSDR()));
                }
                ++test; 
            }
        });

        // Now push some fake data through so that "onNext" is called above
        l.compute(inputs[0]);
        l.compute(inputs[1]);
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    /**
     * Temporary test to test basic sequence mechanisms
     */
    @Test
    public void testBasicSetup_SpatialPooler_AUTO_MODE() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, "", ResourceLocator.path("days-of-week.csv")));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new UniversalRandom(42));
        p.set(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor).add(new SpatialPooler());
        
        final int[] expected0 = new int[] { 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] expected1 = new int[] { 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0 };
        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference spatialPoolerOutput) {
                if(test == 0) {
                    assertTrue(Arrays.equals(expected0, spatialPoolerOutput.getSDR()));
                }
                if(test == 1) {
                    assertTrue(Arrays.equals(expected1, spatialPoolerOutput.getSDR()));
                }
                ++test; 
            }
        });
        
        l.start();

        try {
            l.getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }
    
    /**
     * Temporary test to test basic sequence mechanisms
     */
    int seq = 0;
    @Test
    public void testBasicSetup_TemporalMemory_MANUAL_MODE() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.set(KEY.RANDOM, new MersenneTwister(42));

        final int[] input1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] input2 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] input3 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
        final int[] input4 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input5 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input6 = new int[] { 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 };
        final int[] input7 = new int[] { 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0 };
        final int[][] inputs = { input1, input2, input3, input4, input5, input6, input7 };

        
        Layer<int[]> l = new Layer<>(p, null, null, new TemporalMemory(), null, null);
        
        int timeUntilStable = 600;

        TestObserver<Inference> observer;
        
        l.subscribe(observer = new TestObserver<Inference>() {
            int test = 0;
            @Override
            public void onNext(Inference output) {
                if(seq / 7 >= timeUntilStable) {
                    System.out.println("seq: " + (seq) + "  --> " + (test) + "  output = " + Arrays.toString(output.getSDR()) +
                        ", \t\t\t\t cols = " + Arrays.toString(SDR.asColumnIndices(output.getSDR(), l.getConnections().getCellsPerColumn())));
                    assertTrue(output.getSDR().length >= 5);
                }
                
                ++seq;
                
                if(test == 6) test = 0;
                else test++;                
            }
        });
        
        // Now push some warm up data through so that "onNext" is called above
        for(int j = 0;j < timeUntilStable;j++) {
            for(int i = 0;i < inputs.length;i++) {
                l.compute(inputs[i]);
            }
        }

        for(int j = 0;j < 2;j++) {
            for(int i = 0;i < inputs.length;i++) {
                l.compute(inputs[i]);
            }
        }
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(observer);
    }
    
    @Test
    public void testBasicSetup_SPandTM() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.set(KEY.RANDOM, new MersenneTwister(42));

        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        Layer<int[]> l = new Layer<>(p, null, new SpatialPooler(), new TemporalMemory(), null, null);
        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                assertTrue(i.getSDR().length > 0);
            }
        });

        // Now push some fake data through so that "onNext" is called above
        l.compute(inputs[0]);
        l.compute(inputs[1]);
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    @Test
    public void testSpatialPoolerPrimerDelay() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.set(KEY.RANDOM, new UniversalRandom(42));

        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        final int[] expected0 = new int[] { 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] expected1 = new int[] { 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0 };

        // First test without prime directive :-P
        Layer<int[]> l = new Layer<>(p, null, new SpatialPooler(), null, null, null);
        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                if(test == 0) {
                    assertTrue(Arrays.equals(expected0, i.getSDR()));
                }
                if(test == 1) {
                    assertTrue(Arrays.equals(expected1, i.getSDR()));
                }
                ++test; 
            }
        });

        // SHOULD RECEIVE BOTH
        // Now push some fake data through so that "onNext" is called above
        l.compute(inputs[0]);
        l.compute(inputs[1]);

        // --------------------------------------------------------------------------------------------

        // NOW TEST WITH prime directive
        p.set(KEY.RANDOM, new UniversalRandom(42)); // due to static RNG we have to reset the sequence
        p.set(KEY.SP_PRIMER_DELAY, 1);

        Layer<int[]> l2 = new Layer<>(p, null, new SpatialPooler(), null, null, null);
        TestObserver<Inference> tester2;
        l2.subscribe(tester2 = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                // should be one and only onNext() called 
                assertTrue(Arrays.equals(expected1, i.getSDR()));
            }
        });

        // SHOULD RECEIVE BOTH
        // Now push some fake data through so that "onNext" is called above
        l2.compute(inputs[0]);
        l2.compute(inputs[1]);
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
        checkObserver(tester2);
    }

    /**
     * Simple test to verify data gets passed through the {@link CLAClassifier}
     * configured within the chain of components.
     */
    @Test
    public void testBasicClassifierSetup() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new UniversalRandom(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));

        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                assertEquals(42, i.getSDR().length);
            }
        });

        // Now push some fake data through so that "onNext" is called above
        Map<String, Object> multiInput = new HashMap<>();
        multiInput.put("dayOfWeek", 0.0);
        l.compute(multiInput);
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    /**
     * The {@link SpatialPooler} sometimes needs to have data run through it 
     * prior to passing the data on to subsequent algorithmic components. This
     * tests the ability to specify exactly the number of input records for 
     * the SpatialPooler to consume before passing records on.
     */
    @Test
    public void testMoreComplexSpatialPoolerPriming() {
        final int PRIME_COUNT = 35;
        final int NUM_CYCLES = 20;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        TOTAL = 0;

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));
        p.set(KEY.SP_PRIMER_DELAY, PRIME_COUNT);

        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                TOTAL++;
            }
        });

        // Now push some fake data through so that "onNext" is called above
        Map<String, Object> multiInput = new HashMap<>();
        for(int i = 0;i < NUM_CYCLES;i++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                multiInput.put("dayOfWeek", j);
                l.compute(multiInput);
            }
        }

        // Assert we can accurately specify how many inputs to "prime" the spatial pooler
        // and subtract that from the total input to get the total entries sent through
        // the event chain from bottom to top.
        assertEquals((NUM_CYCLES * INPUT_GROUP_COUNT) - PRIME_COUNT, TOTAL);
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }

    /**
     * Tests the ability for multiple subscribers to receive copies of
     * a given {@link Layer}'s computed values.
     */
    @Test
    public void test2ndAndSubsequentSubscribersPossible() {
        final int PRIME_COUNT = 35;
        final int NUM_CYCLES = 50;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        TOTAL = 0;

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));
        p.set(KEY.SP_PRIMER_DELAY, PRIME_COUNT);

        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);

        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                TOTAL++;
            }
        });

        TestObserver<Inference> tester2;
        l.subscribe(tester2 = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                TOTAL++;
            }
        });

        TestObserver<Inference> tester3;
        l.subscribe(tester3 = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                TOTAL++;
            }
        });

        // Now push some fake data through so that "onNext" is called above
        Map<String, Object> multiInput = new HashMap<>();
        for(int i = 0;i < NUM_CYCLES;i++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                multiInput.put("dayOfWeek", j);
                l.compute(multiInput);
            }
        }

        int NUM_SUBSCRIBERS = 3;
        // Assert we can accurately specify how many inputs to "prime" the spatial pooler
        // and subtract that from the total input to get the total entries sent through
        // the event chain from bottom to top.
        assertEquals( ((NUM_CYCLES * INPUT_GROUP_COUNT) - PRIME_COUNT) * NUM_SUBSCRIBERS, TOTAL);
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
        checkObserver(tester2);
        checkObserver(tester3);
    }

    @Test
    public void testGetAllPredictions() {
        final int PRIME_COUNT = 35;
        final int NUM_CYCLES = 600;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        TOTAL = 0;

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));
        p.set(KEY.SP_PRIMER_DELAY, PRIME_COUNT);
        
        final int cellsPerColumn = (int)p.get(KEY.CELLS_PER_COLUMN);
        assertTrue(cellsPerColumn > 0);

        MultiEncoder me = MultiEncoder.builder().name("").build();
        final Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);

        TestObserver<Inference> tester;
        l.subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                TOTAL++;
                
                if(l.getPreviousPredictiveCells() != null) {
                    //UNCOMMENT TO VIEW STABILIZATION OF PREDICTED FIELDS
//                    System.out.println("recordNum: " + i.getRecordNum() + "  Day: " + ((Map<String, Object>)i.getLayerInput()).get("dayOfWeek") + "  -  " + 
//                       Arrays.toString(ArrayUtils.where(i.getFeedForwardActiveColumns(), ArrayUtils.WHERE_1)) +
//                         "   -   " + Arrays.toString(SDR.cellsAsColumnIndices(i.getPreviousPredictiveCells(), cellsPerColumn)));
                }
            }
        });

        // Now push some fake data through so that "onNext" is called above
        Map<String, Object> multiInput = new HashMap<>();
        for(int i = 0;i < NUM_CYCLES;i++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                multiInput.put("dayOfWeek", j);
                l.compute(multiInput);
            }
            l.reset();
        }

        // Assert we can accurately specify how many inputs to "prime" the spatial pooler
        // and subtract that from the total input to get the total entries sent through
        // the event chain from bottom to top.
        assertEquals((NUM_CYCLES * INPUT_GROUP_COUNT) - PRIME_COUNT, TOTAL);

        double[] all = l.getAllPredictions("dayOfWeek", 1);
        double highestVal = Double.NEGATIVE_INFINITY;
        int highestIdx = -1;
        int i = 0;
        for(double d : all) {
            if(d > highestVal) {
                highestIdx = i;
                highestVal = d;
            }
            i++;
        }

        assertEquals(highestIdx, l.getMostProbableBucketIndex("dayOfWeek", 1));
        assertEquals(7, l.getAllPredictions("dayOfWeek", 1).length);
        
//        assertTrue(Arrays.equals(
//            ArrayUtils.where(l.getFeedForwardActiveColumns(), ArrayUtils.WHERE_1),
//                SDR.cellsAsColumnIndices(l.getPreviousPredictiveCells(), cellsPerColumn)));
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }
    
    /**
     * Test that the Anomaly Func can compute anomalies when no SpatialPooler
     * is present and the input is an int[] representing pre-processed sparse
     * SP output.
     */
    @Test
    public void testTM_Only_AnomalyCompute() {
        UniversalRandom random = new UniversalRandom(42);
        // SP and General
        Parameters parameters = NetworkTestHarness.getParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 104 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        parameters.set(KEY.CELLS_PER_COLUMN, 32);
        parameters.set(KEY.RANDOM, random);
        parameters.set(KEY.POTENTIAL_PCT, 0.85);//0.5
        parameters.set(KEY.GLOBAL_INHIBITION, true);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 40.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.0015);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 1000);
        parameters.set(KEY.MAX_BOOST, 2.0);
        // TM
        parameters.set(KEY.PERMANENCE_INCREMENT, 0.1);//0.05
        parameters.set(KEY.PERMANENCE_DECREMENT, 0.1);//0.05
        
        Network network = Network.create("NAB Network", parameters)
            .add(Network.createRegion("NAB Region")
                .add(Network.createLayer("NAB Layer", parameters)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())));
        
        Object[] testResults = new Object[2];
        
        network.observe().subscribe((inference) -> {
            double score = inference.getAnomalyScore();
            int record = inference.getRecordNum();
            
            if(testResults[0] == null && score < 1.0) {
                testResults[0] = record;
                testResults[1] = score;
            }
        }, (error) -> {
            error.printStackTrace();
        }, () -> {
            // On Complete
        });
        
        int[] input = { 717, 737, 739, 745, 758, 782, 793, 798, 805, 812, 833, 841, 
                        846, 857, 1482, 1515, 1536, 1577, 1578, 1600, 1608, 1612, 1642, 
                        1644, 1645, 1646, 1647, 1648, 1649, 1655, 1661, 1663, 1667, 1669, 
                        1677, 1683, 1688, 1706, 1710, 1720 };
        
        for(int i = 0;i < 100;i++) {
            network.compute(input);
            if(testResults[0] != null) break;
        }
        
        assertEquals(8, testResults[0]);
        assertEquals(0.0, testResults[1]);
    }

    /**
     * Test that a given layer can return an {@link Observable} capable of 
     * service multiple subscribers.
     */
    @Test
    public void testObservableRetrieval() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));
        MultiEncoder me = MultiEncoder.builder().name("").build();
        final Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);

        final List<int[]> emissions = new ArrayList<int[]>();
        Observable<Inference> o = l.observe();
        o.subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override public void onNext(Inference i) {
                emissions.add(l.getFeedForwardActiveColumns());
            }
        });
        o.subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override public void onNext(Inference i) {
                emissions.add(l.getFeedForwardActiveColumns());
            }
        });
        o.subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override public void onNext(Inference i) {
                emissions.add(l.getFeedForwardActiveColumns());
            }
        });

        Map<String, Object> multiInput = new HashMap<>();
        multiInput.put("dayOfWeek", 0.0);
        l.compute(multiInput);

        assertEquals(3, emissions.size());
        int[] e1 = emissions.get(0);
        for(int[] ia : emissions) {
            assertTrue(ia == e1);//test same object propagated
        }
    }

    /**
     * Simple test to verify data gets passed through the {@link CLAClassifier}
     * configured within the chain of components.
     */
    boolean flowReceived = false;
    @Test
    public void testFullLayerFluentAssembly() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.set(KEY.POTENTIAL_RADIUS, 200);
        p.set(KEY.INHIBITION_RADIUS, 50);
        p.set(KEY.GLOBAL_INHIBITION, true);

//        System.out.println(p);

        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        params.put(KEY_WINDOW_SIZE, 3);
        params.put(KEY_USE_MOVING_AVG, true);
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));
        Anomaly anomalyComputer = Anomaly.create(params);

        Layer<?> l = Network.createLayer("TestLayer", p)
            .alterParameter(KEY.AUTO_CLASSIFY, true)
            .add(anomalyComputer)
            .add(new TemporalMemory())
            .add(new SpatialPooler())
            .add(Sensor.create(
                FileSensor::create, 
                SensorParams.create(
                    Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv"))));

        l.getConnections().printParameters();

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                if(flowReceived) return; // No need to set this value multiple times

                flowReceived = i.getClassifiers().size() == 2 &&
                    i.getClassifiers().get("timestamp") == null &&
                        i.getClassifiers().get("consumption") != null;
            }
        });

        l.start();

        try {
            l.getLayerThread().join();
            assertTrue(flowReceived);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    boolean flowReceived2 = false;
    boolean flowReceived3 = false;
    @Test
    public void testMissingEncoderMap() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.set(KEY.POTENTIAL_RADIUS, 200);
        p.set(KEY.INHIBITION_RADIUS, 50);
        p.set(KEY.GLOBAL_INHIBITION, true);

        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        params.put(KEY_WINDOW_SIZE, 3);
        params.put(KEY_USE_MOVING_AVG, true);
        Anomaly anomalyComputer = Anomaly.create(params);

        Layer<?> l = Network.createLayer("TestLayer", p)
            .alterParameter(KEY.AUTO_CLASSIFY, true)
            .add(anomalyComputer)
            .add(new TemporalMemory())
            .add(new SpatialPooler())
            .add(Sensor.create(
                FileSensor::create, 
                SensorParams.create(
                    Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv"))));

        l.getConnections().printParameters();

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                if(flowReceived2) return; // No need to set this value multiple times

                flowReceived2 = i.getClassifiers().size() == 4 &&
                    i.getClassifiers().get("timestamp") != null &&
                        i.getClassifiers().get("consumption") != null;
            }
        });
        
        try {
            l.close();
            fail();
        }catch(Exception e) {
            assertEquals("Cannot initialize this Sensor's MultiEncoder with a null settings", e.getMessage());
        }

        try {
            assertFalse(flowReceived2);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        ////////////////// Test catch with no Sensor ////////////////////
        
        p = NetworkTestHarness.getParameters().copy();
        //p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.set(KEY.POTENTIAL_RADIUS, 200);
        p.set(KEY.INHIBITION_RADIUS, 50);
        p.set(KEY.GLOBAL_INHIBITION, true);

        params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        params.put(KEY_WINDOW_SIZE, 3);
        params.put(KEY_USE_MOVING_AVG, true);
        anomalyComputer = Anomaly.create(params);

        l = Network.createLayer("TestLayer", p)
            .alterParameter(KEY.AUTO_CLASSIFY, true)
            .add(anomalyComputer)
            .add(new TemporalMemory())
            .add(new SpatialPooler())
            .add(anomalyComputer)
            .add(MultiEncoder.builder().name("").build());
        
        l.getConnections().printParameters();

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                if(flowReceived3) return; // No need to set this value multiple times

                flowReceived3 = i.getClassifiers().size() == 4 &&
                    i.getClassifiers().get("timestamp") != null &&
                        i.getClassifiers().get("consumption") != null;
            }
        });
        
        try {
            l.close();
            fail();
        }catch(Exception e) {
            assertEquals("No field encoding map found for specified MultiEncoder", e.getMessage());
        }

        try {
            assertFalse(flowReceived3);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    private Parameters getArrayTestParams() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
            null,
            884, // n
            0, // w
            0, 0, 0, 0, null, null, null,
            "sdr_in", "darr", "SDRPassThroughEncoder");
        Parameters p = Parameters.empty();
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        return p;
    }

    private Map<String, Map<String, Object>> setupMap(
        Map<String, Map<String, Object>> map,
        int n, int w, double min, double max, double radius, double resolution, Boolean periodic,
        Boolean clip, Boolean forced, String fieldName, String fieldType, String encoderType) {

        if(map == null) {
            map = new HashMap<String, Map<String, Object>>();
        }
        Map<String, Object> inner = null;
        if((inner = map.get(fieldName)) == null) {
            map.put(fieldName, inner = new HashMap<String, Object>());
        }

        inner.put("n", n);
        inner.put("w", w);
        inner.put("minVal", min);
        inner.put("maxVal", max);
        inner.put("radius", radius);
        inner.put("resolution", resolution);

        if(periodic != null) inner.put("periodic", periodic);
        if(clip != null) inner.put("clip", clip);
        if(forced != null) inner.put("forced", forced);
        if(fieldName != null) inner.put("fieldName", fieldName);
        if(fieldType != null) inner.put("fieldType", fieldType);
        if(encoderType != null) inner.put("encoderType", encoderType);

        return map;
    }
    
    @Test
    public void testEquality() {
        Parameters p = Parameters.getAllDefaultParameters();
        Layer<Map<String, Object>> l = new Layer<>(p, null, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        Layer<Map<String, Object>> l2 = new Layer<>(p, null, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        
        assertTrue(l.equals(l));
        assertFalse(l.equals(null));
        assertTrue(l.equals(l2));
       
        l2.name("I'm different");
        assertFalse(l.equals(l2));
        
        l2.name(null);
        assertTrue(l.equals(l2));
        
        Network n = new Network("TestNetwork", p);
        Region r = new Region("r1", n);
        l.setRegion(r);
        assertFalse(l.equals(l2));
        
        l2.setRegion(r);
        assertTrue(l.equals(l2));
        
        Region r2 = new Region("r2", n);
        l2.setRegion(r2);
        assertFalse(l.equals(l2));
    }
    
    @Test
    public void testInferInputDimensions() {
        Parameters p = Parameters.getAllDefaultParameters();
        Layer<Map<String, Object>> l = new Layer<>(p, null, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        
        int[] dims = l.inferInputDimensions(16384, 2);
        assertTrue(Arrays.equals(new int[] { 128, 128 }, dims));
        
        dims = l.inferInputDimensions(8000, 3);
        assertTrue(Arrays.equals(new int[] { 20, 20, 20 }, dims));
        
        // Now test non-square dimensions
        dims = l.inferInputDimensions(450, 2);
        assertTrue(Arrays.equals(new int[] { 1, 450 }, dims));
    }
    
    @Test(expected = IllegalStateException.class)
    public void isClosedAddSensorTest() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        Layer<?> l = Network.createLayer("l", p);
        l.close();

        Sensor<File> sensor = Sensor.create(
                FileSensor::create,
                SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv")));
        l.add(sensor);
    }

    @Test(expected = IllegalStateException.class)
    public void isClosedAddMultiEncoderTest() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        Layer<?> l = Network.createLayer("l", p);
        l.close();

        l.add(MultiEncoder.builder().name("").build());
    }

    @Test(expected = IllegalStateException.class)
    public void isClosedAddSpatialPoolerTest() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        Layer<?> l = Network.createLayer("l", p);
        l.close();

        l.add(new SpatialPooler());
    }
    
    @Test
    public void testProperConstructionUsingNonFluentConstructor() {
        try {
            new Layer<>(null, null, null, null, null, null);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("No parameters specified.", e.getMessage());
        }
        
        Parameters p = NetworkTestHarness.getParameters();
        p.set(KEY.FIELD_ENCODING_MAP, null);
        try {
            new Layer<>(p, MultiEncoder.builder().build(), null, null, null, null);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("The passed in Parameters must contain a field encoding map specified by " +
             "org.numenta.nupic.Parameters.KEY.FIELD_ENCODING_MAP", e.getMessage());
        }
    }
    
    @Test
    public void testNullSubscriber() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        Layer<?> l = Network.createLayer("l", p); 
        
        try {
            l.subscribe(null);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("Subscriber cannot be null.", e.getMessage());
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testStringToInferenceTransformer() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        Layer<?> l = Network.createLayer("l", p); 
        FunctionFactory ff = l.new FunctionFactory();
        PublishSubject publisher = PublishSubject.create();
        Observable obs = ff.createEncoderFunc(publisher);
        
        String[] sa = { "42" };
        
        obs.subscribe(new Observer() {
            @Override public void onCompleted() { }
            @Override public void onError(Throwable arg0) { }
            @Override public void onNext(Object arg0) { 
            //    System.out.println("here");
            }
            
        });
        
        assertEquals(0, ff.inference.getRecordNum());
        
        publisher.onNext(sa);
        
        assertEquals("[42]", (Arrays.toString((int[])ff.inference.getLayerInput())));
        assertEquals(-1, ff.inference.getRecordNum());  // Record number gets set by the Layer which hasn't
                                                        // Received a record yet.
        assertEquals("[42]", (Arrays.toString((int[])ff.inference.getSDR())));
    }

    @Test
    public void testMakeClassifiers() {
        // Setup Parameters
        Parameters p = Parameters.getAllDefaultParameters();
        Map<String, Class<? extends Classifier>> inferredFieldsMap = new HashMap<>();
        inferredFieldsMap.put("field1", CLAClassifier.class);
        inferredFieldsMap.put("field2", SDRClassifier.class);
        inferredFieldsMap.put("field3", null);
        p.set(KEY.INFERRED_FIELDS, inferredFieldsMap);

        // Create MultiEncoder and add the fields' encoders to it
        MultiEncoder me = MultiEncoder.builder().name("").build();
        me.addEncoder(
                "field1",
                RandomDistributedScalarEncoder.builder().resolution(1).build()
        );
        me.addEncoder(
                "field2",
                RandomDistributedScalarEncoder.builder().resolution(1).build()
        );
        me.addEncoder(
                "field3",
                RandomDistributedScalarEncoder.builder().resolution(1).build()
        );

        // Create a Layer with Parameters and MultiEncoder
        Layer<Map<String, Object>> l = new Layer<>(
                p,
                me,
                new SpatialPooler(),
                new TemporalMemory(),
                true,
                null
        );

        // Make sure the makeClassifiers() method matches each
        // field to the specified Classifier type
        NamedTuple nt = l.makeClassifiers(l.getEncoder());
        assertEquals(nt.get("field1").getClass(), CLAClassifier.class);
        assertEquals(nt.get("field2").getClass(), SDRClassifier.class);
        assertEquals(nt.get("field3"), null);
    }

    @Test
    public void TestMakeClassifiersWithNoInferredFieldsKey() {
        // Setup Parameters
        Parameters p = Parameters.getAllDefaultParameters();

        // Create MultiEncoder
        MultiEncoder me = MultiEncoder.builder().name("").build();

        // Create a Layer with Parameters and MultiEncoder
        Layer<Map<String, Object>> l = new Layer<>(
                p,
                me,
                new SpatialPooler(),
                new TemporalMemory(),
                true,
                null
        );

        // Make sure the makeClassifiers() method throws exception due to
        // absence of KEY.INFERRED_FIELDS in the Parameters object
        try {
            l.makeClassifiers(l.getEncoder());
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("KEY.INFERRED_FIELDS"));
            assertTrue(e.getMessage().contains("null"));
            assertTrue(e.getMessage().contains("empty"));
        }
    }

    @Test
    public void TestMakeClassifiersWithInvalidInferredFieldsKey() {
         // Setup Parameters
        Parameters p = Parameters.getAllDefaultParameters();
        Map<String, Class<? extends Classifier>> inferredFieldsMap = new HashMap<>();
        inferredFieldsMap.put("field1", Classifier.class);
        p.set(KEY.INFERRED_FIELDS, inferredFieldsMap);

        // Create MultiEncoder and add the fields' encoders to it
        MultiEncoder me = MultiEncoder.builder().name("").build();
        me.addEncoder(
                "field1",
                RandomDistributedScalarEncoder.builder().resolution(1).build()
        );

        // Create a Layer with Parameters and MultiEncoder
        Layer<Map<String, Object>> l = new Layer<>(
                p,
                me,
                new SpatialPooler(),
                new TemporalMemory(),
                true,
                null
        );

        // Make sure the makeClassifiers() method throws exception due to
        // absence of KEY.INFERRED_FIELDS in the Parameters object
        try {
            NamedTuple nt = l.makeClassifiers(l.getEncoder());
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Invalid Classifier class token"));
        }
    }
}
