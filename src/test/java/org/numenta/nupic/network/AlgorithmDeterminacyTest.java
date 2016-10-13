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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.ProximalDendrite;
import org.numenta.nupic.model.SDR;
import org.numenta.nupic.model.Segment;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwister;

import rx.Observer;
import rx.Subscriber;

/**
 * <p>
 * Tests which makes sure that indeterminacy never creeps in to the codebase.
 * This is verified by running the {@link TemporalMemory} using the same 
 * configuration parameters, inputs and random number generator in the following 
 * 3 modes:</p><p>
 * <ol>
 *  <li>Straight through the TM algorithm class</li>
 *  <li>Using a Layer with synchronous calls</li>
 *  <li>Using the full NAPI and starting the Layer's thread</li>
 * </ol>
 * <p>
 * The result should be the same output regardless of input method
 * or algorithm configuration.
 * </p>
 * @author cogmission
 *
 */
public class AlgorithmDeterminacyTest {
    private static final int[][] TEST_AGGREGATION = new int[3][];
    
    private static final int TM_EXPL = 0;
    private static final int TM_LYR = 1;
    private static final int TM_NAPI = 2;
    
    public static Parameters getParameters() {
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
        parameters.set(KEY.RANDOM, new MersenneTwister(42));
        
        return parameters;
    }
    
    @AfterClass
    public static void doTest() {
        System.out.println(Arrays.toString(TEST_AGGREGATION[TM_EXPL]));
        System.out.println(Arrays.toString(TEST_AGGREGATION[TM_LYR]));
        System.out.println(Arrays.toString(TEST_AGGREGATION[TM_NAPI]));
        assertTrue(Arrays.equals(TEST_AGGREGATION[TM_EXPL], TEST_AGGREGATION[TM_LYR]));
        assertTrue(Arrays.equals(TEST_AGGREGATION[TM_EXPL], TEST_AGGREGATION[TM_NAPI]));
    }

    @Test
    public void testTemporalMemoryExplicit() {
        final int[] input1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] input2 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] input3 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
        final int[] input4 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input5 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input6 = new int[] { 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 };
        final int[] input7 = new int[] { 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0 };
        final int[][] inputs = { input1, input2, input3, input4, input5, input6, input7 };
        
        Parameters p = getParameters();
        Connections con = new Connections();
        p.apply(con);
        TemporalMemory tm = new TemporalMemory();
        TemporalMemory.init(con);
        
        ComputeCycle cc = null;
        for(int x = 0;x < 602;x++) {
            for(int[] i : inputs) {
                cc = tm.compute(con, ArrayUtils.where(i, ArrayUtils.WHERE_1), true);
            }
        }
        
        TEST_AGGREGATION[TM_EXPL] = SDR.asCellIndices(cc.activeCells);
    }
    
    /**
     * Temporary test to test basic sequence mechanisms
     */
    @Test
    public void testTemporalMemoryThroughLayer() {
        Parameters p = getParameters();

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
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference output) {}
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
        
        ComputeCycle cc = l.getInference().getComputeCycle();
        TEST_AGGREGATION[TM_LYR] = SDR.asCellIndices(cc.activeCells);
    }
    
    @Test
    public void testThreadedPublisher() {
        Publisher manual = Publisher.builder()
            .addHeader("dayOfWeek")
            .addHeader("darr")
            .addHeader("B").build();

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", manual}));
                    
        Parameters p = getParameters();
        
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
                        null, // map
                        20,    // n
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
            ComputeCycle cc = l.getInference().getComputeCycle();
            TEST_AGGREGATION[TM_NAPI] = SDR.asCellIndices(cc.activeCells);
        }catch(Exception e) {
            assertEquals(InterruptedException.class, e.getClass());
        }
    }
    
    @Test
    public void testModelClasses() {
        //Test Segment equality
        Column column1 = new Column(2, 0);
        Cell cell1 = new Cell(column1, 0);
        Segment s1 = new DistalDendrite(cell1, 0, 1, 0);
        assertTrue(s1.equals(s1)); // test ==
        assertFalse(s1.equals(null));
        
        Segment s2 = new DistalDendrite(cell1, 0, 1, 0);
        assertTrue(s1.equals(s2));
        
        Cell cell2 = new Cell(column1, 0);
        Segment s3 = new DistalDendrite(cell2, 0, 1, 0);
        assertTrue(s1.equals(s3));        
        
        //Segment's Cell has different index
        Cell cell3 = new Cell(column1, 1);
        Segment s4 = new DistalDendrite(cell3, 0, 1, 0);
        assertFalse(s1.equals(s4));
        
        //Segment has different index
        Segment s5 = new DistalDendrite(cell3, 1, 1, 0);
        assertFalse(s4.equals(s5));
        assertTrue(s5.toString().equals("1"));
        assertEquals(-1, s4.compareTo(s5));
        assertEquals(1, s5.compareTo(s4));
        
        //Different type of segment
        Segment s6 = new ProximalDendrite(0);
        assertFalse(s5.equals(s6));
        
        System.out.println(s4.compareTo(s5));
    }

}
