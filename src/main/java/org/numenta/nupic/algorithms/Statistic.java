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

import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.util.NamedTuple;

/**
 * Container to hold a specific calculation for a statistical data point.
 * 
 * Follows the form:
 * <pre>
 * {
 *    "distribution":               # describes the distribution
 *     {
 *        "name": STRING,           # name of the distribution, such as 'normal'
 *        "mean": SCALAR,           # mean of the distribution
 *        "variance": SCALAR,       # variance of the distribution
 *
 *        # There may also be some keys that are specific to the distribution
 *     }
 * </pre>
 * @author David Ray
 */
public class Statistic implements Persistable {
    private static final long serialVersionUID = 1L;
    
    public final double mean;
    public final double variance;
    public final double stdev;
    public final NamedTuple entries;
    
    public Statistic(double mean, double variance, double stdev) {
        this.mean = mean;
        this.variance = variance;
        this.stdev = stdev;
        
        this.entries = new NamedTuple(new String[] { "mean", "variance", "stdev" }, mean, variance, stdev);
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp = Double.doubleToLongBits(mean);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(stdev);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(variance);
        result = prime * result + (int)(temp ^ (temp >>> 32));
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
        Statistic other = (Statistic)obj;
        if(Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean))
            return false;
        if(Double.doubleToLongBits(stdev) != Double.doubleToLongBits(other.stdev))
            return false;
        if(Double.doubleToLongBits(variance) != Double.doubleToLongBits(other.variance))
            return false;
        return true;
    }
}
