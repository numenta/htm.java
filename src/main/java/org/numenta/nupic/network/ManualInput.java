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
package org.numenta.nupic.network;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.algorithms.Classifier;
import org.numenta.nupic.algorithms.Classification;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.NamedTuple;

import rx.functions.Func1;

/**
 * <p>
 * Abstraction used within the Network API, to contain the significant return values of all {@link Layer}
 * inference participating algorithms.
 * </p>
 * Namely:
 * <ul>
 *      <li>Input Value</li>
 *      <li>Bucket Index</li>
 *      <li>SDR</li>
 *      <li>Previous SDR</li>
 *      <li>{@link Classification}</li>
 *      <li>anomalyScore</li>
 * </ul>
 * 
 * All of these fields are "optional", (meaning they depend on the configuration 
 * selected by the user and may not exist depending on the user's choice of "terminal"
 * point. A "Terminal" point is the end point in a chain of a {@code Layer}'s contained
 * algorithms. For instance, if the user does not include an {@link Encoder} in the 
 * {@link Layer} constructor, the slot containing the "Bucket Index" will be empty.
 * 
 * @author David Ray
 *
 */
public class ManualInput implements Inference {
    private static final long serialVersionUID = 1L;
    
    private int recordNum;
    /** Tuple = { Name, inputValue, bucketIndex, encoding } */
    private Map<String, NamedTuple> classifierInput;
    /** Holds one classifier for each field */
    NamedTuple classifiers;
    private Object layerInput;
    private int[] sdr;
    private int[] encoding;
    /** Active columns in the {@link SpatialPooler} at time "t" */
    private int[] feedForwardActiveColumns;
    /** Active column indexes from the {@link SpatialPooler} at time "t" */
    private int[] feedForwardSparseActives;
    /** Predictive {@link Cell}s in the {@link TemporalMemory} at time "t - 1" */
    private Set<Cell> previousPredictiveCells;
    /** Predictive {@link Cell}s in the {@link TemporalMemory} at time "t" */
    private Set<Cell> predictiveCells;
    /** Active {@link Cell}s in the {@link TemporalMemory} at time "t" */
    private Set<Cell> activeCells;
    
    private Map<String, Classification<Object>> classification;
    private double anomalyScore;
    private Object customObject;
    
    ComputeCycle computeCycle;
    
    
    /**
     * Constructs a new {@code ManualInput}
     */
    public ManualInput() {}
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T postDeSerialize(T manualInput) {
        ManualInput mi = (ManualInput)manualInput;
        
        ManualInput retVal = new ManualInput();
        retVal.activeCells = mi.activeCells;
        retVal.anomalyScore = mi.anomalyScore;
        retVal.classification = mi.classification;
        retVal.classifierInput = mi.classifierInput;
        retVal.classifiers = mi.classifiers;
        retVal.customObject = mi.customObject;
        retVal.encoding = mi.encoding;
        retVal.feedForwardActiveColumns = mi.feedForwardActiveColumns;
        retVal.feedForwardSparseActives = mi.feedForwardSparseActives;
        retVal.layerInput = mi.layerInput;
        retVal.predictiveCells = mi.predictiveCells;
        retVal.previousPredictiveCells = mi.previousPredictiveCells;
        retVal.sdr = mi.sdr;
        
        return (T)retVal;
    }
    
    /**
     * Sets the current record num associated with this {@code ManualInput}
     * instance
     * 
     * @param num   the current sequence number.
     * @return      this
     */
    public ManualInput recordNum(int num) {
        this.recordNum = num;
        return this;
    }
    
    /**
     * Returns the current record num associated with this {@code ManualInput}
     * instance
     * 
     * @return      the current sequence number
     */
    @Override
    public int getRecordNum() {
        return recordNum;
    }
    
    /**
     * Sets the {@link ComputeCycle} from the TemporalMemory
     * @param computeCycle
     */
    public ManualInput computeCycle(ComputeCycle computeCycle) {
        this.computeCycle = computeCycle;
        return this;
    }
    
    /**
     * Returns the {@link ComputeCycle}
     * @return
     */
    @Override
    public ComputeCycle getComputeCycle() {
        return computeCycle;
    }
    
    /**
     * Returns a custom Object during sequence processing where one or more 
     * {@link Func1}(s) were added to a {@link Layer} in between algorithmic
     * components.
     *  
     * @return  the custom object set during processing
     */
    @Override
    public Object getCustomObject() {
        return customObject;
    }
    
    /**
     * Sets a custom Object during sequence processing where one or more 
     * {@link Func1}(s) were added to a {@link Layer} in between algorithmic
     * components.
     *  
     * @param o
     * @return
     */
    public ManualInput customObject(Object o) {
        this.customObject = o;
        return this;
    }
    
    /**
     * <p>
     * Returns the {@link Map} used as input into the field's {@link Classifier}
     * (it is only actually used as input if a Classifier type has specified for
     * the field).
     * 
     * This mapping contains the name of the field being classified mapped
     * to a {@link NamedTuple} containing:
     * </p><p>
     * <ul>
     *      <li>name</li>
     *      <li>inputValue</li>
     *      <li>bucketIdx</li>
     *      <li>encoding</li>
     * </ul>
     * 
     * @return the current classifier input
     */
    @Override
    public Map<String, NamedTuple> getClassifierInput() {
        if(classifierInput == null) {
            classifierInput = new HashMap<String, NamedTuple>();
        }
        return classifierInput;
    }
    
    /**
     * Sets the current 
     * @param classifierInput
     * @return
     */
    ManualInput classifierInput(Map<String, NamedTuple> classifierInput) {
        this.classifierInput = classifierInput;
        return this;
    }
    
    /**
     * Sets the {@link NamedTuple} containing the classifiers used
     * for each particular input field.
     * 
     * @param tuple
     * @return
     */
    public ManualInput classifiers(NamedTuple tuple) {
        this.classifiers = tuple;
        return this;
    }
    
    /**
     * Returns a {@link NamedTuple} keyed to the input field
     * names, whose values are the {@link Classifier} used
     * to track the classification of a particular field
     */
    @Override
    public NamedTuple getClassifiers() {
        return classifiers;
    }
    
    /**
     * Returns the most recent input object
     * 
     * @return      the input
     */
    @Override
    public Object getLayerInput() {
        return layerInput;
    }
    
    /**
     * Sets the input object to be used and returns 
     * this {@link ManualInput}
     * 
     * @param inputValue
     * @return
     */
    ManualInput layerInput(Object inputValue) {
        this.layerInput = inputValue;
        return this;
    }
    
    /**
     * Returns the <em>Sparse Distributed Representation</em> vector
     * which is the result of all algorithms in a series of algorithms
     * passed up the hierarchy.
     * 
     * @return
     */
    @Override
    public int[] getSDR() {
        return sdr;
    }
    
    /**
     * Inputs an sdr and returns this {@code ManualInput}
     * 
     * @param sdr
     * @return
     */
    ManualInput sdr(int[] sdr) {
        this.sdr = sdr;
        return this;
    }
    
    /**
     * Returns the initial encoding produced by an {@link Encoder}
     * or one of its subtypes.
     * 
     * @return
     */
    @Override
    public int[] getEncoding() {
        return encoding;
    }
    
    /**
     * Inputs the initial encoding and return this {@code ManualInput}
     * @param sdr
     * @return
     */
    ManualInput encoding(int[] sdr) {
        this.encoding = sdr;
        return this;
    }
    
    /**
     * Convenience method to provide an isolated copy of 
     * this {@link Inference}
     *  
     * @return
     */
    ManualInput copy() {
        ManualInput retVal = new ManualInput();
        retVal.classifierInput = new HashMap<String, NamedTuple>(this.classifierInput);
        retVal.classifiers = new NamedTuple(this.classifiers.keys(), this.classifiers.values().toArray());
        retVal.layerInput = this.layerInput;
        retVal.sdr = Arrays.copyOf(this.sdr, this.sdr.length);
        retVal.encoding = Arrays.copyOf(this.encoding, this.encoding.length);
        retVal.feedForwardActiveColumns = Arrays.copyOf(this.feedForwardActiveColumns, this.feedForwardActiveColumns.length);
        retVal.feedForwardSparseActives = Arrays.copyOf(this.feedForwardSparseActives, this.feedForwardSparseActives.length);
        retVal.previousPredictiveCells = new LinkedHashSet<Cell>(this.previousPredictiveCells);
        retVal.predictiveCells = new LinkedHashSet<Cell>(this.predictiveCells);
        retVal.classification = new HashMap<>(this.classification);
        retVal.anomalyScore = this.anomalyScore;
        retVal.customObject = this.customObject;
        retVal.computeCycle = this.computeCycle;
        retVal.activeCells = new LinkedHashSet<Cell>(this.activeCells);
        
        return retVal;
    }
    
    /**
     * Returns the most recent {@link Classification}
     * 
     * @param fieldName
     * @return the most recent {@link Classification}, or null if none exists.
     */
    @Override
    public Classification<Object> getClassification(String fieldName) {
        if(classification == null)
            return null;
        return classification.get(fieldName);
    }
    
    /**
     * Sets the specified field's last classifier computation and returns
     * this {@link Inference}
     * 
     * @param fieldName
     * @param classification
     * @return
     */
    ManualInput storeClassification(String fieldName, Classification<Object> classification) {
        if(this.classification == null) {
            this.classification = new HashMap<String, Classification<Object>>();
        }
        this.classification.put(fieldName, classification);
        return this;
    }
    
    /**
     * Returns the most recent anomaly calculation.
     * @return
     */
    @Override
    public double getAnomalyScore() {
        return anomalyScore;
    }
    
    /**
     * Sets the current computed anomaly score and 
     * returns this {@link Inference}
     * 
     * @param d
     * @return
     */
    ManualInput anomalyScore(double d) {
        this.anomalyScore = d;
        return this;
    }
    
    /**
     * Returns the column activation from a {@link SpatialPooler}
     * @return
     */
    @Override
    public int[] getFeedForwardActiveColumns() {
        return feedForwardActiveColumns;
    }
    
    /**
     * Sets the column activation from a {@link SpatialPooler}
     * @param cols
     * @return
     */
    public ManualInput feedForwardActiveColumns(int[] cols) {
        this.feedForwardActiveColumns = cols;
        return this;
    }
    
    /**
     * Returns the column activation from a {@link TemporalMemory}
     * @return
     */
    @Override
    public Set<Cell> getActiveCells() {
        return activeCells;
    }
    
    /**
     * Sets the column activation from a {@link TemporalMemory}
     * @param cells
     * @return
     */
    public ManualInput activeCells(Set<Cell> cells) {
        this.activeCells = cells;
        return this;
    }
    
    /**
     * Returns the column activations in sparse form
     * @return
     */
    @Override
    public int[] getFeedForwardSparseActives() {
        if(feedForwardSparseActives == null && feedForwardActiveColumns != null) {
            feedForwardSparseActives = ArrayUtils.where(feedForwardActiveColumns, ArrayUtils.WHERE_1);
        }
        return feedForwardSparseActives;
    }

    /**
     * Sets the column activations in sparse form.
     * @param cols
     * @return
     */
    public ManualInput feedForwardSparseActives(int[] cols) {
        this.feedForwardSparseActives = cols;
        return this;
    }
    
    /**
     * Returns the predicted output from the last inference cycle.
     * @return
     */
    @Override
    public Set<Cell> getPreviousPredictiveCells() {
        return previousPredictiveCells;
    }
    
    /**
     * Sets the previous predicted columns.
     * @param cells
     * @return
     */
    public ManualInput previousPredictiveCells(Set<Cell> cells) {
        this.previousPredictiveCells = cells;
        return this;
    }
    
    /**
     * Returns the currently predicted columns.
     * @return
     */
    @Override
    public Set<Cell> getPredictiveCells() {
        return predictiveCells;
    }
    
    /**
     * Sets the currently predicted columns
     * @param cells
     * @return
     */
    public ManualInput predictiveCells(Set<Cell> cells) {
        previousPredictiveCells = predictiveCells;
        this.predictiveCells = cells;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((activeCells == null) ? 0 : activeCells.hashCode());
        long temp;
        temp = Double.doubleToLongBits(anomalyScore);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + ((classification == null) ? 0 : classification.hashCode());
        result = prime * result + ((classifierInput == null) ? 0 : classifierInput.hashCode());
        result = prime * result + ((computeCycle == null) ? 0 : computeCycle.hashCode());
        result = prime * result + Arrays.hashCode(encoding);
        result = prime * result + Arrays.hashCode(feedForwardActiveColumns);
        result = prime * result + Arrays.hashCode(feedForwardSparseActives);
        result = prime * result + ((predictiveCells == null) ? 0 : predictiveCells.hashCode());
        result = prime * result + ((previousPredictiveCells == null) ? 0 : previousPredictiveCells.hashCode());
        result = prime * result + recordNum;
        result = prime * result + Arrays.hashCode(sdr);
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(!Inference.class.isAssignableFrom(obj.getClass()))
            return false;
        ManualInput other = (ManualInput)obj;
        if(activeCells == null) {
            if(other.activeCells != null)
                return false;
        } else if(!activeCells.equals(other.activeCells))
            return false;
        if(Double.doubleToLongBits(anomalyScore) != Double.doubleToLongBits(other.anomalyScore))
            return false;
        if(classification == null) {
            if(other.classification != null)
                return false;
        } else if(!classification.equals(other.classification))
            return false;
        if(classifierInput == null) {
            if(other.classifierInput != null)
                return false;
        } else if(!classifierInput.equals(other.classifierInput))
            return false;
        if(computeCycle == null) {
            if(other.computeCycle != null)
                return false;
        } else if(!computeCycle.equals(other.computeCycle))
            return false;
        if(!Arrays.equals(encoding, other.encoding))
            return false;
        if(!Arrays.equals(feedForwardActiveColumns, other.feedForwardActiveColumns))
            return false;
        if(!Arrays.equals(feedForwardSparseActives, other.feedForwardSparseActives))
            return false;
        if(predictiveCells == null) {
            if(other.predictiveCells != null)
                return false;
        } else if(!predictiveCells.equals(other.predictiveCells))
            return false;
        if(previousPredictiveCells == null) {
            if(other.previousPredictiveCells != null)
                return false;
        } else if(!previousPredictiveCells.equals(other.previousPredictiveCells))
            return false;
        if(recordNum != other.recordNum)
            return false;
        if(!Arrays.equals(sdr, other.sdr))
            return false;
        return true;
    }

}
