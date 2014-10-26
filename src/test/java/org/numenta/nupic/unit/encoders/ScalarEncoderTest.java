package org.numenta.nupic.unit.encoders;

import org.junit.Test;
import org.numenta.nupic.encoders.*;
import org.numenta.nupic.research.Connections;
import org.numenta.nupic.research.Parameters;
import org.numenta.nupic.research.Parameters.KEY;
import org.numenta.nupic.util.Tuple;

import java.util.*;

import static org.junit.Assert.*;

public class ScalarEncoderTest {
	private ScalarEncoder se;
	private Parameters parameters;
	private Connections mem;
	
	private void setUp() {
		parameters = new Parameters();
        EnumMap<Parameters.KEY, Object> p = parameters.getMap();
        p.put(KEY.N, 14);
        p.put(KEY.W, 3);
        p.put(KEY.RADIUS, 0);//3
        p.put(KEY.MINVAL, 1);
        p.put(KEY.MAXVAL, 8);
        p.put(KEY.PERIODIC, true);
        p.put(KEY.FORCED, true);
    }
	
	private void initSE() {
		se = new ScalarEncoder();
		mem = new Connections();
		Parameters.apply(mem, parameters);
		se.init(mem);
	}
	
	@Test
	public void testScalarEncoder() {
		setUp();
		initSE();
		
		int[] empty = se.encode(mem, Encoder.SENTINEL_VALUE_FOR_MISSING_DATA);
		System.out.println("\nEncoded missing data as: " + Arrays.toString(empty));
		int[] expected = new int[14];
		assertTrue(Arrays.equals(expected, empty));
	}
	
	@Test
	public void testBottomUpEncodingPeriodicEncoder() {
		setUp();
		initSE();
		
		assertEquals("[1:8]", se.getDescription(mem));
		
		setUp();
		parameters.setName("scalar");
		initSE();
		assertEquals("scalar", se.getDescription(mem));
		int[] res = se.encode(mem, 3);
		assertTrue(Arrays.equals(new int[] { 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, res));
		
		res = se.encode(mem, 3.1);
		assertTrue(Arrays.equals(new int[] { 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 }, res));
		
		res = se.encode(mem, 3.5);
		assertTrue(Arrays.equals(new int[] { 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 }, res));
		
		res = se.encode(mem, 3.6);
		assertTrue(Arrays.equals(new int[] { 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 }, res));
		
		res = se.encode(mem, 3.7);
		assertTrue(Arrays.equals(new int[] { 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 }, res));
		
		res = se.encode(mem, 4);
		assertTrue(Arrays.equals(new int[] { 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 }, res));
		
		res = se.encode(mem, 1);
		assertTrue(Arrays.equals(new int[] { 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }, res));

		res = se.encode(mem, 1.5);
		assertTrue(Arrays.equals(new int[] { 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, res));
		
		res = se.encode(mem, 7);
		assertTrue(Arrays.equals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, res));
		
		res = se.encode(mem, 7.5);
		assertTrue(Arrays.equals(new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1 }, res));
		
		assertEquals(0.5d, mem.getResolution(), 0);
		assertEquals(1.5d, mem.getRadius(), 0);
	}

	/**
	 * Test that we get the same encoder when we construct it using resolution
     * instead of n
	 */
	@Test
	public void testCreateResolution() {
		setUp();
		initSE();
		List<Tuple> dict = se.dict(mem);
		
		setUp();
		parameters.setResolution(0.5);
		initSE();
		List<Tuple> compare = se.dict(mem);
		assertEquals(dict, compare);
		
		setUp();
		parameters.setRadius(1.5);
		initSE();
		compare = se.dict(mem);
		assertEquals(dict, compare);
		
		//Negative test
		setUp();
		parameters.setResolution(0.5);
		initSE();
		mem.setName("break this");
		compare = se.dict(mem);
		assertFalse(dict.equals(compare));
	}
	
	/**
	 * Test the input description generation, top-down compute, and bucket
     * support on a periodic encoder
     */
	@Test
	public void testDecodeAndResolution() {
		setUp();
		parameters.setName("scalar");
		initSE();
		double resolution = mem.getResolution();
		System.out.println("resolution = " +resolution);
		StringBuilder out = new StringBuilder();
		for(double v = mem.getMinVal();v < mem.getMaxVal();v+=(resolution / 4.0d)) {
			int[] output = se.encode(mem, v);
			Decode decoded = se.decode(mem, output, "");
			
			System.out.println(out.append("decoding ").append(Arrays.toString(output)).append(" (").
			append(String.format("%.6f", v)).append(")=> ").append(se.decodedToStr(decoded)));
			out.setLength(0);
			
			Map<String, Ranges> fieldsMap = decoded.getFields();
			assertEquals(1, fieldsMap.size());
			Ranges ranges = (Ranges)new ArrayList<Ranges>(fieldsMap.values()).get(0);
			assertEquals(1, ranges.size());
			assertEquals(ranges.getRange(0).min(), ranges.getRange(0).max(), 0);
			assertTrue(ranges.getRange(0).min() - v < mem.getResolution());
			
			EncoderResult topDown = se.topDownCompute(mem, output);
			System.out.println("topdown => " + topDown);
			assertTrue(topDown.get(3).equals(Arrays.toString(output)));
			assertTrue(Math.abs(((Double)topDown.get(1)) - v) <= mem.getResolution() / 2);
			
			//Test bucket support
			int[] bucketIndices = se.getBucketIndices(mem, v);
			System.out.println("bucket index => " + bucketIndices[0]);
			topDown = se.getBucketInfo(mem, bucketIndices);
			assertTrue(Math.abs(((Double)topDown.get(1)) - v) <= mem.getResolution() / 2);
			assertEquals(topDown.get(1), se.getBucketValues(mem).toArray()[bucketIndices[0]]);
			assertEquals(topDown.get(2), topDown.get(1));
			assertTrue(topDown.get(3).equals(Arrays.toString(output)));
		}
		
		// -----------------------------------------------------------------------
	    // Test the input description generation on a large number, periodic encoder
		setUp();
		parameters.setName("scalar");
		parameters.setRadius(1.5);
		parameters.setW(3);
		parameters.setMinVal(1);
		parameters.setMaxVal(8);
		parameters.setPeriodic(true);
		parameters.setForced(true);
		initSE();
		
		System.out.println("\nTesting periodic encoder decoding, resolution of " + mem.getResolution());
		
		//Test with a "hole"
		int[] encoded = new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 };
		Decode decoded = se.decode(mem, encoded, "");
		Map<String, Ranges> fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(1, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "[7.5, 7.5]");
		
		//Test with something wider than w, and with a hole, and wrapped
		encoded = new int[] { 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 };
		decoded = se.decode(mem, encoded, "");
		fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(2, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "[7.5, 8.0]");
		
		//Test with something wider than w, no hole
		encoded = new int[] { 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		decoded = se.decode(mem, encoded, "");
		fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(1, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "[1.5, 2.5]");
		
		//Test with 2 ranges
		encoded = new int[] { 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0 };
		decoded = se.decode(mem, encoded, "");
		fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(2, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "[1.5, 1.5]");
		assertEquals(decoded.getRanges("scalar").getRange(1).toString(), "[5.5, 6.0]");
		
		//Test with 2 ranges, 1 of which is narrower than w
		encoded = new int[] { 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0 };
		decoded = se.decode(mem, encoded, "");
		fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(2, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "[1.5, 1.5]");
		assertEquals(decoded.getRanges("scalar").getRange(1).toString(), "[5.5, 6.0]");
	}
}
