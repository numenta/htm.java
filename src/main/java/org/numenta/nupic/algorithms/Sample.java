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

import org.joda.time.DateTime;
import org.numenta.nupic.model.Persistable;

/**
 * A sample data point or record consisting of a timestamp, value, and score.
 * This class is used as an input value to methods in the {@link AnomalyLikelihood}
 * class.
 */
public class Sample implements Persistable {
    private static final long serialVersionUID = 1L;
    
    public final DateTime date;
    /** Same thing as average */
    public final double score;
    /** Original value */
    public final double value;
    
    public Sample(DateTime timeStamp, double value, double score) {
        if(timeStamp == null) {
            throw new IllegalArgumentException("Sample must have a valid date");
        }
        this.date = timeStamp;
        this.value = value;
        this.score = score;
    }
    
    /**
     * Returns a {@link DateTime} object representing the internal timestamp
     * @return
     */
    public DateTime timeStamp() {
        return date;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return new StringBuilder(timeStamp().toString()).append(", value: ").
            append(value).append(", metric: ").append(score).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((date == null) ? 0 : date.hashCode());
        long temp;
        temp = Double.doubleToLongBits(score);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(value);
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
        Sample other = (Sample)obj;
        if(date == null) {
            if(other.date != null)
                return false;
        } else if(!date.equals(other.date))
            return false;
        if(Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
            return false;
        if(Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
            return false;
        return true;
    }

    
}
