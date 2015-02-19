package org.numenta.nupic.algorithms;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for anomaly score functions and classes.
 * 
 * @author David Ray
 */
public class AnomalyTest {
    private Anomaly anomaly;
    
    public void setup() {
        
    }

    @Test
    public void testComputeRawAnomalyScoreNoActiveOrPredicted() {
        double score = Anomaly.computeRawAnomalyScore(new int[0], new int[0]);
        assertEquals(score, 0.0, 0.00001);
    }
    
    @Test
    public void testComputeRawAnomalyScoreNoActive() {
        double score = Anomaly.computeRawAnomalyScore(new int[0], new int[] { 3, 5 });
        assertEquals(score, 1.0, 0.00001);
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
        System.out.println((2.0 / 3.0));
        assertEquals(score, 2.0 / 3.0, 0.001);
    }

}
