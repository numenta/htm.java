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

package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.network.Persistence;
import org.numenta.nupic.network.PersistenceAPI;
import org.numenta.nupic.serialize.SerialConfig;

import gnu.trove.list.array.TIntArrayList;

public class CLAClassifierTest {
	private CLAClassifier classifier;
	
	public void setUp() {
		classifier = new CLAClassifier();
	}
	
	/**
	 * Send same value 10 times and expect 100% likelihood for prediction.
	 */
	@Test
	public void testSingleValue() {
		setUp();
		
		Classification<Double> retVal = null;
		for(int recordNum = 0;recordNum < 10;recordNum++) {
			retVal = compute(classifier, recordNum, new int[] { 1, 5 }, 0, 10);
		}
		
		checkValue(retVal, 0, 10., 1.);
	}
	
	/**
	 * Send same value 10 times and expect 100% likelihood for prediction 
     * using 0-step ahead prediction
	 */
	@Test
	public void testSingleValue0Steps() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 0 }), 0.001, 0.3, 0);
		
		// Enough times to perform Inference and learn associations
		Classification<Double> retVal = null;
		for(int recordNum = 0;recordNum < 10;recordNum++) {
			retVal = compute(classifier, recordNum, new int[] { 1, 5 }, 0, 10);
		}
		
		assertEquals(10., retVal.getActualValue(0), .00001);
		assertEquals(1., retVal.getStat(0, 0), .00001);
	}
	
	/**
	 * The meaning of this test is diminished in Java, because Java is already strongly typed and 
	 * all expected value types are known and previously declared.
	 */
	@Test
	public void testComputeResultTypes() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		Classification<Double> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
		assertEquals(1, result.getActualValueCount());
		assertEquals(34.7, result.getActualValue(0), 0.01);
	}
	
	@Test
	public void testCompute1() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		Classification<Double> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
		assertEquals(1, result.getActualValueCount());
		assertEquals(34.7, result.getActualValue(0), 0.01);
	}
	
	@Test
	public void testCompute2() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		Classification<Double> result = classifier.compute(1, classification, new int[] { 1, 5, 9 }, true, true);
		
		assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
		assertEquals(5, result.getActualValueCount());
		assertEquals(34.7, result.getActualValue(4), 0.01);
	}
	
	@Test
	public void testComputeComplex() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		int recordNum = 0;
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 5);
		classification.put("actValue", 41.7);
		result = classifier.compute(recordNum, classification, new int[] { 0, 6, 9, 11 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 5);
		classification.put("actValue", 44.9);
		result = classifier.compute(recordNum, classification, new int[] { 6, 9 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 4);
		classification.put("actValue", 42.9);
		result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
		recordNum += 1;
		
		assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
		assertEquals(35.520000457763672, result.getActualValue(4), 0.00001);
		assertEquals(42.020000457763672, result.getActualValue(5), 0.00001);
		assertEquals(6, result.getStatCount(1));
		assertEquals(0.0, result.getStat(1, 0), 0.00001);
		assertEquals(0.0, result.getStat(1, 1), 0.00001);
		assertEquals(0.0, result.getStat(1, 2), 0.00001);
		assertEquals(0.0, result.getStat(1, 3), 0.00001);
		assertEquals(0.12300123, result.getStat(1, 4), 0.00001);
		assertEquals(0.87699877, result.getStat(1, 5), 0.00001);
	}
	
	@Test
	public void testComputeWithMissingValue() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", null);
		classification.put("actValue", null);
		Classification<Double> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
		assertEquals(1, result.getActualValueCount());
		assertEquals(null, result.getActualValue(0));
	}
	
	@Test
	public void testComputeCategory() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", "D");
		classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		Classification<String> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
		assertEquals("D", result.getActualValue(4));
	}
	
	@Test
	public void testComputeCategory2() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", "D");
		classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		classification.put("actValue", "E");
		Classification<String> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
		assertEquals("D", result.getActualValue(4));
	}
	
	@Test
	public void testSerialization() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		int recordNum = 0;
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 5);
		classification.put("actValue", 41.7);
		result = classifier.compute(recordNum, classification, new int[] { 0, 6, 9, 11 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 5);
		classification.put("actValue", 44.9);
		result = classifier.compute(recordNum, classification, new int[] { 6, 9 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 4);
		classification.put("actValue", 42.9);
		result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
		recordNum += 1;
		
		// Configure serializer
		SerialConfig config = new SerialConfig("testSerializeClassifier", SerialConfig.SERIAL_TEST_DIR);
        
        PersistenceAPI api = Persistence.get(config);
        
        // 1. serialize
        byte[] data = api.write(classifier, "testSerializeClassifier");

        // 2. deserialize
        CLAClassifier serialized = api.read(data);
		
		//Using the deserialized classifier, continue test
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		result = serialized.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
		recordNum += 1;
		
		assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
		assertEquals(35.520000457763672, result.getActualValue(4), 0.00001);
		assertEquals(42.020000457763672, result.getActualValue(5), 0.00001);
		assertEquals(6, result.getStatCount(1));
		assertEquals(0.0, result.getStat(1, 0), 0.00001);
		assertEquals(0.0, result.getStat(1, 1), 0.00001);
		assertEquals(0.0, result.getStat(1, 2), 0.00001);
		assertEquals(0.0, result.getStat(1, 3), 0.00001);
		assertEquals(0.12300123, result.getStat(1, 4), 0.00001);
		assertEquals(0.87699877, result.getStat(1, 5), 0.00001);
	}
	
	@Test
	public void testOverlapPattern() {
		setUp();
		
		Classification<Double> result = compute(classifier, 0, new int[] { 1, 5 }, 9, 9);
		result = compute(classifier, 1, new int[] { 1, 5 }, 9, 9);
		result = compute(classifier, 1, new int[] { 1, 5 }, 9, 9);
		result = compute(classifier, 2, new int[] { 3, 5 }, 2, 2);
		
		// Since overlap - should be previous with 100%
		checkValue(result, 9, 9., 1.0);
		
		result = compute(classifier, 3, new int[] { 3, 5 }, 2, 2);
		
		// Second example: now new value should be more probable than old
		assertTrue(result.getStat(1, 2) > result.getStat(1, 9));
	}
	
	public void testScaling() {
		setUp();
		
		int recordNum = 0;
		for(int i = 0;i < 100;i++, recordNum++) {
			compute(classifier, recordNum, new int[] { 1 }, 5, 5);
		}
		for(int i = 0;i < 1000;i++, recordNum++) {
			compute(classifier, recordNum, new int[] { 2 }, 9, 9);
		}
		for(int i = 0;i < 3;i++, recordNum++) {
			compute(classifier, recordNum, new int[] { 1, 2 }, 6, 6);
		}
	}
	
	@Test
	public void testMultistepSingleValue() {
		setUp();
		classifier.steps = new TIntArrayList(new int[] { 1, 2 });
		
		// Only should return one actual value bucket.
		Classification<Double> result = null;
		int recordNum = 0;
		for(int i = 0;i < 10;i++, recordNum++) {
			result = compute(classifier, recordNum, new int[] { 1, 5 }, 0, 10);
		}
		
		assertTrue(Arrays.equals(new Object[] { 10. }, result.getActualValues()));
		// Should have a probability of 100% for that bucket.
		assertTrue(Arrays.equals(new double[] { 1. }, result.getStats(1)));
		assertTrue(Arrays.equals(new double[] { 1. }, result.getStats(2)));
	}
	
	@Test
	public void testMultistepSimple() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1, 2 }), 0.001, 0.3, 0);
		
		Classification<Double> result = null;
		int recordNum = 0;
		for(int i = 0;i < 100;i++, recordNum++) {
			result = compute(classifier, recordNum, new int[] { i % 10 }, i % 10, (i % 10) * 10);
		}
		
		// Only should return one actual value bucket.
		assertTrue(Arrays.equals(new Object[] { 0., 10., 20., 30., 40., 50., 60., 70., 80., 90. }, result.getActualValues()));
		assertEquals(1.0, result.getStat(1, 0), 0.1);
		for(int i = 1;i < 10;i++) {
			assertEquals(0.0, result.getStat(1, i), 0.1);
		}
		assertEquals(1.0, result.getStat(2, 1), 0.1);
	}
	
	/**
	 * Test missing record support.
	 *
     * Here, we intend the classifier to learn the associations:
     *   [1,3,5] => bucketIdx 1
     *   [2,4,6] => bucketIdx 2
     *   [7,8,9] => don't care
	 *
     *  If it doesn't pay attention to the recordNums in this test, it will learn the
     *  wrong associations.
	 */
	@Test
	public void testMissingRecords() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		int recordNum = 0;
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 0);
		classification.put("actValue", 0);
		classifier.compute(recordNum, classification, new int[] { 1, 3, 5 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 1);
		classification.put("actValue", 1);
		classifier.compute(recordNum, classification, new int[] { 2, 4, 6 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 2);
		classification.put("actValue", 2);
		classifier.compute(recordNum, classification, new int[] { 1, 3, 5 }, true, true);
		recordNum += 1;
		
		classification.put("bucketIdx", 1);
		classification.put("actValue", 1);
		classifier.compute(recordNum, classification, new int[] { 2, 4, 6 }, true, true);
		recordNum += 1;
		
		// ----------------------------------------------------------------------------------
		// At this point, we should have learned [1, 3, 5] => bucket 1
		//                                       [2, 4, 6] => bucket 2
		classification.put("bucketIdx", 2);
		classification.put("actValue", 2);
		Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 3, 5 }, true, true);
		recordNum += 1;
		assertEquals(0.0, result.getStat(1, 0), 0.00001);
		assertEquals(1.0, result.getStat(1, 1), 0.00001);
		assertEquals(0.0, result.getStat(1, 2), 0.00001);
		
		classification.put("bucketIdx", 1);
		classification.put("actValue", 1);
		result = classifier.compute(recordNum, classification, new int[] { 2, 4, 6 }, true, true);
		recordNum += 1;
		assertEquals(0.0, result.getStat(1, 0), 0.00001);
		assertEquals(0.0, result.getStat(1, 1), 0.00001);
		assertEquals(1.0, result.getStat(1, 2), 0.00001);
		
		
		// ----------------------------------------------------------------------------------
		// Feed in records that skip and make sure they don't mess up what we learned
		//
		// If we skip a record, the CLA should NOT learn that [2,4,6] from
		// the previous learning associates with bucket 0
		recordNum += 1; // <----- Does the skip
		
		classification.put("bucketIdx", 0);
		classification.put("actValue", 0);
		result = classifier.compute(recordNum, classification, new int[] { 1, 3, 5 }, true, true);
		recordNum += 1;
		assertEquals(0.0, result.getStat(1, 0), 0.00001);
		assertEquals(1.0, result.getStat(1, 1), 0.00001);
		assertEquals(0.0, result.getStat(1, 2), 0.00001);
		
		// If we skip a record, the CLA should NOT learn that [1,3,5] from
		// the previous learning associates with bucket 0
		recordNum += 1; // <----- Does the skip
		
		classification.put("bucketIdx", 0);
		classification.put("actValue", 0);
		result = classifier.compute(recordNum, classification, new int[] { 2, 4, 6 }, true, true);
		recordNum += 1;
		assertEquals(0.0, result.getStat(1, 0), 0.00001);
		assertEquals(0.0, result.getStat(1, 1), 0.00001);
		assertEquals(1.0, result.getStat(1, 2), 0.00001);
		
		// If we skip a record, the CLA should NOT learn that [2,4,6] from
		// the previous learning associates with bucket 0
		recordNum += 1; // <----- Does the skip
		
		classification.put("bucketIdx", 0);
		classification.put("actValue", 0);
		result = classifier.compute(recordNum, classification, new int[] { 1, 3, 5 }, true, true);
		recordNum += 1;
		assertEquals(0.0, result.getStat(1, 0), 0.00001);
		assertEquals(1.0, result.getStat(1, 1), 0.00001);
		assertEquals(0.0, result.getStat(1, 2), 0.00001);
	}
	
	/**
	 * Test missing record edge TestCase
     * Test an edge case in the classifier initialization when there is a missing
     * record in the first n records, where n is the # of prediction steps.
	 */
	@Test
	public void testMissingRecordInitialization() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 2 }), 0.1, 0.1, 0);
		int recordNum = 0;
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 0);
		classification.put("actValue", 34.7);
		classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
		
		recordNum = 2;
		Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
		
		assertTrue(Arrays.equals(new int[] { 2 }, result.stepSet()));
		assertEquals(1, result.getActualValueCount());
		assertEquals(34.7, result.getActualValue(0), 0.01);
	}

	public void checkValue(Classification<?> retVal, int index, Object value, double probability) {
		assertEquals(retVal.getActualValue(index), value);
		assertEquals(probability, retVal.getStat(1, index), 0.01);
	}
	
	public <T> Classification<T> compute(CLAClassifier classifier, int recordNum, int[] pattern,
		int bucket, Object value) {
		
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", bucket);
		classification.put("actValue", value);
		return classifier.compute(recordNum, classification, pattern, true, true);
	}
}
