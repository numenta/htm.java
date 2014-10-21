package org.numenta.nupic.unit.encoders;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.EnumMap;

import org.junit.Test;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.research.Connections;
import org.numenta.nupic.research.Parameters;
import org.numenta.nupic.research.Parameters.KEY;

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

}
