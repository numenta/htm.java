package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;
import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.Network.Node;
import org.numenta.nupic.network.Network.NodeImpl;
import org.numenta.nupic.network.SensorParams.Keys;
import org.numenta.nupic.research.ComputeCycle;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.MethodSignature;
import org.numenta.nupic.util.Tuple;

import rx.Observable;


/**
 * All Nodes interact with their inner contents via the {@link Node#process(Tuple)} method.
 * Here we test that we can load up various contents with different inner computation methods
 * and different return values, and still interact with them in a generic way while also still
 * receiving strongly "typed" return values.
 * 
 * @author David Ray
 * @see Node
 * @see Layer
 * @see Node#process(Tuple)
 * @see Tuple
 * @see MethodSignature
 */
public class NodeTest {
    
    private Anomaly makePureAnomalyComputer() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        Anomaly anomalyComputer = Anomaly.create(params);
        return anomalyComputer;
    }
    
    @SuppressWarnings("unchecked")
    private Observable<String> makeObservable() {
        File f = new File(ResourceLocator.path("rec-center-hourly.csv"));
        try {
            Observable<?> o = Observable.from(Files.lines(f.toPath(), Charset.forName("UTF-8")).toArray());
            return (Observable<String>)o;
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        return null;
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
    
    private Parameters getTestEncoderParams() {
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
        
        // This will work also
        //fieldEncodings.get("timestamp").put(KEY.DATEFIELD_FORMATTER.getFieldName(), DateEncoder.FULL_DATE);
                
        Parameters p = Parameters.getEncoderDefaultParameters();
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        
        return p;
    }
    
    private Parameters getSpatialPoolerTestParams() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 5 });//5
        parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 5 });//5
        parameters.setParameterByKey(KEY.POTENTIAL_RADIUS, 3);//3
        parameters.setParameterByKey(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.setParameterByKey(KEY.GLOBAL_INHIBITIONS, false);
        parameters.setParameterByKey(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 3.0);
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
        
        // Set some temporal memory presets
        parameters.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.setParameterByKey(KEY.MIN_THRESHOLD, 5);
        parameters.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.05);
        parameters.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.05);
        parameters.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 4);
        
        parameters.setInputDimensions(new int[] { 1, 188});
        parameters.setColumnDimensions(new int[] { 2048, 1 });
        parameters.setPotentialRadius(94);
        parameters.setPotentialPct(0.5); 
        parameters.setGlobalInhibition(true);
        parameters.setLocalAreaDensity(-1.0);
        parameters.setNumActiveColumnsPerInhArea(40);
        parameters.setStimulusThreshold(1);
        parameters.setSynPermInactiveDec(0.01);
        parameters.setSynPermActiveInc(0.1);
        parameters.setMinPctOverlapDutyCycle(0.1);
        parameters.setMinPctActiveDutyCycle(0.1);
        parameters.setDutyCyclePeriod(1000);
        parameters.setMaxBoost(10);
        parameters.setSynPermConnected(0.1);
        parameters.setSynPermTrimThreshold(0);
        parameters.setRandom(new MersenneTwister(42));
        
        return parameters;
    }
    
    @Test
    public void testProcessSensor() {
        Object[] n = { "some name", makeObservable() };
        SensorParams parms = SensorParams.create(Keys::path, n);
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(ObservableSensor::create, parms);
        
        ((HTMSensor<ObservableSensor<String[]>>)sensor).setLocalParameters(getTestEncoderParams());
        
        Node<Sensor<ObservableSensor<String[]>>> node = 
            new NodeImpl<Sensor<ObservableSensor<String[]>>>(sensor);
        try {
            int[] encoding = node.process(new MethodSignature(1).setParams((Object)new String[] { "0", "7/2/10 0:00","21.2" }));
            
            int[] expected = new int[] { 
                0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 
                0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1 };
            
            assertTrue(Arrays.equals(expected, encoding));
        }catch(UnsupportedOperationException u) {
            assertEquals("For now, Sensors create their own input", u.getMessage());
        }
    }
    
    @Test
    public void testProcessSpatialPooler() {
        Parameters p = getSpatialPoolerTestParams();
        Connections mem = new Connections();
        p.apply(mem);
        SpatialPooler sp = new SpatialPooler();
        sp.init(mem);
        Node<SpatialPooler> n = new NodeImpl<>(sp);
        
        int[] inputVector = {
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0  
        };
        int[] activeArray = new int[2048];
        
        // Shown with the re-usable and mutable MethodSignature subclass of Tuple 
        // Either way is fine, but in a network hierarchy we use MethodSignature to avoid creating
        // new Tuples on every cycle.
        int[] sdr = n.process(new Tuple(mem, inputVector, activeArray, true, false));
        sdr = n.process(new MethodSignature(5).setParams(mem, inputVector, activeArray, true, false));
        
        int[] real = ArrayUtils.where(sdr, ArrayUtils.WHERE_1);
        
        int[] expected = new int[] {
            46, 61, 86, 216, 314, 543, 554, 587, 630, 675, 736, 
            745, 834, 931, 990, 1131, 1285, 1305, 1307, 1326, 1411, 1414, 
            1431, 1471, 1547, 1579, 1603, 1687, 1698, 1730, 1847, 
            1859, 1885, 1893, 1895, 1907, 1934, 1978, 1984, 1990 };
        
        assertTrue(Arrays.equals(expected, real));
    }
    
    @Test
    public void testProcessTemporalMemory() {
        Parameters p = getSpatialPoolerTestParams();
        Connections mem = new Connections();
        p.apply(mem);
        TemporalMemory tm = new TemporalMemory();
        tm.init(mem);
        Node<TemporalMemory> n = new NodeImpl<>(tm);
        
        int[] expected = new int[] {
            46, 61, 86, 216, 314, 543, 554, 587, 630, 675, 736, 
            745, 834, 931, 990, 1131, 1285, 1305, 1307, 1326, 1411, 1414, 
            1431, 1471, 1547, 1579, 1603, 1687, 1698, 1730, 1847, 
            1859, 1885, 1893, 1895, 1907, 1934, 1978, 1984, 1990 };
        
        // Predictions are the most meaningful for the TM, but here we test burst values
        // since we're only running it once.
        ComputeCycle cc = n.process(new Tuple(mem, expected, true));
        assertNotNull(cc);
        assertEquals(1280, cc.activeCells().size());
        assertEquals(40, cc.learningSegments().size());
        assertEquals(40, cc.winnerCells().size());
        assertEquals(1483, cc.winnerCells().iterator().next().getIndex());
    }
    
    @Test
    public void testProcessClassifier() {
        Node<CLAClassifier> n = new NodeImpl<>(
            new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0));
        Map<String, Object> classification = new LinkedHashMap<String, Object>();
        classification.put("bucketIdx", 4);
        classification.put("actValue", 34.7);
        Tuple t = new Tuple(0, classification, new int[] { 1, 5, 9 }, true, true);
        ClassifierResult<Double> result = n.process(t);
        
        assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
        assertEquals(1, result.getActualValueCount());
        assertEquals(34.7, result.getActualValue(0), 0.01);
    }
    
    @Test
    public void testProcessAnomaly() {
        Node<Anomaly> n = new NodeImpl<>(makePureAnomalyComputer());
        Tuple t = new Tuple(new int[] { 2, 3, 6 }, new int[] { 3, 5, 7 }, 0, 0);
        double score = n.process(t);
        assertEquals(2.0 / 3.0, score, 0);
    }
    
    // These are valid Node contents but they are test more thoroughly in their own unit test
    @Test public void testProcessLayer() {}
    @Test public void testProcessRegion() {}

}
