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

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.SDR;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwister;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Tests the "heart and soul" of the Network API
 * 
 * @author DavidRay
 *
 */
public class LayerTest {

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
    public void testGetAllValues() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);

        // Test that we get the expected exception if there hasn't been any processing.
        try {
            l.getAllValues("dayOfWeek", 1);
            fail();
        }catch(Exception e) {
            assertEquals("Predictions not available. Either classifiers unspecified or inferencing has not yet begun.", e.getMessage());
        }

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                assertEquals(36, i.getSDR().length);
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
    }

    @Test
    public void testResetMethod() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        Layer<?> l = Network.createLayer("l1", p).add(new TemporalMemory());
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
                        FileSensor::create, 
                        SensorParams.create(
                                        Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv")));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        l.start();

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {
                assertTrue(l.isHalted());
                isHalted = true;
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {}
        });

        try {
            l.halt();
            l.getLayerThread().join();
            assertTrue(isHalted);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
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
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        l.start();

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                if(l.getSensor().getMetaInfo().isReset()) {
                    trueCount++;
                }
            }
        });

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
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        l.start();

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                if(l.getSensor().getMetaInfo().isReset()) {
                    seqResetCount++;
                }
            }
        });

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
        Publisher manual = Publisher.builder()
            .addHeader("timestamp,consumption")
            .addHeader("datetime,float")
            .addHeader("B")
            .build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, 
            SensorParams.create(
                Keys::obs, new Object[] {"name", manual}));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);

        HTMSensor<ObservableSensor<String[]>> htmSensor = (HTMSensor<ObservableSensor<String[]>>)sensor;

        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        l.start();

        l.subscribe(new Observer<Inference>() {
            int idx = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                switch(idx) {
                    case 0: assertEquals("[0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1]", Arrays.toString(output.getSDR()));
                    break;
                    case 1: assertEquals("[0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]", Arrays.toString(output.getSDR()));
                    break;
                    case 2: assertEquals("[1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", Arrays.toString(output.getSDR()));
                    break;
                }
                ++idx;
            }
        });

        try {
            String[] entries = { 
                "7/2/10 0:00,21.2",
                "7/2/10 1:00,34.0",
                "7/2/10 2:00,40.4",
            };

            // Send inputs through the observable
            for(String s : entries) {
                manual.onNext(s);
            }

            Thread.sleep(100);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
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
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        HTMSensor<ObservableSensor<String[]>> htmSensor = (HTMSensor<ObservableSensor<String[]>>)sensor;

        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        l.start();

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

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                assertEquals(input, Arrays.toString((int[])output.getLayerInput()));
            }
        });

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
        
    }

    @Test 
    public void testLayerWithGenericObservable() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        final int[] expected0 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] expected1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };

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
        l.subscribe(new Observer<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference i) {
                if(test == 0) {
                    assertTrue(Arrays.equals(expected0, i.getSDR()));
                    assertEquals("Interposed: [0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0]", i.getCustomObject());
                }
                if(test == 1) {
                    assertTrue(Arrays.equals(expected1, i.getSDR()));
                    assertEquals("Interposed: [0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0]", i.getCustomObject());
                }
                ++test; 
            }
        });

        // SHOULD RECEIVE BOTH
        // Now push some fake data through so that "onNext" is called above
        l.compute(inputs[0]);
        l.compute(inputs[1]);
    }

    @Test
    public void testBasicSetupEncoder_UsingSubscribe() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

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

        l.subscribe(new Observer<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                assertTrue(Arrays.equals(expected[seq++], output.getSDR()));
            }
        });

        Map<String, Object> inputs = new HashMap<String, Object>();
        for(double i = 0;i < 7;i++) {
            inputs.put("dayOfWeek", i);
            l.compute(inputs);
        }
    }

    @Test
    public void testBasicSetupEncoder_UsingObserve() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

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
        o.subscribe(new Observer<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                assertTrue(Arrays.equals(expected[seq++], output.getSDR()));
            }
        });

        Map<String, Object> inputs = new HashMap<String, Object>();
        for(double i = 0;i < 7;i++) {
            inputs.put("dayOfWeek", i);
            l.compute(inputs);
        }
    }

    @Test
    public void testBasicSetupEncoder_AUTO_MODE() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv")));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);

        final int[][] expected = new int[][] {
            { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1},
            { 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            { 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0}
        };

        ///////////////////////////////////////////////////////
        //              Test with 2 subscribers              //
        ///////////////////////////////////////////////////////
        l.observe().subscribe(new Observer<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
//                System.out.println("  seq = " + seq + ",    recNum = " + output.getRecordNum() + ",  expected = " + Arrays.toString(expected[seq]));
//                System.out.println("  seq = " + seq + ",    recNum = " + output.getRecordNum() + ",    output = " + Arrays.toString(output.getSDR()));
                assertTrue(Arrays.equals(expected[seq], output.getSDR()));
                seq++;
            }
        });

        l.observe().subscribe(new Observer<Inference>() {
            int seq2 = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
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
    }

    /**
     * Temporary test to test basic sequence mechanisms
     */
    @Test
    public void testBasicSetup_SpatialPooler_MANUAL_MODE() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        final int[] expected0 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] expected1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };

        Layer<int[]> l = new Layer<>(p, null, new SpatialPooler(), null, null, null);

        l.subscribe(new Observer<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
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
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);

        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;

        Network n = Network.create("test network", p);
        Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor).add(new SpatialPooler());
        
        final int[] expected0 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] expected1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
        l.subscribe(new Observer<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
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
    }
    
    /**
     * Temporary test to test basic sequence mechanisms
     */
    int seq = 0;
    @Test
    public void testBasicSetup_TemporalMemory_MANUAL_MODE() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

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

        l.subscribe(new Observer<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference output) {
                if(seq / 7 >= timeUntilStable) {
//                    System.out.println("seq: " + (seq) + "  --> " + (test) + "  output = " + Arrays.toString(output.getSDR()) +
//                        ", \t\t\t\t cols = " + Arrays.toString(SDR.asColumnIndices(output.getSDR(), l.getConnections().getCellsPerColumn())));
                    assertTrue(output.getSDR().length >= 8);
                }

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
    }

    @Test
    public void testBasicSetup_SPandTM() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        Layer<int[]> l = new Layer<>(p, null, new SpatialPooler(), new TemporalMemory(), null, null);

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) {}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                assertEquals(0, i.getSDR().length);
            }
        });

        // Now push some fake data through so that "onNext" is called above
        l.compute(inputs[0]);
        l.compute(inputs[1]);
    }

    @Test
    public void testSpatialPoolerPrimerDelay() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        final int[] expected0 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] expected1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };

        // First test without prime directive :-P
        Layer<int[]> l = new Layer<>(p, null, new SpatialPooler(), null, null, null);

        l.subscribe(new Observer<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
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
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42)); // due to static RNG we have to reset the sequence
        p.setParameterByKey(KEY.SP_PRIMER_DELAY, 1);

        Layer<int[]> l2 = new Layer<>(p, null, new SpatialPooler(), null, null, null);

        l2.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
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
    }

    /**
     * Simple test to verify data gets passed through the {@link CLAClassifier}
     * configured within the chain of components.
     */
    @Test
    public void testBasicClassifierSetup() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                assertEquals(36, i.getSDR().length);
            }
        });

        // Now push some fake data through so that "onNext" is called above
        Map<String, Object> multiInput = new HashMap<>();
        multiInput.put("dayOfWeek", 0.0);
        l.compute(multiInput);
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
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        p.setParameterByKey(KEY.SP_PRIMER_DELAY, PRIME_COUNT);

        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
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
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        p.setParameterByKey(KEY.SP_PRIMER_DELAY, PRIME_COUNT);

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

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                TOTAL++;
            }
        });

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                TOTAL++;
            }
        });

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
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
    }

    @Test
    public void testGetAllPredictions() {
        final int PRIME_COUNT = 35;
        final int NUM_CYCLES = 120;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        TOTAL = 0;

        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

        p.setParameterByKey(KEY.SP_PRIMER_DELAY, PRIME_COUNT);
        
        final int cellsPerColumn = (int)p.getParameterByKey(KEY.CELLS_PER_COLUMN);
        assertTrue(cellsPerColumn > 0);

        MultiEncoder me = MultiEncoder.builder().name("").build();
        final Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);

        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                TOTAL++;
                
                if(l.getPreviousPredictiveCells() != null) {
                    //UNCOMMENT TO VIEW STABILIZATION OF PREDICTED FIELDS
//                    System.out.println("recordNum: " + i.getRecordNum() + "  Day: " + ((Map<String, Object>)i.getLayerInput()).get("dayOfWeek") + "  -  " + 
//                       Arrays.toString(ArrayUtils.where(l.getFeedForwardActiveColumns(), ArrayUtils.WHERE_1)) +
//                         "   -   " + Arrays.toString(SDR.cellsAsColumnIndices(l.getPreviousPredictiveCells(), cellsPerColumn)));
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
        
        assertTrue(Arrays.equals(
            ArrayUtils.where(l.getFeedForwardActiveColumns(), ArrayUtils.WHERE_1),
                SDR.cellsAsColumnIndices(l.getPreviousPredictiveCells(), cellsPerColumn)));
    }

    /**
     * Test that a given layer can return an {@link Observable} capable of 
     * service multiple subscribers.
     */
    @Test
    public void testObservableRetrieval() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));

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
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.setParameterByKey(KEY.POTENTIAL_RADIUS, 200);
        p.setParameterByKey(KEY.INHIBITION_RADIUS, 50);
        p.setParameterByKey(KEY.GLOBAL_INHIBITION, true);

//        System.out.println(p);

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
                if(flowReceived) return; // No need to set this value multiple times

                flowReceived = i.getClassifiers().size() == 4 &&
                    i.getClassifiers().get("timestamp") != null &&
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
    
    @Test
    public void testMissingEncoderMap() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        //p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.setParameterByKey(KEY.POTENTIAL_RADIUS, 200);
        p.setParameterByKey(KEY.INHIBITION_RADIUS, 50);
        p.setParameterByKey(KEY.GLOBAL_INHIBITION, true);

//        System.out.println(p);

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
                if(flowReceived) return; // No need to set this value multiple times

                flowReceived = i.getClassifiers().size() == 4 &&
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
            assertFalse(flowReceived);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        ////////////////// Test catch with no Sensor ////////////////////
        
        p = NetworkTestHarness.getParameters().copy();
        //p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.setParameterByKey(KEY.POTENTIAL_RADIUS, 200);
        p.setParameterByKey(KEY.INHIBITION_RADIUS, 50);
        p.setParameterByKey(KEY.GLOBAL_INHIBITION, true);

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
                if(flowReceived) return; // No need to set this value multiple times

                flowReceived = i.getClassifiers().size() == 4 &&
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
            assertFalse(flowReceived);
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
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);
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
    
    String filterMessage = null;
    @Test
    public void testExplicitCloseFailure() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("2", p)
                    .add(Anomaly.create())
                    .add(new SpatialPooler())
                    .close()));
        
        // Set up a log filter to grab the next message.
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
        lc.addTurboFilter(new TurboFilter() {
            @Override
            public FilterReply decide(Marker arg0, Logger arg1, Level arg2, String arg3, Object[] arg4, Throwable arg5) {
                filterMessage = arg3;
                return FilterReply.ACCEPT;
            }
        });
        
        network.lookup("r1").lookup("2").close();
        
        // Test that the close() method exited after logging the correct message
        assertEquals("Close called on Layer r1:2 which is already closed.", filterMessage);
        // Make sure not to slow the entire test phase down by removing the filter
        lc.resetTurboFilterList();
    }
    
}
