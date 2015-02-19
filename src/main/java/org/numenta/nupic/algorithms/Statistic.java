package org.numenta.nupic.algorithms;

import org.numenta.nupic.util.NamedTuple;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
public class Statistic {
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
    
    /**
     * Creates and returns a JSON ObjectNode containing this Statistic's data.
     * 
     * @param factory
     * @return
     */
    public ObjectNode toJson(JsonNodeFactory factory) {
        ObjectNode distribution = factory.objectNode();
        distribution.put("mean", mean);
        distribution.put("variance", variance);
        distribution.put("stdev", stdev);
        
        return distribution;
    }
}
