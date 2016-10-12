/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.numenta.nupic.algorithms.Anomaly.AveragedAnomalyRecordList;
import org.numenta.nupic.algorithms.AnomalyLikelihood.AnomalyParams;
import org.numenta.nupic.model.Persistable;

/**
 * Container class to hold the results of {@link AnomalyLikelihood} estimations
 * and updates.
 * 
 * @author David Ray
 * @see AnomalyLikelihood
 * @see AnomalyLikelihoodTest
 */
public class AnomalyLikelihoodMetrics implements Persistable {
    private static final long serialVersionUID = 1L;
    
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
    public AnomalyLikelihoodMetrics(double[] likelihoods, AveragedAnomalyRecordList aggRecordList, AnomalyParams params) {
        this.params = params;
        this.aggRecordList = aggRecordList;
        this.likelihoods = likelihoods;
    }
    
    /**
     * Utility method to copy this {@link AnomalyLikelihoodMetrics} object.
     * @return
     */
    public AnomalyLikelihoodMetrics copy() {
        List<Object> vals = new ArrayList<Object>();
        for(String key : params.keys()) {
            vals.add(params.get(key));
        }
        
        return new AnomalyLikelihoodMetrics(
            Arrays.copyOf(likelihoods, likelihoods.length), 
            aggRecordList, 
            new AnomalyParams(params.keys(), vals.toArray()));
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aggRecordList == null) ? 0 : aggRecordList.hashCode());
        result = prime * result + Arrays.hashCode(likelihoods);
        result = prime * result + ((params == null) ? 0 : params.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        AnomalyLikelihoodMetrics other = (AnomalyLikelihoodMetrics)obj;
        if(aggRecordList == null) {
            if(other.aggRecordList != null)
                return false;
        } else if(!aggRecordList.equals(other.aggRecordList))
            return false;
        if(!Arrays.equals(likelihoods, other.likelihoods))
            return false;
        if(params == null) {
            if(other.params != null)
                return false;
        } else if(!params.equals(other.params))
            return false;
        return true;
    }
}
