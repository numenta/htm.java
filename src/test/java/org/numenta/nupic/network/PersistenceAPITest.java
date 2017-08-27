/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_USE_MOVING_AVG;
import static org.numenta.nupic.algorithms.Anomaly.KEY_WINDOW_SIZE;
import static org.numenta.nupic.network.NetworkTestHarness.*;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.AnomalyLikelihood;
import org.numenta.nupic.algorithms.AnomalyLikelihoodMetrics;
import org.numenta.nupic.algorithms.AnomalyLikelihoodTest;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.Classification;
import org.numenta.nupic.algorithms.Sample;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.SDR;
import org.numenta.nupic.network.Persistence.PersistenceAccess;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.serialize.SerialConfig;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.FastRandom;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.Tuple;

import com.cedarsoftware.util.DeepEquals;

import gnu.trove.list.array.TIntArrayList;
import rx.Observer;
import rx.Subscriber;
import rx.observers.TestObserver;


public class PersistenceAPITest extends ObservableTestBase {
    // TO TURN ON PRINTOUT: SET "TRUE" BELOW
    /** Printer to visualize DayOfWeek printouts - SET TO TRUE FOR PRINTOUT */
    private BiFunction<Inference, Integer, Integer> dayOfWeekPrintout = createDayOfWeekInferencePrintout(false);
    
    
    @BeforeClass
    public static void beforeClass(){
    	// Sample data contains datetimes that are invalid in some timezones due to DST.
    	// If UTC is forced, then test runs should yield the same result regardless of timezone
    	System.setProperty("user.timezone", "UTC");
    }

    @AfterClass
    public static void cleanUp() {
        System.out.println("cleaning up...");
        try {
            File serialDir = new File(System.getProperty("user.home") + File.separator + SerialConfig.SERIAL_TEST_DIR);
            if(serialDir.exists()) {
                Files.list(serialDir.toPath()).forEach(
                    f ->  { 
                        try { Files.deleteIfExists(f.toAbsolutePath()); }
                        catch(Exception io) { throw new RuntimeException(io); } 
                    }
                );
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testEnsurePathExists() {
        SerialConfig config = new SerialConfig("testEnsurePathExists", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI persist = Persistence.get();
        persist.setConfig(config);
        
        try {
            ((PersistenceAccess)persist).ensurePathExists(config);
        }catch(Exception e) { fail(); }
        
        File f1 = new File(System.getProperty("user.home") + File.separator + config.getFileDir() + File.separator + "testEnsurePathExists");
        assertTrue(f1.exists());
    }
    
    @Test
    public void testSearchAndListPreviousCheckPoint() {
        Parameters p = NetworkTestHarness.getParameters();
        Network network = Network.create("test network", p).add(Network.createRegion("r1")
            .add(Network.createLayer("1", p)
                .add(Anomaly.create())
                .add(new TemporalMemory())
                .add(new SpatialPooler())));
        
        PersistenceAPI pa = Persistence.get(new SerialConfig(null, SerialConfig.SERIAL_TEST_DIR));
        IntStream.range(0, 5).forEach(i -> ((PersistenceAccess)pa).getCheckPointFunction(network).apply(network));
        
        List<String> checkPointFiles = pa.listCheckPointFiles();
        assertTrue(checkPointFiles.size() > 4);
        
        assertEquals(checkPointFiles.get(checkPointFiles.size() - 2), 
            pa.getPreviousCheckPoint(checkPointFiles.get(checkPointFiles.size() - 1)));
    }
    
    /////////////////////////////////////////////////////////////////////////////
    //     First, Test Serialization of Each (Critical) Object Individually    //
    /////////////////////////////////////////////////////////////////////////////

    /////////////////////
    //    Parameters   //
    /////////////////////
    @Test
    public void testSerializeParameters() {
        Parameters p = getParameters();
        
        SerialConfig config = new SerialConfig("testSerializeParameters", SerialConfig.SERIAL_TEST_DIR);
        
        PersistenceAPI api = Persistence.get(config);
        
        // 1. serialize
        byte[] data = api.write(p, "testSerializeParameters");

        // 2. deserialize
        Parameters serialized = api.read(data);

        assertTrue(p.keys().size() == serialized.keys().size());
        assertTrue(DeepEquals.deepEquals(p, serialized));
        for(KEY k : p.keys()) {
            deepCompare(serialized.get(k), p.get(k));
        }

        // 3. reify from file
        /////////////////////////////////////
        //  SHOW RETRIEVAL USING FILENAME  //
        /////////////////////////////////////
        Parameters fromFile = api.read("testSerializeParameters");
        assertTrue(p.keys().size() == fromFile.keys().size());
        assertTrue(DeepEquals.deepEquals(p, fromFile));
        for(KEY k : p.keys()) {
            deepCompare(fromFile.get(k), p.get(k));
        }
    }
    
    /////////////////////
    //   Connections   //
    /////////////////////
    @Test
    public void testSerializeConnections() {
        Parameters p = getParameters();
        Connections con = new Connections();
        p.apply(con);

        TemporalMemory.init(con);

        SerialConfig config = new SerialConfig("testSerializeConnections", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        
        // 1. serialize
        byte[] data = api.write(con);

        // 2. deserialize
        Connections serialized = api.read(data);
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
        Connections fromFile = api.read(data);
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
//    @SuppressWarnings("unused")
//    @Test
//    public void testMorePopulatedConnections() {
//        TemporalMemory tm = new TemporalMemory();
//        Connections cn = new Connections();
//        cn.setConnectedPermanence(0.50);
//        cn.setMinThreshold(1);
//        // Init with default params defined in Connections.java default fields.
//        tm.init(cn);
//        
//        SerialConfig config = new SerialConfig("testSerializeConnections2", SerialConfig.SERIAL_TEST_DIR);
//        PersistenceAPI api = Persistence.get(config);
//        
//        DistalDendrite dd = cn.getCell(0).createSegment(cn);
//        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6);
//        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4);
//        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9);
//        
//        byte[] dda = api.write(dd);
//        DistalDendrite ddo = api.read(dda);
//        deepCompare(dd, ddo);
//        List<Synapse> l1 = dd.getAllSynapses(cn);
//        List<Synapse> l2 = ddo.getAllSynapses(cn);
//        assertTrue(l2.equals(l1));
//        
//        DistalDendrite dd1 = cn.getCell(0).createSegment(cn);
//        Synapse s3 = dd1.createSynapse(cn, cn.getCell(49), 0.9);
//        Synapse s4 = dd1.createSynapse(cn, cn.getCell(3), 0.8);
//        
//        DistalDendrite dd2 = cn.getCell(1).createSegment(cn);
//        Synapse s5 = dd2.createSynapse(cn, cn.getCell(733), 0.7);
//        
//        DistalDendrite dd3 = cn.getCell(8).createSegment(cn);
//        Synapse s6 = dd3.createSynapse(cn, cn.getCell(486), 0.9);
//        
//        
//        Connections cn2 = new Connections();
//        cn2.setConnectedPermanence(0.50);
//        cn2.setMinThreshold(1);
//        tm.init(cn2);
//        
//        DistalDendrite ddb = cn2.getCell(0).createSegment(cn2);
//        Synapse s0b = ddb.createSynapse(cn2, cn2.getCell(23), 0.6);
//        Synapse s1b = ddb.createSynapse(cn2, cn2.getCell(37), 0.4);
//        Synapse s2b = ddb.createSynapse(cn2, cn2.getCell(477), 0.9);
//        
//        DistalDendrite dd1b = cn2.getCell(0).createSegment(cn2);
//        Synapse s3b = dd1b.createSynapse(cn2, cn2.getCell(49), 0.9);
//        Synapse s4b = dd1b.createSynapse(cn2, cn2.getCell(3), 0.8);
//        
//        DistalDendrite dd2b = cn2.getCell(1).createSegment(cn2);
//        Synapse s5b = dd2b.createSynapse(cn2, cn2.getCell(733), 0.7);
//        
//        DistalDendrite dd3b = cn2.getCell(8).createSegment(cn2);
//        Synapse s6b = dd3b.createSynapse(cn2, cn2.getCell(486), 0.9);
//        
//        assertTrue(cn.equals(cn2));
//        
//        Set<Cell> activeCells = cn.getCellSet(new int[] { 733, 37, 974, 23 });
//        
//        SegmentSearch result = tm.getBestMatchingSegment(cn, cn.getCell(0), activeCells);
//        assertEquals(dd, result.bestSegment);
//        assertEquals(2, result.numActiveSynapses);
//        
//        result = tm.getBestMatchingSegment(cn, cn.getCell(1), activeCells);
//        assertEquals(dd2, result.bestSegment);
//        assertEquals(1, result.numActiveSynapses);
//        
//        result = tm.getBestMatchingSegment(cn, cn.getCell(8), activeCells);
//        assertEquals(null, result.bestSegment);
//        assertEquals(0, result.numActiveSynapses);
//        
//        result = tm.getBestMatchingSegment(cn, cn.getCell(100), activeCells);
//        assertEquals(null, result.bestSegment);
//        assertEquals(0, result.numActiveSynapses);
//        
//        //Test that we can repeat this
//        result = tm.getBestMatchingSegment(cn, cn.getCell(0), activeCells);
//        assertEquals(dd, result.bestSegment);
//        assertEquals(2, result.numActiveSynapses);
//        
//        result = tm.getBestMatchingSegment(cn, cn.getCell(1), activeCells);
//        assertEquals(dd2, result.bestSegment);
//        assertEquals(1, result.numActiveSynapses);
//        
//        result = tm.getBestMatchingSegment(cn, cn.getCell(8), activeCells);
//        assertEquals(null, result.bestSegment);
//        assertEquals(0, result.numActiveSynapses);
//        
//        result = tm.getBestMatchingSegment(cn, cn.getCell(100), activeCells);
//        assertEquals(null, result.bestSegment);
//        assertEquals(0, result.numActiveSynapses);
//        
//        // 1. serialize
//        byte[] data = api.write(cn, "testSerializeConnections2");
//        
//        // 2. deserialize
//        Connections serialized = api.read(data);
//        
//        Set<Cell> serialActiveCells = serialized.getCellSet(new int[] { 733, 37, 974, 23 });
//        
//        deepCompare(activeCells, serialActiveCells);
//        
//        result = tm.getBestMatchingSegment(serialized, serialized.getCell(0), serialActiveCells);
//        assertEquals(dd, result.bestSegment);
//        assertEquals(2, result.numActiveSynapses);
//        
//        result = tm.getBestMatchingSegment(serialized, serialized.getCell(1), serialActiveCells);
//        assertEquals(dd2, result.bestSegment);
//        assertEquals(1, result.numActiveSynapses);
//        
//        result = tm.getBestMatchingSegment(serialized, serialized.getCell(8), serialActiveCells);
//        assertEquals(null, result.bestSegment);
//        assertEquals(0, result.numActiveSynapses);
//        
//        result = tm.getBestMatchingSegment(serialized, serialized.getCell(100), serialActiveCells);
//        assertEquals(null, result.bestSegment);
//        assertEquals(0, result.numActiveSynapses);
//        
//        boolean b = DeepEquals.deepEquals(cn, serialized);
//        deepCompare(cn, serialized);
//        assertTrue(b);
//        
//        //{0=[synapse: [ synIdx=0, inIdx=23, sgmtIdx=0, srcCellIdx=23 ], synapse: [ synIdx=1, inIdx=37, sgmtIdx=0, srcCellIdx=37 ], synapse: [ synIdx=2, inIdx=477, sgmtIdx=0, srcCellIdx=477 ]], 1=[synapse: [ synIdx=3, inIdx=49, sgmtIdx=1, srcCellIdx=49 ], synapse: [ synIdx=4, inIdx=3, sgmtIdx=1, srcCellIdx=3 ]], 2=[synapse: [ synIdx=5, inIdx=733, sgmtIdx=2, srcCellIdx=733 ]], 3=[synapse: [ synIdx=6, inIdx=486, sgmtIdx=3, srcCellIdx=486 ]]}
//        //{0=[synapse: [ synIdx=0, inIdx=23, sgmtIdx=0, srcCellIdx=23 ], synapse: [ synIdx=1, inIdx=37, sgmtIdx=0, srcCellIdx=37 ], synapse: [ synIdx=2, inIdx=477, sgmtIdx=0, srcCellIdx=477 ]], 1=[synapse: [ synIdx=3, inIdx=49, sgmtIdx=1, srcCellIdx=49 ], synapse: [ synIdx=4, inIdx=3, sgmtIdx=1, srcCellIdx=3 ]], 2=[synapse: [ synIdx=5, inIdx=733, sgmtIdx=2, srcCellIdx=733 ]], 3=[synapse: [ synIdx=6, inIdx=486, sgmtIdx=3, srcCellIdx=486 ]]}
//        
//    }
    
    // Test Connections Serialization after running through TemporalMemory
    @Test
    public void testThreadedPublisher_TemporalMemoryNetwork() {
        Network network = createAndRunTestTemporalMemoryNetwork();
        Layer<?> l = network.lookup("r1").lookup("1");
        Connections cn = l.getConnections();
        
        SerialConfig config = new SerialConfig("testThreadedPublisher_TemporalMemoryNetwork", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        
        byte[] bytes = api.write(cn);
        Connections serializedConnections = api.read(bytes);
        
        Network network2 = createAndRunTestTemporalMemoryNetwork();
        Layer<?> l2 = network2.lookup("r1").lookup("1");
        Connections newCons = l2.getConnections();
        
        boolean b = DeepEquals.deepEquals(newCons, serializedConnections);
        deepCompare(newCons, serializedConnections);
        assertTrue(b);
    }
    
    // Test Connections Serialization after running through SpatialPooler
    @Test
    public void testThreadedPublisher_SpatialPoolerNetwork() {
        Network network = createAndRunTestSpatialPoolerNetwork(0, 6);
        Layer<?> l = network.lookup("r1").lookup("1");
        Connections cn = l.getConnections();
        
        SerialConfig config = new SerialConfig("testThreadedPublisher_SpatialPoolerNetwork", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        
        byte[] bytes = api.write(cn);
        //Serialize above Connections for comparison with same run but unserialized below...
        Connections serializedConnections = api.read(bytes);
        
        Network network2 = createAndRunTestSpatialPoolerNetwork(0, 6);
        Layer<?> l2 = network2.lookup("r1").lookup("1");
        Connections newCons = l2.getConnections();
        
        //Compare the two Connections (both serialized and regular runs) - should be equal
        boolean b = DeepEquals.deepEquals(newCons, serializedConnections);
        deepCompare(newCons, serializedConnections);
        assertTrue(b);
    }
    /////////////////////////// End Connections Serialization Testing //////////////////////////////////
    
    /////////////////////
    //    HTMSensor    //
    /////////////////////
    // Serialize HTMSensors though they'll probably be reconstituted rather than serialized
    @Test
    public void testHTMSensor_DaysOfWeek() {
        Object[] n = { "some name", ResourceLocator.path("days-of-week.csv") };
        HTMSensor<File> sensor = (HTMSensor<File>)Sensor.create(
            FileSensor::create, SensorParams.create(Keys::path, n));

        Parameters p = getParameters();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        sensor.initEncoder(p);

        SerialConfig config = new SerialConfig("testHTMSensor_DaysOfWeek", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        
        byte[] bytes = api.write(sensor);
        HTMSensor<File> serializedSensor = api.read(bytes);

        boolean b = DeepEquals.deepEquals(serializedSensor, sensor);
        deepCompare(serializedSensor, sensor);
        assertTrue(b);
    }
    
    @Test
    public void testHTMSensor_HotGym() {
        Object[] n = { "some name", ResourceLocator.path("rec-center-hourly-small.csv") };
        HTMSensor<File> sensor = (HTMSensor<File>)Sensor.create(
            FileSensor::create, SensorParams.create(Keys::path, n));
        
        sensor.initEncoder(getTestEncoderParams());
        
        SerialConfig config = new SerialConfig("testHTMSensor_HotGym");
        PersistenceAPI api = Persistence.get(config);
        
        byte[] bytes = api.write(sensor);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        HTMSensor<File> serializedSensor = api.read(bytes);
        
        boolean b = DeepEquals.deepEquals(serializedSensor, sensor);
        deepCompare(serializedSensor, sensor);
        assertTrue(b);
    }
    
    @Test
    public void testSerializeObservableSensor() {
        PublisherSupplier supplier = PublisherSupplier.builder()
            .addHeader("dayOfWeek")
            .addHeader("darr")
            .addHeader("B").build();
        
        ObservableSensor<String[]> oSensor = new ObservableSensor<>(SensorParams.create(Keys::obs, new Object[] {"name", supplier}));
        
        SerialConfig config = new SerialConfig("testSerializeObservableSensor", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        
        byte[] bytes = api.write(oSensor);
        ObservableSensor<String[]> serializedOSensor = api.read(bytes);
        
        boolean b = DeepEquals.deepEquals(serializedOSensor, oSensor);
        deepCompare(serializedOSensor, oSensor);
        assertTrue(b);
    }
    //////////////////////////////////End HTMSensors ////////////////////////////////////
    
    /////////////////////
    //    Anomaly      //
    /////////////////////
    // Serialize Anomaly, AnomalyLikelihood and its support classes
    @Test
    public void testSerializeAnomaly() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        Anomaly anomalyComputer = Anomaly.create(params);

        // Serialize the Anomaly Computer without errors
        SerialConfig config = new SerialConfig("testSerializeAnomaly1", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        byte[] bytes = api.write(anomalyComputer);

        // Deserialize the Anomaly Computer and make sure its usable (same tests as AnomalyTest.java)
        Anomaly serializedAnomalyComputer = api.read(bytes);
        double score = serializedAnomalyComputer.compute(new int[0], new int[0], 0, 0);
        assertEquals(0.0, score, 0);

        score = serializedAnomalyComputer.compute(new int[0], new int[] {3,5}, 0, 0);
        assertEquals(0.0, score, 0);

        score = serializedAnomalyComputer.compute(new int[] { 3, 5, 7 }, new int[] { 3, 5, 7 }, 0, 0);
        assertEquals(0.0, score, 0);

        score = serializedAnomalyComputer.compute(new int[] { 2, 3, 6 }, new int[] { 3, 5, 7 }, 0, 0);
        assertEquals(2.0 / 3.0, score, 0);
    }
    
    @Test
    public void testSerializeCumulativeAnomaly() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        params.put(KEY_WINDOW_SIZE, 3);
        params.put(KEY_USE_MOVING_AVG, true);
        
        Anomaly anomalyComputer = Anomaly.create(params);
        
        // Serialize the Anomaly Computer without errors
        SerialConfig config = new SerialConfig("testSerializeCumulativeAnomaly", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        byte[] bytes = api.write(anomalyComputer);
        
        // Deserialize the Anomaly Computer and make sure its usable (same tests as AnomalyTest.java)
        Anomaly serializedAnomalyComputer = api.read(bytes);
        assertNotNull(serializedAnomalyComputer);        
        
        Object[] predicted = {
            new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 },
            new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 },
            new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }
        };
        Object[] actual = {
            new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }, new int[] { 1, 4, 6 },
            new int[] { 10, 11, 6 }, new int[] { 10, 11, 12 }, new int[] { 10, 11, 12 },
            new int[] { 10, 11, 12 }, new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }
        };
        
        double[] anomalyExpected = { 0.0, 0.0, 1.0/9.0, 3.0/9.0, 2.0/3.0, 8.0/9.0, 1.0, 2.0/3.0, 1.0/3.0 };
        for(int i = 0;i < 9;i++) {
            double score = serializedAnomalyComputer.compute((int[])actual[i], (int[])predicted[i], 0, 0);
            assertEquals(anomalyExpected[i], score, 0.01);
        }
    }
    
    @Test
    public void testSerializeAnomalyLikelihood() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.LIKELIHOOD);
        AnomalyLikelihood an = (AnomalyLikelihood)Anomaly.create(params);
        
        // Serialize the Anomaly Computer without errors
        SerialConfig config = new SerialConfig("testSerializeAnomalyLikelihood", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        byte[] bytes = api.write(an);
        
        // Deserialize the Anomaly Computer and make sure its usable (same tests as AnomalyTest.java)
        Anomaly serializedAn = api.read(bytes);
        assertNotNull(serializedAn);
    }
    
    @Test
    public void testSerializeAnomalyLikelihoodForUpdates() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.LIKELIHOOD);
        AnomalyLikelihood an = (AnomalyLikelihood)Anomaly.create(params);
        
        // Serialize the Anomaly Computer without errors
        SerialConfig config = new SerialConfig("testSerializeAnomalyLikelihood", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        byte[] bytes = api.write(an);
        
        // Deserialize the Anomaly Computer and make sure its usable (same tests as AnomalyTest.java)
        AnomalyLikelihood serializedAn = api.read(bytes);
        assertNotNull(serializedAn);
        
        //----------------------------------------
        // Step 1. Generate an initial estimate using fake distribution of anomaly scores.
        List<Sample> data1 = AnomalyLikelihoodTest.generateSampleData(0.2, 0.2, 0.2, 0.2).subList(0, 1000);
        AnomalyLikelihoodMetrics metrics1 = serializedAn.estimateAnomalyLikelihoods(data1, 5, 0);
        
        //----------------------------------------
        // Step 2. Generate some new data with a higher average anomaly
        // score. Using the estimator from step 1, to compute likelihoods. Now we
        // should see a lot more anomalies.
        List<Sample> data2 = AnomalyLikelihoodTest.generateSampleData(0.6, 0.2, 0.2, 0.2).subList(0, 300);
        AnomalyLikelihoodMetrics metrics2 = serializedAn.updateAnomalyLikelihoods(data2, metrics1.getParams());
        
        // Serialize the Metrics too just to be sure everything can be serialized
        SerialConfig metricsConfig = new SerialConfig("testSerializeMetrics", SerialConfig.SERIAL_TEST_DIR);
        api = Persistence.get(metricsConfig);
        api.write(metrics2);
        
        // Deserialize the Metrics
        AnomalyLikelihoodMetrics serializedMetrics = api.read();
        assertNotNull(serializedMetrics);
        
        assertEquals(serializedMetrics.getLikelihoods().length, data2.size());
        assertEquals(serializedMetrics.getAvgRecordList().size(), data2.size());
        assertTrue(serializedAn.isValidEstimatorParams(serializedMetrics.getParams()));
        
        // The new running total should be different
        assertFalse(metrics1.getAvgRecordList().total == serializedMetrics.getAvgRecordList().total);
        
        // We should have many more samples where likelihood is < 0.01, but not all
        Condition<Double> cond = new Condition.Adapter<Double>() {
            public boolean eval(double d) { return d < 0.01; }
        };
        int conditionCount = ArrayUtils.where(serializedMetrics.getLikelihoods(), cond).length;
        assertTrue(conditionCount >= 25);
        assertTrue(conditionCount <= 250);
    }
    ///////////////////////   End Serialize Anomaly //////////////////////////
    
    ///////////////////////////
    //      CLAClassifier    //
    ///////////////////////////
    // Test Serialize CLAClassifier
    @Test
    public void testSerializeCLAClassifier() {
        CLAClassifier classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
        int recordNum = 0;
        Map<String, Object> classification = new LinkedHashMap<String, Object>();
        classification.put("bucketIdx", 4);
        classification.put("actValue", 34.7);
        Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
        recordNum += 1;

        classification.put("bucketIdx", 5);
        classification.put("actValue", 41.7);
        result = classifier.compute(recordNum, classification, new int[] { 0, 6, 9, 11 }, true, true);
        recordNum += 1;

        classification.put("bucketIdx", 5);
        classification.put("actValue", 44.9);
        result = classifier.compute(recordNum, classification, new int[] { 6, 9 }, true, true);
        recordNum += 1;

        classification.put("bucketIdx", 4);
        classification.put("actValue", 42.9);
        result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
        recordNum += 1;

        // Serialize the Metrics too just to be sure everything can be serialized
        SerialConfig config = new SerialConfig("testSerializeCLAClassifier", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        api.write(classifier);

        // Deserialize the Metrics
        CLAClassifier serializedClassifier = api.read();
        assertNotNull(serializedClassifier);

        //Using the deserialized classifier, continue test
        classification.put("bucketIdx", 4);
        classification.put("actValue", 34.7);
        result = serializedClassifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
        recordNum += 1;

        assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
        assertEquals(35.520000457763672, result.getActualValue(4), 0.00001);
        assertEquals(42.020000457763672, result.getActualValue(5), 0.00001);
        assertEquals(6, result.getStatCount(1));
        assertEquals(0.0, result.getStat(1, 0), 0.00001);
        assertEquals(0.0, result.getStat(1, 1), 0.00001);
        assertEquals(0.0, result.getStat(1, 2), 0.00001);
        assertEquals(0.0, result.getStat(1, 3), 0.00001);
        assertEquals(0.12300123, result.getStat(1, 4), 0.00001);
        assertEquals(0.87699877, result.getStat(1, 5), 0.00001);
    }
    ////////////////////////  End CLAClassifier ///////////////////////
    
    ///////////////////////////
    //         Layers        //
    ///////////////////////////
    // Serialize a Layer
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testSerializeLayer() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            8,    // n
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

        p.set(KEY.FIELD_ENCODING_MAP, settings);

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", 
                PublisherSupplier.builder()
                .addHeader("dayOfWeek")
                .addHeader("darr")
                .addHeader("B").build() }));

        Layer<?> layer = Network.createLayer("1", p)
            .alterParameter(KEY.AUTO_CLASSIFY, true)
            .add(new SpatialPooler())
            .add(sensor);

        Observer obs = new Observer<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference spatialPoolerOutput) {
                System.out.println("in onNext()");
            }
        };
        layer.subscribe(obs);
        layer.close();

        SerialConfig config = new SerialConfig("testSerializeLayer", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        api.write(layer);

        //Serialize above Connections for comparison with same run but unserialized below...
        Layer<?> serializedLayer = api.read();
        assertEquals(serializedLayer, layer);
        deepCompare(layer, serializedLayer);

        // Now change one attribute and see that they are not equal
        serializedLayer.resetRecordNum();
        assertNotEquals(serializedLayer, layer);
    }
    //////////////////////  End Layers  ///////////////////////
    
    
    ///////////////////////////
    //      Full Network     //
    ///////////////////////////
    @Test
    public void testHierarchicalNetwork() {
        Network network = getLoadedHotGymHierarchy();
        try {
            SerialConfig config = new SerialConfig("testSerializeHierarchy", SerialConfig.SERIAL_TEST_DIR);
            PersistenceAPI api = Persistence.get(config);
            api.store(network);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    
    /**
     * Test that a serialized/de-serialized {@link Network} can be run...
     */
    @Test
    public void testSerializedUnStartedNetworkRuns() {
        final int NUM_CYCLES = 600;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        
        Network network = getLoadedDayOfWeekNetwork();
        
        SerialConfig config = new SerialConfig("testSerializedUnStartedNetworkRuns", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        api.store(network);
        
        //Serialize above Connections for comparison with same run but unserialized below...
        Network serializedNetwork = api.load();
        assertEquals(serializedNetwork, network);
        deepCompare(network, serializedNetwork);
        
        int cellsPerCol = (int)serializedNetwork.getParameters().get(KEY.CELLS_PER_COLUMN);
        
        serializedNetwork.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
                /** see {@link #createDayOfWeekInferencePrintout()} */
                dayOfWeekPrintout.apply(inf, cellsPerCol);
            }
        });
        
        Publisher pub = serializedNetwork.getPublisher();
        
        serializedNetwork.start();
        
        int cycleCount = 0;
        for(;cycleCount < NUM_CYCLES;cycleCount++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                pub.onNext("" + j);
            }
            
            serializedNetwork.reset();
            
            if(cycleCount == 284) {
                break;
            }
        }
        
        pub.onComplete();
        
        try {
            Region r1 = serializedNetwork.lookup("r1");
            r1.lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * The {@link DateEncoder} presents a special challenge because its main
     * field (the DateFormatter) is not serializable and requires its state (format)
     * to be saved and re-installed following de-serialization.
     */
    @Test
    public void testSerializedUnStartedNetworkRuns_DateEncoder() {
        final int NUM_CYCLES = 100;
        
        Network network = getLoadedHotGymNetwork();
        
        SerialConfig config = new SerialConfig("testSerializedUnStartedNetworkRuns_DateEncoderFST", 
            SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        api.store(network);
        
        //Serialize above Connections for comparison with same run but unserialized below...
        Network serializedNetwork = api.load();
        assertEquals(serializedNetwork, network);
        deepCompare(network, serializedNetwork);
        
        TestObserver<Inference> tester;
        serializedNetwork.observe().subscribe(tester = new TestObserver<Inference>() { 
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
                assertNotNull(inf);
            }
        });
        
        Publisher pub = serializedNetwork.getPublisher();
        
        serializedNetwork.start();
        
        int cycleCount = 0;
        List<String> hotStream = makeStream().collect(Collectors.toList());
        for(;cycleCount < NUM_CYCLES;cycleCount++) {
            for(String s : hotStream) {
                pub.onNext(s);
            }
            
            serializedNetwork.reset();
        }
        
        pub.onComplete();
        
        try {
            Region r1 = serializedNetwork.lookup("r1");
            r1.lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        // Check for exception during the TestObserver's onNext() execution.
        checkObserver(tester);
    }
    
    /**
     * Runs two de-serialized {@link Networks}  in lock-step using a {@link CyclicBarrier}, 
     * checking that their RNG seeds are exactly the same after each compute cycle - which 
     * guarantees that the output of both Networks is the same (the RNGs must be called 
     * exactly the same amount of times for this to be true).
     */
    long[] barrierSeeds = new long[2];
    CyclicBarrier barrier;
    int runCycleCount = 0;
    @Test
    public void testDeserializedInstancesRunExactlyTheSame() {
        final int NUM_CYCLES = 100;
        List<String> hotStream = null;
        
        Network network = getLoadedHotGymNetwork();
        
        SerialConfig config = new SerialConfig("testDeserializedInstancesRunExactlyTheSame", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        api.store(network);
        
        Network serializedNetwork1 = api.load();
        Network serializedNetwork2 = api.load();
        
        FastRandom r1 = (FastRandom)serializedNetwork1.lookup("r1").lookup("1").getConnections().getRandom();
        FastRandom r2 = (FastRandom)serializedNetwork2.lookup("r1").lookup("1").getConnections().getRandom();
        // Assert both starting seeds are equal
        assertEquals(r1.getSeed(), r2.getSeed());
        
        // CyclicBarrier which compares each Network's RNG after every compute cycle, and asserts they are equal.
        barrier = new CyclicBarrier(3, () -> {
            try {
                assertEquals(barrierSeeds[0], barrierSeeds[1]);
            }catch(Exception barrierEx) {
                System.out.println("Seed comparison failed at: " + runCycleCount);
                System.exit(1);
            }
        });
        
        serializedNetwork1.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
                barrierSeeds[0] = r1.getSeed();
                try { barrier.await(); }catch(Exception b) { b.printStackTrace(); System.exit(1);}
            }
        });
        serializedNetwork2.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
                barrierSeeds[1] = r2.getSeed();
                try { barrier.await(); }catch(Exception b) { b.printStackTrace(); }
            }
        });
        
        Publisher pub1 = serializedNetwork1.getPublisher();
        Publisher pub2 = serializedNetwork2.getPublisher();
        
        serializedNetwork1.start();
        serializedNetwork2.start();
        
        runCycleCount = 0;
        hotStream = makeStream().collect(Collectors.toList());
        for(;runCycleCount < NUM_CYCLES;runCycleCount++) {
            for(String s : hotStream) {
                pub1.onNext(s);
                pub2.onNext(s);
                try { barrier.await(); }catch(Exception b) { b.printStackTrace(); fail();  }
            }
        }
        
        pub1.onComplete();
        pub2.onComplete();
        
        try {
            serializedNetwork1.lookup("r1").lookup("1").getLayerThread().join();
            serializedNetwork2.lookup("r1").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Ensure that a Network run uninterrupted will have the same output as a {@link Network}
     * that has been halted; serialized, and restarted! This test runs a {@link Network}
     * all the way through, recording 10 outputs between set indexes. Then runs a second
     * Network, stopping in the middle of those indexes and serializing the Network. Then
     * de-serializes that Network, and continues on - both pre and post serialized Networks
     * record the same indexes as the first Network that ran all the way through. The outputs
     * of both Networks from the start to finish indexes are compared and tested that they
     * are exactly the same.
     */
    @Test
    public void testRunSerializedNetworkWithFileSensor() {
        // Stores the sample comparison outputs at the indicated record numbers.
        List<String> sampleExpectedOutput = new ArrayList<>(10);
        
        // Run the network all the way, while storing a sample of 10 outputs.
        Network net = getLoadedHotGymNetwork_FileSensor();
        net.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
                if(inf.getRecordNum() > 1105 && inf.getRecordNum() <= 1115) {
                    sampleExpectedOutput.add("" + inf.getRecordNum() + ":  " + Arrays.toString((int[])inf.getLayerInput()) + ", " + inf.getAnomalyScore());
                }
            }
        });
        
        net.start();
        
        try {
            net.lookup("r1").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        
        // Now run the network part way through, halting in between the save points above
        Network network = getLoadedHotGymNetwork_FileSensor();
        
        // Store the actual outputs and the same record number indexes for comparison across pre and post serialized networks.
        List<String> actualOutputs = new ArrayList<>();
        
        network.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
                if(inf.getRecordNum() > 1105 && inf.getRecordNum() <= 1115) {
                    actualOutputs.add("" + inf.getRecordNum() + ":  " + Arrays.toString((int[])inf.getLayerInput()) + ", " + inf.getAnomalyScore());
                }
                
                if(inf.getRecordNum() == 1109) {
                    network.halt();
                }
            }
        });
        
        network.start();
        
        try {
            network.lookup("r1").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        SerialConfig config = new SerialConfig("testRunSerializedNetworkWithFileSensor", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        api.store(network);
        
        /////////////////////////////////////////////////////////
        
        // Now run the serialized network 
        Network serializedNetwork = api.load();
        
        serializedNetwork.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
                if(inf.getRecordNum() > 1105 && inf.getRecordNum() <= 1115) {
                    actualOutputs.add("" + inf.getRecordNum() + ":  " + Arrays.toString((int[])inf.getLayerInput()) + ", " + inf.getAnomalyScore());
                }
            }
        });
        
        serializedNetwork.start();
        
        try {
            serializedNetwork.lookup("r1").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        assertEquals(sampleExpectedOutput.size(), actualOutputs.size());
        assertTrue(DeepEquals.deepEquals(sampleExpectedOutput, actualOutputs));
    }
    
    /**
     * Tests the Network Serialization API
     */
    @Test
    public void testStoreAndLoad_FileSensor() {
        Network network = getLoadedHotGymNetwork_FileSensor();
        PersistenceAPI api = Persistence.get();
        
        TestObserver<Inference> tester;
        network.observe().subscribe(tester = new TestObserver<Inference>() { 
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
//                System.out.println("" + inf.getRecordNum() + ", " + inf.getAnomalyScore());
                if(inf.getRecordNum() == 500) {
                    /////////////////////////////////
                    //      Network Store Here     //
                    /////////////////////////////////
                    api.store(network);
                }
            }
        });
        
        network.start();
        
        try {
            network.lookup("r1").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        Network serializedNetwork = api.load();
        
        TestObserver<Inference> tester2;
        serializedNetwork.observe().subscribe(tester2 = new TestObserver<Inference>() { 
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
//                System.out.println("1: " + inf.getRecordNum() + ", " + inf.getAnomalyScore());
                assertEquals(501, inf.getRecordNum());
                if(inf.getRecordNum() == 501) {
                    serializedNetwork.halt();
//                    System.out.println("should not see output after this line");
                }else{
                    fail();
                }
            }
        });
        
        serializedNetwork.restart();
        
        try {
            serializedNetwork.lookup("r1").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        
        // Test that we can start the Network from the beginning of the stream.
        Network serializedNetwork2 = api.load();
        
        TestObserver<Inference> tester3;
        serializedNetwork2.observe().subscribe(tester3 = new TestObserver<Inference>() { 
            int idx = 0;
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
//                System.out.println("2: " + inf.getRecordNum() + ", " + inf.getAnomalyScore());
                if(idx != inf.getRecordNum()) {
                    fail();
                    if(idx == 500) serializedNetwork2.halt();
                }
                ++idx;
            }
        });
        
        serializedNetwork2.restart(false);
        
        try {
            serializedNetwork2.lookup("r1").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        
        checkObserver(tester);
        checkObserver(tester2);
        checkObserver(tester3);
    }
    
    /**
     * This test stops and starts a Network which has a Publisher - which is not the way
     * it would be done in production as you could just stop entering data, and continue
     * again as desired. The user controls the data flow when using a Publisher.
     */
    @Test
    public void testStoreAndLoad_ObservableSensor() {
        Network network = getLoadedHotGymNetwork();
        PersistenceAPI api = Persistence.get();
        
        TestObserver<Inference> tester;
        network.observe().subscribe(tester = new TestObserver<Inference>() { 
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
//                System.out.println("" + inf.getRecordNum() + ", " + inf.getAnomalyScore() + (inf.getRecordNum() == 0 ? Arrays.toString((int[])inf.getLayerInput()) : ""));
                if(inf.getRecordNum() == 499) {
                    /////////////////////////////////
                    //      Network Store Here     //
                    /////////////////////////////////
                    api.store(network);
                }
            }
        });
        
        network.start();
        
        Publisher publisher = network.getPublisher();
        
        List<String> hotStream = makeStream().collect(Collectors.toList());
        int numRecords = 0;
        boolean done = false;
        while(true) {
            for(String s : hotStream) {
                publisher.onNext(s);
                numRecords++;
                if(numRecords == 500) {
                    done = true;
                    break;
                }
            }
            if(done) break;
        }
        
        try {
            network.lookup("r1").lookup("1").getLayerThread().join();
//            System.out.println("------------------> buffer size = " + publisher.getBufferSize());
//            System.out.println("NETWORK TEST RECORD NUM: " + network.getRecordNum());
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        Network serializedNetwork = api.load();
        
        TestObserver<Inference> tester2;
        serializedNetwork.observe().subscribe(tester2 = new TestObserver<Inference>() { 
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
//                System.out.println("1: " + inf.getRecordNum() + ", " + inf.getAnomalyScore());
                if(inf.getRecordNum() == 500) {
                    assertEquals(500, inf.getRecordNum());
                    serializedNetwork.halt();
                }else{
                    fail();
                }
            }
        });
        
        boolean startAtIndex = true;
        serializedNetwork.restart(startAtIndex);
        
        // IMPORTANT: Re-acquire the publisher after restart! (Can't use old one)
        publisher = serializedNetwork.getPublisher();
        
        for(String s : hotStream) {
            publisher.onNext(s);
            numRecords++;
            if(numRecords > 500) {
                break;
            }
        }
        
        try {
            serializedNetwork.lookup("r1").lookup("1").getLayerThread().join(5000);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        // Test that we can start the Network from the beginning of the stream.
        Network serializedNetwork2 = api.load();
        
        TestObserver<Inference> tester3;
        serializedNetwork2.observe().subscribe(tester3 = new TestObserver<Inference>() { 
            int idx = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
//                System.out.println("2: " + inf.getRecordNum() + ", " + inf.getAnomalyScore());
                if(idx != inf.getRecordNum()) {
                    fail();
                }
                assertTrue(idx == 0 && idx == inf.getRecordNum());
            }
        });
        
        startAtIndex = false;
        serializedNetwork2.restart(startAtIndex);
        
        // IMPORTANT: Re-acquire the publisher after restart! (Can't use old one)
        publisher = serializedNetwork2.getPublisher();
        
        publisher.onNext(hotStream.get(0));
        
        try {
            serializedNetwork2.lookup("r1").lookup("1").getLayerThread().join(5000);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        checkObserver(tester);
        checkObserver(tester2);
        checkObserver(tester3);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testStoreAndLoad_SynchronousNetwork() {
        Network network = getLoadedHotGymSynchronousNetwork();
        PersistenceAPI api = Persistence.get();
        
        Map<String, Map<String, Object>> fieldEncodingMap = 
            (Map<String, Map<String, Object>>)network.getParameters().get(KEY.FIELD_ENCODING_MAP);
        
        MultiEncoder me = MultiEncoder.builder()
            .name("")
            .build()
            .addMultipleEncoders(fieldEncodingMap);
        network.lookup("r1").lookup("1").add(me);
    
        // We just use this to parse the date field
        DateEncoder dateEncoder = me.getEncoderOfType(FieldMetaType.DATETIME);
        
        Map<String, Object> m = new HashMap<>();
        List<String> l = makeStream().collect(Collectors.toList());
        for(int j = 0;j < 50;j++) {
            for(int i = 0;i < 20;i++) {
                String[] sa = l.get(i).split("[\\s]*\\,[\\s]*");
                m.put("timestamp", dateEncoder.parse(sa[0]));
                m.put("consumption", Double.parseDouble(sa[1]));
//                System.out.println(m);
//                Inference inf = network.computeImmediate(m);
                network.computeImmediate(m);
//                System.out.println("" + inf.getRecordNum() + ", " + inf.getAnomalyScore());
            }
            network.reset();
        }
        
        //////////////////////////////////////
        //        Store the Network         //
        //////////////////////////////////////
        api.store(network);
        
        
        //////////////////////////////////////
        //        Reload the Network        //
        //////////////////////////////////////
        Network serializedNetwork = api.load();
        
        boolean serializedNetworkRan = false;
        // Pump data through the serialized Network
        for(int j = 0;j < 50;j++) {
            for(int i = 0;i < 20;i++) {
                String[] sa = l.get(i).split("[\\s]*\\,[\\s]*");
                m.put("timestamp", dateEncoder.parse(sa[0]));
                m.put("consumption", Double.parseDouble(sa[1]));
//                System.out.println(m);
                Inference inf = serializedNetwork.computeImmediate(m);
                serializedNetworkRan = inf.getRecordNum() > 0;
//                System.out.println("2: " + inf.getRecordNum() + ", " + inf.getAnomalyScore() + ",  " + Arrays.toString((int[])inf.getEncoding()));
            }
            serializedNetwork.reset();
        }
        
        assertTrue(serializedNetwork != null);
        assertTrue(serializedNetworkRan);
    }
    
    TestObserver<byte[]> nestedTester;
    @Test
    public void testCheckpoint_FileSensor() {
        Network network = getLoadedHotGymNetwork_FileSensor();
        PersistenceAPI api = Persistence.get();
        
        SerialConfig config = api.getConfig();
        config.setOneCheckPointOnly(false);
        
        TestObserver<Inference> tester;
        network.observe().subscribe(tester = new TestObserver<Inference>() { 
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
                if(inf.getRecordNum() == 500 || inf.getRecordNum() == 750) {
                    /////////////////////////////////
                    //      Network Store Here     //
                    /////////////////////////////////
                    api.checkPointer(network).checkPoint(nestedTester = new TestObserver<byte[]>() { 
                        @Override public void onCompleted() {}
                        @Override public void onError(Throwable e) { e.printStackTrace(); }
                        @Override public void onNext(byte[] bytes) {
                            assertTrue(bytes != null && bytes.length > 10);
                        }
                    });
                }else if(inf.getRecordNum() == 1000) {
                    network.halt();
                }
            }
        });
        
        network.start();
        
        try {
            network.lookup("r1").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        assertTrue(api.getLastCheckPoint() != null);
        

        /////////////////////// Now test the checkpointed Network /////////////////////////
        
        //////////////////////////////////////
        //       CheckPoint the Network     //
        //////////////////////////////////////

        Network checkPointNetwork = null;
        try {
            checkPointNetwork = api.load(api.getLastCheckPointFileName());
            assertNotNull(checkPointNetwork);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }

        TestObserver<Inference> tester2;
        final Network cpn = checkPointNetwork;
        checkPointNetwork.observe().subscribe(tester2 = new TestObserver<Inference>() { 
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
                // Assert that the records continue from where the checkpoint left off.
                assertEquals(752, inf.getRecordNum());
                cpn.halt();
            }
        });
        
        checkPointNetwork.restart();
        
        try {
            checkPointNetwork.lookup("r1").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        checkObserver(tester);
        checkObserver(nestedTester);
        checkObserver(tester2);
    }
    
    
    TestObserver<byte[]> nestedTester2;
    @Test
    public void testCheckpoint_ObservableSensor() {
        Network network = getLoadedHotGymNetwork();
        PersistenceAPI api = Persistence.get();
        
        SerialConfig conf = api.getConfig();
        assertNotNull(conf);
        conf.setOneCheckPointOnly(false);
        
        TestObserver<Inference> tester;
        network.observe().subscribe(tester = new TestObserver<Inference>() { 
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
                if(inf.getRecordNum() == 500 || inf.getRecordNum() == 750) {
                    /////////////////////////////////
                    //    Network CheckPoint Here  //
                    /////////////////////////////////
                    api.checkPointer(network).checkPoint(nestedTester2 = new TestObserver<byte[]>() { 
                        @Override public void onCompleted() {}
                        @Override public void onNext(byte[] bytes) {
                            assertTrue(bytes != null && bytes.length > 10);
                        }
                    });
                }else if(inf.getRecordNum() == 999) {
                    network.halt();
                }
                
            }
        });
        
        network.start();
        
        Publisher publisher = network.getPublisher();
        
        List<String> hotStream = makeStream().collect(Collectors.toList());
        int numRecords = 0;
        boolean done = false;
        while(true) {
            for(String s : hotStream) {
                publisher.onNext(s);
                numRecords++;
                if(numRecords == 1000) {
                    done = true; break;
                }
            }
            if(done) break;
        }
        
        try {
            network.lookup("r1").lookup("1").getLayerThread().join(5000);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        /////////////////////// Now test the checkpointed Network /////////////////////////
        
        //////////////////////////////////////
        //       CheckPoint the Network     //
        //////////////////////////////////////

        Network checkPointNetwork = null;
        try {
            checkPointNetwork = api.load(api.getLastCheckPointFileName());
            assertNotNull(checkPointNetwork);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }

        TestObserver<Inference> tester2;
        final Network cpn = checkPointNetwork;
        checkPointNetwork.observe().subscribe(tester2 = new TestObserver<Inference>() { 
            @Override public void onCompleted() {}
            @Override
            public void onNext(Inference inf) {
                // Assert that the records continue from where the checkpoint left off.
                assertEquals(752, inf.getRecordNum());
                cpn.halt();
            }
        });
        
        checkPointNetwork.restart();
        
        publisher = checkPointNetwork.getPublisher();
        
        numRecords = 0;
        done = false;
        while(true) {
            for(String s : hotStream) {
                publisher.onNext(s);
                numRecords++;
                if(numRecords == 25) {
                    done = true; break;
                }
            }
            if(done) break;
        }
        
        try {
            checkPointNetwork.lookup("r1").lookup("1").getLayerThread().join(2000);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        checkObserver(tester);
        checkObserver(tester2);
        checkObserver(nestedTester2);
    }
    
    TestObserver<byte[]> nestedTester3;
    @SuppressWarnings("unchecked")
    @Test
    public void testCheckPoint_SynchronousNetwork() {
        Network network = getLoadedHotGymSynchronousNetwork();
        PersistenceAPI api = Persistence.get();
        
        Map<String, Map<String, Object>> fieldEncodingMap = 
            (Map<String, Map<String, Object>>)network.getParameters().get(KEY.FIELD_ENCODING_MAP);
        
        MultiEncoder me = MultiEncoder.builder()
            .name("")
            .build()
            .addMultipleEncoders(fieldEncodingMap);
        network.lookup("r1").lookup("1").add(me);
    
        // We just use this to parse the date field
        DateEncoder dateEncoder = me.getEncoderOfType(FieldMetaType.DATETIME);
        
        Map<String, Object> m = new HashMap<>();
        List<String> l = makeStream().collect(Collectors.toList());
        for(int j = 0;j < 50;j++) {
            for(int i = 0;i < 20;i++) {
                String[] sa = l.get(i).split("[\\s]*\\,[\\s]*");
                m.put("timestamp", dateEncoder.parse(sa[0]));
                m.put("consumption", Double.parseDouble(sa[1]));
                network.computeImmediate(m);
                
                if(j == 49 && i == 0) {
                    api.checkPointer(network).checkPoint(nestedTester3 = new TestObserver<byte[]>() { 
                        @Override public void onCompleted() {}
                        @Override public void onNext(byte[] bytes) {
                            assertTrue(bytes != null && bytes.length > 10);
                        }
                    });
                }
            }
            network.reset();
        }
        
        //////////////////////////////////////
        //       CheckPoint the Network     //
        //////////////////////////////////////
        
        Network checkPointNetwork = null;
        try {
            checkPointNetwork = api.load(api.getLastCheckPointFileName());
            assertNotNull(checkPointNetwork);
        }catch(Exception e) {
            fail();
        }
        
        int postCheckPointProcessCount = 0;
        
        for(int j = 0;j < 1;j++) {
            for(int i = 0;i < 20;i++) {
                String[] sa = l.get(i).split("[\\s]*\\,[\\s]*");
                m.put("timestamp", dateEncoder.parse(sa[0]));
                m.put("consumption", Double.parseDouble(sa[1]));
                Inference inf = checkPointNetwork.computeImmediate(m);
                // Test that we being processing where the checkpoint left off...
                assertTrue(inf.getRecordNum() == 981 + i);
                ++postCheckPointProcessCount;
            }
            checkPointNetwork.reset();
        }
        
        assertTrue(postCheckPointProcessCount > 19);
        
        checkObserver(nestedTester3);
    }
    
    
    TestObserver<byte[]> nestedTester4;
    @Test
    public void testCheckPointHierarchies() {
        Network network = getLoadedDayOfWeekStreamHierarchy();
        PersistenceAPI api = Persistence.get();
        
        TestObserver<Inference> tester;
        network.observe().subscribe(tester = new TestObserver<Inference>() {
            int cycles = 0;
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                if(cycles++ == 10) {
                  ////////////////////////
                  //   CheckPoint Here  //
                  ////////////////////////
                    api.checkPointer(network).checkPoint(nestedTester4 = new TestObserver<byte[]>() { 
                        @Override public void onCompleted() {}
                        @Override public void onNext(byte[] bytes) {
                            assertEquals(10, i.getRecordNum());
                            assertTrue(bytes != null && bytes.length > 10);
                        }
                    });
                }else if(cycles == 12) {
                    network.halt();
                }
            }
        });
        
        network.start();
        
        try {
            network.lookup("r2").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        
        //////////////////////////////////////
        //       CheckPoint the Network     //
        //////////////////////////////////////
        
        Network cpn = null;
        try {
            cpn = api.load(api.getLastCheckPointFileName());
            assertNotNull(cpn);
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
        
        TestObserver<Inference> tester2;
        final Network checkPointNetwork  = cpn;
        checkPointNetwork.observe().subscribe(tester2 = new TestObserver<Inference>() {
            int cycles = 0;
            @Override public void onCompleted() {}
            @Override public void onNext(Inference i) {
                if(cycles++ == 10) {
                    assertEquals(21, i.getRecordNum());
                    checkPointNetwork.halt();
                }
            }
        });
        
        checkPointNetwork.start();
        
        try {
            checkPointNetwork.lookup("r2").lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        checkObserver(tester);
        checkObserver(tester2);
        checkObserver(nestedTester4);
    }
    
    //////////////////////////////
    //     Utility Methods      //
    //////////////////////////////
    private void deepCompare(Object obj1, Object obj2) {
        try {
            assertTrue(DeepEquals.deepEquals(obj1, obj2));
            System.out.println("expected(" + obj1.getClass().getSimpleName() + "): " + obj1 + " actual: (" + obj1.getClass().getSimpleName() + "): " + obj2);
        }catch(AssertionError ae) {
            System.out.println("expected(" + obj1.getClass().getSimpleName() + "): " + obj1 + " but was: (" + obj1.getClass().getSimpleName() + "): " + obj2);
        }
    }
    
    private Network getLoadedDayOfWeekStreamHierarchy() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new FastRandom(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));
        
        Layer<?> l2 = null;
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(l2 = Network.createLayer("2", p)
                    .add(Anomaly.create())
                    .add(new TemporalMemory()))
                .add(Network.createLayer("3", p)
                    .add(new SpatialPooler())
                    .using(l2.getConnections()))
                .connect("2", "3"))
            .add(Network.createRegion("r2")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("days-of-week-stream.csv"))))))
            .connect("r1", "r2");
        
        return network;
    }

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

    private Network getLoadedHotGymHierarchy() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("2", p)
                    .add(Anomaly.create())
                    .add(new TemporalMemory()))
                .add(Network.createLayer("3", p)
                    .add(new SpatialPooler()))
                .connect("2", "3"))
            .add(Network.createRegion("r2")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))))
            .connect("r1", "r2");

        return network;
    }

    private Network getLoadedHotGymNetwork() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new FastRandom(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", 
                PublisherSupplier.builder()
                .addHeader("timestamp, consumption")
                .addHeader("datetime, float")
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
    
    private Network getLoadedHotGymSynchronousNetwork() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new FastRandom(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

        Network network = Network.create("test network", p).add(Network.createRegion("r1")
            .add(Network.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, true)
                .add(Anomaly.create())
                .add(new TemporalMemory())
                .add(new SpatialPooler())));
                
        return network;
    }

    private Network getLoadedHotGymNetwork_FileSensor() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new FastRandom(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

        Object[] n = { "some name", ResourceLocator.path("rec-center-hourly.csv") };
        HTMSensor<File> sensor = (HTMSensor<File>)Sensor.create(
            FileSensor::create, SensorParams.create(Keys::path, n));

        Network network = Network.create("test network", p).add(Network.createRegion("r1")
            .add(Network.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, true)
                .add(Anomaly.create())
                .add(new TemporalMemory())
                .add(new SpatialPooler())
                .add(sensor)));

        return network;
    }

    private Network createAndRunTestSpatialPoolerNetwork(int start, int runTo) {
        Publisher manual = Publisher.builder()
            .addHeader("dayOfWeek")
            .addHeader("darr")
            .addHeader("B").build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", manual}));

        Parameters p = NetworkTestHarness.getParameters().copy();
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));

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

        p.set(KEY.FIELD_ENCODING_MAP, settings);

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

        int[] expected0 = new int[] { 0, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
        int[] expected1 = new int[] { 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0 };
        int[] expected2 = new int[] { 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 };
        int[] expected3 = new int[] { 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0 };
        int[] expected4 = new int[] { 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0 };
        int[] expected5 = new int[] { 1, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0 };
        int[] expected6 = new int[] { 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0 };
        int[][] expecteds = new int[][] { expected0, expected1, expected2, expected3, expected4, expected5, expected6 };

        TestObserver<Inference> tester;
        network.observe().subscribe(tester = new TestObserver<Inference>() {
            int test = 0;

            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { 
                super.onError(e);
                e.printStackTrace(); 
            }
            @Override
            public void onNext(Inference spatialPoolerOutput) {
//                System.out.println("expected: " + Arrays.toString(expecteds[test]) + "  --  " +
//                    "actual: " + Arrays.toString(spatialPoolerOutput.getSDR()));
                assertTrue(Arrays.equals(expecteds[test++], spatialPoolerOutput.getSDR()));
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
        
        checkObserver(tester);

        return network;
    }

    private Network createAndRunTestTemporalMemoryNetwork() {
        Publisher manual = Publisher.builder()
            .addHeader("dayOfWeek")
            .addHeader("darr")
            .addHeader("B").build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", manual}));

        Parameters p = getParameters();
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("dayOfWeek", CLAClassifier.class));

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

        p.set(KEY.FIELD_ENCODING_MAP, settings);

        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .add(new TemporalMemory())
                    .add(sensor)));

        network.start();

        network.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {}
        });

        final int[] input1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] input2 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] input3 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
        final int[] input4 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input5 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input6 = new int[] { 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 };
        final int[] input7 = new int[] { 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0 };
        final int[][] inputs = { input1, input2, input3, input4, input5, input6, input7 };

        // Run until TemporalMemory is "warmed up".
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

        Parameters p = Parameters.getEncoderDefaultParameters();
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

    private BiFunction<Inference, Integer, Integer> createDayOfWeekInferencePrintout(boolean on) {
        return new BiFunction<Inference, Integer, Integer>() {
            private int cycles = 1;

            public Integer apply(Inference inf, Integer cellsPerColumn) {
                Classification<Object> result = inf.getClassification("dayOfWeek");
                double day = mapToInputData((int[])inf.getLayerInput());
                if(day == 1.0) {
                    if(on) {
                        System.out.println("\n=========================");
                        System.out.println("CYCLE: " + cycles);
                    }
                    cycles++;
                }

                if(on) {
                    System.out.println("RECORD_NUM: " + inf.getRecordNum());
                    System.out.println("ScalarEncoder Input = " + day);
                    System.out.println("ScalarEncoder Output = " + Arrays.toString(inf.getEncoding()));
                    System.out.println("SpatialPooler Output = " + Arrays.toString(inf.getFeedForwardActiveColumns()));

                    if(inf.getPreviousPredictiveCells() != null)
                        System.out.println("TemporalMemory Previous Prediction = " + 
                            Arrays.toString(SDR.cellsAsColumnIndices(inf.getPreviousPredictiveCells(), cellsPerColumn)));

                    System.out.println("TemporalMemory Actives = " + Arrays.toString(SDR.asColumnIndices(inf.getSDR(), cellsPerColumn)));

                    System.out.print("CLAClassifier prediction = " + 
                        stringValue((Double)result.getMostProbableValue(1)) + " --> " + ((Double)result.getMostProbableValue(1)));

                    System.out.println("  |  CLAClassifier 1 step prob = " + Arrays.toString(result.getStats(1)) + "\n");
                }

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

    private int[][] dayMap = new int[][] { 
        new int[] { 1, 1, 0, 0, 0, 0, 0, 1 },
        new int[] { 1, 1, 1, 0, 0, 0, 0, 0 },
        new int[] { 0, 1, 1, 1, 0, 0, 0, 0 },
        new int[] { 0, 0, 1, 1, 1, 0, 0, 0 },
        new int[] { 0, 0, 0, 1, 1, 1, 0, 0 },
        new int[] { 0, 0, 0, 0, 1, 1, 1, 0 },
        new int[] { 0, 0, 0, 0, 0, 1, 1, 1 },
    };

    private String stringValue(Double valueIndex) {
        String recordOut = "";
        BigDecimal bdValue = new BigDecimal(valueIndex).setScale(3, RoundingMode.HALF_EVEN);
        switch(bdValue.intValue()) {
            case 1: recordOut = "Monday (1)";break;
            case 2: recordOut = "Tuesday (2)";break;
            case 3: recordOut = "Wednesday (3)";break;
            case 4: recordOut = "Thursday (4)";break;
            case 5: recordOut = "Friday (5)";break;
            case 6: recordOut = "Saturday (6)";break;
            case 0: recordOut = "Sunday (7)";break;
        }
        return recordOut;
    }
    
    public Parameters getParameters() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 20 });
        parameters.set(KEY.CELLS_PER_COLUMN, 6);
        
        //SpatialPooler specific
        parameters.set(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.set(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.set(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.1);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.1);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.set(KEY.MAX_BOOST, 10.0);
        parameters.set(KEY.SEED, 42);
        
        //Temporal Memory specific
        parameters.set(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.set(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.set(KEY.MIN_THRESHOLD, 5);
        parameters.set(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.set(KEY.PERMANENCE_INCREMENT, 0.05);
        parameters.set(KEY.PERMANENCE_DECREMENT, 0.05);
        parameters.set(KEY.ACTIVATION_THRESHOLD, 4);
        parameters.set(KEY.RANDOM, new FastRandom(42));
        
        return parameters;
    }

}
