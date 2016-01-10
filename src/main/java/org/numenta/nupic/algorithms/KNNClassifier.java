/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2015, Numenta, Inc.  Unless you have an agreement
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

import java.lang.reflect.Field;

import org.numenta.nupic.Constants;
import org.numenta.nupic.DistanceMethod;
import org.numenta.nupic.Parameters;

/**
 * This class implements NuPIC's k Nearest Neighbor Classifier. KNN is very
 * useful as a basic classifier for many situations. This implementation contains
 * many enhancements that are useful for HTM experiments. These enhancements
 * include an optimized C++ class for sparse vectors, support for continuous
 * online learning, support for various distance methods (including Lp-norm and
 * raw overlap), support for performing SVD on the input vectors (very useful for
 * large vectors), support for a fixed-size KNN, and a mechanism to store custom
 * ID's for each vector.
 * 
 * @author Numenta
 * @author cogmission
 */
public class KNNClassifier {
    /** The number of nearest neighbors used in the classification of patterns. <b>Must be odd</b> */
    private int k = 1;
    /** If true, patterns must match exactly when assigning class labels */
    private boolean exact;
    /** When distance method is "norm", this specifies the p value of the Lp-norm */
    private double distanceNorm;
    /** 
     * The method used to compute distance between input patterns and prototype patterns.
     * see({@link DistanceMethod}) 
     */
    private DistanceMethod distanceMethod;
    /** 
     * A threshold on the distance between learned
     * patterns and a new pattern proposed to be learned. The distance must be
     * greater than this threshold in order for the new pattern to be added to
     * the classifier's memory
     */
    private double distanceThreshold;
    /** If True, then scalar inputs will be binarized. */
    private boolean doBinarization;
    /** If doBinarization is True, this specifies the threshold for the binarization of inputs */
    private double binarizationThreshold;
    /** If True, classifier will use a sparse memory matrix */
    private boolean useSparseMemory;
    /** 
     * If useSparseMemory is True, input variables whose absolute values are 
     * less than this threshold will be stored as zero
     */
    private double sparseThreshold;
    /** Flag specifying whether to multiply sparseThreshold by max value in input */
    private boolean relativeThreshold;
    /** Number of elements of the input that are stored. If 0, all elements are stored */
    private int numWinners;
    /** 
     * Number of samples the must occur before a SVD
     * (Singular Value Decomposition) transformation will be performed. If 0,
     * the transformation will never be performed
     */
    private int numSVDSamples;
    /** 
     * Controls dimensions kept after SVD transformation. If "adaptive", 
     * the number is chosen automatically
     */
    private Constants.KNN numSVDDims;
    /**
     * If numSVDDims is "adaptive", this controls the
     * smallest singular value that is retained as a fraction of the largest
     * singular value
     */
    private double fractionOfMax;
    /**
     * Limits the maximum number of the training
     * patterns stored. When KNN learns in a fixed capacity mode, the unused
     * patterns are deleted once the number of stored patterns is greater than
     * maxStoredPatterns. A value of -1 is no limit
     */
    private int maxStoredPatterns;
    /**
     * A boolean flag that determines whether,
     * during learning, the classifier replaces duplicates that match exactly,
     * even if distThreshold is 0. Should be TRUE for online learning
     */
    private boolean replaceDuplicates;
    /**
     * If >= 1, input is assumed to be organized into
     * columns, in the same manner as the temporal pooler AND whenever a new
     * prototype is stored, only the start cell (first cell) is stored in any
     * bursting column
     */
    private int cellsPerCol;
    
    
    
    
    /**
     * Privately constructs a {@code KNNClassifier}. 
     * This method is called by the
     */
    private KNNClassifier() {}
    
    /**
     * Returns a {@link Builder} used to fully construct a {@code KNNClassifier}
     * @return
     */
    public static Builder builder() {
        return new KNNClassifier.Builder();
    }
    
    ///////////////////////////////////////////////////////
    //                  Core Methods                     //
    ///////////////////////////////////////////////////////
    /**
     * Train the classifier to associate specified input pattern with a
     * particular category.
     * 
     * @param inputPattern      The pattern to be assigned a category. If
     *                          isSparse is 0, this should be a dense array (both ON and OFF bits
     *                          present). Otherwise, if isSparse > 0, this should be a list of the
     *                          indices of the non-zero bits in sorted order
     *                          
     * @param inputCategory     The category to be associated with the training pattern
     * 
     * @param partitionId       allows you to associate an id with each
     *                          input vector. It can be used to associate input patterns stored in the
     *                          classifier with an external id. This can be useful for debugging or
     *                          visualizing. Another use case is to ignore vectors with a specific id
     *                          during inference (see description of infer() for details). There can be
     *                          at most one partitionId per stored pattern (i.e. if two patterns are
     *                          within distThreshold, only the first partitionId will be stored). This
     *                          is an optional parameter.
     *                          
     * @param sparseSpec        If 0, the input pattern is a dense representation. If
     *                          isSparse > 0, the input pattern is a list of non-zero indices and
     *                          isSparse is the length of the dense representation
     *                          
     * @return                  The number of patterns currently stored in the classifier
     */
    public int learn(int[] inputPattern, int inputCategory, int partitionId, int sparseSpec) {
        return -1;
    }
    
    ///////////////////////////////////////////////////////
    //                  Accessor Methods                 //
    ///////////////////////////////////////////////////////
    /**
     * Returns the number of nearest neighbors used in the classification of patterns. <b>Must be odd</b>
     * @return the k
     */
    public int getK() {
        return k;
    }
    
    /**
     * If true, patterns must match exactly when assigning class labels
     * @return the exact
     */
    public boolean isExact() {
        return exact;
    }
    
    /**
     * When distance method is "norm", this specifies the p value of the Lp-norm
     * @return the distanceNorm
     */
    public double getDistanceNorm() {
        return distanceNorm;
    }
    
    /**
     * The method used to compute distance between input patterns and prototype patterns.
     * see({@link DistanceMethod}) 
     * 
     * @return the distanceMethod
     */
    public DistanceMethod getDistanceMethod() {
        return distanceMethod;
    }
    
    /**
     * A threshold on the distance between learned
     * patterns and a new pattern proposed to be learned. The distance must be
     * greater than this threshold in order for the new pattern to be added to
     * the classifier's memory
     * 
     * @return the distanceThreshold
     */
    public double getDistanceThreshold() {
        return distanceThreshold;
    }
    
    /**
     * If True, then scalar inputs will be binarized.
     * @return the doBinarization
     */
    public boolean isDoBinarization() {
        return doBinarization;
    }
    
    /**
     * If doBinarization is True, this specifies the threshold for the binarization of inputs
     * @return the binarizationThreshold
     */
    public double getBinarizationThreshold() {
        return binarizationThreshold;
    }
    
    /**
     * If True, classifier will use a sparse memory matrix
     * @return the useSparseMemory
     */
    public boolean isUseSparseMemory() {
        return useSparseMemory;
    }
    
    /**
     * If useSparseMemory is True, input variables whose absolute values are 
     * less than this threshold will be stored as zero
     * @return the sparseThreshold
     */
    public double getSparseThreshold() {
        return sparseThreshold;
    }
    
    /**
     * Flag specifying whether to multiply sparseThreshold by max value in input
     * @return the relativeThreshold
     */
    public boolean isRelativeThreshold() {
        return relativeThreshold;
    }
    
    /**
     * Number of elements of the input that are stored. If 0, all elements are stored
     * @return the numWinners
     */
    public int getNumWinners() {
        return numWinners;
    }
    
    /**
     * Number of samples the must occur before a SVD
     * (Singular Value Decomposition) transformation will be performed. If 0,
     * the transformation will never be performed
     * 
     * @return the numSVDSamples
     */
    public int getNumSVDSamples() {
        return numSVDSamples;
    }
    
    /**
     * Controls dimensions kept after SVD transformation. If "adaptive", 
     * the number is chosen automatically
     * 
     * @return the numSVDDims
     */
    public Constants.KNN getNumSVDDims() {
        return numSVDDims;
    }
    
    /**
     * If numSVDDims is "adaptive", this controls the
     * smallest singular value that is retained as a fraction of the largest
     * singular value
     * 
     * @return the fractionOfMax
     */
    public double getFractionOfMax() {
        return fractionOfMax;
    }
    
    /**
     * Limits the maximum number of the training
     * patterns stored. When KNN learns in a fixed capacity mode, the unused
     * patterns are deleted once the number of stored patterns is greater than
     * maxStoredPatterns. A value of -1 is no limit
     * 
     * @return the maxStoredPatterns
     */
    public int getMaxStoredPatterns() {
        return maxStoredPatterns;
    }
    
    /**
     * A boolean flag that determines whether,
     * during learning, the classifier replaces duplicates that match exactly,
     * even if distThreshold is 0. Should be TRUE for online learning
     * 
     * @return the replaceDuplicates
     */
    public boolean isReplaceDuplicates() {
        return replaceDuplicates;
    }
    
    /**
     * If >= 1, input is assumed to be organized into
     * columns, in the same manner as the temporal pooler AND whenever a new
     * prototype is stored, only the start cell (first cell) is stored in any
     * bursting column
     * 
     * @return the cellsPerCol
     */
    public int getCellsPerCol() {
        return cellsPerCol;
    }
    
    /**
     * Implements the Builder Pattern for creating {@link KNNClassifier}s.
     */
    public static class Builder {
        private KNNClassifier fieldHolder = new KNNClassifier();
        
        public Builder() {}
        
        /**
         * Returns a new KNNClassifier contructed from the fields specified
         * by this {@code Builder}
         * @return
         */
        public KNNClassifier build() {
            KNNClassifier retVal = new KNNClassifier();
            for(Field f : fieldHolder.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    f.set(retVal, f.get(fieldHolder));
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
            return retVal;
        }
        
        /**
         * Returns a thoroughly constructed KNNClassifier using the 
         * parameters specified by the argument.
         * @param p
         * @return
         */
        public KNNClassifier apply(Parameters p) {
            KNNClassifier retVal = new KNNClassifier();
            p.apply(retVal);
            return retVal;
        }
        
        /**
         * The number of nearest neighbors used in the classification of patterns. <b>Must be odd</b>
         * @param k
         * @return this Builder
         */
        public Builder k(int k) {
            fieldHolder.k = k;
            return this;
        }
        
        /**
         * If true, patterns must match exactly when assigning class labels
         * @param b
         * @return this Builder
         */
        public Builder exact(boolean b) {
            fieldHolder.exact = b;
            return this;
        }
        
        /**
         * When distance method is "norm", this specifies the p value of the Lp-norm
         * @param distanceNorm
         * @return this Builder
         */
        public Builder distanceNorm(double distanceNorm) {
            fieldHolder.distanceNorm = distanceNorm;
            return this;
        }
        
        /**
         * The method used to compute distance between input patterns and prototype patterns.
         * see({@link DistanceMethod})
         * @param method
         * @return
         */
        public Builder distanceMethod(DistanceMethod method) {
            fieldHolder.distanceMethod = method;
            return this;
        }
        
        /**
         * A threshold on the distance between learned
         * patterns and a new pattern proposed to be learned. The distance must be
         * greater than this threshold in order for the new pattern to be added to
         * the classifier's memory
         * 
         * @param threshold
         * @return this Builder
         */
        public Builder distanceThreshold(double threshold) {
            fieldHolder.distanceThreshold = threshold;
            return this;
        }
        
        /**
         * If True, then scalar inputs will be binarized.
         * @param b
         * @return this Builder
         */
        public Builder doBinarization(boolean b) {
            fieldHolder.doBinarization = b;
            return this;
        }
        
        /**
         * If doBinarization is True, this specifies the threshold for the binarization of inputs
         * @param threshold
         * @return  this Builder
         */
        public Builder binarizationThreshold(double threshold) {
            fieldHolder.binarizationThreshold = threshold;
            return this;
        }
        
        /**
         * If True, classifier will use a sparse memory matrix
         * @param b
         * @return  this Builder
         */
        public Builder useSparseMemory(boolean b) {
            fieldHolder.useSparseMemory = b;
            return this;
        }
        
        /**
         * If useSparseMemory is True, input variables whose absolute values are 
         * less than this threshold will be stored as zero
         * @param threshold
         * @return  this Builder
         */
        public Builder sparseThreshold(double threshold) {
            fieldHolder.sparseThreshold = threshold;
            return this;
        }
        
        /**
         * Flag specifying whether to multiply sparseThreshold by max value in input
         * @param b
         * @return  this Builder
         */
        public Builder relativeThreshold(boolean b) {
            fieldHolder.relativeThreshold = b;
            return this;
        }
        
        /**
         * Number of elements of the input that are stored. If 0, all elements are stored
         * @param b
         * @return  this Builder
         */
        public Builder numWinners(int num) {
            fieldHolder.numWinners = num;
            return this;
        }
        
        /**
         * Number of samples the must occur before a SVD
         * (Singular Value Decomposition) transformation will be performed. If 0,
         * the transformation will never be performed
         * 
         * @param b
         * @return  this Builder
         */
        public Builder numSVDSamples(int num) {
            fieldHolder.numSVDSamples = num;
            return this;
        }
        
        /**
         * Controls dimensions kept after SVD transformation. If "adaptive", 
         * the number is chosen automatically
         * @param con
         * @return  this Builder
         */
        public Builder numSVDDims(Constants.KNN constant) {
            fieldHolder.numSVDDims = constant;
            return this;
        }
        
        /**
         * If numSVDDims is "adaptive", this controls the
         * smallest singular value that is retained as a fraction of the largest
         * singular value
         * 
         * @param fraction
         * @return  this Builder
         */
        public Builder fractionOfMax(double fraction) {
            fieldHolder.fractionOfMax = fraction;
            return this;
        }
        
        /**
         * Limits the maximum number of the training
         * patterns stored. When KNN learns in a fixed capacity mode, the unused
         * patterns are deleted once the number of stored patterns is greater than
         * maxStoredPatterns. A value of -1 is no limit
         * 
         * @param max
         * @return  the Builder
         */
        public Builder maxStoredPatterns(int max) {
            fieldHolder.maxStoredPatterns = max;
            return this;
        }
        
        /**
         * A boolean flag that determines whether,
         * during learning, the classifier replaces duplicates that match exactly,
         * even if distThreshold is 0. Should be TRUE for online learning
         * @param b
         * @return  this Builder
         */
        public Builder replaceDuplicates(boolean b) {
            fieldHolder.replaceDuplicates = b;
            return this;
        }
        
        /**
         * If >= 1, input is assumed to be organized into
         * columns, in the same manner as the temporal pooler AND whenever a new
         * prototype is stored, only the start cell (first cell) is stored in any
         * bursting column
         * @param num
         * @return  this Builder
         */
        public Builder cellsPerCol(int num) {
            fieldHolder.cellsPerCol = num;
            return this;
        }
    }
    
}
