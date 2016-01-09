package org.numenta.nupic;

/**
 * <p>
 * The method used to compute distance between input patterns and prototype patterns. 
 * </p><p>
 * The possible options are:
 * <ul>
 *  <li>NORM:                   When distanceNorm is 2, this is the euclidean distance,
 *                              When distanceNorm is 1, this is the manhattan distance
 *                              In general: sum(abs(x-proto) ^ distanceNorm) ^ (1/distanceNorm)
 *                              The distances are normalized such that farthest prototype from
 *                              a given input is 1.0.</li>
 *            
 *  <li>RAW_OVERLAP:            Only appropriate when inputs are binary. This computes:
 *                              (width of the input) - (# bits of overlap between input
 *                              and prototype).</li>
 *  
 *  <li>PCT_INPUT_OVERLAP:      Only appropriate for binary inputs. This computes
 *                              1.0 - (# bits overlap between input and prototype) /
 *                              (# ON bits in input)
 *                              
 *  <li>PCT_PROTO_OVERLAP:      Only appropriate for binary inputs. This computes
 *                              1.0 - (# bits overlap between input and prototype) /
 *                              (# ON bits in prototype)
 *                               
 *  <li>PCT_LARGER_OVERLAP:     Only appropriate for binary inputs. This computes
 *                              1.0 - (# bits overlap between input and prototype) /
 *                              max(# ON bits in input, # ON bits in prototype)                   
 *                       
 *
 * @author Numenta
 * @author cogmission
 */
public enum DistanceMethod {
    NORM("norm"), 
    RAW_OVERLAP("rawOverlap"), 
    PCT_INPUT_OVERLAP("pctOverlapOfInput"),
    PCT_PROTO_OVERLAP("pctOverlapOfProto"),
    PCT_LARGER_OVERLAP("pctOverlapOfLarger");
    
    private String key;
    
    /**
     * Constructor passing the value used for KNN Distance Method key.
     * @param key   the string value identifying the distance method type.
     */
    private DistanceMethod(String key) {
        this.key = key;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return key;
    }
}
