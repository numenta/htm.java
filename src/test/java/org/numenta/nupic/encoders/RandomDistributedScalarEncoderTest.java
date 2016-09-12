package org.numenta.nupic.encoders;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.util.Tuple;

/**
 * Unit tests for RandomDistributedScalarEncoder class
 * 
 * @author Anubhav Chaturvedi
 *
 */
public class RandomDistributedScalarEncoderTest {

	private RandomDistributedScalarEncoder rdse;
	private RandomDistributedScalarEncoder.Builder builder;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * Test basic encoding functionality. Create encodings without crashing and
     * check they contain the correct number of on and off bits. Check some
     * encodings for expected overlap. Test that encodings for old values don't
     * change once we generate new buckets.
	 */
	@Test
	public void testEncoding() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1)
				.w(23)
				.n(500)
				.setOffset(0);
		rdse = builder.build();

		
		int e0[] = rdse.encode(-0.1);
		assertEquals("Number of on bits is incorrect", getOnBits(e0), 23);
		assertEquals("Width of the vector is incorrect", e0.length, 500);
		assertEquals("Offset doesn't correspond to middle bucket",
				rdse.getBucketIndices(0)[0], rdse.getMaxBuckets() / 2);
		assertEquals("Number of buckets is not 1", 1, rdse.bucketMap.size());

		// Encode with a number that is resolution away from offset. Now we should
        // have two buckets and this encoding should be one bit away from e0
		int e1[] = rdse.encode(1.0);
		assertEquals("Number of buckets is not 2", 2, rdse.bucketMap.size());
		assertEquals("Number of on bits is incorrect", getOnBits(e1), 23);
		assertEquals("Width of the vector is incorrect", e0.length, 500);
		assertEquals("Overlap is not equal to w-1", computeOverlap(e0, e1), 22);

		// Encode with a number that is resolution*w away from offset. Now we should
        // have many buckets and this encoding should have very little overlap with
        // e0
		int e25[] = rdse.encode(25.0);
		assertTrue("Buckets created are not more than 23", rdse.bucketMap.size() > 23);
		assertEquals("Number of on bits is incorrect", getOnBits(e1), 23);
		assertEquals("Width of the vector is incorrect", e0.length, 500);
		assertTrue("Overlap is too high", computeOverlap(e0, e25) < 4);

		// Test encoding consistency. The encodings for previous numbers
	    // shouldn't change even though we have added additional buckets
		assertThat(
				"Encodings are not consistent - they have changed after new buckets have been created",
				rdse.encode(-0.1), is(equalTo(e0)));
		assertThat(
				"Encodings are not consistent - they have changed after new buckets have been created",
				rdse.encode(1.0), is(equalTo(e1)));
	}

	/**
	 * Test that missing values and NaN return all zero's.
	 */
	@Test
	public void testMissingValues() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1);
		rdse = builder.build();

		int[] e2 = rdse.encode(Double.NaN);
		assertEquals(0, getOnBits(e2));

		int[] e1 = rdse.encode(Encoder.SENTINEL_VALUE_FOR_MISSING_DATA);
		assertEquals(0, getOnBits(e1));
	}

	/**
	 * Test that numbers within the same resolution return the same encoding.
     * Numbers outside the resolution should return different encodings.
	 */
	@Test
	public void testResolution() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1);
		rdse = builder.build();

		// Since 23.0 is the first encoded number, it will be the offset.
	    // Since resolution is 1, 22.9 and 23.4 should have the same bucket index and
	    // encoding.
		int[] e23 = rdse.encode(23.0);
		int[] e23_1 = rdse.encode(23.1);
		int[] e22_9 = rdse.encode(22.9);
		int[] e24 = rdse.encode(24.0);

		assertEquals(rdse.getW(), getOnBits(e23));
		assertThat("Numbers within resolution don't have the same encoding",
				e23_1, is(equalTo(e23)));
		assertThat("Numbers within resolution don't have the same encoding",
				e22_9, is(equalTo(e23)));

		assertThat("Numbers outside resolution have the same encoding", e23,
				is(not(equalTo(e24))));
		int[] e22_5 = rdse.encode(22.5);
		assertThat("Numbers outside resolution have the same encoding", e23,
				is(not(equalTo(e22_5))));
	}

	/**
	 * Test that mapBucketIndexToNonZeroBits works and that max buckets and
     * clipping are handled properly.
	 */
	@Test
	public void testMapBucketIndexToNonZeroBits() {
		builder = RandomDistributedScalarEncoder.builder()
				.resolution(1)
				.w(11)
				.n(150);
		rdse = builder.build();

		// Set a low number of max buckets
		rdse.initializeBucketMap(10, null);

		rdse.encode(0.0);
		rdse.encode(-7.0);
		rdse.encode(7.0);

		assertEquals("maxBuckets exceeded", rdse.getMaxBuckets(),
				rdse.bucketMap.size());

		assertThat("mapBucketIndexToNonZeroBits did not handle negative index",
				rdse.mapBucketIndexToNonZeroBits(-1),
				is(equalTo(rdse.bucketMap.get(0))));
		assertThat("mapBucketIndexToNonZeroBits did not handle negative index",
				rdse.mapBucketIndexToNonZeroBits(1000),
				is(equalTo(rdse.bucketMap.get(9))));

		int[] e23 = rdse.encode(23.0);
		int[] e6 = rdse.encode(6.0);
		assertThat("Values not clipped correctly during encoding", e23,
				is(equalTo(e6)));

		int[] e_8 = rdse.encode(-8.0);
		int[] e_7 = rdse.encode(-7.0);
		assertThat("Values not clipped correctly during encoding", e_8,
				is(equalTo(e_7)));

		assertEquals("getBucketIndices returned negative bucket index", 0,
				rdse.getBucketIndices(-8.0)[0]);
		assertEquals("getBucketIndices returned negative bucket index",
				rdse.getMaxBuckets() - 1, rdse.getBucketIndices(23.0)[0]);
	}

	/**
	 * @author Sean Connolly
	 */
	@Test
	public void testParameterCheckWithInvalidN() {
		// n must be >= 6 * w
		exception.expect(IllegalStateException.class);
		exception.expectMessage("n must be strictly greater than 6*w. For good results we recommend n be strictly greater than 11*w.");
		RandomDistributedScalarEncoder.builder()
			.n((int) (5.9 * 21))
			.w(21)
			.resolution(1)
			.build();
	}

	/**
	 * @author Sean Connolly
	 */
	@Test
	public void testParameterCheckWithInvalidW() {
		// w can 't be negative
		exception.expect(IllegalStateException.class);
		exception.expectMessage("W must be an odd positive integer (to eliminate centering difficulty)");
		RandomDistributedScalarEncoder.builder()
			.n(500)
			.w(6)
			.resolution(2)
			.build();
	}

	/**
	 * @author Sean Connolly
	 */
	@Test
	public void testParameterCheckWithInvalidResolution() {
		// resolution can 't be negative
		exception.expect(IllegalStateException.class);
		exception.expectMessage("Resolution must be a positive number");
		RandomDistributedScalarEncoder.builder()
			.n(500)
			.w(5)
			.resolution(-1)
			.build();
	}

	/**
	 * Check that the overlaps for the encodings are within the expected range.
     * Here we ask the encoder to create a bunch of representations under somewhat
     * stressfull conditions, and then verify they are correct. We rely on the fact
     * that the _overlapOK and _countOverlapIndices methods are working correctly.
	 */
	@Test
	public void testOverlapStatistics() {
		builder = RandomDistributedScalarEncoder.builder()
				.resolution(1)
				.w(11)
				.n(150)
				.setSeed(RandomDistributedScalarEncoder.DEFAULT_SEED);
		rdse = builder.build();

		rdse.encode(0.0);
		rdse.encode(-300.0);
		rdse.encode(300.0);

		assertTrue("Illegal overlap encountered in encoder",
				validateEncoder(rdse, 3));
	}

	/**
	 * Test that the getWidth, getDescription, and getDecoderOutputFieldTypes
     * methods work.
	 */
	@Test
	public void testGetMethods() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("theName")
				.resolution(1)
				.n(500);
		rdse = builder.build();

		assertEquals("getWidth doesn't return the correct result", 500,
				rdse.getWidth());
		assertEquals(
				"getDescription doesn't return the correct result",
				new ArrayList<Tuple>(Arrays.asList(new Tuple[] { new Tuple("theName",
						0) })), rdse.getDescription());

		assertThat(
				"getDecoderOutputFieldTypes doesn't return the correct result",
				rdse.getDecoderOutputFieldTypes(),
				is(equalTo(new LinkedHashSet<>(Arrays.asList(FieldMetaType.FLOAT, FieldMetaType.INTEGER)))));
	}

	/**
	 * Test that offset is working properly
	 */
	@Test
	public void testOffset() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1);
		rdse = builder.build();

		rdse.encode(23.0);
		assertEquals(
				"Offset not initialized to specified constructor parameter",
				23, rdse.getOffset(), 0);

		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1)
				.setOffset(25.0);
		rdse = builder.build();

		rdse.encode(23.0);
		assertEquals(
				"Offset not initialized to specified constructor parameter",
				25, rdse.getOffset(), 0);
	}

	@Test
	public void testSeed() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1);

		RandomDistributedScalarEncoder encoder1 = builder.setSeed(42).build();
		RandomDistributedScalarEncoder encoder2 = builder.setSeed(42).build();
		RandomDistributedScalarEncoder encoder3 = builder.setSeed(-2).build();
		//RandomDistributedScalarEncoder encoder4 = builder.setSeed(-1).build();

		int[] e1 = encoder1.encode(23.0);
		int[] e2 = encoder2.encode(23.0);
		int[] e3 = encoder3.encode(23.0);
		//int[] e4 = encoder4.encode(23.0);

		assertThat("Same seed gives rise to different encodings", e1,
				is(equalTo(e2)));
		assertThat("Different seeds gives rise to same encodings", e1,
				is(not(equalTo(e3))));
		//Removing this test because testing the RNG is not part of the scope of
		//this test - and we cannot assure that the RNG will initialize the default
		//seed to different values.
		//assertThat("seeds of -1 give rise to same encodings", e4,
		//		is(not(equalTo(e3))));
	}

	/**
	 * Test that the internal method _countOverlapIndices works as expected.
	 */
	@Test
	public void testCountOverlapIndices() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1)
				.w(5)
				.n(5 * 20);
		rdse = builder.build();

		// Create a fake set of encodings.
		int midIdx = rdse.getMaxBuckets() / 2;

		rdse.bucketMap.put(midIdx - 2, getRangeAsList(3, 8));
		rdse.bucketMap.put(midIdx - 1, getRangeAsList(4, 9));
		rdse.bucketMap.put(midIdx, getRangeAsList(5, 10));
		rdse.bucketMap.put(midIdx + 1, getRangeAsList(6, 11));
		rdse.bucketMap.put(midIdx + 2, getRangeAsList(7, 12));
		rdse.bucketMap.put(midIdx + 3, getRangeAsList(8, 13));
		rdse.minIndex = midIdx - 2;
		rdse.maxIndex = midIdx + 3;

		// Test some overlaps
		assertEquals("countOverlapIndices didn't work", 5,
				rdse.countOverlapIndices(midIdx - 2, midIdx - 2));
		assertEquals("countOverlapIndices didn't work", 4,
				rdse.countOverlapIndices(midIdx - 1, midIdx - 2));
		assertEquals("countOverlapIndices didn't work", 2,
				rdse.countOverlapIndices(midIdx + 1, midIdx - 2));
		assertEquals("countOverlapIndices didn't work", 0,
				rdse.countOverlapIndices(midIdx - 2, midIdx + 3));
	}
	
	@Test
	public void testCountOverlapIndicesWithWrongIndices_i_j() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1)
				.w(5)
				.n(5 * 20);
		rdse = builder.build();

		int midIdx = rdse.getMaxBuckets() / 2;

		rdse.bucketMap.put(midIdx - 2, getRangeAsList(3, 8));
		rdse.bucketMap.put(midIdx - 1, getRangeAsList(4, 9));
		rdse.bucketMap.put(midIdx, getRangeAsList(5, 10));
		rdse.bucketMap.put(midIdx + 1, getRangeAsList(6, 11));
		rdse.bucketMap.put(midIdx + 2, getRangeAsList(7, 12));
		rdse.bucketMap.put(midIdx + 3, getRangeAsList(8, 13));
		rdse.minIndex = midIdx - 2;
		rdse.maxIndex = midIdx + 3;

		exception.expect(IllegalStateException.class);
		exception.expectMessage( allOf( startsWith("index"), endsWith("don't exist") ) );
		rdse.countOverlapIndices(midIdx - 3, midIdx - 4);
	}
	
	@Test
	public void testCountOverlapIndicesWithWrongIndices_i() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1)
				.w(5)
				.n(5 * 20);
		rdse = builder.build();

		int midIdx = rdse.getMaxBuckets() / 2;

		rdse.bucketMap.put(midIdx - 2, getRangeAsList(3, 8));
		rdse.bucketMap.put(midIdx - 1, getRangeAsList(4, 9));
		rdse.bucketMap.put(midIdx, getRangeAsList(5, 10));
		rdse.bucketMap.put(midIdx + 1, getRangeAsList(6, 11));
		rdse.bucketMap.put(midIdx + 2, getRangeAsList(7, 12));
		rdse.bucketMap.put(midIdx + 3, getRangeAsList(8, 13));
		rdse.minIndex = midIdx - 2;
		rdse.maxIndex = midIdx + 3;

		exception.expect(IllegalStateException.class);
		exception.expectMessage( allOf( startsWith("index"), endsWith("doesn't exist") ) );
		rdse.countOverlapIndices(midIdx - 3, midIdx - 2);
	}

	/**
	 * Test that the internal method {@link RandomDistributedScalarEncoder#overlapOK(int, int)}
	 *  works as expected.
	 */
	@Test
	public void testOverlapOK() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1)
				.w(5)
				.n(5 * 20);
		rdse = builder.build();

		int midIdx = rdse.getMaxBuckets() / 2;

		rdse.bucketMap.put(midIdx - 3, getRangeAsList(4, 9));
		rdse.bucketMap.put(midIdx - 2, getRangeAsList(3, 8));
		rdse.bucketMap.put(midIdx - 1, getRangeAsList(4, 9));
		rdse.bucketMap.put(midIdx, getRangeAsList(5, 10));
		rdse.bucketMap.put(midIdx + 1, getRangeAsList(6, 11));
		rdse.bucketMap.put(midIdx + 2, getRangeAsList(7, 12));
		rdse.bucketMap.put(midIdx + 3, getRangeAsList(8, 13));
		rdse.minIndex = midIdx - 3;
		rdse.maxIndex = midIdx + 3;

		assertTrue("overlapOK didn't work", rdse.overlapOK(midIdx, midIdx - 1));
		assertTrue("overlapOK didn't work",
				rdse.overlapOK(midIdx - 2, midIdx + 3));
		assertFalse("overlapOK didn't work",
				rdse.overlapOK(midIdx - 3, midIdx - 1));

		assertTrue("overlapOK didn't work for far values",
				rdse.overlapOK(100, 50, 0));
		assertTrue("overlapOK didn't work for far values",
				rdse.overlapOK(100, 50, rdse.getMaxOverlap()));
		assertFalse("overlapOK didn't work for far values",
				rdse.overlapOK(100, 50, rdse.getMaxOverlap() + 1));
		assertTrue("overlapOK didn't work for far values",
				rdse.overlapOK(50, 50, 5));
		assertTrue("overlapOK didn't work for far values",
				rdse.overlapOK(48, 50, 3));
		assertTrue("overlapOK didn't work for far values",
				rdse.overlapOK(46, 50, 1));
		assertTrue("overlapOK didn't work for far values",
				rdse.overlapOK(45, 50, rdse.getMaxOverlap()));
		assertFalse("overlapOK didn't work for far values",
				rdse.overlapOK(48, 50, 4));
		assertFalse("overlapOK didn't work for far values",
				rdse.overlapOK(48, 50, 2));
		assertFalse("overlapOK didn't work for far values",
				rdse.overlapOK(46, 50, 2));
		assertFalse("overlapOK didn't work for far values",
				rdse.overlapOK(50, 50, 6));
	}

	@Test
	public void testCountOverlap() {
		builder = RandomDistributedScalarEncoder.builder()
				.name("enc")
				.resolution(1)
				.n(500);
		rdse = builder.build();

		int[] r1 = new int[] { 1, 2, 3, 4, 5, 6 };
		int[] r2 = new int[] { 1, 2, 3, 4, 5, 6 };
		assertEquals("countOverlap result is incorrect", 6,
				rdse.countOverlap(r1, r2));

		r1 = new int[] { 1, 2, 3, 4, 5, 6 };
		r2 = new int[] { 1, 2, 3, 4, 5, 7 };
		assertEquals("countOverlap result is incorrect", 5,
				rdse.countOverlap(r1, r2));

		r1 = new int[] { 1, 2, 3, 4, 5, 6 };
		r2 = new int[] { 6, 5, 4, 3, 2, 1 };
		assertEquals("countOverlap result is incorrect", 6,
				rdse.countOverlap(r1, r2));

		r1 = new int[] { 1, 2, 8, 4, 5, 6 };
		r2 = new int[] { 1, 2, 3, 4, 9, 6 };
		assertEquals("countOverlap result is incorrect", 4,
				rdse.countOverlap(r1, r2));

		r1 = new int[] { 1, 2, 3, 4, 5, 6 };
		r2 = new int[] { 1, 2, 3 };
		assertEquals("countOverlap result is incorrect", 3,
				rdse.countOverlap(r1, r2));

		r1 = new int[] { 7, 8, 9, 10, 11, 12 };
		r2 = new int[] { 1, 2, 3, 4, 5, 6 };
		assertEquals("countOverlap result is incorrect", 0,
				rdse.countOverlap(r1, r2));
	}

	private List<Integer> getRangeAsList(int lowerBound, int upperBound) {
		if (lowerBound > upperBound)
			return null;

		Integer[] arr = new Integer[upperBound - lowerBound];
		for (int i = lowerBound; i < upperBound; i++) {
			arr[i - lowerBound] = i;
		}

		return Arrays.asList(arr);
	}

	@Test
	public void testGetOnBitsMethod()
	{
		int input1[] = new int[] {1,0,0,0,1};
		int input2[] = new int[] {1,0,2,0,1};
		
		assertEquals("getOnBits returned wrong value ", 2, getOnBits(input1));
		assertEquals("getOnBits did not return -1 for invalid input", -1, getOnBits(input2));
	}
	
	
	private boolean validateEncoder(RandomDistributedScalarEncoder encoder,
			int subsampling) {
		for (int i = encoder.minIndex; i <= encoder.maxIndex; i++) {
			for (int j = i + 1; j <= encoder.maxIndex; j += subsampling) {
				if (!encoder.overlapOK(i, j))
					return false;
			}
		}
		return true;
	}

	private int computeOverlap(int[] result1, int[] result2) {
		if (result1.length != result2.length)
			return Integer.MIN_VALUE;

		int overlap = 0;
		for (int i = 0; i < result1.length; i++)
			if (result1[i] == 1 && result2[i] == 1)
				overlap++;

		return overlap;
	}

	private int getOnBits(int[] input) {
		int onBits = 0;
		for (int i : input)
		{
			if( i == 1 )
				onBits += 1;
			else if( i != 0 )
				return -1;
		}
		return onBits;
	}
}
