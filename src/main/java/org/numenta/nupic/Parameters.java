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

package org.numenta.nupic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.joda.time.format.DateTimeFormatter;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.research.ComputeCycle;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.BeanUtil;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.Tuple;

/**
 * Specifies parameters to be used as a configuration for a given {@link TemporalMemory}
 * or {@link SpatialPooler}
 *
 * @author David Ray
 * @author Kirill Solovyev
 * @see SpatialPooler
 * @see TemporalMemory
 * @see Connections
 * @see ComputeCycle
 */
public class Parameters {
    private static final Map<KEY, Object> DEFAULTS_ALL;
    private static final Map<KEY, Object> DEFAULTS_TEMPORAL;
    private static final Map<KEY, Object> DEFAULTS_SPATIAL;
    private static final Map<KEY, Object> DEFAULTS_ENCODER;


    static {
        Map<KEY, Object> defaultParams = new ParametersMap();

        /////////// Universal Parameters ///////////

        defaultParams.put(KEY.SEED, 42);
        defaultParams.put(KEY.RANDOM, new MersenneTwister((int)defaultParams.get(KEY.SEED)));

        /////////// Temporal Memory Parameters ///////////
        Map<KEY, Object> defaultTemporalParams = new ParametersMap();
        defaultTemporalParams.put(KEY.COLUMN_DIMENSIONS, new int[]{2048});
        defaultTemporalParams.put(KEY.CELLS_PER_COLUMN, 32);
        defaultTemporalParams.put(KEY.ACTIVATION_THRESHOLD, 13);
        defaultTemporalParams.put(KEY.LEARNING_RADIUS, 2048);
        defaultTemporalParams.put(KEY.MIN_THRESHOLD, 10);
        defaultTemporalParams.put(KEY.MAX_NEW_SYNAPSE_COUNT, 20);
        defaultTemporalParams.put(KEY.INITIAL_PERMANENCE, 0.21);
        defaultTemporalParams.put(KEY.CONNECTED_PERMANENCE, 0.5);
        defaultTemporalParams.put(KEY.PERMANENCE_INCREMENT, 0.10);
        defaultTemporalParams.put(KEY.PERMANENCE_DECREMENT, 0.10);
        defaultTemporalParams.put(KEY.TM_VERBOSITY, 0);
        defaultTemporalParams.put(KEY.LEARN, true);
        DEFAULTS_TEMPORAL = Collections.unmodifiableMap(defaultTemporalParams);
        defaultParams.putAll(DEFAULTS_TEMPORAL);

        //////////// Spatial Pooler Parameters ///////////
        Map<KEY, Object> defaultSpatialParams = new ParametersMap();
        defaultSpatialParams.put(KEY.INPUT_DIMENSIONS, new int[]{64});
        defaultSpatialParams.put(KEY.POTENTIAL_RADIUS, 16);
        defaultSpatialParams.put(KEY.POTENTIAL_PCT, 0.5);
        defaultSpatialParams.put(KEY.GLOBAL_INHIBITIONS, false);
        defaultSpatialParams.put(KEY.INHIBITION_RADIUS, 0);
        defaultSpatialParams.put(KEY.LOCAL_AREA_DENSITY, -1.0);
        defaultSpatialParams.put(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 10.0);
        defaultSpatialParams.put(KEY.STIMULUS_THRESHOLD, 0.0);
        defaultSpatialParams.put(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        defaultSpatialParams.put(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        defaultSpatialParams.put(KEY.SYN_PERM_CONNECTED, 0.10);
        defaultSpatialParams.put(KEY.SYN_PERM_BELOW_STIMULUS_INC, 0.01);
        defaultSpatialParams.put(KEY.SYN_PERM_TRIM_THRESHOLD, 0.5);
        defaultSpatialParams.put(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.001);
        defaultSpatialParams.put(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.001);
        defaultSpatialParams.put(KEY.DUTY_CYCLE_PERIOD, 1000);
        defaultSpatialParams.put(KEY.MAX_BOOST, 10.0);
        defaultSpatialParams.put(KEY.SP_VERBOSITY, 0);
        defaultSpatialParams.put(KEY.LEARN, true);
        DEFAULTS_SPATIAL = Collections.unmodifiableMap(defaultSpatialParams);
        defaultParams.putAll(DEFAULTS_SPATIAL);
        
        ///////////  Encoder Parameters ///////////
        Map<KEY, Object> defaultEncoderParams = new ParametersMap();
        defaultEncoderParams.put(KEY.N, 500);
        defaultEncoderParams.put(KEY.W, 21);
        defaultEncoderParams.put(KEY.MIN_VAL, 0.);
        defaultEncoderParams.put(KEY.MAX_VAL, 1000.);
        defaultEncoderParams.put(KEY.RADIUS, 21.);
        defaultEncoderParams.put(KEY.RESOLUTION, 1.);
        defaultEncoderParams.put(KEY.PERIODIC, false);
        defaultEncoderParams.put(KEY.CLIP_INPUT, false);
        defaultEncoderParams.put(KEY.FORCED, false);
        defaultEncoderParams.put(KEY.FIELD_NAME, "UNSET");
        defaultEncoderParams.put(KEY.FIELD_TYPE, "int");
        defaultEncoderParams.put(KEY.ENCODER, "ScalarEncoder");
        defaultEncoderParams.put(KEY.FIELD_ENCODING_MAP, Collections.emptyMap());
        defaultEncoderParams.put(KEY.AUTO_CLASSIFY, Boolean.FALSE);
        DEFAULTS_ENCODER = Collections.unmodifiableMap(defaultEncoderParams);
        defaultParams.putAll(DEFAULTS_ENCODER);

        DEFAULTS_ALL = Collections.unmodifiableMap(defaultParams);
    }

    /**
     * Constant values representing configuration parameters for the {@link TemporalMemory}
     */
    public static enum KEY {
        /////////// Universal Parameters ///////////
        /**
         * Total number of columns
         */
        COLUMN_DIMENSIONS("columnDimensions", int[].class),
        /**
         * Total number of cells per column
         */
        CELLS_PER_COLUMN("cellsPerColumn", Integer.class, 1, null),
        /**
         * Learning variable
         */
        LEARN("learn", Boolean.class),
        /**
         * Random Number Generator
         */
        RANDOM("random", Random.class),
        /**
         * Seed for random number generator
         */
        SEED("seed", Integer.class),

        /////////// Temporal Memory Parameters ///////////
        /**
         * If the number of active connected synapses on a segment
         * is at least this threshold, the segment is said to be active.
         */
        ACTIVATION_THRESHOLD("activationThreshold", Integer.class, 0, null),
        /**
         * Radius around cell from which it can
         * sample to form distal {@link DistalDendrite} connections.
         */
        LEARNING_RADIUS("learningRadius", Integer.class, 0, null),
        /**
         * If the number of synapses active on a segment is at least this
         * threshold, it is selected as the best matching
         * cell in a bursting column.
         */
        MIN_THRESHOLD("minThreshold", Integer.class, 0, null),
        /**
         * The maximum number of synapses added to a segment during learning.
         */
        MAX_NEW_SYNAPSE_COUNT("maxNewSynapseCount", Integer.class),
        /**
         * Initial permanence of a new synapse
         */
        INITIAL_PERMANENCE("initialPermanence", Double.class, 0.0, 1.0),
        /**
         * If the permanence value for a synapse
         * is greater than this value, it is said
         * to be connected.
         */
        CONNECTED_PERMANENCE("connectedPermanence", Double.class, 0.0, 1.0),
        /**
         * Amount by which permanence of synapses
         * are incremented during learning.
         */
        PERMANENCE_INCREMENT("permanenceIncrement", Double.class, 0.0, 1.0),
        /**
         * Amount by which permanences of synapses
         * are decremented during learning.
         */
        PERMANENCE_DECREMENT("permanenceDecrement", Double.class, 0.0, 1.0),
        TM_VERBOSITY("tmVerbosity", Integer.class, 0, 10),

        /////////// Spatial Pooler Parameters ///////////
        INPUT_DIMENSIONS("inputDimensions", int[].class),
        POTENTIAL_RADIUS("potentialRadius", Integer.class),
        POTENTIAL_PCT("potentialPct", Double.class), //TODO add range here?
        GLOBAL_INHIBITIONS("globalInhibition", Boolean.class),
        INHIBITION_RADIUS("inhibitionRadius", Integer.class, 0, null),
        LOCAL_AREA_DENSITY("localAreaDensity", Double.class), //TODO add range here?
        NUM_ACTIVE_COLUMNS_PER_INH_AREA("numActiveColumnsPerInhArea", Double.class),//TODO add range here?
        STIMULUS_THRESHOLD("stimulusThreshold", Double.class), //TODO add range here?
        SYN_PERM_INACTIVE_DEC("synPermInactiveDec", Double.class, 0.0, 1.0),
        SYN_PERM_ACTIVE_INC("synPermActiveInc", Double.class, 0.0, 1.0),
        SYN_PERM_CONNECTED("synPermConnected", Double.class, 0.0, 1.0),
        SYN_PERM_BELOW_STIMULUS_INC("synPermBelowStimulusInc", Double.class, 0.0, 1.0),
        SYN_PERM_TRIM_THRESHOLD("synPermTrimThreshold", Double.class, 0.0, 1.0),
        MIN_PCT_OVERLAP_DUTY_CYCLE("minPctOverlapDutyCycles", Double.class),//TODO add range here?
        MIN_PCT_ACTIVE_DUTY_CYCLE("minPctActiveDutyCycles", Double.class),//TODO add range here?
        DUTY_CYCLE_PERIOD("dutyCyclePeriod", Integer.class),//TODO add range here?
        MAX_BOOST("maxBoost", Double.class), //TODO add range here?
        SP_VERBOSITY("spVerbosity", Integer.class, 0, 10),
        
        ///////////// SpatialPooler / Network Parameter(s) /////////////
        /** Number of cycles to send through the SP before forwarding data to the rest of the network. */
        SP_PRIMER_DELAY("sp_primer_delay", Integer.class),
        
        ///////////// Encoder Parameters //////////////
        /** number of bits in the representation (must be >= w) */
        N("n", Integer.class),
        /** 
         * The number of bits that are set to encode a single value - the
         * "width" of the output signal
         */
        W("w", Integer.class),
        /** The minimum value of the input signal.  */
        MIN_VAL("minVal", Double.class),
        /** The maximum value of the input signal. */
        MAX_VAL("maxVal", Double.class),
        /**
         * inputs separated by more than, or equal to this distance will have non-overlapping
         * representations
         */
        RADIUS("radius", Double.class),
        /** inputs separated by more than, or equal to this distance will have different representations */
        RESOLUTION("resolution", Double.class),
        /**
         * If true, then the input value "wraps around" such that minval = maxval
         * For a periodic value, the input must be strictly less than maxval,
         * otherwise maxval is a true upper bound.
         */
        PERIODIC("periodic", Boolean.class),
        /** 
         * if true, non-periodic inputs smaller than minval or greater
         * than maxval will be clipped to minval/maxval 
         */
        CLIP_INPUT("clipInput", Boolean.class),
        /** 
         * If true, skip some safety checks (for compatibility reasons), default false 
         * Mostly having to do with being able to set the window size < 21 
         */
        FORCED("forced", Boolean.class),
        /** Name of the field being encoded */
        FIELD_NAME("fieldName", String.class),
        /** Primitive type of the field, used to auto-configure the type of encoder */
        FIELD_TYPE("fieldType", String.class),
        /** Encoder name */
        ENCODER("encoderType", String.class),
        /** Designates holder for the Multi Encoding Map */
        FIELD_ENCODING_MAP("fieldEncodings", Map.class),
        CATEGORY_LIST("categoryList", List.class),
        
        // Network Layer indicator for auto classifier generation
        AUTO_CLASSIFY("hasClassifiers", Boolean.class),
        
        
        // How many bits to use if encoding the respective date fields.
        // e.g. Tuple(bits to use:int, radius:double)
        DATEFIELD_SEASON("season", Tuple.class), 
        DATEFIELD_DOFW("dayOfWeek", Tuple.class),
        DATEFIELD_WKEND("weekend", Tuple.class),
        DATEFIELD_HOLIDAY("holiday", Tuple.class),
        DATEFIELD_TOFD("timeOfDay", Tuple.class),
        DATEFIELD_CUSTOM("customDays", Tuple.class), // e.g. Tuple(bits:int, List<String>:"mon,tue,fri")
        DATEFIELD_PATTERN("formatPattern", String.class),
        DATEFIELD_FORMATTER("dateFormatter", DateTimeFormatter.class);
        

        private static final Map<String, KEY> fieldMap = new HashMap<>();

        static {
            for (KEY key : KEY.values()) {
                fieldMap.put(key.getFieldName(), key);
            }
        }

        public static KEY getKeyByFieldName(String fieldName) {
            return fieldMap.get(fieldName);
        }

        final private String fieldName;
        final private Class<?> fieldType;
        final private Number min;
        final private Number max;

        /**
         * Constructs a new KEY
         *
         * @param fieldName
         * @param fieldType
         */
        private KEY(String fieldName, Class<?> fieldType) {
            this(fieldName, fieldType, null, null);
        }

        /**
         * Constructs a new KEY with range check
         *
         * @param fieldName
         * @param fieldType
         * @param min
         * @param max
         */
        private KEY(String fieldName, Class<?> fieldType, Number min, Number max) {
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.min = min;
            this.max = max;
        }

        public Class<?> getFieldType() {
            return fieldType;
        }

        public String getFieldName() {
            return fieldName;
        }

        public Number getMin() {
            return min;
        }

        public Number getMax() {
            return max;
        }

        public boolean checkRange(Number value) {
            if (value == null) {
                throw new IllegalArgumentException("checkRange argument can not be null");
            }
            return (min == null && max == null) ||
                   (min != null && max == null && min.doubleValue() <= value.doubleValue()) ||
                   (max != null && min == null && value.doubleValue() < value.doubleValue()) ||
                   (min != null && min.doubleValue() <= value.doubleValue() && max != null && value.doubleValue() < max.doubleValue());
        }

    }

    /**
     * Save guard decorator around params map
     */
    private static class ParametersMap extends EnumMap<KEY, Object> {
        /**
         * Default serialvers
         */
        private static final long serialVersionUID = 1L;

        ParametersMap() {
            super(Parameters.KEY.class);
        }

        @Override public Object put(KEY key, Object value) {
            if (value != null) {
                if (!key.getFieldType().isInstance(value)) {
                    throw new IllegalArgumentException(
                            "Can not set Parameters Property '" + key.getFieldName() + "' because of type mismatch. The required type is " + key.getFieldType());
                }
                if (value instanceof Number && !key.checkRange((Number)value)) {
                    throw new IllegalArgumentException(
                            "Can not set Parameters Property '" + key.getFieldName() + "' because of value '" + value + "' not in range. Range[" + key.getMin() + "-" + key.getMax() + "]");
                }
            }
            return super.put(key, value);
        }
    }

    /**
     * Map of parameters to their values
     */
    private final Map<Parameters.KEY, Object> paramMap = Collections.synchronizedMap(new ParametersMap());

    //TODO apply from container to parameters

    /**
     * Factory method. Return global {@link Parameters} object with default values
     *
     * @return {@link Parameters} object
     */
    public static Parameters getAllDefaultParameters() {
        return getParameters(DEFAULTS_ALL);
    }

    /**
     * Factory method. Return temporal {@link Parameters} object with default values
     *
     * @return {@link Parameters} object
     */
    public static Parameters getTemporalDefaultParameters() {
        return getParameters(DEFAULTS_TEMPORAL);
    }


    /**
     * Factory method. Return spatial {@link Parameters} object with default values
     *
     * @return {@link Parameters} object
     */
    public static Parameters getSpatialDefaultParameters() {
        return getParameters(DEFAULTS_SPATIAL);
    }

    /**
     * Factory method. Return Encoder {@link Parameters} object with default values
     * @return
     */
    public static Parameters getEncoderDefaultParameters() {
        return getParameters(DEFAULTS_ENCODER);
    }
    /**
     * Called internally to populate a {@link Parameters} object with the keys
     * and values specified in the passed in map.
     *
     * @return {@link Parameters} object
     */
    private static Parameters getParameters(Map<KEY, Object> map) {
        Parameters result = new Parameters();
        for (KEY key : map.keySet()) {
            result.setParameterByKey(key, map.get(key));
        }
        return result;
    }


    /**
     * Constructs a new {@code Parameters} object.
     * It is private. Only allow instantiation with Factory methods.
     * This way we will never have erroneous Parameters with missing attributes
     */
    private Parameters() {
    }

    /**
     * Sets the fields specified by this {@code Parameters} on the specified
     * {@link Connections} object.
     *
     * @param cn
     */
    public void apply(Object cn) {
        BeanUtil beanUtil = BeanUtil.getInstance();
        Set<KEY> presentKeys = paramMap.keySet();
        synchronized (paramMap) {
            for (KEY key : presentKeys) {
                beanUtil.setSimpleProperty(cn, key.fieldName, getParameterByKey(key));
            }
        }
    }
    
    /**
     * Copies the specified parameters into this {@code Parameters}
     * object over writing the intersecting keys and values.
     * @param p     the Parameters to perform a union with.
     * @return      this Parameters object combined with the specified
     *              Parameters object.
     */
    public Parameters union(Parameters p) {
        for(KEY k : p.paramMap.keySet()) {
            setParameterByKey(k, p.getParameterByKey(k));
        }
        return this;
    }
    
    /**
     * Returns a Set view of the keys in this {@code Parameter}s 
     * object
     * @return
     */
    public Set<KEY> keys() {
        Set<KEY> retVal = paramMap.keySet();
        return retVal;
    }
    
    /**
     * Returns a separate instance of the specified {@code Parameters} object.
     * @return      a unique instance.
     */
    public Parameters copy() {
        return new Parameters().union(this);
    }
    
    /**
     * Returns an empty instance of {@code Parameters};
     * @return
     */
    public static Parameters empty() {
        return new Parameters();
    }

    /**
     * Set parameter by Key{@link KEY}
     *
     * @param key
     * @param value
     */
    public void setParameterByKey(KEY key, Object value) {
        paramMap.put(key, value);
    }

    /**
     * Get parameter by Key{@link KEY}
     *
     * @param key
     * @return
     */
    public Object getParameterByKey(KEY key) {
        return paramMap.get(key);
    }

    /**
     * @param key IMPORTANT! This is a nuclear option, should be used with care. 
     * Will knockout key's parameter from map and compromise integrity
     */
    public void clearParameter(KEY key) {
        paramMap.remove(key);
    }

    /**
     * Convenience method to log difference this {@code Parameters} and specified
     * {@link Connections} object.
     *
     * @param cn
     * @return true if find it different
     */
    public boolean logDiff(Object cn) {
        if (cn == null) {
            throw new IllegalArgumentException("cn Object is required and can not be null");
        }
        boolean result = false;
        BeanUtil beanUtil = BeanUtil.getInstance();
        BeanUtil.PropertyInfo[] properties = beanUtil.getPropertiesInfoForBean(cn.getClass());
        for (int i = 0; i < properties.length; i++) {
            BeanUtil.PropertyInfo property = properties[i];
            String fieldName = property.getName();
            KEY propKey = KEY.getKeyByFieldName(property.getName());
            if (propKey != null) {
                Object paramValue = this.getParameterByKey(propKey);
                Object cnValue = beanUtil.getSimpleProperty(cn, fieldName);
                if ((paramValue != null && !paramValue.equals(cnValue)) || (paramValue == null && cnValue != null)) {
                    result = true;
                    System.out.println(
                            "Property:" + fieldName + " is different - CN:" + cnValue + " | PARAM:" + paramValue);
                }
            }
        }
        return result;
    }

    //TODO I'm not sure we need maintain implicit setters below. Kinda contradict unified access with KEYs

    /**
     * Returns the seeded random number generator.
     *
     * @param r the generator to use.
     */
    public void setRandom(Random r) {
        paramMap.put(KEY.RANDOM, r);
    }

    /**
     * Sets the number of {@link Column}.
     *
     * @param columnDimensions
     */
    public void setColumnDimensions(int[] columnDimensions) {
        paramMap.put(KEY.COLUMN_DIMENSIONS, columnDimensions);
    }

    /**
     * Sets the number of {@link Cell}s per {@link Column}
     *
     * @param cellsPerColumn
     */
    public void setCellsPerColumn(int cellsPerColumn) {
        paramMap.put(KEY.CELLS_PER_COLUMN, cellsPerColumn);
    }

    /**
     * Sets the activation threshold.
     * <p/>
     * If the number of active connected synapses on a segment
     * is at least this threshold, the segment is said to be active.
     *
     * @param activationThreshold
     */
    public void setActivationThreshold(int activationThreshold) {
        paramMap.put(KEY.ACTIVATION_THRESHOLD, activationThreshold);
    }

    /**
     * Radius around cell from which it can
     * sample to form distal dendrite connections.
     *
     * @param learningRadius
     */
    public void setLearningRadius(int learningRadius) {
        paramMap.put(KEY.LEARNING_RADIUS, learningRadius);
    }

    /**
     * If the number of synapses active on a segment is at least this
     * threshold, it is selected as the best matching
     * cell in a bursting column.
     *
     * @param minThreshold
     */
    public void setMinThreshold(int minThreshold) {
        paramMap.put(KEY.MIN_THRESHOLD, minThreshold);
    }

    /**
     * The maximum number of synapses added to a segment during learning.
     *
     * @param maxNewSynapseCount
     */
    public void setMaxNewSynapseCount(int maxNewSynapseCount) {
        paramMap.put(KEY.MAX_NEW_SYNAPSE_COUNT, maxNewSynapseCount);
    }

    /**
     * Seed for random number generator
     *
     * @param seed
     */
    public void setSeed(int seed) {
        paramMap.put(KEY.SEED, seed);
    }

    /**
     * Initial permanence of a new synapse
     *
     * @param
     */
    public void setInitialPermanence(double initialPermanence) {
        paramMap.put(KEY.INITIAL_PERMANENCE, initialPermanence);
    }

    /**
     * If the permanence value for a synapse
     * is greater than this value, it is said
     * to be connected.
     *
     * @param connectedPermanence
     */
    public void setConnectedPermanence(double connectedPermanence) {
        paramMap.put(KEY.CONNECTED_PERMANENCE, connectedPermanence);
    }

    /**
     * Amount by which permanences of synapses
     * are incremented during learning.
     *
     * @param permanenceIncrement
     */
    public void setPermanenceIncrement(double permanenceIncrement) {
        paramMap.put(KEY.PERMANENCE_INCREMENT, permanenceIncrement);
    }

    /**
     * Amount by which permanences of synapses
     * are decremented during learning.
     *
     * @param permanenceDecrement
     */
    public void setPermanenceDecrement(double permanenceDecrement) {
        paramMap.put(KEY.PERMANENCE_DECREMENT, permanenceDecrement);
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
        paramMap.put(KEY.INPUT_DIMENSIONS, inputDimensions);
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
        paramMap.put(KEY.POTENTIAL_RADIUS, potentialRadius);
    }

    /**
     * The inhibition radius determines the size of a column's local
     * neighborhood. of a column. A cortical column must overcome the overlap
     * score of columns in his neighborhood in order to become actives. This
     * radius is updated every learning round. It grows and shrinks with the
     * average number of connected synapses per column.
     *
     * @param inhibitionRadius the local group size
     */
    public void setInhibitionRadius(int inhibitionRadius) {
        paramMap.put(KEY.INHIBITION_RADIUS, inhibitionRadius);
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
        paramMap.put(KEY.POTENTIAL_PCT, potentialPct);
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
        paramMap.put(KEY.GLOBAL_INHIBITIONS, globalInhibition);
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
        paramMap.put(KEY.LOCAL_AREA_DENSITY, localAreaDensity);
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
        paramMap.put(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, numActiveColumnsPerInhArea);
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
        paramMap.put(KEY.STIMULUS_THRESHOLD, stimulusThreshold);
    }

    /**
     * The amount by which an inactive synapse is
     * decremented in each round. Specified as a percent of
     * a fully grown synapse.
     *
     * @param synPermInactiveDec
     */
    public void setSynPermInactiveDec(double synPermInactiveDec) {
        paramMap.put(KEY.SYN_PERM_INACTIVE_DEC, synPermInactiveDec);
    }

    /**
     * The amount by which an active synapse is incremented
     * in each round. Specified as a percent of a
     * fully grown synapse.
     *
     * @param synPermActiveInc
     */
    public void setSynPermActiveInc(double synPermActiveInc) {
        paramMap.put(KEY.SYN_PERM_ACTIVE_INC, synPermActiveInc);
    }

    /**
     * The default connected threshold. Any synapse whose
     * permanence value is above the connected threshold is
     * a "connected synapse", meaning it can contribute to
     * the cell's firing.
     *
     * @param synPermConnected
     */
    public void setSynPermConnected(double synPermConnected) {
        paramMap.put(KEY.SYN_PERM_CONNECTED, synPermConnected);
    }

    /**
     * Sets the increment of synapse permanences below the stimulus
     * threshold
     *
     * @param synPermBelowStimulusInc
     */
    public void setSynPermBelowStimulusInc(double synPermBelowStimulusInc) {
        paramMap.put(KEY.SYN_PERM_BELOW_STIMULUS_INC, synPermBelowStimulusInc);
    }

    /**
     * @param synPermTrimThreshold
     */
    public void setSynPermTrimThreshold(double synPermTrimThreshold) {
        paramMap.put(KEY.SYN_PERM_TRIM_THRESHOLD, synPermTrimThreshold);
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
     * @param minPctOverlapDutyCycles
     */
    public void setMinPctOverlapDutyCycle(double minPctOverlapDutyCycles) {
        paramMap.put(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, minPctOverlapDutyCycles);
    }

    /**
     * A number between 0 and 1.0, used to set a floor on
     * how often a column should be activate.
     * Periodically, each column looks at the activity duty
     * cycle of all other columns within its inhibition
     * radius and sets its own internal minimal acceptable
     * duty cycle to:
     * minPctDutyCycleAfterInh *
     * max(other columns' duty cycles).
     * On each iteration, any column whose duty cycle after
     * inhibition falls below this computed value will get
     * its internal boost factor increased.
     *
     * @param minPctActiveDutyCycles
     */
    public void setMinPctActiveDutyCycle(double minPctActiveDutyCycles) {
        paramMap.put(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, minPctActiveDutyCycles);
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
        paramMap.put(KEY.DUTY_CYCLE_PERIOD, dutyCyclePeriod);
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
     * 2 end points.
     *
     * @param maxBoost
     */
    public void setMaxBoost(double maxBoost) {
        paramMap.put(KEY.MAX_BOOST, maxBoost);
    }

    /**
     * spVerbosity level: 0, 1, 2, or 3
     *
     * @param spVerbosity
     */
    public void setSpVerbosity(int spVerbosity) {
        paramMap.put(KEY.SP_VERBOSITY, spVerbosity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("{\n");
        StringBuilder spatialInfo = new StringBuilder();
        StringBuilder temporalInfo = new StringBuilder();
        StringBuilder otherInfo = new StringBuilder();
        this.paramMap.keySet();
        for (KEY key : paramMap.keySet()) {
            if (DEFAULTS_SPATIAL.containsKey(key)) {
                buildParamStr(spatialInfo, key);
            } else if (DEFAULTS_TEMPORAL.containsKey(key)) {
                buildParamStr(temporalInfo, key);
            } else {
                buildParamStr(otherInfo, key);
            }
        }
        if (spatialInfo.length() > 0) {
            result.append("\tSpatial: {\n").append(spatialInfo).append("\t}\n");
        }
        if (temporalInfo.length() > 0) {
            result.append("\tTemporal: {\n").append(temporalInfo).append("\t}\n");
        }
        if (otherInfo.length() > 0) {
            result.append("\tOther: {\n").append(otherInfo).append("\t}\n");
        }
        return result.append("}").toString();
    }

    private void buildParamStr(StringBuilder spatialInfo, KEY key) {
        Object value = getParameterByKey(key);
        if (value instanceof int[]) {
            value = ArrayUtils.intArrayToString(value);
        }
        spatialInfo.append("\t\t").append(key.getFieldName()).append(":").append(value).append("\n");
    }

}
