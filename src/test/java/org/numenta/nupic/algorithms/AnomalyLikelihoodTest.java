package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MEAN;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_STDEV;
import static org.numenta.nupic.algorithms.Anomaly.KEY_VARIANCE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.numenta.nupic.algorithms.Anomaly.AveragedAnomalyRecordList;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.MersenneTwister;

import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;


public class AnomalyLikelihoodTest {
    private AnomalyLikelihood an;
    
    @Before
    public void setup() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.LIKELIHOOD);
        an = (AnomalyLikelihood)Anomaly.create(params);
    }
    
    /**
     * Given the parameters of a distribution, generate numSamples points from it.
     * This routine is mostly for testing.
     * 
     * @param mean
     * @param variance
     * @return
     */
    public static double[] sampleDistribution(Random random, double mean, double variance, int size) {
        SampleDistribution sampler = new SampleDistribution(mean, Math.sqrt(variance), size);
        return sampler.getSample(random);
    }
    
    /**
     * Generate 1440 samples of fake metrics data with a particular distribution
     * of anomaly scores and metric values. Here we generate values every minute.
     * 
     * @param mean
     * @param variance
     * @param metricMean
     * @param metricVariance
     * @return
     */
    public static List<Sample> generateSampleData(double mean, double variance, double metricMean, double metricVariance) {
        List<Sample> retVal = new ArrayList<>();
        
        Random random = new MersenneTwister(42);
        double[] samples = sampleDistribution(random, mean, variance, 1440);
        double[] metricValues = sampleDistribution(random, metricMean, metricVariance, 1440);
        for(int hour : ArrayUtils.range(0, 24)) {
            for(int minute : ArrayUtils.range(0, 60)) {
                retVal.add(
                    new Sample(
                        new DateTime(2013, 2, 2, hour, minute), 
                        metricValues[hour * 60 + minute], 
                        samples[hour * 60 + minute]
                    )
                );
            }
        }
        
        return retVal;
    }
    
    public static boolean assertWithinEpsilon(double a, double b) {
        return assertWithinEpsilon(a, b, 0.001);
    }
    
    public static boolean assertWithinEpsilon(double a, double b, double epsilon) {
        if(Math.abs(a - b) <= epsilon) {
            return true;
        }
        return false;
    }

	/**
	 * This test attempts to find the anomaly-probability after create an
	 * AnomalyLikelihood instance with default values for the learning period
	 * and estimation samples. This used to generate an exception stating that
	 * you must have at least one anomaly score.
	 */
	@Test
	public void testConstructorWithDefaultLearningPeriodAndEstimationSamples() {
		this.an.anomalyProbability(0.75, 0.5, null);
	}

    @Test
    public void testNormalProbability() {
        TObjectDoubleMap<String> p = new TObjectDoubleHashMap<>();
        p.put(KEY_MEAN, 0.0);
        p.put(KEY_VARIANCE, 1.0);
        p.put(KEY_STDEV, 1.0);
        
        // Test a standard normal distribution
        // Values taken from http://en.wikipedia.org/wiki/Standard_normal_table
        assertWithinEpsilon(an.normalProbability(0.0, p), 0.5);
        assertWithinEpsilon(an.normalProbability(0.3, p), 0.3820885780);
        assertWithinEpsilon(an.normalProbability(1.0, p), 0.1587);
        assertWithinEpsilon(1.0 - an.normalProbability(1.0, p), an.normalProbability(-1.0, p));
        assertWithinEpsilon(an.normalProbability(-0.3, p), 1.0 - an.normalProbability(0.3, p));
        
        // Non standard normal distribution
        // p = {"name": "normal", "mean": 1.0, "variance": 4.0, "stdev": 2.0}
        p.put(KEY_MEAN, 1.0);
        p.put(KEY_VARIANCE, 4.0);
        p.put(KEY_STDEV, 2.0);
        assertWithinEpsilon(an.normalProbability(1.0, p), 0.5);
        assertWithinEpsilon(an.normalProbability(2.0, p), 0.3085);
        assertWithinEpsilon(an.normalProbability(3.0, p), 0.1587);
        assertWithinEpsilon(an.normalProbability(3.0, p), 1.0 - an.normalProbability(-1.0, p));
        assertWithinEpsilon(an.normalProbability(0.0, p), 1.0 - an.normalProbability(2.0, p));
        
        // Non standard normal distribution
        // p = {"name": "normal", "mean": -2.0, "variance": 0.5, "stdev": math.sqrt(0.5)}
        p.put(KEY_MEAN, -2.0);
        p.put(KEY_VARIANCE, 0.5);
        p.put(KEY_STDEV, Math.sqrt(0.5));
        assertWithinEpsilon(an.normalProbability(-2.0, p), 0.5);
        assertWithinEpsilon(an.normalProbability(-1.5, p), 0.241963652);
        assertWithinEpsilon(an.normalProbability(-2.5, p), 1.0 - an.normalProbability(-1.5, p));
    }
    
    /**
     * This passes in a known set of data and ensures the estimateNormal
     * function returns the expected results.
     */
    @Test
    public void testEstimateNormal() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(KEY_MODE, Mode.LIKELIHOOD);
                
       // 100 samples drawn from mean=0.4, stdev = 0.5
        double[] samples = new double[] {
            0.32259025, -0.44936321, -0.15784842, 0.72142628, 0.8794327,
            0.06323451, -0.15336159, -0.02261703, 0.04806841, 0.47219226,
            0.31102718, 0.57608799, 0.13621071, 0.92446815, 0.1870912,
            0.46366935, -0.11359237, 0.66582357, 1.20613048, -0.17735134,
            0.20709358, 0.74508479, 0.12450686, -0.15468728, 0.3982757,
            0.87924349, 0.86104855, 0.23688469, -0.26018254, 0.10909429,
            0.65627481, 0.39238532, 0.77150761, 0.47040352, 0.9676175,
            0.42148897, 0.0967786, -0.0087355, 0.84427985, 1.46526018,
            1.19214798, 0.16034816, 0.81105554, 0.39150407, 0.93609919,
            0.13992161, 0.6494196, 0.83666217, 0.37845278, 0.0368279,
            -0.10201944, 0.41144746, 0.28341277, 0.36759426, 0.90439446,
            0.05669459, -0.11220214, 0.34616676, 0.49898439, -0.23846184,
            1.06400524, 0.72202135, -0.2169164, 1.136582, -0.69576865,
            0.48603271, 0.72781008, -0.04749299, 0.15469311, 0.52942518,
            0.24816816, 0.3483905, 0.7284215, 0.93774676, 0.07286373,
            1.6831539, 0.3851082, 0.0637406, -0.92332861, -0.02066161,
            0.93709862, 0.82114131, 0.98631562, 0.05601529, 0.72214694,
            0.09667526, 0.3857222, 0.50313998, 0.40775344, -0.69624046,
            -0.4448494, 0.99403206, 0.51639049, 0.13951548, 0.23458214,
            1.00712699, 0.40939048, -0.06436434, -0.02753677, -0.23017904           
        };
        
        Statistic result = an.estimateNormal(samples, true);
        assertTrue(assertWithinEpsilon(result.mean, 0.3721));
        assertTrue(assertWithinEpsilon(result.variance, 0.22294));
        assertTrue(assertWithinEpsilon(result.stdev, 0.47216));
    }
    
    /**
     * Test that sampleDistribution from a generated distribution returns roughly
     * the same parameters.
     */
    @Test
    public void testSampleDistribution() {
        TObjectDoubleMap<String> p = new TObjectDoubleHashMap<>();
        p.put(KEY_MEAN, 0.5);
        p.put(KEY_STDEV, Math.sqrt(0.1));
        p.put(KEY_VARIANCE, 0.1);
        
        double[] samples = sampleDistribution(new MersenneTwister(), 0.5, 0.1, 1000);
        
        Statistic np = an.estimateNormal(samples, true);
        assertTrue(assertWithinEpsilon(p.get(KEY_MEAN), np.mean, 0.1));
        assertTrue(assertWithinEpsilon(p.get(KEY_VARIANCE), np.variance, 0.1));
        assertTrue(assertWithinEpsilon(p.get(KEY_STDEV), np.stdev, 0.1));
    }
    
    /**
     * This calls estimateAnomalyLikelihoods to estimate the distribution on fake
     * data and validates the results
     */
    @Test
    public void testEstimateAnomalyLikelihoods() {
        // Generate an estimate using fake distribution of anomaly scores.
        List<Sample> data = generateSampleData(0.2, 0.2, 0.2, 0.2).subList(0, 1000);
        
        AnomalyLikelihoodMetrics metrics = an.estimateAnomalyLikelihoods(data, 10, 0);
        assertEquals(1000, metrics.getLikelihoods().length);
        assertEquals(1000, metrics.getAvgRecordList().averagedRecords.size());
        assertTrue(an.isValidEstimatorParams(metrics.getParams()));
        
        // Get the total
        double total = 0;
        for(Sample sample : metrics.getAvgRecordList().averagedRecords) {
            total = total + sample.score;
        }
        
        // Check that the estimated mean is correct
        Statistic statistic = (Statistic)metrics.getParams().distribution();
        assertTrue(
            assertWithinEpsilon(
                statistic.mean, (total / (double)metrics.getAvgRecordList().averagedRecords.size())
            )
        );
        
        int count = ArrayUtils.where(metrics.getLikelihoods(), new Condition.Adapter<Double>() {
            public boolean eval(double d) { return d < 0.02; }
        }).length;
        assertTrue(count <= 50);
        assertTrue(count >= 1);
    }
    
    /**
     * NOTE: SKIPPED
     * 
     * This calls {@link AnomalyLikelihood#estimateAnomalyLikelihoods(List, int, int)}
     * to estimate the distribution on fake data and validates the results
     */
    @Test
    public void testEstimateAnomalyLikelihoodsMalformedRecords() {
        // Skipped due to impossibility of forming bad Sample objects in Java
    }

	/**
	 * This tests the anomalyProbability method with a number of calls that will
	 * trigger copying of the sample array.
	 */
	@Test
	public void testAnomalyProbabilityArrayCopying() {
		Map<String, Object> params = new HashMap<>();
		params.put(KEY_MODE, Mode.LIKELIHOOD);
		params.put(AnomalyLikelihood.KEY_LEARNING_PERIOD, 300);
		params.put(AnomalyLikelihood.KEY_ESTIMATION_SAMPLES, 300);
		an = (AnomalyLikelihood) Anomaly.create(params);

		for (int i = 0; i < 2000; i++) {
			an.anomalyProbability(0.07, .5, null);
		}
	}
    
    /**
     * This calls estimateAnomalyLikelihoods with various values of skipRecords
     */
    @Test
    public void testSkipRecords() {
        // Generate an estimate using fake distribution of anomaly scores.
        List<Sample> data = generateSampleData(0.1, 0.2, 0.2, 0.2).subList(0, 200);
        data.addAll(generateSampleData(0.9, 0.2, 0.2, 0.2).subList(0, 200));
        
        // skipRecords = 200
        AnomalyLikelihoodMetrics metrics = an.estimateAnomalyLikelihoods(data, 10, 200);
        Statistic stats = (Statistic)metrics.getParams().distribution();
        // Check results are correct, i.e. we are actually skipping the first 50
        assertWithinEpsilon(stats.mean, 0.9, 0.1);
        
        // Check case where skipRecords > num records
        // In this case a null distribution should be returned which makes all
        // the likelihoods reasonably high
        metrics = an.estimateAnomalyLikelihoods(data, 10, 500);
        assertEquals(metrics.getLikelihoods().length, data.size());
        assertTrue(ArrayUtils.sum(metrics.getLikelihoods()) >= 0.3 * metrics.getLikelihoods().length);
        
        // Check the case where skipRecords == num records
        metrics = an.estimateAnomalyLikelihoods(data, 10, data.size());
        assertEquals(metrics.getLikelihoods().length, data.size());
        assertTrue(ArrayUtils.sum(metrics.getLikelihoods()) >= 0.3 * metrics.getLikelihoods().length);
    }
    
    /**
     * A slight more complex test. This calls estimateAnomalyLikelihoods
     * to estimate the distribution on fake data, followed by several calls
     * to updateAnomalyLikelihoods.
     */
    @Test
    public void testUpdateAnomalyLikelihoods() {
        
        //----------------------------------------
        // Step 1. Generate an initial estimate using fake distribution of anomaly scores.
        List<Sample> data1 = generateSampleData(0.2, 0.2, 0.2, 0.2).subList(0, 1000);
        AnomalyLikelihoodMetrics metrics1 = an.estimateAnomalyLikelihoods(data1, 5, 0);
        
        //----------------------------------------
        // Step 2. Generate some new data with a higher average anomaly
        // score. Using the estimator from step 1, to compute likelihoods. Now we
        // should see a lot more anomalies.
        List<Sample> data2 = generateSampleData(0.6, 0.2, 0.2, 0.2).subList(0, 300);
        AnomalyLikelihoodMetrics metrics2 = an.updateAnomalyLikelihoods(data2, metrics1.getParams());
        assertEquals(metrics2.getLikelihoods().length, data2.size());
        assertEquals(metrics2.getAvgRecordList().size(), data2.size());
        assertTrue(an.isValidEstimatorParams(metrics2.getParams()));
        
        // The new running total should be different
        assertFalse(metrics1.getAvgRecordList().total == metrics2.getAvgRecordList().total);
        
        // We should have many more samples where likelihood is < 0.01, but not all
        Condition<Double> cond = new Condition.Adapter<Double>() {
            public boolean eval(double d) { return d < 0.01; }
        };
        int conditionCount = ArrayUtils.where(metrics2.getLikelihoods(), cond).length;
        assertTrue(conditionCount >= 25);
        assertTrue(conditionCount <= 250);
        
        //----------------------------------------
        // Step 3. Generate some new data with the expected average anomaly score. We
        // should see fewer anomalies than in Step 2.
        List<Sample> data3 = generateSampleData(0.2, 0.2, 0.2, 0.2).subList(0, 1000);
        AnomalyLikelihoodMetrics metrics3 = an.updateAnomalyLikelihoods(data3, metrics2.getParams());
        assertEquals(metrics3.getLikelihoods().length, data3.size());
        assertEquals(metrics3.getAvgRecordList().size(), data3.size());
        assertTrue(an.isValidEstimatorParams(metrics3.getParams()));
        
        // The new running total should be different
        assertFalse(metrics1.getAvgRecordList().total == metrics3.getAvgRecordList().total);
        assertFalse(metrics2.getAvgRecordList().total == metrics3.getAvgRecordList().total);
        
        // We should have a small number of samples where likelihood is < 0.02
        conditionCount = ArrayUtils.where(metrics3.getLikelihoods(), cond).length;
        assertTrue(conditionCount >= 1);
        assertTrue(conditionCount <= 100);
        
        //------------------------------------------
        // Step 4. Validate that sending data incrementally is the same as sending
        // in one batch
        List<Sample> allData = new ArrayList<>();
        allData.addAll(data1);
        allData.addAll(data2);
        allData.addAll(data3);
        AveragedAnomalyRecordList recordList = an.anomalyScoreMovingAverage(allData, 5);
        
        double[] historicalValuesAll = new double[recordList.historicalValues.size()];
        int i = 0;
        for(TDoubleIterator it = recordList.historicalValues.iterator();it.hasNext();) {
            historicalValuesAll[i++] = it.next();
        }
        assertEquals(ArrayUtils.sum(historicalValuesAll), ArrayUtils.sum(
            metrics3.getParams().movingAverage().getSlidingWindow().toArray()), 0);
        
        assertEquals(recordList.total, metrics3.getParams().movingAverage().getTotal(), 0);
    }
    
    /**
     * This calls estimateAnomalyLikelihoods with flat distributions and
     * ensures things don't crash.
     */
    @Test
    public void testFlatAnomalyScores() {
        // Generate an estimate using fake distribution of anomaly scores.
        List<Sample> data1 = generateSampleData(42, 1e-10, 0.2, 0.2).subList(0, 1000);
        
        AnomalyLikelihoodMetrics metrics1 = an.estimateAnomalyLikelihoods(data1, 10, 0);
        assertEquals(metrics1.getLikelihoods().length, data1.size());
        assertEquals(metrics1.getAvgRecordList().size(), data1.size());
        assertTrue(an.isValidEstimatorParams(metrics1.getParams()));
        
        // Check that the estimated mean is correct
        Statistic stats = metrics1.getParams().distribution();
        assertWithinEpsilon(stats.mean, data1.get(0).score);
        
        // If you deviate from the mean, you should get probability 0
        // Test this by sending in just slightly different values.
        List<Sample> data2 = generateSampleData(42.5, 1e-10, 0.2, 0.2);
        AnomalyLikelihoodMetrics metrics2 = an.updateAnomalyLikelihoods(data2.subList(0, 10), metrics1.getParams());
        // The likelihoods should go to zero very quickly
        assertTrue(ArrayUtils.sum(metrics2.getLikelihoods()) <= 0.01);
        
        // Test edge case where anomaly scores are very close to 0
        // In this case we don't let likelihood to get too low. An average
        // anomaly score of 0.1 should be essentially zero, but an average
        // of 0.04 should be higher
        List<Sample> data3 = generateSampleData(0.01, 1e-6, 0.2, 0.2);
        AnomalyLikelihoodMetrics metrics3 = an.estimateAnomalyLikelihoods(data3.subList(0, 100), 10, 0);
        
        List<Sample> data4 = generateSampleData(0.1, 1e-6, 0.2, 0.2);
        AnomalyLikelihoodMetrics metrics4 = an.updateAnomalyLikelihoods(data4.subList(0, 20), metrics3.getParams());
        
        // Average of 0.1 should go to zero
        double[] likelihoods4 = Arrays.copyOfRange(metrics4.getLikelihoods(), 10, metrics4.getLikelihoods().length);
        assertTrue(ArrayUtils.average(likelihoods4) <= 0.002);
        
        List<Sample> data5 = generateSampleData(0.05, 1e-6, 0.2, 0.2);
        AnomalyLikelihoodMetrics metrics5 = an.updateAnomalyLikelihoods(data5.subList(0, 20), metrics4.getParams());
        
        // The likelihoods should be low but not near zero
        double[] likelihoods5 = Arrays.copyOfRange(metrics5.getLikelihoods(), 10, metrics4.getLikelihoods().length);
        assertTrue(ArrayUtils.average(likelihoods5) <= 0.28);
        assertTrue(ArrayUtils.average(likelihoods5) > 0.015);
    }
    
    /**
     * This calls estimateAnomalyLikelihoods with flat metric values. In this case
     * we should use the null distribution, which gets reasonably high likelihood
     * for everything.
     */
    @Test
    public void testFlatMetricScores() {
        // Generate samples with very flat metric values
        List<Sample> data1 = generateSampleData(0.2, 0.2, 42, 1e-10).subList(0, 1000);
        
        // Check that we do indeed get reasonable likelihood values
        AnomalyLikelihoodMetrics metrics1 = an.estimateAnomalyLikelihoods(data1, 10, 0);
        assertEquals(metrics1.getLikelihoods().length, data1.size());
        double[] likelihoods = metrics1.getLikelihoods();
        assertTrue(ArrayUtils.sum(likelihoods) >= 0.4 * likelihoods.length);
        metrics1.getParams().distribution().equals(an.nullDistribution());
        assertTrue(metrics1.getParams().distribution().equals(an.nullDistribution()));
    }
    
    /**
     * This calls estimateAnomalyLikelihoods and updateAnomalyLikelihoods
     * with one or no scores.
     */
    @Test
    public void testVeryFewScores() {
        // Generate an estimate using two data points
        List<Sample> data1 = generateSampleData(42, 1e-10, 0.2, 0.2).subList(0, 2);
        AnomalyLikelihoodMetrics metrics1 = an.estimateAnomalyLikelihoods(data1, 10, 0);
        assertTrue(an.isValidEstimatorParams(metrics1.getParams()));
        
        // Check that the estimated mean is that value
        assertWithinEpsilon(metrics1.getParams().distribution().mean, data1.get(0).score);
        
        // Can't generate an estimate using no data points
        List<Sample> test = new ArrayList<>();
        try {
            an.estimateAnomalyLikelihoods(test, 10, 0);
            fail();
        }catch(Exception e) {
            assertTrue(e.getMessage().equals("Must have at least one anomaly score."));
        }
        
        // Can't update without scores
        try {
            an.updateAnomalyLikelihoods(test, metrics1.getParams());
            fail();
        }catch(Exception e) {
            assertTrue(e.getMessage().equals("Must have at least one anomaly score."));
        }
    }
    
    /**
     * NOTE: Not a valid test in java. Remnant of Python ability to substitute types, so we 
     * just do a simple test
     */
    @Test
    public void testFilterLikelihoodsInputType() {
        double[] l2 = an.filterLikelihoods(new double[] { 0.0, 0.0, 0.3, 0.3, 0.5 });
        double[] filtered = new double[] { 0.0, 0.001, 0.3, 0.3, 0.5 };
        int i = 0;
        for(double d : l2) {
            assertEquals(d, filtered[i++], 0.01);
        }
    }
    
    /**
     * <pre>
     * Tests _filterLikelihoods function for several cases:
     * i.   Likelihood goes straight to redzone, skipping over yellowzone, repeats
     * ii.  Case (i) with different values, and numpy array instead of float list
     * iii. A scenario where changing the redzone from four to five 9s should
     *      filter differently
     * </pre>
     */
    @Test
    public void testFilterLikelihoods() {
        double redThreshold = 0.9999;
        double yellowThreshold = 0.999;
        
        // Case (i): values at indices 1 and 7 should be filtered to yellow zone
        double[] l = { 1.0, 1.0, 0.9, 0.8, 0.5, 0.4, 1.0, 1.0, 0.6, 0.0 };
        l = Arrays.stream(l).map(d -> 1.0d - d).toArray();
        double[] l2a = Arrays.copyOf(l, l.length);
        l2a[1] = 1 - yellowThreshold;
        l2a[7] = 1 - yellowThreshold;
        double[] l3a = an.filterLikelihoods(l, redThreshold, yellowThreshold);
        
        int successIndexes = 
            IntStream.range(0, l.length).map(i -> { assertEquals(l2a[i], l3a[i], 0.01); return 1; }).sum();
        assertEquals(successIndexes, l.length);
        
        // Case (ii): values at indices 1-10 should be filtered to yellow zone
        l = new double[] { 
            0.999978229, 0.999978229, 0.999999897, 1, 1, 1, 1,
            0.999999994, 0.999999966, 0.999999966, 0.999994331,
            0.999516576, 0.99744487 };
        l = Arrays.stream(l).map(d -> 1.0d - d).toArray();
        double[] l2b = Arrays.copyOf(l, l.length);
        ArrayUtils.setIndexesTo(l2b, ArrayUtils.range(1, 11), 1 - yellowThreshold);
        double[] l3b = an.filterLikelihoods(l);
        
        successIndexes = 
            IntStream.range(0, l.length).map(i -> { assertEquals(l2b[i], l3b[i], 0.01); return 1; }).sum();
        assertEquals(successIndexes, l.length);
        
        // Case (iii): redThreshold difference should be at index 2
        l = new double[] {
            0.999968329, 0.999999897, 1, 1, 1,
            1, 0.999999994, 0.999999966, 0.999999966,
            0.999994331, 0.999516576, 0.99744487 
        };
        l = Arrays.stream(l).map(d -> 1.0d - d).toArray();
        double[] l2a2 = Arrays.copyOf(l, l.length);
        double[] l2b2 = Arrays.copyOf(l, l.length);
        ArrayUtils.setIndexesTo(l2a2, ArrayUtils.range(1, 10), 1 - yellowThreshold);
        ArrayUtils.setIndexesTo(l2b2, ArrayUtils.range(2, 10), 1 - yellowThreshold);
        double[] l3a2 = an.filterLikelihoods(l);
        double[] l3b2 = an.filterLikelihoods(l, 0.99999, yellowThreshold);
        
        successIndexes = 
            IntStream.range(0, l2a2.length).map(i -> { assertEquals(l2a2[i], l3a2[i], 0.01); return 1; }).sum();
        assertEquals(successIndexes, l2a2.length);
        
        successIndexes = 
            IntStream.range(0, l2b2.length).map(i -> { assertEquals(l2b2[i], l3b2[i], 0.01); return 1; }).sum();
        assertEquals(successIndexes, l2b2.length);
    }
    
}
