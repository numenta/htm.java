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

package org.numenta.nupic.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;

public class CoordinateEncoderTest {
	private CoordinateEncoder ce;
	private CoordinateEncoder.Builder builder;
	
	private boolean verbose;
	
	private void setUp() {
		builder = CoordinateEncoder.builder()
			.name("coordinate")
			.n(33)
			.w(3);
	}
	
	private void initCE() {
		ce = builder.build();
	}

	@Test
	public void testInvalidW() {
		setUp();
		initCE();
		
		// Even
		try {
			setUp();
			builder.n(45);
			builder.w(4);
			//Should fail here
			initCE();
			
			fail();
		}catch(Exception e) {
			assertEquals("w must be odd, and must be a positive integer", e.getMessage());
		}
		
		// 0
		try {
			setUp();
			builder.n(45);
			builder.w(0);
			//Should fail here
			initCE();
			
			fail();
		}catch(Exception e) {
			assertEquals("w must be odd, and must be a positive integer", e.getMessage());
		}
		
		// Negative
		try {
			setUp();
			builder.n(45);
			builder.w(-2);
			//Should fail here
			initCE();
			
			fail();
		}catch(Exception e) {
			assertEquals("w must be odd, and must be a positive integer", e.getMessage());
		}
	}
	
	@Test
	public void testInvalidN() {
		setUp();
		initCE();
		
		// Even
		try {
			setUp();
			builder.n(11);
			builder.w(3);
			//Should fail here
			initCE();
			
			fail();
		}catch(Exception e) {
			assertEquals("n must be an int strictly greater than 6*w. For " +
                "good results we recommend n be strictly greater than 11*w", e.getMessage());
		}
	}
	
	@Test
	public void testOrderForCoordinate() {
		CoordinateEncoder c = new CoordinateEncoder();
		double h1 = c.orderForCoordinate(new int[] { 2, 5, 10 });
		double h2 = c.orderForCoordinate(new int[] { 2, 5, 11 });
		double h3 = c.orderForCoordinate(new int[] { 2497477, -923478 });
		
		assertTrue(0 <= h1 && h1 < 1);
		assertTrue(0 <= h2 && h2 < 1);
		assertTrue(0 <= h3 && h3 < 1);
		
		System.out.println(h1 + ", " + h2 + ", " + h3);
		
		assertTrue(h1 != h2);
		assertTrue(h2 != h3);
	}
	
	@Test
	public void testBitForCoordinate() {
		int n = 1000;
		double b1 = CoordinateEncoder.bitForCoordinate(new int[] { 2, 5, 10 }, n);
		double b2 = CoordinateEncoder.bitForCoordinate(new int[] { 2, 5, 11 }, n);
		double b3 = CoordinateEncoder.bitForCoordinate(new int[] { 2497477, -923478 }, n);
		
		assertTrue(0 <= b1 && b1 < n);
		assertTrue(0 <= b2 && b2 < n);
		assertTrue(0 <= b3 && b3 < n);
		
		assertTrue(b1 != b2);
		assertTrue(b2 != b3);
		
		// Small n
		n = 2;
		double b4 = CoordinateEncoder.bitForCoordinate(new int[] { 5, 10 }, n);
		
		assertTrue(0 <= b4 && b4 < n);
	}
	
	@Test
	public void testTopWCoordinates() {
		final int[][] coordinates = new int[][] { { 1 }, { 2 }, { 3 }, { 4 }, { 5 } };
		
		CoordinateOrder mock = new CoordinateOrder() {
			@Override public double orderForCoordinate(int[] coordinate) {
				return  ArrayUtils.sum(coordinate) / 5.0d;
			}
			
		};
		
		int[][] top = new CoordinateEncoder().topWCoordinates(mock, coordinates, 2);
		assertEquals(2, top.length);
		assertTrue(Arrays.equals(new int[] { 4 } , top[0]));
		assertTrue(Arrays.equals(new int[] { 5 } , top[1]));
	}
	
	@Test
	public void testNeighbors1D() {
		CoordinateEncoder ce = new CoordinateEncoder();
		
		int[] coordinate = new int[] { 100 };
		int radius = 5;
		List<int[]> neighbors = ce.neighbors(coordinate, radius);
		assertEquals(11, neighbors.size());
		assertTrue(Arrays.equals(new int[] { 95 }, neighbors.get(0)));
		assertTrue(Arrays.equals(new int[] { 100 }, neighbors.get(5)));
		assertTrue(Arrays.equals(new int[] { 105 }, neighbors.get(10)));
	}
	
	@Test
	public void testNeighbors2D() {
		CoordinateEncoder ce = new CoordinateEncoder();
		
		int[] coordinate = new int[] { 100, 200 };
		int radius = 5;
		List<int[]> neighbors = ce.neighbors(coordinate, radius);
		assertEquals(121, neighbors.size());
		assertTrue(ArrayUtils.contains(new int[] { 95, 195 }, neighbors));
		assertTrue(ArrayUtils.contains(new int[] { 95, 205 }, neighbors));
		assertTrue(ArrayUtils.contains(new int[] { 100, 200 }, neighbors));
		assertTrue(ArrayUtils.contains(new int[] { 105, 195 }, neighbors));
		assertTrue(ArrayUtils.contains(new int[] { 105, 205 }, neighbors));
	}
	
	@Test
	public void testNeighbors0Radius() {
		CoordinateEncoder ce = new CoordinateEncoder();
		
		int[] coordinate = new int[] { 100, 200, 300 };
		int radius = 0;
		List<int[]> neighbors = ce.neighbors(coordinate, radius);
		assertEquals(1, neighbors.size());
		assertTrue(ArrayUtils.contains(new int[] { 100, 200, 300 }, neighbors));
	}
	
	@Test
	public void testEncodeIntoArray() {
		setUp();
		builder.n(33);
		builder.w(3);
		initCE();
		
		int[] coordinate = new int[] { 100, 200 };
		int[] output1 = encode(ce, coordinate, 5);
		assertEquals(ArrayUtils.sum(output1), ce.w);
		
		int[] output2 = encode(ce, coordinate, 5);
		assertTrue(Arrays.equals(output1, output2));
	}
	
	@Test
	public void testEncodeSaturateArea() {
		setUp();
		builder.n(1999);
		builder.w(25);
		builder.radius(2);
		initCE();
		
		int[] outputA = encode(ce, new int[] { 0, 0 }, 2);
		int[] outputB = encode(ce, new int[] { 0, 1 }, 2);
		
		assertEquals(0.8, overlap(outputA, outputB), 0.019);
	}
	
	/**
	 * As you get farther from a coordinate, the overlap should decrease
	 */
	@Test
	public void testEncodeRelativePositions() {
		// As you get farther from a coordinate, the overlap should decrease
		double[] overlaps = overlapsForRelativeAreas(999, 25, new int[] {100, 200}, 10, 
			new int[] {2, 2}, 0, 5, false);
		assertDecreasingOverlaps(overlaps);
	}
	
	/**
	 * As radius increases, the overlap should decrease
	 */
	@Test
	public void testEncodeRelativeRadii() {
		// As radius increases, the overlap should decrease
		double[] overlaps = overlapsForRelativeAreas(999, 25, new int[] {100, 200}, 5, 
			null, 1, 5, false);
		assertDecreasingOverlaps(overlaps);
		
		// As radius decreases, the overlap should decrease
		overlaps = overlapsForRelativeAreas(999, 25, new int[] {100, 200}, 20, 
			null, -2, 5, false);
		assertDecreasingOverlaps(overlaps);
	}
	
	/**
	 * As radius increases, the overlap should decrease
	 */
	@Test
	public void testEncodeRelativePositionsAndRadii() {
		// As radius increases and positions change, the overlap should decrease
		double[] overlaps = overlapsForRelativeAreas(999, 25, new int[] {100, 200}, 5, 
			new int[] { 1, 1}, 1, 5, false);
		assertDecreasingOverlaps(overlaps);
	}
	
	@Test
	public void testEncodeUnrelatedAreas() {
		double avgThreshold = 0.3;
		
		double maxThreshold = 0.14;
		double[] overlaps = overlapsForUnrelatedAreas(1499, 37, 5, 100, false);
		assertTrue(ArrayUtils.max(overlaps) < maxThreshold);
		assertTrue(ArrayUtils.average(overlaps) < avgThreshold);
		
		maxThreshold = 0.12;
		overlaps = overlapsForUnrelatedAreas(1499, 37, 10, 100, false);
		assertTrue(ArrayUtils.max(overlaps) < maxThreshold);
		assertTrue(ArrayUtils.average(overlaps) < avgThreshold);
		
		maxThreshold = 0.13;
		overlaps = overlapsForUnrelatedAreas(999, 25, 10, 100, false);
		assertTrue(ArrayUtils.max(overlaps) < maxThreshold);
		assertTrue(ArrayUtils.average(overlaps) < avgThreshold);
		
		maxThreshold = 0.16;
		overlaps = overlapsForUnrelatedAreas(499, 13, 10, 100, false);
		assertTrue(ArrayUtils.max(overlaps) < maxThreshold);
		assertTrue(ArrayUtils.average(overlaps) < avgThreshold);
	}
	
	@Test
	public void testEncodeAdjacentPositions() {
		int repetitions = 100;
		int n = 999;
		int w = 25;
		int radius = 10;
		double minThreshold = 0.75;
		double avgThreshold = 0.90;
		double[] allOverlaps = new double[repetitions];
		
		for(int i = 0;i < repetitions;i++) {
			double[] overlaps = overlapsForRelativeAreas(
				n, w, new int[] { i * 10,  i * 10 }, radius, new int[] { 0, 1 }, 0, 1, false);
			
			allOverlaps[i] = overlaps[0];
		}
		
		assertTrue(ArrayUtils.min(allOverlaps) > minThreshold);
		assertTrue(ArrayUtils.average(allOverlaps) > avgThreshold);
		
		if(verbose) {
			System.out.println(String.format("===== Adjacent positions overlap " +
				"(n = {0}, w = {1}, radius = {2} ===", n, w, radius));
			System.out.println(String.format("Max: {0}", ArrayUtils.max(allOverlaps)));
			System.out.println(String.format("Min: {0}", ArrayUtils.min(allOverlaps)));
			System.out.println(String.format("Average: {0}", ArrayUtils.average(allOverlaps)));
		}
	}
	
	public void assertDecreasingOverlaps(double[] overlaps) {
		assertEquals(0, 
			ArrayUtils.sum(
				ArrayUtils.where(
					ArrayUtils.diff(overlaps), ArrayUtils.GREATER_THAN_0)));
	}
	
	public int[] encode(CoordinateEncoder encoder, int[] coordinate, double radius) {
		int[] output = new int[encoder.getWidth()];
		encoder.encodeIntoArray(new Tuple(coordinate, radius), output);
		return output;
	}
	
	public double overlap(int[] sdr1, int[] sdr2) {
		assertEquals(sdr1.length, sdr2.length);
		int sum = ArrayUtils.sum(ArrayUtils.and(sdr1, sdr2));
//		System.out.println("and = " + Arrays.toString(ArrayUtils.where(ArrayUtils.and(sdr1, sdr2), ArrayUtils.WHERE_1)));
//		System.out.println("sum = " + ArrayUtils.sum(ArrayUtils.and(sdr1, sdr2)));
		return (double)sum / (double)ArrayUtils.sum(sdr1);
	}
	
	public double[] overlapsForRelativeAreas(int n, int w, int[] initPosition, int initRadius, 
		int[] dPosition, int dRadius, int num, boolean verbose) {
		
		setUp();
		builder.n(n);
		builder.w(w);
		initCE();
		
		double[] overlaps = new double[num];
		
		int[] outputA = encode(ce, initPosition, initRadius);
		int[] newPosition;
		for(int i = 0;i < num;i++) {
			newPosition = dPosition == null ? initPosition : 
				ArrayUtils.i_add(
					newPosition = Arrays.copyOf(initPosition, initPosition.length), 
						ArrayUtils.multiply(dPosition, (i + 1)));
			int newRadius = initRadius + (i + 1) * dRadius;
			int[] outputB = encode(ce, newPosition, newRadius);
			overlaps[i] = overlap(outputA, outputB);
		}
		
		return overlaps;
	}
	
	public double[] overlapsForUnrelatedAreas(int n, int w, int radius, int repetitions, boolean verbose) {
		return overlapsForRelativeAreas(n, w, new int[] { 0, 0 }, radius, 
			new int[] { 0, radius * 10 }, 0, repetitions, verbose);
	}
 
	@Test
	public void testTopStrict() {
		int[][] input = new int[][] 
			{{ 95, 195 },
			 { 95, 196 },
			 { 95, 197 },
			 { 95, 198 },
			 { 95, 199 },
			 { 95, 200 },
			 { 95, 201 },
			 { 95, 202 },
			 { 95, 203 },
			 { 95, 204 },
			 { 95, 205 },
			 { 96, 195 },
			 { 96, 196 },
			 { 96, 197 },
			 { 96, 198 },
			 { 96, 199 },
			 { 96, 200 },
			 { 96, 201 },
			 { 96, 202 },
			 { 96, 203 },
			 { 96, 204 },
			 { 96, 205 },
			 { 97, 195 },
			 { 97, 196 },
			 { 97, 197 },
			 { 97, 198 },
			 { 97, 199 },
			 { 97, 200 },
			 { 97, 201 },
			 { 97, 202 },
			 { 97, 203 },
			 { 97, 204 },
			 { 97, 205 },
			 { 98, 195 },
			 { 98, 196 },
			 { 98, 197 },
			 { 98, 198 },
			 { 98, 199 },
			 { 98, 200 },
			 { 98, 201 },
			 { 98, 202 },
			 { 98, 203 },
			 { 98, 204 },
			 { 98, 205 },
			 { 99, 195 },
			 { 99, 196 },
			 { 99, 197 },
			 { 99, 198 },
			 { 99, 199 },
			 { 99, 200 },
			 { 99, 201 },
			 { 99, 202 },
			 { 99, 203 },
			 { 99, 204 },
			 { 99, 205 },
			 {100, 195 },
			 {100, 196 },
			 {100, 197 },
			 {100, 198 },
			 {100, 199 },
			 {100, 200 },
			 {100, 201 },
			 {100, 202 },
			 {100, 203 },
			 {100, 204 },
			 {100, 205 },
			 {101, 195 },
			 {101, 196 },
			 {101, 197 },
			 {101, 198 },
			 {101, 199 },
			 {101, 200 },
			 {101, 201 },
			 {101, 202 },
			 {101, 203 },
			 {101, 204 },
			 {101, 205 },
			 {102, 195 },
			 {102, 196 },
			 {102, 197 },
			 {102, 198 },
			 {102, 199 },
			 {102, 200 },
			 {102, 201 },
			 {102, 202 },
			 {102, 203 },
			 {102, 204 },
			 {102, 205 },
			 {103, 195 },
			 {103, 196 },
			 {103, 197 },
			 {103, 198 },
			 {103, 199 },
			 {103, 200 },
			 {103, 201 },
			 {103, 202 },
			 {103, 203 },
			 {103, 204 },
			 {103, 205 },
			 {104, 195 },
			 {104, 196 },
			 {104, 197 },
			 {104, 198 },
			 {104, 199 },
			 {104, 200 },
			 {104, 201 },
			 {104, 202 },
			 {104, 203 },
			 {104, 204 },
			 {104, 205 },
			 {105, 195 },
			 {105, 196 },
			 {105, 197 },
			 {105, 198 },
			 {105, 199 },
			 {105, 200 },
			 {105, 201 },
			 {105, 202 },
			 {105, 203 },
			 {105, 204 },
			 {105, 205 } };
		
		CoordinateEncoder c = new CoordinateEncoder();
		int[][] results = c.topWCoordinates(c, input, 3);
		int[][] expected = new int[][] { {95, 200}, {99, 202}, {102, 198} };
		
		for(int i = 0;i < results.length;i++) {
			assertTrue(Arrays.equals(results[i], expected[i]));
		}
		
		System.out.println("done");
	}
}


