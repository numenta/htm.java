package org.numenta.nupic.encoders;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.numenta.nupic.util.Tuple;

import com.oracle.jrockit.jfr.InvalidValueException;

/**
 * Unit tests for RandomDistributedScalarEncoder class
 * 
 * @author Anubhav Chaturvedi
 *
 */
public class RDSETest {

	private RDSE rdse;
	private RDSE.Builder builder;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	void setUp()
	{
		builder =  RDSE.builder()
			        .n(500)
			        .w(23)
			        .resolution(1)
			        .setOffset(0);
	}
	
	@Test
	public void testEncoding()
	{
		builder = RDSE.builder()
				.name("enc")
				.resolution(1)
				.w(23)
				.n(500)
				.setOffset(0);
		rdse = builder.build();
		
		
		int e0[] = rdse.encode(-0.1);
		assertEquals("Number of on bits is incorrect", getOnBits(e0), 23);
		assertEquals("Width of the vector is incorrect", e0.length, 500);
		assertEquals("Offset doesn't correspond to middle bucket", rdse.getBucketIndices(0)[0], 
				rdse.getMaxBuckets()/2);
		assertEquals("Number of buckets is not 1", 1, rdse.bucketMap.size());
		
		
		int e1[] = rdse.encode(1.0);
		assertEquals("Number of buckets is not 2", 2, rdse.bucketMap.size());
		assertEquals("Number of on bits is incorrect", getOnBits(e1), 23);
		assertEquals("Width of the vector is incorrect", e0.length, 500);
		assertEquals("Overlap is not equal to w-1", computeOverlap(e0, e1), 22);
		
		
		int e25[] = rdse.encode(25.0);
		assertTrue("Buckets created are not more than 23", rdse.bucketMap.size()>23);
		assertEquals("Number of on bits is incorrect", getOnBits(e1), 23);
		assertEquals("Width of the vector is incorrect", e0.length, 500);
		assertTrue("Overlap is too high", computeOverlap(e0, e25) < 4);
		
		
		assertArrayEquals("Encodings are not consistent - they have changed after new buckets "
				+ "have been created", rdse.encode(-0.1), e0);
		assertArrayEquals("Encodings are not consistent - they have changed after new buckets "
				+ "have been created", rdse.encode(1.0), e1);
	}
	
	@Test
	public void testMissingValues()
	{
		builder = RDSE.builder()
				.name("enc")
				.resolution(1);
		rdse = builder.build();
		
		int[] e2 = rdse.encode(Double.NaN);
		assertEquals(0, getOnBits(e2));
		
		int[] e1 = rdse.encode(Encoder.SENTINEL_VALUE_FOR_MISSING_DATA);
		assertEquals(0, getOnBits(e1));
	}
	
	@Test
	public void testResolution()
	{
		builder = RDSE.builder()
				.name("enc")
				.resolution(1);
		rdse = builder.build();
		
		int[] e23 = rdse.encode(23.0);
		int[] e23_1 = rdse.encode(23.1);
		int[] e22_9 = rdse.encode(22.9);
		int[] e24 = rdse.encode(24.0);
		
		assertEquals(rdse.getW(), getOnBits(e23));
		assertArrayEquals("Numbers within resolution don't have the same encoding", e23_1, e23);
		assertArrayEquals("Numbers within resolution don't have the same encoding", e22_9, e23);
		
		assertFalse("Numbers outside resolution have the same encoding", Arrays.equals(e23, e24));
		int[] e22_5 = rdse.encode(22.5);
		assertFalse("Numbers outside resolution have the same encoding", Arrays.equals(e23, e22_5));
	}
	
	@Test
	public void testMapBucketIndexToNonZeroBits(){
		builder = RDSE.builder()
				.resolution(1)
				.w(11)
				.n(150);
		rdse = builder.build();
		
		rdse.initializeBucketMap(10, null);
		
		rdse.encode(0.0);
		rdse.encode(-7.0);
		rdse.encode(7.0);
		
		assertEquals("maxBuckets exceeded", rdse.getMaxBuckets(), rdse.bucketMap.size());
		
		try {
			assertTrue("mapBucketIndexToNonZeroBits did not handle negative index", 
					areListsEqual(rdse.mapBucketIndexToNonZeroBits(-1),rdse.bucketMap.get(0)));
			assertTrue("mapBucketIndexToNonZeroBits did not handle negative index", 
					areListsEqual(rdse.mapBucketIndexToNonZeroBits(1000),rdse.bucketMap.get(9)));
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		int[] e23 = rdse.encode(23.0);
		int[] e6 = rdse.encode(6.0);
		assertArrayEquals("Values not clipped correctly during encoding", e23 , e6);
		
		int[] e_8 = rdse.encode(-8.0);
		int[] e_7 = rdse.encode(-7.0);
		assertArrayEquals("Values not clipped correctly during encoding", e_8 , e_7);
		
		assertEquals("getBucketIndices returned negative bucket index", 0,
				rdse.getBucketIndices(-8.0)[0]);
		assertEquals("getBucketIndices returned negative bucket index", rdse.getMaxBuckets()-1,
				rdse.getBucketIndices(23.0)[0]);
	}
	
	@Test
	public void testParameterChecks()
	{
		// TODO
	}
	
	@Test
	public void testOverlapStatistics()
	{
		builder = RDSE.builder()
				.resolution(1)
				.w(11)
				.n(150)
				.setSeed(RDSE.DEFAULT_SEED);
		rdse = builder.build();
		
		rdse.encode(0.0);
		rdse.encode(-300.0);
		rdse.encode(300.0);
		
		assertTrue("Illegal overlap encountered in encoder", validateEncoder(rdse, 3));
	}
	
	@Test
	public void testGetMethods()
	{
		builder = RDSE.builder()
				.name("theName")
				.resolution(1)
				.n(500);
		rdse = builder.build();
		
		assertEquals("getWidth doesn't return the correct result", 500, rdse.getWidth());
		assertEquals("getDescription doesn't return the correct result",
				new ArrayList<Tuple>(Arrays.asList(new Tuple[] { new Tuple(2, "theName", 0) })),
				rdse.getDescription());
		
		// TODO getDecoderOutputFieldTypes doesn't return the correct result
	}
	
	@Test
	public void testOffset()
	{
		builder = RDSE.builder()
				.name("enc")
				.resolution(1);
		rdse = builder.build();
		
		int[] e23 = rdse.encode(23.0);
		assertEquals("Offset not initialized to specified constructor parameter",23, rdse.getOffset(), 0);
		
		builder = RDSE.builder()
				.name("enc")
				.resolution(1)
				.setOffset(25.0);
		rdse = builder.build();
		
		e23 = rdse.encode(23.0);
		assertEquals("Offset not initialized to specified constructor parameter",25, rdse.getOffset(), 0);
	}
	
	@Test
	public void testSeed()
	{
		builder = RDSE.builder()
				.name("enc")
				.resolution(1);
		
		RDSE encoder1 = builder.setSeed(42).build();
		RDSE encoder2 = builder.setSeed(42).build();
		RDSE encoder3 = builder.setSeed(-1).build();
		RDSE encoder4 = builder.setSeed(-1).build();
		
		int[] e1 = encoder1.encode(23.0);
		int[] e2 = encoder2.encode(23.0);
		int[] e3 = encoder3.encode(23.0);
		int[] e4 = encoder4.encode(23.0);
		
		assertArrayEquals("Same seed gives rise to different encodings", e1, e2);
		assertFalse("Different seeds gives rise to same encodings", Arrays.equals(e1, e3));
		assertFalse("seeds of -1 give rise to same encodings", Arrays.equals(e4, e3));
	}
	
	@Test
	public void testCountOverlapIndices()
	{
		builder = RDSE.builder()
				.name("enc")
				.resolution(1)
				.w(5)
				.n(5*20);
		rdse = builder.build();
		
		int midIdx = rdse.getMaxBuckets()/2;
		
		rdse.bucketMap.put(midIdx-2, getRangeAsList(3, 8));
		rdse.bucketMap.put(midIdx-1, getRangeAsList(4, 9));
		rdse.bucketMap.put(midIdx, getRangeAsList(5, 10));
		rdse.bucketMap.put(midIdx+1, getRangeAsList(6, 11));
		rdse.bucketMap.put(midIdx+2, getRangeAsList(7, 12));
		rdse.bucketMap.put(midIdx+3, getRangeAsList(8, 13));
		rdse.minIndex = midIdx-2;
		rdse.maxIndex = midIdx+3;
		
		// TODO Indices must exist
		
		// Test some overlaps
		try {
			assertEquals("countOverlapIndices didn't work", 5, rdse.countOverlapIndices(midIdx-2,midIdx-2));
			assertEquals("countOverlapIndices didn't work", 4, rdse.countOverlapIndices(midIdx-1,midIdx-2));
			assertEquals("countOverlapIndices didn't work", 2, rdse.countOverlapIndices(midIdx+1,midIdx-2));
			assertEquals("countOverlapIndices didn't work", 0, rdse.countOverlapIndices(midIdx-2,midIdx+3));
		} catch (InvalidValueException e) {
			Assert.fail("countOverlapIndices raised an InvalidValueException");
			e.printStackTrace();
		}
	}
	
	@Test
	public void testOverlapOK()
	{
		builder = RDSE.builder()
				.name("enc")
				.resolution(1)
				.w(5)
				.n(5*20);
		rdse = builder.build();
		
		int midIdx = rdse.getMaxBuckets()/2;
		
		rdse.bucketMap.put(midIdx-3, getRangeAsList(4, 9));
		rdse.bucketMap.put(midIdx-2, getRangeAsList(3, 8));
		rdse.bucketMap.put(midIdx-1, getRangeAsList(4, 9));
		rdse.bucketMap.put(midIdx, getRangeAsList(5, 10));
		rdse.bucketMap.put(midIdx+1, getRangeAsList(6, 11));
		rdse.bucketMap.put(midIdx+2, getRangeAsList(7, 12));
		rdse.bucketMap.put(midIdx+3, getRangeAsList(8, 13));
		rdse.minIndex = midIdx-3;
		rdse.maxIndex = midIdx+3;
		
		try {
			assertTrue("overlapOK didn't work", rdse.overlapOK(midIdx, midIdx-1));
			assertTrue("overlapOK didn't work", rdse.overlapOK(midIdx-2, midIdx+3));
			assertFalse("overlapOK didn't work", rdse.overlapOK(midIdx-3, midIdx-1));
			
			assertTrue("overlapOK didn't work for far values", rdse.overlapOK(100, 50, 0));
			assertTrue("overlapOK didn't work for far values", rdse.overlapOK(100, 50, rdse.getMaxOverlap()));
			assertFalse("overlapOK didn't work for far values", rdse.overlapOK(100, 50, rdse.getMaxOverlap()+1));
			assertTrue("overlapOK didn't work for far values", rdse.overlapOK(50, 50, 5));
			assertTrue("overlapOK didn't work for far values", rdse.overlapOK(48, 50, 3));
			assertTrue("overlapOK didn't work for far values", rdse.overlapOK(46, 50, 1));
			assertTrue("overlapOK didn't work for far values", rdse.overlapOK(45, 50, rdse.getMaxOverlap()));
			assertFalse("overlapOK didn't work for far values", rdse.overlapOK(48, 50, 4));
			assertFalse("overlapOK didn't work for far values", rdse.overlapOK(48, 50, 2));
			assertFalse("overlapOK didn't work for far values", rdse.overlapOK(46, 50, 2));
			assertFalse("overlapOK didn't work for far values", rdse.overlapOK(50, 50, 6));
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCountOverlap()
	{
		builder = RDSE.builder()
				.name("enc")
				.resolution(1)
				.n(500);
		rdse = builder.build();
		
		int[] r1 = new int[]{1, 2, 3, 4, 5, 6};
		int[] r2 = new int[]{1, 2, 3, 4, 5, 6};
		assertEquals("countOverlap result is incorrect", 6, rdse.countOverlap(r1, r2));
		
		r1 = new int[]{1, 2, 3, 4, 5, 6};
		r2 = new int[]{1, 2, 3, 4, 5, 7};
		assertEquals("countOverlap result is incorrect", 5, rdse.countOverlap(r1, r2));
		
		r1 = new int[]{1, 2, 3, 4, 5, 6};
		r2 = new int[]{6, 5, 4, 3, 2, 1};
		assertEquals("countOverlap result is incorrect", 6, rdse.countOverlap(r1, r2));
		
		r1 = new int[]{1, 2, 8, 4, 5, 6};
		r2 = new int[]{1, 2, 3, 4, 9, 6};
		assertEquals("countOverlap result is incorrect", 4, rdse.countOverlap(r1, r2));
		
		r1 = new int[]{1, 2, 3, 4, 5, 6};
		r2 = new int[]{1, 2, 3};
		assertEquals("countOverlap result is incorrect", 3, rdse.countOverlap(r1, r2));
		
		r1 = new int[]{7, 8, 9, 10, 11, 12};
		r2 = new int[]{1, 2, 3, 4, 5, 6};
		assertEquals("countOverlap result is incorrect", 0, rdse.countOverlap(r1, r2));
	}
	
	@Test
	public void testVerbosity()
	{
		
	}
	
	@Test
	public void testEncodeInvalidInputType()
	{
		// not valid for java as it does not follow duck typing
	}
	
	private List<Integer> getRangeAsList(int lowerBound, int upperBound)
	{
		if(lowerBound>upperBound)
			return null;
		
		Integer[] arr = new Integer[upperBound-lowerBound];
		for(int i=lowerBound;i<upperBound;i++)
		{
			arr[i-lowerBound] = i;
		}
		
		return Arrays.asList(arr);
	}
	
	private boolean validateEncoder(RDSE encoder, int subsampling)
	{
		for(int i = encoder.minIndex; i<= encoder.maxIndex; i++){
			for(int j = i+1; j<= encoder.maxIndex; j+=subsampling)
			{
				try {
					if(!encoder.overlapOK(i, j))
						return false;
				} catch (InvalidValueException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	
	private int computeOverlap(int[] result1, int[] result2)
	{
		if(result1.length!=result2.length)
			return Integer.MIN_VALUE;
		
		int overlap=0;
		for(int i=0;i<result1.length;i++)
			if(result1[i]==1 && result2[i]==1)
				overlap++;
		
		return overlap;
	}

	private boolean areListsEqual(List<Integer> list1, List<Integer> list2)
	{
		if(list1==null && list2==null)
			return true;
		else if(list1==null || list2==null)
			return false;
		
		if(list1.size() != list2.size())
			return false;
		
		for(int i = 0; i<list1.size(); i++)
		{
			if(list1.get(i)!=list2.get(i))
				return false;
		}
		
		return true;
	}
	
	private int getOnBits(int[] input)
	{
		int onBits=0;
		for(int i : input)
			onBits+=i;
		return onBits;
	}
}
