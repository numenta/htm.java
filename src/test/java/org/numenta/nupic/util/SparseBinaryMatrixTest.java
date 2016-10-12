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

package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.model.Connections;

public class SparseBinaryMatrixTest {

    private int[] dimensions = new int[]{5, 10};

    @Test 
    public void testBackingStoreAndSliceAccess() {
        doTestBackingStoreAndSliceAccess(new SparseBinaryMatrix(this.dimensions));
        doTestBackingStoreAndSliceAccess(new LowMemorySparseBinaryMatrix(this.dimensions));
        doTestBackingStoreAndSliceAccess(new FastConnectionsMatrix(this.dimensions));
    }

    private void doTestBackingStoreAndSliceAccess(AbstractSparseBinaryMatrix sm) {
        int[][] connectedSynapses = new int[][]{
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};

            for (int i = 0; i < sm.getDimensions()[0]; i++) {
                for (int j = 0; j < sm.getDimensions()[1]; j++) {
                    sm.set(connectedSynapses[i][j], i, j);
                }
            }

            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 10; j++) {
                    assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
                }
            }

            for (int i = 0; i < connectedSynapses.length; i++) {
                for (int j = 0; j < connectedSynapses[i].length; j++) {
                    assertEquals(connectedSynapses[i][j], ((int[])sm.getSlice(i))[j], 0);
                }
            }

            //Make sure warning is proper for exact access
            try {
                sm.getSlice(0, 4);
                fail();
            } catch (Exception e) {
                assertEquals("This method only returns the array holding the specified maximum index: " +
                    Arrays.toString(sm.getDimensions()), e.getMessage());
            }
    }

    @Test
    public void testRightVecSumAtNZFast() {
        doTestRightVecSumAtNZFast(new SparseBinaryMatrix(this.dimensions));
        doTestRightVecSumAtNZFast(new LowMemorySparseBinaryMatrix(this.dimensions));
        doTestRightVecSumAtNZFast(new FastConnectionsMatrix(this.dimensions));
    }

    private void doTestRightVecSumAtNZFast(AbstractSparseBinaryMatrix sm) {
        int[] dimensions = new int[]{5, 10};
        int[][] connectedSynapses = new int[][]{
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};

        for (int i = 0; i < sm.getDimensions()[0]; i++) {
            for (int j = 0; j < sm.getDimensions()[1]; j++) {
                sm.set(connectedSynapses[i][j], i, j);
            }
        }

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
            }
        }

        int[] inputVector = new int[]{1, 0, 1, 0, 1, 0, 1, 0, 1, 0};
        int[] results = new int[5];
        int[] trueResults = new int[]{1, 1, 1, 1, 1};
        sm.rightVecSumAtNZ(inputVector, results);

        for (int i = 0; i < results.length; i++) {
            assertEquals(trueResults[i], results[i]);
        }

        ///////////////////////

        connectedSynapses = new int[][]{
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 1, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 1}};
            sm = new SparseBinaryMatrix(dimensions);
            for (int i = 0; i < sm.getDimensions()[0]; i++) {
                for (int j = 0; j < sm.getDimensions()[1]; j++) {
                    sm.set(connectedSynapses[i][j], i, j);
                }
            }

            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 10; j++) {
                    assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
                }
            }

            inputVector = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
            results = new int[5];
            trueResults = new int[]{10, 8, 6, 4, 2};
            sm.rightVecSumAtNZ(inputVector, results);

            for (int i = 0; i < results.length; i++) {
                assertEquals(trueResults[i], results[i]);
            }
    }

    @Test
    public void testSetTrueCount() {
        doTestSetTrueCount(new SparseBinaryMatrix(this.dimensions));
        doTestSetTrueCount(new LowMemorySparseBinaryMatrix(this.dimensions));
        doTestSetTrueCount(new FastConnectionsMatrix(this.dimensions));
    }

    private void doTestSetTrueCount(AbstractSparseBinaryMatrix sm) {
        int[][] connectedSynapses = new int[][]{
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};

            for (int i = 0; i < sm.getDimensions()[0]; i++) {
                for (int j = 0; j < sm.getDimensions()[1]; j++) {
                    sm.set(connectedSynapses[i][j], i, j);
                }
            }

            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 10; j++) {
                    assertEquals(connectedSynapses[i][j], sm.getIntValue(i, j));
                }
            }

            for (int i = 0; i < 5; i++) {
                assertEquals(2, sm.getTrueCount(i));
            }
    }


    public static void fillWithSomeRandomValues(Object array, Random r, int... sizes) {
        for (int i = 0; i < sizes[0]; i++)
            if (sizes.length == 1) {
                ((int[])array)[i] = r.nextInt(2);
            } else {
                fillWithSomeRandomValues(Array.get(array, i), r, ArrayUtils.tail(sizes));
            }
    }

    @Test
    public void testBackingStoreAndSliceAccessManyDimensions() {
        /*Create 3 dimensional matrix*/
        int[] dimensions = {5, 5, 5};
        doTestBackingStoreAndSliceAccessManyDimensions(new SparseBinaryMatrix(dimensions));
        doTestBackingStoreAndSliceAccessManyDimensions(new LowMemorySparseBinaryMatrix(dimensions));
    }

    private void doTestBackingStoreAndSliceAccessManyDimensions(AbstractSparseBinaryMatrix sm) {
        /*set diagonal element to true*/
        sm.set(1, 0, 0, 0);
        sm.set(1, 1, 1, 1);
        sm.set(1, 2, 2, 2);
        sm.set(1, 3, 3, 3);
        sm.set(1, 4, 4, 4);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 5; k++) {
                    if (k == j & j == i) {
                        assertEquals(1, sm.getIntValue(i, j, k));
                    }
                }
            }
        }
        int[] slice = (int[])sm.getSlice(4, 4);
        for (int i = 0; i < 5; i++) {
            assertEquals(1, sm.getTrueCount(i));
        }
        System.out.println("slice:" + ArrayUtils.intArrayToString(slice));
        assertEquals(1, slice[4]);
        
        /*update first row to true, other to false*/
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 5; k++) {
                    if (0 == i) {
                        sm.set(1, i, j, k);
                    } else {
                        sm.set(0, i, j, k);
                    }
                }
            }
        }
        assertEquals(25, sm.getTrueCounts()[0]);
        assertEquals(0, sm.getTrueCounts()[1]);

    }

    @Test
    public void testArraySet() {
        int[] dimensions =  { 5, 2 };
        doTestArraySet(new SparseBinaryMatrix(dimensions));
        doTestArraySet(new LowMemorySparseBinaryMatrix(dimensions));
        doTestArraySet(new FastConnectionsMatrix(dimensions));
    }

    private void doTestArraySet(AbstractSparseBinaryMatrix sm) {
        int[] expected = { 1, 0, 0, 0, 1, 0, 1, 1, 1, 0 };
        int[] values = { 1, 1, 1, 1, 1 };
        int[] indexes = { 0, 4, 6, 7, 8 };
        sm.set(indexes, values);
        int[] dense = new int[sm.getMaxIndex() + 1];

        for (int i = 0; i < sm.getMaxIndex() + 1; i++) {
            dense[i] = sm.getIntValue(i);
        }

        assertArrayEquals(expected, dense);
    }
    
    @Test
    public void testGetSparseIndices() {
        doTestGetSparseIndices(new SparseBinaryMatrix(this.dimensions));
        doTestGetSparseIndices(new LowMemorySparseBinaryMatrix(this.dimensions));
        doTestGetSparseIndices(new FastConnectionsMatrix(this.dimensions));
    }
    
    private void doTestGetSparseIndices(AbstractSparseBinaryMatrix sm) {
        int[] expected = {0, 5, 11, 16, 22, 27, 33, 38, 44, 49};
        int[][] values = new int[][]{
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                sm.set(values[i][j], new int[] {i, j});
            }
        }
        
        int[] sdr = sm.getSparseIndices();
        assertArrayEquals(expected, sdr);
    }
    
    @Test
    public void testSliceIndexes() {
        SparseBinaryMatrix sm = new SparseBinaryMatrix(this.dimensions);
        int[][] expected =  {
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, 
                {10, 11, 12, 13, 14, 15, 16, 17, 18, 19}, 
                {20, 21, 22, 23, 24, 25, 26, 27, 28, 29}, 
                {30, 31, 32, 33, 34, 35, 36, 37, 38, 39}, 
                {40, 41, 42, 43, 44, 45, 46, 47, 48, 49}};
        
       for (int i = 0; i < this.dimensions[0]; i++)
           assertArrayEquals(expected[i], sm.getSliceIndexes(new int[] {i}));
    }
    
    @Test
    public void testOr() {
        SparseBinaryMatrix sm = createDefaultMatrix();
        int[] orBits = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        int[] expected = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        sm.or(orBits);
        assertArrayEquals(expected, (int[]) sm.getSlice(new int[] {0}));
    }
    
    @Test
    public void testAll() {
        SparseBinaryMatrix sm = createDefaultMatrix();
        int[] all = {0, 5, 11, 16, 22, 27, 33, 38, 44, 49};
        assertTrue(sm.all(all));
    }
    
    private SparseBinaryMatrix createDefaultMatrix() {
        SparseBinaryMatrix sm  = new SparseBinaryMatrix(this.dimensions);
        int[][] values = {
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};
        
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                sm.set(values[i][j], new int[] {i, j});
            }
        }
        
        return sm;
    }
    
    @Test
    public void testCalculateOverlap() {
        doTestCalculateOverlap(new SparseBinaryMatrix(this.dimensions));
        doTestCalculateOverlap(new LowMemorySparseBinaryMatrix(this.dimensions));
        doTestCalculateOverlap(new FastConnectionsMatrix(this.dimensions));
    }
    
    private void doTestCalculateOverlap(AbstractSparseBinaryMatrix sm) {
        setupParameters();
        parameters.setInputDimensions(new int[] { 10 });
        parameters.setColumnDimensions(new int[] { 5 });
        initSP();
        
        ///////////////////
        // test stimulsThreshold = 2
        parameters.set(KEY.STIMULUS_THRESHOLD, 3.0);
        initSP();
        
        int[][] connectedSynapses = new int[][] {
          {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
          {0, 0, 1, 1, 1, 1, 1, 1, 1, 1},
          {0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
          {0, 0, 0, 0, 0, 0, 1, 1, 1, 1},
          {0, 0, 0, 0, 0, 0, 0, 0, 1, 1}};
        
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
        
        int[] inputVector = new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
        int[] overlaps = sp.calculateOverlap(mem, inputVector);
        int[] trueOverlaps = new int[] { 10, 8, 6, 4, 0 }; // last gets squelched by stimulus threshold of 3
        double[] overlapsPct = sp.calculateOverlapPct(mem, overlaps);
        double[] trueOverlapsPct = new double[] { 1, 1, 1, 1, 0 };
        assertTrue(Arrays.equals(trueOverlaps, overlaps));
        assertTrue(Arrays.equals(trueOverlapsPct, overlapsPct));
    }
    
    private Parameters parameters;
    private SpatialPooler sp;
    private Connections mem;
    
    public void setupParameters() {
        parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 5 });//5
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 5 });//5
        parameters.set(KEY.POTENTIAL_RADIUS, 3);//3
        parameters.set(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 3.0);
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
    }

    private void initSP() {
        sp = new SpatialPooler();
        mem = new Connections();
        parameters.apply(mem);
        sp.init(mem);
    }
}
