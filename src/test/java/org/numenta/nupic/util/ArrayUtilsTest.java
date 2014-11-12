package org.numenta.nupic.util;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class ArrayUtilsTest {

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

}
