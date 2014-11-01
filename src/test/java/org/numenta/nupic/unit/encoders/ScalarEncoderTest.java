package org.numenta.nupic.unit.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.encoders.DecodeResult;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.EncoderResult;
import org.numenta.nupic.encoders.RangeList;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.research.Connections;
import org.numenta.nupic.research.Parameters;
import org.numenta.nupic.research.Parameters.KEY;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.Tuple;

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
		
		assertEquals("[1:8]", se.getDescription(mem).get(0).get(0));
		
		setUp();
		parameters.setName("scalar");
		initSE();
		assertEquals("scalar", se.getDescription(mem).get(0).get(0));
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
			DecodeResult decoded = se.decode(mem, output, "");
			
			System.out.println(out.append("decoding ").append(Arrays.toString(output)).append(" (").
			append(String.format("%.6f", v)).append(")=> ").append(se.decodedToStr(decoded)));
			out.setLength(0);
			
			Map<String, RangeList> fieldsMap = decoded.getFields();
			assertEquals(1, fieldsMap.size());
			RangeList ranges = (RangeList)new ArrayList<RangeList>(fieldsMap.values()).get(0);
			assertEquals(1, ranges.size());
			assertEquals(ranges.getRange(0).min(), ranges.getRange(0).max(), 0);
			assertTrue(ranges.getRange(0).min() - v < mem.getResolution());
			
			EncoderResult topDown = se.topDownCompute(mem, output).get(0);
			System.out.println("topdown => " + topDown);
			assertTrue(topDown.get(3).equals(Arrays.toString(output)));
			assertTrue(Math.abs(((double)topDown.get(1)) - v) <= mem.getResolution() / 2);
			
			//Test bucket support
			int[] bucketIndices = se.getBucketIndices(mem, v);
			System.out.println("bucket index => " + bucketIndices[0]);
			topDown = se.getBucketInfo(mem, bucketIndices).get(0);
			assertTrue(Math.abs(((double)topDown.get(1)) - v) <= mem.getResolution() / 2);
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
		DecodeResult decoded = se.decode(mem, encoded, "");
		Map<String, RangeList> fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(1, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "7.5, 7.5");
		
		//Test with something wider than w, and with a hole, and wrapped
		encoded = new int[] { 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0 };
		decoded = se.decode(mem, encoded, "");
		fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(2, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "7.5, 8.0");
		
		//Test with something wider than w, no hole
		encoded = new int[] { 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		decoded = se.decode(mem, encoded, "");
		fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(1, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "1.5, 2.5");
		
		//Test with 2 ranges
		encoded = new int[] { 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0 };
		decoded = se.decode(mem, encoded, "");
		fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(2, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "1.5, 1.5");
		assertEquals(decoded.getRanges("scalar").getRange(1).toString(), "5.5, 6.0");
		
		//Test with 2 ranges, 1 of which is narrower than w
		encoded = new int[] { 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0 };
		decoded = se.decode(mem, encoded, "");
		fieldsMap = decoded.getFields();
		assertEquals(1, fieldsMap.size());
		assertEquals(2, decoded.getRanges("scalar").size());
		assertEquals(decoded.getRanges("scalar").getRange(0).toString(), "1.5, 1.5");
		assertEquals(decoded.getRanges("scalar").getRange(1).toString(), "5.5, 6.0");
	}
	
	/**
	 * Test closenessScores for a periodic encoder
	 */
	@Test
	public void testCloseness() {
		setUp();
		parameters.setName("day of week");
		parameters.setRadius(1);
		parameters.setW(7);
		parameters.setMinVal(0);
		parameters.setMaxVal(7);
		parameters.setPeriodic(true);
		parameters.setForced(true);
		initSE();
		
		TDoubleList expValues = new TDoubleArrayList(new double[] { 2, 4, 7 });
		TDoubleList actValues = new TDoubleArrayList(new double[] { 4, 2, 1 });
		
		TDoubleList scores = se.closenessScores(mem, expValues, actValues, false);
		for(Tuple t : ArrayUtils.zip(Arrays.asList(2, 2, 1), Arrays.asList(scores.get(0)))) {
			double a = (int)t.get(0);
			double b = (double)t.get(1);
			assertTrue(a == b);
		}
	}
	
	@Test
	public void testNonPeriodicBottomUp() {
		setUp();
		parameters.setName("day of week");
		parameters.setRadius(1);
		parameters.setN(14);
		parameters.setW(5);
		parameters.setMinVal(1);
		parameters.setMaxVal(10);
		parameters.setPeriodic(false);
		parameters.setForced(true);
		initSE();
		
		System.out.println(String.format("Testing non-periodic encoder encoding resolution of ", mem.getResolution()));
		
		assertTrue(Arrays.equals(se.encode(mem, 1), new int[] { 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }));
		assertTrue(Arrays.equals(se.encode(mem, 2), new int[] { 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 }));
		assertTrue(Arrays.equals(se.encode(mem, 10), new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1 }));
		
		// Test that we get the same encoder when we construct it using resolution
	    // instead of n
		setUp();
		parameters.setName("day of week");
		parameters.setRadius(5);
		parameters.setW(5);
		parameters.setMinVal(1);
		parameters.setMaxVal(10);
		parameters.setPeriodic(false);
		parameters.setForced(true);
		initSE();
		
		double v = mem.getMinVal();
		while(v < mem.getMaxVal()) {
			int[] output = se.encode(mem, v);
			DecodeResult decoded = se.decode(mem, output, "");
			System.out.println("decoding " + Arrays.toString(output) + String.format("(%f)=>", v) + se.decodedToStr(decoded));
			
			assertEquals(decoded.getFields().size(), 1, 0);
			List<RangeList> rangeList = new ArrayList<RangeList>(decoded.getFields().values());
			assertEquals(rangeList.get(0).size(), 1, 0);
			MinMax minMax = rangeList.get(0).getRanges().get(0);
			assertEquals(minMax.min(), minMax.max(), 0);
			assertTrue(Math.abs(minMax.min() - v) <= mem.getResolution());
			
			List<EncoderResult> topDowns = se.topDownCompute(mem, output);
			EncoderResult topDown = topDowns.get(0);
			System.out.println("topDown => " + topDown);
			assertEquals(topDown.getEncoding(), Arrays.toString(output));
			assertTrue(Math.abs(((double)topDown.getValue()) - v) <= mem.getResolution());
			
			//Test bucket support
			int[] bucketIndices = se.getBucketIndices(mem, v);
			System.out.println("bucket index => " + bucketIndices[0]);
			topDown = se.getBucketInfo(mem, bucketIndices).get(0);
			assertTrue(Math.abs(((double)topDown.getValue()) - v) <= mem.getResolution() / 2);
			assertEquals(topDown.getScalar(), topDown.getValue());
			assertEquals(topDown.getEncoding(), Arrays.toString(output));
			
			// Next value
			v += mem.getResolution() / 4;
		}
		
		// Make sure we can fill in holes
		DecodeResult decoded = se.decode(mem, new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1 }, "");
		assertEquals(decoded.getFields().size(), 1, 0);
		List<RangeList> rangeList = new ArrayList<RangeList>(decoded.getFields().values());
		assertEquals(1, rangeList.get(0).size(), 0);
		System.out.println("decodedToStr of " + rangeList + " => " + se.decodedToStr(decoded));
		
		decoded = se.decode(mem, new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1 }, "");
		assertEquals(decoded.getFields().size(), 1, 0);
		rangeList = new ArrayList<RangeList>(decoded.getFields().values());
		assertEquals(1, rangeList.get(0).size(), 0);
		System.out.println("decodedToStr of " + rangeList + " => " + se.decodedToStr(decoded));
		
		// Test min and max
		setUp();
		parameters.setName("scalar");
		parameters.setW(3);
		parameters.setMinVal(1);
		parameters.setMaxVal(10);
		parameters.setPeriodic(false);
		parameters.setForced(true);
		initSE();
		
		List<EncoderResult> decode = se.topDownCompute(mem, new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1 });
		assertEquals(10, (Double)decode.get(0).getScalar(), 0);
		decode = se.topDownCompute(mem, new int[] { 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });
		assertEquals(1, (Double)decode.get(0).getScalar(), 0);
		
		// Make sure only the last and first encoding encodes to max and min, and there is no value greater than max or min
		setUp();
		parameters.setName("scalar");
		parameters.setN(140);
		parameters.setW(3);
		parameters.setMinVal(1);
		parameters.setMaxVal(141);
		parameters.setPeriodic(false);
		parameters.setForced(true);
		initSE();
		
		List<int[]> iterlist = new ArrayList<int[]>();
		for(int i = 0;i < 137;i++) {
			iterlist.add(new int[140]);
			ArrayUtils.setRangeTo(iterlist.get(i), i, i+3, 1);
			decode = se.topDownCompute(mem, iterlist.get(i));
			int value = decode.get(0).getScalar().intValue();
			assertTrue(value <= 141);
			assertTrue(value >= 1);
			assertTrue(value < 141 || i==137);
			assertTrue(value > 1 || i==0);
		}
		
		// -------------------------------------------------------------------------
	    // Test the input description generation and top-down compute on a small number
	    //   non-periodic encoder
		setUp();
		parameters.setName("scalar");
		parameters.setN(15);
		parameters.setW(3);
		parameters.setMinVal(.001);
		parameters.setMaxVal(.002);
		parameters.setPeriodic(false);
		parameters.setForced(true);
		initSE();
		
		System.out.println(String.format("\nTesting non-periodic encoder decoding resolution of %f...", mem.getResolution()));
		v = mem.getMinVal();
		while(v < mem.getMaxVal()) {
			int[] output = se.encode(mem, v);
			decoded = se.decode(mem, output, "");
			System.out.println(String.format("decoding (%f)=>", v) + " " + se.decodedToStr(decoded));
			
			assertEquals(decoded.getFields().size(), 1, 0);
			rangeList = new ArrayList<RangeList>(decoded.getFields().values());
			assertEquals(rangeList.get(0).size(), 1, 0);
			MinMax minMax = rangeList.get(0).getRanges().get(0);
			assertEquals(minMax.min(), minMax.max(), 0);
			assertTrue(Math.abs(minMax.min() - v) <= mem.getResolution());
			
			decode = se.topDownCompute(mem, output);
			System.out.println("topdown => " + decode);
			assertTrue(Math.abs((Double)decode.get(0).getScalar() - v) <= mem.getResolution() / 2);
			
			v += (mem.getResolution() / 4);
		}
		
		// -------------------------------------------------------------------------
	    // Test the input description generation on a large number, non-periodic encoder
		setUp();
		parameters.setName("scalar");
		parameters.setN(15);
		parameters.setW(3);
		parameters.setMinVal(1);
		parameters.setMaxVal(1000000000);
		parameters.setPeriodic(false);
		parameters.setForced(true);
		initSE();
		
		System.out.println(String.format("\nTesting non-periodic encoder decoding resolution of %f...", mem.getResolution()));
		v = mem.getMinVal();
		while(v < mem.getMaxVal()) {
			int[] output = se.encode(mem, v);
			decoded = se.decode(mem, output, "");
			System.out.println(String.format("decoding (%f)=>", v) + " " + se.decodedToStr(decoded));
			
			assertEquals(decoded.getFields().size(), 1, 0);
			rangeList = new ArrayList<RangeList>(decoded.getFields().values());
			assertEquals(rangeList.get(0).size(), 1, 0);
			MinMax minMax = rangeList.get(0).getRanges().get(0);
			assertEquals(minMax.min(), minMax.max(), 0);
			assertTrue(Math.abs(minMax.min() - v) <= mem.getResolution());
			
			decode = se.topDownCompute(mem, output);
			System.out.println("topdown => " + decode);
			assertTrue(Math.abs((Double)decode.get(0).getScalar() - v) <= mem.getResolution() / 2);
			
			v += (mem.getResolution() / 4);
		}
	}
}
