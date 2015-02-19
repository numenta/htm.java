package org.numenta.nupic.algorithms;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.List;


/**
 * Container to hold interim {@link AnomalyLikelihood} calculations.
 * 
 * @author David Ray
 * @see AnomalyLikelihood
 * @see MovingAverage
 */
public class AveragedAnomalyRecordList {
    List<Sample> averagedRecords;
    TDoubleList historicalValues;
    double total;
    
    /**
     * Constructs a new {@code AveragedAnomalyRecordList}
     * 
     * @param averagedRecords       List of samples which are { timestamp, average, value } at a data point
     * @param historicalValues      List of values of a given window size (moving average grouping)
     * @param total                 Sum of all values in the series
     */
    public AveragedAnomalyRecordList(List<Sample> averagedRecords, TDoubleList historicalValues, double total) {
        this.averagedRecords = averagedRecords;
        this.historicalValues = historicalValues;
        this.total = total;
    }
    
    /**
     * Returns a list of the averages in the contained averaged record list.
     * @return
     */
    public TDoubleList getMetrics() {
        TDoubleList retVal = new TDoubleArrayList();
        for(Sample s : averagedRecords) {
            retVal.add(s.score);
        }
        
        return retVal;
    }
    
    /**
     * Returns a list of the sample values in the contained averaged record list.
     * @return
     */
    public TDoubleList getSamples() {
        TDoubleList retVal = new TDoubleArrayList();
        for(Sample s : averagedRecords) {
            retVal.add(s.value);
        }
        
        return retVal;
    }
    
    /**
     * Returns the size of the count of averaged records (i.e. {@link Sample}s)
     * @return
     */
    public int size() {
        return averagedRecords.size(); //let fail if null
    }
}
