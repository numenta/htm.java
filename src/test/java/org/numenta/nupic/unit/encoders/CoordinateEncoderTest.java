package org.numenta.nupic.unit.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.numenta.nupic.encoders.CoordinateEncoder;

public class CoordinateEncoderTest {
	private CoordinateEncoder ce;
	private CoordinateEncoder.Builder builder;
	
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
		double h1 = CoordinateEncoder.orderForCoordinate(new int[] { 2, 5, 10 });
		double h2 = CoordinateEncoder.orderForCoordinate(new int[] { 2, 5, 11 });
		double h3 = CoordinateEncoder.orderForCoordinate(new int[] { 2497477, -923478 });
		
		assertTrue(0 <= h1 && h1 < 1);
		assertTrue(0 <= h2 && h2 < 1);
		assertTrue(0 <= h3 && h3 < 1);
		
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
		
	}

}
