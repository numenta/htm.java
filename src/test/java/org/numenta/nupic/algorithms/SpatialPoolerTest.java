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
package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.SpatialPooler.InvalidSPParamValueException;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.Pool;
import org.numenta.nupic.util.AbstractSparseBinaryMatrix;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.SparseBinaryMatrix;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.UniversalRandom;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

public class SpatialPoolerTest {
    private Parameters parameters;
    private SpatialPooler sp;
    private Connections mem;
    
    public void setupParameters() {
        parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 5 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 5 });
        parameters.set(KEY.POTENTIAL_RADIUS, 5);
        parameters.set(KEY.POTENTIAL_PCT, 0.5);
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 3.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 0.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.set(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.1);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.1);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.set(KEY.MAX_BOOST, 10.0);
        parameters.setRandom(new UniversalRandom(42));
    }
    
    public void setupDefaultParameters() {
        parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 32, 32 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 64, 64 });
        parameters.set(KEY.POTENTIAL_RADIUS, 16);
        parameters.set(KEY.POTENTIAL_PCT, 0.5);
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 10.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 0.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.008);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.05);
        parameters.set(KEY.SYN_PERM_CONNECTED, 0.10);
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.001);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.001);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 1000);
        parameters.set(KEY.MAX_BOOST, 10.0);
        parameters.set(KEY.SEED, 42);
        parameters.setRandom(new UniversalRandom(42));
    }

    private void initSP() {
        sp = new SpatialPooler();
        mem = new Connections();
        parameters.apply(mem);
        sp.init(mem);
    }
    
    @Test
    public void confirmSPConstruction() {
        setupParameters();

        initSP();

        assertEquals(5, mem.getInputDimensions()[0]);
        assertEquals(5, mem.getColumnDimensions()[0]);
        assertEquals(5, mem.getPotentialRadius());
        assertEquals(0.5, mem.getPotentialPct(), 0);
        assertEquals(false, mem.getGlobalInhibition());
        assertEquals(-1.0, mem.getLocalAreaDensity(), 0);
        assertEquals(3, mem.getNumActiveColumnsPerInhArea(), 0);
        assertEquals(1, mem.getStimulusThreshold(), 1);
        assertEquals(0.01, mem.getSynPermInactiveDec(), 0);
        assertEquals(0.1, mem.getSynPermActiveInc(), 0);
        assertEquals(0.1, mem.getSynPermConnected(), 0);
        assertEquals(0.1, mem.getMinPctOverlapDutyCycles(), 0);
        assertEquals(0.1, mem.getMinPctActiveDutyCycles(), 0);
        assertEquals(10, mem.getDutyCyclePeriod(), 0);
        assertEquals(10.0, mem.getMaxBoost(), 0);
        assertEquals(42, mem.getSeed());
        
        assertEquals(5, mem.getNumInputs());
        assertEquals(5, mem.getNumColumns());
    }
    
    /**
     * Checks that feeding in the same input vector leads to polarized
     * permanence values: either zeros or ones, but no fractions
     */
    @Test
    public void testCompute1() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 9 });
        parameters.setColumnDimensions(new int[] { 5 });
        parameters.setPotentialRadius(5);

        //This is 0.3 in Python version due to use of dense 
        // permanence instead of sparse (as it should be)
        parameters.setPotentialPct(0.5); 

        parameters.setGlobalInhibition(false);
        parameters.setLocalAreaDensity(-1.0);
        parameters.setNumActiveColumnsPerInhArea(3);
        parameters.setStimulusThreshold(1);
        parameters.setSynPermInactiveDec(0.01);
        parameters.setSynPermActiveInc(0.1);
        parameters.setMinPctOverlapDutyCycles(0.1);
        parameters.setMinPctActiveDutyCycles(0.1);
        parameters.setDutyCyclePeriod(10);
        parameters.setMaxBoost(10);
        parameters.setSynPermTrimThreshold(0);

        //This is 0.5 in Python version due to use of dense 
        // permanence instead of sparse (as it should be)
        parameters.setPotentialPct(1);

        parameters.setSynPermConnected(0.1);

        initSP();

        SpatialPooler mock = new SpatialPooler() {
            private static final long serialVersionUID = 1L;

            public int[] inhibitColumns(Connections c, double[] overlaps) {
                return new int[] { 0, 1, 2, 3, 4 };
            }
        };

        int[] inputVector = new int[] { 1, 0, 1, 0, 1, 0, 0, 1, 1 };
        int[] activeArray = new int[] { 0, 0, 0, 0, 0 };
        for(int i = 0;i < 20;i++) {
            mock.compute(mem, inputVector, activeArray, true);
        }

        for(int i = 0;i < mem.getNumColumns();i++) {
            int[] permanences = ArrayUtils.toIntArray(mem.getPotentialPools().get(i).getDensePermanences(mem));
            assertTrue(Arrays.equals(inputVector, permanences));
        }
    }
    
    /**
     * Checks that columns only change the permanence values for 
     * inputs that are within their potential pool
     */
    @Test
    public void testCompute2() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 10 });
        parameters.setColumnDimensions(new int[] { 5 });
        parameters.setPotentialRadius(3);
        parameters.setPotentialPct(0.3); 
        parameters.setGlobalInhibition(false);
        parameters.setLocalAreaDensity(-1.0);
        parameters.setNumActiveColumnsPerInhArea(3);
        parameters.setStimulusThreshold(1);
        parameters.setSynPermInactiveDec(0.01);
        parameters.setSynPermActiveInc(0.1);
        parameters.setMinPctOverlapDutyCycles(0.1);
        parameters.setMinPctActiveDutyCycles(0.1);
        parameters.setDutyCyclePeriod(10);
        parameters.setMaxBoost(10);
        parameters.setSynPermConnected(0.1);

        initSP();

        SpatialPooler mock = new SpatialPooler() {
            private static final long serialVersionUID = 1L;

            public int[] inhibitColumns(Connections c, double[] overlaps) {
                return new int[] { 0, 1, 2, 3, 4 };
            }
        };

        int[] inputVector = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
        int[] activeArray = new int[] { 0, 0, 0, 0, 0 };
        for(int i = 0;i < 20;i++) {
            mock.compute(mem, inputVector, activeArray, true);
        }

        for(int i = 0;i < mem.getNumColumns();i++) {
            int[] permanences = ArrayUtils.toIntArray(mem.getPotentialPools().get(i).getDensePermanences(mem));
            int[] potential = (int[])mem.getConnectedCounts().getSlice(i);
            assertTrue(Arrays.equals(permanences, potential));
        }
    }
    
    /**
     * When stimulusThreshold is 0, allow columns without any overlap to become
     * active. This test focuses on the global inhibition code path.
     */
    @Test
    public void testZeroOverlap_NoStimulusThreshold_GlobalInhibition() {
        int inputSize = 10;
        int nColumns = 20;
        parameters = Parameters.getSpatialDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { inputSize });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { nColumns });
        parameters.set(KEY.POTENTIAL_RADIUS, 10);
        parameters.set(KEY.GLOBAL_INHIBITION, true);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 3.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 0.0);
        parameters.set(KEY.RANDOM, new UniversalRandom(42));
        parameters.set(KEY.SEED, 42);
        
        SpatialPooler sp = new SpatialPooler();
        Connections cn = new Connections();
        parameters.apply(cn);
        sp.init(cn);
        
        int[] activeArray = new int[nColumns];
        sp.compute(cn, new int[inputSize], activeArray, true);
        
        assertEquals(3, ArrayUtils.where(activeArray, ArrayUtils.INT_GREATER_THAN_0).length);
    }
    
    /**
     * When stimulusThreshold is > 0, don't allow columns without any overlap to
     * become active. This test focuses on the global inhibition code path.
     */
    @Test
    public void testZeroOverlap_StimulusThreshold_GlobalInhibition() {
        int inputSize = 10;
        int nColumns = 20;
        parameters = Parameters.getSpatialDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { inputSize });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { nColumns });
        parameters.set(KEY.POTENTIAL_RADIUS, 10);
        parameters.set(KEY.GLOBAL_INHIBITION, true);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 3.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.set(KEY.RANDOM, new UniversalRandom(42));
        parameters.set(KEY.SEED, 42);
        
        SpatialPooler sp = new SpatialPooler();
        Connections cn = new Connections();
        parameters.apply(cn);
        sp.init(cn);
        
        int[] activeArray = new int[nColumns];
        sp.compute(cn, new int[inputSize], activeArray, true);
        
        assertEquals(0, ArrayUtils.where(activeArray, ArrayUtils.INT_GREATER_THAN_0).length);
    }
    
    @Test
    public void testZeroOverlap_NoStimulusThreshold_LocalInhibition() {
        int inputSize = 10;
        int nColumns = 20;
        parameters = Parameters.getSpatialDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { inputSize });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { nColumns });
        parameters.set(KEY.POTENTIAL_RADIUS, 5);
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 1.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 0.0);
        parameters.set(KEY.RANDOM, new UniversalRandom(42));
        parameters.set(KEY.SEED, 42);
        
        SpatialPooler sp = new SpatialPooler();
        Connections cn = new Connections();
        parameters.apply(cn);
        sp.init(cn);
        
        // This exact number of active columns is determined by the inhibition
        // radius, which changes based on the random synapses (i.e. weird math).
        // Force it to a known number.
        cn.setInhibitionRadius(2);
        
        int[] activeArray = new int[nColumns];
        sp.compute(cn, new int[inputSize], activeArray, true);
        
        assertEquals(6, ArrayUtils.where(activeArray, ArrayUtils.INT_GREATER_THAN_0).length);
    }
    
    /**
     * When stimulusThreshold is > 0, don't allow columns without any overlap to
     * become active. This test focuses on the local inhibition code path.
     */
    @Test
    public void testZeroOverlap_StimulusThreshold_LocalInhibition() {
        int inputSize = 10;
        int nColumns = 20;
        parameters = Parameters.getSpatialDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { inputSize });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { nColumns });
        parameters.set(KEY.POTENTIAL_RADIUS, 10);
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 3.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.set(KEY.RANDOM, new UniversalRandom(42));
        parameters.set(KEY.SEED, 42);
        
        SpatialPooler sp = new SpatialPooler();
        Connections cn = new Connections();
        parameters.apply(cn);
        sp.init(cn);
        
        int[] activeArray = new int[nColumns];
        sp.compute(cn, new int[inputSize], activeArray, true);
        
        assertEquals(0, ArrayUtils.where(activeArray, ArrayUtils.INT_GREATER_THAN_0).length);
    }
    
    @Test
    public void testOverlapsOutput() {
        parameters = Parameters.getSpatialDefaultParameters();
        parameters.setColumnDimensions(new int[] { 3 });
        parameters.setInputDimensions(new int[] { 5 });
        parameters.setPotentialRadius(5);
        parameters.setNumActiveColumnsPerInhArea(5);
        parameters.setGlobalInhibition(true);
        parameters.setSynPermActiveInc(0.1);
        parameters.setSynPermInactiveDec(0.1);
        parameters.setSeed(42);
        parameters.setRandom(new UniversalRandom(42));
        
        SpatialPooler sp = new SpatialPooler();
        Connections cn = new Connections();
        parameters.apply(cn);
        sp.init(cn);
        
        cn.setBoostFactors(new double[] { 2.0, 2.0, 2.0 });
        int[] inputVector = { 1, 1, 1, 1, 1 };
        int[] activeArray = { 0, 0, 0 };
        int[] expOutput = { 2, 1, 0 };
        sp.compute(cn, inputVector, activeArray, true);
        
        double[] boostedOverlaps = cn.getBoostedOverlaps();
        int[] overlaps = cn.getOverlaps();
        
        for(int i = 0;i < cn.getNumColumns();i++) {
            assertEquals(expOutput[i], overlaps[i]);
            assertEquals(expOutput[i] * 2, boostedOverlaps[i], 0.01);
        }
    }
    
    /**
     * Given a specific input and initialization params the SP should return this
     * exact output.
     *
     * Previously output varied between platforms (OSX/Linux etc) == (in Python)
     */
    @Test
    public void testExactOutput() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 1, 188 });
        parameters.setColumnDimensions(new int[] { 2048, 1 });
        parameters.setPotentialRadius(94);
        parameters.setPotentialPct(0.5); 
        parameters.setGlobalInhibition(true);
        parameters.setLocalAreaDensity(-1.0);
        parameters.setNumActiveColumnsPerInhArea(40);
        parameters.setStimulusThreshold(0);
        parameters.setSynPermInactiveDec(0.01);
        parameters.setSynPermActiveInc(0.1);
        parameters.setMinPctOverlapDutyCycles(0.001);
        parameters.setMinPctActiveDutyCycles(0.001);
        parameters.setDutyCyclePeriod(1000);
        parameters.setMaxBoost(10);
        initSP();

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

        sp.compute(mem, inputVector, activeArray, true);

        int[] real = ArrayUtils.where(activeArray, new Condition.Adapter<Object>() {
            public boolean eval(int n) {
                return n > 0;
            }
        });

        int[] expected = new int[] {
             74, 203, 237, 270, 288, 317, 479, 529, 530, 622, 659, 720, 757, 790, 924, 956, 1033, 
             1041, 1112, 1332, 1386, 1430, 1500, 1517, 1578, 1584, 1651, 1664, 1717, 1735, 1747, 
             1748, 1775, 1779, 1788, 1813, 1888, 1911, 1938, 1958 };

        assertTrue(Arrays.equals(expected, real));
    }
    
    @Test
    public void testStripNeverLearned() {
        setupParameters();
        parameters.setColumnDimensions(new int[] { 6 });
        parameters.setInputDimensions(new int[] { 9 });
        initSP();

        mem.updateActiveDutyCycles(new double[] { 0.5, 0.1, 0, 0.2, 0.4, 0 });
        int[] activeColumns = new int[] { 0, 1, 2, 4 };
        int[] stripped = sp.stripUnlearnedColumns(mem, activeColumns);
        int[] trueStripped = new int[] { 0, 1, 4 };
        assertTrue(Arrays.equals(trueStripped, stripped));

        mem.updateActiveDutyCycles(new double[] { 0.9, 0, 0, 0, 0.4, 0.3 });
        activeColumns = ArrayUtils.range(0,  6);
        stripped = sp.stripUnlearnedColumns(mem, activeColumns);
        trueStripped = new int[] { 0, 4, 5 };
        assertTrue(Arrays.equals(trueStripped, stripped));

        mem.updateActiveDutyCycles(new double[] { 0, 0, 0, 0, 0, 0 });
        activeColumns = ArrayUtils.range(0,  6);
        stripped = sp.stripUnlearnedColumns(mem, activeColumns);
        trueStripped = new int[] {};
        assertTrue(Arrays.equals(trueStripped, stripped));

        mem.updateActiveDutyCycles(new double[] { 1, 1, 1, 1, 1, 1 });
        activeColumns = ArrayUtils.range(0,  6);
        stripped = sp.stripUnlearnedColumns(mem, activeColumns);
        trueStripped = ArrayUtils.range(0,  6);
        assertTrue(Arrays.equals(trueStripped, stripped));
    }
    
    @Test
    public void testMapColumn() {
        // Test 1D
        setupParameters();
        parameters.setColumnDimensions(new int[] { 4 });
        parameters.setInputDimensions(new int[] { 12 });
        initSP();

        assertEquals(1, sp.mapColumn(mem, 0));
        assertEquals(4, sp.mapColumn(mem, 1));
        assertEquals(7, sp.mapColumn(mem, 2));
        assertEquals(10, sp.mapColumn(mem, 3));

        // Test 1D with same dimension of columns and inputs
        setupParameters();
        parameters.setColumnDimensions(new int[] { 4 });
        parameters.setInputDimensions(new int[] { 4 });
        initSP();

        assertEquals(0, sp.mapColumn(mem, 0));
        assertEquals(1, sp.mapColumn(mem, 1));
        assertEquals(2, sp.mapColumn(mem, 2));
        assertEquals(3, sp.mapColumn(mem, 3));

        // Test 1D with dimensions of length 1
        setupParameters();
        parameters.setColumnDimensions(new int[] { 1 });
        parameters.setInputDimensions(new int[] { 1 });
        initSP();

        assertEquals(0, sp.mapColumn(mem, 0));

        // Test 2D
        setupParameters();
        parameters.setColumnDimensions(new int[] { 12, 4 });
        parameters.setInputDimensions(new int[] { 36, 12 });
        initSP();

        assertEquals(13, sp.mapColumn(mem, 0));
        assertEquals(49, sp.mapColumn(mem, 4));
        assertEquals(52, sp.mapColumn(mem, 5));
        assertEquals(58, sp.mapColumn(mem, 7));
        assertEquals(418, sp.mapColumn(mem, 47));
        
        // Test 2D with some input dimensions smaller than column dimensions.
        setupParameters();
        parameters.setColumnDimensions(new int[] { 4, 4 });
        parameters.setInputDimensions(new int[] { 3, 5 });
        initSP();
        
        assertEquals(0, sp.mapColumn(mem, 0));
        assertEquals(4, sp.mapColumn(mem, 3));
        assertEquals(14, sp.mapColumn(mem, 15));
    }
    
    @Test
    public void testMapPotential1D() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 12 });
        parameters.setColumnDimensions(new int[] { 4 });
        parameters.setPotentialRadius(2);
        parameters.setPotentialPct(1);
        parameters.set(KEY.WRAP_AROUND, false);
        initSP();

        assertEquals(12, mem.getInputDimensions()[0]);
        assertEquals(4, mem.getColumnDimensions()[0]);
        assertEquals(2, mem.getPotentialRadius());

        // Test without wrapAround and potentialPct = 1
        int[] expected = new int[] { 0, 1, 2, 3 };
        int[] mask = sp.mapPotential(mem, 0, false);
        assertTrue(Arrays.equals(expected, mask));

        expected = new int[] { 5, 6, 7, 8, 9 };
        mask = sp.mapPotential(mem, 2, false);
        assertTrue(Arrays.equals(expected, mask));

        // Test with wrapAround and potentialPct = 1
        mem.setWrapAround(true);
        expected = new int[] { 0, 1, 2, 3, 11 };
        mask = sp.mapPotential(mem, 0, true);
        assertTrue(Arrays.equals(expected, mask));

        expected = new int[] { 0, 8, 9, 10, 11 };
        mask = sp.mapPotential(mem, 3, true);
        assertTrue(Arrays.equals(expected, mask));

        // Test with wrapAround and potentialPct < 1
        parameters.setPotentialPct(0.5);
        parameters.set(KEY.WRAP_AROUND, true);
        initSP();

        int[] supersetMask = new int[] { 0, 1, 2, 3, 11 }; 
        mask = sp.mapPotential(mem, 0, true);
        assertEquals(mask.length, 3);
        TIntArrayList unionList = new TIntArrayList(supersetMask);
        unionList.addAll(mask);
        int[] unionMask = ArrayUtils.unique(unionList.toArray());
        assertTrue(Arrays.equals(unionMask, supersetMask));
    }
    
    @Test
    public void testMapPotential2D() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 6, 12 });
        parameters.setColumnDimensions(new int[] { 2, 4 });
        parameters.setPotentialRadius(1);
        parameters.setPotentialPct(1);
        initSP();

        //Test without wrapAround
        int[] mask = sp.mapPotential(mem, 0, false);
        TIntHashSet trueIndices = new TIntHashSet(new int[] { 0, 1, 2, 12, 13, 14, 24, 25, 26 });
        TIntHashSet maskSet = new TIntHashSet(mask);
        assertTrue(trueIndices.equals(maskSet));

        trueIndices.clear();
        maskSet.clear();
        trueIndices.addAll(new int[] { 6, 7, 8, 18, 19, 20, 30, 31, 32 });
        mask = sp.mapPotential(mem, 2, false);
        maskSet.addAll(mask);
        assertTrue(trueIndices.equals(maskSet));

        //Test with wrapAround
        trueIndices.clear();
        maskSet.clear();
        parameters.setPotentialRadius(2);
        initSP();
        trueIndices.addAll(
                new int[] { 0, 1, 2, 3, 11, 
                        12, 13, 14, 15, 23,
                        24, 25, 26, 27, 35, 
                        36, 37, 38, 39, 47, 
                        60, 61, 62, 63, 71 });
        mask = sp.mapPotential(mem, 0, true);
        maskSet.addAll(mask);
        assertTrue(trueIndices.equals(maskSet));

        trueIndices.clear();
        maskSet.clear();
        trueIndices.addAll(
                new int[] { 0, 8, 9, 10, 11, 
                        12, 20, 21, 22, 23, 
                        24, 32, 33, 34, 35, 
                        36, 44, 45, 46, 47, 
                        60, 68, 69, 70, 71 });
        mask = sp.mapPotential(mem, 3, true);
        maskSet.addAll(mask);
        assertTrue(trueIndices.equals(maskSet));
    }
    
    @Test
    public void testMapPotential1Column1Input() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 1 });
        parameters.setColumnDimensions(new int[] { 1 });
        parameters.setPotentialRadius(2);
        parameters.setPotentialPct(1);
        parameters.set(KEY.WRAP_AROUND, false);
        initSP();

        //Test without wrapAround and potentialPct = 1
        int[] expectedMask = new int[] { 0 }; 
        int[] mask = sp.mapPotential(mem, 0, false);
        TIntHashSet trueIndices = new TIntHashSet(expectedMask);
        TIntHashSet maskSet = new TIntHashSet(mask);
        // The *position* of the one "on" bit expected. 
        // Python version returns [1] which is the on bit in the zero'th position
        assertTrue(trueIndices.equals(maskSet));
    }
    
    //////////////////////////////////////////////////////////////
    /**
     * Local test apparatus for {@link #testInhibitColumns()}
     */
    boolean globalCalled = false;
    boolean localCalled = false;
    double _density = 0;
    public void reset() {
        this.globalCalled = false;
        this.localCalled = false;
        this._density = 0;
    }
    public void setGlobalCalled(boolean b) {
        this.globalCalled = b;
    }
    public void setLocalCalled(boolean b) {
        this.localCalled = b;
    }
    //////////////////////////////////////////////////////////////

    @Test
    public void testInhibitColumns() {
        setupParameters();
        parameters.setColumnDimensions(new int[] { 5 });
        parameters.setInhibitionRadius(10);
        initSP();

        //Mocks to test which method gets called
        SpatialPooler inhibitColumnsGlobal = new SpatialPooler() {
            private static final long serialVersionUID = 1L;

            @Override public int[] inhibitColumnsGlobal(Connections c, double[] overlap, double density) {
                setGlobalCalled(true);
                _density = density;
                return new int[] { 1 };
            }
        };
        SpatialPooler inhibitColumnsLocal = new SpatialPooler() {
            private static final long serialVersionUID = 1L;
            @Override public int[] inhibitColumnsLocal(Connections c, double[] overlap, double density) {
                setLocalCalled(true);
                _density = density;
                return new int[] { 2 };
            }
        };

        double[] overlaps = ArrayUtils.sample(mem.getNumColumns(), mem.getRandom());
        mem.setNumActiveColumnsPerInhArea(5);
        mem.setLocalAreaDensity(0.1);
        mem.setGlobalInhibition(true);
        mem.setInhibitionRadius(5);
        double trueDensity = mem.getLocalAreaDensity();
        inhibitColumnsGlobal.inhibitColumns(mem, overlaps);
        assertTrue(globalCalled);
        assertTrue(!localCalled);
        assertEquals(trueDensity, _density, .01d);

        //////
        reset();
        mem.setColumnDimensions(new int[] { 50, 10 });
        //Internally calculated during init, to overwrite we put after init
        mem.setGlobalInhibition(false);
        mem.setInhibitionRadius(7);

        double[] tieBreaker = new double[500];
        Arrays.fill(tieBreaker, 0);
        mem.setTieBreaker(tieBreaker);
        overlaps = ArrayUtils.sample(mem.getNumColumns(), mem.getRandom());
        inhibitColumnsLocal.inhibitColumns(mem, overlaps);
        trueDensity = mem.getLocalAreaDensity();
        assertTrue(!globalCalled);
        assertTrue(localCalled);
        assertEquals(trueDensity, _density, .01d);

        //////
        reset();
        parameters.setInputDimensions(new int[] { 100, 10 });
        parameters.setColumnDimensions(new int[] { 100, 10 });
        parameters.setGlobalInhibition(false);
        parameters.setLocalAreaDensity(-1);
        parameters.setNumActiveColumnsPerInhArea(3);
        initSP();

        //Internally calculated during init, to overwrite we put after init
        mem.setInhibitionRadius(4);
        tieBreaker = new double[1000];
        Arrays.fill(tieBreaker, 0);
        mem.setTieBreaker(tieBreaker);
        overlaps = ArrayUtils.sample(mem.getNumColumns(), mem.getRandom());
        inhibitColumnsLocal.inhibitColumns(mem, overlaps);
        trueDensity = 3.0 / 81.0;
        assertTrue(!globalCalled);
        assertTrue(localCalled);
        assertEquals(trueDensity, _density, .01d);

        //////
        reset();
        mem.setNumActiveColumnsPerInhArea(7);

        //Internally calculated during init, to overwrite we put after init
        mem.setInhibitionRadius(1);
        tieBreaker = new double[1000];
        Arrays.fill(tieBreaker, 0);
        mem.setTieBreaker(tieBreaker);
        overlaps = ArrayUtils.sample(mem.getNumColumns(), mem.getRandom());
        inhibitColumnsLocal.inhibitColumns(mem, overlaps);
        trueDensity = 0.5;
        assertTrue(!globalCalled);
        assertTrue(localCalled);
        assertEquals(trueDensity, _density, .01d);

    }
    
    @Test
    public void testUpdateBoostFactors() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 5/*Don't care*/ });
        parameters.setColumnDimensions(new int[] { 5 });
        parameters.setMaxBoost(10.0);
        parameters.setRandom(new UniversalRandom(42));
        initSP();
        
        mem.setNumColumns(6);

        double[] minActiveDutyCycles = new double[6];
        Arrays.fill(minActiveDutyCycles, 0.000001D);
        mem.setMinActiveDutyCycles(minActiveDutyCycles);

        double[] activeDutyCycles = new double[] { 0.1, 0.3, 0.02, 0.04, 0.7, 0.12 };
        mem.setActiveDutyCycles(activeDutyCycles);

        double[] trueBoostFactors = new double[] { 1, 1, 1, 1, 1, 1 };
        sp.updateBoostFactors(mem);
        double[] boostFactors = mem.getBoostFactors();
        for(int i = 0;i < boostFactors.length;i++) {
            assertEquals(trueBoostFactors[i], boostFactors[i], 0.1D);
        }

        ////////////////
        minActiveDutyCycles = new double[] { 0.1, 0.3, 0.02, 0.04, 0.7, 0.12 };
        mem.setMinActiveDutyCycles(minActiveDutyCycles);
        Arrays.fill(mem.getBoostFactors(), 0);
        sp.updateBoostFactors(mem);
        boostFactors = mem.getBoostFactors();
        for(int i = 0;i < boostFactors.length;i++) {
            assertEquals(trueBoostFactors[i], boostFactors[i], 0.1D);
        }

        ////////////////
        minActiveDutyCycles = new double[] { 0.1, 0.2, 0.02, 0.03, 0.7, 0.12 };
        mem.setMinActiveDutyCycles(minActiveDutyCycles);
        activeDutyCycles = new double[] { 0.01, 0.02, 0.002, 0.003, 0.07, 0.012 };
        mem.setActiveDutyCycles(activeDutyCycles);
        trueBoostFactors = new double[] { 9.1, 9.1, 9.1, 9.1, 9.1, 9.1 };
        sp.updateBoostFactors(mem);
        boostFactors = mem.getBoostFactors();
        for(int i = 0;i < boostFactors.length;i++) {
            assertEquals(trueBoostFactors[i], boostFactors[i], 0.1D);
        }

        ////////////////
        minActiveDutyCycles = new double[] { 0.1, 0.2, 0.02, 0.03, 0.7, 0.12 };
        mem.setMinActiveDutyCycles(minActiveDutyCycles);
        Arrays.fill(activeDutyCycles, 0);
        mem.setActiveDutyCycles(activeDutyCycles);
        Arrays.fill(trueBoostFactors, 10.0);
        sp.updateBoostFactors(mem);
        boostFactors = mem.getBoostFactors();
        for(int i = 0;i < boostFactors.length;i++) {
            assertEquals(trueBoostFactors[i], boostFactors[i], 0.1D);
        }
    }
    
    @Test
    public void testUpdateInhibitionRadius() {
        setupParameters();
        initSP();

        //Test global inhibition case
        mem.setGlobalInhibition(true);
        mem.setColumnDimensions(new int[] { 57, 31, 2 });
        sp.updateInhibitionRadius(mem);
        assertEquals(57, mem.getInhibitionRadius());

        ////////////
        
        // ((3 * 4) - 1) / 2 => round up
        SpatialPooler mock = new SpatialPooler() {
            private static final long serialVersionUID = 1L;
            public double avgConnectedSpanForColumnND(Connections c, int columnIndex) {
                return 3;
            }

            public double avgColumnsPerInput(Connections c) {
                return 4;
            }
        };
        mem.setGlobalInhibition(false);
        sp = mock;
        sp.updateInhibitionRadius(mem);
        assertEquals(6, mem.getInhibitionRadius());
        
        //////////////

        //Test clipping at 1.0
        mock = new SpatialPooler() {
            private static final long serialVersionUID = 1L;
            public double avgConnectedSpanForColumnND(Connections c, int columnIndex) {
                return 0.5;
            }

            public double avgColumnsPerInput(Connections c) {
                return 1.2;
            }
        };
        mem.setGlobalInhibition(false);
        sp = mock;
        sp.updateInhibitionRadius(mem);
        assertEquals(1, mem.getInhibitionRadius());
        
        /////////////

        //Test rounding up
        mock = new SpatialPooler() {
            private static final long serialVersionUID = 1L;
            public double avgConnectedSpanForColumnND(Connections c, int columnIndex) {
                return 2.4;
            }

            public double avgColumnsPerInput(Connections c) {
                return 2;
            }
        };
        mem.setGlobalInhibition(false);
        sp = mock;
        //((2 * 2.4) - 1) / 2.0 => round up
        sp.updateInhibitionRadius(mem);
        assertEquals(2, mem.getInhibitionRadius());
    }
    
    @Test
    public void testAvgColumnsPerInput() {
        setupParameters();
        initSP();

        mem.setColumnDimensions(new int[] { 2, 2, 2, 2 });
        mem.setInputDimensions(new int[] { 4, 4, 4, 4 });
        assertEquals(0.5, sp.avgColumnsPerInput(mem), 0);

        mem.setColumnDimensions(new int[] { 2, 2, 2, 2 });
        mem.setInputDimensions(new int[] { 7, 5, 1, 3 });
        double trueAvgColumnPerInput = (2.0/7 + 2.0/5 + 2.0/1 + 2/3.0) / 4.0d;
        assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(mem), 0);

        mem.setColumnDimensions(new int[] { 3, 3 });
        mem.setInputDimensions(new int[] { 3, 3 });
        trueAvgColumnPerInput = 1;
        assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(mem), 0);

        mem.setColumnDimensions(new int[] { 25 });
        mem.setInputDimensions(new int[] { 5 });
        trueAvgColumnPerInput = 5;
        assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(mem), 0);

        mem.setColumnDimensions(new int[] { 3, 3, 3, 5, 5, 6, 6 });
        mem.setInputDimensions(new int[] { 3, 3, 3, 5, 5, 6, 6 });
        trueAvgColumnPerInput = 1;
        assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(mem), 0);

        mem.setColumnDimensions(new int[] { 3, 6, 9, 12 });
        mem.setInputDimensions(new int[] { 3, 3, 3 , 3 });
        trueAvgColumnPerInput = 2.5;
        assertEquals(trueAvgColumnPerInput, sp.avgColumnsPerInput(mem), 0);
    }
    
    @Test
    public void testAvgConnectedSpanForColumnND() {
        sp = new SpatialPooler();
        mem = new Connections();

        int[] inputDimensions = new int[] { 4, 4, 2, 5 };
        mem.setInputDimensions(inputDimensions);
        mem.setColumnDimensions(new int[] { 5 });
        sp.initMatrices(mem);

        TIntArrayList connected = new TIntArrayList();
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 1, 0, 1, 0 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 1, 0, 1, 1 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 2, 1, 0 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 0, 1, 0 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 1, 0, 1, 3 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 2, 2, 1, 0 }, false));
        connected.sort(0, connected.size());
        //[ 45  46  48 105 125 145]
        //mem.getConnectedSynapses().set(0, connected.toArray());
        mem.getPotentialPools().set(0, new Pool(6));
        mem.getColumn(0).setProximalConnectedSynapsesForTest(mem, connected.toArray());

        connected.clear();
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 2, 0, 1, 0 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 2, 0, 0, 0 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 0, 0, 0 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 0, 1, 0 }, false));
        connected.sort(0, connected.size());
        //[ 80  85 120 125]
        //mem.getConnectedSynapses().set(1, connected.toArray());
        mem.getPotentialPools().set(1, new Pool(4));
        mem.getColumn(1).setProximalConnectedSynapsesForTest(mem, connected.toArray());

        connected.clear();
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 1, 4 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 0, 3 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 0, 1 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 1, 0, 0, 2 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 1, 1 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 3, 1, 1 }, false));
        connected.sort(0, connected.size());
        //[  1   3   6   9  42 156]
        //mem.getConnectedSynapses().set(2, connected.toArray());
        mem.getPotentialPools().set(2, new Pool(4));
        mem.getColumn(2).setProximalConnectedSynapsesForTest(mem, connected.toArray());

        connected.clear();
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 3, 1, 4 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 0, 0 }, false));
        connected.sort(0, connected.size());
        //[  0 159]
        //mem.getConnectedSynapses().set(3, connected.toArray());
        mem.getPotentialPools().set(3, new Pool(4));
        mem.getColumn(3).setProximalConnectedSynapsesForTest(mem, connected.toArray());

        //[]
        connected.clear();
        mem.getPotentialPools().set(4, new Pool(4));
        mem.getColumn(4).setProximalConnectedSynapsesForTest(mem, connected.toArray());

        double[] trueAvgConnectedSpan = new double[] { 11.0/4d, 6.0/4d, 14.0/4d, 15.0/4d, 0d };
        for(int i = 0;i < mem.getNumColumns();i++) {
            double connectedSpan = sp.avgConnectedSpanForColumnND(mem, i);
            assertEquals(trueAvgConnectedSpan[i], connectedSpan, 0);
        }
    }
    
    @Test
    public void testBumpUpWeakColumns() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 8 });
        parameters.setColumnDimensions(new int[] { 5 });
        initSP();

        mem.setSynPermBelowStimulusInc(0.01);
        mem.setSynPermTrimThreshold(0.05);
        mem.setOverlapDutyCycles(new double[] { 0, 0.009, 0.1, 0.001, 0.002 });
        mem.setMinOverlapDutyCycles(new double[] { .01, .01, .01, .01, .01 });

        int[][] potentialPools = new int[][] {
            { 1, 1, 1, 1, 0, 0, 0, 0 },
            { 1, 0, 0, 0, 1, 1, 0, 1 },
            { 0, 0, 1, 0, 1, 1, 1, 0 },
            { 1, 1, 1, 0, 0, 0, 1, 0 },
            { 1, 1, 1, 1, 1, 1, 1, 1 }
        };

        double[][] permanences = new double[][] {
            { 0.200, 0.120, 0.090, 0.040, 0.000, 0.000, 0.000, 0.000 },
            { 0.150, 0.000, 0.000, 0.000, 0.180, 0.120, 0.000, 0.450 },
            { 0.000, 0.000, 0.014, 0.000, 0.032, 0.044, 0.110, 0.000 },
            { 0.041, 0.000, 0.000, 0.000, 0.000, 0.000, 0.178, 0.000 },
            { 0.100, 0.738, 0.045, 0.002, 0.050, 0.008, 0.208, 0.034 }  
        };

        double[][] truePermanences = new double[][] {
            { 0.210, 0.130, 0.100, 0.000, 0.000, 0.000, 0.000, 0.000 },
            { 0.160, 0.000, 0.000, 0.000, 0.190, 0.130, 0.000, 0.460 },
            { 0.000, 0.000, 0.014, 0.000, 0.032, 0.044, 0.110, 0.000 },
            { 0.051, 0.000, 0.000, 0.000, 0.000, 0.000, 0.188, 0.000 },
            { 0.110, 0.748, 0.055, 0.000, 0.060, 0.000, 0.218, 0.000 }  
        };

        Condition<?> cond = new Condition.Adapter<Integer>() {
            public boolean eval(int n) {
                return n == 1;
            }
        };
        for(int i = 0;i < mem.getNumColumns();i++) {
            int[] indexes = ArrayUtils.where(potentialPools[i], cond);
            mem.getColumn(i).setProximalConnectedSynapsesForTest(mem, indexes);
            mem.getColumn(i).setProximalPermanences(mem, permanences[i]);
        }

        //Execute method being tested
        sp.bumpUpWeakColumns(mem);

        for(int i = 0;i < mem.getNumColumns();i++) {
            double[] perms = mem.getPotentialPools().get(i).getDensePermanences(mem);
            for(int j = 0;j < truePermanences[i].length;j++) {
                assertEquals(truePermanences[i][j], perms[j], 0.01);
            }
        }
    }
    
    @Test
    public void testUpdateMinDutyCycleLocal() {
        setupDefaultParameters();
        parameters.setInputDimensions(new int[] { 5 });
        parameters.setColumnDimensions(new int[] { 8 });
        parameters.set(KEY.WRAP_AROUND, false);
        initSP();
        
        mem.setInhibitionRadius(1);
        mem.setOverlapDutyCycles(new double[] { 0.7, 0.1, 0.5, 0.01, 0.78, 0.55, 0.1, 0.001 });
        mem.setActiveDutyCycles(new double[] { 0.9, 0.3, 0.5, 0.7, 0.1, 0.01, 0.08, 0.12 });
        mem.setMinPctActiveDutyCycles(0.1);
        mem.setMinPctOverlapDutyCycles(0.2);
        sp.updateMinDutyCyclesLocal(mem);
        
        double[] resultMinActiveDutyCycles = mem.getMinActiveDutyCycles();
        double[] expected0 = { 0.09, 0.09, 0.07, 0.07, 0.07, 0.01, 0.012, 0.012 };
        IntStream.range(0, expected0.length)
            .forEach(i -> assertEquals(expected0[i], resultMinActiveDutyCycles[i], 0.01));
        
        double[] resultMinOverlapDutyCycles = mem.getMinOverlapDutyCycles();
        double[] expected1 = new double[] { 0.14, 0.14, 0.1, 0.156, 0.156, 0.156, 0.11, 0.02 };
        IntStream.range(0, expected1.length)
            .forEach(i -> assertEquals(expected1[i], resultMinOverlapDutyCycles[i], 0.01));
        
        // wrapAround = true
        setupDefaultParameters();
        parameters.setInputDimensions(new int[] { 5 });
        parameters.setColumnDimensions(new int[] { 8 });
        parameters.set(KEY.WRAP_AROUND, true);
        initSP();
        
        mem.setInhibitionRadius(1);
        mem.setOverlapDutyCycles(new double[] { 0.7, 0.1, 0.5, 0.01, 0.78, 0.55, 0.1, 0.001 });
        mem.setActiveDutyCycles(new double[] { 0.9, 0.3, 0.5, 0.7, 0.1, 0.01, 0.08, 0.12 });
        mem.setMinPctActiveDutyCycles(0.1);
        mem.setMinPctOverlapDutyCycles(0.2);
        sp.updateMinDutyCyclesLocal(mem);
        
        double[] resultMinActiveDutyCycles2 = mem.getMinActiveDutyCycles();
        double[] expected2 = { 0.09, 0.09, 0.07, 0.07, 0.07, 0.01, 0.012, 0.09 };
        IntStream.range(0, expected2.length)
            .forEach(i -> assertEquals(expected2[i], resultMinActiveDutyCycles2[i], 0.01));
        
        double[] resultMinOverlapDutyCycles2 = mem.getMinOverlapDutyCycles();
        double[] expected3 = new double[] { 0.14, 0.14, 0.1, 0.156, 0.156, 0.156, 0.11, 0.14 };
        IntStream.range(0, expected3.length)
            .forEach(i -> assertEquals(expected3[i], resultMinOverlapDutyCycles2[i], 0.01));
        
    }
    
    @Test
    public void testUpdateMinDutyCycleGlobal() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 5 });
        parameters.setColumnDimensions(new int[] { 5 });
        initSP();

        mem.setMinPctOverlapDutyCycles(0.01);
        mem.setMinPctActiveDutyCycles(0.02);
        mem.setOverlapDutyCycles(new double[] { 0.06, 1, 3, 6, 0.5 });
        mem.setActiveDutyCycles(new double[] { 0.6, 0.07, 0.5, 0.4, 0.3 });

        sp.updateMinDutyCyclesGlobal(mem);
        double[] trueMinActiveDutyCycles = new double[mem.getNumColumns()];
        Arrays.fill(trueMinActiveDutyCycles, 0.02*0.6);
        double[] trueMinOverlapDutyCycles = new double[mem.getNumColumns()];
        Arrays.fill(trueMinOverlapDutyCycles, 0.01*6);
        for(int i = 0;i < mem.getNumColumns();i++) {
            //          System.out.println(i + ") " + trueMinOverlapDutyCycles[i] + "  -  " +  mem.getMinOverlapDutyCycles()[i]);
            //          System.out.println(i + ") " + trueMinActiveDutyCycles[i] + "  -  " +  mem.getMinActiveDutyCycles()[i]);
            assertEquals(trueMinOverlapDutyCycles[i], mem.getMinOverlapDutyCycles()[i], 0.01);
            assertEquals(trueMinActiveDutyCycles[i], mem.getMinActiveDutyCycles()[i], 0.01);
        }
        
        mem.setMinPctOverlapDutyCycles(0.015);
        mem.setMinPctActiveDutyCycles(0.03);
        mem.setOverlapDutyCycles(new double[] { 0.86, 2.4, 0.03, 1.6, 1.5 });
        mem.setActiveDutyCycles(new double[] { 0.16, 0.007, 0.15, 0.54, 0.13 });
        sp.updateMinDutyCyclesGlobal(mem);
        Arrays.fill(trueMinOverlapDutyCycles, 0.015*2.4);
        for(int i = 0;i < mem.getNumColumns();i++) {
            //          System.out.println(i + ") " + trueMinOverlapDutyCycles[i] + "  -  " +  mem.getMinOverlapDutyCycles()[i]);
            //          System.out.println(i + ") " + trueMinActiveDutyCycles[i] + "  -  " +  mem.getMinActiveDutyCycles()[i]);
            assertEquals(trueMinOverlapDutyCycles[i], mem.getMinOverlapDutyCycles()[i], 0.01);
        }
        
        mem.setMinPctOverlapDutyCycles(0.015);
        mem.setMinPctActiveDutyCycles(0.03);
        mem.setOverlapDutyCycles(new double[5]);
        mem.setActiveDutyCycles(new double[5]);
        sp.updateMinDutyCyclesGlobal(mem);
        Arrays.fill(trueMinOverlapDutyCycles, 0);
        Arrays.fill(trueMinActiveDutyCycles, 0);
        for(int i = 0;i < mem.getNumColumns();i++) {
            //          System.out.println(i + ") " + trueMinOverlapDutyCycles[i] + "  -  " +  mem.getMinOverlapDutyCycles()[i]);
            //          System.out.println(i + ") " + trueMinActiveDutyCycles[i] + "  -  " +  mem.getMinActiveDutyCycles()[i]);
            assertEquals(trueMinActiveDutyCycles[i], mem.getMinActiveDutyCycles()[i], 0.01);
            assertEquals(trueMinOverlapDutyCycles[i], mem.getMinOverlapDutyCycles()[i], 0.01);
        }
    }
    
    @Test
    public void testIsUpdateRound() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 5 });
        parameters.setColumnDimensions(new int[] { 5 });
        initSP();

        mem.setUpdatePeriod(50);
        mem.setIterationNum(1);
        assertFalse(sp.isUpdateRound(mem));
        mem.setIterationNum(39);
        assertFalse(sp.isUpdateRound(mem));
        mem.setIterationNum(50);
        assertTrue(sp.isUpdateRound(mem));
        mem.setIterationNum(1009);
        assertFalse(sp.isUpdateRound(mem));
        mem.setIterationNum(1250);
        assertTrue(sp.isUpdateRound(mem));

        mem.setUpdatePeriod(125);
        mem.setIterationNum(0);
        assertTrue(sp.isUpdateRound(mem));
        mem.setIterationNum(200);
        assertFalse(sp.isUpdateRound(mem));
        mem.setIterationNum(249);
        assertFalse(sp.isUpdateRound(mem));
        mem.setIterationNum(1330);
        assertFalse(sp.isUpdateRound(mem));
        mem.setIterationNum(1249);
        assertFalse(sp.isUpdateRound(mem));
        mem.setIterationNum(1375);
        assertTrue(sp.isUpdateRound(mem));

    }
    
    @Test
    public void testAdaptSynapses() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 8 });
        parameters.setColumnDimensions(new int[] { 4 });
        parameters.setSynPermInactiveDec(0.01);
        parameters.setSynPermActiveInc(0.1);
        initSP();

        mem.setSynPermTrimThreshold(0.05);

        int[][] potentialPools = new int[][] {
            { 1, 1, 1, 1, 0, 0, 0, 0 },
            { 1, 0, 0, 0, 1, 1, 0, 1 },
            { 0, 0, 1, 0, 0, 0, 1, 0 },
            { 1, 0, 0, 0, 0, 0, 1, 0 }
        };

        double[][] permanences = new double[][] {
            { 0.200, 0.120, 0.090, 0.040, 0.000, 0.000, 0.000, 0.000 },
            { 0.150, 0.000, 0.000, 0.000, 0.180, 0.120, 0.000, 0.450 },
            { 0.000, 0.000, 0.014, 0.000, 0.000, 0.000, 0.110, 0.000 },
            { 0.040, 0.000, 0.000, 0.000, 0.000, 0.000, 0.178, 0.000 }
        };

        double[][] truePermanences = new double[][] {
            { 0.300, 0.110, 0.080, 0.140, 0.000, 0.000, 0.000, 0.000 },
        //     Inc     Dec    Dec    Inc    -      -      -      -
            { 0.250, 0.000, 0.000, 0.000, 0.280, 0.110, 0.000, 0.440 },
        //     Inc     -      -      -      Inc    Dec    -      Dec
            { 0.000, 0.000, 0.000, 0.000, 0.000, 0.000, 0.210, 0.000 },
        //      -      -     Trim    -      -      -      Inc    -
            { 0.040, 0.000, 0.000, 0.000, 0.000, 0.000, 0.178, 0.000 }
        //      -      -      -      -      -      -      -      -     // Only cols 0,1,2 are active 
                                                                       // (see 'activeColumns' below)
        };

        Condition<?> cond = new Condition.Adapter<Integer>() {
            public boolean eval(int n) {
                return n == 1;
            }
        };
        for(int i = 0;i < mem.getNumColumns();i++) {
            int[] indexes = ArrayUtils.where(potentialPools[i], cond);
            mem.getColumn(i).setProximalConnectedSynapsesForTest(mem, indexes);
            mem.getColumn(i).setProximalPermanences(mem, permanences[i]);
        }

        int[] inputVector = new int[] { 1, 0, 0, 1, 1, 0, 1, 0 };
        int[] activeColumns = new int[] { 0, 1, 2 };

        sp.adaptSynapses(mem, inputVector, activeColumns);

        for(int i = 0;i < mem.getNumColumns();i++) {
            double[] perms = mem.getPotentialPools().get(i).getDensePermanences(mem);
            for(int j = 0;j < truePermanences[i].length;j++) {
                assertEquals(truePermanences[i][j], perms[j], 0.01);
            }
        }

        //////////////////////////////

        potentialPools = new int[][] {
            { 1, 1, 1, 0, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 0, 0, 0, 0 },
            { 0, 0, 1, 1, 1, 0, 0, 0 },
            { 1, 0, 0, 0, 0, 0, 1, 0 }
        };

        permanences = new double[][] {
            { 0.200, 0.120, 0.090, 0.000, 0.000, 0.000, 0.000, 0.000 },
            { 0.000, 0.017, 0.232, 0.400, 0.180, 0.120, 0.000, 0.450 },
            { 0.000, 0.000, 0.014, 0.051, 0.730, 0.000, 0.000, 0.000 },
            { 0.170, 0.000, 0.000, 0.000, 0.000, 0.000, 0.380, 0.000 }
        };

        truePermanences = new double[][] {
            { 0.300, 0.110, 0.080, 0.000, 0.000, 0.000, 0.000, 0.000 },
            { 0.000, 0.000, 0.222, 0.500, 0.000, 0.000, 0.000, 0.000 },
            { 0.000, 0.000, 0.000, 0.151, 0.830, 0.000, 0.000, 0.000 },
            { 0.170, 0.000, 0.000, 0.000, 0.000, 0.000, 0.380, 0.000 }
        };

        for(int i = 0;i < mem.getNumColumns();i++) {
            int[] indexes = ArrayUtils.where(potentialPools[i], cond);
            mem.getColumn(i).setProximalConnectedSynapsesForTest(mem, indexes);
            mem.getColumn(i).setProximalPermanences(mem, permanences[i]);
        }

        sp.adaptSynapses(mem, inputVector, activeColumns);

        for(int i = 0;i < mem.getNumColumns();i++) {
            double[] perms = mem.getPotentialPools().get(i).getDensePermanences(mem);
            for(int j = 0;j < truePermanences[i].length;j++) {
                assertEquals(truePermanences[i][j], perms[j], 0.01);
            }
        }
    }
    
    @Test
    public void testRaisePermanenceThreshold() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 5 });
        parameters.setColumnDimensions(new int[] { 5 });
        parameters.setSynPermConnected(0.1);
        parameters.setStimulusThreshold(3);
        parameters.setSynPermBelowStimulusInc(0.01);
        //The following parameter is not set to "1" in the Python version
        //This is necessary to reproduce the test conditions of having as
        //many pool members as Input Bits, which would never happen under
        //normal circumstances because we want to enforce sparsity
        parameters.setPotentialPct(1);

        initSP();

        //We set the values on the Connections permanences here just for illustration
        SparseObjectMatrix<double[]> objMatrix = new SparseObjectMatrix<double[]>(new int[] { 5, 5 });
        objMatrix.set(0, new double[] { 0.0, 0.11, 0.095, 0.092, 0.01 });
        objMatrix.set(1, new double[] { 0.12, 0.15, 0.02, 0.12, 0.09 });
        objMatrix.set(2, new double[] { 0.51, 0.081, 0.025, 0.089, 0.31 });
        objMatrix.set(3, new double[] { 0.18, 0.0601, 0.11, 0.011, 0.03 });
        objMatrix.set(4, new double[] { 0.011, 0.011, 0.011, 0.011, 0.011 });
        mem.setProximalPermanences(objMatrix);

        //      mem.setConnectedSynapses(new SparseObjectMatrix<int[]>(new int[] { 5, 5 }));
        //      SparseObjectMatrix<int[]> syns = mem.getConnectedSynapses();
        //      syns.set(0, new int[] { 0, 1, 0, 0, 0 });
        //      syns.set(1, new int[] { 1, 1, 0, 1, 0 });
        //      syns.set(2, new int[] { 1, 0, 0, 0, 1 });
        //      syns.set(3, new int[] { 1, 0, 1, 0, 0 });
        //      syns.set(4, new int[] { 0, 0, 0, 0, 0 });

        mem.setConnectedCounts(new int[] { 1, 3, 2, 2, 0 });

        double[][] truePermanences = new double[][] { 
            {0.01, 0.12, 0.105, 0.102, 0.02},       // incremented once
            {0.12, 0.15, 0.02, 0.12, 0.09},         // no change
            {0.53, 0.101, 0.045, 0.109, 0.33},      // increment twice
            {0.22, 0.1001, 0.15, 0.051, 0.07},      // increment four times
            {0.101, 0.101, 0.101, 0.101, 0.101}};   // increment 9 times

            //FORGOT TO SET PERMANENCES ABOVE - DON'T USE mem.setPermanences() 
            int[] indices = mem.getMemory().getSparseIndices();
            for(int i = 0;i < mem.getNumColumns();i++) {
                double[] perm = mem.getPotentialPools().get(i).getSparsePermanences();
                sp.raisePermanenceToThreshold(mem, perm, indices);

                for(int j = 0;j < perm.length;j++) {
                    assertEquals(truePermanences[i][j], perm[j], 0.001);
                }
            }
    }
    
    @Test
    public void testUpdatePermanencesForColumn() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 5 });
        parameters.setColumnDimensions(new int[] { 5 });
        parameters.setSynPermTrimThreshold(0.05);
        //The following parameter is not set to "1" in the Python version
        //This is necessary to reproduce the test conditions of having as
        //many pool members as Input Bits, which would never happen under
        //normal circumstances because we want to enforce sparsity
        parameters.setPotentialPct(1);
        initSP();

        double[][] permanences = new double[][] {
            {-0.10, 0.500, 0.400, 0.010, 0.020},
            {0.300, 0.010, 0.020, 0.120, 0.090},
            {0.070, 0.050, 1.030, 0.190, 0.060},
            {0.180, 0.090, 0.110, 0.010, 0.030},
            {0.200, 0.101, 0.050, -0.09, 1.100}};

        int[][] trueConnectedSynapses = new int[][] {
            {0, 1, 1, 0, 0},
            {1, 0, 0, 1, 0},
            {0, 0, 1, 1, 0},
            {1, 0, 1, 0, 0},
            {1, 1, 0, 0, 1}};

        int[][] connectedDense = new int[][] {
            { 1, 2 },
            { 0, 3 },
            { 2, 3 },
            { 0, 2 },
            { 0, 1, 4 }
        };

        int[] trueConnectedCounts = new int[] {2, 2, 2, 2, 3};

        for(int i = 0;i < mem.getNumColumns();i++) {
            mem.getColumn(i).setProximalPermanences(mem, permanences[i]);
            sp.updatePermanencesForColumn(mem, permanences[i], mem.getColumn(i), connectedDense[i], true);
            int[] dense = mem.getColumn(i).getProximalDendrite().getConnectedSynapsesDense(mem);
            assertEquals(Arrays.toString(trueConnectedSynapses[i]), Arrays.toString(dense));
        }

        assertEquals(Arrays.toString(trueConnectedCounts), Arrays.toString(mem.getConnectedCounts().getTrueCounts()));
    }
    
    @Test
    public void testCalculateOverlap() {
        setupDefaultParameters();
        parameters.setInputDimensions(new int[] { 10 });
        parameters.setColumnDimensions(new int[] { 5 });
        initSP();

        int[] dimensions = new int[] { 5, 10 };
        int[][] connectedSynapses = new int[][] {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 1}};
        AbstractSparseBinaryMatrix sm = new SparseBinaryMatrix(dimensions);
        for(int i = 0;i < sm.getDimensions()[0];i++) {
            for(int j = 0;j < sm.getDimensions()[1];j++) {
                sm.set(connectedSynapses[i][j], i, j);
            }
        }

        mem.setConnectedMatrix(sm);
        
        for(int i = 0;i < 5;i++) {
            for(int j = 0;j < 10;j++) {
                assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
            }
        }

        int[] inputVector = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        int[] overlaps = sp.calculateOverlap(mem, inputVector);
        int[] trueOverlaps = new int[5];
        double[] overlapsPct = sp.calculateOverlapPct(mem, overlaps);
        double[] trueOverlapsPct = new double[5];
        assertTrue(Arrays.equals(trueOverlaps, overlaps));
        assertTrue(Arrays.equals(trueOverlapsPct, overlapsPct));
        
        /////////////
        
        connectedSynapses = new int[][] {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 1}};
        sm = new SparseBinaryMatrix(dimensions);
        for(int i = 0;i < sm.getDimensions()[0];i++) {
            for(int j = 0;j < sm.getDimensions()[1];j++) {
                sm.set(connectedSynapses[i][j], i, j);
            }
        }

        mem.setConnectedMatrix(sm);
        
        for(int i = 0;i < 5;i++) {
            for(int j = 0;j < 10;j++) {
                assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
            }
        }
        
        inputVector = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
        overlaps = sp.calculateOverlap(mem, inputVector);
        trueOverlaps = new int[] { 10, 8, 6, 4, 2 };
        overlapsPct = sp.calculateOverlapPct(mem, overlaps);
        trueOverlapsPct = new double[] { 1, 1, 1, 1, 1 };
        assertTrue(Arrays.equals(trueOverlaps, overlaps));
        assertTrue(Arrays.equals(trueOverlapsPct, overlapsPct));
        
        //////////////////
        
        connectedSynapses = new int[][] {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 1}};
        sm = new SparseBinaryMatrix(dimensions);
        for(int i = 0;i < sm.getDimensions()[0];i++) {
            for(int j = 0;j < sm.getDimensions()[1];j++) {
                sm.set(connectedSynapses[i][j], i, j);
            }
        }

        mem.setConnectedMatrix(sm);
        
        for(int i = 0;i < 5;i++) {
            for(int j = 0;j < 10;j++) {
                assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
            }
        }
        
        inputVector = new int[10];
        inputVector[9] = 1;
        overlaps = sp.calculateOverlap(mem, inputVector);
        trueOverlaps = new int[] { 1, 1, 1, 1, 1 };
        overlapsPct = sp.calculateOverlapPct(mem, overlaps);
        trueOverlapsPct = new double[] { 0.1, 0.125, 1.0/6, 0.25, 0.5 };
        assertTrue(Arrays.equals(trueOverlaps, overlaps));
        assertTrue(Arrays.equals(trueOverlapsPct, overlapsPct));
        
        ///////////////////
        
        connectedSynapses = new int[][] {
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};
        sm = new SparseBinaryMatrix(dimensions);
        for(int i = 0;i < sm.getDimensions()[0];i++) {
            for(int j = 0;j < sm.getDimensions()[1];j++) {
                sm.set(connectedSynapses[i][j], i, j);
            }
        }

        mem.setConnectedMatrix(sm);

        for(int i = 0;i < 5;i++) {
            for(int j = 0;j < 10;j++) {
                assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
            }
        }
        
        inputVector = new int[] { 1, 0, 1, 0, 1, 0, 1, 0, 1, 0 };
        overlaps = sp.calculateOverlap(mem, inputVector);
        trueOverlaps = new int[] { 1, 1, 1, 1, 1 };
        overlapsPct = sp.calculateOverlapPct(mem, overlaps);
        trueOverlapsPct = new double[] { 0.5, 0.5, 0.5, 0.5, 0.5 };
        assertTrue(Arrays.equals(trueOverlaps, overlaps));
        assertTrue(Arrays.equals(trueOverlapsPct, overlapsPct));
    }

    /**
     * test initial permanence generation. ensure that
     * a correct amount of synapses are initialized in 
     * a connected state, with permanence values drawn from
     * the correct ranges
     */
    @Test
    public void testInitPermanence1() {
        setupParameters();
        sp = new SpatialPooler() {
            private static final long serialVersionUID = 1L;
            public void raisePermanenceToThreshold(Connections c, double[] perm, int[] maskPotential) {
                //Mock out
            }
        };
        mem = new Connections();
        parameters.apply(mem);
        sp.init(mem);
        mem.setNumInputs(10);

        mem.setPotentialRadius(2);
        double connectedPct = 1;
        int[] mask = new int[] { 0, 1, 2, 8, 9 };
        double[] perm = sp.initPermanence(mem, mask, 0, connectedPct);
        int numcon = ArrayUtils.valueGreaterCount(mem.getSynPermConnected(), perm);
        assertEquals(5, numcon, 0);

        connectedPct = 0;
        perm = sp.initPermanence(mem, mask, 0, connectedPct);
        numcon = ArrayUtils.valueGreaterCount(mem.getSynPermConnected(), perm);
        assertEquals(0, numcon, 0);

        connectedPct = 0.5;
        mem.setPotentialRadius(100);
        mem.setNumInputs(100);
        mask = new int[100];
        for(int i = 0;i < 100;i++) mask[i] = i;
        final double[] perma = sp.initPermanence(mem, mask, 0, connectedPct);
        numcon = ArrayUtils.valueGreaterOrEqualCount(mem.getSynPermConnected(), perma);
        assertTrue(numcon > 0);
        assertTrue(numcon < mem.getNumInputs());

        final double minThresh = 0.0;
        final double maxThresh = mem.getSynPermMax();
        double[] results = ArrayUtils.retainLogicalAnd(perma, new Condition[] {
                new Condition.Adapter<Object>() {
                    public boolean eval(double d) {
                        return d >= minThresh;
                    }
                },
                new Condition.Adapter<Object>() {
                    public boolean eval(double d) {
                        return d < maxThresh;
                    }
                }
        });
        assertTrue(results.length > 0);
    }
    
    /**
     * Test initial permanence generation. ensure that permanence values
     * are only assigned to bits within a column's potential pool.
     */
    @Test
    public void testInitPermanence2() {
        setupParameters();
        sp = new SpatialPooler() {
            private static final long serialVersionUID = 1L;
            public void raisePermanenceToThreshold(Connections c, double[] perm, int[] maskPotential) {
                //Mock out
            }
        };
        mem = new Connections();
        parameters.apply(mem);
        sp.init(mem);
        
        mem.setNumInputs(10);
        double connectedPct = 1;
        int[] mask = new int[] { 0, 1 };
        double[] perm = sp.initPermanence(mem, mask, 0, connectedPct);
        int[] trueConnected = new int[] { 0, 1 };
        Condition<?> cond = new Condition.Adapter<Object>() {
            public boolean eval(double d) {
                return d > 0;
            }
        };
        assertTrue(Arrays.equals(trueConnected, ArrayUtils.where(perm, cond)));

        connectedPct = 1;
        mask = new int[] { 4, 5, 6 };
        perm = sp.initPermanence(mem, mask, 0, connectedPct);
        trueConnected = new int[] { 4, 5, 6 };
        assertTrue(Arrays.equals(trueConnected, ArrayUtils.where(perm, cond)));

        connectedPct = 1;
        mask = new int[] { 8, 9 };
        perm = sp.initPermanence(mem, mask, 0, connectedPct);
        trueConnected = new int[] { 8, 9 };
        assertTrue(Arrays.equals(trueConnected, ArrayUtils.where(perm, cond)));

        connectedPct = 1;
        mask = new int[] { 0, 1, 2, 3, 4, 5, 6, 8, 9 };
        perm = sp.initPermanence(mem, mask, 0, connectedPct);
        trueConnected = new int[] { 0, 1, 2, 3, 4, 5, 6, 8, 9 };
        assertTrue(Arrays.equals(trueConnected, ArrayUtils.where(perm, cond)));
    }
    
    /**
     * Tests that duty cycles are updated properly according
     * to the mathematical formula. also check the effects of
     * supplying a maxPeriod to the function.
     */
    @Test
    public void testUpdateDutyCycleHelper() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 5 });
        parameters.setColumnDimensions(new int[] { 5 });
        initSP();

        double[] dc = new double[5];
        Arrays.fill(dc, 1000.0);
        double[] newvals = new double[5];
        int period = 1000;
        double[] newDc = sp.updateDutyCyclesHelper(mem, dc, newvals, period);
        double[] trueNewDc = new double[] { 999, 999, 999, 999, 999 };
        assertTrue(Arrays.equals(trueNewDc, newDc));

        dc = new double[5];
        Arrays.fill(dc, 1000.0);
        newvals = new double[5];
        Arrays.fill(newvals, 1000);
        period = 1000;
        newDc = sp.updateDutyCyclesHelper(mem, dc, newvals, period);
        trueNewDc = Arrays.copyOf(dc, 5);
        assertTrue(Arrays.equals(trueNewDc, newDc));

        dc = new double[5];
        Arrays.fill(dc, 1000.0);
        newvals = new double[] { 2000, 4000, 5000, 6000, 7000 };
        period = 1000;
        newDc = sp.updateDutyCyclesHelper(mem, dc, newvals, period);
        trueNewDc = new double[] { 1001, 1003, 1004, 1005, 1006 };
        assertTrue(Arrays.equals(trueNewDc, newDc));

        dc = new double[] { 1000, 800, 600, 400, 2000 };
        newvals = new double[5];
        period = 2;
        newDc = sp.updateDutyCyclesHelper(mem, dc, newvals, period);
        trueNewDc = new double[] { 500, 400, 300, 200, 1000 };
        assertTrue(Arrays.equals(trueNewDc, newDc));
    }
    
    @Test
    public void testInhibitColumnsGlobal() {
        setupParameters();
        parameters.setColumnDimensions(new int[] { 10 });
        initSP();
        //Internally calculated during init, to overwrite we put after init
        parameters.setInhibitionRadius(2);
        double density = 0.3;
        double[] overlaps = new double[] { 1, 2, 1, 4, 8, 3, 12, 5, 4, 1 };
        int[] active = sp.inhibitColumnsGlobal(mem, overlaps, density);
        int[] trueActive = new int[] { 4, 6, 7 };
        Arrays.sort(active);
        assertTrue(Arrays.equals(trueActive, active));
        
        density = 0.5;
        mem.setNumColumns(10);
        overlaps = IntStream.range(0, 10).mapToDouble(i -> i).toArray();
        active = sp.inhibitColumnsGlobal(mem, overlaps, density);
        trueActive = IntStream.range(5, 10).toArray();
        assertTrue(Arrays.equals(trueActive, active));        
    }

    @Test
    public void testInhibitColumnsLocal() {
        setupParameters();
        parameters.setInputDimensions(new int[] { 5 });
        parameters.setColumnDimensions(new int[] { 10 });
        initSP();

        //Internally calculated during init, to overwrite we put after init
        mem.setInhibitionRadius(2);
        double density = 0.5;
        double[] overlaps = new double[] { 1, 2, 7, 0, 3, 4, 16, 1, 1.5, 1.7 };
                                       //  L  W  W  L  L  W  W   L   W    W (wrapAround=true)
                                       //  L  W  W  L  L  W  W   L   L    W (wrapAround=false)
        
        mem.setWrapAround(true);
        int[] trueActive = new int[] {1, 2, 5, 6, 8, 9};
        int[] active = sp.inhibitColumnsLocal(mem, overlaps, density);
        assertTrue(Arrays.equals(trueActive, active));
        
        mem.setWrapAround(false);
        trueActive = new int[] {1, 2, 5, 6, 9};
        active = sp.inhibitColumnsLocal(mem, overlaps, density);
        assertTrue(Arrays.equals(trueActive, active));

        density = 0.5;
        mem.setInhibitionRadius(3);
        overlaps = new double[] { 1, 2, 7, 0, 3, 4, 16, 1, 1.5, 1.7 };
                              //  L  W  W  L  W  W  W   L   L    W (wrapAround=true)
                              //  L  W  W  L  W  W  W   L   L    L (wrapAround=false)
        
        mem.setWrapAround(true);
        trueActive = new int[] { 1, 2, 4, 5, 6, 9 };
        active = sp.inhibitColumnsLocal(mem, overlaps, density);
        assertTrue(Arrays.equals(trueActive, active));
        
        mem.setWrapAround(false);
        trueActive = new int[] { 1, 2, 4, 5, 6, 9 };
        active = sp.inhibitColumnsLocal(mem, overlaps, density);
        assertTrue(Arrays.equals(trueActive, active));
        
        // Test add to winners
        density = 0.3333;
        mem.setInhibitionRadius(3);
        overlaps = new double[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
                              //  W  W  L  L  W  W  L  L  L  L (wrapAround=true)
                              //  W  W  L  L  W  W  L  L  W  L (wrapAround=false)
        
        mem.setWrapAround(true);
        trueActive = new int[] { 0, 1, 4, 5 };
        active = sp.inhibitColumnsLocal(mem, overlaps, density);
        assertTrue(Arrays.equals(trueActive, active));
        
        mem.setWrapAround(false);
        trueActive = new int[] { 0, 1, 4, 5, 8 };
        active = sp.inhibitColumnsLocal(mem, overlaps, density);
        assertTrue(Arrays.equals(trueActive, active));
    }
    
//    /**
//     * As coded in the Python test
//     */
//    @Test
//    public void testGetNeighborsND() {
//        //This setup isn't relevant to this test
//        setupParameters();
//        parameters.setInputDimensions(new int[] { 9, 5 });
//        parameters.setColumnDimensions(new int[] { 5, 5 });
//        initSP();
//        ////////////////////// Test not part of Python port /////////////////////
//        int[] result = sp.getNeighborsND(mem, 2, mem.getInputMatrix(), 3, true).toArray();
//        int[] expected = new int[] { 
//                0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 
//                13, 14, 15, 16, 17, 18, 19, 30, 31, 32, 33, 
//                34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44 
//        };
//        for(int i = 0;i < result.length;i++) {
//            assertEquals(expected[i], result[i]);
//        }
//        /////////////////////////////////////////////////////////////////////////
//        setupParameters();
//        int[] dimensions = new int[] { 5, 7, 2 };
//        parameters.setInputDimensions(dimensions);
//        parameters.setColumnDimensions(dimensions);
//        initSP();
//        int radius = 1;
//        int x = 1;
//        int y = 3;
//        int z = 2;
//        int columnIndex = mem.getInputMatrix().computeIndex(new int[] { z, y, x });
//        int[] neighbors = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        String expect = "[18, 19, 20, 21, 22, 23, 32, 33, 34, 36, 37, 46, 47, 48, 49, 50, 51]";
//        assertEquals(expect, ArrayUtils.print1DArray(neighbors));
//
//        /////////////////////////////////////////
//        setupParameters();
//        dimensions = new int[] { 5, 7, 9 };
//        parameters.setInputDimensions(dimensions);
//        parameters.setColumnDimensions(dimensions);
//        initSP();
//        radius = 3;
//        x = 0;
//        y = 0;
//        z = 3;
//        columnIndex = mem.getInputMatrix().computeIndex(new int[] { z, y, x });
//        neighbors = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        expect = "[0, 1, 2, 3, 6, 7, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19, 20, 21, 24, 25, 26, "
//                + "27, 28, 29, 30, 33, 34, 35, 36, 37, 38, 39, 42, 43, 44, 45, 46, 47, 48, 51, "
//                + "52, 53, 54, 55, 56, 57, 60, 61, 62, 63, 64, 65, 66, 69, 70, 71, 72, 73, 74, "
//                + "75, 78, 79, 80, 81, 82, 83, 84, 87, 88, 89, 90, 91, 92, 93, 96, 97, 98, 99, "
//                + "100, 101, 102, 105, 106, 107, 108, 109, 110, 111, 114, 115, 116, 117, 118, 119, "
//                + "120, 123, 124, 125, 126, 127, 128, 129, 132, 133, 134, 135, 136, 137, 138, 141, "
//                + "142, 143, 144, 145, 146, 147, 150, 151, 152, 153, 154, 155, 156, 159, 160, 161, "
//                + "162, 163, 164, 165, 168, 169, 170, 171, 172, 173, 174, 177, 178, 179, 180, 181, "
//                + "182, 183, 186, 187, 188, 190, 191, 192, 195, 196, 197, 198, 199, 200, 201, 204, "
//                + "205, 206, 207, 208, 209, 210, 213, 214, 215, 216, 217, 218, 219, 222, 223, 224, "
//                + "225, 226, 227, 228, 231, 232, 233, 234, 235, 236, 237, 240, 241, 242, 243, 244, "
//                + "245, 246, 249, 250, 251, 252, 253, 254, 255, 258, 259, 260, 261, 262, 263, 264, "
//                + "267, 268, 269, 270, 271, 272, 273, 276, 277, 278, 279, 280, 281, 282, 285, 286, "
//                + "287, 288, 289, 290, 291, 294, 295, 296, 297, 298, 299, 300, 303, 304, 305, 306, "
//                + "307, 308, 309, 312, 313, 314]";
//        assertEquals(expect, ArrayUtils.print1DArray(neighbors));
//
//        /////////////////////////////////////////
//        setupParameters();
//        dimensions = new int[] { 5, 10, 7, 6 };
//        parameters.setInputDimensions(dimensions);
//        parameters.setColumnDimensions(dimensions);
//        initSP();
//
//        radius = 4;
//        int w = 2;
//        x = 5;
//        y = 6;
//        z = 2;
//        columnIndex = mem.getInputMatrix().computeIndex(new int[] { z, y, x, w });
//        neighbors = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        TIntHashSet trueNeighbors = new TIntHashSet();
//        for(int i = -radius;i <= radius;i++) {
//            for(int j = -radius;j <= radius;j++) {
//                for(int k = -radius;k <= radius;k++) {
//                    for(int m = -radius;m <= radius;m++) {
//                        int zprime = (int)ArrayUtils.positiveRemainder((z + i), dimensions[0]);
//                        int yprime = (int)ArrayUtils.positiveRemainder((y + j), dimensions[1]);
//                        int xprime = (int)ArrayUtils.positiveRemainder((x + k), dimensions[2]);
//                        int wprime = (int)ArrayUtils.positiveRemainder((w + m), dimensions[3]);
//                        trueNeighbors.add(mem.getInputMatrix().computeIndex(new int[] { zprime, yprime, xprime, wprime }));
//                    }
//                }
//            }
//        }
//        trueNeighbors.remove(columnIndex);
//        int[] tneighbors = ArrayUtils.unique(trueNeighbors.toArray());
//        assertEquals(ArrayUtils.print1DArray(tneighbors), ArrayUtils.print1DArray(neighbors));
//
//        /////////////////////////////////////////
//        //Tests from getNeighbors1D from Python unit test
//        setupParameters();
//        dimensions = new int[] { 8 };
//        parameters.setColumnDimensions(dimensions);
//        parameters.setInputDimensions(dimensions);
//        initSP();
//        AbstractSparseBinaryMatrix sbm = (AbstractSparseBinaryMatrix)mem.getInputMatrix();
//        sbm.set(new int[] { 2, 4 }, new int[] { 1, 1 }, true);
//        radius = 1;
//        columnIndex = 3;
//        int[] mask = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        TIntArrayList msk = new TIntArrayList(mask);
//        TIntArrayList neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
//        neg.removeAll(msk);
//        assertTrue(sbm.all(mask));
//        assertFalse(sbm.any(neg));
//
//        //////
//        setupParameters();
//        dimensions = new int[] { 8 };
//        parameters.setInputDimensions(dimensions);
//        initSP();
//        sbm = (AbstractSparseBinaryMatrix)mem.getInputMatrix();
//        sbm.set(new int[] { 1, 2, 4, 5 }, new int[] { 1, 1, 1, 1 }, true);
//        radius = 2;
//        columnIndex = 3;
//        mask = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        msk = new TIntArrayList(mask);
//        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
//        neg.removeAll(msk);
//        assertTrue(sbm.all(mask));
//        assertFalse(sbm.any(neg));
//
//        //Wrap around
//        setupParameters();
//        dimensions = new int[] { 8 };
//        parameters.setInputDimensions(dimensions);
//        initSP();
//        sbm = (AbstractSparseBinaryMatrix)mem.getInputMatrix();
//        sbm.set(new int[] { 1, 2, 6, 7 }, new int[] { 1, 1, 1, 1 }, true);
//        radius = 2;
//        columnIndex = 0;
//        mask = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        msk = new TIntArrayList(mask);
//        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
//        neg.removeAll(msk);
//        assertTrue(sbm.all(mask));
//        assertFalse(sbm.any(neg));
//
//        //Radius too big
//        setupParameters();
//        dimensions = new int[] { 8 };
//        parameters.setInputDimensions(dimensions);
//        initSP();
//        sbm = (AbstractSparseBinaryMatrix)mem.getInputMatrix();
//        sbm.set(new int[] { 0, 1, 2, 3, 4, 5, 7 }, new int[] { 1, 1, 1, 1, 1, 1, 1 }, true);
//        radius = 20;
//        columnIndex = 6;
//        mask = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        msk = new TIntArrayList(mask);
//        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
//        neg.removeAll(msk);
//        assertTrue(sbm.all(mask));
//        assertFalse(sbm.any(neg));
//
//        //These are all the same tests from 2D
//        setupParameters();
//        dimensions = new int[] { 6, 5 };
//        parameters.setInputDimensions(dimensions);
//        parameters.setColumnDimensions(dimensions);
//        initSP();
//        sbm = (AbstractSparseBinaryMatrix)mem.getInputMatrix();
//        int[][] input = new int[][] { 
//            {0, 0, 0, 0, 0},
//            {0, 0, 0, 0, 0},
//            {0, 1, 1, 1, 0},
//            {0, 1, 0, 1, 0},
//            {0, 1, 1, 1, 0},
//            {0, 0, 0, 0, 0}};
//            for(int i = 0;i < input.length;i++) {
//                for(int j = 0;j < input[i].length;j++) {
//                    if(input[i][j] == 1) 
//                        sbm.set(sbm.computeIndex(new int[] { i, j }), 1);
//                }
//            }
//        radius = 1;
//        columnIndex = 3*5 + 2;
//        mask = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        msk = new TIntArrayList(mask);
//        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
//        neg.removeAll(msk);
//        assertTrue(sbm.all(mask));
//        assertFalse(sbm.any(neg));
//
//        ////////
//        setupParameters();
//        dimensions = new int[] { 6, 5 };
//        parameters.setInputDimensions(dimensions);
//        parameters.setColumnDimensions(dimensions);
//        initSP();
//        sbm = (AbstractSparseBinaryMatrix)mem.getInputMatrix();
//        input = new int[][] { 
//            {0, 0, 0, 0, 0},
//            {1, 1, 1, 1, 1},
//            {1, 1, 1, 1, 1},
//            {1, 1, 0, 1, 1},
//            {1, 1, 1, 1, 1},
//            {1, 1, 1, 1, 1}};
//        for(int i = 0;i < input.length;i++) {
//            for(int j = 0;j < input[i].length;j++) {
//                if(input[i][j] == 1) 
//                    sbm.set(sbm.computeIndex(new int[] { i, j }), 1);
//            }
//        }
//        radius = 2;
//        columnIndex = 3*5 + 2;
//        mask = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        msk = new TIntArrayList(mask);
//        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
//        neg.removeAll(msk);
//        assertTrue(sbm.all(mask));
//        assertFalse(sbm.any(neg));
//
//        //Radius too big
//        setupParameters();
//        dimensions = new int[] { 6, 5 };
//        parameters.setInputDimensions(dimensions);
//        parameters.setColumnDimensions(dimensions);
//        initSP();
//        sbm = (AbstractSparseBinaryMatrix)mem.getInputMatrix();
//        input = new int[][] { 
//            {1, 1, 1, 1, 1},
//            {1, 1, 1, 1, 1},
//            {1, 1, 1, 1, 1},
//            {1, 1, 0, 1, 1},
//            {1, 1, 1, 1, 1},
//            {1, 1, 1, 1, 1}};
//            for(int i = 0;i < input.length;i++) {
//                for(int j = 0;j < input[i].length;j++) {
//                    if(input[i][j] == 1) 
//                        sbm.set(sbm.computeIndex(new int[] { i, j }), 1);
//                }
//            }
//        radius = 7;
//        columnIndex = 3*5 + 2;
//        mask = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        msk = new TIntArrayList(mask);
//        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
//        neg.removeAll(msk);
//        assertTrue(sbm.all(mask));
//        assertFalse(sbm.any(neg));
//
//        //Wrap-around
//        setupParameters();
//        dimensions = new int[] { 6, 5 };
//        parameters.setInputDimensions(dimensions);
//        parameters.setColumnDimensions(dimensions);
//        initSP();
//        sbm = (AbstractSparseBinaryMatrix)mem.getInputMatrix();
//        input = new int[][] { 
//            {1, 0, 0, 1, 1},
//            {0, 0, 0, 0, 0},
//            {0, 0, 0, 0, 0},
//            {0, 0, 0, 0, 0},
//            {1, 0, 0, 1, 1},
//            {1, 0, 0, 1, 0}};
//        for(int i = 0;i < input.length;i++) {
//            for(int j = 0;j < input[i].length;j++) {
//                if(input[i][j] == 1) 
//                    sbm.set(sbm.computeIndex(new int[] { i, j }), 1);
//            }
//        }
//        radius = 1;
//        columnIndex = sbm.getMaxIndex();
//        mask = sp.getNeighborsND(mem, columnIndex, mem.getInputMatrix(), radius, true).toArray();
//        msk = new TIntArrayList(mask);
//        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
//        neg.removeAll(msk);
//        assertTrue(sbm.all(mask));
//        assertFalse(sbm.any(neg));
//    }
    
    @Test
    public void testInit() {
        setupParameters();
        parameters.setNumActiveColumnsPerInhArea(0);
        parameters.setLocalAreaDensity(0);
        
        Connections c = new Connections();
        parameters.apply(c);
        
        SpatialPooler sp = new SpatialPooler();
        
        // Local Area Density cannot be 0
        try {
            sp.init(c);
            fail();
        }catch(Exception e) {
            assertEquals("Inhibition parameters are invalid", e.getMessage());
            assertEquals(InvalidSPParamValueException.class, e.getClass());
        }
        
        // Local Area Density can't be above 0.5
        parameters.setLocalAreaDensity(0.51);
        c = new Connections();
        parameters.apply(c);
        try {
            sp.init(c);
            fail();
        }catch(Exception e) {
            assertEquals("Inhibition parameters are invalid", e.getMessage());
            assertEquals(InvalidSPParamValueException.class, e.getClass());
        }
        
        // Local Area Density should be sane here
        parameters.setLocalAreaDensity(0.5);
        c = new Connections();
        parameters.apply(c);
        try {
            sp.init(c);
        }catch(Exception e) {
            fail();
        }
        
        // Num columns cannot be 0
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 0 });
        c = new Connections();
        parameters.apply(c);
        try {
            sp.init(c);
            fail();
        }catch(Exception e) {
            assertEquals("Invalid number of columns: 0", e.getMessage());
            assertEquals(InvalidSPParamValueException.class, e.getClass());
        }
        
        // Reset column dims
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 5 });
        
        // Num columns cannot be 0
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 0 });
        c = new Connections();
        parameters.apply(c);
        try {
            sp.init(c);
            fail();
        }catch(Exception e) {
            assertEquals("Invalid number of inputs: 0", e.getMessage());
            assertEquals(InvalidSPParamValueException.class, e.getClass());
        }
    }
    
    @Test
    public void testComputeInputMismatch() {
        setupParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 2, 4 });
        parameters.setColumnDimensions(new int[] { 5, 1 });
        
        Connections c = new Connections();
        parameters.apply(c);
        
        int misMatchedDims = 6; // not 8
        SpatialPooler sp = new SpatialPooler();
        sp.init(c);
        try {
            sp.compute(c, new int[misMatchedDims], new int[25], true);
            fail();
        }catch(Exception e) {
            assertEquals("Input array must be same size as the defined number"
                + " of inputs: From Params: 8, From Input Vector: 6", e.getMessage());
            assertEquals(InvalidSPParamValueException.class, e.getClass());
        }
        
        
        // Now Do the right thing
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 2, 4 });
        parameters.setColumnDimensions(new int[] { 5, 1 });
        
        c = new Connections();
        parameters.apply(c);
        
        int matchedDims = 8; // same as input dimension multiplied, above
        sp.init(c);
        try {
            sp.compute(c, new int[matchedDims], new int[25], true);
        }catch(Exception e) {
            fail();
        }
    }
}
