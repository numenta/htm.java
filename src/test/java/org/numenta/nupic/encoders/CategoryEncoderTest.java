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

import org.junit.Test;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.MinMax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CategoryEncoderTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(CategoryEncoderTest.class);

	private CategoryEncoder ce;
	private CategoryEncoder.Builder builder;

	private void setUp() {
        builder =  ((CategoryEncoder.Builder)CategoryEncoder.builder())
            .w(3)
            .radius(0)
            .minVal(0.0)
            .maxVal(8.0)
            .periodic(false)
            .forced(true);
    }

	private void initCE() {
		ce = builder.build();
	}

	@Test
	public void testCategoryEncoder() {
		String[] categories = new String[] { "ES", "GB", "US" };

		setUp();
		builder.radius(1);
		builder.categoryList(Arrays.<String>asList(categories));
		initCE();

		LOGGER.info("Testing CategoryEncoder...");
		// forced: is not recommended, but is used here for readability. see scalar.py
		int[] output = ce.encode("US");
		assertTrue(Arrays.equals(new int[] { 0,0,0,0,0,0,0,0,0,1,1,1 }, output));

		// Test reverse lookup
		DecodeResult decoded = ce.decode(output, "");
		assertEquals(decoded.getFields().size(), 1, 0);
		List<RangeList> rangeList = new ArrayList<RangeList>(decoded.getFields().values());
		assertEquals(1, rangeList.get(0).size(), 0);
		MinMax minMax = rangeList.get(0).getRanges().get(0);
		assertEquals(minMax.min(), minMax.max(), 0);
		assertTrue(minMax.min() == 3 && minMax.max() == 3);
		LOGGER.info("decodedToStr of " + minMax + "=>" + ce.decodedToStr(decoded));

		// Test topdown compute
		for(String v : categories) {
			output = ce.encode(v);
			Encoding topDown = ce.topDownCompute(output).get(0);
			assertEquals(v, topDown.getValue());
			assertEquals((int)ce.getScalars(v).get(0), (int)topDown.getScalar().doubleValue());

			int[] bucketIndices = ce.getBucketIndices(v);
			LOGGER.info("bucket index => " + bucketIndices[0]);
			topDown = ce.getBucketInfo(bucketIndices).get(0);
			assertEquals(v, topDown.getValue());
			assertEquals((int)ce.getScalars(v).get(0), (int)topDown.getScalar().doubleValue());
			assertTrue(Arrays.equals(topDown.getEncoding(), output));
			assertEquals(topDown.getValue(), ce.getBucketValues(String.class).get(bucketIndices[0]));
		}

		//-------------
		// unknown category
		output = ce.encode("NA");
		assertTrue(Arrays.equals(new int[] { 1,1,1,0,0,0,0,0,0,0,0,0 }, output));

		// Test reverse lookup
		decoded = ce.decode(output, "");
		assertEquals(decoded.getFields().size(), 1, 0);
		rangeList = new ArrayList<RangeList>(decoded.getFields().values());
		assertEquals(1, rangeList.get(0).size(), 0);
		minMax = rangeList.get(0).getRanges().get(0);
		assertEquals(minMax.min(), minMax.max(), 0);
		assertTrue(minMax.min() == 0 && minMax.max() == 0);
		LOGGER.info("decodedToStr of " + minMax + "=>" + ce.decodedToStr(decoded));

		Encoding topDown = ce.topDownCompute(output).get(0);
		assertEquals(topDown.getValue(), "<UNKNOWN>");
		assertEquals(topDown.getScalar(), 0);

		//--------------
		// ES
		output = ce.encode("ES");
		assertTrue(Arrays.equals( new int[] {0,0,0,1,1,1,0,0,0,0,0,0 }, output));

		// MISSING VALUE
		int[] outputForMissing = ce.encode((String)null);
		assertTrue(Arrays.equals( new int[] {0,0,0,0,0,0,0,0,0,0,0,0 }, outputForMissing));

		// Test reverse lookup
		decoded = ce.decode(output, "");
		assertEquals(decoded.getFields().size(), 1, 0);
		rangeList = new ArrayList<RangeList>(decoded.getFields().values());
		assertEquals(1, rangeList.get(0).size(), 0);
		minMax = rangeList.get(0).getRanges().get(0);
		assertEquals(minMax.min(), minMax.max(), 0);
		assertTrue(minMax.min() == 1 && minMax.max() == 1);
		LOGGER.info("decodedToStr of " + minMax + "=>" + ce.decodedToStr(decoded));

		// Test topdown compute
		topDown = ce.topDownCompute(output).get(0);
		assertEquals(topDown.getValue(), "ES");
		assertEquals(topDown.getScalar(), (int)ce.getScalars("ES").get(0));

		//----------------
		// Multiple categories
		Arrays.fill(output, 1);

		// Test reverse lookup
		decoded = ce.decode(output, "");
		assertEquals(decoded.getFields().size(), 1, 0);
		rangeList = new ArrayList<RangeList>(decoded.getFields().values());
		assertEquals(1, rangeList.get(0).size(), 0);
		minMax = rangeList.get(0).getRanges().get(0);
		assertTrue(minMax.min() != minMax.max());
		assertTrue(minMax.min() == 0 && minMax.max() == 3);
		LOGGER.info("decodedToStr of " + minMax + "=>" + ce.decodedToStr(decoded));


		//----------------
		// Test with width = 1
		categories = new String[] { "cat1", "cat2", "cat3", "cat4", "cat5" };

		setUp();
		builder.radius(1);
		builder.categoryList(Arrays.<String>asList(categories));
		initCE();

		for(String cat : categories) {
			output = ce.encode(cat);
			topDown = ce.topDownCompute(output).get(0);
			LOGGER.debug(cat + "->" + Arrays.toString(output) +
					" " + ArrayUtils.where(output, new Condition.Adapter<Integer>() {
						public boolean eval(int i) { return i == 1; }
					}));
			LOGGER.debug(" scalarTopDown: " + ce.topDownCompute(output));
			LOGGER.debug(" topDown " + topDown);
			assertEquals(topDown.getValue(), cat);
			assertEquals(topDown.getScalar(), (int)ce.getScalars(cat).get(0));
		}

		//==================
		// Test with width = 9, removing some bits in the encoded output
		categories = new String[9];
		for(int i = 0;i < 9;i++) categories[i] = String.format("cat%d", i + 1);
		//forced: is not recommended, but is used here for readability.
		setUp();
		builder.radius(1);
		builder.w(9);
		builder.forced(true);
		builder.categoryList(Arrays.<String>asList(categories));
		initCE();

		for(String cat : categories) {
			output = ce.encode(cat);
			topDown = ce.topDownCompute(output).get(0);
			LOGGER.debug(cat + "->" + Arrays.toString(output) +
						" " + ArrayUtils.where(output, new Condition.Adapter<Integer>() {
					public boolean eval(int i) {
						return i == 1;
					}
			}));
			LOGGER.debug(" scalarTopDown: " + ce.topDownCompute(output));
			LOGGER.debug(" topDown " + topDown);
			assertEquals(topDown.getValue(), cat);
			assertEquals(topDown.getScalar(), (int)ce.getScalars(cat).get(0));

			// Get rid of 1 bit on the left
			int[] outputNZs = ArrayUtils.where(output, new Condition.Adapter<Integer>() {
				public boolean eval(int i) { return i == 1; }
			});
//			int[] outputPreserve = Arrays.copyOf(output, output.length);
			output[outputNZs[0]] = 0;
//			LOGGER.info("output = " + Arrays.toString(outputPreserve));
//			LOGGER.info("outputNZs = " + Arrays.toString(outputNZs));
//			LOGGER.info("outputDelta = " + Arrays.toString(output));
			topDown = ce.topDownCompute(output).get(0);
			LOGGER.debug("missing 1 bit on left: ->" + Arrays.toString(output) +
					" " + ArrayUtils.where(output, new Condition.Adapter<Integer>() {
						public boolean eval(int i) { return i == 1; }
					}));
			LOGGER.debug(" scalarTopDown: " + ce.topDownCompute(output));
			LOGGER.debug(" topDown " + topDown);
			assertEquals(topDown.getValue(), cat);
			assertEquals(topDown.getScalar(), (int)ce.getScalars(cat).get(0));

		// Get rid of 1 bit on the right
			output[outputNZs[0]] = 1;
			output[outputNZs[outputNZs.length - 1]] = 0;
			topDown = ce.topDownCompute(output).get(0);
			LOGGER.debug("missing 1 bit on right: ->" + Arrays.toString(output) +
					" " + ArrayUtils.where(output, new Condition.Adapter<Integer>() {
						public boolean eval(int i) { return i == 1; }
					}));
			LOGGER.debug(" scalarTopDown: " + ce.topDownCompute(output));
			LOGGER.debug(" topDown " + topDown);
			assertEquals(topDown.getValue(), cat);
			assertEquals(topDown.getScalar(), (int)ce.getScalars(cat).get(0));

		// Get rid of 4 bits on the left
			Arrays.fill(output, 0);
			int[] indexes = ArrayUtils.range(outputNZs[outputNZs.length - 5], outputNZs[outputNZs.length - 1] + 1);
			for(int i = 0;i < indexes.length;i++) output[indexes[i]] = 1;
			LOGGER.info(Arrays.toString(output));
			topDown = ce.topDownCompute(output).get(0);
			LOGGER.debug("missing 4 bits on left: ->" + Arrays.toString(output) +
					" " + ArrayUtils.where(output, new Condition.Adapter<Integer>() {
						public boolean eval(int i) { return i == 1; }
					}));
			LOGGER.debug(" scalarTopDown: " + ce.topDownCompute(output));
			LOGGER.debug(" topDown " + topDown);
			assertEquals(topDown.getValue(), cat);
			assertEquals(topDown.getScalar(), (int)ce.getScalars(cat).get(0));

		// Get rid of 4 bits on the right
			Arrays.fill(output, 0);
			indexes = ArrayUtils.range(outputNZs[0], outputNZs[5]);
			for(int i = 0;i < indexes.length;i++) output[indexes[i]] = 1;
			LOGGER.info(Arrays.toString(output));
			topDown = ce.topDownCompute(output).get(0);
			LOGGER.debug("missing 4 bits on left: ->" + Arrays.toString(output) +
					" " + ArrayUtils.where(output, new Condition.Adapter<Integer>() {
						public boolean eval(int i) { return i == 1; }
					}));
			LOGGER.debug(" scalarTopDown: " + ce.topDownCompute(output));
			LOGGER.debug(" topDown " + topDown);
			assertEquals(topDown.getValue(), cat);
			assertEquals(topDown.getScalar(), (int)ce.getScalars(cat).get(0));
		}

		int[] output1 = ce.encode("cat1");
		int[] output2 = ce.encode("cat9");
		output = ArrayUtils.or(output1, output2);
		topDown = ce.topDownCompute(output).get(0);
		LOGGER.debug("cat1 + cat9 ->" + Arrays.toString(output) +
				" " + ArrayUtils.where(output, new Condition.Adapter<Integer>() {
					public boolean eval(int i) { return i == 1; }
				}));
		LOGGER.debug(" scalarTopDown: " + ce.topDownCompute(output));
		LOGGER.debug(" topDown " + topDown);

		assertTrue(topDown.getScalar().equals((int)ce.getScalars("cat1").get(0)) ||
			topDown.getScalar().equals((int)ce.getScalars("cat9").get(0)));

		LOGGER.info("passed"); //Just because they did it in the Python version :-)
	}

}
