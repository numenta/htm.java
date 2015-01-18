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
package org.numenta.nupic.encoders;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Calendar;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.util.Tuple;

/**
 * Unit tests for {@link RandomDistributedScalarEncoder}.
 *
 * @author Sean Connolly
 */
public class RandomDistributedScalarEncoderTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * Test basic encoding functionality.<br/>
	 * Create encodings without crashing and check they contain the correct
	 * number of on and off bits. Check some encodings for expected overlap.
	 * Test that encodings for old values don't change once we generate new
	 * buckets.
	 */
	@Test
	public void testEncoding() {
		// Initialize with non-default parameters
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().name("enc").resolution(1.0).w(23).n(500).offset(0.0)
				.build();
		// Encode with a number close to the offset
		int[] e0 = enc.encode(-0.1);
		assertEquals("Number of on bits is incorrect", 23, sum(e0));
		assertEquals("Width of the vector is incorrect", 500, e0.length);
		assertEquals("Offset doesn't correspond to middle bucket", 1000 / 2,
				enc.getBucketIndices(0.0)[0]);
		assertEquals("Number of buckets is not 1", 1, enc.getBucketMap().size());
		// Encode with a number that is resolution away from offset. Now we
		// should have two buckets and this encoding should be one bit away from
		// e0
		int[] e1 = enc.encode(1.0);
		assertEquals("Number of buckets is not 2", 2, enc.getBucketMap().size());
		assertEquals("Number of on bits is incorrect", 23, sum(e1));
		assertEquals("Width of the vector is incorrect", 500, e1.length);
		assertEquals("Overlap is not equal to w-1", 22, overlap(e0, e1));
		// Encode with a number that is resolution*w away from offset. Now we
		// should have many buckets and this encoding should have very little
		// overlap with e0
		int[] e25 = enc.encode(25.0);
		assertTrue("Number of buckets is not 2", enc.getBucketMap().size() > 23);
		assertEquals("Number of on bits is incorrect", 23, sum(e25));
		assertEquals("Width of the vector is incorrect", 500, e25.length);
		assertTrue("Overlap is too high", overlap(e0, e25) < 4);
		// Test encoding consistency. The encodings for previous numbers
		// shouldn't change even though we have added additional buckets
		assertThat("Encodings are not consistent - they have changed after "
				+ "new buckets have been created", e0,
				equalTo(enc.encode(-0.1)));
		assertThat("Encodings are not consistent - they have changed after "
				+ "new buckets have been created", e1, equalTo(enc.encode(1.0)));
	}

	/**
	 * Test that missing values and {@code null} return all zeros.
	 */
	@Test
	public void testMissingValues() {
		Encoder<Double> enc = RandomDistributedScalarEncoder.builder()
				.name("enc").resolution(1.0).build();

		int[] empty = enc.encode(Encoder.SENTINEL_VALUE_FOR_MISSING_DATA);
		assertEquals(0, sum(empty));

		empty = enc.encode(null);
		assertEquals(0, sum(empty));
	}

	/**
	 * Test that numbers within the same resolution return the same encoding.
	 * Numbers outside the resolution should return different encodings.
	 */
	@Test
	public void testResolution() {
		Encoder<Double> enc = RandomDistributedScalarEncoder.builder()
				.name("enc").resolution(1.0).build();
		// Since 23.0 is the first encoded number, it will be the offset.
		// Since resolution is 1, 22.9 and 23.4 should have the same bucket
		// index and encoding.
		int[] e23 = enc.encode(23.0);
		int[] e23_1 = enc.encode(23.1);
		int[] e22_9 = enc.encode(22.9);
		int[] e24 = enc.encode(24.0);
		assertEquals(enc.w, sum(e23));
		assertThat("Numbers within resolution don't have the same encoding",
				e23_1, equalTo(e23));
		assertThat("Numbers within resolution don't have the same encoding",
				e22_9, equalTo(e23));
		assertThat("Numbers outside resolution have the same encoding", e24,
				not(equalTo(e23)));

		int[] e22_5 = enc.encode(22.5);
		assertThat("Numbers outside resolution have the same encoding", e22_5,
				not(equalTo(e23)));
	}

	/**
	 * Identify point at which resolution forces values into different buckets;
	 * uses increasing values.
	 *
	 * @see {@link #testBucketIndexDecreasing()}
	 */
	@Test
	public void testBucketIndexIncreasing() {
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().resolution(1.0).build();
		assertEquals(500, (int) enc.getBucketIndex(23.0));
		assertEquals(500, (int) enc.getBucketIndex(23.1));
		assertEquals(500, (int) enc.getBucketIndex(23.2));
		assertEquals(500, (int) enc.getBucketIndex(23.3));
		assertEquals(500, (int) enc.getBucketIndex(23.4));
		assertEquals(501, (int) enc.getBucketIndex(23.5));
		assertEquals(501, (int) enc.getBucketIndex(23.6));
		assertEquals(501, (int) enc.getBucketIndex(23.7));
		assertEquals(501, (int) enc.getBucketIndex(23.8));
		assertEquals(501, (int) enc.getBucketIndex(23.9));
		assertEquals(501, (int) enc.getBucketIndex(24.0));
		assertEquals(501, (int) enc.getBucketIndex(24.1));
	}

	/**
	 * Identify point at which resolution forces values into different buckets;
	 * uses decreasing values.
	 *
	 * @see {@link #testBucketIndexIncreasing()}
	 */
	@Test
	public void testBucketIndexDecreasing() {
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().resolution(1.0).build();
		assertEquals(500, (int) enc.getBucketIndex(23.0));
		assertEquals(500, (int) enc.getBucketIndex(22.9));
		assertEquals(500, (int) enc.getBucketIndex(22.8));
		assertEquals(500, (int) enc.getBucketIndex(22.7));
		assertEquals(500, (int) enc.getBucketIndex(22.6));
		assertEquals(499, (int) enc.getBucketIndex(22.5));
		assertEquals(499, (int) enc.getBucketIndex(22.4));
		assertEquals(499, (int) enc.getBucketIndex(22.3));
		assertEquals(499, (int) enc.getBucketIndex(22.2));
		assertEquals(499, (int) enc.getBucketIndex(22.1));
		assertEquals(499, (int) enc.getBucketIndex(22.0));
		assertEquals(499, (int) enc.getBucketIndex(21.9));
	}

	/**
	 * Test that mapBucketIndexToNonZeroBits works and that max buckets and
	 * clipping are handled properly.
	 */
	@Test
	public void testMapBucketIndexToNonZeroBits() {
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().resolution(1.0).w(11).n(150).offset(null)
				// Set a low number of max buckets
				.maxBuckets(10).build();
		enc.encode(0.0);
		enc.encode(-7.0);
		enc.encode(7.0);

		assertEquals("maxBuckets exceeded", enc.getMaxBuckets(), enc
				.getBucketMap().size());
		assertTrue("mapBucketIndexToNonZeroBits did not handle negative index",
				enc.mapBucketIndexToNonZeroBits(-1) == enc.getBucketMap()
						.get(0));
		assertTrue("mapBucketIndexToNonZeroBits did not handle negative index",
				enc.mapBucketIndexToNonZeroBits(1000) == enc.getBucketMap()
						.get(9));

		int[] e23 = enc.encode(23.0);
		int[] e6 = enc.encode(6.0);
		assertThat("Values not clipped correctly during encoding", e6,
				equalTo(e23));

		int[] e_8 = enc.encode(-8.0);
		int[] e_7 = enc.encode(-7.0);
		assertThat("Values not clipped correctly during encoding", e_7,
				equalTo(e_8));

		assertEquals("getBucketIndices returned negative bucket index", 0,
				enc.getBucketIndices(-8)[0]);
		assertEquals(
				"getBucketIndices returned bucket index that is too large",
				enc.getMaxBuckets() - 1, enc.getBucketIndices(23)[0]);
	}

	/**
	 * Test that some bad construction parameters get handled.
	 */
	@Test
	public void testParameterCheckWithInvalidN() {
		// n must be >= 6 * w
		exception.expect(IllegalStateException.class);
		exception
				.expectMessage(startsWith("n must be an int strictly greater than 6*w"));
		RandomDistributedScalarEncoder.builder().n((int) (5.9 * 21)).w(21)
				.resolution(1).build();
	}

	/**
	 * Test that some bad construction parameters get handled.
	 */
	@Test
	public void testParameterCheckWithInvalidW() {
		// w can 't be negative
		exception.expect(IllegalStateException.class);
		exception
				.expectMessage(startsWith("w must be an odd positive integer"));
		RandomDistributedScalarEncoder.builder().w(-1).build();
	}

	/**
	 * Test that some bad construction parameters get handled.
	 */
	@Test
	public void testParameterCheckWithInvalidResolution() {
		// resolution can 't be negative
		exception.expect(IllegalStateException.class);
		exception
				.expectMessage(startsWith("resolution must be a positive number"));
		RandomDistributedScalarEncoder.builder().resolution(-1).build();
	}

	/**
	 * Check that the overlaps for the encodings are within the expected range.
	 * Here we ask the encoder to create a bunch of representations under
	 * somewhat stressful conditions, and then verify they are correct. We rely
	 * on the fact that the
	 * {@link RandomDistributedScalarEncoder.BucketMap#overlapOK(int i, int j)}
	 * and
	 * {@link RandomDistributedScalarEncoder.BucketMap#countOverlap(int i, int j)}
	 * methods are working correctly.
	 */
	@Test
	public void testOverlapStatistics() {
		// Generate and log a 32-bit compatible seed value.
		long time = Calendar.getInstance().getTimeInMillis();
		int seed = (int) (time % 10000) * 10;
		// Generate about 600 encodings.Set n relatively low to increase
		// chance of false overlaps
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().resolution(1.0).w(11).n(150).seed(seed).build();
		enc.encode(0.0);
		enc.encode(-3.0);
		enc.encode(3.0);
		assertTrue("Illegal overlap encountered in encoder",
				validateEncoder(enc, 3));
	}

	/**
	 * Test that the getWidth, getDescription, and getDecoderOutputFieldTypes
	 */
	@Test
	public void testGetMethods() {
		Encoder<Double> enc = RandomDistributedScalarEncoder.builder()
				.name("theName").resolution(1.0).n(500).build();
		assertEquals("getWidth doesn't return the correct result", 500,
				enc.getWidth());
		assertEquals("getDescription doesn't return the correct result",
				Arrays.asList(new Tuple(1, "theName", 0)), enc.getDescription());
		assertEquals(
				"getDecoderOutputFieldTypes doesn't return the correct result",
				Arrays.asList(FieldMetaType.FLOAT),
				enc.getDecoderOutputFieldTypes());
	}

	/**
	 * Test that offset is working properly
	 */
	@Test
	public void testOffset() {
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().name("enc").resolution(1.0).build();
		enc.encode(23.0);
		assertEquals("Offset not specified and not initialized to first input",
				Double.valueOf(23.0), enc.getOffset());

		enc = RandomDistributedScalarEncoder.builder().name("enc")
				.resolution(1.0).offset(25.0).build();
		enc.encode(23.0);
		assertEquals(
				"Offset not initialized to specified constructor parameter",
				Double.valueOf(25.0), enc.getOffset());
	}

	/**
	 * Test that initializing twice with the same seed returns identical
	 * encodings and different when not specified
	 */
	@Test
	public void testSeed() {
		Encoder<Double> enc1 = RandomDistributedScalarEncoder.builder()
				.name("enc").resolution(1.0).seed(42).build();
		Encoder<Double> enc2 = RandomDistributedScalarEncoder.builder()
				.name("enc").resolution(1.0).seed(42).build();
		Encoder<Double> enc3 = RandomDistributedScalarEncoder.builder()
				.name("enc").resolution(1.0).seed(-1).build();
		Encoder<Double> enc4 = RandomDistributedScalarEncoder.builder()
				.name("enc").resolution(1.0).seed(-1).build();
		int[] e1 = enc1.encode(23.0);
		int[] e2 = enc2.encode(23.0);
		int[] e3 = enc3.encode(23.0);
		int[] e4 = enc4.encode(23.0);
		assertThat("Same seed gives rise to different encodings", e1,
				equalTo(e2));
		assertThat("Different seeds gives rise to same encodings", e1,
				not(equalTo(e3)));
		assertThat("seeds of -1 give rise to same encodings", e3,
				not(equalTo(e4)));
	}

	/**
	 * Test that the internal method
	 * {@link RandomDistributedScalarEncoder.BucketMap#countOverlap(int i, int j)}
	 * works as expected.
	 */
	@Test
	public void testCountOverlapIndices() {
		// Create a fake set of encodings.
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().name("enc").resolution(1.0).w(5).n(5 * 20).build();
		RandomDistributedScalarEncoder.BucketMap bucketMap = enc.getBucketMap();
		int midIdx = enc.getMaxBuckets() / 2;
		bucketMap.put(midIdx - 2, range(3, 8));
		bucketMap.put(midIdx - 1, range(4, 9));
		bucketMap.put(midIdx, range(5, 10));
		bucketMap.put(midIdx + 1, range(6, 11));
		bucketMap.put(midIdx + 2, range(7, 12));
		bucketMap.put(midIdx + 3, range(8, 13));
		enc.setMinIndex(midIdx - 2);
		enc.setMaxIndex(midIdx + 3);
		// Indices must exist
		exception.expect(IllegalArgumentException.class);
		bucketMap.countOverlap(midIdx - 3, midIdx - 2);
		exception.expect(IllegalArgumentException.class);
		bucketMap.countOverlap(midIdx - 2, midIdx - 3);
		// Test some overlaps
		assertEquals("countOverlap didn't work", 5,
				bucketMap.countOverlap(midIdx - 2, midIdx - 2));
		assertEquals("countOverlap didn't work", 4,
				bucketMap.countOverlap(midIdx - 1, midIdx - 2));
		assertEquals("countOverlap didn't work", 2,
				bucketMap.countOverlap(midIdx + 1, midIdx - 2));
		assertEquals("countOverlap didn't work", 0,
				bucketMap.countOverlap(midIdx - 2, midIdx + 3));
	}

	/**
	 * Test that the internal method overlapOK works as expected.
	 */
	@Test
	public void testOverlapOK() {
		// Create a fake set of encodings.
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().name("enc").resolution(1.0).w(5).n(5 * 20).build();
		RandomDistributedScalarEncoder.BucketMap bucketMap = enc.getBucketMap();
		int midIdx = enc.getMaxBuckets() / 2;
		bucketMap.put(midIdx - 3, range(4, 9));// Not ok with midIdx -1
		bucketMap.put(midIdx - 2, range(3, 8));
		bucketMap.put(midIdx - 1, range(4, 9));
		bucketMap.put(midIdx, range(5, 10));
		bucketMap.put(midIdx + 1, range(6, 11));
		bucketMap.put(midIdx + 2, range(7, 12));
		bucketMap.put(midIdx + 3, range(8, 13));
		enc.setMinIndex(midIdx - 3);
		enc.setMaxIndex(midIdx + 3);

		assertTrue("overlapOK didn't work",
				bucketMap.overlapOK(midIdx, midIdx - 1));
		assertTrue("overlapOK didn't work",
				bucketMap.overlapOK(midIdx - 2, midIdx + 3));
		assertFalse("overlapOK didn't work",
				bucketMap.overlapOK(midIdx - 3, midIdx - 1));

		// We 'll just use our own numbers
		assertTrue("overlapOK didn't work for far values",
				bucketMap.overlapOK(100, 50, 0));
		assertTrue("overlapOK didn't work for far values", bucketMap.overlapOK(
				100, 50, RandomDistributedScalarEncoder.MAX_OVERLAP));
		assertFalse("overlapOK didn't work for far values",
				bucketMap.overlapOK(100, 50,
						RandomDistributedScalarEncoder.MAX_OVERLAP + 1));
		assertTrue("overlapOK didn't work for near values",
				bucketMap.overlapOK(50, 50, 5));
		assertTrue("overlapOK didn't work for near values",
				bucketMap.overlapOK(48, 50, 3));
		assertTrue("overlapOK didn't work for near values",
				bucketMap.overlapOK(46, 50, 1));
		assertTrue("overlapOK didn't work for near values",
				bucketMap.overlapOK(45, 50,
						RandomDistributedScalarEncoder.MAX_OVERLAP));
		assertFalse("overlapOK didn't work for near values",
				bucketMap.overlapOK(48, 50, 4));
		assertFalse("overlapOK didn't work for near values",
				bucketMap.overlapOK(48, 50, 2));
		assertFalse("overlapOK didn't work for near values",
				bucketMap.overlapOK(46, 50, 2));
		assertFalse("overlapOK didn't work for near values",
				bucketMap.overlapOK(50, 50, 6));

	}

	/**
	 * Test that the internal method
	 * {@link RandomDistributedScalarEncoder.BucketMap#countOverlap(int[] rep1, int[] rep2)}
	 * works as expected.
	 */
	@Test
	public void testCountOverlap() {
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().name("enc").resolution(1.0).n(500).build();
		RandomDistributedScalarEncoder.BucketMap bucketMap = enc.getBucketMap();

		int[] r01 = new int[] { 1, 2, 3, 4, 5, 6 };
		int[] r02 = new int[] { 1, 2, 3, 4, 5, 6 };
		assertEquals("countOverlap result is incorrect", 6,
				bucketMap.countOverlap(r01, r02));

		int[] r03 = new int[] { 1, 2, 3, 4, 5, 6 };
		int[] r04 = new int[] { 1, 2, 3, 4, 5, 7 };
		assertEquals("countOverlap result is incorrect", 5,
				bucketMap.countOverlap(r03, r04));

		int[] r05 = new int[] { 1, 2, 3, 4, 5, 6 };
		int[] r06 = new int[] { 6, 5, 4, 3, 2, 1 };
		assertEquals("countOverlap result is incorrect", 6,
				bucketMap.countOverlap(r05, r06));

		int[] r07 = new int[] { 1, 2, 8, 4, 5, 6 };
		int[] r08 = new int[] { 1, 2, 3, 4, 9, 6 };
		assertEquals("countOverlap result is incorrect", 4,
				bucketMap.countOverlap(r07, r08));

		int[] r09 = new int[] { 1, 2, 3, 4, 5, 6 };
		int[] r10 = new int[] { 1, 2, 3 };
		assertEquals("countOverlap result is incorrect", 3,
				bucketMap.countOverlap(r09, r10));

		int[] r11 = new int[] { 7, 8, 9, 10, 11, 12 };
		int[] r12 = new int[] { 1, 2, 3, 4, 5, 6 };
		assertEquals("countOverlap result is incorrect", 0,
				bucketMap.countOverlap(r11, r12));
	}

	/**
	 * Test that nothing is printed out when {@code verbosity = 0}.
	 */
	@Test
	public void testVerbosity() {
		// Given
		ByteArrayOutputStream outContent = new ByteArrayOutputStream();
		ByteArrayOutputStream errContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		Encoder<Double> encoder = RandomDistributedScalarEncoder.builder()
				.name("mv").resolution(1.0).verbosity(0).build();
		int[] output = new int[encoder.getWidth()];
		// When
		encoder.encodeIntoArray(23.0, output);
		encoder.getBucketIndices(23.0);
		// Then
		assertEquals("zero verbosity doesn't lead to zero output", "",
				outContent.toString());
		assertEquals("zero verbosity doesn't lead to zero err output", "",
				errContent.toString());
	}

	/**
	 * Test that the {@link RandomDistributedScalarEncoder} only encodes
	 * {@code Double}.<br/>
	 * Note: This is well covered by our, ahem, statically typed compiler.
	 * Included here for completeness of consistency with the Python project.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testEncodeInvalidInputType() {
		// Given
		Encoder encoder = RandomDistributedScalarEncoder.builder().name("enc")
				.resolution(1.0).verbosity(0).build();
		// Then
		exception.expect(ClassCastException.class);
		exception
				.expectMessage("java.lang.String cannot be cast to java.lang.Double");
		// When
		encoder.encode("String");
	}

	/**
	 * Compute the sum of all on bits; equivelant to the number of on bits.
	 */
	private int sum(int[] x) {
		int count = 0;
		for (int next : x) {
			count += next;
		}
		return count;
	}

	/**
	 * Given two binary arrays, compute their overlap. The overlap is the number
	 * of bits where x[i] and y[i] are both 1
	 */
	private int overlap(int[] x, int[] y) {
		assertEquals(x.length, y.length);
		int overlap = 0;
		for (int i = 0; i < x.length; i++) {
			if (x[i] == 1 && y[i] == 1) {
				overlap++;
			}
		}
		return overlap;
	}

	/**
	 * Given an encoder, calculate overlaps statistics and ensure everything is
	 * ok. We don't check every possible combination for speed reasons.
	 *
	 * @param enc
	 *            the encoder under test
	 * @param subsampling
	 *            the amount of sub-sampling to perform
	 * @return {@code true} if the encoder validates
	 */
	private boolean validateEncoder(RandomDistributedScalarEncoder enc,
			int subsampling) {
		for (int i : range(enc.getMinIndex(), enc.getMaxIndex() + 1, 1)) {
			for (int j : range(i + 1, enc.getMaxIndex() + 1, subsampling)) {
				if (!enc.getBucketMap().overlapOK(i, j)) {
					return false;
				}
			}
		}
		return true;
	}

	private int[] range(int start, int stop) {
		return range(start, stop, 1);
	}

	private int[] range(int start, int stop, int step) {
		int[] r = new int[(int) Math.ceil((stop - start) / (double) step)];
		int i = 0;
		for (int x = start; x < stop; x += step) {
			r[i] = x;
			i++;
		}
		return r;
	}

}
