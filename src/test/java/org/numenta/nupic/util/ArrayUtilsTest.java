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

import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArrayUtilsTest {
    
    @Test
    public void testConcat() {
        // Test happy path
        double[] one = new double[] { 1., 2., 3. };
        double[] two = new double[] { 4., 5., 6. };
        double[] retVal = ArrayUtils.concat(one, two);
        assertEquals(6, retVal.length);
        for(int i = 0;i < retVal.length;i++) {
            assertEquals(i + 1, retVal[i], 0);
        }
        
        // Test unequal sizes
        one = new double[] { 1., 2. };
        retVal = ArrayUtils.concat(one, two);
        assertEquals(5, retVal.length);
        for(int i = 0;i < retVal.length;i++) {
            if(i == 2) continue;
            assertEquals(i + 1, retVal[i > 2 ? i - 1 : i], 0);
        }
        
        one = new double[] { 1., 2., 3. };
        two = new double[] { 4., 5. };
        retVal = ArrayUtils.concat(one, two);
        assertEquals(5, retVal.length);
        for(int i = 0;i < retVal.length;i++) {
            assertEquals(i + 1, retVal[i], 0);
        }
        
        //Test zero length
        one = new double[0];
        two = new double[] { 4., 5., 6. };
        retVal = ArrayUtils.concat(one, two);
        assertEquals(3, retVal.length);
        for(int i = 0;i < retVal.length;i++) {
            assertEquals(i + 4, retVal[i], 0);
        }
        
        one = new double[] { 1., 2., 3. };
        two = new double[0];
        retVal = ArrayUtils.concat(one, two);
        assertEquals(3, retVal.length);
        for(int i = 0;i < retVal.length;i++) {
            assertEquals(i + 1, retVal[i], 0);
        }
        
    }
    
    @Test
    public void testInterleave() {
        String[] f = { "0" };
        double[] s = { 0.8 };
         
        // Test most simple interleave of equal length arrays
        Object[] result = ArrayUtils.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals(0.8, result[1]);
        
        // Test simple interleave of larger array
        f = new String[] { "0", "1" };
        s = new double[] { 0.42, 2.5 };
        result = ArrayUtils.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals(0.42, result[1]);
        assertEquals("1", result[2]);
        assertEquals(2.5, result[3]);
        
        // Test complex interleave of larger array
        f = new String[] { "0", "1", "bob", "harry", "digit", "temperature" };
        s = new double[] { 0.42, 2.5, .001, 1e-2, 34.0, .123 };
        result = ArrayUtils.interleave(f, s);
        for(int i = 0, j = 0;j < result.length;i++, j+=2) {
            assertEquals(f[i], result[j]);
            assertEquals(s[i], result[j + 1]);
        }
        
        // Test interleave with zero length of first
        f = new String[0];
        s = new double[] { 0.42, 2.5 };
        result = ArrayUtils.interleave(f, s);
        assertEquals(0.42, result[0]);
        assertEquals(2.5, result[1]);
        
        // Test interleave with zero length of second
        f = new String[] { "0", "1" };
        s = new double[0];
        result = ArrayUtils.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals("1", result[1]);
        
        // Test complex unequal length: left side smaller
        f = new String[] { "0", "1", "bob" };
        s = new double[] { 0.42, 2.5, .001, 1e-2, 34.0, .123 };
        result = ArrayUtils.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals(0.42, result[1]);
        assertEquals("1", result[2]);
        assertEquals(2.5, result[3]);
        assertEquals("bob", result[4]);
        assertEquals(.001, result[5]);
        assertEquals(1e-2, result[6]);
        assertEquals(34.0, result[7]);
        assertEquals(.123, result[8]);
        
        // Test complex unequal length: right side smaller
        f = new String[] { "0", "1", "bob", "harry", "digit", "temperature" };
        s = new double[] { 0.42, 2.5, .001 };
        result = ArrayUtils.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals(0.42, result[1]);
        assertEquals("1", result[2]);
        assertEquals(2.5, result[3]);
        assertEquals("bob", result[4]);
        assertEquals(.001, result[5]);
        assertEquals("harry", result[6]);
        assertEquals("digit", result[7]);
        assertEquals("temperature", result[8]);
        
        // Negative testing
        try {
            f = null;
            s = new double[] { 0.42, 2.5, .001 };
            result = ArrayUtils.interleave(f, s);
            fail();
        }catch(Exception e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
    }
    
    @Test
    public void testIn1d() {
        int[] ar1 = { 0, 1, 5, 9, 3, 1000 };
        int[] ar2 = Arrays.copyOf(ar1, ar1.length);
        assertTrue(Arrays.equals(ar1, ar2));
        int[] retVal = ArrayUtils.in1d(ar1, ar2);
        assertTrue(Arrays.equals(ar1, ArrayUtils.reverse(retVal)));
        
        ar1 = new int[] { 0, 2, 1000 };
        int[] expected = { 0, 1000 };
        assertTrue(Arrays.equals(expected, ArrayUtils.reverse(ArrayUtils.in1d(ar1, ar2))));
        
        ar1 = new int[] { 2, 6, 4 };
        expected = new int[0];
        assertTrue(Arrays.equals(expected, ArrayUtils.in1d(ar1, ar2)));
        
        // Test none in the second
        assertTrue(Arrays.equals(expected, ArrayUtils.in1d(ar1, expected)));
        // Test none in both
        assertTrue(Arrays.equals(expected, ArrayUtils.in1d(expected, expected)));
    }

    @Test
    public void testRecursiveCoordinatesAssemble() throws InterruptedException {
        /*Create huge 5 dimensional matrix*/
        int dimSize = 14, dimNumber = 5;
        int[] dimCoordinates = new int[dimSize];
        List<int[]> dimensions = new ArrayList<int[]>();
        for (int i = 0; i < dimNumber; i++) {
            for (int j = 0; j < dimSize; j++) {
                dimCoordinates[j] = j;
            }
            dimensions.add(dimCoordinates);
        }
        long startTime = System.currentTimeMillis();
        
        List<int[]> neighborList = ArrayUtils.dimensionsToCoordinateList(dimensions);
        long take = System.currentTimeMillis() - startTime;
        System.out.print("Execute in:" + take + " milliseconds");

        assertEquals(neighborList.size(), 537824);
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
		}catch(Exception e) {
			assertEquals("Division by Zero!", e.getMessage());
		}

		//Example F
		a = 2;
		n = 10;
		assertEquals(2, ArrayUtils.modulo(a, n));
	}
	
	@Test
	public void testAnd() {
		int[] a = new int[] { 0, 0, 0, 0, 1, 1, 1 };
		int[] b = new int[] { 0, 0, 0, 0, 1, 1, 1 };
		int[] result = ArrayUtils.and(a, b);
		assertTrue(Arrays.equals(a , result));
		
		a = new int[] { 0, 0, 0, 0, 1, 0, 1 };
		result = ArrayUtils.and(a, b);
		assertTrue(Arrays.equals(a, result));
		
		a = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		result = ArrayUtils.and(a, b);
		assertTrue(Arrays.equals(a, result));
		
		a = new int[] { 1, 1, 1, 1, 0, 0, 0 };
		int[] expected = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		result = ArrayUtils.and(a, b);
		assertTrue(Arrays.equals(expected, result));
	}
	
	@Test
	public void testBitsToString() {
		String expected = "c....***";
		String result = ArrayUtils.bitsToString(new int[] { 0, 0, 0, 0, 1, 1, 1 });
		assertEquals(expected, result);
	}
	
	@Test
	public void testDiff() {
		double[] t = new double[] { 5, 4, 3, 2, 1, 0 };
		double[] result = ArrayUtils.diff(t);
		assertEquals(5, result.length);
		assertTrue(Arrays.equals(new double[] { -1, -1, -1, -1, -1 }, result));
		assertEquals(-5, ArrayUtils.sum(result), 0);
	}

    @Test
   	public void testMultiDimensionArrayOperation() {
        int[] dimensions = {5, 5 ,5};
          Object multiDimArray = createMultiDimensionArray(dimensions);
          ArrayUtils.fillArray(multiDimArray, 1);
          assertEquals(125, ArrayUtils.aggregateArray(multiDimArray));
          System.out.println(ArrayUtils.intArrayToString(multiDimArray));
   	}

    private Object createMultiDimensionArray(int[] sizes){
            System.out.println("Creating array with dimensions / sizes: " +
                    Arrays.toString(sizes).replaceAll(", ", "]["));
            return Array.newInstance(int.class, sizes);
        }

	@Test
	public void testConcatAll() {
		assertTrue(Arrays.equals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0},
				ArrayUtils.concatAll(new int[]{1, 2}, new int[]{3, 4, 5, 6, 7}, new int[]{8, 9, 0})));
	}
}
