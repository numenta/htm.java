package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.numenta.nupic.network.NetworkSerializer.SERIAL_FILE_NAME;
import static org.numenta.nupic.network.NetworkSerializer.SERIAL_DIR;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.SDR;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.algorithms.TemporalMemory.SegmentSearch;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.network.NetworkSerializer.Scheme;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.FastRandom;
import org.numenta.nupic.util.MersenneTwister;

import com.cedarsoftware.util.DeepEquals;

import rx.Observer;
import rx.Subscriber;


public class JavaKryoNetworkSerializationTest {
    
    public Parameters getParameters() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 20 });
        parameters.setParameterByKey(KEY.CELLS_PER_COLUMN, 6);
        
        //SpatialPooler specific
        parameters.setParameterByKey(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.setParameterByKey(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.setParameterByKey(KEY.GLOBAL_INHIBITION, false);
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
        parameters.setParameterByKey(KEY.RANDOM, new FastRandom(42));
        
        return parameters;
    }

    public Kryo createKryo() {
        Kryo.DefaultInstantiatorStrategy initStrategy = new Kryo.DefaultInstantiatorStrategy();

        // use Objenesis to create classes without calling the constructor (Flink's technique)
        //initStrategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());

        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(initStrategy);
        return kryo;
    }

    private static <T> T copy(Kryo kryo, T from) {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        Output output = new Output(baout);

        kryo.writeObject(output, from);

        output.close();

        ByteArrayInputStream bain = new ByteArrayInputStream(baout.toByteArray());
        Input input = new Input(bain);

        return (T)kryo.readObject(input, from.getClass());
    }

    @Test
    public void testGetSerializer() {
        SerialConfig config = new SerialConfig(SERIAL_FILE_NAME, Scheme.KRYO);
        NetworkSerializer<?> serializer = Network.serializer(config, false);
        assertNotNull(serializer);
        
        NetworkSerializer<?> serializer2 = Network.serializer(config, false);
        assertTrue(serializer == serializer2);
        
        NetworkSerializer<?> serializer3 = Network.serializer(config, true);
        assertTrue(serializer != serializer3);
        assertTrue(serializer2 != serializer3);
        assertTrue(serializer == serializer2);
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void testEnsurePathExists() {
        SerialConfig config = new SerialConfig("testEnsurePathExistsKRYO", Scheme.KRYO);
        NetworkSerializer<?> serializer = Network.serializer(config, true);
        
        try {
            ((NetworkSerializerImpl)serializer).ensurePathExists(config);
        }catch(Exception e) { fail(); }
        
        File f1 = new File(System.getProperty("user.home") + File.separator + SERIAL_DIR + File.separator + "testEnsurePathExistsKRYO");
        assertTrue(f1.exists());
        File f2 = serializer.getSerializedFile();
        try { Thread.sleep(2000); } catch(Exception e) {e.printStackTrace();}
        assertEquals(f1.getAbsolutePath(), f2.getAbsolutePath());
    }
    
    //////////////////////////////////////////////////////////////////
    //     First, Test Serialization of Each Object Individually    //
    //////////////////////////////////////////////////////////////////
    
    // Parameters
    @Test
    public void testSerializeParameters() {
        Parameters p = getParameters();
        SerialConfig config = new SerialConfig("testSerializeParameters", Scheme.KRYO);
        NetworkSerializer<Parameters> serializer = Network.serializer(config, false);
        
        // 1. serialize
        byte[] data = serializer.serialize(p);
        
        // 2. deserialize
        Parameters serialized = serializer.deSerialize(Parameters.class, data);
        
        assertTrue(p.keys().size() == serialized.keys().size());
        assertTrue(DeepEquals.deepEquals(p, serialized));
        for(KEY k : p.keys()) {
            deepCompare(serialized.getParameterByKey(k), p.getParameterByKey(k));
        }
        
        // 3. reify from file
        Parameters fromFile = serializer.deSerialize(Parameters.class, null);
        assertTrue(p.keys().size() == fromFile.keys().size());
        assertTrue(DeepEquals.deepEquals(p, fromFile));
        for(KEY k : p.keys()) {
            deepCompare(fromFile.getParameterByKey(k), p.getParameterByKey(k));
        }
    }
    
    // Connections
    @Test
    public void testSerializeConnections() {
        Parameters p = getParameters();
        Connections con = new Connections();
        p.apply(con);
        
        TemporalMemory tm = new TemporalMemory();
        tm.init(con);
        
        SerialConfig config = new SerialConfig("testSerializeConnections", Scheme.KRYO);
        NetworkSerializer<Connections> serializer = Network.serializer(config, false);
        
        // 1. serialize
        byte[] data = serializer.serialize(con);
        
        // 2. deserialize
        Connections serialized = serializer.deSerialize(Connections.class, data);
        assertTrue(DeepEquals.deepEquals(con, serialized));
        
        serialized.printParameters();
        int cellCount = con.getCellsPerColumn();
        for(int i = 0;i < con.getNumColumns();i++) {
            deepCompare(con.getColumn(i), serialized.getColumn(i));
            for(int j = 0;j < cellCount;j++) {
                Cell cell = serialized.getCell(i * cellCount + j);
                deepCompare(con.getCell(i * cellCount + j), cell);
            }
        }
        
        // 3. reify from file
        Connections fromFile = serializer.deSerialize(Connections.class, null);
        assertTrue(DeepEquals.deepEquals(con, fromFile));
        for(int i = 0;i < con.getNumColumns();i++) {
            deepCompare(con.getColumn(i), fromFile.getColumn(i));
            for(int j = 0;j < cellCount;j++) {
                Cell cell = fromFile.getCell(i * cellCount + j);
                deepCompare(con.getCell(i * cellCount + j), cell);
            }
        }
    }
    
    // Connections with all types populated
    @SuppressWarnings("unused")
    @Test
    public void testMorePopulatedConnections() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setConnectedPermanence(0.50);
        cn.setMinThreshold(1);
        // Init with default params defined in Connections.java default fields.
        tm.init(cn);
        
        SerialConfig config = new SerialConfig("testMorePopulatedConnections", Scheme.KRYO);
        NetworkSerializer<DistalDendrite> serializer = Network.serializer(config, false);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        byte[] dda = serializer.serialize(dd);
        DistalDendrite ddo = serializer.deSerialize(DistalDendrite.class, dda);
        deepCompare(dd, ddo);
        List<Synapse> l1 = dd.getAllSynapses(cn);
        List<Synapse> l2 = ddo.getAllSynapses(cn);
        assertTrue(l2.equals(l1));
        
        DistalDendrite dd1 = cn.getCell(0).createSegment(cn);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(49), 0.9);
        Synapse s4 = dd1.createSynapse(cn, cn.getCell(3), 0.8);
        
        DistalDendrite dd2 = cn.getCell(1).createSegment(cn);
        Synapse s5 = dd2.createSynapse(cn, cn.getCell(733), 0.7);
        
        DistalDendrite dd3 = cn.getCell(8).createSegment(cn);
        Synapse s6 = dd3.createSynapse(cn, cn.getCell(486), 0.9);
        
        
        Connections cn2 = new Connections();
        cn2.setConnectedPermanence(0.50);
        cn2.setMinThreshold(1);
        tm.init(cn2);
        
        DistalDendrite ddb = cn2.getCell(0).createSegment(cn2);
        Synapse s0b = ddb.createSynapse(cn2, cn2.getCell(23), 0.6);
        Synapse s1b = ddb.createSynapse(cn2, cn2.getCell(37), 0.4);
        Synapse s2b = ddb.createSynapse(cn2, cn2.getCell(477), 0.9);
        
        DistalDendrite dd1b = cn2.getCell(0).createSegment(cn2);
        Synapse s3b = dd1b.createSynapse(cn2, cn2.getCell(49), 0.9);
        Synapse s4b = dd1b.createSynapse(cn2, cn2.getCell(3), 0.8);
        
        DistalDendrite dd2b = cn2.getCell(1).createSegment(cn2);
        Synapse s5b = dd2b.createSynapse(cn2, cn2.getCell(733), 0.7);
        
        DistalDendrite dd3b = cn2.getCell(8).createSegment(cn2);
        Synapse s6b = dd3b.createSynapse(cn2, cn2.getCell(486), 0.9);
        
        assertTrue(cn.equals(cn2));
        
        Set<Cell> activeCells = cn.getCellSet(new int[] { 733, 37, 974, 23 });
        
        SegmentSearch result = tm.getBestMatchingSegment(cn, cn.getCell(0), activeCells);
        assertEquals(dd, result.bestSegment);
        assertEquals(2, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(1), activeCells);
        assertEquals(dd2, result.bestSegment);
        assertEquals(1, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(8), activeCells);
        assertEquals(null, result.bestSegment);
        assertEquals(0, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(100), activeCells);
        assertEquals(null, result.bestSegment);
        assertEquals(0, result.numActiveSynapses);
        
        //Test that we can repeat this
        result = tm.getBestMatchingSegment(cn, cn.getCell(0), activeCells);
        assertEquals(dd, result.bestSegment);
        assertEquals(2, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(1), activeCells);
        assertEquals(dd2, result.bestSegment);
        assertEquals(1, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(8), activeCells);
        assertEquals(null, result.bestSegment);
        assertEquals(0, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(100), activeCells);
        assertEquals(null, result.bestSegment);
        assertEquals(0, result.numActiveSynapses);
        
        SerialConfig config2 = new SerialConfig("testMorePopulatedConnections2", Scheme.KRYO);
        NetworkSerializer<Connections> serializer2 = Network.serializer(config, false);
        
        // 1. serialize
        byte[] data = serializer2.serialize(cn);
        
        // 2. deserialize
        Connections serialized = serializer2.deSerialize(Connections.class, data);
        
        Set<Cell> serialActiveCells = serialized.getCellSet(new int[] { 733, 37, 974, 23 });
        
        deepCompare(activeCells, serialActiveCells);
        
        result = tm.getBestMatchingSegment(serialized, serialized.getCell(0), serialActiveCells);
        assertEquals(dd, result.bestSegment);
        assertEquals(2, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(serialized, serialized.getCell(1), serialActiveCells);
        assertEquals(dd2, result.bestSegment);
        assertEquals(1, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(serialized, serialized.getCell(8), serialActiveCells);
        assertEquals(null, result.bestSegment);
        assertEquals(0, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(serialized, serialized.getCell(100), serialActiveCells);
        assertEquals(null, result.bestSegment);
        assertEquals(0, result.numActiveSynapses);
        
        boolean b = DeepEquals.deepEquals(cn, serialized);
        deepCompare(cn, serialized);
        assertTrue(b);
        
        //{0=[synapse: [ synIdx=0, inIdx=23, sgmtIdx=0, srcCellIdx=23 ], synapse: [ synIdx=1, inIdx=37, sgmtIdx=0, srcCellIdx=37 ], synapse: [ synIdx=2, inIdx=477, sgmtIdx=0, srcCellIdx=477 ]], 1=[synapse: [ synIdx=3, inIdx=49, sgmtIdx=1, srcCellIdx=49 ], synapse: [ synIdx=4, inIdx=3, sgmtIdx=1, srcCellIdx=3 ]], 2=[synapse: [ synIdx=5, inIdx=733, sgmtIdx=2, srcCellIdx=733 ]], 3=[synapse: [ synIdx=6, inIdx=486, sgmtIdx=3, srcCellIdx=486 ]]}
        //{0=[synapse: [ synIdx=0, inIdx=23, sgmtIdx=0, srcCellIdx=23 ], synapse: [ synIdx=1, inIdx=37, sgmtIdx=0, srcCellIdx=37 ], synapse: [ synIdx=2, inIdx=477, sgmtIdx=0, srcCellIdx=477 ]], 1=[synapse: [ synIdx=3, inIdx=49, sgmtIdx=1, srcCellIdx=49 ], synapse: [ synIdx=4, inIdx=3, sgmtIdx=1, srcCellIdx=3 ]], 2=[synapse: [ synIdx=5, inIdx=733, sgmtIdx=2, srcCellIdx=733 ]], 3=[synapse: [ synIdx=6, inIdx=486, sgmtIdx=3, srcCellIdx=486 ]]}
        
    }
    
    @Test
    public void testThreadedPublisher_TemporalMemoryNetwork() {
        Kryo kryo = createKryo();
        kryo.register(Network.class, new KryoSerializer<>(SerialConfig.DEFAULT_REGISTERED_TYPES));
        kryo.register(Connections.class, new KryoSerializer<>(SerialConfig.DEFAULT_REGISTERED_TYPES));

        List<Inference> inferences = new ArrayList<>();
        Network network = createAndRunTestTemporalMemoryNetwork(inferences);
        Layer<?> l = network.lookup("r1").lookup("1");
        Connections cn = l.getConnections();

        Network serializedNetwork = copy(kryo, network);
        Connections serializedConnections = copy(kryo, cn);

        List<Inference> inferences2 = new ArrayList<>();
        Network network2 = createAndRunTestTemporalMemoryNetwork(inferences2);
        Layer<?> l2 = network2.lookup("r1").lookup("1");
        Connections newCons = l2.getConnections();
        
        //Compare the two Connections (both serialized and regular runs) - should be equal
        boolean b = DeepEquals.deepEquals(newCons, serializedConnections);
        deepCompare(newCons, serializedConnections);
        assertTrue(b);
    }
    
    @Test
    public void testThreadedPublisher_SpatialPoolerNetwork() {
        Kryo kryo = createKryo();
        kryo.register(Network.class, new KryoSerializer<>(SerialConfig.DEFAULT_REGISTERED_TYPES));
        kryo.register(Connections.class, new KryoSerializer<>(SerialConfig.DEFAULT_REGISTERED_TYPES));

        List<Inference> inferences = new ArrayList<>();
        Network network = createAndRunTestSpatialPoolerNetwork(0, 6, inferences);
        Layer<?> l = network.lookup("r1").lookup("1");
        Connections cn = l.getConnections();

        Network serializedNetwork = copy(kryo, network);
        Connections serializedConnections = copy(kryo, cn);

        List<Inference> inferences2 = new ArrayList<>();
        Network network2 = createAndRunTestSpatialPoolerNetwork(0, 6, inferences2);
        Layer<?> l2 = network2.lookup("r1").lookup("1");
        Connections newCons = l2.getConnections();
        
        //Compare the two Connections (both serialized and regular runs) - should be equal
        boolean b = DeepEquals.deepEquals(newCons, serializedConnections);
        deepCompare(newCons, serializedConnections);
        assertTrue(b);
    }

    @Test
    public void testInferenceSerialization() {
        Kryo kryo = createKryo();
        kryo.register(Inference.class, new KryoSerializer<>(SerialConfig.DEFAULT_REGISTERED_TYPES));

        List<Inference> inferences = new ArrayList<>();
        Network network = createAndRunTestSpatialPoolerNetwork(0, 6, inferences);

        List<Inference> serializedInferences = copy(kryo, inferences);

        boolean b = DeepEquals.deepEquals(inferences, serializedInferences);
        deepCompare(inferences, serializedInferences);
        assertTrue(b);
    }

    private void deepCompare(Object obj1, Object obj2) {
        try {
            assertTrue(DeepEquals.deepEquals(obj1, obj2));
        }catch(AssertionError ae) {
            System.out.println("expected(" + obj1.getClass().getSimpleName() + "): " + obj1 + " but was: (" + obj1.getClass().getSimpleName() + "): " + obj2);
        }
    }
    
    private Network createAndRunTestSpatialPoolerNetwork(int start, int runTo, List<Inference> inferences) {
        Publisher manual = Publisher.builder()
            .addHeader("dayOfWeek")
            .addHeader("darr")
            .addHeader("B").build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", manual}));
        
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
                    
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            8,   // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "dayOfWeek",          // fieldName
            "darr",               // fieldType (dense array as opposed to sparse array or "sarr")
            "SDRPassThroughEncoder"); // encoderType
        
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, settings);
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .add(new SpatialPooler())
                    .add(sensor)));
                                
        network.start();
        
        int[][] inputs = new int[7][8];
        inputs[0] = new int[] { 1, 1, 0, 0, 0, 0, 0, 1 };
        inputs[1] = new int[] { 1, 1, 1, 0, 0, 0, 0, 0 };
        inputs[2] = new int[] { 0, 1, 1, 1, 0, 0, 0, 0 };
        inputs[3] = new int[] { 0, 0, 1, 1, 1, 0, 0, 0 };
        inputs[4] = new int[] { 0, 0, 0, 1, 1, 1, 0, 0 };
        inputs[5] = new int[] { 0, 0, 0, 0, 1, 1, 1, 0 };
        inputs[6] = new int[] { 0, 0, 0, 0, 0, 1, 1, 1 };

        int[] expected0 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        int[] expected1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
        int[] expected2 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
        int[] expected3 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        int[] expected4 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        int[] expected5 = new int[] { 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 };
        int[] expected6 = new int[] { 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0 };
        int[][] expecteds = new int[][] { expected0, expected1, expected2, expected3, expected4, expected5, expected6 };

        network.observe().subscribe(new Observer<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference spatialPoolerOutput) {
                assertTrue(Arrays.equals(expecteds[test++], spatialPoolerOutput.getSDR()));
                inferences.add(spatialPoolerOutput);
            }
        });

        // Now push some fake data through so that "onNext" is called above
        for(int i = start;i <= runTo;i++) {
            manual.onNext(Arrays.toString(inputs[i]));
        }
        
        manual.onComplete();
        
        try {
            network.lookup("r1").lookup("1").getLayerThread().join();
            
        }catch(Exception e) { e.printStackTrace(); }
        
        return network;
    }
    
    private Network createAndRunTestTemporalMemoryNetwork(List<Inference> inferences) {
        Publisher manual = Publisher.builder()
            .addHeader("dayOfWeek")
            .addHeader("darr")
            .addHeader("B").build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", manual}));
                    
        Parameters p = getParameters();
        
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            20,   // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "dayOfWeek",          // fieldName
            "darr",               // fieldType (dense array as opposed to sparse array or "sarr")
            "SDRPassThroughEncoder"); // encoderType
        
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, settings);
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .add(new TemporalMemory())
                    .add(sensor)));
                    
        network.start();
        
        network.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) { inferences.add(i); }
        });
        
        final int[] input1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] input2 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] input3 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
        final int[] input4 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input5 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input6 = new int[] { 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 };
        final int[] input7 = new int[] { 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0 };
        final int[][] inputs = { input1, input2, input3, input4, input5, input6, input7 };
        
        // Now push some warm up data through so that "onNext" is called above
        int timeUntilStable = 602;
        for(int j = 0;j < timeUntilStable;j++) {
            for(int i = 0;i < inputs.length;i++) {
                manual.onNext(Arrays.toString(inputs[i]));
            }
        }
        
        manual.onComplete();
        
        Layer<?> l = network.lookup("r1").lookup("1");
        try {
            l.getLayerThread().join();
            
            System.out.println(Arrays.toString(SDR.asCellIndices(l.getConnections().getActiveCells())));
            
        }catch(Exception e) {
            assertEquals(InterruptedException.class, e.getClass());
        }
       
        return network;
    }
    
}
