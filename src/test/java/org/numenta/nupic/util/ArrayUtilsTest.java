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

package org.numenta.nupic.util;

import gnu.trove.list.TIntList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArrayUtilsTest {

    @Test
    public void testRecursiveCoordinatesAssemble() {
        /*Create huge 4 dimensional matrix*/
        int dimSize = 10, dimNumber = 4;
        int[] dimCoordinates = new int[dimSize];
        List<int[]> dimensions = new ArrayList<int[]>();
        for (int i = 0; i < dimNumber; i++) {
            for (int j = 0; j < dimSize; j++) {
                dimCoordinates[j] = j;
            }
            dimensions.add(dimCoordinates);
        }
        List<TIntList> neighborList = ArrayUtils.dimensionsToCoordinateList(dimensions);
        /*
        *If uncomment below - will fail with StackOverflowError
        */
        //List<TIntList> neighborList = ArrayUtils.dimensionsToCoordinateListOld(dimensions);
        System.out.print("neighborList:" + neighborList);

    }

    /**
     * Python does modulus operations differently than the rest of the world
     * (C++ or Java) so...
     */
    @Test
    public void testModulo() {
        int a = -7;
        int n = 5;
        assertEquals(3, ArrayUtils.modulo(a, n));

        //Example A
        a = 5;
        n = 2;
        assertEquals(1, ArrayUtils.modulo(a, n));

        //Example B
        a = 5;
        n = 3;
        assertEquals(2, ArrayUtils.modulo(a, n));

        //Example C
        a = 10;
        n = 3;
        assertEquals(1, ArrayUtils.modulo(a, n));

        //Example D
        a = 9;
        n = 3;
        assertEquals(0, ArrayUtils.modulo(a, n));

        //Example E
        a = 3;
        n = 0;
        try {
            assertEquals(3, ArrayUtils.modulo(a, n));
            fail();
        } catch (Exception e) {
            assertEquals("Division by Zero!", e.getMessage());
        }

        //Example F
        a = 2;
        n = 10;
        assertEquals(2, ArrayUtils.modulo(a, n));
    }

    @Test
    public void testAnd() {
        int[] a = new int[]{0, 0, 0, 0, 1, 1, 1};
        int[] b = new int[]{0, 0, 0, 0, 1, 1, 1};
        int[] result = ArrayUtils.and(a, b);
        assertTrue(Arrays.equals(a, result));

        a = new int[]{0, 0, 0, 0, 1, 0, 1};
        result = ArrayUtils.and(a, b);
        assertTrue(Arrays.equals(a, result));

        a = new int[]{0, 0, 0, 0, 0, 0, 0};
        result = ArrayUtils.and(a, b);
        assertTrue(Arrays.equals(a, result));

        a = new int[]{1, 1, 1, 1, 0, 0, 0};
        int[] expected = new int[]{0, 0, 0, 0, 0, 0, 0};
        result = ArrayUtils.and(a, b);
        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testBitsToString() {
        String expected = "c....***";
        String result = ArrayUtils.bitsToString(new int[]{0, 0, 0, 0, 1, 1, 1});
        assertEquals(expected, result);
    }

}
