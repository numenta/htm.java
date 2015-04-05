package org.numenta.nupic.network;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.Tuple;

import rx.Observable;
import rx.Observer;


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
     * Sets up an Encoder Mapping of typical values.
     * 
     * @param map
     * @param n
     * @param w
     * @param min
     * @param max
     * @param radius
     * @param resolution
     * @param periodic
     * @param clip
     * @param forced
     * @param fieldName
     * @param fieldType
     * @param encoderType
     * @return
     */
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

    private Map<String, Map<String, Object>> getFieldEncodingMap() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                null,
                0, // n
                0, // w
                0, 0, 0, 0, null, null, null,
                "timestamp", "datetime", "DateEncoder");
        fieldEncodings = setupMap(
                fieldEncodings, 
                25, 
                3, 
                0, 0, 0, 0.1, null, null, null, 
                "consumption", "float", "RandomDistributedScalarEncoder");
        
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_DOFW.getFieldName(), new Tuple(1, 1.0)); // Day of week
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(5, 4.0)); // Time of day
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_PATTERN.getFieldName(), "MM/dd/YY HH:mm");
        
        return fieldEncodings;
    }

    /**
     * Returns Encoder parameters.
     * @return
     */
    @SuppressWarnings("unused")
    private Parameters getTestEncoderParams() {
        Map<String, Map<String, Object>> fieldEncodings = getFieldEncodingMap();

        Parameters p = Parameters.getEncoderDefaultParameters();
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);

        return p;
    }
    
    private Map<String, Map<String, Object>> getSimpleFieldEncodingMap() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                null,
                8, // n
                3, // w
                0.0, 8.0, 0, 1, Boolean.TRUE, null, Boolean.TRUE,
                "dayOfWeek", "number", "ScalarEncoder");
        return fieldEncodings;
    }

    /**
     * Returns Encoder parameters.
     * @return
     */
    private Parameters getSimpleTestEncoderParams() {
        Map<String, Map<String, Object>> fieldEncodings = getSimpleFieldEncodingMap();

        Parameters p = Parameters.getEncoderDefaultParameters();
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);

        return p;
    }
    
    private Parameters getParameters() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 20 });
        parameters.setParameterByKey(KEY.CELLS_PER_COLUMN, 6);
        
        //SpatialPooler specific
        parameters.setParameterByKey(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.setParameterByKey(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.setParameterByKey(KEY.GLOBAL_INHIBITIONS, false);
        parameters.setParameterByKey(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.setParameterByKey(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.setParameterByKey(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.setParameterByKey(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.setParameterByKey(KEY.MAX_BOOST, 10.0);
        parameters.setParameterByKey(KEY.SEED, 42);
        parameters.setParameterByKey(KEY.SP_VERBOSITY, 0);
        
        //Temporal Memory specific
        parameters.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.setParameterByKey(KEY.MIN_THRESHOLD, 5);
        parameters.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.05);
        parameters.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.05);
        parameters.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 4);
        
        return parameters;
    }
    
    private boolean isWarmedUp(Layer l, double anomalyScore) {
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
    
    @Test
    public void testBasicSetupEncoder_UsingSubscribe() {
        Parameters p = getParameters();
        p = p.union(getSimpleTestEncoderParams());
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
        Parameters p = getParameters();
        p = p.union(getSimpleTestEncoderParams());
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
        
        Parameters p = getParameters();
        p = p.union(getTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        p.setParameterByKey(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        htmSensor.setLocalParameters(p);
        
        Network n = Network.create(p);
        Layer<int[]> l = new Layer<>(n);
        l.add(htmSensor);
        
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
                //System.out.println("output = " + Arrays.toString(output.getSDR()));
                assertTrue(Arrays.equals(expected[seq++], output.getSDR()));
            }
        });
        
        l.subscribe(new Observer<Inference>() {
            int seq = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {
                //System.out.println("output = " + Arrays.toString(output.getSDR()));
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
        Parameters p = getParameters();
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
}
