package org.numenta.nupic.algorithms;

import org.joda.time.DateTime;

/**
 * A sample data point or record consisting of a timestamp, value, and score.
 * This class is used as an input value to methods in the {@link AnomalyLikelihood}
 * class.
 */
class Sample {
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