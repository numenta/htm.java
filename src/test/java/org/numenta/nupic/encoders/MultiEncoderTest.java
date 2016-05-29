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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.Tuple;

public class MultiEncoderTest {
	private MultiEncoder me;
	private MultiEncoder.Builder builder;
	
	private void setUp() {
        builder = MultiEncoder.builder().name("");
    }
	
	private void initME() {
		me = builder.build();
	}
	/**
	 * Test addition of encoders one-by-one.
	 */
	@Test
	public void testAdaptiveScalarEncoder() {
		setUp();
		initME();
		Encoder.Builder<?,?> ase = me.getBuilder("AdaptiveScalarEncoder");
		assertNotNull(ase);
		
		try {
			me.getBuilder("BogusEncoder");
			fail(); //Expect exception thrown here
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		//runMixedTests(me);
	}

	/**
	 * Test addition of encoders one-by-one.
	 */
	@Test
	public void testSerialAdditions() {
		setUp();
		initME();
		
		ScalarEncoder dow = ScalarEncoder.builder()
				.w(3)
				.resolution(1)
				.minVal(1)
				.maxVal(8)
				.periodic(true)
				.name("day of week")
				.forced(true)
				.build();
		me.addEncoder("dow", dow);
		
		ScalarEncoder myval = ScalarEncoder.builder()
				.w(5)
				.resolution(1)
				.minVal(1)
				.maxVal(10)
				.periodic(false)
				.name("aux")
				.forced(true)
				.build();
		me.addEncoder("myval", myval);
		
		runScalarTests(me);
		
		List<String> categoryList = new ArrayList<String>();
		categoryList.add("run");
		categoryList.add("pass");
		categoryList.add("kick");
		CategoryEncoder myCat = CategoryEncoder.builder()
				.radius(2)
				.w(3)
				.categoryList(categoryList)
				.forced(true)
				.build();
		
		me.addEncoder("myCat", myCat);
		
		runMixedTests(me);
	}
	
	/**
	 * Test addition of encoders all at once.
	 */
	@Test
	public void testMultipleAdditions() {
		setUp();
		initME();
		
		Map<String, Map<String, Object>> fieldEncodings = new HashMap<String, Map<String, Object>>();
		fieldEncodings.put("dow", new HashMap<String, Object>());
		fieldEncodings.get("dow").put("encoderType", "ScalarEncoder");
		fieldEncodings.get("dow").put("fieldName", "dow");
		fieldEncodings.get("dow").put("w", 3);
		fieldEncodings.get("dow").put("resolution", 1.);
		fieldEncodings.get("dow").put("minVal", 1.);
		fieldEncodings.get("dow").put("maxVal", 8.);
		fieldEncodings.get("dow").put("periodic", true);
		fieldEncodings.get("dow").put("name", "day of week");
		fieldEncodings.get("dow").put("forced", true);
		
		fieldEncodings.put("myval", new HashMap<String, Object>());
		fieldEncodings.get("myval").put("encoderType", "ScalarEncoder");
		fieldEncodings.get("myval").put("fieldName", "myval");
		fieldEncodings.get("myval").put("w", 5);
		fieldEncodings.get("myval").put("resolution", 1.);
		fieldEncodings.get("myval").put("minVal", 1.);
		fieldEncodings.get("myval").put("maxVal", 10.);
		fieldEncodings.get("myval").put("periodic", false);
		fieldEncodings.get("myval").put("name", "aux");
		fieldEncodings.get("myval").put("forced", true);
		
		me.addMultipleEncoders(fieldEncodings);
		
		runScalarTests(me);
		
		setUp();
		initME();
		
		List<String> categoryList = new ArrayList<String>();
		categoryList.add("run");
		categoryList.add("pass");
		categoryList.add("kick");
		
		fieldEncodings.put("myCat", new HashMap<String, Object>());
		fieldEncodings.get("myCat").put("encoderType", "CategoryEncoder");
		fieldEncodings.get("myCat").put("fieldName", "myCat");
		fieldEncodings.get("myCat").put("w", 3);
		fieldEncodings.get("myCat").put("radius", 2.);
		fieldEncodings.get("myCat").put("categoryList", categoryList);
		fieldEncodings.get("myCat").put("forced", true);
		
		me.addMultipleEncoders(fieldEncodings);
		runMixedTests(me);
	}
	
	@SuppressWarnings("unchecked")
	public void runScalarTests(MultiEncoder me) {
		// should be 7 bits wide
		// use of forced=true is not recommended, but here for readability, see scalar.py
		int[] expected = new int[]{0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1};
		Map<String, Object> d = new HashMap<String, Object>();
		d.put("dow",  3.);
		d.put("myval",  10.);
		int[] output = me.encode(d);

		assertTrue(Arrays.equals(expected, output));
		
		// Check decoding
		Tuple decoded = me.decode(output, "");
		Map<String, RangeList> fields = (HashMap<String, RangeList>) decoded.get(0);
		assertEquals(fields.keySet().size(), 2);
		
		MinMax minMax = fields.get("aux").getRange(0);
		assertTrue(minMax.toString().equals(new MinMax(10.0, 10.0).toString()));
		
		minMax = fields.get("day of week").getRange(0);
		assertTrue(minMax.toString().equals(new MinMax(3.0, 3.0).toString()));
	}
	
	public void runMixedTests(MultiEncoder me) {
		Map<String, Object> d = new HashMap<String, Object>();
		d.put("dow", 4.);
		d.put("myval",  6.);
		d.put("myCat", "pass");
		int[] output = me.encode(d);
		
		List<Encoding> topDownOut = me.topDownCompute(output);
		
		// When encoders are added one at a time, they're kept in the order they were added,
		// but when they're added all at once, they're sorted by name, so we need to be careful
		// here.
		ScalarEncoder dow= null, myval = null;
		CategoryEncoder myCat = null;
		Encoding dowActual = null, myvalActual = null, myCatActual = null;
		for (int i = 0; i < me.getEncoders(me).size(); i++) {
			EncoderTuple t = me.getEncoders(me).get(i);
			String name = t.getName();
			if (name.equals("dow")) {
				dow = (ScalarEncoder) t.getEncoder();
				dowActual = topDownOut.get(i);
			} else if (name.equals("myval")) {
				myval = (ScalarEncoder) t.getEncoder();
				myvalActual = topDownOut.get(i);
			} else if (name.equals("myCat")) {
				myCat = (CategoryEncoder) t.getEncoder();
				myCatActual = topDownOut.get(i);
			}
		}
		
		Encoding dowExpected = dow.topDownCompute(dow.encode(4.)).get(0);
		Encoding myvalExpected = myval.topDownCompute(myval.encode(6.)).get(0);
		Encoding myCatExpected = myCat.topDownCompute(myCat.encode("pass")).get(0);
		
		assertTrue(dowActual.equals(dowExpected));
		assertTrue(myvalActual.equals(myvalExpected));
		assertTrue(myCatActual.equals(myCatExpected));
	}
}
