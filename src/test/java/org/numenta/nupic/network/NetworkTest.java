package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
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
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.MersenneTwister;

import rx.Subscriber;


public class NetworkTest {
    
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
    public void test() {
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
        
        r2.start();
        
        // Let run for 5 secs.
        try {
            r2.lookup("1").getLayerThread().join(5000);
            assertTrue(!Arrays.equals(topInference.getSparseActives(), 
                bottomInference.getSparseActives()));
            assertTrue(!Arrays.equals(topInference.getPredictedColumns(), 
                bottomInference.getPredictedColumns()));
            assertTrue(topInference.getPredictedColumns().length > 0);
            System.out.println("length = " + topInference.getPredictedColumns().length);
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

}
