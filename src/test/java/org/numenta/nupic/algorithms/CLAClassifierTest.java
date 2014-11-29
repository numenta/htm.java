package org.numenta.nupic.algorithms;

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
		
		Map<String, Object> retVal = null;
		for(int recordNum = 0;recordNum < 10;recordNum++) {
			retVal = compute(classifier, recordNum, new int[] { 1, 5 }, 0, 10);
		}
		
		checkValue(retVal, 0, 10, 1.);
	}

	public void checkValue(Map<String, Object> retVal, int index, Object value, double probability) {
		
	}
	
	public Map<String, Object> compute(CLAClassifier classifier, int recordNum, int[] pattern,
		int bucket, Object value) {
		
		Map<String, Object> classification = new LinkedHashMap<String, Object>();
		classification.put("bucketIdx", bucket);
		classification.put("actValue", value);
		return classifier.compute(recordNum, classification, pattern, true, true);
	}
}
