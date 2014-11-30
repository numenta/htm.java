package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import gnu.trove.list.array.TIntArrayList;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

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
		
		Map<Object, Object> retVal = null;
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
		Map<Object, Object> retVal = null;
		for(int recordNum = 0;recordNum < 10;recordNum++) {
			retVal = compute(classifier, recordNum, new int[] { 1, 5 }, 0, 10);
		}
		
		assertEquals(((Object[])retVal.get("actualValues"))[0], 10.);
		assertEquals(((double[])retVal.get(0))[0], 1., .1);
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
		Map<Object, Object> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		HashSet<Object> expectedKeys = new HashSet<Object>();
		expectedKeys.add("actualValues");
		expectedKeys.add(1);
		assertEquals(expectedKeys, result.keySet());
		assertEquals(1, ((Object[])result.get("actualValues")).length);
		assertEquals(34.7, (double)((Object[])result.get("actualValues"))[0], 0.01);
	}
	
	@Test
	public void testCompute1() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		Map<Object, Object> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		HashSet<Object> expectedKeys = new HashSet<Object>();
		expectedKeys.add("actualValues");
		expectedKeys.add(1);
		assertEquals(expectedKeys, result.keySet());
		assertEquals(1, ((Object[])result.get("actualValues")).length);
		assertEquals(34.7, (double)((Object[])result.get("actualValues"))[0], 0.01);
	}
	
	@Test
	public void testCompute2() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		Map<Object, Object> result = classifier.compute(1, classification, new int[] { 1, 5, 9 }, true, true);
		
		HashSet<Object> expectedKeys = new HashSet<Object>();
		expectedKeys.add("actualValues");
		expectedKeys.add(1);
		assertEquals(expectedKeys, result.keySet());
		assertEquals(5, ((Object[])result.get("actualValues")).length);
		assertEquals(34.7, (double)((Object[])result.get("actualValues"))[4], 0.01);
	}
	
	@Test
	public void testComputeComplex() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		int recordNum = 0;
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", 34.7);
		Map<Object, Object> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
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
		
		HashSet<Object> expectedKeys = new HashSet<Object>();
		expectedKeys.add("actualValues");
		expectedKeys.add(1);
		assertEquals(expectedKeys, result.keySet());
		assertEquals(35.520000457763672, (double)((Object[])result.get("actualValues"))[4], 0.00001);
		assertEquals(42.020000457763672, (double)((Object[])result.get("actualValues"))[5], 0.00001);
		assertEquals(6, ((double[])result.get(1)).length);
		assertEquals(0.0, ((double[])result.get(1))[0], 0.00001);
		assertEquals(0.0, ((double[])result.get(1))[1], 0.00001);
		assertEquals(0.0, ((double[])result.get(1))[2], 0.00001);
		assertEquals(0.0, ((double[])result.get(1))[3], 0.00001);
		assertEquals(0.12300123, ((double[])result.get(1))[4], 0.00001);
		assertEquals(0.87699877, ((double[])result.get(1))[5], 0.00001);
	}
	
	@Test
	public void testComputeWithMissingValue() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", null);
		classification.put("actValue", null);
		Map<Object, Object> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		HashSet<Object> expectedKeys = new HashSet<Object>();
		expectedKeys.add("actualValues");
		expectedKeys.add(1);
		assertEquals(expectedKeys, result.keySet());
		assertEquals(1, ((Object[])result.get("actualValues")).length);
		assertEquals(null, ((Object[])result.get("actualValues"))[0]);
	}
	
	@Test
	public void testComputeCategory() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", "D");
		classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		Map<Object, Object> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		HashSet<Object> expectedKeys = new HashSet<Object>();
		expectedKeys.add("actualValues");
		expectedKeys.add(1);
		assertEquals(expectedKeys, result.keySet());
		assertEquals("D", ((Object[])result.get("actualValues"))[4]);
	}
	
	@Test
	public void testComputeCategory2() {
		classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", 4);
		classification.put("actValue", "D");
		classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		classification.put("actValue", "E");
		Map<Object, Object> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);
		
		HashSet<Object> expectedKeys = new HashSet<Object>();
		expectedKeys.add("actualValues");
		expectedKeys.add(1);
		assertEquals(expectedKeys, result.keySet());
		assertEquals("D", ((Object[])result.get("actualValues"))[4]);
	}

	public void checkValue(Map<Object, Object> retVal, int index, Object value, double probability) {
		assertEquals(((Object[])retVal.get("actualValues"))[index], value);
		assertEquals(probability, ((double[])retVal.get(1))[index], 0.01);
	}
	
	public Map<Object, Object> compute(CLAClassifier classifier, int recordNum, int[] pattern,
		int bucket, Object value) {
		
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", bucket);
		classification.put("actValue", value);
		return classifier.compute(recordNum, classification, pattern, true, true);
	}
}
