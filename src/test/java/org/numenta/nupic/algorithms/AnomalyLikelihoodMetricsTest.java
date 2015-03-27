package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;
import org.numenta.nupic.algorithms.Anomaly.AveragedAnomalyRecordList;
import org.numenta.nupic.algorithms.AnomalyLikelihood.AnomalyParams;


public class AnomalyLikelihoodMetricsTest {

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

}
