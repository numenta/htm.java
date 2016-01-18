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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ArrayUtilsTest {
    
    @Test
    public void testAdd() {
        int[] ia = { 1, 1, 1, 1 };
        int[] expected = { 2, 2, 2, 2};
        assertTrue(Arrays.equals(expected, ArrayUtils.add(ia, 1)));
        
        // add one array to another
        expected = new int[] { 4, 4, 4, 4 };
        assertTrue(Arrays.equals(expected, ArrayUtils.add(ia, ia)));
        
        ///////// double version //////////
        double[] da = { 1., 1., 1., 1. };
        double[] d_expected = { 2., 2., 2., 2.};
        assertTrue(Arrays.equals(d_expected, ArrayUtils.d_add(da, 1.)));
        
        // add one array to another
        d_expected = new double[] { 4., 4., 4., 4. };
        assertTrue(Arrays.equals(d_expected, ArrayUtils.d_add(da, da)));
    }
    
    @Test
    public void testDSubtract() {
        double[] da = { 2., 2., 2., 2. };
        double[] d_expected = { 1.5, 1.5, 1.5, 1.5};
        assertTrue(Arrays.equals(d_expected, ArrayUtils.d_sub(da, 0.5)));
        
        da = new double[] { 2., 2., 2., 2. };
        double[] sa = new double[] { 1., 1., 1., 1. };
        assertTrue(Arrays.equals(sa, ArrayUtils.d_sub(da, sa)));
    }
    
    @Test
    public void testTranspose_int() {
        int[][] a = { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } };
        int[][] expected = { { 1, 5 }, { 2, 6, }, { 3, 7, }, { 4, 8 } };
        
        int[][] result = ArrayUtils.transpose(a);
        for(int i = 0;i < expected.length;i++) {
            for(int j = 0;j < expected[i].length;j++) {
                assertEquals(expected[i][j], result[i][j]);
            }
        }
        
        int[][] zero = { {} };
        expected = new int[0][0];
        result = ArrayUtils.transpose(zero);
        assertEquals(expected.length, result.length);
        assertEquals(0, result.length);
    }
    
    @Test
    public void testTranspose_double() {
        double[][] a = { { 1, 2, 3, 4 }, { 5, 6, 7, 8 } };
        double[][] expected = { { 1, 5 }, { 2, 6, }, { 3, 7, }, { 4, 8 } };
        
        double[][] result = ArrayUtils.transpose(a);
        for(int i = 0;i < expected.length;i++) {
            for(int j = 0;j < expected[i].length;j++) {
                assertEquals(expected[i][j], result[i][j], 0.);
            }
        }
        
        double[][] zero = { {} };
        expected = new double[0][0];
        result = ArrayUtils.transpose(zero);
        assertEquals(expected.length, result.length);
        assertEquals(0, result.length);
    }
    
    @Test
    public void testDot_int() {
        int[][] a = new int[][] { { 1, 2 }, { 3, 4 } };
        int[][] b = new int[][] { { 1, 1 }, { 1, 1 } };
        
        int[][] c = ArrayUtils.dot(a, b);
        
        assertEquals(3, c[0][0]);
        assertEquals(3, c[0][1]);
        assertEquals(7, c[1][0]);
        assertEquals(7, c[1][1]);
        
        // Single dimension
        int[][] x = new int[][] { { 2, 2, 2 } };
        b = new int[][] { { 3 }, { 3 }, { 3 } };
        
        c = ArrayUtils.dot(x, b);
        
        assertTrue(c.length == 1);
        assertTrue(c[0].length == 1);
        assertEquals(c[0][0], 18);
        
        
        // Ensure un-aligned dimensions get reported
        b = new int[][] { { 0, 0 }, { 0, 0 }, { 0, 0 } };
        try {
            ArrayUtils.dot(a, b);
            fail();
        }catch(Exception e) {
            assertTrue(e.getClass().equals(IllegalArgumentException.class));
            assertEquals("Matrix inner dimensions must agree.", e.getMessage());
        }
        
        // Test 2D.1d
        a = new int[][] { { 1, 2 }, { 3, 4 } };
        int[] b2 = new int[] { 2, 2 };
        int[] result = ArrayUtils.dot(a, b2);
        assertTrue(Arrays.equals(new int[] { 6, 14 }, result));
    }
    
    @Test
    public void testDot_double() {
        double[][] a = new double[][] { { 1., 2. }, { 3., 4. } };
        double[][] b = new double[][] { { 1., 1. }, { 1., 1. } };
        
        double[][] c = ArrayUtils.dot(a, b);
        
        assertEquals(3, c[0][0], 0.);
        assertEquals(3, c[0][1], 0.);
        assertEquals(7, c[1][0], 0.);
        assertEquals(7, c[1][1], 0.);
        
        // Single dimension
        double[][] x = new double[][] { { 2., 2., 2. } };
        b = new double[][] { { 3. }, { 3. }, { 3. } };
        
        c = ArrayUtils.dot(x, b);
        
        assertTrue(c.length == 1);
        assertTrue(c[0].length == 1);
        assertEquals(c[0][0], 18., 0.);
        
        
        // Ensure un-aligned dimensions get reported
        b = new double[][] { { 0., 0. }, { 0., 0. }, { 0., 0. } };
        try {
            ArrayUtils.dot(a, b);
            fail();
        }catch(Exception e) {
            assertTrue(e.getClass().equals(IllegalArgumentException.class));
            assertEquals("Matrix inner dimensions must agree.", e.getMessage());
        }
        
        // Test 2D.1d
        a = new double[][] { { 1., 2. }, { 3., 4. } };
        double[] b2 = new double[] { 2., 2. };
        double[] result = ArrayUtils.dot(a, b2);
        assertTrue(Arrays.equals(new double[] { 6., 14. }, result));
    }
    
    @Test
    public void testZip() {
        int[] t1 = { 1, 2, 3 };
        int[] t2 = { 4, 5, 6 };
        List<Tuple> tuples = ArrayUtils.zip(new int[][] { t1, t2 });
        assertEquals(3, tuples.size());
        assertTrue(
            ((Integer)tuples.get(0).get(0)) == 1 &&
            ((Integer)tuples.get(0).get(1)) == 4 &&
            ((Integer)tuples.get(1).get(0)) == 2 &&
            ((Integer)tuples.get(1).get(1)) == 5 &&
            ((Integer)tuples.get(2).get(0)) == 3 &&
            ((Integer)tuples.get(2).get(1)) == 6);
    }
    
    @Test
    public void testTo1D() {
        int[][] test = { { 1, 2 }, { 3, 4 } };
        int[] expected = { 1, 2, 3, 4 };
        int[] result = ArrayUtils.to1D(test);
        assertTrue(Arrays.equals(expected, result));
        
        // Test double version
        double[][] d_test = { { 1., 2. }, { 3., 4. } };
        double[] d_expected = { 1., 2., 3., 4. };
        double[] d_result = ArrayUtils.to1D(d_test);
        assertTrue(Arrays.equals(d_expected, d_result));
    }
    
    @Test
    public void testFromCoordinate() {
        int[] shape = { 2, 2 };
        int[] testCoord = { 1, 1 };
        int result = ArrayUtils.fromCoordinate(testCoord, shape);
        assertEquals(3, result);
    }
    
    @Test 
    public void testReshape() {
        int[][] test = {
            { 0, 1, 2, 3, 4, 5 },
            { 6, 7, 8, 9, 10, 11 }
        };
        
        int[][] expected = {
            { 0, 1, 2 },
            { 3, 4, 5 },
            { 6, 7, 8 },
            { 9, 10, 11 }
        };
        
        int[][] result = ArrayUtils.reshape(test, 3);
        for(int i = 0;i < result.length;i++) {
            for(int j = 0;j < result[i].length;j++) {
                assertEquals(expected[i][j], result[i][j]);
            }
        }
        
        // Unhappy case
        try {
            ArrayUtils.reshape(test, 5);
        }catch(Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals("12 is not evenly divisible by 5", e.getMessage());
        }
        
        // Test zero-length case
        int[] result4 = ArrayUtils.unravel(new int[0][]);
        assertNotNull(result4);
        assertTrue(result4.length == 0);
        
        // Test empty array arg
        test = new int[][]{};
        expected = new int[0][0];
        result = ArrayUtils.reshape(test, 1);
        assertTrue(Arrays.equals(expected, result));
    }
    
    @Test
    public void testRavelAndUnRavel() {
        int[] test = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
        int[][] expected = {
            { 0, 1, 2, 3, 4, 5 },
            { 6, 7, 8, 9, 10, 11 }
        };
        
        int[][] result = ArrayUtils.ravel(test, 6);
        for(int i = 0;i < result.length;i++) {
            for(int j = 0;j < result[i].length;j++) {
                assertEquals(expected[i][j], result[i][j]);
            }
        }
        
        int[] result2 = ArrayUtils.unravel(result);
        for(int i = 0;i < result2.length;i++) {
            assertEquals(test[i], result2[i]);
        }
        
        // Unhappy case
        try {
            ArrayUtils.ravel(test, 5);
        }catch(Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals("12 is not evenly divisible by 5", e.getMessage());
        }
        
        // Test zero-length case
        int[] result4 = ArrayUtils.unravel(new int[0][]);
        assertNotNull(result4);
        assertTrue(result4.length == 0);
    }
    
    @Test
    public void testRotateRight() {
        int[][] test = new int[][] {
            { 1, 0, 1, 0 },
            { 1, 0, 1, 0 },
            { 1, 0, 1, 0 },
            { 1, 0, 1, 0 }
        };
        
        int[][] expected = new int[][] {
            { 1, 1, 1, 1 },
            { 0, 0, 0, 0 },
            { 1, 1, 1, 1 },
            { 0, 0, 0, 0 }            
        };
        
        int[][] result = ArrayUtils.rotateRight(test);
        for(int i = 0;i < result.length;i++) {
            for(int j = 0;j < result[i].length;j++) {
                assertEquals(result[i][j], expected[i][j]);
            }
        }
        
        // Test empty array arg
        test = new int[][]{};
        expected = new int[0][0];
        result = ArrayUtils.rotateRight(test);
        assertTrue(Arrays.equals(expected, result));
    }
    
    @Test
    public void testRotateLeft() {
        int[][] test = new int[][] {
            { 1, 0, 1, 0 },
            { 1, 0, 1, 0 },
            { 1, 0, 1, 0 },
            { 1, 0, 1, 0 }
        };
        
        int[][] expected = new int[][] {
            { 0, 0, 0, 0 },
            { 1, 1, 1, 1 },
            { 0, 0, 0, 0 },
            { 1, 1, 1, 1 }
        };
        
        int[][] result = ArrayUtils.rotateLeft(test);
        for(int i = 0;i < result.length;i++) {
            for(int j = 0;j < result[i].length;j++) {
                assertEquals(result[i][j], expected[i][j]);
            }
        }
        
        // Test empty array arg
        test = new int[][]{};
        expected = new int[0][0];
        result = ArrayUtils.rotateLeft(test);
        assertTrue(Arrays.equals(expected, result));
    }
    
    @Test
    public void testSubst() {
        int[] original = new int[] { 30, 30, 30, 30, 30 };
        int[] substitutes = new int[] { 0, 1, 2, 3, 4 };
        int[] substInds = new int[] { 4, 1, 3 };
        
        int[] expected = { 30, 1, 30, 3, 4 };
        
        assertTrue(Arrays.equals(expected, ArrayUtils.subst(original, substitutes, substInds)));
    }
    
    @Test
    public void testSubst_doubles() {
        double[] original = new double[] { 30, 30, 30, 30, 30 };
        double[] substitutes = new double[] { 0, 1, 2, 3, 4 };
        int[] substInds = new int[] { 4, 1, 3 };
        
        double[] expected = { 30, 1, 30, 3, 4 };
        
        assertTrue(Arrays.equals(expected, ArrayUtils.subst(original, substitutes, substInds)));
    }
    
    @Test
    public void testMaxIndex() {
        int max = ArrayUtils.maxIndex(new int[] { 2, 4, 5 });
        assertEquals(39, max);
    }
    
    @Test
    public void testToCoordinates() {
        int[] coords = ArrayUtils.toCoordinates(19, new int[] { 2, 4, 5 }, false);
        assertTrue(Arrays.equals(new int[] { 0, 3, 4 }, coords));
        
        coords = ArrayUtils.toCoordinates(19, new int[] { 2, 4, 5 }, true);
        assertTrue(Arrays.equals(new int[] { 4, 3, 0 }, coords));
    }
    
    @Test
    public void testArgsort() {
        int[] args = ArrayUtils.argsort(new int[] { 11, 2, 3, 7, 0 });
        assertTrue(Arrays.equals(new int[] {4, 1, 2, 3, 0}, args));
        
        args = ArrayUtils.argsort(new int[] { 11, 2, 3, 7, 0 }, -1, -1);
        assertTrue(Arrays.equals(new int[] {4, 1, 2, 3, 0}, args));
        
        args = ArrayUtils.argsort(new int[] { 11, 2, 3, 7, 0 }, 0, 3);
        assertTrue(Arrays.equals(new int[] {4, 1, 2}, args));
        
        // Test double version
        int[] d_args = ArrayUtils.argsort(new double[] { 11, 2, 3, 7, 0 }, 0, 3);
        assertTrue(Arrays.equals(new int[] {4, 1, 2}, d_args));
        
        d_args = ArrayUtils.argsort(new double[] { 11, 2, 3, 7, 0 }, -1, 3);
        assertTrue(Arrays.equals(new int[] {4, 1, 2, 3, 0}, d_args));
    }
    
    @Test
    public void testShape() {
        int[][] inputPattern = { { 2, 3, 4, 5 }, { 6, 7, 8, 9} };
        int[] shape = ArrayUtils.shape(inputPattern);
        assertTrue(Arrays.equals(new int[] { 2, 4 }, shape));
    }
    
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
    }

    private Object createMultiDimensionArray(int[] sizes){
        return Array.newInstance(int.class, sizes);
    }

	@Test
	public void testConcatAll() {
		assertTrue(Arrays.equals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0},
			ArrayUtils.concatAll(new int[]{1, 2}, new int[]{3, 4, 5, 6, 7}, new int[]{8, 9, 0})));
	}
	
	@Test
	public void testReplace() {
	    assertTrue(Arrays.equals(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0},
	        ArrayUtils.replace(5, 10, new int[]{1, 2, 3, 4, 5, -1, -1, -1, -1, -1}, new int[] { 6, 7, 8, 9, 0})));
	        
	}
	
	@Test
	public void testIsSparse() {
	    int[] t = new int[] { 0, 1, 0 };
	    int[] t1 = new int[] { 4, 5, 6, 7 };
	    
	    assertFalse(ArrayUtils.isSparse(t));
	    assertTrue(ArrayUtils.isSparse(t1));
	}

	@Test
	public void testNGreatest() {
	    double[] overlaps = new double[] { 1, 2, 1, 4, 8, 3, 12, 5, 4, 1 };
	    assertTrue(Arrays.equals(new int[] { 6, 4, 7 }, ArrayUtils.nGreatest(overlaps, 3)));
	}
}
