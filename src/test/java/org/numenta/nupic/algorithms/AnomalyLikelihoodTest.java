package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.numenta.nupic.algorithms.Anomaly.KEY_DIST;
import static org.numenta.nupic.algorithms.Anomaly.KEY_HIST_LIKE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MEAN;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MVG_AVG;
import static org.numenta.nupic.algorithms.Anomaly.KEY_STDEV;
import static org.numenta.nupic.algorithms.Anomaly.KEY_VARIANCE;
import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.AnomalyLikelihood.AnomalyParams;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.MersenneTwister;


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
    private double[] sampleDistribution(Random random, double mean, double variance, int size) {
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
    private List<Sample> generateSampleData(double mean, double variance, double metricMean, double metricVariance) {
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
    
    private boolean assertWithinEpsilon(double a, double b) {
        return assertWithinEpsilon(a, b, 0.001);
    }
    
    private boolean assertWithinEpsilon(double a, double b, double epsilon) {
        if(Math.abs(a - b) <= epsilon) {
            return true;
        }
        return false;
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
            total = total + sample.value;
        }
        
        // Check that the estimated mean is correct
        Statistic statistic = (Statistic)metrics.getParams().get("distribution");
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
        // Skipped due to impossibility of forming bad Sample objects
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
        Statistic stats = (Statistic)metrics.getParams().get("distribution");
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
            ((MovingAverage)metrics3.getParams().get("movingAverage")).getSlidingWindow().toArray()), 0);
        
        assertEquals(recordList.total, ((MovingAverage)metrics3.getParams().get("movingAverage")).getTotal(), 0);
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
        metrics1.getParams().get("distribution");
    }
    
    /**
     * Tests the AnomalyParams return value and its json creation
     */
    @Test
    public void testAnomalyParamsToJson() {
        AnomalyParams params = new AnomalyParams(
            new String[] { KEY_DIST, KEY_HIST_LIKE, KEY_MVG_AVG},
                new Statistic(0.38423985556178486, 0.009520602474199693, 0.09757357467162762),
                new double[] { 0.460172163,0.344578258,0.344578258,0.382088578,0.460172163 },
                new MovingAverage(
                    new TDoubleArrayList(
                        new double[] { 0.09528343752779542,0.5432072190186226,0.9062454498382395,0.44264021533137254,-0.009955323005220784 }),
                    1.9774209987108093, // total 
                    5                   // window size
            )
        );
        
        String expected = "{\"distribution\":{\"mean\":0.38423985556178486,\"variance\":0.009520602474199693,\"stdev\":0.09757357467162762},"+
        "\"historicalLikelihoods\":[0.460172163,0.344578258,0.344578258,0.382088578,0.460172163],"+
        "\"movingAverage\":{\"windowSize\":5,"+
        "\"historicalValues\":[0.09528343752779542,0.5432072190186226,0.9062454498382395,0.44264021533137254,-0.009955323005220784],"+
        "\"total\":1.9774209987108093}}";
        
        assertEquals(expected, params.toJson(true));
    }
    
}
