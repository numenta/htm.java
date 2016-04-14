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

import gnu.trove.list.array.TDoubleArrayList;
import org.junit.Test;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.util.MinMax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LogEncoderTest {
	private LogEncoder le;
	private LogEncoder.Builder builder;
	
	private void setUp() {
        builder =  LogEncoder.builder()
	        .w(5)
	        .resolution(0.1)
	        .minVal(1.0)
	        .maxVal(10000.)
	        .name("amount")
	        .forced(true);
    }
	
	private void initLE() {
		le = builder.build();
	}
	
	@Test
	public void testLogEncoder() {
		setUp();
		initLE();
		
		// Verify we're setting the description properly
		assertTrue(le.getDescription().get(0).get(0).equals("amount"));
		assertEquals(le.getDescription().get(0).get(1), 0);
		
		// Verify we're getting the correct field types
		FieldMetaType fieldType = le.getDecoderOutputFieldTypes().iterator().next();
		assertTrue(fieldType.equals(FieldMetaType.FLOAT));
		
		// Verify the encoder ends up with the correct width
	    //
	    // 10^0 -> 10^4 => 0 -> 4; With a resolution of 0.1
	    // 41 possible values plus padding = 4 = width 45
		assertEquals(le.getWidth(),  45);
		
	    // Verify the encoder ends up with the correct width
	    //
	    // 10^0 -> 10^4 => 0 -> 4; With a resolution of 0.1
	    // 41 possible values plus padding = 4 = width 45
		assertEquals(le.getBucketValues(Double.class).size(),  41);
	}
	
	@Test
	public void testClosenessCalculations() {
		setUp();
		initLE();
		
		double[] expectedValues = new double[]{1., 1., 1., 1.};
		double[] actualValues = new double[]{10000., 1000., 1., -200.};
		double[] expectedResults = new double[]{0.0, 0.25, 1.0, 1.0};
		
		for (int i = 0; i < expectedValues.length; i++) {
			assertEquals(String.format("exp: %.0f act: %.0f expR: 0.2f", expectedValues[i], actualValues[i], expectedResults[i]),
						 le.closenessScores(new TDoubleArrayList(new double[] {expectedValues[i]}), 
								 new TDoubleArrayList(new double[] {actualValues[i]}),
								 true).get(0),
						 expectedResults[i], 1e-07);
		}
	}
	
	@Test
	public void verifyEncodingAndReverseLookup() {
		setUp();
		initLE();
		
		double value = 1.0;
		int[] output = le.encode(value);
		
		int[] expected = new int[45];
		for (int i = 0; i < 5; i++) {
			expected[i] = 1;
		}

		assertTrue(Arrays.equals(output, expected));
		
		DecodeResult decoded = le.decode(output, "");
		assertEquals(decoded.getFields().keySet().size(), 1);
		
		MinMax minMax = ((RangeList) decoded.getFields().values().toArray()[0]).getRange(0);
		assertTrue(minMax.min() == 1 && minMax.max() == 1);
	}

	@Test
	public void testMissingValue() {
		setUp();
		initLE();
		
		int[] output = le.encode(Encoder.SENTINEL_VALUE_FOR_MISSING_DATA);
		
		int[] expected = new int[45];
		
		assertTrue(Arrays.equals(output, expected));
	}
	
	@Test
	public void testTopDown() {
		setUp();
		initLE();
		
		double value = le.getMinVal();
		
		while (value < le.getMaxVal()) {
			int[] output = le.encode(value);
			Encoding topDown = le.topDownCompute(output).get(0);
			
			// Do the scaling by hand here.
			double scaledVal=  Math.log10(value);
			
			// Find the range of values that would also produce this top down output.
			double minTopDown = Math.pow(10, (scaledVal - le.getResolution()));
			double maxTopDown = Math.pow(10, (scaledVal + le.getResolution()));
			
			// Verify the range surrounds this scaled val
			assertTrue((Double)topDown.getValue() >= minTopDown && 
					   (Double)topDown.getValue() <= maxTopDown);
			
			// Test bucket support
			int[] bucketIndices = le.getBucketIndices(value);
			topDown = le.getBucketInfo(bucketIndices).get(0);
			
			// Verify our reconstructed value is in the valid range
			assertTrue((Double)topDown.getValue() >= minTopDown && 
					   (Double)topDown.getValue() <= maxTopDown);
			
			// Same for the scalar value
			assertTrue((Double)topDown.getScalar() >= minTopDown && 
					   (Double)topDown.getScalar() <= maxTopDown);
			
			// The the encoding portion of our EncoderResult matched the result of encode()
			int[] encoding = topDown.getEncoding();
			assertTrue(Arrays.equals(encoding,output));
			
			// Verify out reconstructed value is the same as the bucket value
			List<Double> bucketValues = le.getBucketValues(Double.class);
			assertEquals((Double)topDown.getValue(), bucketValues.get(bucketIndices[0]));
			
			//Next value
			scaledVal += le.getResolution() / 4.0;
			value = Math.pow(10,  scaledVal);
		}
	}
	
	@Test
	public void testNextPowerOf10() {
		setUp();
		initLE();
		
		// Verify next power of 10 encoding
		int[] output = le.encode(100.);
		// increase of 2 decades = 20 decibels
	    // bit 0, 1 are padding; bit 3 is 1, ..., bit 22 is 20 (23rd bit) 
		int[] expected = new int[45];
		for (int i = 20; i < 25; i++) {
			expected[i] = 1;
		}
		assertTrue(Arrays.equals(output,  expected));
		
		// Test reverse lookup
		DecodeResult decoded = le.decode(output, "");
		assertEquals(decoded.getFields().keySet().size(), 1);		
		MinMax minMax = ((RangeList) decoded.getFields().values().toArray()[0]).getRange(0);
		assertTrue(decoded.getFields().keySet().size() == 1 &&
			       minMax.min() == 100 && minMax.max() == 100);
		
		// Verify next power of 10 encoding
		output = le.encode(10000.);
		expected = new int[45];
		for (int i = 40; i < 45; i++) {
			expected[i] = 1;
		}
		assertTrue(Arrays.equals(output,  expected));
		
		// Test reverse lookup
		decoded = le.decode(output, "");
		assertEquals(decoded.getFields().keySet().size(), 1);		
		minMax = ((RangeList) decoded.getFields().values().toArray()[0]).getRange(0);
		assertTrue(decoded.getFields().keySet().size() == 1 &&
			       minMax.min() == 10000 && minMax.max() == 10000);	
	}
	
	/**
	 * Verify that the value of buckets are as expected for given init params
	 */
	@Test
	public void testGetBucketValue() {
		LogEncoder le = LogEncoder.builder()
		        .w(5)
		        .resolution(0.1)
		        .minVal(1.0)
		        .maxVal(10000.)
		        .name("amount")
		        .forced(true)
		        .build();
		
		// Build our expected values
		double inc = 0.1;
		double exp = 0;
		List<Double> expected = new ArrayList<Double>();
		// Incrementing to exactly 4.0 runs into fp issues
		while (exp <= 4.0001) {
			double val = Math.pow(10, exp);
			expected.add(val);
			exp += inc;
		}
		
		List<Double> actual = le.getBucketValues(Double.class);
		
		for (int i = 0; i < actual.size(); i++) {
			assertEquals(expected.get(i), actual.get(i), 0.0000001);
		}
	}
	
	/**
	 * Verifies you can use radius to specify a log encoder
	 */
	@Test
	public void testInitWithRadius() {
		LogEncoder le = LogEncoder.builder()
		        .w(1)
		        .radius(1)
		        .minVal(1.0)
		        .maxVal(10000.)
		        .name("amount")
		        .forced(true)
		        .build();
		
		assertEquals(le.getN(), 5);
		
		// Verify a couple powers of 10 are encoded as expected
		double value = 1.0;
		int[] output = le.encode(value);
		int[] expected = new int[]{1, 0, 0, 0, 0};
		assertTrue(Arrays.equals(output, expected));
		
		value = 100.0;
		output = le.encode(value);
		expected = new int[]{0, 0, 1, 0, 0};
		assertTrue(Arrays.equals(output, expected));
	}
	
	/**
	 * Verifies you can use N to specify a log encoder
	 */
	@Test
	public void testInitWithN() {
		int n = 100;
		LogEncoder le = LogEncoder.builder()
		        .n(n)
		        .forced(true)
		        .build();
		assertEquals(le.getN(), n);
	}
	
	/**
	 * Verifies unusual instances of minval and maxval are handled properly
	 */
	@Test
	public void testMinValMaxVal() {
		try {
			LogEncoder.builder()
			        .n(100)
			        .minVal(0.0)
			        .maxVal(-100.)
			        .forced(true)
			        .build();
			fail("IllegalStateException not thrown");
		} catch (IllegalStateException expectedException) {
		}
		
		
		try {
			LogEncoder.builder()
			        .n(100)
			        .minVal(0.0)
			        .maxVal(1e-07)
			        .forced(true)
			        .build();
			fail("IllegalStateException not thrown");
		} catch (IllegalStateException expectedException) {
		}
	}
}
