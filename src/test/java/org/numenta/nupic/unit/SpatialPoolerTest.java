/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.EnumMap;

import org.junit.Test;
import org.numenta.nupic.data.ArrayUtils;
import org.numenta.nupic.data.SparseBinaryMatrix;
import org.numenta.nupic.data.SparseObjectMatrix;
import org.numenta.nupic.model.Pool;
import org.numenta.nupic.research.Connections;
import org.numenta.nupic.research.Parameters;
import org.numenta.nupic.research.Parameters.KEY;
import org.numenta.nupic.research.SpatialPooler;

public class SpatialPoolerTest {
    private Parameters parameters;
    private SpatialPooler sp;
    private Connections mem;
    
    public void setupParameters() {
        parameters = new Parameters();
        EnumMap<Parameters.KEY, Object> p = parameters.getMap();
        p.put(KEY.INPUT_DIMENSIONS, new int[] { 5 });//5
        p.put(KEY.COLUMN_DIMENSIONS, new int[] { 5 });//5
        p.put(KEY.POTENTIAL_RADIUS, 3);//3
        p.put(KEY.POTENTIAL_PCT, 0.5);//0.5
        p.put(KEY.GLOBAL_INHIBITIONS, false);
        p.put(KEY.LOCAL_AREA_DENSITY, -1.0);
        p.put(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 3);
        p.put(KEY.STIMULUS_THRESHOLD, 1.0);
        p.put(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        p.put(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        p.put(KEY.SYN_PERM_CONNECTED, 0.1);
        p.put(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
        p.put(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        p.put(KEY.DUTY_CYCLE_PERIOD, 10);
        p.put(KEY.MAX_BOOST, 10.0);
        p.put(KEY.SEED, 42);
        p.put(KEY.SP_VERBOSITY, 0);
    }
    
    private void initSP() {
        sp = new SpatialPooler();
        mem = new Connections();
        Parameters.apply(mem, parameters);
        sp.init(mem);
    }
    
    @Test
    public void confirmSPConstruction() {
        setupParameters();
        
        initSP();
        
        assertEquals(5, mem.getInputDimensions()[0]);
        assertEquals(5, mem.getColumnDimensions()[0]);
        assertEquals(3, mem.getPotentialRadius());
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
        assertEquals(0, mem.getSpVerbosity());
        
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
        initSP();
        
        SparseObjectMatrix<Pool> s = mem.getPotentialPools();
        
        System.out.println(s);
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
    	
    	// Test 1D with same dimensions of length 1
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
    }
    
    @Test
    public void testStripNeverLearned() {
    	setupParameters();
    	parameters.setColumnDimensions(new int[] { 6 });
    	parameters.setInputDimensions(new int[] { 9 });
    	initSP();
    	
    	mem.updateActiveDutyCycles(new double[] { 0.5, 0.1, 0, 0.2, 0.4, 0 });
    	int[] activeColumns = new int[] { 0, 1, 2, 4 };
    	TIntArrayList stripped = sp.stripNeverLearned(mem, activeColumns);
    	TIntArrayList trueStripped = new TIntArrayList(new int[] { 0, 1, 4 });
    	assertEquals(trueStripped, stripped);
    	
    	mem.updateActiveDutyCycles(new double[] { 0.9, 0, 0, 0, 0.4, 0.3 });
    	activeColumns = ArrayUtils.range(0,  6);
    	stripped = sp.stripNeverLearned(mem, activeColumns);
    	trueStripped = new TIntArrayList(new int[] { 0, 4, 5 });
    	assertEquals(trueStripped, stripped);
    	
    	mem.updateActiveDutyCycles(new double[] { 0, 0, 0, 0, 0, 0 });
    	activeColumns = ArrayUtils.range(0,  6);
    	stripped = sp.stripNeverLearned(mem, activeColumns);
    	trueStripped = new TIntArrayList();
    	assertEquals(trueStripped, stripped);
    	
    	mem.updateActiveDutyCycles(new double[] { 1, 1, 1, 1, 1, 1 });
    	activeColumns = ArrayUtils.range(0,  6);
    	stripped = sp.stripNeverLearned(mem, activeColumns);
    	trueStripped = new TIntArrayList(ArrayUtils.range(0,  6));
    	assertEquals(trueStripped, stripped);
    	
    	
    }
    
    @Test
    public void testMapPotential1D() {
    	setupParameters();
        parameters.setInputDimensions(new int[] { 12 });
        parameters.setColumnDimensions(new int[] { 4 });
        parameters.setPotentialRadius(2);
        parameters.setPotentialPct(1);
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
        expected = new int[] { 0, 1, 2, 3, 11 };
        mask = sp.mapPotential(mem, 0, true);
        assertTrue(Arrays.equals(expected, mask));
        
        expected = new int[] { 0, 8, 9, 10, 11 };
        mask = sp.mapPotential(mem, 3, true);
        assertTrue(Arrays.equals(expected, mask));
        
        // Test with wrapAround and potentialPct < 1
        parameters.setPotentialPct(0.5);
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
        mem.getConnectedSynapses().set(0, connected.toArray());
        
        connected.clear();
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 2, 0, 1, 0 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 2, 0, 0, 0 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 0, 0, 0 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 0, 1, 0 }, false));
        connected.sort(0, connected.size());
        //[ 80  85 120 125]
        mem.getConnectedSynapses().set(1, connected.toArray());
        
        connected.clear();
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 1, 4 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 0, 3 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 0, 1 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 1, 0, 0, 2 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 1, 1 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 3, 1, 1 }, false));
        connected.sort(0, connected.size());
        //[  1   3   6   9  42 156]
        mem.getConnectedSynapses().set(2, connected.toArray());
        
        connected.clear();
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 3, 3, 1, 4 }, false));
        connected.add(mem.getInputMatrix().computeIndex(new int[] { 0, 0, 0, 0 }, false));
        connected.sort(0, connected.size());
        //[  0 159]
        mem.getConnectedSynapses().set(3, connected.toArray());
        
        //[]
        mem.getConnectedSynapses().set(4, new int[0]);
        
        double[] trueAvgConnectedSpan = new double[] { 11.0/4d, 6.0/4d, 14.0/4d, 15.0/4d, 0d };
        for(int i = 0;i < mem.getNumColumns();i++) {
        	double connectedSpan = sp.avgConnectedSpanForColumnND(mem, i);
        	assertEquals(trueAvgConnectedSpan[i], connectedSpan, 0);
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
    	
    	// ((3 * 4) - 1) / 2 => round up
    	SpatialPooler mock = new SpatialPooler() {
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
    	
    	//Test clipping at 1.0
    	mock = new SpatialPooler() {
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
    	
    	//Test rounding up
    	mock = new SpatialPooler() {
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
    
    /**
     * As coded in the Python test
     */
    @Test
    public void testGetNeighborsND() {
        //This setup isn't relevant to this test
        setupParameters();
        parameters.setInputDimensions(new int[] { 9, 5 });
        parameters.setColumnDimensions(new int[] { 5, 5 });
        initSP();
        
        ////////////////////// Test not part of Python port /////////////////////
        int[] result = sp.getNeighborsND(mem, 2, 3, true).toArray();
        int[] expected = new int[] { 
            0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 
            13, 14, 15, 16, 17, 18, 19, 30, 31, 32, 33, 
            34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44 
        };
        for(int i = 0;i < result.length;i++) {
            assertEquals(expected[i], result[i]);
        }
        /////////////////////////////////////////////////////////////////////////
        
        setupParameters();
        int[] dimensions = new int[] { 5, 7, 2 };
        parameters.setInputDimensions(dimensions);
        parameters.setColumnDimensions(dimensions);
        initSP();
        int radius = 1;
        int x = 1;
        int y = 3;
        int z = 2;
        int columnIndex = mem.getInputMatrix().computeIndex(new int[] { z, y, x });
        int[] neighbors = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        String expect = "[18, 19, 20, 21, 22, 23, 32, 33, 34, 36, 37, 46, 47, 48, 49, 50, 51]";
        assertEquals(expect, ArrayUtils.print1DArray(neighbors));
        
        /////////////////////////////////////////
        
        setupParameters();
        dimensions = new int[] { 5, 7, 9 };
        parameters.setInputDimensions(dimensions);
        parameters.setColumnDimensions(dimensions);
        initSP();
        radius = 3;
        x = 0;
        y = 0;
        z = 3;
        columnIndex = mem.getInputMatrix().computeIndex(new int[] { z, y, x });
        neighbors = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        expect = "[0, 1, 2, 3, 6, 7, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19, 20, 21, 24, 25, 26, "
                + "27, 28, 29, 30, 33, 34, 35, 36, 37, 38, 39, 42, 43, 44, 45, 46, 47, 48, 51, "
                + "52, 53, 54, 55, 56, 57, 60, 61, 62, 63, 64, 65, 66, 69, 70, 71, 72, 73, 74, "
                + "75, 78, 79, 80, 81, 82, 83, 84, 87, 88, 89, 90, 91, 92, 93, 96, 97, 98, 99, "
                + "100, 101, 102, 105, 106, 107, 108, 109, 110, 111, 114, 115, 116, 117, 118, 119, "
                + "120, 123, 124, 125, 126, 127, 128, 129, 132, 133, 134, 135, 136, 137, 138, 141, "
                + "142, 143, 144, 145, 146, 147, 150, 151, 152, 153, 154, 155, 156, 159, 160, 161, "
                + "162, 163, 164, 165, 168, 169, 170, 171, 172, 173, 174, 177, 178, 179, 180, 181, "
                + "182, 183, 186, 187, 188, 190, 191, 192, 195, 196, 197, 198, 199, 200, 201, 204, "
                + "205, 206, 207, 208, 209, 210, 213, 214, 215, 216, 217, 218, 219, 222, 223, 224, "
                + "225, 226, 227, 228, 231, 232, 233, 234, 235, 236, 237, 240, 241, 242, 243, 244, "
                + "245, 246, 249, 250, 251, 252, 253, 254, 255, 258, 259, 260, 261, 262, 263, 264, "
                + "267, 268, 269, 270, 271, 272, 273, 276, 277, 278, 279, 280, 281, 282, 285, 286, "
                + "287, 288, 289, 290, 291, 294, 295, 296, 297, 298, 299, 300, 303, 304, 305, 306, "
                + "307, 308, 309, 312, 313, 314]";
        assertEquals(expect, ArrayUtils.print1DArray(neighbors));
        
        /////////////////////////////////////////
        
        setupParameters();
        dimensions = new int[] { 5, 10, 7, 6 };
        parameters.setInputDimensions(dimensions);
        parameters.setColumnDimensions(dimensions);
        initSP();
        radius = 4;
        int w = 2;
        x = 5;
        y = 6;
        z = 2;
        columnIndex = mem.getInputMatrix().computeIndex(new int[] { z, y, x, w });
        neighbors = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        
        TIntHashSet trueNeighbors = new TIntHashSet();
        for(int i = -radius;i <= radius;i++) {
            for(int j = -radius;j <= radius;j++) {
                for(int k = -radius;k <= radius;k++) {
                    for(int m = -radius;m <= radius;m++) {
                        int zprime = (int)ArrayUtils.positiveRemainder((z + i), dimensions[0]);
                        int yprime = (int)ArrayUtils.positiveRemainder((y + j), dimensions[1]);
                        int xprime = (int)ArrayUtils.positiveRemainder((x + k), dimensions[2]);
                        int wprime = (int)ArrayUtils.positiveRemainder((w + m), dimensions[3]);
                        trueNeighbors.add(mem.getInputMatrix().computeIndex(new int[] { zprime, yprime, xprime, wprime }));
                    }
                }
            }
        }
        trueNeighbors.remove(columnIndex);
        int[] tneighbors = ArrayUtils.unique(trueNeighbors.toArray());
        assertEquals(ArrayUtils.print1DArray(tneighbors), ArrayUtils.print1DArray(neighbors));
        
        /////////////////////////////////////////
        //Tests from getNeighbors1D from Python unit test
        setupParameters();
        dimensions = new int[] { 8 };
        parameters.setInputDimensions(dimensions);
        initSP();
        SparseBinaryMatrix sbm = (SparseBinaryMatrix)mem.getInputMatrix();
        sbm.set(new int[] { 2, 4 }, new int[] { 1, 1 });
        radius = 1;
        columnIndex = 3;
        int[] mask = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        TIntArrayList msk = new TIntArrayList(mask);
        TIntArrayList neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //////
        setupParameters();
        dimensions = new int[] { 8 };
        parameters.setInputDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)mem.getInputMatrix();
        sbm.set(new int[] { 1, 2, 4, 5 }, new int[] { 1, 1, 1, 1 });
        radius = 2;
        columnIndex = 3;
        mask = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //Wrap around
        setupParameters();
        dimensions = new int[] { 8 };
        parameters.setInputDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)mem.getInputMatrix();
        sbm.set(new int[] { 1, 2, 6, 7 }, new int[] { 1, 1, 1, 1 });
        radius = 2;
        columnIndex = 0;
        mask = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //Radius too big
        setupParameters();
        dimensions = new int[] { 8 };
        parameters.setInputDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)mem.getInputMatrix();
        sbm.set(new int[] { 0, 1, 2, 3, 4, 5, 7 }, new int[] { 1, 1, 1, 1, 1, 1, 1 });
        radius = 20;
        columnIndex = 6;
        mask = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //These are all the same tests from 2D
        setupParameters();
        dimensions = new int[] { 6, 5 };
        parameters.setInputDimensions(dimensions);
        parameters.setColumnDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)mem.getInputMatrix();
        int[][] input = new int[][] { {0, 0, 0, 0, 0},
                                  {0, 0, 0, 0, 0},
                                  {0, 1, 1, 1, 0},
                                  {0, 1, 0, 1, 0},
                                  {0, 1, 1, 1, 0},
                                  {0, 0, 0, 0, 0}};
        for(int i = 0;i < input.length;i++) {
            for(int j = 0;j < input[i].length;j++) {
                if(input[i][j] == 1) 
                    sbm.set(sbm.computeIndex(new int[] { i, j }), 1);
            }
        }
        radius = 1;
        columnIndex = 3*5 + 2;
        mask = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        ////////
        setupParameters();
        dimensions = new int[] { 6, 5 };
        parameters.setInputDimensions(dimensions);
        parameters.setColumnDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)mem.getInputMatrix();
        input = new int[][] { {0, 0, 0, 0, 0},
                              {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 0, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1}};
        for(int i = 0;i < input.length;i++) {
            for(int j = 0;j < input[i].length;j++) {
                if(input[i][j] == 1) 
                    sbm.set(sbm.computeIndex(new int[] { i, j }), 1);
            }
        }
        radius = 2;
        columnIndex = 3*5 + 2;
        mask = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //Radius too big
        setupParameters();
        dimensions = new int[] { 6, 5 };
        parameters.setInputDimensions(dimensions);
        parameters.setColumnDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)mem.getInputMatrix();
        input = new int[][] { {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 0, 1, 1},
                              {1, 1, 1, 1, 1},
                              {1, 1, 1, 1, 1}};
        for(int i = 0;i < input.length;i++) {
            for(int j = 0;j < input[i].length;j++) {
                if(input[i][j] == 1) 
                    sbm.set(sbm.computeIndex(new int[] { i, j }), 1);
            }
        }
        radius = 7;
        columnIndex = 3*5 + 2;
        mask = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
        
        //Wrap-around
        setupParameters();
        dimensions = new int[] { 6, 5 };
        parameters.setInputDimensions(dimensions);
        parameters.setColumnDimensions(dimensions);
        initSP();
        sbm = (SparseBinaryMatrix)mem.getInputMatrix();
        input = new int[][] { {1, 0, 0, 1, 1},
                              {0, 0, 0, 0, 0},
                              {0, 0, 0, 0, 0},
                              {0, 0, 0, 0, 0},
                              {1, 0, 0, 1, 1},
                              {1, 0, 0, 1, 0}};
        for(int i = 0;i < input.length;i++) {
            for(int j = 0;j < input[i].length;j++) {
                if(input[i][j] == 1) 
                    sbm.set(sbm.computeIndex(new int[] { i, j }), 1);
            }
        }
        radius = 1;
        columnIndex = sbm.getMaxIndex();
        mask = sp.getNeighborsND(mem, columnIndex, radius, true).toArray();
        msk = new TIntArrayList(mask);
        neg = new TIntArrayList(ArrayUtils.range(0, dimensions[0]));
        neg.removeAll(msk);
        assertTrue(sbm.all(mask));
        assertFalse(sbm.any(neg));
    }
    
    @Test
    public void testRaisePermanenceThreshold() {
    	setupParameters();
    	parameters.setInputDimensions(new int[] { 5 });
    	parameters.setColumnDimensions(new int[] { 5 });
    	parameters.setSynPermConnected(0.1);
    	parameters.setStimulusThreshold(3);
    	parameters.setSynPermBelowStimulusInc(0.01);
    	initSP();
    	
    	//We set the values on the Connections permanences here just for illustration
    	SparseObjectMatrix<double[]> objMatrix = new SparseObjectMatrix<double[]>(new int[] { 5, 5 });
    	objMatrix.set(0, new double[] { 0.0, 0.11, 0.095, 0.092, 0.01 });
    	objMatrix.set(1, new double[] { 0.12, 0.15, 0.02, 0.12, 0.09 });
    	objMatrix.set(2, new double[] { 0.51, 0.081, 0.025, 0.089, 0.31 });
    	objMatrix.set(3, new double[] { 0.18, 0.0601, 0.11, 0.011, 0.03 });
    	objMatrix.set(4, new double[] { 0.011, 0.011, 0.011, 0.011, 0.011 });
    	mem.setPermanences(objMatrix);
    	
    	mem.setConnectedSysnapses(new SparseObjectMatrix<int[]>(new int[] { 5, 5 }));
    	SparseObjectMatrix<int[]> syns = mem.getConnectedSynapses();
    	syns.set(0, new int[] { 0, 1, 0, 0, 0 });
    	syns.set(1, new int[] { 1, 1, 0, 1, 0 });
    	syns.set(2, new int[] { 1, 0, 0, 0, 1 });
    	syns.set(3, new int[] { 1, 0, 1, 0, 0 });
    	syns.set(4, new int[] { 0, 0, 0, 0, 0 });
    	
    	mem.setConnectedCounts(new int[] { 1, 3, 2, 2, 0 });
    	
    	double[][] truePermanences = new double[][] { 
    		{0.01, 0.12, 0.105, 0.102, 0.02},  		// incremented once
            {0.12, 0.15, 0.02, 0.12, 0.09},  		// no change
            {0.53, 0.101, 0.045, 0.109, 0.33},  	// increment twice
            {0.22, 0.1001, 0.15, 0.051, 0.07},  	// increment four times
            {0.101, 0.101, 0.101, 0.101, 0.101}};	// increment 9 times
    	
    	//FORGOT TO SET PERMANENCES ABOVE - DON'T USE mem.setPermanences() 
    	int[] indices = mem.getMemory().getSparseIndices();
    	for(int i = 0;i < mem.getNumColumns();i++) {
    		double[] perm = objMatrix.getObject(i);//mem.getPotentialPools().getObject(i).getPermanences();
    		sp.raisePermanenceToThreshold(mem, perm, indices);
    		System.out.println(Arrays.toString(perm));
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
    	initSP();
    	
    	System.out.println("synPermConnected = " + mem.getSynPermConnected());
    	
    	double[][] permanences = new double[][] {
    		{-0.10, 0.500, 0.400, 0.010, 0.020},
	        {0.300, 0.010, 0.020, 0.120, 0.090},
	        {0.070, 0.050, 1.030, 0.190, 0.060},
	        {0.180, 0.090, 0.110, 0.010, 0.030},
	        {0.200, 0.101, 0.050, -0.09, 1.100}};
    
    	for(int i = 0;i < mem.getNumColumns();i++) {
    		sp.updatePermanencesForColumn(mem, permanences[i], mem.getColumn(i), true);
    		System.out.println(Arrays.toString(mem.getPermanences(i)));
    	}
    }

    
}
