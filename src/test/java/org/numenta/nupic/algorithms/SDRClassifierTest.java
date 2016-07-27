package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.numenta.nupic.network.Persistence;
import org.numenta.nupic.network.PersistenceAPI;
import org.numenta.nupic.serialize.SerialConfig;

import com.cedarsoftware.util.DeepEquals;

import gnu.trove.list.array.TIntArrayList;

public class SDRClassifierTest {
    private SDRClassifier classifier;

    /**
     * Send same value 10 times and expect 100% likelihood for prediction.
     */
    @Test
    public void testSingleValue() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 1.0, 0.3, 0);

        // Enough times to perform Inference and expect high likelihood for prediction.
        Classification<Double> retVal = null;
        for(int recordNum = 0; recordNum < 10; recordNum++) {
            retVal = compute(classifier, recordNum, new int[] {1, 5}, 0, 10);
        }

        assertEquals(10.0, retVal.getActualValue(0), 0.0);
        assertEquals(1.0, retVal.getStat(1, 0), 0.1);
    }

    @Test
    /**
     *  Send same value 10 times and expect high likelihood for prediction
     *  using 0-step ahead prediction.
     */
    public void testSingleValue0Steps() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 0 }), 1.0, 0.3, 0);

        // Enough times to perform Inference and learn associations
        Classification<Double> retVal = null;
        for(int recordNum = 0; recordNum < 10; recordNum++) {
            retVal = compute(classifier, recordNum, new int[] {1, 5}, 0, 10);
        }

        assertEquals(10, retVal.getActualValue(0), 0.0);
        assertEquals(retVal.getStat(0, 0), 1.0, 0.01);
    }

    /**
     * The meaning of this test is diminished in Java, because Java is already strongly typed and
     * all expected value types are known and previously declared.
     */
    @Test
    public void testComputeResultTypes() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
        Map<String, Object> classification = new LinkedHashMap<String, Object>();
        classification.put("bucketIdx", 4);
        classification.put("actValue", 34.7);
        Classification<Double> result = classifier.compute(0, classification, new int[] { 1, 5, 9 }, true, true);

        assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
        assertEquals(1, result.getActualValueCount());
        assertEquals(34.7, result.getActualValue(0), 0.00001);
    }

    @Test
    public void testComputeInferOrLearnOnly() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 1.0, 0.1, 0);

        // learn only
        int recordNum = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        classification.put("bucketIdx", 4);
        classification.put("actValue", 34.7);
        Classification<Double> retVal = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, false);

        assertTrue(retVal == null);

        // infer only
        recordNum = 0;
        classification.put("bucketIdx", 2);
        classification.put("actValue", 14.2);
        Classification<Double> retVal1 = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, false, true);
        recordNum += 1;

        classification.put("bucketIdx", 3);
        classification.put("actValue", 20.5);
        Classification<Double> retVal2 = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, false, true);
        recordNum += 1;

        // Since learning was turned off and pattern was the same, predDist should
        // be same for previous two computes.
        assertArrayEquals(retVal1.getStats(1), retVal2.getStats(1), 0);

        // return null when learn and infer are both false
        classification.put("bucketIdx", 2);
        classification.put("actValue", 14.2);
        Classification<Double> retVal3 = classifier.compute(recordNum, classification, new int[] { 1, 2 }, false, false);

        assertTrue(retVal3 == null);
    }

    @Test
    public void testCompute1() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
        int recordNum = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        classification.put("bucketIdx", 4);
        classification.put("actValue", 34.7);
        Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);

        assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
        assertEquals(1, result.getActualValueCount());
        assertEquals(34.7, result.getActualValue(0), 0.00001);
    }

    @Test
    public void testCompute2() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
        int recordNum = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        classification.put("bucketIdx", 4);
        classification.put("actValue", 34.7);
        classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
        recordNum += 1;
        Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);

        assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
        assertEquals(34.7, result.getActualValue(4), 0.00001);
    }

    @Test
    public void testComputeComplex() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 1.0, 0.1, 0);
        int recordNum = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        classification.put("bucketIdx", 4);
        classification.put("actValue", 34.7);
        classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
        recordNum += 1;

        classification.put("bucketIdx", 5);
        classification.put("actValue", 41.7);
        classifier.compute(recordNum, classification, new int[] { 0, 6, 9, 11 }, true, true);
        recordNum += 1;

        classification.put("bucketIdx", 5);
        classification.put("actValue", 44.9);
        classifier.compute(recordNum, classification, new int[] { 6, 9 }, true, true);
        recordNum += 1;

        classification.put("bucketIdx", 4);
        classification.put("actValue", 42.9);
        classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
        recordNum += 1;

        classification.put("bucketIdx", 4);
        classification.put("actValue", 34.7);
        Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);

        assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
        assertEquals(35.520000457763672, result.getActualValue(4), 0.00001);
        assertEquals(42.020000457763672, result.getActualValue(5), 0.00001);
        assertEquals(6, result.getStatCount(1));
        assertEquals(0.034234, result.getStat(1, 0), 0.00001);
        assertEquals(0.034234, result.getStat(1, 1), 0.00001);
        assertEquals(0.034234, result.getStat(1, 2), 0.00001);
        assertEquals(0.034234, result.getStat(1, 3), 0.00001);
        assertEquals(0.093058, result.getStat(1, 4), 0.00001);
        assertEquals(0.770004, result.getStat(1, 5), 0.00001);
    }

    @Test
    public void testComputeWithMissingValue() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
        int recordNum = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        classification.put("bucketIdx", null);
        classification.put("actValue", null);
        Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);

        assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
        assertEquals(1, result.getActualValueCount());
        assertEquals(null, result.getActualValue(0));
    }

    @Test
    public void testComputeCategory() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
        int recordNum = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        classification.put("bucketIdx", 4);
        classification.put("actValue", "D");
        classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
        recordNum += 1;
        classification.put("bucketIdx", 4);
        classification.put("actValue", "D");
        Classification<String> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);

        assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
        assertEquals("D", result.getActualValue(4));

        recordNum += 1;
        classification.put("bucketIdx", 5);
        classification.put("actValue", null);
        Classification<String> predictResult = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);

        for(int i = 0; i < predictResult.getActualValueCount(); i++) {
            assertTrue(predictResult.getActualValue(i) == null ||
                    predictResult.getActualValue(i).getClass().equals(String.class));
        }
    }

    @Test
    public void testComputeCategory2() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
        int recordNum = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        classification.put("bucketIdx", 4);
        classification.put("actValue", "D");
        classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);
        recordNum += 1;

        classification.put("bucketIdx", 4);
        classification.put("actValue", "E");
        Classification<String> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);

        assertTrue(Arrays.equals(new int[] { 1 }, result.stepSet()));
        assertEquals("D", result.getActualValue(4));
    }

    @Test
    public void testOverlapPattern() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 10.0, 0.3, 0);

        compute(classifier, 0, new int[] { 1, 5 }, 9, 9);
        compute(classifier, 1, new int[] { 1, 5 }, 9, 9);
        Classification<Double> retVal = compute(classifier, 2, new int[] { 3, 5 }, 2, 2.0);

        // Since overlap - should be previous with high likelihood
        assertEquals(9.0, retVal.getActualValue(9), 0.0);
        assertTrue(retVal.getStat(1, 9) > 0.9);

        retVal = compute(classifier, 3,  new int[] { 3, 5 }, 2, 2);

        // Second example: now new value should be more probable than old
        assertTrue(retVal.getStat(1, 2) > retVal.getStat(1, 9));
    }

    @Test
    public void testMultistepSingleValue() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1, 2 }), 0.001, 0.3, 0);

        Classification<Double> retVal = null;
        for(int recordNum = 0; recordNum < 10; recordNum++) {
            retVal = compute(classifier, recordNum, new int[] { 1, 5 }, 0, 10);
        }

        //Only should return one actual value bucket.
        assertEquals(10, retVal.getActualValue(retVal.getMostProbableBucketIndex(1)), 0.0);
        assertTrue(Arrays.equals(new Double[] { 10.0 }, retVal.getActualValues()));

        //Should have a probability of 100% for that bucket.
        assertEquals(1.0, retVal.getStat(1, 0), 0.0);
        assertEquals(1.0, retVal.getStat(2, 0), 0.0);
    }

    @Test
    public void testMultiStepSimple() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1, 2 }), 10.0, 0.3, 0);

        Classification<Double> retVal = null;
        int recordNum = 0;
        for(int i = 0; i < 100; i++) {
            retVal = compute(classifier, recordNum, new int[] { i % 10 }, i % 10, (i % 10)*10);
            recordNum += 1;
        }

        //Only should return one actual value bucket.
        assertArrayEquals(new Double[] { 0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0 },
                retVal.getActualValues());

        assertTrue(retVal.getStat(1, 0) > 0.99);
        for(int i = 1; i < 10; i++) {
            assertTrue(retVal.getStat(1, i) < 0.01);
        }

        assertTrue(retVal.getStat(2, 1) > 0.99);
        for(int i = 0; i < 10; i++) {
            if(i == 1) continue;
            assertTrue(retVal.getStat(2, i) < 0.01);
        }
    }

    @Test
    /**
     * Test missing record support.
     *
     * Here, we intend the classifier to learn the associations:
     *  [1,3,5] => bucketIdx 1
     *  [2,4,6] => bucketIdx 2
     *  [7,8,9] => don't care
     *
     * If it doesn't pay attention to the recordNums in this test, it
     * will learn the wrong associations.
     */
    public void testMissingRecords() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 1.0, 0.1, 0);

        int recordNum = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
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

        // -----------------------------------------------------------------------
        // At this point, we should have learned [1,3,5] => bucket 1
        //                                       [2,4,6] => bucket 2
        classification.put("bucketIdx", 2);
        classification.put("actValue", 2);
        Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 3, 5 }, true, true);
        recordNum += 1;

        assertTrue(result.getStat(1, 0) < 0.1);
        assertTrue(result.getStat(1, 1) > 0.9);
        assertTrue(result.getStat(1, 2) < 0.1);

        classification.put("bucketIdx", 1);
        classification.put("actValue", 1);
        result = classifier.compute(recordNum, classification, new int[] { 2, 4, 6 }, true, true);
        recordNum += 1;

        assertTrue(result.getStat(1, 0) < 0.1);
        assertTrue(result.getStat(1, 1) < 0.1);
        assertTrue(result.getStat(1, 2) > 0.9);

        // -----------------------------------------------------------------------
        // Feed in records that skip and make sure they don't mess up
        // what we learned.
        // If we skip a record, the SDRClassifier should NOT learn that [2,4,6]
        // from the previous learn associates with bucket 0
        recordNum += 1;
        classification.put("bucketIdx", 0);
        classification.put("actValue", 0);
        result = classifier.compute(recordNum, classification, new int[] { 1, 3, 5 }, true, true);
        recordNum += 1;

        assertTrue(result.getStat(1, 0) < 0.1);
        assertTrue(result.getStat(1, 1) > 0.9);
        assertTrue(result.getStat(1, 2) < 0.1);

        // If we skip a record, the SDRClassifier should NOT learn that [1,3,5]
        // from the previous learn associates with bucket 0
        recordNum += 1;
        classification.put("bucketIdx", 0);
        classification.put("actValue", 0);
        result = classifier.compute(recordNum, classification, new int[] { 2, 4, 6 }, true, true);
        recordNum += 1;

        assertTrue(result.getStat(1, 0) < 0.1);
        assertTrue(result.getStat(1, 1) < 0.1);
        assertTrue(result.getStat(1, 2) > 0.9);

        // If we skip a record, the SDRClassifier should NOT learn that [2,4,6]
        // from the previous learn associates with bucket 0
        recordNum += 1;
        classification.put("bucketIdx", 0);
        classification.put("actValue", 0);
        result = classifier.compute(recordNum, classification, new int[] { 1, 3, 5 }, true, true);
        recordNum += 1;

        assertTrue(result.getStat(1, 0) < 0.1);
        assertTrue(result.getStat(1, 1) > 0.9);
        assertTrue(result.getStat(1, 2) < 0.1);
    }

    @Test
    /**
     * Test missing record edge TestCase
     * Test an edge case in the classifier initialization when there is a
     * missing record in the first n records, where n is the # of prediction steps.
     */
    public void testMissingRecordInitialization() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 2 }), 0.1, 0.1, 0);

        int recordNum = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        classification.put("bucketIdx", 0);
        classification.put("actValue", 34.7);
        classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);

        recordNum = 2;
        classification.put("bucketIdx", 0);
        classification.put("actValue", 34.7);
        Classification<Double> result = classifier.compute(recordNum, classification, new int[] { 1, 5, 9 }, true, true);

        assertTrue(Arrays.equals(new int[] { 2 }, result.stepSet()));
        assertEquals(1, result.getStepCount());
        assertEquals(34.7, result.getActualValue(0), 0.01);
    }

    @Test
    /**
     * Test the distribution of predictions.
     *
     * Here, we intend the classifier to learn the associations:
     *  [1,3,5] => bucketIdx 0 (30%)
     *          => bucketIdx 1 (30%)
     *          => bucketIdx 2 (40%)
     *
     *  [2,4,6] => bucketIdx 1 (50%)
     *          => bucketIdx 3 (50%)
     *
     * The classifier should get the distribution almost right given
     * enough repetitions and a small learning rate.
     */
    public void testPredictionDistribution() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 0 }), 0.001, 0.1, 0);

        int[] SDR1 = {1, 3, 5};
        int[] SDR2 = {2, 4, 5};
        int recordNum = 0;
        int bucketIdx = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        Random random = new Random(42);
        for(int i = 0; i < 5000; i++) {
            double randomNumber = random.nextDouble();
            if(randomNumber < 0.3) {
                bucketIdx = 0;
            }
            else if(randomNumber < 0.6) {
                bucketIdx = 1;
            }
            else {
                bucketIdx = 2;
            }

            classification.put("bucketIdx", bucketIdx);
            classification.put("actValue", bucketIdx);
            classifier.compute(recordNum, classification, SDR1, true, false);
            recordNum += 1;

            randomNumber = random.nextDouble();
            if(randomNumber < 0.5) {
                bucketIdx = 1;
            }
            else {
                bucketIdx = 3;
            }

            classification.put("bucketIdx", bucketIdx);
            classification.put("actValue", bucketIdx);
            classifier.compute(recordNum, classification, SDR2, true, false);
            recordNum += 1;
        }

        Classification<Double> result1 = classifier.compute(2, null, SDR1, false, true);

        assertEquals(0.3, result1.getStat(0, 0), 0.1);
        assertEquals(0.3, result1.getStat(0, 1), 0.1);
        assertEquals(0.4, result1.getStat(0, 2), 0.1);

        Classification<Double> result2 = classifier.compute(2, null, SDR2, false, true);

        assertEquals(0.5, result2.getStat(0, 1), 0.1);
        assertEquals(0.5, result2.getStat(0, 3), 0.1);
    }

    @Test
    /**
     * Test the distribution of predictions with overlapping input SDRs.
     *
     * Here, we intend the classifier to learn the associations:
     *  SDR1    => bucketIdx 0 (30%)
     *          => bucketIdx 1 (30%)
     *          => bucketIdx 2 (40%)
     *
     *  SDR2    => bucketIdx 1 (50%)
     *          => bucketIdx 3 (50%)
     *
     * SDR1 and SDR2 have 10% overlap (2 bits out of 20)
     * The classifier should get the distribution almost right despite the overlap
     */
    public void testPredictionDistributionOverlap() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 0 }), 0.0005, 0.1, 0);
        int recordNum = 0;

        // Generate 2 SDRs with 2 shared bits
        int[] SDR1 = new int[20];
        int[] SDR2 = new int[20];
        for(int i = 0; i < 40; i++) {
            if(i % 2 == 0)
                SDR1[i/2] = i;     // SDR1 = {0, 2, 4, 6, ... , 38}
            else
                SDR2[(i - 1)/2] = i; // SDR2 = {1, 3, 5, 7, ... , 39}
        }
        SDR2[3] = SDR1[5];
        SDR2[5] = SDR1[11];

        int bucketIdx = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        Random random = new Random(42);
        for(int i = 0; i < 5000; i++) {
            double randomNumber = random.nextDouble();
            if (randomNumber < 0.3) {
                bucketIdx = 0;
            } else if (randomNumber < 0.6) {
                bucketIdx = 1;
            } else {
                bucketIdx = 2;
            }

            classification.put("bucketIdx", bucketIdx);
            classification.put("actValue", bucketIdx);
            classifier.compute(recordNum, classification, SDR1, true, false);
            recordNum += 1;

            randomNumber = random.nextDouble();
            if(randomNumber < 0.5) {
                bucketIdx = 1;
            }
            else {
                bucketIdx = 3;
            }

            classification.put("bucketIdx", bucketIdx);
            classification.put("actValue", bucketIdx);
            classifier.compute(recordNum, classification, SDR2, true, false);
            recordNum += 1;
        }

        Classification<Double> result1 = classifier.compute(2, null, SDR1, false, true);

        assertEquals(0.3, result1.getStat(0, 0), 0.1);
        assertEquals(0.3, result1.getStat(0, 1), 0.1);
        assertEquals(0.4, result1.getStat(0, 2), 0.1);

        Classification<Double> result2 = classifier.compute(2, null, SDR2, false, true);

        assertEquals(0.5, result2.getStat(0, 1), 0.1);
        assertEquals(0.5, result2.getStat(0, 3), 0.1);
    }

    @Test
    /**
     * Test continuous learning
     *
     * First, we intend the classifier to learn the associations:
     *  SDR1    => bucketIdx 0 (30%)
     *          => bucketIdx 1 (30%)
     *          => bucketIdx 2 (40%)
     *
     *  SDR2    => bucketIdx 1 (50%)
     *          => bucketIdx 3 (50%)
     *
     * After 20,000 iterations, we change the association to
     *  SDR1    => bucketIdx 0 (30%)
     *          => bucketIdx 1 (20%)
     *          => bucketIdx 2 (40%)
     *
     *  No further training for SDR2
     *
     * The classifier should adapt continuously and learn new associations
     * for SDR1, but at the same time remember the old association for SDR2.
     */
    public void testPredictionDistributionContinuousLearning() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 0 }), 0.001, 0.1, 0);
        int recordNum = 0;

        int[] SDR1 = {1, 3, 5};
        int[] SDR2 = {2, 4, 6};

        int bucketIdx = 0;
        Map<String, Object> classification = new HashMap<String, Object>();
        Random random = new Random(42);
        double randomNumber = 0;
        for(int i = 0; i < 10000; i++) {
            randomNumber = random.nextDouble();
            if (randomNumber < 0.3) {
                bucketIdx = 0;
            } else if (randomNumber < 0.6) {
                bucketIdx = 1;
            } else {
                bucketIdx = 2;
            }

            classification.put("bucketIdx", bucketIdx);
            classification.put("actValue", bucketIdx);
            classifier.compute(recordNum, classification, SDR1, true, false);
            recordNum += 1;

            randomNumber = random.nextDouble();
            if(randomNumber < 0.5) {
                bucketIdx = 1;
            }
            else {
                bucketIdx = 3;
            }

            classification.put("bucketIdx", bucketIdx);
            classification.put("actValue", bucketIdx);
            classifier.compute(recordNum, classification, SDR2, true, true);
            recordNum += 1;
        }

        classification.put("bucketIdx", 0);
        classification.put("actValue", 0);
        Classification<Double> result1 = classifier.compute(2, classification, SDR1, false, true);

        assertEquals(0.3, result1.getStat(0, 0), 0.1);
        assertEquals(0.3, result1.getStat(0, 1), 0.1);
        assertEquals(0.4, result1.getStat(0, 2), 0.1);

        classification.put("bucketIdx", 0);
        classification.put("actValue", 0);
        Classification<Double> result2 = classifier.compute(2, classification, SDR2, false, true);

        assertEquals(0.5, result2.getStat(0, 1), 0.1);
        assertEquals(0.5, result2.getStat(0, 3), 0.1);

        for(int i = 0; i < 20000; i++) {
            randomNumber = random.nextDouble();
            if (randomNumber < 0.3) {
                bucketIdx = 0;
            } else if (randomNumber < 0.6) {
                bucketIdx = 1;
            } else {
                bucketIdx = 3;
            }

            classification.put("bucketIdx", bucketIdx);
            classification.put("actValue", bucketIdx);
            classifier.compute(recordNum, classification, SDR1, true, false);
            recordNum += 1;
        }

        Classification<Double> result1new = classifier.compute(2, null, SDR1, false, true);

        assertEquals(0.3, result1new.getStat(0, 0), 0.1);
        assertEquals(0.3, result1new.getStat(0, 1), 0.1);
        assertEquals(0.4, result1new.getStat(0, 3), 0.1);

        Classification<Double> result2new = classifier.compute(2, null, SDR2, false, true);

        assertTrue(Arrays.equals(result2new.getStats(0), result2.getStats(0)));
    }

    @Test
    /**
     * Test multi-step predictions
     *
     * We train the 0-step and the 1-step classifiers simultaneously on
     * data stream
     * (SDR1, bucketIdx0)
     * (SDR2, bucketIdx1)
     * (SDR1, bucketIdx0)
     * (SDR2, bucketIdx1)
     * ...
     *
     * We intend the 0-step classifier to learn the associations:
     *  SDR1    => bucketIdx 0
     *  SDR2    => bucketIdx 1
     *
     * and the 1-step classifier to learn the associations
     *  SDR1    => bucketIdx 1
     *  SDR2    => bucketIdx 0
     */
    public void testMultiStepPredictions() {
        classifier = new SDRClassifier(new TIntArrayList(new int[] { 0 }), 1.0, 0.1, 0);
        int recordNum = 0;

        int[] SDR1 = {1, 3, 5};
        int[] SDR2 = {2, 4, 6};

        Map<String, Object> classification = new HashMap<String, Object>();
        for(int i = 0; i < 100; i++) {
            classification.put("bucketIdx", 0);
            classification.put("actValue", 0);
            classifier.compute(recordNum, classification, SDR1, true, false);
            recordNum += 1;

            classification.put("bucketIdx", 1);
            classification.put("actValue", 1);
            classifier.compute(recordNum, classification, SDR2, true, true);
            recordNum += 1;
        }

        Classification<Double> result1 = classifier.compute(recordNum, null, SDR1, false, true);

        Classification<Double> result2 = classifier.compute(recordNum, null, SDR2, false, true);

        assertEquals(1.0, result1.getStat(0, 0), 0.1);
        assertEquals(0.0, result1.getStat(0, 1), 0.1);
        assertEquals(0.0, result2.getStat(0, 0), 0.1);
        assertEquals(1.0, result2.getStat(0, 1), 0.1);
    }

    @Test
    public void testWriteRead() {
        // Create two classifiers, so one can be serialized and tested against the other
        SDRClassifier c1 = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);
        SDRClassifier c2 = new SDRClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.1, 0);

        // Create input vectors A, B, and C (int[] of active bit indices from below)
        int[] inputA = new int[] { 1, 5, 9 };
        int[] inputB = new int[] { 2, 4, 6 };
        int[] inputC = new int[] { 3, 5, 7 };

        // Create classification Map
        Map<String, Object> classification = new HashMap<>();

        // Have both classifiers process input A
        classification.put("bucketIdx", 0);
        classification.put("actValue", "A");
        Classification<String> result1 = c1.compute(0, classification, inputA, true, true);
        Classification<String> result2 = c2.compute(0, classification, inputA, true, true);

        // Compare results, should be equal
        assertArrayEquals(result1.getStats(1), result2.getStats(1), 0.0);
        assertArrayEquals(result1.getActualValues(), result2.getActualValues());

        // Serialize classifier #2
        SerialConfig config = new SerialConfig("testSerializeSDRClassifier", SerialConfig.SERIAL_TEST_DIR);
        PersistenceAPI api = Persistence.get(config);
        byte[] data = api.write(c2);

        // Deserialize classifier #2 into new reference.
        SDRClassifier reifiedC2 = api.read(data);

        // Make sure it isn't null...
        assertNotNull(reifiedC2);

        // Make sure pre- and post-serialization classifiers are identical
        assertTrue(DeepEquals.deepEquals(c2, reifiedC2));

        // Have the non-serialized classifier and the deserialized classifier
        // process input B.
        classification.put("bucketIdx", 1);
        classification.put("actValue", "B");
        result1 = c1.compute(0, classification, inputB, true, true);
        result2 = reifiedC2.compute(0, classification, inputB, true, true);

        // Compare the results. Make sure (de)serialization hasn't
        // messed up classifier #2.
        assertArrayEquals(result1.getStats(1), result2.getStats(1), 0.0);
        assertArrayEquals(result1.getActualValues(), result2.getActualValues());

        // Process input C - just to be safe...
        classification.put("bucketIdx", 2);
        classification.put("actValue", "C");
        result1 = c1.compute(0, classification, inputC, true, true);
        result2 = reifiedC2.compute(0, classification, inputC, true, true);

        // Compare results
        assertArrayEquals(result1.getStats(1), result2.getStats(1), 0.0);
        assertArrayEquals(result1.getActualValues(), result2.getActualValues());
    }

    public <T> Classification<T> compute(SDRClassifier classifier, int recordNum, int[] pattern,
                                         int bucket, Object value) {

        Map<String, Object> classification = new LinkedHashMap<String, Object>();
        classification.put("bucketIdx", bucket);
        classification.put("actValue", value);
        return classifier.compute(recordNum, classification, pattern, true, true);
    }
}
