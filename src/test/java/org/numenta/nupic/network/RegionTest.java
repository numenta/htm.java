package org.numenta.nupic.network;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.MersenneTwister;

import rx.Subscriber;


public class RegionTest {
    
    @Test
    public void testClose() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("4", p)
                .add(MultiEncoder.builder().name("").build()))
            .close();
        
        try {
            r1.add(n.createLayer("5", p));
            fail();
        }catch(Exception e) {
            assertTrue(e.getClass().isAssignableFrom(IllegalStateException.class));
            assertEquals("Cannot add Layers when Region has already been closed.", e.getMessage());
        }
    }
    
    @Test
    public void testAdd() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("4", p)
                .add(MultiEncoder.builder().name("").build()));
        
        Layer<?>layer4 = r1.lookup("4");
        assertNotNull(layer4);
        assertEquals("r1:4", layer4.getName());
        
        try {
            r1.add(n.createLayer("4"));
            fail();
        }catch(Exception e) {
            assertTrue(e.getClass().isAssignableFrom(IllegalArgumentException.class));
            assertEquals("A Layer with the name: 4 has already been added.", e.getMessage());
        }
    }
    
    boolean isHalted;
    @Test
    public void testHalt() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE))
            .add(n.createLayer("2", p)
                .add(Anomaly.create(params)))
            .add(n.createLayer("3", p)
                .add(new TemporalMemory()))
            .add(n.createLayer("4", p)
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("days-of-week.csv"))))
                .add(new SpatialPooler()))
            .connect("1", "2")
            .connect("2", "3")
            .connect("3", "4")
            .close();
        
        r1.observe().subscribe(new Subscriber<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {
                System.out.println("onCompleted() called");
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                if(seq == 2) {
                    isHalted = true;
                }
                seq++;
                System.out.println("output: " + i.getSDR());
            }
        });
        
        (new Thread() {
            public void run() {
                while(!isHalted) {
                    try { Thread.sleep(1); }catch(Exception e) {e.printStackTrace();}
                }
                r1.stop();
            }
        }).start();
        
        r1.start();
        
        try {
            r1.lookup("4").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Test that we automatically calculate the input dimensions despite
     * there being an improper Parameter setting.
     */
    @Test
    public void testInputDimensionsAutomaticallyInferredFromEncoderWidth() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        // Purposefully set this to be wrong
        p.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 40, 40 });
        
        Network n = Network.create("test network", p);
        n.createRegion("r1")
            .add(n.createLayer("4", p)
                .add(MultiEncoder.builder().name("").build()))
            .close();
        
        // Should correct the above ( {40,40} ) to have only one dimension whose width is 8 ( {8} )
        assertTrue(Arrays.equals(new int[] { 8 }, (int[])p.getParameterByKey(KEY.INPUT_DIMENSIONS)));
    }
    
    /**
     * Test encoder bubbles up to L1
     */
    @Test
    public void testEncoderPassesUpToTopLayer() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE))
            .add(n.createLayer("2", p)
                .add(Anomaly.create(params)))
            .add(n.createLayer("3", p)
                .add(new TemporalMemory()))
            .add(n.createLayer("4", p)
                .add(new SpatialPooler())
                .add(MultiEncoder.builder().name("").build()));
        
        assertNull(r1.lookup("1").getEncoder());
            
        r1.connect("1", "2")
            .connect("2", "3")
            .connect("3", "4");
        
        assertNotNull(r1.lookup("1").getEncoder());
    }
    
    /**
     * Test that we can assemble a multi-layer Region and manually feed in
     * input and have the processing pass through each Layer.
     */
    @Test
    public void testMultiLayerAssemblyNoSensor() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
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
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE))
            .add(n.createLayer("2", p)
                .add(Anomaly.create(params)))
            .add(n.createLayer("3", p)
                .add(new TemporalMemory()))
            .add(n.createLayer("4", p)
                .add(new SpatialPooler())
                .add(MultiEncoder.builder().name("").build()))
            .connect("1", "2")
            .connect("2", "3")
            .connect("3", "4");
        r1.lookup("3").using(r1.lookup("4").getConnections()); // How to share Connections object between Layers
        r1.close();
        
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
        r1.compute(multiInput);
        
    }
    
    /**
     * Tests that a Region can be assembled containing multiple layers
     * with an underlying FileSensor and started and that it produces the proper emissions.
     */
    @Test
    public void testMultiLayerAssemblyWithSensor() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE))
            .add(n.createLayer("2", p)
                .add(Anomaly.create(params)))
            .add(n.createLayer("3", p)
                .add(new TemporalMemory()))
            .add(n.createLayer("4", p)
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("days-of-week.csv"))))
                .add(new SpatialPooler()))
            .connect("1", "2")
            .connect("2", "3")
            .connect("3", "4")
            .close();
        
        r1.observe().subscribe(new Subscriber<Inference>() {
            int idx = 0;
            
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                // Test Classifier and Anomaly output
                switch(idx) {
                    case 0: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertEquals(1.0, i.getClassification("dayOfWeek").getStats(1)[0], 0.0);
                            assertEquals(1, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 1: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertEquals(1.0, i.getClassification("dayOfWeek").getStats(1)[0], 0.0);
                            assertEquals(1, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 2: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 0.5, 0.5 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(2, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 3: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 
                                0.33333333333333333, 0.33333333333333333, 0.33333333333333333 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(3, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 4: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 0.25, 0.25, 0.25, 0.25 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(4, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 5: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 0.2, 0.2, 0.2, 0.2, 0.2 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(5, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                    case 6: assertEquals(1.0, i.getAnomalyScore(), 0.0); 
                            assertTrue(Arrays.equals(new double[] { 
                                0.16666666666666666, 0.16666666666666666,
                                0.16666666666666666, 0.16666666666666666, 
                                0.16666666666666666, 0.16666666666666666 }, i.getClassification("dayOfWeek").getStats(1)));
                            assertEquals(6, i.getClassification("dayOfWeek").getStats(1).length);
                            break;
                }
                idx++;
            }
        });
       
        r1.start();
        
        try {
            r1.lookup("4").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        
    }
    
    int idx0 = 0;
    int idx1 = 0;
    int idx2 = 0;
    /**
     * For this test, see that we can subscribe to each layer and also to the
     * Region itself and that emissions for each sequence occur for all 
     * subscribers.
     */
    @Test
    public void test2LayerAssemblyWithSensor() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Network n = Network.create("test network", p);
        Region r1 = n.createRegion("r1")
            .add(n.createLayer("2/3", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                .add(new TemporalMemory()))
            .add(n.createLayer("4", p)
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("days-of-week.csv"))))
                .add(new SpatialPooler()))
            .connect("2/3", "4")
            .close();
        
        final int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };
        
        // Observe the top layer
        r1.lookup("4").observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                assertTrue(Arrays.equals(inputs[idx0++], i.getEncoding()));
            }
        });
        
        // Observe the bottom layer
        r1.lookup("2/3").observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                assertTrue(Arrays.equals(inputs[idx1++], i.getEncoding()));
            }
        });
        
        // Observe the Region output
        r1.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                assertTrue(Arrays.equals(inputs[idx2++], i.getEncoding()));
            }
        });
        
        r1.start();
                    
        try {
            r1.lookup("4").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        assertEquals(7, idx0);
        assertEquals(7, idx1);
        assertEquals(7, idx2);
    }

}
