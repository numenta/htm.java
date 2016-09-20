package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;
import org.numenta.nupic.algorithms.Anomaly.AveragedAnomalyRecordList;
import org.numenta.nupic.algorithms.AnomalyLikelihood.AnomalyParams;


public class AnomalyLikelihoodMetricsTest {

    @SuppressWarnings("serial")
    @Test
    public void testEquals() {
        double[] likelihoods = new double[] { 0.2, 0.3 };

        Sample s = new Sample(new DateTime(), 0.1, 0.1);
        List<Sample> samples = new ArrayList<>();
        samples.add(s);
        TDoubleList d = new TDoubleArrayList();
        d.add(0.5);
        double total = 0.4;
        AveragedAnomalyRecordList avges = (
                new Anomaly() {
                    @Override
                    public double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp) {
                        return 0;
                    }
                }
        ).new AveragedAnomalyRecordList(samples, d, total);

        Statistic stat = new Statistic(0.1, 0.1, 0.1);
        MovingAverage ma = new MovingAverage(new TDoubleArrayList(), 1);
        AnomalyParams params = new AnomalyParams(new String[] { Anomaly.KEY_DIST, Anomaly.KEY_MVG_AVG, Anomaly.KEY_HIST_LIKE}, stat, ma, likelihoods);

        // Test equality
        AnomalyLikelihoodMetrics metrics = new AnomalyLikelihoodMetrics(likelihoods, avges, params);
        AnomalyLikelihoodMetrics metrics2 = metrics.copy();
        assertEquals(metrics, metrics2);

        assertTrue(metrics.equals(metrics));
        assertFalse(metrics.equals(null));
        assertFalse(metrics.equals(s));

        AnomalyLikelihoodMetrics metricsNoRecs = new AnomalyLikelihoodMetrics(likelihoods, null, params);
        assertFalse(metricsNoRecs.equals(metrics));

        double[] likelihoods2 = new double[] { 0.1, 0.2 };
        AnomalyLikelihoodMetrics metricsDiffLikes = new AnomalyLikelihoodMetrics(likelihoods2, avges, params);
        assertFalse(metrics.equals(metricsDiffLikes));

        AnomalyLikelihoodMetrics metricsNoLikes = new AnomalyLikelihoodMetrics(null, avges, params);
        assertFalse(metricsNoLikes.equals(metricsDiffLikes));

        AnomalyLikelihoodMetrics metricsNoParams = new AnomalyLikelihoodMetrics(likelihoods, avges, null);
        assertFalse(metricsNoParams.equals(metrics));
    }

    @SuppressWarnings("serial")
    @Test
    public void testCopy() {
        double[] likelihoods = new double[] { 0.2, 0.3 };

        Sample s = new Sample(new DateTime(), 0.1, 0.1);
        List<Sample> samples = new ArrayList<>();
        samples.add(s);
        TDoubleList d = new TDoubleArrayList();
        d.add(0.5);
        double total = 0.4;
        AveragedAnomalyRecordList avges = (
                new Anomaly() {
                    @Override
                    public double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp) {
                        return 0;
                    }
                }
        ).new AveragedAnomalyRecordList(samples, d, total);

        Statistic stat = new Statistic(0.1, 0.1, 0.1);
        MovingAverage ma = new MovingAverage(new TDoubleArrayList(), 1);
        AnomalyParams params = new AnomalyParams(new String[] { Anomaly.KEY_DIST, Anomaly.KEY_MVG_AVG, Anomaly.KEY_HIST_LIKE}, stat, ma, likelihoods);

        // Test equality
        AnomalyLikelihoodMetrics metrics = new AnomalyLikelihoodMetrics(likelihoods, avges, params);
        AnomalyLikelihoodMetrics metrics2 = metrics.copy();
        assertEquals(metrics, metrics2);
    }

    @Test
    public void testHashCodeAndEquals() {
        double[] likelihoods = new double[] { 0.2, 0.3 };

        DateTime metricTime = new DateTime();
        Sample s = new Sample(metricTime, 0.1, 0.1);
        List<Sample> samples = new ArrayList<>();
        samples.add(s);
        TDoubleList d = new TDoubleArrayList();
        d.add(0.5);
        double total = 0.4;
        AveragedAnomalyRecordList avges = (
                new Anomaly() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp) {
                        return 0;
                    }
                }
        ).new AveragedAnomalyRecordList(samples, d, total);

        Statistic stat = new Statistic(0.1, 0.1, 0.1);
        MovingAverage ma = new MovingAverage(new TDoubleArrayList(), 1);
        AnomalyParams params = new AnomalyParams(new String[] { Anomaly.KEY_DIST, Anomaly.KEY_MVG_AVG, Anomaly.KEY_HIST_LIKE}, stat, ma, likelihoods);
        AnomalyLikelihoodMetrics metrics = new AnomalyLikelihoodMetrics(likelihoods, avges, params);
        AnomalyLikelihoodMetrics metrics2 = metrics.copy();
        assertEquals(metrics, metrics2);
        assertEquals(metrics.hashCode(), metrics2.hashCode());

        ////////////////////////// NEGATIVE TESTS //////////////////////////
        // Test with different Samples / Different Date

        double[] likelihoods2 = new double[] { 0.2, 0.3 };

        samples = new ArrayList<Sample>();
        Sample s2 = new Sample(new DateTime(), 0.1, 0.1); // Different date
        samples.add(s2);
        avges = (
                new Anomaly() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp) {
                        return 0;
                    }
                }
        ).new AveragedAnomalyRecordList(samples, d, total);

        Statistic stat2 = new Statistic(0.1, 0.1, 0.1);
        MovingAverage ma2 = new MovingAverage(new TDoubleArrayList(), 1);
        AnomalyParams params2 = new AnomalyParams(new String[] { Anomaly.KEY_DIST, Anomaly.KEY_MVG_AVG, Anomaly.KEY_HIST_LIKE}, stat2, ma2, likelihoods2);
        AnomalyLikelihoodMetrics metrics3 = new AnomalyLikelihoodMetrics(likelihoods, avges, params2);

        assertNotEquals(metrics, metrics3);

        ///////////////////////////
        // Test with different Samples / Different Value

        double[] likelihoods3 = new double[] { 0.2, 0.3 };

        samples = new ArrayList<Sample>();
        Sample s3 = new Sample(s2.date, 0.2, 0.1); // Different Value
        samples.add(s3);
        avges = (
                new Anomaly() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp) {
                        return 0;
                    }
                }
        ).new AveragedAnomalyRecordList(samples, d, total);

        Statistic stat3 = new Statistic(0.1, 0.1, 0.1);
        MovingAverage ma3 = new MovingAverage(new TDoubleArrayList(), 1);
        AnomalyParams params3 = new AnomalyParams(new String[] { Anomaly.KEY_DIST, Anomaly.KEY_MVG_AVG, Anomaly.KEY_HIST_LIKE}, stat3, ma3, likelihoods3);
        AnomalyLikelihoodMetrics metrics4 = new AnomalyLikelihoodMetrics(likelihoods, avges, params3);

        assertNotEquals(metrics, metrics4);

        ///////////////////////////
        // Test with different Samples / Different Score

        double[] likelihoods4 = new double[] { 0.2, 0.3 };

        samples = new ArrayList<Sample>();
        Sample s4 = new Sample(s2.date, 0.1, 0.9); // Different Value
        samples.add(s4);
        avges = (
                new Anomaly() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp) {
                        return 0;
                    }
                }
        ).new AveragedAnomalyRecordList(samples, d, total);

        Statistic stat4 = new Statistic(0.1, 0.1, 0.1);
        MovingAverage ma4 = new MovingAverage(new TDoubleArrayList(), 1);
        AnomalyParams params4 = new AnomalyParams(new String[] { Anomaly.KEY_DIST, Anomaly.KEY_MVG_AVG, Anomaly.KEY_HIST_LIKE}, stat4, ma4, likelihoods4);
        AnomalyLikelihoodMetrics metrics5 = new AnomalyLikelihoodMetrics(likelihoods, avges, params4);

        assertNotEquals(metrics, metrics5);

        ///////////////////////////
        // Test with different Samples / Different Params

        avges = (
                new Anomaly() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp) {
                        return 0;
                    }
                }
        ).new AveragedAnomalyRecordList(samples, d, total);

        Statistic stat5 = new Statistic(0.5, 0.1, 0.1);
        AnomalyParams params5 = new AnomalyParams(new String[] { Anomaly.KEY_DIST, Anomaly.KEY_MVG_AVG, Anomaly.KEY_HIST_LIKE}, stat5, ma4, likelihoods4);
        AnomalyLikelihoodMetrics metrics6 = new AnomalyLikelihoodMetrics(likelihoods, avges, params5);

        assertNotEquals(metrics, metrics6);
        
        //////////////////////////
        // Test same Samples / Different Params
        likelihoods = new double[] { 0.2, 0.3 };

        s = new Sample(metricTime, 0.1, 0.1);
        samples = new ArrayList<>();
        samples.add(s);
        d = new TDoubleArrayList();
        d.add(0.5);
        total = 0.4;
        avges = (
                new Anomaly() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp) {
                        return 0;
                    }
                }
        ).new AveragedAnomalyRecordList(samples, d, total);
        
        Statistic stat6 = new Statistic(0.1, 0.1, 0.1);
        MovingAverage ma6 = new MovingAverage(new TDoubleArrayList(), 1);
        AnomalyParams params6 = new AnomalyParams(new String[] { Anomaly.KEY_DIST, Anomaly.KEY_MVG_AVG, Anomaly.KEY_HIST_LIKE}, stat6, ma6, likelihoods);
        AnomalyLikelihoodMetrics metrics7 = new AnomalyLikelihoodMetrics(likelihoods, avges, params6);
        
        assertNotEquals(metrics, metrics7);
    }

}