package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_USE_MOVING_AVG;
import static org.numenta.nupic.algorithms.Anomaly.KEY_WINDOW_SIZE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.algorithms.Anomaly.Mode;

/**
 * Tests for anomaly score functions and classes.
 * 
 * @author David Ray
 */
public class AnomalyTest {
    
    @Test
    public void testComputeRawAnomalyScoreNoActiveOrPredicted() {
        double score = Anomaly.computeRawAnomalyScore(new int[0], new int[0]);
        assertEquals(score, 0.0, 0.00001);
    }
    
    @Test
    public void testComputeRawAnomalyScoreNoActive() {
        double score = Anomaly.computeRawAnomalyScore(new int[0], new int[] { 3, 5 });
        assertEquals(score, 0.0, 0.00001);
    }
    
    @Test
    public void testComputeRawAnomalyScorePerfectMatch() {
        double score = Anomaly.computeRawAnomalyScore(new int[] { 3, 5, 7 }, new int[] { 3, 5, 7 });
        assertEquals(score, 0.0, 0.00001);
    }
    
    @Test
    public void testComputeRawAnomalyScoreNoMatch() {
        double score = Anomaly.computeRawAnomalyScore(new int[] { 2, 4, 6 }, new int[] { 3, 5, 7 });
        assertEquals(score, 1.0, 0.00001);
    }
    
    @Test
    public void testComputeRawAnomalyPartialNoMatch() {
        double score = Anomaly.computeRawAnomalyScore(new int[] { 2, 3, 6 }, new int[] { 3, 5, 7 });
        assertEquals(score, 2.0 / 3.0, 0.001);
    }

    /////////////////////////////////////////////////////////////////
    
    @Test
    public void testComputeAnomalyScoreNoActiveOrPredicted() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        Anomaly anomalyComputer = Anomaly.create(params);
        double score = anomalyComputer.compute(new int[0], new int[0], 0, 0);
        assertEquals(0.0, score, 0);
    }
    
    @Test
    public void testComputeAnomalyScoreNoActive() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        Anomaly anomalyComputer = Anomaly.create(params);
        double score = anomalyComputer.compute(new int[0], new int[] {3,5}, 0, 0);
        assertEquals(0.0, score, 0);
    }
    
    @Test
    public void testComputeAnomalyScorePerfectMatch() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        Anomaly anomalyComputer = Anomaly.create(params);
        double score = anomalyComputer.compute(new int[] { 3, 5, 7 }, new int[] { 3, 5, 7 }, 0, 0);
        assertEquals(0.0, score, 0);
    }
    
    @Test
    public void testComputeAnomalyScoreNoMatch() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        Anomaly anomalyComputer = Anomaly.create(params);
        double score = anomalyComputer.compute(new int[] { 2, 4, 6 }, new int[] { 3, 5, 7 }, 0, 0);
        assertEquals(1.0, score, 0);
    }
    
    @Test
    public void testComputeAnomalyScorePartialMatch() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        Anomaly anomalyComputer = Anomaly.create(params);
        double score = anomalyComputer.compute(new int[] { 2, 3, 6 }, new int[] { 3, 5, 7 }, 0, 0);
        assertEquals(2.0 / 3.0, score, 0);
    }
    
    @Test
    public void testAnomalyCumulative() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        params.put(KEY_WINDOW_SIZE, 3);
        params.put(KEY_USE_MOVING_AVG, true);
        
        Anomaly anomalyComputer = Anomaly.create(params);
        
        Object[] predicted = {
            new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 },
            new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 },
            new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }
        };
        Object[] actual = {
            new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }, new int[] { 1, 4, 6 },
            new int[] { 10, 11, 6 }, new int[] { 10, 11, 12 }, new int[] { 10, 11, 12 },
            new int[] { 10, 11, 12 }, new int[] { 1, 2, 6 }, new int[] { 1, 2, 6 }
        };
        
        double[] anomalyExpected = { 0.0, 0.0, 1.0/9.0, 3.0/9.0, 2.0/3.0, 8.0/9.0, 1.0, 2.0/3.0, 1.0/3.0 };
        for(int i = 0;i < 9;i++) {
            double score = anomalyComputer.compute((int[])actual[i], (int[])predicted[i], 0, 0);
            assertEquals(anomalyExpected[i], score, 0.01);
        }
    }
    
}

