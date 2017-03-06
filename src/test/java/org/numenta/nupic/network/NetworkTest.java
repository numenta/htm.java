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
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.FastRandom;
import org.numenta.nupic.util.MersenneTwister;
import static org.numenta.nupic.network.NetworkTestHarness.*;

import rx.Observer;
import rx.Subscriber;
import rx.observers.TestObserver;


public class NetworkTest extends ObservableTestBase {
    private int[][] dayMap = new int[][] { 
        new int[] { 1, 1, 0, 0, 0, 0, 0, 1 }, // Sunday
        new int[] { 1, 1, 1, 0, 0, 0, 0, 0 }, // Monday
        new int[] { 0, 1, 1, 1, 0, 0, 0, 0 }, // Tuesday
        new int[] { 0, 0, 1, 1, 1, 0, 0, 0 }, // Wednesday
        new int[] { 0, 0, 0, 1, 1, 1, 0, 0 }, // Thursday
        new int[] { 0, 0, 0, 0, 1, 1, 1, 0 }, // Friday
        new int[] { 0, 0, 0, 0, 0, 1, 1, 1 }, // Saturday
    };
    
    private BiFunction<Inference, Integer, Integer> dayOfWeekPrintout = createDayOfWeekInferencePrintout();
    
    @Test
    public void testResetMethod() {
        
        Parameters p = NetworkTestHarness.getParameters();
        Network network = new Network("ResetTestNetwork", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("l1", p).add(new TemporalMemory())));
        try {
            network.reset();
            assertTrue(network.lookup("r1").lookup("l1").hasTemporalMemory());
        }catch(Exception e) {
            fail();
        }
        
        network = new Network("ResetMethodTestNetwork", p)
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
        Network network = new Network("ResetRecordNumNetwork", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("l1", p).add(new TemporalMemory())));
        network.observe().subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
//                System.out.println("output = " + Arrays.toString(output.getSDR()));
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
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

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
    
    String onCompleteStr2 = null;
    @Test
    public void testBasicNetworkHalt_ThenRestart() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

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
                onCompleteStr2 = "On completed reached!";
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
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
            System.out.println(l);
        }
        
        assertEquals("On completed reached!", onCompleteStr2);
        
        ///////////////////////
        //     Now Restart   //
        ///////////////////////
        onCompleteStr2 = null;
        
        // Listen to network emissions
        network.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {
                onCompleteStr2 = "On completed reached!";
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
//                System.out.println("second listener: " + i.getRecordNum());
                lines.add(i.getRecordNum() + "," + 
                    i.getClassifierInput().get("consumption").get("inputValue") + "," + i.getAnomalyScore());
                
                if(i.getRecordNum() == 19) {
                    network.halt();
                }
            }
        });
        
        network.restart();
        
        // Test network output
        try {
            Region r1 = network.lookup("r1");
            r1.lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        assertEquals(20, lines.size());
        
        i = 0;
        for(String l : lines) {
            String[] sa = l.split("[\\s]*\\,[\\s]*");
            assertEquals(3, sa.length);
            assertEquals(i++, Integer.parseInt(sa[0]));
            System.out.println(l);
        }
        
        assertEquals("On completed reached!", onCompleteStr2);
    }
    
    boolean expectedDataFlag = true;
    String failMessage;
    @Test
    public void testBasicNetworkHalt_ThenRestart_TighterExpectation() {
        final int NUM_CYCLES = 600;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        
        ///////////////////////////////////////
        //   Run until CYCLE 284, then halt  //
        ///////////////////////////////////////
        Network network = getLoadedDayOfWeekNetwork();
        int cellsPerCol = (int)network.getParameters().get(KEY.CELLS_PER_COLUMN);
        
        network.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
                /** see {@link #createDayOfWeekInferencePrintout()} */
                int cycle = dayOfWeekPrintout.apply(inf, cellsPerCol);
                if(cycle == 284) {
                    System.out.println("halting publisher = " + network.getPublisher());
                    network.halt();
                }
            }
        });
        
        Publisher pub = network.getPublisher();
        
        network.start();
        
        int cycleCount = 0;
        for(;cycleCount < NUM_CYCLES;cycleCount++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                pub.onNext("" + j);
            }
            
            network.reset();
            
            if(cycleCount == 284) {
                break;
            }
        }
        
        // Test network output
        try {
            Region r1 = network.lookup("r1");
            r1.lookup("1").getLayerThread().join(2000);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        // Announce new start
        System.out.println("\n\n\n Network Restart \n\n\n");
        
        
        ///////////////////////
        //     Now Restart   //
        ///////////////////////
        
        // 1. Re-Attach Observer
        // 2. Restart Network
        
        network.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
                /** see {@link #createDayOfWeekInferencePrintout()} */
                
                dayOfWeekPrintout.apply(inf, cellsPerCol);
               
                ////////////////////////////////////////////////////////////
                // Ensure the records pick up precisely where we left off //
                ////////////////////////////////////////////////////////////
                if(inf.getRecordNum() == 1975) {
                    expectedDataFlag = true;
                }
            }
        });
        
        network.halt();
        try { 
            network.lookup("r1").lookup("1").getLayerThread().join(3000);
            // Add a little more wait time
            Thread.sleep(3000);
        }catch(Exception e) { e.printStackTrace(); }
        network.restart();
        
        Publisher newPub = network.getPublisher();
        
        // Assert that we have a new Publisher being created in the background upon restart()
        assertFalse(pub == newPub);
        
        for(;cycleCount < NUM_CYCLES;cycleCount++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                newPub.onNext("" + j);
            }
            network.reset();
        }
        
        newPub.onComplete();
                
        try {
            Region r1 = network.lookup("r1");
            r1.lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        if(!expectedDataFlag) {
            fail(failMessage);
        }
    }
    
    @Test
    public void testBasicNetworkRunAWhileThenHalt() {
        onCompleteStr = null;
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

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
        p.setPotentialRadius(16);
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));
        
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
                if(r1.getHead().getInference().getPredictiveCells().size() > 0 && 
                    r2.getHead().getInference().getPredictiveCells().size() > 0) {
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
//            System.out.println("top ff = " + Arrays.toString(topInference.getFeedForwardSparseActives()));
//            System.out.println("bot ff = " + Arrays.toString(bottomInference.getFeedForwardSparseActives()));
//            System.out.println("top pred = " + topInference.getPredictiveCells());
//            System.out.println("bot pred = " + bottomInference.getPredictiveCells());
//            System.out.println("top active = " + topInference.getActiveCells());
//            System.out.println("bot active = " + bottomInference.getActiveCells());
            assertTrue(!topInference.getPredictiveCells().equals(bottomInference.getPredictiveCells()));
            assertTrue(topInference.getPredictiveCells().size() > 0);
            assertTrue(bottomInference.getPredictiveCells().size() > 0);
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
        p.set(KEY.RANDOM, new MersenneTwister(42));
        
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
        p.set(KEY.GLOBAL_INHIBITION, true);
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 30 });
        p.set(KEY.SYN_PERM_INACTIVE_DEC, 0.008);
        p.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        p.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        p.set(KEY.SYN_PERM_CONNECTED, 0.1);
        p.set(KEY.PERMANENCE_INCREMENT, 0.10);
        p.set(KEY.PERMANENCE_DECREMENT, 0.10);
        p.set(KEY.MAX_BOOST, 1.0);
        p.set(KEY.DUTY_CYCLE_PERIOD, 7);
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));
        
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
//                Set<Cell> prevPred = i.getPreviousPredictiveCells();
//                if(prevPred == null) {
//                    prevPred = Collections.emptySet();
//                }
//                System.out.println("Day: " + r1.getInput() + " - predictions: " + Arrays.toString(SDR.cellsAsColumnIndices(prevPred, 6)) +
//                    "   -   " + Arrays.toString(i.getFeedForwardSparseActives()) + " - " + 
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
        TestObserver<Inference> tester;
        r1.observe().subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onNext(Inference i) {
                int nextDay = ((int)Math.rint(((Number)i.getClassification("dayOfWeek").getMostProbableValue(1)).doubleValue()));
                assertEquals(6, nextDay);
            }
        });
        
        multiInput.put("dayOfWeek", 5.0);
        n.compute(multiInput);
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }
    
    @Test
    public void testSynchronousBlockingComputeCall() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 30 });
        p.set(KEY.SYN_PERM_INACTIVE_DEC, 0.1);
        p.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        p.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        p.set(KEY.SYN_PERM_CONNECTED, 0.4);
        p.set(KEY.MAX_BOOST, 10.0);
        p.set(KEY.DUTY_CYCLE_PERIOD, 7);
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));
        
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
                if(inf.getPredictiveCells().size() > 6) {
                    assertTrue(inf.getPredictiveCells() != null);
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
    
    @Test
    public void testThreadedStartFlagging() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 30 });
        p.set(KEY.SYN_PERM_INACTIVE_DEC, 0.1);
        p.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        p.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        p.set(KEY.SYN_PERM_CONNECTED, 0.4);
        p.set(KEY.MAX_BOOST, 10.0);
        p.set(KEY.DUTY_CYCLE_PERIOD, 7);
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));
        
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
        
        assertFalse(n.isThreadedOperation());
        n.start();
        assertFalse(n.isThreadedOperation());
        
        //////////////////////////////////////////////////////
        // Add a Sensor which should allow Network to start //
        //////////////////////////////////////////////////////
        p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));
        n = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE))
                .add(Network.createLayer("2", p)
                    .add(Anomaly.create(params)))
                .add(Network.createLayer("3", p)
                    .add(new TemporalMemory()))
                .add(Network.createLayer("4", p)
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv")))))
                .connect("1", "2")
                .connect("2", "3")
                .connect("3", "4"));
        assertFalse(n.isThreadedOperation());
        n.start();
        assertTrue(n.isThreadedOperation());
        
        try {
            p = NetworkTestHarness.getParameters();
            p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
            p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));
            n = Network.create("test network", p)
                .add(Network.createRegion("r1")
                    .add(Network.createLayer("1", p)
                        .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                        .add(new TemporalMemory())
                        .add(new SpatialPooler())
                        .add(Sensor.create(FileSensor::create, SensorParams.create(
                            Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))));
            
            n.start();
            
            n.computeImmediate(new HashMap<String, Object>());
            
            // SHOULD FAIL HERE WITH EXPECTED EXCEPTION
            fail();
        }catch(Exception e) {
            assertEquals("Cannot call computeImmediate() when Network has been started.", e.getMessage());
        }
    }
    
    double anomaly = 1;
    boolean completed = false;
    @Test
    public void testObservableWithCoordinateEncoder() {
        Publisher manual = Publisher.builder()
            .addHeader("timestamp,consumption,location")
            .addHeader("datetime,float,geo")
            .addHeader("T,,").build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, "", manual));
                    
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getGeospatialTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        HTMSensor<ObservableSensor<String[]>> htmSensor = (HTMSensor<ObservableSensor<String[]>>)sensor;

        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(htmSensor)));

        network.start();

        network.observe().subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {
                assertEquals(0, anomaly, 0);
                completed = true;
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                 //System.out.println(output.getRecordNum() + ":  input = " + Arrays.toString(output.getEncoding()));//output = " + Arrays.toString(output.getSDR()) + ", " + output.getAnomalyScore());
                if(output.getAnomalyScore() < anomaly) {
                    anomaly = output.getAnomalyScore();
//                    System.out.println("anomaly = " + anomaly);
                }
            }
        });
        
        int x = 0;
        for(int i = 0;i < 100;i++) {
            x = i % 10;
            manual.onNext("7/12/10 13:10,35.3,40.6457;-73.7" + x + "692;" + x); //5 = meters per second
        }
        
        manual.onComplete();
        
        Layer<?> l = network.lookup("r1").lookup("1");
        try {
            l.getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        assertTrue(completed);
        
    }
    
    String errorMessage = null;
    @Test
    public void testObservableWithCoordinateEncoder_NEGATIVE() {
        Publisher manual = Publisher.builder()
            .addHeader("timestamp,consumption,location")
            .addHeader("datetime,float,geo")
            .addHeader("T,,").build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, "", manual));
                    
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getGeospatialTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("", CLAClassifier.class));

        HTMSensor<ObservableSensor<String[]>> htmSensor = (HTMSensor<ObservableSensor<String[]>>)sensor;

        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(htmSensor)));

        TestObserver<Inference> tester;
        network.observe().subscribe(tester = new TestObserver<Inference>() {
            @Override public void onCompleted() {
                //Should never happen here.
                assertEquals(0, anomaly, 0);
                completed = true;
                
                super.onCompleted();
            }
            @Override public void onError(Throwable e) { 
                super.onError(e);
                errorMessage = e.getMessage();
                network.halt();
            }
            @Override public void onNext(Inference output) {}
        });
        
        network.start();
        
        int x = 0;
        for(int i = 0;i < 100;i++) {
            x = i % 10;
            manual.onNext("7/12/10 13:10,35.3,40.6457;-73.7" + x + "692;" + x); //1st "x" is attempt to vary coords, 2nd "x" = meters per second
        }
        
        manual.onComplete();
        
        Layer<?> l = network.lookup("r1").lookup("1");
        try {
            l.getLayerThread().join();
        }catch(Exception e) {
            assertEquals(InterruptedException.class, e.getClass());
        }
        
        // Assert onNext condition never gets set
        assertFalse(completed);
        assertEquals("Cannot autoclassify with raw array input or  " +
            "Coordinate based encoders... Remove auto classify setting.", errorMessage);
        
        assertTrue(hasErrors(tester));
    }
    
    @Test
    public void testPotentialRadiusFollowsInputWidth() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.INPUT_DIMENSIONS, new int[] { 200 });
        p.set(KEY.RANDOM, new MersenneTwister(42));

        Network network = Network.create("test network", p)
                .add(Network.createRegion("r1")
                        .add(Network.createLayer("2", p)
                                .add(Anomaly.create())
                                .add(new TemporalMemory())
                                .add(new SpatialPooler())
                                .close()));

        Region r1 = network.lookup("r1");
        Layer<?> layer2 = r1.lookup("2");

        int width = layer2.calculateInputWidth();
        assertEquals(200, width);
        assertEquals(200, layer2.getConnections().getPotentialRadius());
    }
    
    ///////////////////////////////////////////////////////////////////////////////////
    //    Tests of Calculate Input Width for inter-regional and inter-layer calcs    //
    ///////////////////////////////////////////////////////////////////////////////////
    @Test
    public void testCalculateInputWidth_NoPrevLayer_UpstreamRegion_with_TM() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

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
        Layer<?> layer2 = r1.lookup("2");
        
        int width = layer2.calculateInputWidth();
        assertEquals(65536, width);
    }
    
    @Test
    public void testCalculateInputWidth_NoPrevLayer_UpstreamRegion_without_TM() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("2", p)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())))
            .add(Network.createRegion("r2")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))))
            .connect("r1", "r2");
        
        Region r1 = network.lookup("r1");
        Layer<?> layer2 = r1.lookup("2");
        
        int width = layer2.calculateInputWidth();
        assertEquals(2048, width);
    }
    
    @Test
    public void testCalculateInputWidth_NoPrevLayer_NoPrevRegion_andTM() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                    .add(Network.createLayer("2", p)
                            .add(Anomaly.create())
                            .add(new TemporalMemory())
                            .close()));
        
        Region r1 = network.lookup("r1");
        Layer<?> layer2 = r1.lookup("2");
        
        int width = layer2.calculateInputWidth();
        assertEquals(65536, width);
    }

    @Test
    public void testCalculateInputWidth_NoPrevLayer_NoPrevRegion_andSPTM() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        Network network = Network.create("test network", p)
                .add(Network.createRegion("r1")
                        .add(Network.createLayer("2", p)
                                .add(Anomaly.create())
                                .add(new TemporalMemory())
                                .add(new SpatialPooler())
                                .close()));

        Region r1 = network.lookup("r1");
        Layer<?> layer2 = r1.lookup("2");

        int width = layer2.calculateInputWidth();
        assertEquals(8, width);
        assertEquals(8, layer2.getConnections().getPotentialRadius());
    }

    @Test
    public void testCalculateInputWidth_NoPrevLayer_NoPrevRegion_andNoTM() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("2", p)
                    .add(Anomaly.create())
                    .add(new SpatialPooler())
                    .close()));
        
        Region r1 = network.lookup("r1");
        Layer<?> layer2 = r1.lookup("2");
        
        int width = layer2.calculateInputWidth();
        assertEquals(8, width);
        assertEquals(8, layer2.getConnections().getPotentialRadius());
    }
    
    @Test
    public void testCalculateInputWidth_WithPrevLayer_WithTM() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .add(Anomaly.create())
                    .add(new SpatialPooler()))
                .add(Network.createLayer("2", p)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler()))
                .connect("1", "2"));
                    
        Region r1 = network.lookup("r1");
        Layer<?> layer1 = r1.lookup("1");
        
        int width = layer1.calculateInputWidth();
        assertEquals(65536, width);
    }
    
    @Test
    public void testCalculateInputWidth_WithPrevLayer_NoTM() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .add(Anomaly.create())
                    .add(new SpatialPooler()))
                .add(Network.createLayer("2", p)
                    .add(new SpatialPooler()))
                .connect("1", "2"));
                    
        Region r1 = network.lookup("r1");
        Layer<?> layer1 = r1.lookup("1");
        
        int width = layer1.calculateInputWidth();
        assertEquals(2048, width);
    }

    @Test
    public void closeTest() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));

        Region region1 = Network.createRegion("region1");
        Layer<?> layer1 = Network.createLayer("layer1", p);
        region1.add(layer1);

        Region region2 = Network.createRegion("region2");
        Layer<?> layer2 = Network.createLayer("layer2", p);
        region2.add(layer2);

        Network network = Network.create("test network", p);

        // Calling close on an empty Network should not throw any Exceptions
        network.close();

        // Calling close on a Network with a single unclosed Region
        network.add(region1);
        network.close();

        assertTrue("Region 1 did not close, after closing Network", region1.isClosed());
        assertTrue("Layer 1 did not close, after closing Network", layer1.isClosed());

        // Calling close on a Network with two regions, one of which is closed
        network.add(region2);
        network.close();

        assertTrue("Region 1 did not close, after closing Network with 2 Regions", region1.isClosed());
        assertTrue("Layer 1 did not close, after closing Network with 2 Regions", layer1.isClosed());
        assertTrue("Region 2 did not close, after closing Network with 2 Regions", region2.isClosed());
        assertTrue("Layer 2 did not close, after closing Network with 2 Regions", layer2.isClosed());

    }
    
    @Test
    public void testLearn() {
        Region region = Network.createRegion("Region 1");
        Layer<?> layer = Network.createLayer("Layer 2/3", Parameters.getAllDefaultParameters());
        Network network = Network.create("Network 1", Parameters.getAllDefaultParameters());
        region.add(layer);
        network.add(region);
        
        // Close must be called to have this work
        region.close();
        
        network.setLearn(true);
        assertTrue(network.isLearn()); //true
        assertTrue(region.isLearn()); //true
        assertTrue(layer.isLearn()); //true
    
        network.setLearn(false);
        assertFalse(network.isLearn()); //false
        assertFalse(region.isLearn()); //false
        assertFalse(layer.isLearn());
    }

    
    ////////////////////////////////////////
    //         Utility Methods            //
    ////////////////////////////////////////
    
    Publisher pub = null;
    private Network getLoadedDayOfWeekNetwork() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new FastRandom(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", 
                PublisherSupplier.builder()
                    .addHeader("dayOfWeek")
                    .addHeader("number")
                    .addHeader("B").build() }));
        
        Network network = Network.create("test network", p).add(Network.createRegion("r1")
            .add(Network.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, true)
                .add(Anomaly.create())
                .add(new TemporalMemory())
                .add(new SpatialPooler())
                .add(sensor)));
        
        return network;
    }

    private BiFunction<Inference, Integer, Integer> createDayOfWeekInferencePrintout() {
        return new BiFunction<Inference, Integer, Integer>() {
            private int cycles = 1;
              
            public Integer apply(Inference inf, Integer cellsPerColumn) {
//                Classification<Object> result = inf.getClassification("dayOfWeek");
                double day = mapToInputData((int[])inf.getLayerInput());
                if(day == 1.0) {
//                    System.out.println("\n=========================");
//                    System.out.println("CYCLE: " + cycles);
                    cycles++;
                }
                
//                System.out.println("RECORD_NUM: " + inf.getRecordNum());
//                System.out.println("ScalarEncoder Input = " + day);
//                System.out.println("ScalarEncoder Output = " + Arrays.toString(inf.getEncoding()));
//                System.out.println("SpatialPooler Output = " + Arrays.toString(inf.getFeedForwardActiveColumns()));
//                
//                if(inf.getPreviousPredictiveCells() != null)
//                    System.out.println("TemporalMemory Previous Prediction = " + 
//                        Arrays.toString(SDR.cellsAsColumnIndices(inf.getPreviousPredictiveCells(), cellsPerColumn)));
//                
//                System.out.println("TemporalMemory Actives = " + Arrays.toString(SDR.asColumnIndices(inf.getSDR(), cellsPerColumn)));
//                
//                System.out.print("CLAClassifier prediction = " + 
//                    stringValue((Double)result.getMostProbableValue(1)) + " --> " + ((Double)result.getMostProbableValue(1)));
//                
//                System.out.println("  |  CLAClassifier 1 step prob = " + Arrays.toString(result.getStats(1)) + "\n");
                
                return cycles;
            }
        };
    }
    
    private double mapToInputData(int[] encoding) {
        for(int i = 0;i < dayMap.length;i++) {
            if(Arrays.equals(encoding, dayMap[i])) {
                return i + 1;
            }
        }
        return -1;
    }
    
    public Stream<String> makeStream() {
        return Stream.of(
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,16.4",
            "7/2/10 2:00,4.7",
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
}
