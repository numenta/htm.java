/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.research;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;

/**
 * Specifies parameters to be used as a configuration for a given {@link TemporalMemory}
 * or {@link SpatialPooler}
 * 
 * @author David Ray
 * 
 * @see SpatialPooler
 * @see TemporalMemory
 * @see Connections
 * @see ComputeCycle
 */
@SuppressWarnings("unused")
public class Parameters {
    /**
     * Constant values representing configuration parameters for the {@link TemporalMemory}
     */
    public enum KEY { 
        /////////// Universal Parameters ///////////
        COLUMN_DIMENSIONS("columnDimensions", new int[] { 2048 }),
        CELLS_PER_COLUMN("cellsPerColumn", 32),
        RANDOM("random", -1),
        SEED("seed", 42),
        
        /////////// Temporal Memory Parameters ///////////
        ACTIVATION_THRESHOLD("activationThreshold", 13),
        LEARNING_RADIUS("learningRadius", 2048),
        MIN_THRESHOLD("minThreshold", 10),
        MAX_NEW_SYNAPSE_COUNT("maxNewSynapseCount", 20),
        INITIAL_PERMANENCE("initialPermanence", 0.21),
        CONNECTED_PERMANENCE("connectedPermanence", 0.5),
        PERMANENCE_INCREMENT("permanenceIncrement", 0.10), 
        PERMANENCE_DECREMENT("permanenceDecrement", 0.10),
        TM_VERBOSITY("tmVerbosity", 0),
        
        /////////// Spatial Pooler Parameters ///////////
        INPUT_DIMENSIONS("inputDimensions", new int[] { 64 }),
        POTENTIAL_RADIUS("potentialRadius", 16),
        POTENTIAL_PCT("potentialPct", 0.5),
        GLOBAL_INHIBITIONS("globalInhibition", false),
        INHIBITION_RADIUS("inhibitionRadius", 0),
        LOCAL_AREA_DENSITY("localAreaDensity", -1.0),
        NUM_ACTIVE_COLUMNS_PER_INH_AREA("numActiveColumnsPerInhArea", 10),
        STIMULUS_THRESHOLD("stimulusThreshold", 0),
        SYN_PERM_INACTIVE_DEC("synPermInactiveDec", 0.01),
        SYN_PERM_ACTIVE_INC("synPermActiveInc", 0.1),
        SYN_PERM_CONNECTED("synPermConnected", 0.10),
        SYN_PERM_BELOW_STIMULUS("synPermBelowStimulusInc", 0.01),
        SYN_PERM_TRIM_THRESHOLD("synPermTrimThreshold", 0.5),
        MIN_PCT_OVERLAP_DUTY_CYCLE("minPctOverlapDutyCycles", 0.001),
        MIN_PCT_ACTIVE_DUTY_CYCLE("minPctActiveDutyCycles", 0.001),
        DUTY_CYCLE_PERIOD("dutyCyclePeriod", 1000),
        MAX_BOOST("maxBoost", 10),
        SP_VERBOSITY("spVerbosity", 0),
        
        //////////// Encoder Parameters ///////////
        W("w", 0),
        MINVAL("minval", 0),
        MAXVAL("maxval", 0),
        PERIODIC("periodic", false),
        N("n", 0),
        RADIUS("radius", 0),
        RESOLUTION("resolution", 0),
        NAME("name", "None"),
        CLIP_INPUT("clipInput", false),
        FORCED("forced", false),
        ENC_VERBOSITY("encVerbosity", 0);
        
        private String fieldName;
        private Object fieldValue;
        
        /**
         * Constructs a new KEY
         * @param fieldName
         * @param fieldValue
         */
        private KEY(String fieldName, Object fieldValue) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
        }
    };
    
    /** Total number of columns */
    private int[] columnDimensions = new int[] { 2048 };
    /** Total number of cells per column */
    private int cellsPerColumn = 32;
    /** 
     * If the number of active connected synapses on a segment 
     * is at least this threshold, the segment is said to be active.
     */
    private int activationThreshold = 13;
    /**
     * Radius around cell from which it can
     * sample to form distal {@link Dendrite} connections.
     */
    private int learningRadius = 2048;
    /**
     * If the number of synapses active on a segment is at least this
     * threshold, it is selected as the best matching
     * cell in a bursting column.
     */
    private int minThreshold = 10;
    /** The maximum number of synapses added to a segment during learning. */
    private int maxNewSynapseCount = 20;
    /** Seed for random number generator */
    private int seed = 42;
    /** Initial permanence of a new synapse */
    private double initialPermanence = 0.21;
    /**
     * If the permanence value for a synapse
     * is greater than this value, it is said
     * to be connected.
     */
    private double connectedPermanence = 0.50;
    /** 
     * Amount by which permanences of synapses
     * are incremented during learning.
     */
    private double permanenceIncrement = 0.10;
    /** 
     * Amount by which permanences of synapses
     * are decremented during learning.
     */
    private double permanenceDecrement = 0.10;
    
    /////////////////// Spatial Pooler Vars ///////////////////
    private int[] inputDimensions = new int[] { 32, 32 };
    private int potentialRadius = 16;
    private int inhibitionRadius = 0;
    private double potentialPct = 0.5;
    private boolean globalInhibition = false;
    private double localAreaDensity = -1.0;
    private double numActiveColumnsPerInhArea;
    private double stimulusThreshold = 0;
    private double synPermInactiveDec = 0.01;
    private double synPermActiveInc = 0.10;
    private double synPermTrimThreshold = synPermActiveInc / 2.0;
    private double synPermConnected = 0.10;
    private double synPermBelowStimulusInc = synPermConnected / 10.0;
    private double minPctOverlapDutyCycles = 0.001;
    private double minPctActiveDutyCycles = 0.001;
    private int dutyCyclePeriod = 1000;
    private double maxBoost = 10.0;
    private int spVerbosity = 0;
    
    /////////////////// Encoder Vars /////////////////////
    /** The number of bits that are set to encode a single value - the
     * "width" of the output signal 
     */
    private int w = 0;
    /** number of bits in the representation (must be > w) */
    private int n = 0;
    /** 
     * inputs separated by more than, or equal to this distance will have non-overlapping 
     * representations 
     */
    private double radius = 0;
    /** inputs separated by more than, or equal to this distance will have different representations */
    private double resolution = 0;
    /**
     * If true, then the input value "wraps around" such that minval = maxval
     * For a periodic value, the input must be strictly less than maxval,
     * otherwise maxval is a true upper bound.
     */
    private boolean periodic = true;
    /** The minimum value of the input signal.  */
    private double minval = 0;
    /** The maximum value of the input signal. */
    private double maxval = 0;
    /** if true, non-periodic inputs smaller than minval or greater
            than maxval will be clipped to minval/maxval */
    private boolean clipInput;
    /** if true, skip some safety checks (for compatibility reasons), default false */
    private boolean forced;
    /** Encoder name - an optional string which will become part of the description */
    private String name;
    /** Encoder verbosity setting */
    private int encVerbosity = 0;
    
    //////////////// General ////////////////
    /** Random Number Generator */
    private Random random;
    
    /** Map of parameters to their values */
    private EnumMap<Parameters.KEY, Object> paramMap;
    
    
    
    
    /**
     * Constructs a new {@code Parameters} object.
     */
    public Parameters() {}
    
    /**
     * Sets up default parameters for both the {@link SpatialPooler} and the 
     * {@link TemporalMemory}
     * 
     * @param inputDimensions
     * @param columnDimensions
     * @param cellsPerColumn
     * @param potentialRadius
     * @param potentialPct
     * @param globalInhibition
     * @param localAreaDensity
     * @param numActiveColumnsPerInhArea
     * @param stimulusThreshold
     * @param synPermInactiveDec
     * @param synPermActiveInc
     * @param synPermConnected
     * @param synPermBelowStimulusInc
     * @param minPctOverlapDutyCycles
     * @param minPctActiveDutyCycles
     * @param dutyCyclePeriod
     * @param maxBoost
     * @param activationThreshold
     * @param learningRadius
     * @param minThreshold
     * @param maxNewSynapseCount
     * @param seed
     * @param initialPermanence
     * @param connectedPermanence
     * @param permanenceIncrement
     * @param permanenceDecrement
     * @param random
     */
    public Parameters(int[] inputDimensions, int[] columnDimensions, int cellsPerColumn,
    		int potentialRadius/*SP*/, double potentialPct/*SP*/, boolean globalInhibition/*SP*/,
    		double localAreaDensity/*SP*/, double numActiveColumnsPerInhArea/*SP*/, double stimulusThreshold/*SP*/,
    		double synPermInactiveDec/*SP*/, double synPermActiveInc/*SP*/, double synPermConnected/*SP*/,
    		double synPermBelowStimulusInc/*SP*/, double minPctOverlapDutyCycles/*SP*/, double minPctActiveDutyCycles/*SP*/,
            int dutyCyclePeriod/*SP*/, double maxBoost/*SP*/, int activationThreshold/*TM*/, int learningRadius/*TM*/, int minThreshold/*TM*/,
            int maxNewSynapseCount/*TM*/, int seed, double initialPermanence/*TM*/,
            double connectedPermanence/*TM*/, double permanenceIncrement/*TM*/,
            double permanenceDecrement/*TM*/, int w/*ENC*/, int n/*ENC*/, int radius/*ENC*/,
            int resolution/*ENC*/, boolean periodic/*ENC*/, boolean clipInput/*ENC*/, boolean forced/*ENC*/,
            int minval/*ENC*/, int maxval/*ENC*/, String name/*ENC*/, Random random) {
    	
        super();
        
        //SpatialPooler 
        setInputDimensions(inputDimensions);
        setColumnDimensions(columnDimensions);
        setCellsPerColumn(cellsPerColumn);
        setPotentialRadius(potentialRadius);
        setPotentialPct(potentialPct);
        setGlobalInhibition(globalInhibition);
        setLocalAreaDensity(localAreaDensity);
        setNumActiveColumnsPerInhArea(numActiveColumnsPerInhArea);
        setStimulusThreshold(stimulusThreshold);
        setSynPermInactiveDec(synPermInactiveDec);
        setSynPermActiveInc(synPermActiveInc);
        setSynPermConnected(synPermConnected);
        setSynPermBelowStimulusInc(synPermBelowStimulusInc);
        setMinPctOverlapDutyCycle(minPctOverlapDutyCycles);
        setMinPctActiveDutyCycle(minPctActiveDutyCycles);
        setDutyCyclePeriod(dutyCyclePeriod);
        setMaxBoost(maxBoost);
        
        //TemporalMemory
        setActivationThreshold(activationThreshold);
        setLearningRadius(learningRadius);
        setMinThreshold(minThreshold);
        setMaxNewSynapseCount(maxNewSynapseCount);
        setSeed(seed);
        setInitialPermanence(initialPermanence);
        setConnectedPermanence(connectedPermanence);
        setPermanenceIncrement(permanenceIncrement);
        setPermanenceDecrement(permanenceDecrement);
        setRandom(random);
    }
    
    /**
     * Copy constructor for convenience
     * @param other
     */
    public Parameters(Parameters other) {
    	setInputDimensions(other.inputDimensions);
        setColumnDimensions(other.columnDimensions);
        setCellsPerColumn(other.cellsPerColumn);
        setPotentialRadius(other.potentialRadius);
        setPotentialPct(other.potentialPct);
        setGlobalInhibition(other.globalInhibition);
        setLocalAreaDensity(other.localAreaDensity);
        setNumActiveColumnsPerInhArea(other.numActiveColumnsPerInhArea);
        setStimulusThreshold(other.stimulusThreshold);
        setSynPermInactiveDec(other.synPermInactiveDec);
        setSynPermActiveInc(other.synPermActiveInc);
        setSynPermConnected(other.synPermConnected);
        setSynPermBelowStimulusInc(other.synPermBelowStimulusInc);
        setMinPctOverlapDutyCycle(other.minPctOverlapDutyCycles);
        setMinPctActiveDutyCycle(other.minPctActiveDutyCycles);
        setDutyCyclePeriod(other.dutyCyclePeriod);
        setMaxBoost(other.maxBoost);
        
        //TemporalMemory
        setActivationThreshold(other.activationThreshold);
        setLearningRadius(other.learningRadius);
        setMinThreshold(other.minThreshold);
        setMaxNewSynapseCount(other.maxNewSynapseCount);
        setSeed(other.seed);
        setInitialPermanence(other.initialPermanence);
        setConnectedPermanence(other.connectedPermanence);
        setPermanenceIncrement(other.permanenceIncrement);
        setPermanenceDecrement(other.permanenceDecrement);
        setRandom(other.random);
        
        //Encoder values
        setW(other.w);
        setN(other.n);
        setRadius(other.radius);
        setResolution(other.resolution);
        setPeriodic(other.periodic);
        setMinVal(other.minval);
        setMaxVal(other.maxval);
        setClipInput(other.clipInput);
        setForced(other.forced);
        setName(other.name);
    }
    
    /**
     * Creates and returns an {@link EnumMap} containing the specified keys; whose
     * values are to be loaded later. The map returned is only created if the internal
     * reference is null (never been created), therefore the map is a singleton which
     * cannot be recreated once created.
     * 
     * @return
     */
    public EnumMap<Parameters.KEY, Object> getMap() {
        if(paramMap == null) {
            paramMap = new EnumMap<Parameters.KEY, Object>(Parameters.KEY.class);
            
            
        }
        
        return paramMap;
    }
    
    /**
     * Sets the fields specified by the {@code Parameters} on the specified
     * {@link Connections} object. 
     * 
     * @param cn
     * @param p
     */
    public static void apply(Connections cn, Parameters p) {
        try {
            for(Parameters.KEY key : p.paramMap.keySet()) {
                switch(key){
                    case RANDOM: {
                        Field f = cn.getClass().getDeclaredField(key.fieldName);
                        f.setAccessible(true);
                        f.set(cn, p.random);
                        
                        f = p.getClass().getDeclaredField(key.fieldName);
                        f.setAccessible(true);
                        f.set(p, p.random);
                        
                        break;
                    }
                    default: {
                    	Field f = cn.getClass().getDeclaredField(key.fieldName);
                    	f.setAccessible(true);
                        f.set(cn, p.paramMap.get(key));
                        
                        f = p.getClass().getDeclaredField(key.fieldName);
                        f.setAccessible(true);
                        f.set(p, p.paramMap.get(key));
                        
                        break;
                    }
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Returns the seeded random number generator.
     * @param   r   the generator to use.
     */
    public void setRandom(Random r) {
        this.random = r;
    }

    /**
     * Sets the number of {@link Column}.
     * 
     * @param columnDimensions
     */
    public void setColumnDimensions(int[] columnDimensions) {
        this.columnDimensions = columnDimensions;
        getMap().put(KEY.COLUMN_DIMENSIONS, columnDimensions);
    }

    /**
     * Sets the number of {@link Cell}s per {@link Column}
     * @param cellsPerColumn
     */
    public void setCellsPerColumn(int cellsPerColumn) {
        this.cellsPerColumn = cellsPerColumn;
        getMap().put(KEY.CELLS_PER_COLUMN, cellsPerColumn);
    }

    /**
     * Sets the activation threshold.
     * 
     * If the number of active connected synapses on a segment 
     * is at least this threshold, the segment is said to be active.
     * 
     * @param activationThreshold
     */
    public void setActivationThreshold(int activationThreshold) {
        this.activationThreshold = activationThreshold;
        getMap().put(KEY.ACTIVATION_THRESHOLD, activationThreshold);
    }

    /**
     * Radius around cell from which it can
     * sample to form distal dendrite connections.
     * 
     * @param   learningRadius
     */
    public void setLearningRadius(int learningRadius) {
        this.learningRadius = learningRadius;
        getMap().put(KEY.LEARNING_RADIUS, learningRadius);
    }

    /**
     * If the number of synapses active on a segment is at least this
     * threshold, it is selected as the best matching
     * cell in a bursting column.
     * 
     * @param   minThreshold
     */
    public void setMinThreshold(int minThreshold) {
        this.minThreshold = minThreshold;
        getMap().put(KEY.MIN_THRESHOLD, minThreshold);
    }

    /** 
     * The maximum number of synapses added to a segment during learning. 
     * 
     * @param   maxNewSynapseCount
     */
    public void setMaxNewSynapseCount(int maxNewSynapseCount) {
        this.maxNewSynapseCount = maxNewSynapseCount;
        getMap().put(KEY.MAX_NEW_SYNAPSE_COUNT, maxNewSynapseCount);
    }

    /** 
     * Seed for random number generator 
     * 
     * @param   seed
     */
    public void setSeed(int seed) {
        this.seed = seed;
        getMap().put(KEY.SEED, seed);
    }
    
    /** 
     * Initial permanence of a new synapse 
     * 
     * @param   
     */
    public void setInitialPermanence(double initialPermanence) {
        this.initialPermanence = initialPermanence;
        getMap().put(KEY.INITIAL_PERMANENCE, initialPermanence);
    }
    
    /**
     * If the permanence value for a synapse
     * is greater than this value, it is said
     * to be connected.
     * 
     * @param connectedPermanence
     */
    public void setConnectedPermanence(double connectedPermanence) {
        this.connectedPermanence = connectedPermanence;
        getMap().put(KEY.CONNECTED_PERMANENCE, connectedPermanence);
    }

    /** 
     * Amount by which permanences of synapses
     * are incremented during learning.
     * 
     * @param   permanenceIncrement
     */
    public void setPermanenceIncrement(double permanenceIncrement) {
        this.permanenceIncrement = permanenceIncrement;
        getMap().put(KEY.PERMANENCE_INCREMENT, permanenceIncrement);
    }
    
    /** 
     * Amount by which permanences of synapses
     * are decremented during learning.
     * 
     * @param   permanenceDecrement
     */
    public void setPermanenceDecrement(double permanenceDecrement) {
        this.permanenceDecrement = permanenceDecrement;
        getMap().put(KEY.PERMANENCE_DECREMENT, permanenceDecrement);
    }
    
    ////////////////////////////// SPACIAL POOLER PARAMS //////////////////////////////////
    /**
     * A list representing the dimensions of the input
     * vector. Format is [height, width, depth, ...], where
     * each value represents the size of the dimension. For a
     * topology of one dimension with 100 inputs use 100, or
     * [100]. For a two dimensional topology of 10x5 use
     * [10,5].
     * 
     * @param inputDimensions
     */
    public void setInputDimensions(int[] inputDimensions) {
        this.inputDimensions = inputDimensions;
        getMap().put(KEY.INPUT_DIMENSIONS, inputDimensions);
    }
    
    /**
     * This parameter determines the extent of the input
     * that each column can potentially be connected to.
     * This can be thought of as the input bits that
     * are visible to each column, or a 'receptiveField' of
     * the field of vision. A large enough value will result
     * in 'global coverage', meaning that each column
     * can potentially be connected to every input bit. This
     * parameter defines a square (or hyper square) area: a
     * column will have a max square potential pool with
     * sides of length 2 * potentialRadius + 1.
     * 
     * @param potentialRadius
     */
    public void setPotentialRadius(int potentialRadius) {
        this.potentialRadius = potentialRadius;
        getMap().put(KEY.POTENTIAL_RADIUS, potentialRadius);
    }
    
    /** 
     * The inhibition radius determines the size of a column's local
     * neighborhood. of a column. A cortical column must overcome the overlap
     * score of columns in his neighborhood in order to become actives. This
     * radius is updated every learning round. It grows and shrinks with the
     * average number of connected synapses per column.
     * 
     * @param	inhibitionRadius	the local group size
     */
    public void setInhibitionRadius(int inhibitionRadius) {
    	this.inhibitionRadius = inhibitionRadius;
    	getMap().put(KEY.INHIBITION_RADIUS, inhibitionRadius);
    }

    /**
     * The percent of the inputs, within a column's
     * potential radius, that a column can be connected to.
     * If set to 1, the column will be connected to every
     * input within its potential radius. This parameter is
     * used to give each column a unique potential pool when
     * a large potentialRadius causes overlap between the
     * columns. At initialization time we choose
     * ((2*potentialRadius + 1)^(# inputDimensions) *
     * potentialPct) input bits to comprise the column's
     * potential pool.
     * 
     * @param potentialPct
     */
    public void setPotentialPct(double potentialPct) {
        this.potentialPct = potentialPct;
        getMap().put(KEY.POTENTIAL_PCT, potentialPct);
    }

    /**
     * If true, then during inhibition phase the winning
     * columns are selected as the most active columns from
     * the region as a whole. Otherwise, the winning columns
     * are selected with respect to their local
     * neighborhoods. Using global inhibition boosts
     * performance x60.
     * 
     * @param globalInhibition
     */
    public void setGlobalInhibition(boolean globalInhibition) {
        this.globalInhibition = globalInhibition;
        getMap().put(KEY.GLOBAL_INHIBITIONS, globalInhibition);
    }

    /**
     * The desired density of active columns within a local
     * inhibition area (the size of which is set by the
     * internally calculated inhibitionRadius, which is in
     * turn determined from the average size of the
     * connected potential pools of all columns). The
     * inhibition logic will insure that at most N columns
     * remain ON within a local inhibition area, where N =
     * localAreaDensity * (total number of columns in
     * inhibition area).
     * 
     * @param localAreaDensity
     */
    public void setLocalAreaDensity(double localAreaDensity) {
        this.localAreaDensity = localAreaDensity;
        getMap().put(KEY.LOCAL_AREA_DENSITY, localAreaDensity);
    }

    /**
     * An alternate way to control the density of the active
     * columns. If numActivePerInhArea is specified then
     * localAreaDensity must be less than 0, and vice versa.
     * When using numActivePerInhArea, the inhibition logic
     * will insure that at most 'numActivePerInhArea'
     * columns remain ON within a local inhibition area (the
     * size of which is set by the internally calculated
     * inhibitionRadius, which is in turn determined from
     * the average size of the connected receptive fields of
     * all columns). When using this method, as columns
     * learn and grow their effective receptive fields, the
     * inhibitionRadius will grow, and hence the net density
     * of the active columns will *decrease*. This is in
     * contrast to the localAreaDensity method, which keeps
     * the density of active columns the same regardless of
     * the size of their receptive fields.
     * 
     * @param numActiveColumnsPerInhArea
     */
    public void setNumActiveColumnsPerInhArea(double numActiveColumnsPerInhArea) {
        this.numActiveColumnsPerInhArea = numActiveColumnsPerInhArea;
        getMap().put(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, numActiveColumnsPerInhArea);
    }

    /**
     * This is a number specifying the minimum number of
     * synapses that must be on in order for a columns to
     * turn ON. The purpose of this is to prevent noise
     * input from activating columns. Specified as a percent
     * of a fully grown synapse.
     * 
     * @param stimulusThreshold
     */
    public void setStimulusThreshold(double stimulusThreshold) {
        this.stimulusThreshold = stimulusThreshold;
        getMap().put(KEY.STIMULUS_THRESHOLD, stimulusThreshold);
    }

    /**
     * The amount by which an inactive synapse is
     * decremented in each round. Specified as a percent of
     * a fully grown synapse.
     * 
     * @param synPermInactiveDec
     */
    public void setSynPermInactiveDec(double synPermInactiveDec) {
        this.synPermInactiveDec = synPermInactiveDec;
        getMap().put(KEY.SYN_PERM_INACTIVE_DEC, synPermInactiveDec);
    }

    /**
     * The amount by which an active synapse is incremented
     * in each round. Specified as a percent of a
     * fully grown synapse.
     * 
     * @param synPermActiveInc
     */
    public void setSynPermActiveInc(double synPermActiveInc) {
        this.synPermActiveInc = synPermActiveInc;
        getMap().put(KEY.SYN_PERM_ACTIVE_INC, synPermActiveInc);
    }

    /**
     * The default connected threshold. Any synapse whose
     * permanence value is above the connected threshold is
     * a "connected synapse", meaning it can contribute to
     * the cell's firing.
     * 
     * @param minPctOverlapDutyCycle
     */
    public void setSynPermConnected(double synPermConnected) {
        this.synPermConnected = synPermConnected;
        getMap().put(KEY.SYN_PERM_CONNECTED, synPermConnected);
    }
    
    /**
     * Sets the increment of synapse permanences below the stimulus
     * threshold
     * @param inc
     */
    public void setSynPermBelowStimulusInc(double inc) {
    	this.synPermBelowStimulusInc = inc;
    	getMap().put(KEY.SYN_PERM_BELOW_STIMULUS, synPermBelowStimulusInc);
    }
    
    /**
     * 
     * @param threshold
     */
    public void setSynPermTrimThreshold(double threshold) {
    	this.synPermTrimThreshold = threshold;
    	getMap().put(KEY.SYN_PERM_TRIM_THRESHOLD, synPermTrimThreshold);
    }

    /**
     * A number between 0 and 1.0, used to set a floor on
     * how often a column should have at least
     * stimulusThreshold active inputs. Periodically, each
     * column looks at the overlap duty cycle of
     * all other columns within its inhibition radius and
     * sets its own internal minimal acceptable duty cycle
     * to: minPctDutyCycleBeforeInh * max(other columns'
     * duty cycles).
     * On each iteration, any column whose overlap duty
     * cycle falls below this computed value will  get
     * all of its permanence values boosted up by
     * synPermActiveInc. Raising all permanences in response
     * to a sub-par duty cycle before  inhibition allows a
     * cell to search for new inputs when either its
     * previously learned inputs are no longer ever active,
     * or when the vast majority of them have been
     * "hijacked" by other columns.
     * 
     * @param minPctOverlapDutyCycle
     */
    public void setMinPctOverlapDutyCycle(double minPctOverlapDutyCycles) {
        this.minPctOverlapDutyCycles = minPctOverlapDutyCycles;
        getMap().put(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, minPctOverlapDutyCycles);
    }

    /**
     * A number between 0 and 1.0, used to set a floor on
     * how often a column should be activate.
     * Periodically, each column looks at the activity duty
     * cycle of all other columns within its inhibition
     * radius and sets its own internal minimal acceptable
     * duty cycle to:
     *   minPctDutyCycleAfterInh *
     *   max(other columns' duty cycles).
     * On each iteration, any column whose duty cycle after
     * inhibition falls below this computed value will get
     * its internal boost factor increased.
     * 
     * @param minPctActiveDutyCycle
     */
    public void setMinPctActiveDutyCycle(double minPctActiveDutyCycles) {
        this.minPctActiveDutyCycles = minPctActiveDutyCycles;
        getMap().put(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, minPctActiveDutyCycles);
    }

    /**
     * The period used to calculate duty cycles. Higher
     * values make it take longer to respond to changes in
     * boost or synPerConnectedCell. Shorter values make it
     * more unstable and likely to oscillate.
     * 
     * @param dutyCyclePeriod
     */
    public void setDutyCyclePeriod(int dutyCyclePeriod) {
        this.dutyCyclePeriod = dutyCyclePeriod;
        getMap().put(KEY.DUTY_CYCLE_PERIOD, dutyCyclePeriod);
    }
    
    /**
     * The maximum overlap boost factor. Each column's
     * overlap gets multiplied by a boost factor
     * before it gets considered for inhibition.
     * The actual boost factor for a column is number
     * between 1.0 and maxBoost. A boost factor of 1.0 is
     * used if the duty cycle is >= minOverlapDutyCycle,
     * maxBoost is used if the duty cycle is 0, and any duty
     * cycle in between is linearly extrapolated from these
     * 2 endpoints.
     * 
     * @param maxBoost
     */
    public void setMaxBoost(double maxBoost) {
        this.maxBoost = maxBoost;
        getMap().put(KEY.MAX_BOOST, maxBoost);
    }

    /**
     * spVerbosity level: 0, 1, 2, or 3
     * 
     * @param spVerbosity
     */
    public void setSpVerbosity(int spVerbosity) {
        this.spVerbosity = spVerbosity;
        getMap().put(KEY.SP_VERBOSITY, spVerbosity);
    }
    
    /////////////////// Encoder Params ///////////////////
    /** 
     * Sets the "w" or width of the output signal
     * <em>Restriction:</em> w must be odd to avoid centering problems.
     * @param w
     */
    public void setW(int w) {
    	this.w = w;
    	getMap().put(KEY.W, w);
    }
    
    /**
     * The number of bits in the output. Must be greater than or equal to w
     * @param n
     */
    public void setN(int n) {
    	this.n = n;
    	getMap().put(KEY.N, n);
    }
    
    /**
     * The minimum value of the input signal.
     * @param minVal
     */
    public void setMinVal(double minVal) {
    	this.minval = minVal;
    	getMap().put(KEY.MINVAL, minVal);
    }
    
    /**
     * The maximum value of the input signal.
     * @param maxVal
     */
    public void setMaxVal(double maxVal) {
    	this.maxval = maxVal;
    	getMap().put(KEY.MAXVAL, maxVal);
    }
    
    /**
     * inputs separated by more than, or equal to this distance will have non-overlapping
     * representations
     * 
     * @param radius
     */
    public void setRadius(double radius) {
    	this.radius = radius;
    	getMap().put(KEY.RADIUS, radius);
    }
    
    /**
     * inputs separated by more than, or equal to this distance will have different
     * representations
     * 
     * @param resolution
     */
    public void setResolution(double resolution) {
    	this.resolution = resolution;
    	getMap().put(KEY.RESOLUTION, resolution);
    }
    
    /**
     * If true, non-periodic inputs smaller than minval or greater
     * than maxval will be clipped to minval/maxval
     * @param b
     */
    public void setClipInput(boolean b) {
    	this.clipInput = b;
    	getMap().put(KEY.CLIP_INPUT, b);
    }
    
    /**
     * If true, then the input value "wraps around" such that minval = maxval
     * For a periodic value, the input must be strictly less than maxval,
     * otherwise maxval is a true upper bound.
     * 
     * @param b
     */
    public void setPeriodic(boolean b) {
    	this.periodic = b;
    	getMap().put(KEY.PERIODIC, b);
    }
    
    /**
     * If true, skip some safety checks (for compatibility reasons), default false 
     * @param b
     */
    public void setForced(boolean b) {
    	this.forced = b;
    	getMap().put(KEY.FORCED, b);
    }
    
    /**
     * An optional string which will become part of the description
     * @param name
     */
    public void setName(String name) {
    	this.name = name;
    	getMap().put(KEY.NAME, name);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ Spatial\n")
        .append("\t").append("inputDimensions :  ").append(inputDimensions).append("\n")
        .append("\t").append("potentialRadius :  ").append(potentialRadius).append("\n")
        .append("\t").append("potentialPct :  ").append(potentialPct).append("\n")
        .append("\t").append("globalInhibition :  ").append(globalInhibition).append("\n")
        .append("\t").append("inhibitionRadius :  ").append(inhibitionRadius).append("\n")
        .append("\t").append("localAreaDensity :  ").append(localAreaDensity).append("\n")
        .append("\t").append("numActiveColumnsPerInhArea :  ").append(numActiveColumnsPerInhArea).append("\n")
        .append("\t").append("stimulusThreshold :  ").append(stimulusThreshold).append("\n")
        .append("\t").append("synPermInactiveDec :  ").append(synPermInactiveDec).append("\n")
        .append("\t").append("synPermActiveInc :  ").append(synPermActiveInc).append("\n")
        .append("\t").append("synPermConnected :  ").append(synPermConnected).append("\n")
        .append("\t").append("synPermBelowStimulusInc :  ").append(synPermBelowStimulusInc).append("\n")
        .append("\t").append("minPctOverlapDutyCycles :  ").append(minPctOverlapDutyCycles).append("\n")
        .append("\t").append("minPctActiveDutyCycles :  ").append(minPctActiveDutyCycles).append("\n")
        .append("\t").append("dutyCyclePeriod :  ").append(dutyCyclePeriod).append("\n")
        .append("\t").append("maxBoost :  ").append(maxBoost).append("\n")
        .append("\t").append("spVerbosity :  ").append(spVerbosity).append("\n")
        .append("}\n\n")
        
        .append("{ Temporal\n")
        .append("\t").append("activationThreshold :  ").append(activationThreshold).append("\n")
        .append("\t").append("cellsPerColumn :  ").append(cellsPerColumn).append("\n")
        .append("\t").append("columnDimensions :  ").append(columnDimensions).append("\n")
        .append("\t").append("connectedPermanence :  ").append(connectedPermanence).append("\n")
        .append("\t").append("initialPermanence :  ").append(initialPermanence).append("\n")
        .append("\t").append("maxNewSynapseCount :  ").append(maxNewSynapseCount).append("\n")
        .append("\t").append("minThreshold :  ").append(minThreshold).append("\n")
        .append("\t").append("permanenceIncrement :  ").append(permanenceIncrement).append("\n")
        .append("\t").append("permanenceDecrement :  ").append(permanenceDecrement).append("\n")
        .append("}\n\n")
        
        .append("{ Encoder\n")
        .append("\t").append("w :  ").append(w).append("\n")
        .append("\t").append("n :  ").append(n).append("\n")
        .append("\t").append("radius :  ").append(radius).append("\n")
        .append("\t").append("resolution :  ").append(resolution).append("\n")
        .append("\t").append("minval :  ").append(minval).append("\n")
        .append("\t").append("maxval :  ").append(maxval).append("\n")
        .append("\t").append("periodic :  ").append(periodic).append("\n")
        .append("\t").append("clipInput :  ").append(clipInput).append("\n")
        .append("\t").append("forced :  ").append(forced).append("\n")
        .append("\t").append("name :  ").append(name).append("\n")
        .append("}\n\n");
        
        return sb.toString();
    }
    
}
