package org.numenta.nupic.algorithms;

import org.numenta.nupic.algorithms.Anomaly.AveragedAnomalyRecordList;
import org.numenta.nupic.algorithms.AnomalyLikelihood.AnomalyParams;

/**
 * Container class to hold the results of {@link AnomalyLikelihood} estimations
 * and updates.
 * 
 * @author David Ray
 * @see AnomalyLikelihood
 * @see AnomalyLikelihoodTest
 */
public class AnomalyLikelihoodMetrics {
    private AnomalyParams params;
    private AveragedAnomalyRecordList aggRecordList;
    private double[] likelihoods;
    
    /**
     * Constructs a new {@code AnomalyLikelihoodMetrics}
     * 
     * @param likelihoods       array of pre-computed estimations
     * @param aggRecordList     List of {@link Sample}s which are basically a set of date, value, average score,
     *                          a list of historical values, and a running total.
     * @param params            {@link AnomalyParams} which are a {@link Statistic}, array of likelihoods,
     *                          and a {@link MovingAverage} 
     */
    public AnomalyLikelihoodMetrics( double[] likelihoods, AveragedAnomalyRecordList aggRecordList, AnomalyParams params) {
        this.params = params;
        this.aggRecordList = aggRecordList;
        this.likelihoods = likelihoods;
    }
    
    /**
     * Returns the array of computed likelihoods
     * @return
     */
    public double[] getLikelihoods() {
        return likelihoods;
    }
    
    /**
     * <pre>
     * Returns the record list which are:
     *     List of {@link Sample}s which are basically a set of date, value, average score,
     *     a list of historical values, and a running total.
     * </pre>
     * @return
     */
    public AveragedAnomalyRecordList getAvgRecordList() {
        return aggRecordList;
    }
    
    /**
     * <pre>
     * Returns the {@link AnomalyParams} which is:
     *     a {@link Statistic}, array of likelihoods,
     *     and a {@link MovingAverage}
     * </pre> 
     * @return
     */
    public AnomalyParams getParams() {
        return params;
    }
}
