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

import org.junit.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SparseBinaryMatrixTest {

    @Test
    public void testBackingStoreAndSliceAccess() {
        int[] dimensions = new int[]{5, 10};
        int[][] connectedSynapses = new int[][]{
                {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
                {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
                {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
                {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};
        SparseBinaryMatrix sm = new SparseBinaryMatrix(dimensions);
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
            assertEquals("This method only returns the array holding the specified index: " +
                         Arrays.toString(new int[]{0, 4}), e.getMessage());
        }
    }

    @Test
    public void testRightVecSumAtNZFast() {
        int[] dimensions = new int[]{5, 10};
        int[][] connectedSynapses = new int[][]{
                {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
                {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
                {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
                {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};
        SparseBinaryMatrix sm = new SparseBinaryMatrix(dimensions);
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
        int[] dimensions = new int[]{5, 10};
        int[][] connectedSynapses = new int[][]{
                {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
                {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
                {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
                {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};
        SparseBinaryMatrix sm = new SparseBinaryMatrix(dimensions);
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
        SparseBinaryMatrix sm = new SparseBinaryMatrix(dimensions);
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


}
