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
import org.numenta.nupic.research.Parameters;
import org.numenta.nupic.research.Parameters.KEY;
import org.numenta.nupic.research.SpatialLattice;
import org.numenta.nupic.research.SpatialPooler;

public class SpatialPoolerTest {
    private Parameters parameters;
    private SpatialPooler sp;
    private SpatialLattice mem;
    
    public void defaultSetup() {
        parameters = new Parameters();
        EnumMap<Parameters.KEY, Object> p = parameters.getMap();
        p.put(KEY.INPUT_DIMENSIONS, new int[] { 9 });
        p.put(KEY.COLUMN_DIMENSIONS, new int[] { 5 });
        p.put(KEY.POTENTIAL_RADIUS, 3);
        p.put(KEY.POTENTIAL_PCT, 0.5);
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
        mem = new SpatialLattice(sp, parameters);
    }
    
    @Test
    public void confirmSPConstruction() {
        defaultSetup();
        
        initSP();
        
        assertEquals(9, mem.getInputDimensions()[0]);
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
        
        assertEquals(9, mem.getNumInputs());
        assertEquals(5, mem.getNumColumns());
    }
    
    @Test
    public void testMapPotential1D() {
    	defaultSetup();
        parameters.setInputDimensions(new int[] { 10 });
        parameters.setColumnDimensions(new int[] { 4 });
        parameters.setPotentialRadius(2);
        parameters.setPotentialPct(1);
        initSP();
        
        assertEquals(10, mem.getInputDimensions()[0]);
        assertEquals(4, mem.getColumnDimensions()[0]);
        assertEquals(2, mem.getPotentialRadius());
        
        // Test without wrapAround and potentialPct = 1
        int[] expected = new int[] { 0, 1, 2 };
        int[] mask = sp.mapPotential(mem, 0, false);
        assertTrue(Arrays.equals(expected, mask));
        
        expected = new int[] { 4, 5, 6, 7, 8 };
        mask = sp.mapPotential(mem, 2, false);
        assertTrue(Arrays.equals(expected, mask));
        
        // Test with wrapAround and potentialPct = 1        
        expected = new int[] { 0, 1, 2, 8, 9 };
        mask = sp.mapPotential(mem, 0, true);
        assertTrue(Arrays.equals(expected, mask));
        
        expected = new int[] { 0, 1, 7, 8, 9 };
        mask = sp.mapPotential(mem, 3, true);
        assertTrue(Arrays.equals(expected, mask));
        
        // Test with wrapAround and potentialPct < 1
        parameters.setPotentialPct(0.5);
        initSP();
        
        int[] supersetMask = new int[] { 0, 1, 2, 8, 9 }; 
        mask = sp.mapPotential(mem, 0, true);
        assertEquals(mask.length, 3);
        TIntArrayList unionList = new TIntArrayList(supersetMask);
        unionList.addAll(mask);
        int[] unionMask = ArrayUtils.unique(unionList.toArray());
        assertTrue(Arrays.equals(unionMask, supersetMask));
    }
    
    @Test
    public void testMapPotential2D() {
    	defaultSetup();
        parameters.setInputDimensions(new int[] { 5, 10 });
        parameters.setColumnDimensions(new int[] { 2, 4 });
        parameters.setPotentialRadius(1);
        parameters.setPotentialPct(1);
        initSP();
        
        //Test without wrapAround
        int[] mask = sp.mapPotential(mem, 0, false);
        TIntHashSet trueIndices = new TIntHashSet(new int[] { 0, 1, 10, 11 });
        TIntHashSet maskSet = new TIntHashSet(mask);
        assertTrue(trueIndices.equals(maskSet));
        
        trueIndices.clear();
        maskSet.clear();
        trueIndices.addAll(new int[] { 5, 6, 7, 15, 16, 17 });
        mask = sp.mapPotential(mem, 2, false);
        maskSet.addAll(mask);
        assertTrue(trueIndices.equals(maskSet));
        
        //Test with wrapAround
        trueIndices.clear();
        maskSet.clear();
        trueIndices.addAll(new int[] { 49, 9, 19, 40, 0, 10, 41, 1, 11 });
        mask = sp.mapPotential(mem, 0, true);
        maskSet.addAll(mask);
        assertTrue(trueIndices.equals(maskSet));
        
        trueIndices.clear();
        maskSet.clear();
        trueIndices.addAll(new int[] { 48, 8, 18, 49, 9, 19, 40, 0, 10 });
        mask = sp.mapPotential(mem, 3, true);
        maskSet.addAll(mask);
        assertTrue(trueIndices.equals(maskSet));
    }
    
    @Test
    public void testMapPotential1Column1Input() {
    	defaultSetup();
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
    	defaultSetup();
    	parameters = null;
    	initSP();
    	
    	int[] inputDimensions = new int[] { 4, 4, 2, 5 };
        mem.setInputDimensions(inputDimensions);
        mem.setColumnDimensions(new int[] { 5 });
        mem.initMatrices();
        
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
    	defaultSetup();
    	initSP();
    	 
    	//Test global inhibition case
    	mem.setGlobalInhibition(true);
    	mem.setColumnDimensions(new int[] { 57, 31, 2 });
    	mem.initMatrices();
    	sp.updateInhibitionRadius(mem);
    	assertEquals(57, mem.getInhibitionRadius());
    	
    	// ((3 * 4) - 1) / 2 => round up
    	SpatialPooler mock = new SpatialPooler() {
    		public double avgConnectedSpanForColumnND(SpatialLattice l, int columnIndex) {
    			return 3;
    		}
    		
    		public double avgColumnsPerInput(SpatialLattice l) {
    			return 4;
    		}
    	};
    	mem.setGlobalInhibition(false);
    	sp = mock;
    	sp.updateInhibitionRadius(mem);
    	assertEquals(6, mem.getInhibitionRadius());
    	
    	//Test clipping at 1.0
    	mock = new SpatialPooler() {
    		public double avgConnectedSpanForColumnND(SpatialLattice l, int columnIndex) {
    			return 0.5;
    		}
    		
    		public double avgColumnsPerInput(SpatialLattice l) {
    			return 1.2;
    		}
    	};
    	mem.setGlobalInhibition(false);
    	sp = mock;
    	sp.updateInhibitionRadius(mem);
    	assertEquals(1, mem.getInhibitionRadius());
    	
    	//Test rounding up
    	mock = new SpatialPooler() {
    		public double avgConnectedSpanForColumnND(SpatialLattice l, int columnIndex) {
    			return 2.4;
    		}
    		
    		public double avgColumnsPerInput(SpatialLattice l) {
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
    	defaultSetup();
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
        defaultSetup();
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
        
        defaultSetup();
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
        
        defaultSetup();
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
        
        defaultSetup();
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
        defaultSetup();
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
        defaultSetup();
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
        defaultSetup();
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
        defaultSetup();
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
        defaultSetup();
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
        defaultSetup();
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
        defaultSetup();
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
        defaultSetup();
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

    /**
     * Checks that feeding in the same input vector leads to polarized
     * permanence values: either zeros or ones, but no fractions
     */
    @Test
    public void testCompute1() {
        defaultSetup();
        parameters.setPotentialRadius(5);
        initSP();
    }

}
