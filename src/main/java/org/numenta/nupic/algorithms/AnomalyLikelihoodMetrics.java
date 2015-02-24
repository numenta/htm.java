/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

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
