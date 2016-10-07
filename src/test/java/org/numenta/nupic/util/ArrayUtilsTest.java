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

import static org.junit.Assert.assertArrayEquals;
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
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;

public class ArrayUtilsTest {
    
    @Test
    public void testToBytes() {
        boolean[] ba = { true, true, };
        byte[] bytes = ArrayUtils.toBytes(ba);
        assertEquals(0, bytes.length);
        
        // 8 positions -> binary 1
        ba = new boolean[] { false, false, false, false, false, false, false, true };
        bytes = ArrayUtils.toBytes(ba);
        assertEquals(1, bytes.length);
        assertEquals(1, bytes[0]);
        
        // 8 positions -> binary 3
        ba = new boolean[] { false, false, false, false, false, false, true, true };
        bytes = ArrayUtils.toBytes(ba);
        assertEquals(1, bytes.length);
        assertEquals(3, bytes[0]);
        
        // 9 positions -> squeezes last bit out
        ba = new boolean[] { false, false, false, false, false, false, false, true, true };
        bytes = ArrayUtils.toBytes(ba);
        assertEquals(1, bytes.length);
        assertEquals(1, bytes[0]);
        
        // 10 positions -> squeeze last to bits out
        ba = new boolean[] { false, false, false, false, false, false, false, false, true, true };
        bytes = ArrayUtils.toBytes(ba);
        assertEquals(1, bytes.length);
        assertEquals(0, bytes[0]);
        
        // 16 positions -> enough for two bytes, array length increases to 2
        ba = new boolean[] { false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true };
        bytes = ArrayUtils.toBytes(ba);
        assertEquals(2, bytes.length);
        assertEquals(0, bytes[0]);
        assertEquals(3, bytes[1]);
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
    }
    
    @Test
    public void testShape() {
        int[][] inputPattern = { { 2, 3, 4, 5 }, { 6, 7, 8, 9} };
        int[] shape = ArrayUtils.shape(inputPattern);
        assertTrue(Arrays.equals(new int[] { 2, 4 }, shape));
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
    public void testTo1d() {
        double[][] da = new double[][] { { 1., 1.}, {2., 2.}};
        double[] expected = new double[] { 1., 1., 2., 2. };
        assertTrue(Arrays.equals(expected, ArrayUtils.to1D(da)));
        
        int[][] ia = new int[][] { { 1, 1 }, { 2, 2 } };
        int[] expectedia = new int[] { 1, 1, 2, 2 };
        assertTrue(Arrays.equals(expectedia, ArrayUtils.to1D(ia)));
    }
    
    @Test
    public void testZip_ArrayOfLists() {
        Cell cell0 = new Cell(new Column(1, 0), 0);
        Cell cell1 = new Cell(new Column(1, 1), 1);
        List<?> o1 = Arrays.asList(new Cell[] { cell0, cell1 });
        List<?> o2 = Arrays.asList(new Integer[] { new Integer(1), new Integer(2) });
        List<?> o3 = Arrays.asList(new Double[] { 2.3, 4.5 });
        
        List<Tuple> zipped = ArrayUtils.zip(o1, o2, o3);
        assertEquals(2, zipped.size());
        assertTrue(zipped.get(0).get(0) instanceof Cell && zipped.get(0).get(1) instanceof Integer);
        assertTrue(zipped.get(0).get(0).equals(cell0) && zipped.get(0).get(1).equals(new Integer(1)) && zipped.get(0).get(2).equals(new Double(2.3)));
        assertTrue(zipped.get(1).get(0).equals(cell1) && zipped.get(1).get(1).equals(new Integer(2)) && zipped.get(1).get(2).equals(new Double(4.5)));
        
        // Negative tests
        assertFalse(zipped.get(0).get(0).equals(cell0) && zipped.get(0).get(1).equals(new Integer(2))); // Bad Integer
        assertFalse(zipped.get(1).get(0).equals(cell0) && zipped.get(1).get(1).equals(new Integer(2))); // Bad Cell
    }
    
    @Test
    public void testZip_ArrayOfIntArrays() {
        int[] o1 = { 3, 4 };
        int[] o2 = { 5, 5 };
        int[] o3 = { -1, 7 };
        
        List<Tuple> zipped = ArrayUtils.zip(o1, o2, o3);
        assertEquals(2, zipped.size());
        assertTrue(Arrays.equals((int[])zipped.get(0).all().stream().mapToInt(i -> (int)i).toArray(), new int[] { 3, 5, -1 }));
        assertTrue(Arrays.equals((int[])zipped.get(1).all().stream().mapToInt(i -> (int)i).toArray(), new int[] { 4, 5, 7 }));
    }
    
    @Test
    public void testZip() {
        Cell cell0 = new Cell(new Column(1, 0), 0);
        Cell cell1 = new Cell(new Column(1, 1), 1);
        Object[] o1 = new Object[] { cell0, cell1 };
        Object[] o2 = new Object[] { new Integer(1), new Integer(2) };
        
        List<Tuple> zipped = ArrayUtils.zip(o1, o2);
        assertEquals(2, zipped.size());
        assertTrue(zipped.get(0).get(0) instanceof Cell && zipped.get(0).get(1) instanceof Integer);
        assertTrue(zipped.get(0).get(0).equals(cell0) && zipped.get(0).get(1).equals(new Integer(1)));
        assertTrue(zipped.get(1).get(0).equals(cell1) && zipped.get(1).get(1).equals(new Integer(2)));
        
        // Negative tests
        assertFalse(zipped.get(0).get(0).equals(cell0) && zipped.get(0).get(1).equals(new Integer(2))); // Bad Integer
        assertFalse(zipped.get(1).get(0).equals(cell0) && zipped.get(1).get(1).equals(new Integer(2))); // Bad Cell
    }
    
    @Test
    public void testMaximum() {
        double value = 6.7;
        double[] input = new double[] { 3.2, 6.8 };
        double[] expected = new double[] { 6.7, 6.8 };
        
        double[] result = ArrayUtils.maximum(input, value);
        assertTrue(Arrays.equals(expected, result));
    }
    
    @Test
    public void testRoundDivide() {
        assertEquals(2.35, 4.7 / 2, 0.01);
        assertEquals(2.45, 4.9 / 2, 0.01);
        assertEquals(2.70, 5.4 / 2, 0.01);
        
        double[] inputDividend = new double[] { 4.7, 4.9, 5.4 };
        double[] inputDivisor = new double[] { 2., 2., 2. };
        
        double[] expected0 = new double[] { 2.4, 2.5, 2.7 };
        double[] expected1 = new double[] { 2.0, 2.0, 3.0 };
        
        double[] result = ArrayUtils.roundDivide(inputDividend, inputDivisor, 2);
        assertTrue(Arrays.equals(expected0, result));
        
        double[] result2 = ArrayUtils.roundDivide(inputDividend, inputDivisor, 1);
        assertTrue(Arrays.equals(expected1, result2));
    }
    
    @Test
    public void testSubtract() {
        // minuend - subtrahend = difference
        List<Integer> minuend = Arrays.asList(new Integer[] { 2, 2, 2 });
        List<Integer> subtrahend = Arrays.asList(new Integer[] { 0, 1, 2 });
        List<Integer> difference = Arrays.asList(new Integer[] { 2, 1, 0 });
        
        List<Integer> result = ArrayUtils.subtract(subtrahend, minuend);
        assertEquals(difference, result);
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

    @Test
    public void testGreaterThanXFunctions() {
        int[] overlaps = new int[] { 0, 1, 0, 0, 3, 0, 6, 7, 1, 2, 0};
        double[] overlapArray = new double[overlaps.length];
        ArrayUtils.greaterThanXThanSetToYInB(overlaps, overlapArray, 0, 1);
        assertArrayEquals(new double[] {  0, 1, 0, 0, 1, 0, 1, 1, 1, 1, 0 }, overlapArray, 0.0001);

        int[] arrayInt = new int[] { 0, 1, 0, 0, 3, 0, 6, 7, 1, 2, 0};
        ArrayUtils.greaterThanXThanSetToY(arrayInt, 0, 1);
        assertArrayEquals(new int[] { 0, 1, 0, 0, 1, 0, 1, 1, 1, 1, 0}, arrayInt);

        double[] arrayDouble = new double[] { 0, 5, 3, 0, 2, 0, 0, 7, 1, 2, 0};
        ArrayUtils.greaterThanXThanSetToY(arrayDouble, 0, 2);
        assertArrayEquals(new double[] { 0, 2, 2, 0, 2, 0, 0, 2, 2, 2, 0}, arrayDouble, 0.0001);
    }


    @Test
    public void testArgmax()
    {
        int[] iarray = new int[] { 0, 1, 0, 0, 3, 0, 6, 7, 1, 2, 0};
        double[] darray = new double[] { 0, 1, 10, 0, 3, 0, 6, 7, 1, 2, 0};
        assertEquals(ArrayUtils.argmax(iarray), 7 );
        assertEquals(ArrayUtils.argmax(darray), 2 );
    }


}
