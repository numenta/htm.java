package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
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
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.datagen.NetworkInputKit;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwister;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;


public class LayerTest {

    /** Total used for spatial pooler priming tests */
    private int TOTAL = 0;
    
    private int timesWithinThreshold = 0;
    private final double THRESHOLD = 7.0E-16; // 
    private int lastSeqNum = 0;
    
    
    /**
     * Resets the warm up detection variable state
     */
    private void resetWarmUp() {
        this.timesWithinThreshold = 0;
        this.lastSeqNum = 0;
    }
    
    /**
     * Checks the anomaly score against a known threshold which indicates that the
     * TM predictions have been "warmed up". When there have been enough occurrences
     * within the threshold, we conclude that the algorithms have been adequately 
     * stabilized.
     * 
     * @param l                 the {@link Layer} in question
     * @param anomalyScore      the current anomaly score
     * @return
     */
    private boolean isWarmedUp(Layer<Map<String, Object>> l, double anomalyScore) {
        if(anomalyScore > 0 && anomalyScore < THRESHOLD && (lastSeqNum == 0 || lastSeqNum == l.getRecordNum() - 1)) {
            ++timesWithinThreshold;
            lastSeqNum = l.getRecordNum();
        }else{
            lastSeqNum = 0;
            timesWithinThreshold = 0;
        }
        
        if(timesWithinThreshold > 13) {
            return true;
        }
        
        return false;
    }
    
    boolean isHalted = false;
    @Test
    public void testHalt() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv")));
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        
        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);
        l.close();
        
        l.start();
        
        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {
                assertTrue(l.isHalted());
                isHalted = true;
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                System.out.println("output = " + Arrays.toString(output.getSDR()));
            }
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
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        
        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);
        l.close();
        
        l.start();
        
        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                if(l.getSensor().getHeader().isReset()) {
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
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        
        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);
        l.close();
        
        l.start();
        
        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                if(l.getSensor().getHeader().isReset()) {
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
    public void testObservableInputLayer() {
        NetworkInputKit kit = new NetworkInputKit();
        
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, 
                SensorParams.create(
                    Keys::obs, new Object[] {"name", kit.observe()}));
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        
        HTMSensor<ObservableSensor<String[]>> htmSensor = (HTMSensor<ObservableSensor<String[]>>)sensor;
        
        Network n = Network.create("test network", p);
        final Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);
        l.close();
        
        l.start();
        
        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                System.out.println("layer out: " + output.getSDR());
            }
        });
        
        try {
            l.getLayerThread().join();
            
            String[] entries = { 
                            "timestamp", "consumption",
                            "datetime", "float",
                            "B",
                            "7/2/10 0:00,21.2",
                            "7/2/10 1:00,34.0",
                            "7/2/10 2:00,40.4",
                        };
            
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test
    public void testBasicSetupEncoder_UsingSubscribe() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, null, null, null, null);
        l.close();
        
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
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, null, null, null, null);
        l.close();
        
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
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        
        Network n = Network.create("test network", p);
        Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);
        l.close();
        
        int[][] expected = new int[][] {
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
        
        l.start();
        
        ///////////////////////////////////////////////////////
        //              Test with 2 subscribers              //
        ///////////////////////////////////////////////////////
        l.subscribe(new Observer<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
//                System.out.println("output = " + Arrays.toString(output.getSDR()));
                assertTrue(Arrays.equals(expected[seq++], output.getSDR()));
            }
        });
        
        l.subscribe(new Observer<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
//                System.out.println("output2 = " + Arrays.toString(output.getSDR()));
                assertTrue(Arrays.equals(expected[seq++], output.getSDR()));
            }
        });
        
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
        Parameters p = NetworkTestHarness.getParameters();
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
        l.close();
        
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
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        
        Network n = Network.create("test network", p);
        Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor).add(new SpatialPooler());
        l.close();
        l.start();
        
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
    public void testBasicSetup_TemporalMemory_MANUAL_MODE() {
        Parameters p = NetworkTestHarness.getParameters();
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        final int[] input1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] input2 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] input3 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
        final int[] input4 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input5 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input6 = new int[] { 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 };
        final int[] input7 = new int[] { 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0 };
        final int[][] inputs = { input1, input2, input3, input4, input5, input6, input7 };
        
        int[] expected1 = { 1, 5, 11, 12, 13 };
        int[] expected2 = { 2, 3, 11, 12, 13, 14 };
        int[] expected3 = { 2, 3, 8, 9, 12, 17, 18 };
        int[] expected4 = { 1, 2, 3, 5, 7, 8, 11, 12, 16, 17, 18 };
        int[] expected5 = { 2, 7, 8, 9, 17, 18, 19 };
        int[] expected6 = { 1, 7, 8, 9, 17, 18 };
        int[] expected7 = { 1, 5, 7, 11, 12, 16 };
        final int[][] expecteds = { expected1, expected2, expected3, expected4, expected5, expected6, expected7 };
        
        Layer<int[]> l = new Layer<>(p, null, null, new TemporalMemory(), null, null);
        l.close();
        
        int timeUntilStable = 415;
        
        l.subscribe(new Observer<Inference>() {
            int test = 0;
            int seq = 0;
            
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference output) {
                if(seq / 7 >= timeUntilStable) {
                    //System.out.println("seq: " + (seq) + "  --> " + (test) + "  output = " + Arrays.toString(output.getSDR()));
                    assertTrue(Arrays.equals(expecteds[test], output.getSDR()));
                }
                
                if(test == 6) test = 0;
                else test++;
                seq++;
            }
        });
        
        // Now push some fake data through so that "onNext" is called above
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
        Parameters p = NetworkTestHarness.getParameters();
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
        Parameters p = NetworkTestHarness.getParameters();
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
        l.close();
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
        l2.close();
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
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        l.close();
        
        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                assertEquals(0, i.getSDR().length);
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
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        p.setParameterByKey(KEY.SP_PRIMER_DELAY, PRIME_COUNT);
        
        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        l.close();
        
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
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        p.setParameterByKey(KEY.SP_PRIMER_DELAY, PRIME_COUNT);
        
        MultiEncoder me = MultiEncoder.builder().name("").build();
        Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        l.close();
        
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
    
    boolean testingAnomaly;
    double highestAnomaly = 0;
    /**
     * <p>
     * Complex test that tests the Anomaly function automatically being setup and working.
     * To test this, we do the following:
     * </p><p>
     * <ol>
     *      <li>Reset the warm up state vars</li>
     *      <li>Warm up prediction inferencing - make sure predictions are trained</li>
     *      <li>Throw in an anomalous record</li>
     *      <li>Test to make sure the Layer detects the anomaly, and that it is significantly registered</li>
     * </ol>
     * </p>
     */
    @Test
    public void testAnomalySetup() {
        TOTAL = 0;
        // Reset warm up detection state
        resetWarmUp();
        
        final int PRIME_COUNT = 35;
        final int NUM_CYCLES = 10;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.SP_PRIMER_DELAY, PRIME_COUNT);
        
        MultiEncoder me = MultiEncoder.builder().name("").build();
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        params.put(KEY_WINDOW_SIZE, 3);
        params.put(KEY_USE_MOVING_AVG, true);
        Anomaly anomalyComputer = Anomaly.create(params);
        
        final Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), 
            Boolean.TRUE, anomalyComputer);
        l.close();
        
        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                TOTAL++;
                
                assertNotNull(i);
                
                if(testingAnomaly) {
                    //System.out.println("tested anomaly = " + i.getAnomalyScore());
                    if(i.getAnomalyScore() > highestAnomaly) highestAnomaly = i.getAnomalyScore();
                }
//                System.out.println("prev predicted = " + Arrays.toString(l.getPreviousPredictedColumns()));
//                System.out.println("current active = " + Arrays.toString(l.getActiveColumns()));
//                System.out.println("rec# " + i.getRecordNum() + ",  input " + i.getLayerInput() + ",  anomaly = " + i.getAnomalyScore() + ",  inference = " + l.getInference());                
//                System.out.println("----------------------------------------");
            }
        });
        
        // Warm up so that we can have a baseline to detect an anomaly
        Map<String, Object> multiInput = new HashMap<>();
        boolean isWarmedUp = false;
        while(l.getInference() == null || !isWarmedUp) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                multiInput.put("dayOfWeek", j);
                l.compute(multiInput);
                if(l.getInference() != null && isWarmedUp(l, l.getInference().getAnomalyScore())) {
                    isWarmedUp = true;
                }
            }
        }
        
        // Now throw in an anomaly and see if it is detected.
        boolean exit = false;
        for(int i = 0;!exit && i < NUM_CYCLES;i++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                if(i == 2) {
                    testingAnomaly = true;
                    multiInput.put("dayOfWeek", j+=0.5);
                    l.compute(multiInput);
                    exit = true;
                    //break;
                }else{
                    multiInput.put("dayOfWeek", j);
                    l.compute(multiInput);
                }
                
            }
        }
        
        // Now assert we detected anomaly greater than average and significantly greater than 0 (i.e. 20%)
        //System.out.println("highestAnomaly = " + highestAnomaly);
        assertTrue(highestAnomaly > 0.2);
        
    }
    
    @Test
    public void testGetAllPredictions() {
        final int PRIME_COUNT = 35;
        final int NUM_CYCLES = 200;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        TOTAL = 0;
        
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        p.setParameterByKey(KEY.SP_PRIMER_DELAY, PRIME_COUNT);
        
        MultiEncoder me = MultiEncoder.builder().name("").build();
        final Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        l.close();
        
        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                assertNotNull(i);
                TOTAL++;
                // UNCOMMENT TO VIEW STABILIZATION OF PREDICTED FIELDS
                // System.out.println("Day: " + ((Map<String, Object>)i.getLayerInput()).get("dayOfWeek") + "  -  " + Arrays.toString(ArrayUtils.where(l.getActiveColumns(), ArrayUtils.WHERE_1)) +
                // "   -   " + Arrays.toString(l.getPreviousPredictedColumns()));
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
        assertTrue(Arrays.equals(ArrayUtils.where(l.getActiveColumns(), ArrayUtils.WHERE_1), l.getPreviousPredictedColumns()));
    }
    
    /**
     * Test that a given layer can return an {@link Observable} capable of 
     * service multiple subscribers.
     */
    @Test
    public void testObservableRetrieval() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getSimpleTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        MultiEncoder me = MultiEncoder.builder().name("").build();
        final Layer<Map<String, Object>> l = new Layer<>(p, me, new SpatialPooler(), new TemporalMemory(), Boolean.TRUE, null);
        l.close();
        
        final List<int[]> emissions = new ArrayList<int[]>();
        Observable<Inference> o = l.observe();
        o.subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override public void onNext(Inference i) {
                emissions.add(l.getActiveColumns());
            }
        });
        o.subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override public void onNext(Inference i) {
                emissions.add(l.getActiveColumns());
            }
        });
        o.subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override public void onNext(Inference i) {
                emissions.add(l.getActiveColumns());
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
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.setParameterByKey(KEY.POTENTIAL_RADIUS, 200);
        p.setParameterByKey(KEY.INHIBITION_RADIUS, 50);
        p.setParameterByKey(KEY.GLOBAL_INHIBITIONS, true);
        
        System.out.println(p);
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        params.put(KEY_WINDOW_SIZE, 3);
        params.put(KEY_USE_MOVING_AVG, true);
        Anomaly anomalyComputer = Anomaly.create(params);
        
        Layer<?> l = Network.create("Fluent Network Layer", p)
            .createLayer("TestLayer", p)
                .alterParameter(KEY.AUTO_CLASSIFY, true)
                .add(anomalyComputer)
                .add(new TemporalMemory())
                .add(new SpatialPooler())
                .add(Sensor.create(
                    FileSensor::create, 
                        SensorParams.create(
                            Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv"))))
                .close();
        
        l.getConnections().printParameters();
        System.out.println("inhibit radius = " + l.getConnections().getInhibitionRadius());
        
        l.subscribe(new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { System.out.println("error: " + e.getMessage()); e.printStackTrace();}
            @Override
            public void onNext(Inference i) {
                if(flowReceived) return; // No need to set this value multiple times
                
                System.out.println("classifier size = " + i.getClassifiers().size());
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
}