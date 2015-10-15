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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joda.time.DateTime;
import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.EncoderTuple;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.NamedTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * <p>
 * Implementation of the biological layer of a region in the neocortex. Here,
 * a {@code Layer} contains the physical structure (columns, cells, dendrites etc)
 * shared by a sequence of algorithms which serve to implement the predictive inferencing
 * present in this, the allegory to its biological equivalent.
 * </p><p>
 * <b>COMPOSITION:</b>
 * A Layer is constructed with {@link Parameters} which configure the behavior of most
 * of the key algorithms a Layer may contain. It is also <em>optionally</em> constructed with each of the
 * algorithms in turn. A Layer that includes an {@link Encoder} is always initially configured with
 * a {@link MultiEncoder}. The child encoders contained within the MultiEncoder are configured from
 * the Map included with the specified Parameters, keyed by {@link Parameters.KEY#FIELD_ENCODING_MAP}. 
 * </p><p>
 * A field encoding map consists of one map for each of the fields to be encoded. Each individual map
 * in the field encoding map contains the typical {@link Encoder} parameters, plus a few "meta" parameters
 * needed to describe the field and its data type as follows:
 * </p>
 * 
 * <pre>
 *      Map&lt;String, Map&lt;String, Object&gt;&gt; fieldEncodings = new HashMap&lt;&gt;();
 *      
 *      Map&lt;String, Object&gt; inner = new HashMap&lt;&gt;();
 *      inner.put("n", n);
 *      inner.put("w", w);
 *      inner.put("minVal", min);
 *      inner.put("maxVal", max);
 *      inner.put("radius", radius);
 *      inner.put("resolution", resolution);
 *      inner.put("periodic", periodic);
 *      inner.put("clip", clip);
 *      inner.put("forced", forced);
 *      // These are meta info to aid in Encoder construction
 *      inner.put("fieldName", fieldName);
 *      inner.put("fieldType", fieldType); (see {@link FieldMetaType} for type examples)
 *      inner.put("encoderType", encoderType); (i.e. ScalarEncoder, SDRCategoryEncoder, DateEncoder...etc.)
 *      
 *      Map&lt;String, Object&gt; inner2 = new HashMap&lt;&gt;();
 *      inner.put("n", n);
 *      inner.put("w", w);
 *      inner.put("minVal", min);
 *      inner.put("maxVal", max);
 *      inner.put("radius", radius);
 *      inner.put("resolution", resolution);
 *      inner.put("periodic", periodic);
 *      inner.put("clip", clip);
 *      inner.put("forced", forced);
 *      // These are meta info to aid in Encoder construction
 *      inner.put("fieldName", fieldName);
 *      inner.put("fieldType", fieldType); (see {@link FieldMetaType} for type examples)
 *      inner.put("encoderType", encoderType); (i.e. ScalarEncoder, SDRCategoryEncoder, DateEncoder...etc.)
 *      
 *      fieldEncodings.put("consumption", inner);  // Where "consumption" is an example field name (field name is "generic" in above code)
 *      fieldEncodings.put("temperature", inner2);
 *      
 *      Parameters p = Parameters.getDefaultParameters();
 *      p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);
 * </pre>
 * For an example of how to create the field encodings map in a reusable way, see NetworkTestHarness and
 * its usage within the LayerTest class.
 * 
 * <p>
 * The following is an example of Layer construction with everything included (i.e. Sensor, SpatialPooler, TemporalMemory, CLAClassifier, Anomaly (computer))
 * <pre>
 *      // See the test harness for more information
 *      Parameters p = NetworkTestHarness.getParameters();
 *      
 *      // How to merge (union) two {@link Parameters} objects. This one merges
 *      // the Encoder parameters into default parameters.
 *      p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
 *      
 *      // You can overwrite parameters as needed like this
 *      p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
 *      p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
 *      p.setParameterByKey(KEY.POTENTIAL_RADIUS, 200);
 *      p.setParameterByKey(KEY.INHIBITION_RADIUS, 50);
 *      p.setParameterByKey(KEY.GLOBAL_INHIBITIONS, true);
 *      
 *      Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
 *      params.put(KEY_MODE, Mode.PURE);
 *      params.put(KEY_WINDOW_SIZE, 3);
 *      params.put(KEY_USE_MOVING_AVG, true);
 *      Anomaly anomalyComputer = Anomaly.create(params);
 *      
 *      Layer&lt;?&gt; l = Network.createLayer("TestLayer", p)
 *          .alterParameter(KEY.AUTO_CLASSIFY, true)
 *          .add(anomalyComputer)
 *          .add(new TemporalMemory())
 *          .add(new SpatialPooler())
 *          .add(Sensor.create(
 *              FileSensor::create, 
 *                  SensorParams.create(
 *                      Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv"))));
 * </pre>
 * 
 * 
 *  
 *  
 * @author David Ray
 */
public class Layer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Layer.class);

    private Network parentNetwork;
    private Region parentRegion;
    
    private Parameters params;
    private Connections connections;
    private HTMSensor<?> sensor;
    private MultiEncoder encoder;
    private SpatialPooler spatialPooler;
    private TemporalMemory temporalMemory;
    private Boolean autoCreateClassifiers;
    private Anomaly anomalyComputer;
    
    private PublishSubject<T> publisher = null;
    private Subscription subscription;
    private Observable<Inference> userObservable;
    private Inference currentInference;
    
    FunctionFactory factory;

    private int[] activeColumns;
    private int[] sparseActives;
    private int[] previousPrediction;
    private int[] currentPrediction;
    private int cellsPerColumn;
    private int recordNum = -1;
    
    private String name;
    
    private boolean isClosed;
    private boolean isHalted;
    private boolean isLearn = true;
    
    private Layer<Inference> next;
    private Layer<Inference> previous;
    
    private List<Observer<Inference>> observers = new ArrayList<Observer<Inference>>();
    private ConcurrentLinkedQueue<Observer<Inference>> subscribers = new ConcurrentLinkedQueue<Observer<Inference>>();
    
    /** Retains the order of added items - for use with interposed {@link Observable} */
    private List<Object> addedItems = new ArrayList<>();
    /** Indicates whether there is a generic processing node entered */
    private boolean hasGenericProcess;
    
    /** 
     * List of {@link Encoders} used when storing bucket information 
     * see {@link #doEncoderBucketMapping(Inference, Map)} 
     */
    private List<EncoderTuple> encoderTuples;
    
    private Map<Class<T>, Observable<ManualInput>> observableDispatch = 
        Collections.synchronizedMap(new HashMap<Class<T>, Observable<ManualInput>>());

    /** This layer's thread */
    private Thread LAYER_THREAD;
    
    static final byte SPATIAL_POOLER = 1;
    static final byte TEMPORAL_MEMORY = 2;
    static final byte CLA_CLASSIFIER = 4;
    static final byte ANOMALY_COMPUTER = 8;
    
    private byte algo_content_mask = 0;
    
    /**
     * Creates a new {@code Layer} using the {@link Network}
     * level {@link Parameters}
     * 
     * @param n     the parent {@link Network}
     */
    public Layer(Network n) {
        this(n, n.getParameters());
    }

    /**
     * Creates a new {@code Layer} using the specified {@link Parameters}
     * 
     * @param n     the parent {@link Network}
     * @param p     the {@link Parameters} to use with this {@code Layer}
     */
    public Layer(Network n, Parameters p) {
        this("[Layer " + System.currentTimeMillis() + "]", n, p);
    }
    
    /**
     * Creates a new {@code Layer} using the specified {@link Parameters}
     * 
     * @param name  the name identifier of this {@code Layer}
     * @param n     the parent {@link Network}
     * @param p     the {@link Parameters} to use with this {@code Layer}
     */
    public Layer(String name, Network n, Parameters p) {
        this.name = name;
        this.parentNetwork = n;
        this.params = p;

        connections = new Connections();
        
        this.autoCreateClassifiers = (Boolean)p.getParameterByKey(KEY.AUTO_CLASSIFY);

        factory = new FunctionFactory();
        
        observableDispatch = createDispatchMap();
    }
    
    /**
     * Sets the parent {@link Network} on this {@code Layer}
     * @param network
     */
    public void setNetwork(Network network) {
        this.parentNetwork = network;
    }

    /**
     * Creates a new {@code Layer} initialized with the specified 
     * algorithmic components.
     * 
     * @param params                    A {@link Parameters} object containing configurations
     *                                  for a SpatialPooler, TemporalMemory, and Encoder (all or none
     *                                  may be used).
     * @param e                         (optional) The Network API only uses a {@link MultiEncoder} at the top level
     *                                  because of its ability to delegate to child encoders.
     * @param sp                        (optional) {@link SpatialPooler}
     * @param tm                        (optional) {@link TemporalMemory}
     * @param autoCreateClassifiers     (optional) Indicates that the {@link Parameters} object contains the configurations
     *                                  necessary to create the required encoders.
     * @param a                         (optional) An {@link Anomaly} computer.
     */
    public Layer(Parameters params, MultiEncoder e, SpatialPooler sp, 
        TemporalMemory tm, Boolean autoCreateClassifiers, Anomaly a) {
        
        // Make sure we have a valid parameters object
        if(params == null) {
            throw new IllegalArgumentException("No parameters specified.");
        }

        // Check to see if the Parameters include the encoder configuration.
        if(params.getParameterByKey(KEY.FIELD_ENCODING_MAP) == null && e != null) {
            throw new IllegalArgumentException("The passed in Parameters must contain a field encoding map " +
                "specified by org.numenta.nupic.Parameters.KEY.FIELD_ENCODING_MAP");
        }

        this.params = params;
        this.encoder = e;
        this.spatialPooler = sp;
        this.temporalMemory = tm;
        this.autoCreateClassifiers = autoCreateClassifiers;
        this.anomalyComputer = a;

        connections = new Connections();
        factory = new FunctionFactory();
        
        observableDispatch = createDispatchMap();
        
        initializeMask();

        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Layer successfully created containing: {}{}{}{}{}",
                (encoder == null ? "" : "MultiEncoder,"),
                (spatialPooler == null ? "" : "SpatialPooler,"),
                (temporalMemory == null ? "" : "TemporalMemory,"),
                (autoCreateClassifiers == null ? "" : "Auto creating CLAClassifiers for each input field."),
                (anomalyComputer == null ? "" : "Anomaly"));
        }
    }
    
    /**
     * Sets the parent region which contains this {@code Layer}
     * @param r
     */
    public void setRegion(Region r) {
        this.parentRegion = r;
    }
    
    /**
     * Finalizes the initialization in one method call so that side effect operations
     * to share objects and other special initialization tasks can happen all at once
     * in a central place for maintenance ease.
     */
    @SuppressWarnings("unchecked")
    public Layer<T> close() {
        if(isClosed) {
            LOGGER.warn("Close called on Layer " + getName() + " which is already closed.");
            return this;
        }
        
        params.apply(connections);
        
        if(sensor != null) {
            encoder = encoder == null ? sensor.getEncoder() : encoder;
            sensor.initEncoder(params);
            connections.setNumInputs(encoder.getWidth());
            if(parentNetwork != null && parentRegion != null) {
                parentNetwork.setSensorRegion(parentRegion);
            }
        }

        // Create Encoder hierarchy from definitions & auto create classifiers if specified
        if(encoder != null) {
            if(encoder.getEncoders(encoder) == null || encoder.getEncoders(encoder).size() < 1) {
                if(params.getParameterByKey(KEY.FIELD_ENCODING_MAP) == null ||
                    ((Map<String, Map<String, Object>>)params.getParameterByKey(KEY.FIELD_ENCODING_MAP)).size() < 1) {
                    LOGGER.error("No field encoding map found for specified MultiEncoder");
                    throw new IllegalStateException("No field encoding map found for specified MultiEncoder");
                }
                
                encoder.addMultipleEncoders(
                    (Map<String, Map<String, Object>>)params.getParameterByKey(
                        KEY.FIELD_ENCODING_MAP));
            }
            
            // Make the declared column dimensions match the actual input dimensions retrieved from the encoder 
            int product = 0, inputLength = 0, columnLength = 0;
            if(((inputLength = ((int[])params.getParameterByKey(KEY.INPUT_DIMENSIONS)).length) !=
                (columnLength = ((int[])params.getParameterByKey(KEY.COLUMN_DIMENSIONS)).length)) ||
                    encoder.getWidth() != (product = ArrayUtils.product((int[])params.getParameterByKey(KEY.INPUT_DIMENSIONS))) ) {
                
                LOGGER.warn("The number of Input Dimensions (" + inputLength + ") != number of Column Dimensions " +
                    "(" + columnLength + ") --OR-- Encoder width (" + encoder.getWidth() + ") != product of dimensions (" + product +") -- now attempting to fix it.");
                
                int[] inferredDims = inferInputDimensions(encoder.getWidth(), columnLength);
                if(inferredDims != null && inferredDims.length > 0 && encoder.getWidth() == ArrayUtils.product(inferredDims)) {
                    LOGGER.info("Input dimension fix successful!");
                    LOGGER.info("Using calculated input dimensions: " + Arrays.toString(inferredDims));
                }
                
                params.setInputDimensions(inferredDims);
                connections.setInputDimensions(inferredDims);
            }
            
        }
        
        autoCreateClassifiers = autoCreateClassifiers != null && 
            (autoCreateClassifiers | (Boolean)params.getParameterByKey(KEY.AUTO_CLASSIFY));
        
        if(autoCreateClassifiers != null && 
            autoCreateClassifiers.booleanValue() &&
                (factory.inference.getClassifiers() == null || factory.inference.getClassifiers().size() < 1)) {
            factory.inference.classifiers(makeClassifiers(encoder == null ? parentNetwork.getEncoder() : encoder));
            
            // Note classifier addition by setting content mask
            algo_content_mask |= CLA_CLASSIFIER;
        }
        
        // We must adjust this Layer's inputDimensions to the size of the input received from the
        // previous Region's output vector.
        if(parentRegion != null && parentRegion.getUpstreamRegion() != null) {
            int[] upstreamDims = parentRegion.getUpstreamRegion().getHead().getConnections().getColumnDimensions();
            params.setInputDimensions(upstreamDims);
            connections.setInputDimensions(upstreamDims);
        }else if(parentRegion != null && parentNetwork != null && 
            parentRegion.equals(parentNetwork.getSensorRegion()) && encoder == null && spatialPooler != null) {
            
            Layer<?> curr = this;
            while((curr = curr.getPrevious()) != null) {
                if(curr.getEncoder() != null) {
                    int[] dims = (int[])curr.getParameters().getParameterByKey(KEY.INPUT_DIMENSIONS);
                    params.setInputDimensions(dims);
                    connections.setInputDimensions(dims);
                }
            }
        }

        // Let the SpatialPooler initialize the matrix with its requirements
        if(spatialPooler != null) {
            // The exact dimensions don't have to be the same but the number of dimensions do!
            int inputLength, columnLength = 0;
            if((inputLength = ((int[])params.getParameterByKey(KEY.INPUT_DIMENSIONS)).length) !=
                (columnLength = ((int[])params.getParameterByKey(KEY.COLUMN_DIMENSIONS)).length)) {
                LOGGER.warn("The number of Input Dimensions (" + inputLength + ") is not same as the number of Column Dimensions " +
                    "(" + columnLength + ") in Parameters!");
                
                return this;
            }
            spatialPooler.init(connections);
        }

        // Let the TemporalMemory initialize the matrix with its requirements
        if(temporalMemory != null) {
            temporalMemory.init(connections);
        }

        this.activeColumns = new int[connections.getNumColumns()];
        this.cellsPerColumn = connections.getCellsPerColumn();
        
        this.isClosed = true;
        
        LOGGER.debug("Layer " + name + " content initialize mask = " + Integer.toBinaryString(algo_content_mask));
        
        return this;
    }
    
    /**
     * Given an input field width and Spatial Pooler dimensionality; this
     * method will return an array of dimension sizes whose number is equal
     * to the number of column dimensions. The sum of the returned dimensions
     * will be equal to the flat input field width specified. 
     * 
     * This method should be called when a disparity in dimensionality between
     * the input field and the number of column dimensions is detected. Otherwise
     * if the input field dimensionality is correctly specified, this method 
     * should <b>not</b> be used.
     * 
     * @param inputWidth        the flat input width of an {@link Encoder}'s output or the
     *                          vector used as input to the {@link SpatialPooler}
     * @param numColumnDims     a number specifying the number of column dimensions that 
     *                          should be returned.
     * @return
     */
    public int[] inferInputDimensions(int inputWidth, int numColumnDims) {
        double flatSize = inputWidth;
        double numColDims = numColumnDims;
        double sliceArrangement = Math.pow(flatSize, 1/numColDims);
        double remainder = sliceArrangement % (int)sliceArrangement;
        int[] retVal = new int[(int)numColDims];
        if(remainder > 0) {
            for(int i = 0;i < numColDims - 1;i++) retVal[i] = 1;
            retVal[(int)numColDims - 1] = (int)flatSize;
        }else{
            for(int i = 0;i < numColDims;i++) retVal[i] = (int)sliceArrangement; 
        }
        
        return retVal;
    }
    
    /**
     * Returns an {@link Observable} that can be subscribed to, or
     * otherwise operated upon by a 
     * @return
     */
    @SuppressWarnings("unchecked")
    public Observable<Inference> observe() {
        if(userObservable == null) {
            userObservable = Observable.create(new Observable.OnSubscribe<Inference>() {
                @Override public void call(Subscriber<? super Inference> t1) {
                    observers.add((Observer<Inference>) t1);
                }
            });
        }
        
        return userObservable;
    }

    /**
     * Called by the {@code Layer} client to receive output {@link Inference}s
     * from the configured algorithms.
     * 
     * @param subscriber	a {@link Subscriber} to be notified as data is published.
     * @return
     */
    public Subscription subscribe(final Observer<Inference> subscriber) {
        if(subscriber == null) {
            throw new IllegalArgumentException("Subscriber cannot be null.");
        }

        subscribers.add(subscriber);

        return createSubscription(subscriber);
    }
    
    /**
     * Allows the user to define the {@link Connections} object data structure to use.
     * Or possibly to share connections between two {@code Layer}s
     * 
     * @param c     the {@code Connections} object to use.
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> using(Connections c) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        this.connections = c;
        return this;
    }
    
    /**
     * Allows the user to specify the {@link Parameters} object used by
     * this {@code Layer}. If the intent is to share Parameters across 
     * multiple Layers, it must be kept in mind that two Layers containing
     * the same algorithm may require specification of locally different
     * parameter settings. In this case, one could use {@link #alterParameter(KEY, Object)}
     * method to change a local setting without impacting the same setting
     * in the source parameters object. This is made possible because the 
     * {@link #alterParameter(KEY, Object)} method first makes a local copy
     * of the {@link Parameters} object, then modifies the specified parameter.
     * 
     * @param p
     * @return
     */
    public Layer<T> using(Parameters p) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        this.params = p;
        return this;
    }

    /**
     * Adds an {@link HTMSensor} to this {@code Layer}
     * @param sensor
     * @return          this Layer instance (in fluent-style)
     */
    @SuppressWarnings("rawtypes")
    public Layer<T> add(Sensor sensor) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
     
        this.sensor = (HTMSensor<?>)sensor;
        if(parentNetwork != null && parentRegion != null) {
            parentNetwork.setSensorRegion(parentRegion);
        }
        return this;
    }

    /**
     * Adds a {@link MultiEncoder} to this {@code Layer}
     * @param encoder   the added MultiEncoder
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> add(MultiEncoder encoder) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        
        this.encoder = encoder;
        return this;
    }

    /**
     * Adds a {@link SpatialPooler} to this {@code Layer}
     * @param sp        the added SpatialPooler
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> add(SpatialPooler sp) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        
        // Preserve addition order
        addedItems.add(sp);
        
        this.algo_content_mask |= SPATIAL_POOLER;
        this.spatialPooler = sp;
        return this;
    }

    /**
     * Adds a {@link TemporalMemory} to this {@code Layer}
     * @param tm        the added TemporalMemory
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> add(TemporalMemory tm) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        
        // Preserve addition order
        addedItems.add(tm);
        
        this.algo_content_mask |= TEMPORAL_MEMORY;
        this.temporalMemory = tm;
        
        return this;
    }

    /**
     * Adds an {@link Anomaly} computer to this {@code Layer}
     * @param anomalyComputer   the Anomaly instance
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> add(Anomaly anomalyComputer) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        
        // Preserve addition order
        addedItems.add(anomalyComputer);
        
        this.algo_content_mask |= ANOMALY_COMPUTER;
        this.anomalyComputer = anomalyComputer;
        return this;
    }
    
    /**
     * Adds a "generic" processing node into this {@code Layer}'s 
     * processing chain.
     * 
     * <em><b>NOTE: When adding a generic node, the order of calls to
     * the addXXX() methods becomes crucially important. Make sure you 
     * have added items in a valid order in your "fluent" add call declarations.</b></em>
     * 
     * @param func  a {@link Func1} function to be performed at the point of 
     *              insertion within the {@code Layer}'s declaration.
     * @return
     */
    public Layer<T> add(Func1<ManualInput, ManualInput> func) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        if(func == null) {
            throw new IllegalArgumentException("Cannot add a null Function");
        }
        
        hasGenericProcess = true;
        // Preserve addition order
        addedItems.add(func);
        return this;
    }
    
    /**
     * Adds the ability to alter a given parameter in place during
     * a fluent creation statement. This {@code Layer}'s {@link Parameters}
     * object is copied and then the specified key/value pair are set on 
     * the internal copy. This call does not affect the original Parameters
     * object so that local modifications may be made without having to reset
     * them afterward for subsequent use with another network structure.
     * 
     * @param key
     * @param value
     * @return
     */
    public Layer<T> alterParameter(KEY key, Object value) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        
        // Preserve any input dimensions that might have been set prior to this in 
        // previous layers
        int[] inputDims = (int[])params.getParameterByKey(KEY.INPUT_DIMENSIONS);
        
        this.params = this.params.copy();
        this.params.setParameterByKey(key, value);
        this.params.setParameterByKey(KEY.INPUT_DIMENSIONS, inputDims);
        
        if(key == KEY.AUTO_CLASSIFY) {
            this.autoCreateClassifiers = value == null ? 
                false : ((Boolean)value).booleanValue();
            // Note the addition of a classifier
            algo_content_mask |= CLA_CLASSIFIER;
        }
        return this;
    }
    
    /**
     * Returns the configured {@link Sensor} if any exists in this
     * {@code Layer}, or null if one does not.
     * @return
     */
    public HTMSensor<?> getSensor() {
        return sensor;
    }
    
    /**
     * Returns the {@link Connections} object being used by this {@link Layer}
     * 
     * @return
     */
    public Connections getConnections() {
        return this.connections;
    }
    
    /**
     * Processes a single element, sending the specified input up the configured 
     * chain of algorithms or components within this {@code Layer}; resulting in
     * any {@link Subscriber}s or {@link Observer}s being notified of results corresponding
     * to the specified input (unless a {@link SpatialPooler} "primer delay" has 
     * been configured).
     * 
     * The first input to the Layer invokes a method to resolve the transformer at the
     * bottom of the input chain, therefore the "type" (&lt;T&gt;) of the input cannot
     * be changed once this method is called for the first time.
     * 
     * @param t     the input object who's type is generic.
     */
    public void compute(T t) {
        if(!isClosed) {
            close();
        }
        
        increment();
        
        if(!dispatchCompleted()) {
            completeDispatch(t);
        }
        
        publisher.onNext(t);
    }
    
    /**
     * Stops the processing of this {@code Layer}'s processing
     * thread.
     */
    public void halt() {
        // Signal the Observer chain to complete
        if(LAYER_THREAD == null) {
            publisher.onCompleted();
            if(next != null) {
                next.halt();
            }
        }
        this.isHalted = true; 
    }
    
    /**
     * Returns a flag indicating whether this layer's processing
     * thread has been halted or not.
     * @return
     */
    public boolean isHalted() {
        return isHalted;
    }
    
    /**
     * Sets the learning mode.
     * @param isLearn
     */
    public void setLearn(boolean isLearn) {
        this.isLearn = isLearn;
    }
    
    /**
     * Returns the learning mode setting.
     * @return
     */
    public boolean isLearn() {
        return isLearn;
    }
    
    /**
     * Completes the dispatch chain of algorithm {@link Observable}s with
     * specialized {@link Transformer}s for each algorithm contained within
     * this Layer. This method then starts the output stream processing of
     * its {@link Sensor} in a separate {@link Thread} (if it exists) - logging
     * this event.
     * 
     * Calling this method sets a flag on the underlying Sensor marking it as
     * "Terminal" meaning that it cannot be restarted and its output stream
     * cannot be accessed again. 
     */
    @SuppressWarnings("unchecked")
    public void start() {
        // Save boilerplate setup steps by automatically closing when start is called.
        if(!isClosed) {
            close();
        }
        
        if(sensor == null) {
            throw new IllegalStateException("A sensor must be added when the mode is not Network.Mode.MANUAL");
        }
        
        this.encoder = encoder == null ? sensor.getEncoder() : encoder;
        
        completeDispatch((T)new int[] {});
        
        (LAYER_THREAD = new Thread("Sensor Layer [" + getName() + "] Thread") {
            public void run() {
                LOGGER.debug("Layer [" + getName() + "] started Sensor output stream processing.");
                
                // Applies "terminal" function, at this point the input stream is  "sealed".
                sensor.getOutputStream().filter(i ->  {
                    if(isHalted) {
                        notifyComplete();
                        if(next != null) {
                            next.halt();
                        }
                        return false;
                    }
                    
                    return true;
                }).forEach(intArray -> {
                    ((ManualInput)Layer.this.factory.inference).encoding(intArray);
                    Layer.this.compute((T)intArray);
                    
                    // Notify all downstream observers that the stream is closed
                    if(!sensor.hasNext()) {
                        notifyComplete();
                    }
                });
            }
        }).start(); 
        
        LOGGER.debug("Start called on Layer thread {}", LAYER_THREAD);
    }
    
    /**
     * Sets a pointer to the "next" Layer in this {@code Layer}'s 
     * {@link Observable} sequence.
     * @param l
     */
    public void next(Layer<Inference> l) {
        this.next = l;
    }
    
    /**
     * Returns the next Layer following this Layer in order of 
     * process flow.
     * 
     * @return
     */
    public Layer<Inference> getNext() {
        return next;
    }
    
    /**
     * Sets a pointer to the "previous" Layer in this {@code Layer}'s 
     * {@link Observable} sequence.
     * @param l
     */
    public void previous(Layer<Inference> l) {
        this.previous = l;
    }
    
    /**
     * Returns the previous Layer preceding this Layer in order of 
     * process flow.
     * 
     * @return
     */
    public Layer<Inference> getPrevious() {
        return previous;
    }
    
    /**
     * Returns a flag indicating whether this {@code Layer} is configured with
     * a {@link Sensor} which requires starting up.
     * @return
     */
    public boolean hasSensor() {
        return sensor != null;
    }
    
    /**
     * Returns the {@link Thread} from which this {@code Layer} is currently
     * outputting data.
     * 
     * @return
     */
    public Thread getLayerThread() {
        if(LAYER_THREAD != null) {
            return LAYER_THREAD;
        }
        return Thread.currentThread();
    }
    
    /**
     * Returns the {@link Parameters} used to configure this layer.
     * @return
     */
    public Parameters getParameters() {
        return this.params;
    }
    /**
     * Returns the current prediction.
     * 
     * @return  the binary vector representing the current prediction.
     */
    public int[] getPredictedColumns() {
        return currentPrediction;
    }

    /**
     * Returns the current prediction.
     * 
     * @return  the binary vector representing the current prediction.
     */
    public int[] getPreviousPredictedColumns() {
        return previousPrediction;
    }

    /**
     * Returns the current (dense) array of column indexes which 
     * represent columns which have become activated during the 
     * current input sequence.
     * 
     * @return the array of active column indexes
     */
    public int[] getActiveColumns() {
        return activeColumns;
    }

    /**
     * Sets the sparse form of the {@link SpatialPooler} column 
     * activations and returns the specified array.
     * 
     * @param activesInSparseForm   the sparse column activations
     * @return
     */
    int[] sparseActives(int[] activesInSparseForm) {
        this.sparseActives = activesInSparseForm;
        return this.sparseActives;
    }

    /**
     * Returns the SpatialPooler column activations in sparse form
     * (indexes of the on bits).
     * 
     * @return
     */
    public int[] getSparseActives() {
        return sparseActives;
    }

    /**
     * Returns the {@link Connections} object being used as the
     * structural matrix and state.
     */
    public Connections getMemory() {
        return connections;
    }

    /**
     * Returns the count of records historically inputted into
     * this {@code Layer}
     * 
     * @return  the current record input count
     */
    public int getRecordNum() {
        return recordNum;
    }

    /**
     * Resets the internal record count to zero
     * 
     * @return  this {@code LayerImpl}
     */
    public Layer<T> resetRecordNum() {
        recordNum = 0;
        return this;
    }
    
    /**
     * Resets the {@link TemporalMemory} if it exists.
     */
    public void reset() {
        if(temporalMemory == null) {
            LOGGER.debug("Attempt to reset Layer: " + getName() + "without TemporalMemory");
        }else{
            temporalMemory.reset(connections);
            resetRecordNum();
        }
    }
    
    /**
     * Returns a flag indicating whether this {@code Layer} contains
     * a {@link TemporalMemory}.
     * @return
     */
    public boolean hasTemporalMemory() {
        return temporalMemory != null;
    }

    /**
     * Increments the current record sequence number.
     */
    public Layer<T> increment() {
        ++recordNum;
        return this;
    }

    /**
     * Skips the specified count of records and internally
     * alters the record sequence number.
     * 
     * @param count
     */
    public Layer<T> skip(int count) {
        recordNum += count;
        return this;
    }
    
    /**
     * Sets the name and returns this Layer.
     * @param name
     * @return
     */
    Layer<T> name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * Returns the String identifier of this {@code Layer}
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the last computed {@link Inference} of this {@code Layer}
     * @return  the last computed inference.
     */
    public Inference getInference() {
        return currentInference;
    }
    
    /**
     * Returns the resident {@link MultiEncoder} or the encoder residing
     * in this {@code Layer}'s {@link Sensor}, if any.
     * 
     * @return
     */
    public MultiEncoder getEncoder() {
        if(encoder != null) {
            return encoder;
        }
        if(hasSensor()) {
            return sensor.getEncoder();
        }
        
        MultiEncoder e = parentNetwork.getEncoder();
        if(e != null) {
            return e;
        }
        
        return null;
    }
    
    /**
     * Returns the values submitted to this {@code Layer} in an array 
     * whose indexes correspond to the indexes of probabilities returned
     * when calling {@link #getAllPredictions(String, int)}.
     * 
     * @param field     The field name of the required prediction
     * @param step      The step for the required prediction
     * @return
     */
    @SuppressWarnings("unchecked")
    public <V> V[] getAllValues(String field, int step) {
        if(currentInference == null || currentInference.getClassifiers() == null) {
            throw new IllegalStateException("Predictions not available. " +
                "Either classifiers unspecified or inferencing has not yet begun.");
        }

        ClassifierResult<?> c = currentInference.getClassification(field);
        if(c == null) {
            LOGGER.debug("No ClassifierResult exists for the specified field: {}", field);
        }

        return (V[])c.getActualValues();
    }

    /**
     * Returns a double[] containing a prediction confidence measure for 
     * each bucket (unique entry as determined by an encoder). In order 
     * to relate the probability to an actual value, call {@link #getAllValues(String, int)}
     * which returns an array containing the actual values submitted to this {@code Layer} -
     * the indexes of each probability will match the index of each actual value entered.
     *  
     * @param field     The field name of the required prediction
     * @param step      The step for the required prediction
     * @return
     */
    public double[] getAllPredictions(String field, int step) {
        if(currentInference == null || currentInference.getClassifiers() == null) {
            throw new IllegalStateException("Predictions not available. " +
                "Either classifiers unspecified or inferencing has not yet begun.");
        }

        ClassifierResult<?> c = currentInference.getClassification(field);
        if(c == null) {
            LOGGER.debug("No ClassifierResult exists for the specified field: {}", field);
        }

        return c.getStats(step);
    }

    /**
     * Returns the value whose probability is calculated to be the highest for
     * the specified field and step.
     * 
     * @param field     The field name of the required prediction
     * @param step      The step for the required prediction
     * @return
     */
    @SuppressWarnings("unchecked")
    public <K> K getMostProbableValue(String field, int step) {
        if(currentInference == null || currentInference.getClassifiers() == null) {
            throw new IllegalStateException("Predictions not available. " +
                "Either classifiers unspecified or inferencing has not yet begun.");
        }

        ClassifierResult<?> c = currentInference.getClassification(field);
        if(c == null) {
            LOGGER.debug("No ClassifierResult exists for the specified field: {}", field);
        }

        return (K)c.getMostProbableValue(step);
    }

    /**
     * Returns the bucket index of the value with the highest calculated probability
     * for the specified field and step.
     * 
     * @param field     The field name of the required prediction
     * @param step      The step for the required prediction
     * @return
     */
    public int getMostProbableBucketIndex(String field, int step) {
        if(currentInference == null || currentInference.getClassifiers() == null) {
            throw new IllegalStateException("Predictions not available. " +
                "Either classifiers unspecified or inferencing has not yet begun.");
        }

        ClassifierResult<?> c = currentInference.getClassification(field);
        if(c == null) {
            LOGGER.debug("No ClassifierResult exists for the specified field: {}", field);
        }

        return c.getMostProbableBucketIndex(step);
    }


    //////////////////////////////////////////////////////////////
    //          PRIVATE METHODS AND CLASSES BELOW HERE          //
    ////////////////////////////////////////////////////////////// 
    /**
     * Notify all subscribers through the delegate that stream processing
     * has been completed or halted.
     */
    void notifyComplete() {
        for(Observer<Inference> o : subscribers) {
            o.onCompleted();
        }
        for(Observer<Inference> o : observers) {
            o.onCompleted();
        }
        publisher.onCompleted();
    }
    
    /**
     * <p>
     * Returns the content mask used to indicate what algorithm
     * contents this {@code Layer} has. This is used to determine
     * whether the {@link Inference} object passed between layers
     * should share values.
     * </p> 
     * <p>
     * If any algorithms are repeated then
     * {@link Inference}s will <em><b>NOT</b></em> be shared between layers.
     * {@link Regions} <em><b>NEVER</b></em> share {@link Inference}s
     * </p>
     * @return
     */
    byte getMask() {
        return algo_content_mask;
    }
    
    /**
     * Initializes the algorithm content mask used for detection
     * of repeated algorithms among {@code Layer}s in a {@link Region}.
     * see {@link #getMask()} for more information.
     */
    private void initializeMask() {
        algo_content_mask |= (this.spatialPooler == null ? 0 : SPATIAL_POOLER);
        algo_content_mask |= (this.temporalMemory == null ? 0 : TEMPORAL_MEMORY);
        algo_content_mask |= (this.autoCreateClassifiers == null || !autoCreateClassifiers ? 0 : CLA_CLASSIFIER);
        algo_content_mask |= (this.anomalyComputer == null ? 0 : ANOMALY_COMPUTER);
    }
    
    /**
     * Returns a flag indicating whether we've connected the first observable in 
     * the sequence (which lazily does the input type of &lt;T&gt; to {@link Inference} 
     * transformation) to the Observables connecting the rest of the algorithm
     * components.
     * 
     * @return  flag indicating all observables connected. True if so, false if not
     */
    private boolean dispatchCompleted() {
        return observableDispatch == null;
    }
    
    /**
     * Connects the first observable which does the transformation of input 
     * types, to the rest of the sequence - then clears the helper map and sets
     * it to null.
     * 
     * @param t
     */
    private void completeDispatch(T t) {
        // Get the Input Transformer for the specified input type
        Observable<ManualInput> sequence = resolveObservableSequence(t);
        
        // If this Layer has a Sensor, map its encoder buckets
        sequence = mapEncoderBuckets(sequence);
        
        // Add the rest of the chain observables for the other added algorithms.
        sequence = fillInSequence(sequence);
        
        // All subscribers and observers are notified from a single delegate.
        subscribers.add(getDelegateObserver());
        subscription = sequence.subscribe(getDelegateSubscriber());
        
        // The map of input types to transformers is no longer needed.
        observableDispatch.clear();
        observableDispatch = null;
        
        // Handle global network sensor access.
        if(sensor == null) {
            sensor = parentNetwork == null ? null : parentNetwork.getSensor();
        }else if(parentNetwork != null) {
            parentNetwork.setSensor(sensor);
        }
    }
    
    /**
     * We cannot create the {@link Observable} sequence all at once because the 
     * first step is to transform the input type to the type the rest of the 
     * sequence uses (Observable<b>&lt;Inference&gt;</b>). This can only happen during
     * the actual call to {@link #compute(Object)} which presents the input type -
     * so we create a map of all types of expected inputs, and then connect the
     * sequence at execution time; being careful to only incur the cost of sequence assembly
     * on the first call to {@link #compute(Object)}. After the first call, we dispose
     * of this map and its contents.
     * 
     * @return  the map of input types to {@link Transformer}
     */
    @SuppressWarnings("unchecked")
    private Map<Class<T>, Observable<ManualInput>> createDispatchMap() {
        Map<Class<T>, Observable<ManualInput>> observableDispatch = 
            Collections.synchronizedMap(new HashMap<Class<T>, Observable<ManualInput>>());
        
        publisher = PublishSubject.create();
        
        observableDispatch.put((Class<T>)Map.class, factory.createMultiMapFunc(publisher));
        observableDispatch.put((Class<T>)ManualInput.class, factory.createManualInputFunc(publisher));
        observableDispatch.put((Class<T>)String[].class, factory.createEncoderFunc(publisher));
        observableDispatch.put((Class<T>)int[].class, factory.createVectorFunc(publisher));
        
        return observableDispatch;
    }
    
    /**
     * If this Layer has a Sensor, map its encoder's buckets
     * 
     * @param sequence
     * @return
     */
    private Observable<ManualInput> mapEncoderBuckets(Observable<ManualInput> sequence) {
        if(hasSensor()) {
            if(getSensor().getMetaInfo().getFieldTypes().stream().anyMatch(
               ft -> { return ft == FieldMetaType.SARR || ft == FieldMetaType.DARR; })) {
                if(autoCreateClassifiers) {
                    throw new IllegalStateException("Cannot autoclassify with raw array input... Remove auto classify setting.");
                }
               return sequence; 
            }
            sequence = sequence.map(m -> {
                doEncoderBucketMapping(m, getSensor().getInputMap());
                return m; 
            });
        }
        
        return sequence;
    }
    
    /**
     * This method is necessary to be able to retrieve the mapped {@link Observable}
     * types to input types or their subclasses if any.
     * 
     * @param t     the input type. The "expected" types are:
     *              <ul><li>{@link Map}</li><li>{@link ManualInput}</li><li>String[]</li>
     *              <li>int[]</li></ul> or their subclasses.
     * @return      an Observable suitable for subscribing or operating on.
     */
    private Observable<ManualInput> resolveObservableSequence(T t) {
        Observable<ManualInput> sequenceStart = null;
        
        if(observableDispatch != null) {
            if(ManualInput.class.isAssignableFrom(t.getClass())) {
                sequenceStart = observableDispatch.get(ManualInput.class);
            }else if(Map.class.isAssignableFrom(t.getClass())) {
                sequenceStart = observableDispatch.get(Map.class);
            }else if(t.getClass().isArray()) {
                if(t.getClass().equals(String[].class)) {
                    sequenceStart = observableDispatch.get(String[].class);
                }else if(t.getClass().equals(int[].class)) {
                    sequenceStart = observableDispatch.get(int[].class);
                }
            }
        }
        
        return sequenceStart;
    }
    
    /**
     * Stores a {@link NamedTuple} which contains the input values and
     * bucket information - keyed to the encoded field name so that a 
     * classifier can retrieve it later on in the processing sequence.
     * 
     * @param inference
     * @param encoderInputMap
     */
    private void doEncoderBucketMapping(Inference inference, Map<String, Object> encoderInputMap) {
        if(encoderTuples == null) {
            encoderTuples = encoder.getEncoders(encoder);
        }
        
        // Store the encoding
        int[] encoding = inference.getEncoding();

        for(EncoderTuple t : encoderTuples) {
            String name = t.getName();
            Encoder<?> e = t.getEncoder();
            
            int bucketIdx = -1;
            Object o = encoderInputMap.get(name);
            if(DateTime.class.isAssignableFrom(o.getClass())) {
                bucketIdx = ((DateEncoder)e).getBucketIndices((DateTime)o)[0];
            }else if(Number.class.isAssignableFrom(o.getClass())) {
                bucketIdx = e.getBucketIndices((double)o)[0];
            }else{
                bucketIdx = e.getBucketIndices((String)o)[0];
            }

            int offset = t.getOffset();
            int[] tempArray = new int[e.getWidth()];
            System.arraycopy(encoding, offset, tempArray, 0, tempArray.length);

            inference.getClassifierInput().put(name, 
                new NamedTuple(new String[] { "name", "inputValue", "bucketIdx", "encoding" }, 
                    name, o, bucketIdx, tempArray));
        }
    }
    
    /**
     * Connects the {@link Transformer} to the rest of the {@link Observable} sequence.
     * 
     * @param o     the Transformer part of the sequence.
     * @return      the completed {@link Observable} sequence.
     */
    private Observable<ManualInput> fillInSequence(Observable<ManualInput> o) {
        // Route to ordered dispatching if required.
        if(hasGenericProcess) {
            return fillInOrderedSequence(o);
        }
        
        // Spatial Pooler config
        if(spatialPooler != null) {
            Integer skipCount = 0;
            if((skipCount = ((Integer)params.getParameterByKey(KEY.SP_PRIMER_DELAY))) != null) {
                o = o.map(factory.createSpatialFunc(spatialPooler)).skip(skipCount.intValue());
            }else{
                o = o.map(factory.createSpatialFunc(spatialPooler));
            }
        }

        // Temporal Memory config
        if(temporalMemory != null) {
            o = o.map(factory.createTemporalFunc(temporalMemory));
        }

        // Classifier config
        if(autoCreateClassifiers != null && autoCreateClassifiers.booleanValue()) {
            o = o.map(factory.createClassifierFunc());
        }

        // Anomaly config
        if(anomalyComputer != null) {
            o = o.map(factory.createAnomalyFunc(anomalyComputer));
        }

        return o;
    }
    
    /**
     * Connects {@link Observable} or {@link Transformer} emissions in the order
     * they are declared.
     * 
     * @param o     first {@link Observable} in sequence.
     * @return
     */
    @SuppressWarnings("unchecked")
    private Observable<ManualInput> fillInOrderedSequence(Observable<ManualInput> o) {
        for(Object node : addedItems) {
            if(node instanceof Func1<?,?>) {
                o = o.map((Func1<ManualInput,ManualInput>)node);
            }else if(node instanceof SpatialPooler) {
                Integer skipCount = 0;
                if((skipCount = ((Integer)params.getParameterByKey(KEY.SP_PRIMER_DELAY))) != null) {
                    o = o.map(factory.createSpatialFunc(spatialPooler)).skip(skipCount.intValue());
                }else{
                    o = o.map(factory.createSpatialFunc(spatialPooler));
                }
            }else if(node instanceof TemporalMemory) {
                o = o.map(factory.createTemporalFunc(temporalMemory));
            }
        }
        
        // Classifier config
        if(autoCreateClassifiers != null && autoCreateClassifiers.booleanValue()) {
            o = o.map(factory.createClassifierFunc());
        }

        // Anomaly config
        if(anomalyComputer != null) {
            o = o.map(factory.createAnomalyFunc(anomalyComputer));
        }
        
        return o;
    }
    
    /**
     * Called internally to create a subscription on behalf 
     * of the specified {@link LayerObserver}
     * 
     * @param   sub     the LayerObserver (subscriber).
     * @return
     */
    private Subscription createSubscription(final Observer<Inference> sub) {
        return new Subscription() {
            private Observer<Inference> observer = sub;
            @Override
            public void unsubscribe() {
                subscribers.remove(observer);
                if(subscribers.isEmpty()) {
                    subscription.unsubscribe();
                }
            }

            @Override
            public boolean isUnsubscribed() {
                return subscribers.contains(observer);
            }
        };
    }

    /**
     * Returns the {@link PublishSubject}'s subscriber which delegates
     * to all the {@link Layer}'s subscribers.
     * 
     * @return
     */
    private Observer<Inference> getDelegateSubscriber() {
        return new Observer<Inference>() {
            @Override public void onCompleted() {
                for(Observer<Inference> o : subscribers) {
                    o.onCompleted();
                }
            }
            @Override public void onError(Throwable e) { 
                for(Observer<Inference> o : subscribers) {
                    o.onError(e);
                }
            }
            @Override public void onNext(Inference i) {
                currentInference = i;
                for(Observer<Inference> o : subscribers) {
                    o.onNext(i);
                }
            }
        };
    }

    /**
     * Returns the {@link PublishSubject}'s subscriber which delegates
     * to all the {@link Layer}'s subscribers.
     * 
     * @return
     */
    private Observer<Inference> getDelegateObserver() {
        return new Observer<Inference>() {
            @Override public void onCompleted() {
                for(Observer<Inference> o : observers) {
                    o.onCompleted();
                }
            }
            @Override public void onError(Throwable e) { 
                for(Observer<Inference> o : observers) {
                    o.onError(e);
                    e.printStackTrace();
                }
            }
            @Override public void onNext(Inference i) {
                currentInference = i;
                for(Observer<Inference> o : observers) {
                    o.onNext(i);
                }
            }
        };
    }


    /**
     * Creates the {@link NamedTuple} of names to encoders used in the 
     * observable sequence.
     * 
     * @param encoder
     * @return
     */
    NamedTuple makeClassifiers(MultiEncoder encoder) {
        String[] names = new String[encoder.getEncoders(encoder).size()];
        CLAClassifier[] ca = new CLAClassifier[names.length];
        int i = 0;
        for(EncoderTuple et : encoder.getEncoders(encoder)) {
            names[i] = et.getName();
            ca[i] = new CLAClassifier();
            i++;
        }
        return new NamedTuple(names,(Object[])ca);
    }
    /**
     * Called internally to invoke the {@link SpatialPooler}
     * 
     * @param input
     * @return
     */
    private int[] spatialInput(int[] input) {
        if(input == null) {
            LOGGER.info("Layer ".concat(getName()).concat(" received null input"));
        }else if(input.length < 1) {
            LOGGER.info("Layer ".concat(getName()).concat(" received zero length bit vector"));
            return input;
        }
        spatialPooler.compute(
            connections, input, activeColumns, 
                sensor == null || sensor.getMetaInfo().isLearn(), isLearn);
        return activeColumns;
    }

    /**
     * Called internally to invoke the {@link TemporalMemory}
     * 
     * @param input
     * @return
     */
    private int[] temporalInput(int[] input) {
        ComputeCycle cc = null;
        if(sensor != null) {
            if(sensor.getMetaInfo().isReset()) {
                temporalMemory.reset(connections);
            }
            
            cc = temporalMemory.compute(connections, input, sensor.getMetaInfo().isLearn());
        } else {
            cc = temporalMemory.compute(connections, input, isLearn);
        }
        
        previousPrediction = currentPrediction;
        return currentPrediction = getSDR(cc.predictiveCells());
    }

    /**
     * Converts a {@link Set} of {@link Cell}s to a sparse array
     * 
     * @param cells
     * @return
     */
    private int[] getSDR(Set<Cell> cells) {
        int[] retVal = new int[cells.size()];
        int i = 0;
        for(Iterator<Cell> it = cells.iterator();i < retVal.length;i++) {
            retVal[i] = it.next().getIndex();
            retVal[i] /= cellsPerColumn; // Get the column index
        }
        Arrays.sort(retVal);
        retVal = ArrayUtils.unique(retVal);

        return retVal;
    }

    //////////////////////////////////////////////////////////////
    //        Inner Class Definition Transformer Example        //
    //////////////////////////////////////////////////////////////
    /**
     * Factory which returns an {@link Observable} capable of transforming
     * known input formats to the universal format object passed between
     * all Observables in the Observable chains making up the processing
     * steps involving HTM algorithms.
     * 
     * The {@link Transformer} implementations are used to transform various
     * inputs into a {@link ManualInput}, and thus are used at the beginning
     * of any Observable chain; each succeding Observable in a given chain would
     * then communicate via passed ManualInputs or {@link Inference}s (which are
     * the same thing).
     * 
     * @author David Ray
     * @see Layer#completeDispatch(Object)
     * @see Layer#resolveObservableSequence(Object)
     * @see Layer#fillInSequence(Observable)
     */
    class FunctionFactory {
        ManualInput inference = new ManualInput();
        
        //////////////////////////////////////////////////////////////////////////////
        //                             TRANSFORMERS                                 //
        //////////////////////////////////////////////////////////////////////////////
        /**
         * WARNING: UNIMPLEMENTED
         * 
         * <p>
         * Emits an {@link Observable} which is transformed from a String[] of csv input
         * to one that emits {@link Inference}s. 
         * </p><p>
         * This class currently lacks the implementation of csv parsing into distinct Object
         * types - which is necessary to compose a "multi-map" necessary to input data into
         * the {@link MultiEncoder} necessary to encode multiple field inputs.
         * </p>
         * <p> TODO: Implement later</p>
         */
        class String2Inference implements Transformer<String[], ManualInput> {
            @Override
            public Observable<ManualInput> call(Observable<String[]> t1) {
                return t1.map(new Func1<String[], ManualInput>() {
                    @Override
                    public ManualInput call(String[] t1) {

                        /////////////////////  Do transformative work here /////////////////////
                        //    In "real life", this will send data through the MultiEncoder    //
                        //    Below is simply a faked out place holder...                     //
                        ////////////////////////////////////////////////////////////////////////
                        int[] sdr = new int[t1.length];
                        for(int i = 0;i < sdr.length;i++) {
                            sdr[i] = Integer.parseInt(t1[i]);
                        }

                        return inference.sdr(sdr).layerInput(sdr);
                    }
                });
            }
        }

        /**
         * <p>
         * Emits an {@link Observable} which is transformed from a Map input
         * type to one that emits {@link Inference}s. 
         * </p><p>
         * This {@link Transformer} is used when the input to a given {@link Layer}
         * is a map of fields to input Objects. It is typically used when a Layer is
         * configured with a {@link MultiEncoder} (which is the only encoder type that
         * may be contained within a Layer, because it can be used to hold any combination
         * of encoders required.).
         * </p> 
         * 
         */
        @SuppressWarnings("rawtypes")
        class Map2Inference implements Transformer<Map, ManualInput> {
            @Override
            public Observable<ManualInput> call(Observable<Map> t1) {
                return t1.map(new Func1<Map, ManualInput>() {
                    
                    @SuppressWarnings("unchecked")
                    @Override
                    public ManualInput call(Map t1) {
                        if(encoderTuples == null) {
                            encoderTuples = encoder.getEncoders(encoder);
                        }
                        
                        // Store the encoding
                        int[] encoding = encoder.encode(t1);
                        inference.sdr(encoding).encoding(encoding);
                        
                        doEncoderBucketMapping(inference, t1);

                        return inference.layerInput(t1);
                    }
                });
            }
        }

        /**
         * <p>
         * Emits an {@link Observable} which is transformed from a binary vector input
         * type to one that emits {@link Inference}s.
         * </p><p>
         * This type is used when bypassing an encoder and possibly any other algorithm
         * usually connected in a sequence of algorithms within a {@link Layer}
         * </p>
         */
        class Vector2Inference implements Transformer<int[], ManualInput> {
            @Override
            public Observable<ManualInput> call(Observable<int[]> t1) {
                return t1.map(new Func1<int[], ManualInput>() {
                    @Override
                    public ManualInput call(int[] t1) {
                        // Indicates a value that skips the encoding step
                        return inference.sdr(t1).layerInput(t1);
                    }
                });
            }
        }

        /**
         * Emits an {@link Observable} which copies an Inference input to the 
         * output, storing relevant information in this layer's inference object
         * along the way.
         */
        class Copy2Inference implements Transformer<ManualInput, ManualInput> {
            @Override
            public Observable<ManualInput> call(Observable<ManualInput> t1) {
                return t1.map(new Func1<ManualInput, ManualInput>() {
                    NamedTuple swap;
                    boolean swapped;
                    @Override
                    public ManualInput call(ManualInput t1) {
                        // Inference is shared along entire network
                        if(!swapped) {
                            swap = inference.getClassifiers();
                            inference = t1;
                            inference.classifiers(swap);
                            swapped = true;
                        }
                        // Indicates a value that skips the encoding step
                        return inference.sdr(t1.getSDR()).recordNum(t1.getRecordNum()).layerInput(t1);
                    }
                });
            }
        }

        //////////////////////////////////////////////////////////////////////////////
        //                     INPUT TRANSFORMATION FUNCTIONS                       //
        //////////////////////////////////////////////////////////////////////////////
        public Observable<ManualInput> createEncoderFunc(Observable<T> in) {
            return in.ofType(String[].class).compose(new String2Inference());
        }

        public Observable<ManualInput> createMultiMapFunc(Observable<T> in) {
            return in.ofType(Map.class).compose(new Map2Inference());
        }

        public Observable<ManualInput> createVectorFunc(Observable<T> in) {
            return in.ofType(int[].class).compose(new Vector2Inference());
        }

        public Observable<ManualInput> createManualInputFunc(Observable<T> in) {
            return in.ofType(ManualInput.class).compose(new Copy2Inference());
        }


        //////////////////////////////////////////////////////////////////////////////
        //                OBSERVABLE COMPONENT CREATION METHODS                     //
        //////////////////////////////////////////////////////////////////////////////
        public Func1<ManualInput, ManualInput> createSpatialFunc(final SpatialPooler sp) {
            return new Func1<ManualInput, ManualInput>() {
                int inputWidth = -1;
                @Override
                public ManualInput call(ManualInput t1) {
                    if(t1.getSDR().length > 0 && ArrayUtils.isSparse(t1.getSDR())) {
                        if(inputWidth == -1) {
                            inputWidth = connections.getInputMatrix().getMaxIndex() + 1;
                        }
                       t1.sdr(ArrayUtils.asDense(t1.getSDR(), inputWidth));
                    }
                    return t1.sdr(spatialInput(t1.getSDR())).activeColumns(t1.getSDR());
                }
            };
        }

        public Func1<ManualInput, ManualInput> createTemporalFunc(final TemporalMemory tm) {
            return new Func1<ManualInput, ManualInput>() {
                @Override
                public ManualInput call(ManualInput t1) {
                    if(!ArrayUtils.isSparse(t1.getSDR())) {
                        // Set on Layer, then set sparse actives as the sdr, then set on Manual Input (t1) 
                        t1 = t1.sdr(
                           sparseActives(ArrayUtils.where(t1.getSDR(), ArrayUtils.WHERE_1)))
                               .sparseActives(t1.getSDR());
                    }
                    return t1.sdr(temporalInput(t1.getSDR())).predictedColumns(t1.getSDR());
                }
            };
        }

        public Func1<ManualInput, ManualInput> createClassifierFunc() {
            return new Func1<ManualInput, ManualInput>() {
                private Object bucketIdx;
                private Object actValue;
                Map<String, Object> inputMap = new HashMap<String, Object>() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public Object get(Object o) {
                        return o.equals("bucketIdx") ? bucketIdx : actValue;
                    }
                };

                @Override
                public ManualInput call(ManualInput t1) {
                    Map<String, NamedTuple> ci = t1.getClassifierInput();
                    int recordNum = getRecordNum();
                    for(String key : ci.keySet()) {
                        NamedTuple inputs = ci.get(key);
                        bucketIdx = inputs.get("bucketIdx");
                        actValue = inputs.get("inputValue");
                        
                        CLAClassifier c = (CLAClassifier)t1.getClassifiers().get(key);
                        ClassifierResult<Object> result = c.compute(
                            recordNum, inputMap, t1.getSDR(), isLearn, true);

                        t1.recordNum(recordNum).storeClassification((String)inputs.get("name"), result);
                    }

                    return t1;
                }
            };
        }

        public Func1<ManualInput, ManualInput> createAnomalyFunc(final Anomaly an) {
            return new Func1<ManualInput, ManualInput>() {
                @Override
                public ManualInput call(ManualInput t1) {
                    if(t1.getSparseActives() == null || t1.getPreviousPrediction() == null) {
                        return t1.anomalyScore(1.0);
                    }
                    return t1.anomalyScore(
                        anomalyComputer.compute(
                            t1.getSparseActives(), t1.getPreviousPrediction(), 0, 0));
                }
            };
        }
        
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((parentRegion == null) ? 0 : parentRegion.hashCode());
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
        Layer<?> other = (Layer<?>)obj;
        if(name == null) {
            if(other.name != null)
                return false;
        } else if(!name.equals(other.name))
            return false;
        if(parentRegion == null) {
            if(other.parentRegion != null)
                return false;
        } else if(!parentRegion.equals(other.parentRegion))
            return false;
        return true;
    }
}
