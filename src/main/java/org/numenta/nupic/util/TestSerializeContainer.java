package org.numenta.nupic.util;

import java.io.Serializable;

import org.numenta.nupic.encoders.DateEncoder;

public class TestSerializeContainer implements Serializable {
    private static final long serialVersionUID = 1L;
   
    private MinMax minMax;
    private Tuple tuple;
    private DateEncoder dateEncoder;
    
    public TestSerializeContainer() {
        
    }

    
    /**
     * @return the minMax
     */
    public MinMax getMinMax() {
        return minMax;
    }

    
    /**
     * @return the tuple
     */
    public Tuple getTuple() {
        return tuple;
    }

    
    /**
     * @return the dateEncoder
     */
    public DateEncoder getDateEncoder() {
        return dateEncoder;
    }

    
    /**
     * @param minMax the minMax to set
     */
    public void setMinMax(MinMax minMax) {
        this.minMax = minMax;
    }

    
    /**
     * @param tuple the tuple to set
     */
    public void setTuple(Tuple tuple) {
        this.tuple = tuple;
    }

    
    /**
     * @param dateEncoder the dateEncoder to set
     */
    public void setDateEncoder(DateEncoder dateEncoder) {
        this.dateEncoder = dateEncoder;
    }

    
    
}
