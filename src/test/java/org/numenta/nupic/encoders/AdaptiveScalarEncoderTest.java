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
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author sambit
 * 
 */
public class AdaptiveScalarEncoderTest {

	private AdaptiveScalarEncoder ase;
	private AdaptiveScalarEncoder.Builder builder;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 *
	 */
	@Before
	public void setUp() {
		builder = AdaptiveScalarEncoder.adaptiveBuilder().n(14).w(3).minVal(1)
				.maxVal(8).radius(1.5).resolution(0.5).periodic(false)
				.forced(true);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	private void initASE() {
		ase = builder.build();
	}

	/**
	 * Test method for
	 * {@link org.numenta.nupic.encoders.AdaptiveScalarEncoder#AdaptiveScalarEncoder()}
	 * .
	 */
	@Test
	public void testAdaptiveScalarEncoder() {
		setUp();
		initASE();
		Assert.assertNotNull("AdaptiveScalarEncoder class is null", ase);
	}

	@Test
	public void testInit() {
		setUp();
		initASE();
		Assert.assertNotNull("AdaptiveScalarEncoder class is null", ase);
		ase.setW(3);
		ase.setMinVal(1);
		ase.setMaxVal(8);
		ase.setN(14);
		ase.setRadius(1.5);
		ase.setResolution(0.5);
		ase.setForced(true);
		ase.init();
	}

	/**
	 * Test method for
	 * {@link org.numenta.nupic.encoders.AdaptiveScalarEncoder#initEncoder(int, double, double, int, double, double)}
	 * .
	 */
	@Test
	public void testInitEncoder() {
		setUp();
		initASE();
		ase.initEncoder(3, 1, 8, 14, 1.5, 0.5);
		Assert.assertNotNull("AdaptiveScalarEncoder class is null", ase);
		
		/////////// Negative Test ///////////
		setUp();
		initASE();
        Assert.assertNotNull("AdaptiveScalarEncoder class is null", ase);
        try {
            ase.setPeriodic(true); // Should cause failure during init
            ase.initEncoder(3, 1, 8, 14, 1.5, 0.5);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("Adaptive scalar encoder does not encode periodic inputs", e.getMessage());
        }
	}
	
	@Test
	public void testMissingData() {
		setUp();
		initASE();
		ase.initEncoder(3, 1, 8, 14, 1.5, 0.5);
		ase.setName("mv");
		ase.setPeriodic(false);
		
		int[] empty = ase.encode(Encoder.SENTINEL_VALUE_FOR_MISSING_DATA);
		System.out.println("\nEncoded missing data as: " + Arrays.toString(empty));
		int[] expected = new int[14];
		assertTrue(Arrays.equals(expected, empty));
	}
	
	@Test
	public void testNonPeriodicEncoderMinMaxSpec() {
		initASE();
		
		int[] res = ase.encode(1.0);
		System.out.println("\nEncoded data as: " + Arrays.toString(res));
		assertTrue(Arrays.equals(new int[] { 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, res));
		
		res = ase.encode(2.0);
		System.out.println("\nEncoded data as: " + Arrays.toString(res));
		assertTrue(Arrays.equals(new int[] { 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, res));
		
		res = ase.encode(8.0);
		System.out.println("\nEncoded data as: " + Arrays.toString(res));
		assertTrue(Arrays.equals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, res));
	}
	
	@Test
	public void testTopDownDecode() {
		initASE();
		
		double minVal = ase.getMinVal();
		System.out.println("\nThe min value is:" + minVal);
		double resolution = ase.getResolution();
		System.out.println(String.format("\nTesting non-periodic encoder decoding, resolution of %f ...", resolution));
		double maxVal = ase.getMaxVal();
		System.out.println("\nThe max value is:" + maxVal);
		
		while(minVal < maxVal) {
			int[] output = ase.encode(minVal);
			DecodeResult decoded = ase.decode(output, "");
			System.out.println("\nDecoding " + Arrays.toString(output) + String.format("(%f)", minVal) + " => " + decoded.toString());
			Map<String, RangeList> fields = decoded.getFields();
			
			Assert.assertEquals("Number of keys not matching", 1, fields.keySet().size());
			System.out.println("\nField Key: " + fields.keySet().iterator().next());
			
			Assert.assertEquals("Number of range not matching", 1, fields.get(fields.keySet().iterator().next()).size());
			System.out.println("\nField Range Value: " + fields.get(fields.keySet().iterator().next()).get(0));
			Assert.assertEquals("Range max and min are not matching", fields.get(fields.keySet().iterator().next()).getRange(0).max(),
					fields.get(fields.keySet().iterator().next()).getRange(0).min(), 0);
			
			assertTrue(Math.abs(fields.get(fields.keySet().iterator().next()).getRange(0).min() - minVal) < ase.getResolution());
			
			java.util.List<Encoding> topDown = ase.topDownCompute(output);
			assertTrue(topDown.size() == 1);
			System.out.println("\nTopDown => " + topDown.toString());
			
			int[] bucketIndices = ase.getBucketIndices(minVal);
			assertTrue("The bucket indice size is not matching", bucketIndices.length == 1);
			System.out.println("Bucket indices => " + Arrays.toString(bucketIndices));
			List<Encoding> bucketInfoList = ase.getBucketInfo(bucketIndices);
			assertTrue((Math.abs((double)bucketInfoList.get(0).getValue() - minVal)) <= (ase.getResolution() / 2));
			System.out.println("Bucket info value: " + bucketInfoList.get(0).getValue());
			System.out.println("Minval: " + minVal + " Abs(BucketVal - Minval): " + Math.abs((double)bucketInfoList.get(0).getValue() - minVal));
			System.out.println("Resolution: " + ase.getResolution() + " Resolution/2: " + ase.getResolution() / 2);
			assertTrue((double)bucketInfoList.get(0).getValue() == (double)ase.getBucketValues(Double.class).toArray()[bucketIndices[0]]);
			System.out.println("\nBucket info scalar: " + bucketInfoList.get(0).getScalar());
			System.out.println("\nBucket info value: " + bucketInfoList.get(0).getValue());
			assertTrue(bucketInfoList.get(0).getScalar().doubleValue() == (double)bucketInfoList.get(0).getValue());
			System.out.println("\nBucket info encoding: " + bucketInfoList.get(0).getEncoding());
			System.out.println("\nOriginal encoding: " + Arrays.toString(output));
			assertTrue(Arrays.equals(bucketInfoList.get(0).getEncoding(), output));
			
			minVal += resolution / 4;
		}
	}
	
	@Test
	public void testFillHoles() {
		initASE();
		int[] inputArray = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1 };
		double minVal = ase.getMinVal();
		
		DecodeResult decoded = ase.decode(inputArray, "");
		System.out.println("\nDecoding " + Arrays.toString(inputArray) + String.format("(%f)", minVal) + " => " + decoded.toString());
		Map<String, RangeList> fields = decoded.getFields();
		assertTrue(fields.size() == 1);
		
		Assert.assertEquals("Number of keys not matching", 1, fields.keySet().size());
		System.out.println("\nField Key: " + fields.keySet().iterator().next());
		
		Assert.assertEquals("Number of range not matching", 2, fields.get(fields.keySet().iterator().next()).size());
		System.out.println("\nField Range Value: " + fields.get(fields.keySet().iterator().next()).get(0));
		Assert.assertEquals("Range max and min are not matching", fields.get(fields.keySet().iterator().next()).getRange(0).max(),
				fields.get(fields.keySet().iterator().next()).getRange(0).min(), 0);
		
		assertTrue(fields.get(fields.keySet().iterator().next()).getRange(1).min() == 8.00);
		assertTrue(fields.get(fields.keySet().iterator().next()).getRange(1).max() == 8.00);
		
		int[] newArray = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1 };
		DecodeResult newDecoded = ase.decode(newArray, "");
		System.out.println("\nDecoding new array " + Arrays.toString(newArray) + String.format("(%f)", minVal) + " => " + newDecoded.toString());
		Map<String, RangeList> newFields = newDecoded.getFields();
		assertTrue(newFields.size() == 1);
		
		Assert.assertEquals("Number of keys not matching", 1, newFields.keySet().size());
		System.out.println("\nField Key: " + newFields.keySet().iterator().next());
		
		Assert.assertEquals("Number of range not matching", 2, newFields.get(newFields.keySet().iterator().next()).size());
		System.out.println("\nField Range Value: " + newFields.get(newFields.keySet().iterator().next()).get(0));
		Assert.assertEquals("Range max and min are not matching", newFields.get(newFields.keySet().iterator().next()).getRange(0).max(),
				newFields.get(newFields.keySet().iterator().next()).getRange(0).min(), 0);
		
		assertTrue(newFields.get(newFields.keySet().iterator().next()).getRange(1).min() == 8.00);
		assertTrue(newFields.get(newFields.keySet().iterator().next()).getRange(1).max() == 8.00);
		
	}
	
	@Test
	public void testSkippedMinMaxCode() {
	    setUp();
        initASE();
        ase.setMinVal(ase.getMaxVal());
        ase.getBucketIndices(ase.getMaxVal());
        assertEquals(1, ase.getRangeInternal(), 0); // ASE enforces minimum range of 1.0
	}
}
