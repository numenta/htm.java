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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.MersenneTwister;

import rx.Observer;
import rx.Subscriber;


public class NetworkTest {
    @Test
    public void testResetMethod() {
        
        Parameters p = NetworkTestHarness.getParameters();
        Network network = new Network("", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("l1", p).add(new TemporalMemory())));
        try {
            network.reset();
            assertTrue(network.lookup("r1").lookup("l1").hasTemporalMemory());
        }catch(Exception e) {
            fail();
        }
        
        network = new Network("", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("l1", p).add(new SpatialPooler())));
        try {
            network.reset();
            assertFalse(network.lookup("r1").lookup("l1").hasTemporalMemory());
        }catch(Exception e) {
            fail();
        }
    }
    
    @Test
    public void testResetRecordNum() {
        Parameters p = NetworkTestHarness.getParameters();
        Network network = new Network("", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("l1", p).add(new TemporalMemory())));
        network.observe().subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                System.out.println("output = " + Arrays.toString(output.getSDR()));
            }
        });
        
        network.compute(new int[] { 2,3,4 });
        network.compute(new int[] { 2,3,4 });
        assertEquals(1, network.lookup("r1").lookup("l1").getRecordNum());
        
        network.resetRecordNum();
        assertEquals(0, network.lookup("r1").lookup("l1").getRecordNum());
    }
    
    @Test
    public void testAdd() {
        Parameters p = NetworkTestHarness.getParameters();
        Network network = Network.create("test", NetworkTestHarness.getParameters());
        
        // Add Layers to regions but regions not yet added to Network
        Region r1 = Network.createRegion("r1").add(Network.createLayer("l", p).add(new SpatialPooler()));
        Region r2 = Network.createRegion("r2").add(Network.createLayer("l", p).add(new SpatialPooler()));
        Region r3 = Network.createRegion("r3").add(Network.createLayer("l", p).add(new SpatialPooler()));
        Region r4 = Network.createRegion("r4").add(Network.createLayer("l", p).add(new SpatialPooler()));
        Region r5 = Network.createRegion("r5").add(Network.createLayer("l", p).add(new SpatialPooler()));
        
        Region[] regions = new Region[] { r1, r2, r3, r4, r5 };
        for(Region r : regions) {
            assertNull(network.lookup(r.getName()));
        }
        
        // Add the regions to the network
        for(Region r : regions) {
            network.add(r);
        }
        
        String[] names = new String[] { "r1","r2","r3","r4","r5" };
        int i = 0;
        for(Region r : regions) {
            assertNotNull(network.lookup(r.getName()));
            assertEquals(names[i++], r.getName());
        }
    }
    
    @Test
    public void testConnect() {
        Parameters p = NetworkTestHarness.getParameters();
        Network network = Network.create("test", NetworkTestHarness.getParameters());
        
        Region r1 = Network.createRegion("r1").add(Network.createLayer("l", p).add(new SpatialPooler()));
        Region r2 = Network.createRegion("r2").add(Network.createLayer("l", p).add(new SpatialPooler()));
        Region r3 = Network.createRegion("r3").add(Network.createLayer("l", p).add(new SpatialPooler()));
        Region r4 = Network.createRegion("r4").add(Network.createLayer("l", p).add(new SpatialPooler()));
        Region r5 = Network.createRegion("r5").add(Network.createLayer("l", p).add(new SpatialPooler()));
        
        try {
            network.connect("r1", "r2");
            fail();
        }catch(Exception e) {
            assertEquals("Region with name: r2 not added to Network.", e.getMessage());
        }
        
        Region[] regions = new Region[] { r1, r2, r3, r4, r5 };
        for(Region r : regions) {
            network.add(r);
        }
        
        for(int i = 1;i < regions.length;i++) {
            try {
                network.connect(regions[i - 1].getName(), regions[i].getName());
            }catch(Exception e) {
                fail();
            }
        }
        
        Region upstream = r1;
        Region tail = r1;
        while((tail = tail.getUpstreamRegion()) != null) {
            upstream = tail;
        }
        
        // Assert that the connect method sets the upstream region on all regions
        assertEquals(regions[4], upstream);
        
        Region downstream = r5;
        Region head = r5;
        while((head = head.getDownstreamRegion()) != null) {
            downstream = head;
        }
        
        // Assert that the connect method sets the upstream region on all regions
        assertEquals(regions[0], downstream);
        assertEquals(network.getHead(), downstream);
    }
    
    String onCompleteStr = null;
    @Test
    public void testBasicNetworkHaltGetsOnComplete() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        // Create a Network
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))));
        
        final List<String> lines = new ArrayList<>();
        
        // Listen to network emissions
        network.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {
                onCompleteStr = "On completed reached!";
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
//                System.out.println(Arrays.toString(i.getSDR()));
//                System.out.println(i.getRecordNum() + "," + 
//                    i.getClassifierInput().get("consumption").get("inputValue") + "," + i.getAnomalyScore());
                lines.add(i.getRecordNum() + "," + 
                    i.getClassifierInput().get("consumption").get("inputValue") + "," + i.getAnomalyScore());
                
                if(i.getRecordNum() == 9) {
                    network.halt();
                }
            }
        });
        
        // Start the network
        network.start();
        
        // Test network output
        try {
            Region r1 = network.lookup("r1");
            r1.lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        assertEquals(10, lines.size());
        int i = 0;
        for(String l : lines) {
            String[] sa = l.split("[\\s]*\\,[\\s]*");
            assertEquals(3, sa.length);
            assertEquals(i++, Integer.parseInt(sa[0]));
        }
        
        assertEquals("On completed reached!", onCompleteStr);
    }
    
    @Test
    public void testBasicNetworkRunAWhileThenHalt() {
        onCompleteStr = null;
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        // Create a Network
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))));
        
        final List<String> lines = new ArrayList<>();
        
        // Listen to network emissions
        network.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {
                onCompleteStr = "On completed reached!";
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
//                System.out.println(Arrays.toString(i.getSDR()));
//                System.out.println(i.getRecordNum() + "," + 
//                    i.getClassifierInput().get("consumption").get("inputValue") + "," + i.getAnomalyScore());
                lines.add(i.getRecordNum() + "," + 
                    i.getClassifierInput().get("consumption").get("inputValue") + "," + i.getAnomalyScore());
                
                if(i.getRecordNum() == 1000) {
                    network.halt();
                }
            }
        });
        
        // Start the network
        network.start();
        
        // Test network output
        try {
            Region r1 = network.lookup("r1");
            r1.lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        assertEquals(1001, lines.size());
        int i = 0;
        for(String l : lines) {
            String[] sa = l.split("[\\s]*\\,[\\s]*");
            assertEquals(3, sa.length);
            assertEquals(i++, Integer.parseInt(sa[0]));
        }
        
        assertEquals("On completed reached!", onCompleteStr);
    }
    
    
    ManualInput netInference = null;
    ManualInput topInference = null;
    ManualInput bottomInference = null;
    @Test
    public void testRegionHierarchies() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("2", p)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())))
            .add(Network.createRegion("r2")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))))
            .connect("r1", "r2");
        
        Region r1 = network.lookup("r1");
        Region r2 = network.lookup("r2");
        
        network.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                netInference = (ManualInput)i;
                if(netInference.getPredictedColumns().length > 15) {
                    network.halt();
                }
            }
        });
        
        r1.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                topInference = (ManualInput)i;
            }
        });
        r2.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                bottomInference = (ManualInput)i;
            }
        });
        
        network.start();
        
        // Let run for 5 secs.
        try {
            r2.lookup("1").getLayerThread().join();//5000);
            assertTrue(!Arrays.equals(topInference.getSparseActives(), 
                bottomInference.getSparseActives()));
            assertTrue(!Arrays.equals(topInference.getPredictedColumns(), 
                bottomInference.getPredictedColumns()));
            assertTrue(topInference.getPredictedColumns().length > 0);
            assertTrue(bottomInference.getPredictedColumns().length > 0);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test that a null {@link Assembly.Mode} results in exception
     */
    @Test
    public void testFluentBuildSemantics() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Map<String, Object> anomalyParams = new HashMap<>();
        anomalyParams.put(KEY_MODE, Mode.LIKELIHOOD);
        
        try {
            // Idea: Build up ResourceLocator paths in fluent style such as:
            // Layer.using(
            //     ResourceLocator.addPath("...") // Adds a search path for later mentioning terminal resources (i.e. files)
            //         .addPath("...")
            //         .addPath("..."))
            //     .add(new SpatialPooler())
            //     ...
            Network.create("test network", p)   // Add Network.add() method for chaining region adds
                .add(Network.createRegion("r1")             // Add version of createRegion(String name) for later connecting by name
                    .add(Network.createLayer("2/3", p)      // so that regions can be added and connecting in one long chain.
                        .using(new Connections())           // Test adding connections before elements which use them
                        .add(Sensor.create(FileSensor::create, SensorParams.create(
                            Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))
                        .add(new SpatialPooler())
                        .add(new TemporalMemory())
                        .add(Anomaly.create(anomalyParams))
                )
                    .add(Network.createLayer("1", p)            // Add another Layer, and the Region internally connects it to the 
                        .add(new SpatialPooler())               // previously added Layer
                        .using(new Connections())               // Test adding connections after one element and before another
                        .add(new TemporalMemory())
                        .add(Anomaly.create(anomalyParams))
                ))            
                .add(Network.createRegion("r2")
                    .add(Network.createLayer("2/3", p)
                        .add(new SpatialPooler())
                        .using(new Connections()) // Test adding connections after one element and before another
                        .add(new TemporalMemory())
                        .add(Anomaly.create(anomalyParams))
                ))
                .add(Network.createRegion("r3")
                    .add(Network.createLayer("1", p)
                        .add(new SpatialPooler())
                        .add(new TemporalMemory())
                        .add(Anomaly.create(anomalyParams))
                            .using(new Connections()) // Test adding connections after elements which use them.
                ))
                
                .connect("r1", "r2")
                .connect("r2", "r3");
        }catch(Exception e) {
            e.printStackTrace();
        }
        
    }
    
    @Test
    public void testNetworkComputeWithNoSensor() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 30 });
        p.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.1);
        p.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        p.setParameterByKey(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        p.setParameterByKey(KEY.SYN_PERM_CONNECTED, 0.4);
        p.setParameterByKey(KEY.MAX_BOOST, 10.0);
        p.setParameterByKey(KEY.DUTY_CYCLE_PERIOD, 7);
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        
        Network n = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE))
                .add(Network.createLayer("2", p)
                    .add(Anomaly.create(params)))
                .add(Network.createLayer("3", p)
                    .add(new TemporalMemory()))
                .add(Network.createLayer("4", p)
                    .add(new SpatialPooler())
                    .add(MultiEncoder.builder().name("").build()))
                .connect("1", "2")
                .connect("2", "3")
                .connect("3", "4"));
        
        Region r1 = n.lookup("r1");
        r1.lookup("3").using(r1.lookup("4").getConnections()); // How to share Connections object between Layers
        
        r1.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                // UNCOMMENT TO VIEW STABILIZATION OF PREDICTED FIELDS
//                System.out.println("Day: " + r1.getInput() + " - predictions: " + Arrays.toString(i.getPreviousPrediction()) +
//                    "   -   " + Arrays.toString(i.getSparseActives()) + " - " + 
//                    ((int)Math.rint(((Number)i.getClassification("dayOfWeek").getMostProbableValue(1)).doubleValue())));
            }
        });
       
        final int NUM_CYCLES = 400;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        Map<String, Object> multiInput = new HashMap<>();
        for(int i = 0;i < NUM_CYCLES;i++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                multiInput.put("dayOfWeek", j);
                r1.compute(multiInput);
            }
            n.reset();
        }
        
        // Test that we get proper output after prediction stabilization
        r1.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                int nextDay = ((int)Math.rint(((Number)i.getClassification("dayOfWeek").getMostProbableValue(1)).doubleValue()));
                assertEquals(6, nextDay);
            }
        });
        
        multiInput.put("dayOfWeek", 5.0);
        n.compute(multiInput);
    }
    
    @Test
    public void testSynchronousBlockingComputeCall() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 30 });
        p.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.1);
        p.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        p.setParameterByKey(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        p.setParameterByKey(KEY.SYN_PERM_CONNECTED, 0.4);
        p.setParameterByKey(KEY.MAX_BOOST, 10.0);
        p.setParameterByKey(KEY.DUTY_CYCLE_PERIOD, 7);
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        
        Network n = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(MultiEncoder.builder().name("").build())));
        
        boolean gotResult = false;
        final int NUM_CYCLES = 400;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        Map<String, Object> multiInput = new HashMap<>();
        for(int i = 0;i < NUM_CYCLES;i++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                multiInput.put("dayOfWeek", j);
                Inference inf = n.computeImmediate(multiInput);
                if(inf.getPredictedColumns().length > 6) {
                    assertTrue(inf.getPredictedColumns() != null);
                    // Make sure we've gotten all the responses
                    assertEquals((i * 7) + (int)j, inf.getRecordNum());
                    gotResult = true;
                    break;
                }
            }
            if(gotResult) {
                break;
            }
        }
         
        assertTrue(gotResult);
    }
    
}
