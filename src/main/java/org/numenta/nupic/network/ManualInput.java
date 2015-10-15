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
import java.util.Map;

import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.encoders.Encoder;
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
 *      <li>{@link ClassifierResult}</li>
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
    private int recordNum;
    /** Tuple = { Name, inputValue, bucketIndex, encoding } */
    private Map<String, NamedTuple> classifierInput;
    /** Holds one classifier for each field */
    NamedTuple classifiers;
    private Object layerInput;
    private int[] sdr;
    private int[] encoding;
    private int[] activeColumns;
    private int[] sparseActives;
    private int[] previousPrediction;
    private int[] currentPrediction;
    private Map<String, ClassifierResult<Object>> classification;
    private double anomalyScore;
    private Object customObject;
    
    
    
    /**
     * Constructs a new {@code ManualInput}
     */
    public ManualInput() {}
    
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
     * Returns a custom Object during sequence processing where one or more 
     * {@link Func1}(s) were added to a {@link Layer} in between algorithmic
     * components.
     *  
     * @return  the custom object set during processing
     */
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
     * Returns the {@link Map} used as input into the {@link CLAClassifier}
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
     * names, whose values are the {@link CLAClassifier} used 
     * to track the classification of a particular field
     */
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
        retVal.activeColumns = Arrays.copyOf(this.activeColumns, this.activeColumns.length);
        retVal.sparseActives = Arrays.copyOf(this.sparseActives, this.sparseActives.length);
        retVal.previousPrediction = Arrays.copyOf(this.previousPrediction, this.previousPrediction.length);
        retVal.currentPrediction = Arrays.copyOf(this.currentPrediction, this.currentPrediction.length);
        retVal.classification = new HashMap<>(this.classification);
        retVal.anomalyScore = this.anomalyScore;
        retVal.customObject = this.customObject;
        
        return retVal;
    }
    
    /**
     * Returns the most recent {@link ClassifierResult}
     * 
     * @param fieldName
     * @return
     */
    @Override
    public ClassifierResult<Object> getClassification(String fieldName) {
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
    ManualInput storeClassification(String fieldName, ClassifierResult<Object> classification) {
        if(this.classification == null) {
            this.classification = new HashMap<String, ClassifierResult<Object>>();
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
    public int[] getActiveColumns() {
        return activeColumns;
    }
    
    /**
     * Sets the column activation from a {@link SpatialPooler}
     * @param cols
     * @return
     */
    public ManualInput activeColumns(int[] cols) {
        this.activeColumns = cols;
        return this;
    }
    
    /**
     * Returns the column activations in sparse form
     * @return
     */
    @Override
    public int[] getSparseActives() {
        if(sparseActives == null && activeColumns != null) {
            sparseActives = ArrayUtils.where(activeColumns, ArrayUtils.WHERE_1);
        }
        return sparseActives;
    }

    /**
     * Sets the column activations in sparse form.
     * @param cols
     * @return
     */
    public ManualInput sparseActives(int[] cols) {
        this.sparseActives = cols;
        return this;
    }
    
    /**
     * Returns the predicted output from the last inference cycle.
     * @return
     */
    @Override
    public int[] getPreviousPrediction() {
        return previousPrediction;
    }
    
    /**
     * Sets the previous predicted columns.
     * @param cols
     * @return
     */
    public ManualInput previousPrediction(int[] cols) {
        this.previousPrediction = cols;
        return this;
    }
    
    /**
     * Returns the currently predicted columns.
     * @return
     */
    @Override
    public int[] getPredictedColumns() {
        return currentPrediction;
    }
    
    /**
     * Sets the currently predicted columns
     * @param cols
     * @return
     */
    public ManualInput predictedColumns(int[] cols) {
        previousPrediction = currentPrediction;
        this.currentPrediction = cols;
        return this;
    }
}
