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

import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joda.time.DateTime;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Classification;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Classifier;
import org.numenta.nupic.algorithms.SDRClassifier;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.EncoderTuple;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.model.SDR;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.network.sensor.URISensor;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.NamedTuple;
import org.numenta.nupic.util.SparseBinaryMatrix;
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
 * Implementation of the biological layer of a region in the neocortex. Here, a
 * {@code Layer} contains the physical structure (columns, cells, dendrites etc)
 * shared by a sequence of algorithms which serve to implement the predictive
 * inferencing present in this, the allegory to its biological equivalent.
 * </p>
 * <p>
 * <b>COMPOSITION:</b> A Layer is constructed with {@link Parameters} which
 * configure the behavior of most of the key algorithms a Layer may contain. It
 * is also <em>optionally</em> constructed with each of the algorithms in turn.
 * A Layer that includes an {@link Encoder} is always initially configured with
 * a {@link MultiEncoder}. The child encoders contained within the MultiEncoder
 * are configured from the Map included with the specified Parameters, keyed by
 * {@link Parameters.KEY#FIELD_ENCODING_MAP}.
 * </p>
 * <p>
 * A field encoding map consists of one map for each of the fields to be
 * encoded. Each individual map in the field encoding map contains the typical
 * {@link Encoder} parameters, plus a few "meta" parameters needed to describe
 * the field and its data type as follows:
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
 * 
 * For an example of how to create the field encodings map in a reusable way,
 * see NetworkTestHarness and its usage within the LayerTest class.
 * 
 * <p>
 * The following is an example of Layer construction with everything included
 * (i.e. Sensor, SpatialPooler, TemporalMemory, CLAClassifier, Anomaly
 * (computer))
 * 
 * <pre>
 * // See the test harness for more information
 * Parameters p = NetworkTestHarness.getParameters();
 * 
 * // How to merge (union) two {@link Parameters} objects. This one merges
 * // the Encoder parameters into default parameters.
 * p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
 * 
 * // You can overwrite parameters as needed like this
 * p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
 * p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
 * p.setParameterByKey(KEY.POTENTIAL_RADIUS, 200);
 * p.setParameterByKey(KEY.INHIBITION_RADIUS, 50);
 * p.setParameterByKey(KEY.GLOBAL_INHIBITIONS, true);
 * 
 * Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
 * params.put(KEY_MODE, Mode.PURE);
 * params.put(KEY_WINDOW_SIZE, 3);
 * params.put(KEY_USE_MOVING_AVG, true);
 * Anomaly anomalyComputer = Anomaly.create(params);
 * 
 * Layer&lt;?&gt; l = Network.createLayer(&quot;TestLayer&quot;, p).alterParameter(KEY.AUTO_CLASSIFY, true).add(anomalyComputer).add(new TemporalMemory()).add(new SpatialPooler())
 *                 .add(Sensor.create(FileSensor::create, SensorParams.create(Keys::path, &quot;&quot;, ResourceLocator.path(&quot;rec-center-hourly-small.csv&quot;))));
 * </pre>
 * 
 * 
 * 
 * 
 * @author David Ray
 */
public class Layer<T> implements Persistable {
    private static final long serialVersionUID = 1L;

    protected static final Logger LOGGER = LoggerFactory.getLogger(Layer.class);
    
    protected int numColumns;

    protected Network parentNetwork;
    protected Region parentRegion;

    protected Parameters params;
    protected SensorParams sensorParams;
    protected Connections connections;
    protected HTMSensor<?> sensor;
    protected MultiEncoder encoder;
    protected SpatialPooler spatialPooler;
    protected TemporalMemory temporalMemory;
    private Boolean autoCreateClassifiers;
    private Anomaly anomalyComputer;

    private transient ConcurrentLinkedQueue<Observer<Inference>> subscribers = new ConcurrentLinkedQueue<Observer<Inference>>();
    private transient PublishSubject<T> publisher = null;
    private transient Observable<Inference> userObservable;
    private transient Subscription subscription;
    
    private volatile Inference currentInference;

    FunctionFactory factory;

    /** Used to track and document the # of records processed */
    private int recordNum = -1;
    /** Keeps track of number of records to skip on restart */
    private int skip = -1;

    private String name;

    private volatile boolean isClosed;
    private volatile boolean isHalted;
    private volatile boolean isPostSerialized;
    protected volatile boolean isLearn = true;
    
    private Layer<Inference> next;
    private Layer<Inference> previous;

    private transient List<Observer<Inference>> observers = new ArrayList<Observer<Inference>>();
    private transient CheckPointOperator<?> checkPointOp;
    private transient List<Observer<byte[]>> checkPointOpObservers = new ArrayList<>();
    
    
    
    /**
     * Retains the order of added items - for use with interposed
     * {@link Observable}
     */
    private List<Object> addedItems = new ArrayList<>();
    /** Indicates whether there is a generic processing node entered */
    private boolean hasGenericProcess;

    /**
     * List of {@link Encoder}s used when storing bucket information see
     * {@link #doEncoderBucketMapping(Inference, Map)}
     */
    private List<EncoderTuple> encoderTuples;

    private transient Map<Class<T>, Observable<ManualInput>> observableDispatch = Collections.synchronizedMap(new HashMap<Class<T>, Observable<ManualInput>>());

    /** This layer's thread */
    private transient Thread LAYER_THREAD;

    static final byte SPATIAL_POOLER = 1;
    static final byte TEMPORAL_MEMORY = 2;
    static final byte CLA_CLASSIFIER = 4;
    static final byte ANOMALY_COMPUTER = 8;

    private byte algo_content_mask = 0;
    
    

    /**
     * Creates a new {@code Layer} using the {@link Network} level
     * {@link Parameters}
     * 
     * @param n
     *            the parent {@link Network}
     */
    public Layer(Network n) {
        this(n, n.getParameters());
    }

    /**
     * Creates a new {@code Layer} using the specified {@link Parameters}
     * 
     * @param n
     *            the parent {@link Network}
     * @param p
     *            the {@link Parameters} to use with this {@code Layer}
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

        this.autoCreateClassifiers = (Boolean)p.get(KEY.AUTO_CLASSIFY);

        factory = new FunctionFactory();

        observableDispatch = createDispatchMap();
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Layer<T> preSerialize() {
        isPostSerialized = false;
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Layer<T> postDeSerialize() {
        recreateSensors();
        
        FunctionFactory old = factory;
        factory = new FunctionFactory();
        factory.inference = old.inference.postDeSerialize(old.inference);
        
        checkPointOpObservers = new ArrayList<>();
        
        if(sensor != null) {
            sensor.setLocalParameters(params);
            // Initialize encoders and recreate encoding index mapping.
            sensor.postDeSerialize();
        }else{
            // Dispatch functions (Observables) are transient & non-serializable so they must be rebuilt.
            observableDispatch = createDispatchMap();
            // Dispatch chain will not propagate unless it has subscribers.
            parentNetwork.addDummySubscriber();
        }
        // Flag which lets us know to skip or do certain setups during initialization.
        isPostSerialized = true;
        
        observers = new ArrayList<Observer<Inference>>();
        
        return this;
    }
    
    /**
     * Sets the parent {@link Network} on this {@code Layer}
     * 
     * @param network
     */
    public void setNetwork(Network network) {
        this.parentNetwork = network;
    }
    
    /**
     * Returns the parent {@link Network}
     * @return  the parent Network;
     */
    public Network getNetwork() {
        return this.parentNetwork;
    }

    /**
     * Creates a new {@code Layer} initialized with the specified algorithmic
     * components.
     * 
     * @param params                    A {@link Parameters} object containing configurations for a
     *                                  SpatialPooler, TemporalMemory, and Encoder (all or none may be used).
     * @param e                         (optional) The Network API only uses a {@link MultiEncoder} at
     *                                  the top level because of its ability to delegate to child encoders.
     * @param sp                        (optional) {@link SpatialPooler}
     * @param tm                        (optional) {@link TemporalMemory}
     * @param autoCreateClassifiers     (optional) Indicates that the {@link Parameters} object
     *                                  contains the configurations necessary to create the required encoders.
     * @param a                         (optional) An {@link Anomaly} computer.
     */
    public Layer(Parameters params, MultiEncoder e, SpatialPooler sp, TemporalMemory tm, Boolean autoCreateClassifiers, Anomaly a) {

        // Make sure we have a valid parameters object
        if(params == null) {
            throw new IllegalArgumentException("No parameters specified.");
        }

        // Check to see if the Parameters include the encoder configuration.
        if(params.get(KEY.FIELD_ENCODING_MAP) == null && e != null) {
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
                (autoCreateClassifiers == null ? "" : "Auto creating Classifiers for each input field."),
                (anomalyComputer == null ? "" : "Anomaly"));
        }
    }
    
    /**
     * USED INTERNALLY, DO NOT CALL.
     * @return
     */
    public CheckPointOp<byte[]> delegateCheckPointCall() {
        if(parentNetwork != null) {
            return parentNetwork.getCheckPointOperator();
        }
        return null;
    }
    
    /**
     * Sets the parent region which contains this {@code Layer}
     * 
     * @param r
     */
    public void setRegion(Region r) {
        this.parentRegion = r;
    }
    
    /**
     * Returns the parent {@link Region}
     * 
     * @return  the parent Region
     */
    public Region getRegion() {
        return this.parentRegion;
    }

    /**
     * Finalizes the initialization in one method call so that side effect
     * operations to share objects and other special initialization tasks can
     * happen all at once in a central place for maintenance ease.
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
                
                Object supplier;
                if((supplier = sensor.getSensorParams().get("ONSUB")) != null) {
                    if(supplier instanceof PublisherSupplier) {
                        ((PublisherSupplier)supplier).setNetwork(parentNetwork);
                        parentNetwork.setPublisher(((PublisherSupplier)supplier).get());
                    }
                }
            }
        }

        // Create Encoder hierarchy from definitions & auto create classifiers
        // if specified
        if(encoder != null) {
            if(encoder.getEncoders(encoder) == null || encoder.getEncoders(encoder).size() < 1) {
                if(params.get(KEY.FIELD_ENCODING_MAP) == null || ((Map<String, Map<String, Object>>)params.get(KEY.FIELD_ENCODING_MAP)).size() < 1) {
                    LOGGER.error("No field encoding map found for specified MultiEncoder");
                    throw new IllegalStateException("No field encoding map found for specified MultiEncoder");
                }

                encoder.addMultipleEncoders((Map<String, Map<String, Object>>)params.get(KEY.FIELD_ENCODING_MAP));
            }

            // Make the declared column dimensions match the actual input
            // dimensions retrieved from the encoder
            int product = 0, inputLength = 0, columnLength = 0;
            if(((inputLength = ((int[])params.get(KEY.INPUT_DIMENSIONS)).length) != (columnLength = ((int[])params.get(KEY.COLUMN_DIMENSIONS)).length))
                            || encoder.getWidth() != (product = ArrayUtils.product((int[])params.get(KEY.INPUT_DIMENSIONS)))) {

                LOGGER.warn("The number of Input Dimensions (" + inputLength + ") != number of Column Dimensions " + "(" + columnLength + ") --OR-- Encoder width (" + encoder.getWidth()
                                + ") != product of dimensions (" + product + ") -- now attempting to fix it.");

                int[] inferredDims = inferInputDimensions(encoder.getWidth(), columnLength);
                if(inferredDims != null && inferredDims.length > 0 && encoder.getWidth() == ArrayUtils.product(inferredDims)) {
                    LOGGER.info("Input dimension fix successful!");
                    LOGGER.info("Using calculated input dimensions: " + Arrays.toString(inferredDims));
                }

                params.setInputDimensions(inferredDims);
                connections.setInputDimensions(inferredDims);
            }
        }

        autoCreateClassifiers = autoCreateClassifiers != null && (autoCreateClassifiers | (Boolean)params.get(KEY.AUTO_CLASSIFY));

        if(autoCreateClassifiers != null && autoCreateClassifiers.booleanValue() && (factory.inference.getClassifiers() == null || factory.inference.getClassifiers().size() < 1)) {
            factory.inference.classifiers(makeClassifiers(encoder == null ? parentNetwork.getEncoder() : encoder));

            // Note classifier addition by setting content mask
            algo_content_mask |= CLA_CLASSIFIER;
        }

        // We must adjust this Layer's inputDimensions to the size of the input
        // received from the previous Region's output vector.
        if(parentRegion != null && parentRegion.getUpstreamRegion() != null) {
            int[] upstreamDims = new int[] { calculateInputWidth() };
            params.setInputDimensions(upstreamDims);
            connections.setInputDimensions(upstreamDims);
        } else if(parentRegion != null && parentNetwork != null
                && parentRegion.equals(parentNetwork.getSensorRegion()) && encoder == null && spatialPooler != null) {
            Layer<?> curr = this;
            while((curr = curr.getPrevious()) != null) {
                if(curr.getEncoder() != null) {
                    int[] dims = (int[])curr.getParameters().get(KEY.INPUT_DIMENSIONS);
                    params.setInputDimensions(dims);
                    connections.setInputDimensions(dims);
                }
            }
        }

        // Let the SpatialPooler initialize the matrix with its requirements
        if(spatialPooler != null) {
            // The exact dimensions don't have to be the same but the number of
            // dimensions do!
            int inputLength, columnLength = 0;
            if((inputLength = ((int[])params.get(KEY.INPUT_DIMENSIONS)).length) != 
                (columnLength = ((int[])params.get(KEY.COLUMN_DIMENSIONS)).length)) {
                
                LOGGER.error("The number of Input Dimensions (" + inputLength + ") is not same as the number of Column Dimensions " + 
                    "(" + columnLength + ") in Parameters! - SpatialPooler not initialized!");
                
                return this;
            }
            spatialPooler.init(connections);
        }

        // Let the TemporalMemory initialize the matrix with its requirements
        if(temporalMemory != null) {
            TemporalMemory.init(connections);
        }
        
        this.numColumns = connections.getNumColumns();

        this.isClosed = true;

        LOGGER.debug("Layer " + name + " content initialize mask = " + Integer.toBinaryString(algo_content_mask));

        return this;
    }
    
    /**
     * Called from {@link FunctionFactory#createSpatialFunc(SpatialPooler)} and from {@link #close()}
     * to calculate the size of the input vector given the output source either being a {@link TemporalMemory}
     * or a {@link SpatialPooler} - from this {@link Region} or a previous {@link Region}.
     * 
     * @return  the length of the input vector
     */
    int calculateInputWidth() {
        // If no previous Layer, check upstream region for its output layer's output.
        if(previous == null) {
            if(parentRegion.getUpstreamRegion() != null) {
                // Upstream region with TM
                if((parentRegion.getUpstreamRegion().getHead().algo_content_mask & Layer.TEMPORAL_MEMORY) == Layer.TEMPORAL_MEMORY) {
                    int out = -1;
                    out = (parentRegion.getUpstreamRegion().getHead().getConnections().getCellsPerColumn() *
                        (parentRegion.getUpstreamRegion().getHead().getConnections().getMemory().getMaxIndex() + 1));
                    
                    return out;
                }
                // Upstream region but no TM, so input is the upstream region's SP
                
                return new SparseBinaryMatrix(parentRegion.getUpstreamRegion().getHead().getConnections().getColumnDimensions()).getMaxIndex() + 1;
            }
            // No previous Layer, and no upstream region
            // layer contains a TM so compute by cells;
            if(hasTM() && !hasSP()) {
                return getConnections().getCellsPerColumn() * (getConnections().getMemory().getMaxIndex() + 1); 
            }
            // layer only contains a SP
            return connections.getNumInputs();
        }else{
            // There is a previous Layer and that layer contains a TM so compute by cells;
            if((previous.algo_content_mask & Layer.TEMPORAL_MEMORY) == Layer.TEMPORAL_MEMORY) {
                SparseBinaryMatrix matrix = new SparseBinaryMatrix(previous.getConnections().getColumnDimensions());
                return previous.getConnections().getCellsPerColumn() * (matrix.getMaxIndex() + 1); 
            }
            // Previous Layer but it has no TM so use the previous' column output (from SP)
            return new SparseBinaryMatrix(previous.getConnections().getColumnDimensions()).getMaxIndex() + 1;
        }
    }

    /**
     * For internal use only. Returns a flag indicating whether this {@link Layer}
     * contains a {@link TemporalMemory}
     * @return
     */
    boolean hasTM() {
        return (algo_content_mask & Layer.TEMPORAL_MEMORY) == Layer.TEMPORAL_MEMORY;
    }
    
    /**
     * For internal use only. Returns a flag indicating whether this {@link Layer}
     * contains a {@link SpatialPooler}
     * @return
     */
    boolean hasSP() {
        return (algo_content_mask & Layer.SPATIAL_POOLER) == Layer.SPATIAL_POOLER;
    }

    /**
     * Given an input field width and Spatial Pooler dimensionality; this method
     * will return an array of dimension sizes whose number is equal to the
     * number of column dimensions. The sum of the returned dimensions will be
     * equal to the flat input field width specified.
     * 
     * This method should be called when a disparity in dimensionality between
     * the input field and the number of column dimensions is detected.
     * Otherwise if the input field dimensionality is correctly specified, this
     * method should <b>not</b> be used.
     * 
     * @param inputWidth        the flat input width of an {@link Encoder}'s output or the
     *                          vector used as input to the {@link SpatialPooler}
     * @param numDims           a number specifying the number of dimensions that
     *                          should be returned.
     * @return
     */
    public int[] inferInputDimensions(int inputWidth, int numDims) {
        double flatSize = inputWidth;
        double numColDims = numDims;
        int[] retVal = new int[(int)numColDims];
        
        BigDecimal log = new BigDecimal(Math.log10(flatSize));
        BigDecimal dimensions = new BigDecimal(numColDims);
        double sliceArrangement = new BigDecimal(
            Math.pow(10, log.divide(dimensions).doubleValue()), 
                MathContext.DECIMAL32).doubleValue();
        double remainder = sliceArrangement % (int)sliceArrangement;
        
        if(remainder > 0) {
            for(int i = 0;i < numColDims - 1;i++)
                retVal[i] = 1;
            retVal[(int)numColDims - 1] = (int)flatSize;
        } else {
            for(int i = 0;i < numColDims;i++)
                retVal[i] = (int)sliceArrangement;
        }

        return retVal;
    }

    /**
     * Returns an {@link Observable} that can be subscribed to, or otherwise
     * operated upon by another Observable or by an Observable chain.
     * 
     * @return  this {@code Layer}'s output {@link Observable}
     */
    @SuppressWarnings("unchecked")
    public Observable<Inference> observe() {
        // This will be called again after the Network is halted so we have to prepare
        // for rebuild of the Observer chain
        if(isHalted) {
            clearSubscriberObserverLists();
        }
        
        if(userObservable == null) {
            userObservable = Observable.create(new Observable.OnSubscribe<Inference>() {
                @Override
                public void call(Subscriber<? super Inference> t1) {
                    if(observers == null) {
                        observers = new ArrayList<Observer<Inference>>();
                    }
                    observers.add((Observer<Inference>)t1);
                }
            });
        }

        return userObservable;
    }

    /**
     * Called by the {@code Layer} client to receive output {@link Inference}s
     * from the configured algorithms.
     * 
     * @param subscriber    a {@link Subscriber} to be notified as data is published.
     * @return  a {@link Subscription}
     */
    public Subscription subscribe(final Observer<Inference> subscriber) {
        // This will be called again after the Network is halted so we have to prepare
        // for rebuild of the Observer chain
        if(isHalted) {
            clearSubscriberObserverLists();
        }
        
        if(subscriber == null) {
            throw new IllegalArgumentException("Subscriber cannot be null.");
        }

        if(subscribers == null) {
            subscribers = new ConcurrentLinkedQueue<Observer<Inference>>();
        }
        subscribers.add(subscriber);
        
        return createSubscription(subscriber);
    }
    
    /**
     * Allows the user to define the {@link Connections} object data structure
     * to use. Or possibly to share connections between two {@code Layer}s
     * 
     * @param c     the {@code Connections} object to use.
     * @return this Layer instance (in fluent-style)
     */
    public Layer<T> using(Connections c) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        this.connections = c;
        return this;
    }

    /**
     * Allows the user to specify the {@link Parameters} object used by this
     * {@code Layer}. If the intent is to share Parameters across multiple
     * Layers, it must be kept in mind that two Layers containing the same
     * algorithm may require specification of locally different parameter
     * settings. In this case, one could use
     * {@link #alterParameter(KEY, Object)} method to change a local setting
     * without impacting the same setting in the source parameters object. This
     * is made possible because the {@link #alterParameter(KEY, Object)} method
     * first makes a local copy of the {@link Parameters} object, then modifies
     * the specified parameter.
     * 
     * @param p     the {@link Parameters} to use in this {@code Layer}
     * @return      this {@code Layer}
     */
    public Layer<T> using(Parameters p) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }
        this.params = p;
        return this;
    }

    /**
     * Adds an {@link HTMSensor} to this {@code Layer}. An HTMSensor is a regular
     * {@link Sensor} (i.e. {@link FileSensor}, {@link URISensor}, or {@link ObservableSensor})
     * which has had an {@link Encoder} configured and added to it. HTMSensors are
     * HTM Aware, where as regular Sensors have no knowledge of HTM requirements.
     * 
     * @param sensor    the {@link HTMSensor}
     * @return this Layer instance (in fluent-style)
     */
    @SuppressWarnings("rawtypes")
    public Layer<T> add(Sensor sensor) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }

        this.sensor = (HTMSensor<?>)sensor;
        
        // Configure the parent Network's reference to its sensor Region.
        if(parentNetwork != null && parentRegion != null) {
            parentNetwork.setSensorRegion(parentRegion);
            parentNetwork.setSensor(this.sensor);
        }
        
        // Store the SensorParams for Sensor rebuild after deserialization
        this.sensorParams = this.sensor.getSensorParams();
        
        return this;
    }

    /**
     * Adds a {@link MultiEncoder} to this {@code Layer}
     * 
     * @param encoder   the added MultiEncoder
     * @return this Layer instance (in fluent-style)
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
     * 
     * @param sp    the added SpatialPooler
     * @return this Layer instance (in fluent-style)
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
     * 
     * @param tm    the added TemporalMemory
     * @return this Layer instance (in fluent-style)
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
     * 
     * @param anomalyComputer   the Anomaly instance
     * @return this Layer instance (in fluent-style)
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
     * Adds a "generic" processing node into this {@code Layer}'s processing
     * chain.
     * 
     * <em><b>NOTE: When adding a generic node, the order of calls to
     * the addXXX() methods becomes crucially important. Make sure you 
     * have added items in a valid order in your "fluent" add call declarations.</b></em>
     * 
     * @param func      a {@link Func1} function to be performed at the point of
     *                  insertion within the {@code Layer}'s declaration.
     * @return     this Layer instance (in fluent-style)
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
     * Adds the ability to alter a given parameter in place during a fluent
     * creation statement. This {@code Layer}'s {@link Parameters} object is
     * copied and then the specified key/value pair are set on the internal
     * copy. This call does not affect the original Parameters object so that
     * local modifications may be made without having to reset them afterward
     * for subsequent use with another network structure.
     * 
     * @param key       the {@link Parameters} key.
     * @param value     the value of the parameter
     * @return  this Layer instance (in fluent-style)
     */
    public Layer<T> alterParameter(KEY key, Object value) {
        if(isClosed) {
            throw new IllegalStateException("Layer already \"closed\"");
        }

        // Preserve any input dimensions that might have been set prior to this
        // in
        // previous layers
        int[] inputDims = (int[])params.get(KEY.INPUT_DIMENSIONS);

        this.params = this.params.copy();
        this.params.set(key, value);
        this.params.set(KEY.INPUT_DIMENSIONS, inputDims);

        if(key == KEY.AUTO_CLASSIFY) {
            this.autoCreateClassifiers = value == null ? false : ((Boolean)value).booleanValue();
            // Note the addition of a classifier
            algo_content_mask |= CLA_CLASSIFIER;
        }
        return this;
    }

    /**
     * Returns the configured {@link Sensor} if any exists in this {@code Layer},
     * or null if one does not.
     * 
     * @return    any existing HTMSensor applied to this {@code Layer}
     */
    public HTMSensor<?> getSensor() {
        return sensor;
    }

    /**
     * Returns the {@link Connections} object being used by this {@link Layer}
     * 
     * @return      this {@code Layer}'s {@link Connections}
     */
    public Connections getConnections() {
        return this.connections;
    }

    /**
     * Processes a single element, sending the specified input up the configured
     * chain of algorithms or components within this {@code Layer}; resulting in
     * any {@link Subscriber}s or {@link Observer}s being notified of results
     * corresponding to the specified input (unless a {@link SpatialPooler}
     * "primer delay" has been configured).
     * 
     * The first input to the Layer invokes a method to resolve the transformer
     * at the bottom of the input chain, therefore the "type" (&lt;T&gt;) of the
     * input cannot be changed once this method is called for the first time.
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
     * Stops the processing of this {@code Layer}'s processing thread.
     */
    public void halt() {
        Object supplier = null;
        if(sensor != null && (supplier = sensor.getSensorParams().get("ONSUB")) != null) {
            if(supplier instanceof PublisherSupplier) {
                ((PublisherSupplier)supplier).clearSuppliedInstance();
            }
        }
        
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
     * Returns a flag indicating whether this layer's processing thread has been
     * halted or not.
     * 
     * @return
     */
    public boolean isHalted() {
        return isHalted;
    }

    /**
     * Sets the learning mode.
     * 
     * @param isLearn
     */
    public void setLearn(boolean isLearn) {
        this.isLearn = isLearn;
    }

    /**
     * Returns the learning mode setting.
     * 
     * @return
     */
    public boolean isLearn() {
        return isLearn;
    }

    /**
     * Completes the dispatch chain of algorithm {@link Observable}s with
     * specialized {@link Transformer}s for each algorithm contained within this
     * Layer. This method then starts the output stream processing of its
     * {@link Sensor} in a separate {@link Thread} (if it exists) - logging this
     * event.
     * 
     * Calling this method sets a flag on the underlying Sensor marking it as
     * "Terminal" meaning that it cannot be restarted and its output stream
     * cannot be accessed again.
     */
    @SuppressWarnings("unchecked")
    public void start() {
        if(isHalted) {
            restart(true);
            return;
        }
        
        // Save boilerplate setup steps by automatically closing when start is
        // called.
        if(!isClosed) {
            close();
        }

        if(sensor == null) {
            throw new IllegalStateException("A sensor must be added when the mode is not Network.Mode.MANUAL");
        }

        this.encoder = encoder == null ? sensor.getEncoder() : encoder;

        try {
            completeDispatch((T)new int[] {});
        } catch(Exception e) {
            notifyError(e);
        }

        startLayerThread();

        LOGGER.debug("Start called on Layer thread {}", LAYER_THREAD);
    }
    
    /**
     * Restarts this {@code Layer}
     * 
     * {@link #restart} is to be called after a call to {@link #halt()}, to begin
     * processing again. The {@link Network} will continue from where it previously
     * left off after the last call to halt().
     * 
     * @param startAtIndex      flag indicating whether the Layer should be started and
     *                          run from the previous save point or not.
     */
    @SuppressWarnings("unchecked")
    public void restart(boolean startAtIndex) {
        isHalted = false;
        
        if(!isClosed) {
            start();
        }else{
            if(sensor == null) {
                throw new IllegalStateException("A sensor must be added when the mode is not Network.Mode.MANUAL");
            }
            
            // Re-init the Sensor only if we're halted and haven't already been initialized
            // following a deserialization.
            if(!isPostSerialized) {
                // Recreate the Sensor and its underlying Stream
                recreateSensors();
            }
            
            if(parentNetwork != null) {
                parentNetwork.setSensor(sensor);
            }
            
            observableDispatch = createDispatchMap();
            
            this.encoder = encoder == null ? sensor.getEncoder() : encoder;
            
            skip = startAtIndex ? 
                (sensor.getSensorParams().get("ONSUB")) != null ? -1 : recordNum : 
                    (recordNum = -1);
            
            try {
                completeDispatch((T)new int[] {});
            } catch(Exception e) {
                notifyError(e);
            }
            
            startLayerThread();

            LOGGER.debug("Re-Start called on Layer thread {}", LAYER_THREAD);
        }
    }
    
    /**
     * Sets a pointer to the "next" Layer in this {@code Layer}'s
     * {@link Observable} sequence.
     * 
     * @param l
     */
    public void next(Layer<Inference> l) {
        this.next = l;
    }

    /**
     * Returns the next Layer following this Layer in order of process flow.
     * 
     * @return
     */
    public Layer<Inference> getNext() {
        return next;
    }

    /**
     * Sets a pointer to the "previous" Layer in this {@code Layer}'s
     * {@link Observable} sequence.
     * 
     * @param l
     */
    public void previous(Layer<Inference> l) {
        this.previous = l;
    }

    /**
     * Returns the previous Layer preceding this Layer in order of process flow.
     * 
     * @return
     */
    public Layer<Inference> getPrevious() {
        return previous;
    }

    /**
     * Returns a flag indicating whether this {@code Layer} is configured with a
     * {@link Sensor} which requires starting up.
     * 
     * @return
     */
    public boolean hasSensor() {
        return sensor != null;
    }

    /**
     * Returns the {@link Thread} from which this {@code Layer} is currently
     * outputting data. 
     * 
     * <b> Warning: This method returns the current thread if the Layer's processing
     * thread has never been started and therefore is null!</b>
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
     * 
     * @return
     */
    public Parameters getParameters() {
        return this.params;
    }

    /**
     * Returns the current predictive {@link Cell}s
     * 
     * @return the binary vector representing the current prediction.
     */
    public Set<Cell> getPredictiveCells() {
        return currentInference.getPredictiveCells();
    }

    /**
     * Returns the previous predictive {@link Cell}s
     * 
     * @return the binary vector representing the current prediction.
     */
    public Set<Cell> getPreviousPredictiveCells() {
        return currentInference.getPreviousPredictiveCells();
    }

    /**
     * Returns the current (dense) array of column indexes which represent
     * columns which have become activated during the current input sequence
     * from the SpatialPooler.
     * 
     * @return the array of active column indexes
     */
    public int[] getFeedForwardActiveColumns() {
        return currentInference.getFeedForwardActiveColumns();
    }

    /**
     * Returns the {@link Cell}s activated in the {@link TemporalMemory} at time
     * "t"
     * 
     * @return
     */
    public Set<Cell> getActiveCells() {
        return currentInference.getActiveCells();
    }

    /**
     * Returns the SpatialPooler column activations in sparse form (indexes of
     * the on bits).
     * 
     * @return
     */
    public int[] getFeedForwardSparseActives() {
        return currentInference.getFeedForwardSparseActives();
    }

    /**
     * Returns the {@link Connections} object being used as the structural
     * matrix and state.
     */
    public Connections getMemory() {
        return connections;
    }

    /**
     * Returns the count of records historically inputted into this
     * {@code Layer}
     * 
     * @return the current record input count
     */
    public int getRecordNum() {
        return recordNum;
    }

    /**
     * Returns a flag indicating whether this {@code Layer} has had
     * its {@link #close} method called, or not.
     * @return
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Resets the internal record count to zero
     * 
     * @return this {@code LayerImpl}
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
        } else {
            temporalMemory.reset(connections);
        }
    }

    /**
     * Returns a flag indicating whether this {@code Layer} contains a
     * {@link TemporalMemory}.
     * 
     * @return
     */
    public boolean hasTemporalMemory() {
        return temporalMemory != null;
    }

    /**
     * Increments the current record sequence number.
     */
    public Layer<T> increment() {
        if(skip > -1) {
            --skip;
        } else {
            ++recordNum;
        }
        return this;
    }

    /**
     * Sets the name and returns this Layer.
     * 
     * @param name
     * @return
     */
    public Layer<T> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Returns the String identifier of this {@code Layer}
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the last computed {@link Inference} of this {@code Layer}
     * 
     * @return the last computed inference.
     */
    public Inference getInference() {
        return currentInference;
    }

    /**
     * Returns the resident {@link MultiEncoder} or the encoder residing in this
     * {@code Layer}'s {@link Sensor}, if any.
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
     * Returns the values submitted to this {@code Layer} in an array whose
     * indexes correspond to the indexes of probabilities returned when calling
     * {@link #getAllPredictions(String, int)}.
     * 
     * @param field     The field name of the required prediction
     * @param step      The step for the required prediction
     * @return
     */
    @SuppressWarnings("unchecked")
    public <V> V[] getAllValues(String field, int step) {
        if(currentInference == null || currentInference.getClassifiers() == null) {
            throw new IllegalStateException("Predictions not available. " + "Either classifiers unspecified or inferencing has not yet begun.");
        }

        Classification<?> c = currentInference.getClassification(field);
        if(c == null) {
            LOGGER.debug("No ClassifierResult exists for the specified field: {}", field);
        }

        return (V[])c.getActualValues();
    }

    /**
     * Returns a double[] containing a prediction confidence measure for each
     * bucket (unique entry as determined by an encoder). In order to relate the
     * probability to an actual value, call {@link #getAllValues(String, int)}
     * which returns an array containing the actual values submitted to this
     * {@code Layer} - the indexes of each probability will match the index of
     * each actual value entered.
     * 
     * @param field     The field name of the required prediction
     * @param step      The step for the required prediction
     * @return
     */
    public double[] getAllPredictions(String field, int step) {
        if(currentInference == null || currentInference.getClassifiers() == null) {
            throw new IllegalStateException("Predictions not available. " + "Either classifiers unspecified or inferencing has not yet begun.");
        }

        Classification<?> c = currentInference.getClassification(field);
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
            throw new IllegalStateException("Predictions not available. " + "Either classifiers unspecified or inferencing has not yet begun.");
        }

        Classification<?> c = currentInference.getClassification(field);
        if(c == null) {
            LOGGER.debug("No ClassifierResult exists for the specified field: {}", field);
        }

        return (K)c.getMostProbableValue(step);
    }

    /**
     * Returns the bucket index of the value with the highest calculated
     * probability for the specified field and step.
     * 
     * @param field     The field name of the required prediction
     * @param step      The step for the required prediction
     * @return
     */
    public int getMostProbableBucketIndex(String field, int step) {
        if(currentInference == null || currentInference.getClassifiers() == null) {
            throw new IllegalStateException("Predictions not available. " + "Either classifiers unspecified or inferencing has not yet begun.");
        }

        Classification<?> c = currentInference.getClassification(field);
        if(c == null) {
            LOGGER.debug("No ClassifierResult exists for the specified field: {}", field);
        }

        return c.getMostProbableBucketIndex(step);
    }

    //////////////////////////////////////////////////////////////
    //          PRIVATE METHODS AND CLASSES BELOW HERE          //
    //////////////////////////////////////////////////////////////
    /**
     * Notify all subscribers through the delegate that stream processing has
     * been completed or halted.
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
     * Called internally to propagate the specified {@link Exception} up the
     * network hierarchy
     * 
     * @param e     the exception to notify users of
     */
    void notifyError(Exception e) {
        for(Observer<Inference> o : subscribers) {
            o.onError(e);
        }
        for(Observer<Inference> o : observers) {
            o.onError(e);
        }
        publisher.onError(e);
    }

    /**
     * <p>
     * Returns the content mask used to indicate what algorithm contents this
     * {@code Layer} has. This is used to determine whether the
     * {@link Inference} object passed between layers should share values.
     * </p>
     * <p>
     * If any algorithms are repeated then {@link Inference}s will
     * <em><b>NOT</b></em> be shared between layers. {@link Region}s
     * <em><b>NEVER</b></em> share {@link Inference}s
     * </p>
     * 
     * @return
     */
    byte getMask() {
        return algo_content_mask;
    }

    /**
     * Initializes the algorithm content mask used for detection of repeated
     * algorithms among {@code Layer}s in a {@link Region}. see
     * {@link #getMask()} for more information.
     */
    private void initializeMask() {
        algo_content_mask |= (this.spatialPooler == null ? 0 : SPATIAL_POOLER);
        algo_content_mask |= (this.temporalMemory == null ? 0 : TEMPORAL_MEMORY);
        algo_content_mask |= (this.autoCreateClassifiers == null || !autoCreateClassifiers ? 0 : CLA_CLASSIFIER);
        algo_content_mask |= (this.anomalyComputer == null ? 0 : ANOMALY_COMPUTER);
    }

    /**
     * Returns a flag indicating whether we've connected the first observable in
     * the sequence (which lazily does the input type of &lt;T&gt; to
     * {@link Inference} transformation) to the Observables connecting the rest
     * of the algorithm components.
     * 
     * @return flag indicating all observables connected. True if so, false if
     *         not
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
        if(subscribers == null) subscribers = new ConcurrentLinkedQueue<Observer<Inference>>();
        subscribers.add(getDelegateObserver());
        subscription = sequence.subscribe(getDelegateSubscriber());

        // The map of input types to transformers is no longer needed.
        observableDispatch.clear();
        observableDispatch = null;

        // Handle global network sensor access.
        if(sensor == null && parentNetwork != null && parentNetwork.isTail(this)) {
            sensor = parentNetwork == null ? null : parentNetwork.getSensor();
        } else if(parentNetwork != null && sensor != null) {
            parentNetwork.setSensor(sensor);
        }
    }

    /**
     * We cannot create the {@link Observable} sequence all at once because the
     * first step is to transform the input type to the type the rest of the
     * sequence uses (Observable<b>&lt;Inference&gt;</b>). This can only happen
     * during the actual call to {@link #compute(Object)} which presents the
     * input type - so we create a map of all types of expected inputs, and then
     * connect the sequence at execution time; being careful to only incur the
     * cost of sequence assembly on the first call to {@link #compute(Object)}.
     * After the first call, we dispose of this map and its contents.
     * 
     * @return the map of input types to {@link Transformer}
     */
    @SuppressWarnings("unchecked")
    private Map<Class<T>, Observable<ManualInput>> createDispatchMap() {
        Map<Class<T>, Observable<ManualInput>> observableDispatch = Collections.synchronizedMap(new HashMap<Class<T>, Observable<ManualInput>>());

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
            if(getSensor().getMetaInfo().getFieldTypes().stream().anyMatch(ft -> {
                return ft == FieldMetaType.SARR || ft == FieldMetaType.DARR || ft == FieldMetaType.COORD || ft == FieldMetaType.GEO;
            })) {
                if(autoCreateClassifiers) {
                    throw new IllegalStateException("Cannot autoclassify with raw array input or " + " Coordinate based encoders... Remove auto classify setting.");
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
     * This method is necessary to be able to retrieve the mapped
     * {@link Observable} types to input types or their subclasses if any.
     * 
     * @param t   the input type. The "expected" types are:
     *            <ul>
     *            <li>{@link Map}</li>
     *            <li>{@link ManualInput}</li>
     *            <li>String[]</li>
     *            <li>int[]</li>
     *            </ul>
     *            or their subclasses.
     * @return an Observable suitable for subscribing or operating on.
     */
    private Observable<ManualInput> resolveObservableSequence(T t) {
        Observable<ManualInput> sequenceStart = null;

        if(observableDispatch == null) {
            observableDispatch = createDispatchMap();
        }
        
        if(observableDispatch != null) {
            if(ManualInput.class.isAssignableFrom(t.getClass())) {
                sequenceStart = observableDispatch.get(ManualInput.class);
            } else if(Map.class.isAssignableFrom(t.getClass())) {
                sequenceStart = observableDispatch.get(Map.class);
            } else if(t.getClass().isArray()) {
                if(t.getClass().equals(String[].class)) {
                    sequenceStart = observableDispatch.get(String[].class);
                } else if(t.getClass().equals(int[].class)) {
                    sequenceStart = observableDispatch.get(int[].class);
                }
            }
        }
        
        // Insert skip observable operator if initializing with an advanced record number
        // (i.e. Serialized Network)
        if(recordNum > 0 && skip != -1) {
            sequenceStart = sequenceStart.skip(recordNum + 1);
            
            Integer skipCount;
            if(((skipCount = (Integer)params.get(KEY.SP_PRIMER_DELAY)) != null)) {
                // No need to "warm up" the SpatialPooler if we're deserializing an SP
                // that has been running... However "skipCount - recordNum" is there so 
                // we make sure the Network has run at least long enough to satisfy the 
                // original requested "primer delay".
                params.set(KEY.SP_PRIMER_DELAY, Math.max(0, skipCount - recordNum));
            }
        }
        
        sequenceStart = sequenceStart.filter(m -> {
            if(!checkPointOpObservers.isEmpty() && parentNetwork != null) {
                // Execute check point logic
                doCheckPoint();
            }
            
            return true;
        });

        return sequenceStart;
    }
    
    /**
     * Executes the check point logic, handles the return of the serialized byte array
     * by delegating the call to {@link rx.Observer#onNext}(byte[]) of all the currently queued
     * Observers; then clears the list of Observers.
     */
    private void doCheckPoint() {
        byte[] bytes = parentNetwork.internalCheckPointOp();
        
        if(bytes != null) {
            LOGGER.debug("Layer [" + getName() + "] checkPointed file: " + 
                Persistence.get().getLastCheckPointFileName());
        }else{
            LOGGER.debug("Layer [" + getName() + "] checkPoint   F A I L E D   at: " + (new DateTime()));
        }
        
        for(Observer<byte[]> o : checkPointOpObservers) {
            o.onNext(bytes);
            o.onCompleted();
        }
        
        checkPointOpObservers.clear();
    }

    /**
     * Stores a {@link NamedTuple} which contains the input values and bucket
     * information - keyed to the encoded field name so that a classifier can
     * retrieve it later on in the processing sequence.
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
            } else if(Number.class.isAssignableFrom(o.getClass())) {
                bucketIdx = e.getBucketIndices((double)o)[0];
            } else {
                bucketIdx = e.getBucketIndices((String)o)[0];
            }

            int offset = t.getOffset();
            int[] tempArray = new int[e.getWidth()];
            System.arraycopy(encoding, offset, tempArray, 0, tempArray.length);

            inference.getClassifierInput().put(
                    name,
                    new NamedTuple(
                            new String[] { "name", "inputValue", "bucketIdx", "encoding" },
                            name,
                            o,
                            bucketIdx,
                            tempArray
                    ));
        }
    }

    /**
     * Connects the {@link Transformer} to the rest of the {@link Observable}
     * sequence.
     * 
     * @param o     the Transformer part of the sequence.
     * @return the completed {@link Observable} sequence.
     */
    private Observable<ManualInput> fillInSequence(Observable<ManualInput> o) {
        // Route to ordered dispatching if required.
        if(hasGenericProcess) {
            return fillInOrderedSequence(o);
        }

        // Spatial Pooler config
        if(spatialPooler != null) {
            Integer skipCount = 0;
            if((skipCount = ((Integer)params.get(KEY.SP_PRIMER_DELAY))) != null) {
                o = o.map(factory.createSpatialFunc(spatialPooler)).skip(skipCount.intValue());
            } else {
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
        Collections.reverse(addedItems);
        
        for(Object node : addedItems) {
            if(node instanceof Func1<?, ?>) {
                o = o.map((Func1<ManualInput, ManualInput>)node);
            } else if(node instanceof SpatialPooler) {
                Integer skipCount = 0;
                if((skipCount = ((Integer)params.get(KEY.SP_PRIMER_DELAY))) != null) {
                    o = o.map(factory.createSpatialFunc(spatialPooler)).skip(skipCount.intValue());
                } else {
                    o = o.map(factory.createSpatialFunc(spatialPooler));
                }
            } else if(node instanceof TemporalMemory) {
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
     * Called internally to create a subscription on behalf of the specified
     * Layer {@link Observer}
     * 
     * @param sub       the Layer Observer (subscriber).
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
     * Returns the {@link PublishSubject}'s subscriber which delegates to all
     * the {@link Layer}'s subscribers.
     * 
     * @return
     */
    private Observer<Inference> getDelegateSubscriber() {
        return new Observer<Inference>() {

            @Override
            public void onCompleted() {
                for(Observer<Inference> o : subscribers) {
                    o.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                for(Observer<Inference> o : subscribers) {
                    o.onError(e);
                }
            }

            @Override
            public void onNext(Inference i) {
                currentInference = i;
                for(Observer<Inference> o : subscribers) {
                    o.onNext(i);
                }
            }
        };
    }

    /**
     * Returns the {@link PublishSubject}'s subscriber which delegates to all
     * the {@link Layer}'s subscribers.
     * 
     * @return
     */
    private Observer<Inference> getDelegateObserver() {
        return new Observer<Inference>() {

            @Override
            public void onCompleted() {
                for(Observer<Inference> o : observers) {
                    o.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                for(Observer<Inference> o : observers) {
                    o.onError(e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onNext(Inference i) {
                currentInference = i;
                for(Observer<Inference> o : observers) {
                    o.onNext(i);
                }
            }
        };
    }
    
    /**
     * Clears the subscriber and observer lists so they can be rebuilt
     * during restart or deserialization.
     */
    private void clearSubscriberObserverLists() {
        if(observers == null) observers = new ArrayList<Observer<Inference>>(); 
        if(subscribers == null) subscribers = new ConcurrentLinkedQueue<Observer<Inference>>();
        subscribers.clear();
        userObservable = null;
    }

    /**
     * Creates the {@link NamedTuple} of names to encoders used in the
     * observable sequence.
     * 
     * @param encoder
     * @return
     */
    @SuppressWarnings("unchecked")
    NamedTuple makeClassifiers(MultiEncoder encoder) {
        Map<String, Class<? extends Classifier>> inferredFields = (Map<String, Class<? extends Classifier>>) params.get(KEY.INFERRED_FIELDS);
        if(inferredFields == null || inferredFields.entrySet().size() == 0) {
            throw new IllegalStateException(
                    "KEY.AUTO_CLASSIFY has been set to \"true\", but KEY.INFERRED_FIELDS is null or\n\t" +
                    "empty. Must specify desired Classifier for at least one input field in\n\t" +
                    "KEY.INFERRED_FIELDS or set KEY.AUTO_CLASSIFY to \"false\" (which is its default\n\t" +
                    "value in Parameters)."
            );
        }
        String[] names = new String[encoder.getEncoders(encoder).size()];
        Classifier[] ca = new Classifier[names.length];
        int i = 0;
        for(EncoderTuple et : encoder.getEncoders(encoder)) {
            names[i] = et.getName();
            Class fieldClassifier = inferredFields.get(et.getName());
            if(fieldClassifier == null) {
                LOGGER.info("Not classifying \"" + et.getName() + "\" input field");
            }
            else if(CLAClassifier.class.isAssignableFrom(fieldClassifier)) {
                LOGGER.info("Classifying \"" + et.getName() + "\" input field with CLAClassifier");
                ca[i] = new CLAClassifier();
            }
            else if(SDRClassifier.class.isAssignableFrom(fieldClassifier)) {
                LOGGER.info("Classifying \"" + et.getName() + "\" input field with SDRClassifier");
                ca[i] = new SDRClassifier();
            }
            else {
                throw new IllegalStateException(
                        "Invalid Classifier class token, \"" + fieldClassifier + "\",\n\t" +
                        "specified for, \"" + et.getName() + "\", input field.\n\t" +
                        "Valid class tokens are CLAClassifier.class and SDRClassifier.class"
                );
            }
            i++;
        }
        return new NamedTuple(names, (Object[])ca);
    }

    /**
     * Called internally to invoke the {@link SpatialPooler}
     * 
     * @param input
     * @return
     */
    protected int[] spatialInput(int[] input) {
        if(input == null) {
            LOGGER.info("Layer ".concat(getName()).concat(" received null input"));
        } else if(input.length < 1) {
            LOGGER.info("Layer ".concat(getName()).concat(" received zero length bit vector"));
            return input;
        }
        
        int[] activeColumns = new int[numColumns];
        spatialPooler.compute(connections, input, activeColumns, isLearn || (sensor != null && sensor.getMetaInfo().isLearn()));
      
        return activeColumns;
    }

    /**
     * Called internally to invoke the {@link TemporalMemory}
     * 
     * @param input     the current input vector
     * @param mi        the current input inference container
     * @return
     */
    protected int[] temporalInput(int[] input, ManualInput mi) {
        ComputeCycle cc = null;
        if(sensor != null) {
            if(sensor.getMetaInfo().isReset()) {
                temporalMemory.reset(connections);
            }

            cc = temporalMemory.compute(connections, input, sensor.getMetaInfo().isLearn());
        } else {
            cc = temporalMemory.compute(connections, input, isLearn);
        }
        
        // Store the predictive columns / simultaneously storing previous predictive in this method
        mi.predictiveCells(cc.predictiveCells());
        // Store activeCells
        mi.activeCells(cc.activeCells);
        // Store the Compute Cycle
        mi.computeCycle = cc;
        
        return SDR.asCellIndices(cc.activeCells);
    }
    
    /**
     * Starts this {@code Layer}'s thread
     */
    protected void startLayerThread() {
        LAYER_THREAD = new Thread("Sensor Layer [" + getName() + "] Thread") {

            @SuppressWarnings("unchecked")
            public void run() {
                LOGGER.debug("Layer [" + getName() + "] started Sensor output stream processing.");

                // Applies "terminal" function, at this point the input stream
                // is "sealed".
                sensor.getOutputStream().filter(i -> {
                    if(isHalted) {
                        notifyComplete();
                        if(next != null) {
                            next.halt();
                        }
                        return false;
                    }
                    
                    if(Thread.currentThread().isInterrupted()) {
                        notifyError(new RuntimeException("Unknown Exception while filtering input"));
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
        };
        
        LAYER_THREAD.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				notifyError(new RuntimeException("Unhandled Exception in "+LAYER_THREAD.getName(),e));
			}
		});
        LAYER_THREAD.start();
    }
    
    /**
     * Returns an {@link rx.Observable} operator that when subscribed to, invokes an operation
     * that stores the state of this {@code Network} while keeping the Network up and running.
     * The Network will be stored at the pre-configured location (in binary form only, not JSON).
     * 
     * @return  the {@link CheckPointOp} operator
     */
    @SuppressWarnings("unchecked")
    CheckPointOp<byte[]> getCheckPointOperator() {
        if(checkPointOp == null) {
            checkPointOp = new CheckPointOperator<byte[]>(Layer.this);
        }
        return (CheckPointOp<byte[]>)checkPointOp;
    }
    
    
    //////////////////////////////////////////////////////////////
    //   Inner Class Definition for CheckPointer (Observable)   //
    //////////////////////////////////////////////////////////////
    
    /**
     * <p>
     * Implementation of the CheckPointOp interface which serves to checkpoint
     * and register a listener at the same time. The {@link rx.Observer} will be
     * notified with the byte array of the {@link Network} being serialized.
     * </p><p>
     * The layer thread automatically tests for the list of observers to 
     * contain > 0 elements, which indicates a check point operation should
     * be executed.
     * </p>
     * 
     * @param <T>       {@link rx.Observer}'s return type
     */
    static class CheckPointOperator<T> extends Observable<T> implements CheckPointOp<T> {
        private CheckPointOperator(Layer<?> l) {
            this(new Observable.OnSubscribe<T>() {
                @SuppressWarnings({ "unchecked" })
                @Override public void call(Subscriber<? super T> r) {
                    if(l.LAYER_THREAD != null) {
                        // The layer thread automatically tests for the list of observers to 
                        // contain > 0 elements, which indicates a check point operation should
                        // be executed.
                        l.checkPointOpObservers.add((Observer<byte[]>)r);
                    }else{
                        l.doCheckPoint();
                    }
                }
            });
        }
        
        /**
         * Constructs this {@code CheckPointOperator}
         * @param f     a subscriber function
         */
        protected CheckPointOperator(rx.Observable.OnSubscribe<T> f) {
            super(f);
        }
        
        /**
         * Queues the specified {@link rx.Observer} for notification upon
         * completion of a check point operation.
         */
        public Subscription checkPoint(Observer<? super T> t) {
            return super.subscribe(t);
        }
    }
        
    
    //////////////////////////////////////////////////////////////
    //        Inner Class Definition Transformer Example        //
    //////////////////////////////////////////////////////////////
    /**
     * Factory which returns an {@link Observable} capable of transforming known
     * input formats to the universal format object passed between all
     * Observables in the Observable chains making up the processing steps
     * involving HTM algorithms.
     * 
     * The {@link Transformer} implementations are used to transform various
     * inputs into a {@link ManualInput}, and thus are used at the beginning of
     * any Observable chain; each succeding Observable in a given chain would
     * then communicate via passed ManualInputs or {@link Inference}s (which are
     * the same thing).
     * 
     * @author David Ray
     * @see Layer#completeDispatch(Object)
     * @see Layer#resolveObservableSequence(Object)
     * @see Layer#fillInSequence(Observable)
     */
    class FunctionFactory implements Persistable {
        private static final long serialVersionUID = 1L;
        
        ManualInput inference = new ManualInput();

        //////////////////////////////////////////////////////////////////////////////
        //                              TRANSFORMERS                                //
        //////////////////////////////////////////////////////////////////////////////
        /**
         * WARNING: UNIMPLEMENTED
         * 
         * <p>
         * Emits an {@link Observable} which is transformed from a String[] of
         * csv input to one that emits {@link Inference}s.
         * </p>
         * <p>
         * This class currently lacks the implementation of csv parsing into
         * distinct Object types - which is necessary to compose a "multi-map"
         * necessary to input data into the {@link MultiEncoder} necessary to
         * encode multiple field inputs.
         * </p>
         * <p>
         * TODO: Implement later
         * </p>
         */
        class String2Inference implements Transformer<String[], ManualInput> {

            @Override
            public Observable<ManualInput> call(Observable<String[]> t1) {
                return t1.map(new Func1<String[], ManualInput>() {

                    @Override
                    public ManualInput call(String[] t1) {

                        ////////////////////////////////////////////////////////////////////////
                        //                  Do transformative work here                       //
                        //                                                                    //
                        // In "real life", this will send data through the MultiEncoder       //
                        // Below is simply a faked out place holder...                        //
                        ////////////////////////////////////////////////////////////////////////
                        int[] sdr = new int[t1.length];
                        for(int i = 0;i < sdr.length;i++) {
                            sdr[i] = Integer.parseInt(t1[i]);
                        }

                        return inference.recordNum(getRecordNum()).sdr(sdr).layerInput(sdr);
                    }
                });
            }
        }

        /**
         * <p>
         * Emits an {@link Observable} which is transformed from a Map input
         * type to one that emits {@link Inference}s.
         * </p>
         * <p>
         * This {@link Transformer} is used when the input to a given
         * {@link Layer} is a map of fields to input Objects. It is typically
         * used when a Layer is configured with a {@link MultiEncoder} (which is
         * the only encoder type that may be contained within a Layer, because
         * it can be used to hold any combination of encoders required.).
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

                        return inference.recordNum(getRecordNum()).layerInput(t1);
                    }
                });
            }
        }

        /**
         * <p>
         * Emits an {@link Observable} which is transformed from a binary vector
         * input type to one that emits {@link Inference}s.
         * </p>
         * <p>
         * This type is used when bypassing an encoder and possibly any other
         * algorithm usually connected in a sequence of algorithms within a
         * {@link Layer}
         * </p>
         */
        class Vector2Inference implements Transformer<int[], ManualInput> {

            @Override
            public Observable<ManualInput> call(Observable<int[]> t1) {
                return t1.map(new Func1<int[], ManualInput>() {

                    @Override
                    public ManualInput call(int[] t1) {
                        // Indicates a value that skips the encoding step
                        return inference.recordNum(getRecordNum()).sdr(t1).layerInput(t1);
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
                        return inference.recordNum(getRecordNum()).sdr(t1.getSDR()).recordNum(t1.getRecordNum()).layerInput(t1);
                    }
                });
            }
        }

        //////////////////////////////////////////////////////////////////////////////
        //                    INPUT TRANSFORMATION FUNCTIONS                        //
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
        //                   OBSERVABLE COMPONENT CREATION METHODS                  //
        //////////////////////////////////////////////////////////////////////////////
        public Func1<ManualInput, ManualInput> createSpatialFunc(final SpatialPooler sp) {
            return new Func1<ManualInput, ManualInput>() {

                int inputWidth = -1;

                @Override
                public ManualInput call(ManualInput t1) {
                    int[] sdr = t1.getSDR();
                    if(sdr.length > 0 && ArrayUtils.isSparse(sdr)) {
                        if(inputWidth == -1) {
                            inputWidth = calculateInputWidth();
                        }
                        sdr = ArrayUtils.asDense(sdr, inputWidth);
                    }
                    sdr = spatialInput(sdr);
                    return t1.sdr(sdr).feedForwardActiveColumns(sdr);
                }
            };
        }

        public Func1<ManualInput, ManualInput> createTemporalFunc(final TemporalMemory tm) {
            return new Func1<ManualInput, ManualInput>() {

                @Override
                public ManualInput call(ManualInput t1) {
                    int[] sdr = t1.getSDR();
                    if(!ArrayUtils.isSparse(sdr)) {
                        // Set on Layer, then set sparse actives as the sdr,
                        // then set on Manual Input (t1)
                        sdr = ArrayUtils.where(sdr, ArrayUtils.WHERE_1);
                        t1.sdr(sdr).feedForwardSparseActives(sdr);
                    }
                    return t1.sdr(temporalInput(sdr, t1));
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

                        Classifier c = (Classifier)t1.getClassifiers().get(key);

                        // c will be null if no classifier was specified for this field in KEY.INFERRED_FIELDS map
                        if(c != null) {
                            Classification<Object> result = c.compute(recordNum, inputMap, t1.getSDR(), isLearn, true);
                            t1.recordNum(recordNum).storeClassification((String)inputs.get("name"), result);
                        }
                    }

                    return t1;
                }
            };
        }

        public Func1<ManualInput, ManualInput> createAnomalyFunc(final Anomaly an) {
            return new Func1<ManualInput, ManualInput>() {
                int isArrayInput = -1;
                int cellsPerColumn = connections.getCellsPerColumn();

                @Override
                public ManualInput call(ManualInput t1) {
                    if((hasSP() && t1.getFeedForwardSparseActives() == null) || t1.getPreviousPredictiveCells() == null) {
                        return t1.anomalyScore(1.0);
                    }else if(!hasSP() && (isArrayInput == 1 || t1.getLayerInput().getClass().equals(int[].class))) {
                        isArrayInput = 1;
                        t1.feedForwardSparseActives((int[])t1.getLayerInput());
                    }
                    
                    return t1.anomalyScore(anomalyComputer.compute(t1.getFeedForwardSparseActives(), 
                        SDR.cellsAsColumnIndices(t1.getPreviousPredictiveCells(), cellsPerColumn), 0, 0));
                }
            };
        }

    }
    
    /**
     * Re-initializes the {@link HTMSensor} following deserialization or restart
     * after halt.
     */
    private void recreateSensors() {
        if(sensor != null) {
            // Recreate the Sensor and its underlying Stream
            Class<?> sensorKlass = sensor.getSensorClass();
            if(sensorKlass.toString().indexOf("File") != -1) {
                Object path = sensor.getSensorParams().get("PATH");
                sensor = (HTMSensor<?>)Sensor.create(
                    FileSensor::create, SensorParams.create(Keys::path, "", path));
            }else if(sensorKlass.toString().indexOf("Observ") != -1) {
                Object supplierOfObservable = sensor.getSensorParams().get("ONSUB");
                sensor = (HTMSensor<?>)Sensor.create(
                    ObservableSensor::create, SensorParams.create(Keys::obs, "", supplierOfObservable));
            }else if(sensorKlass.toString().indexOf("URI") != -1) {
                Object url = sensor.getSensorParams().get("URI");
                sensor = (HTMSensor<?>)Sensor.create(
                    URISensor::create, SensorParams.create(Keys::uri, "", url));
            }
        }
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + recordNum;
        result = prime * result + algo_content_mask;
        result = prime * result + ((currentInference == null) ? 0 : currentInference.hashCode());
        result = prime * result + (hasGenericProcess ? 1231 : 1237);
        result = prime * result + (isClosed ? 1231 : 1237);
        result = prime * result + (isHalted ? 1231 : 1237);
        result = prime * result + (isLearn ? 1231 : 1237);
        result = prime * result + ((parentRegion == null) ? 0 : parentRegion.hashCode());
        result = prime * result + ((sensorParams == null) ? 0 : sensorParams.hashCode());
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
        if(algo_content_mask != other.algo_content_mask)
            return false;
        if(currentInference == null) {
            if(other.currentInference != null)
                return false;
        } else if(!currentInference.equals(other.currentInference))
            return false;
        if(recordNum != other.recordNum)
          return false;
        if(hasGenericProcess != other.hasGenericProcess)
            return false;
        if(isClosed != other.isClosed)
            return false;
        if(isHalted != other.isHalted)
            return false;
        if(isLearn != other.isLearn)
            return false;
        if(parentRegion == null) {
            if(other.parentRegion != null)
                return false;
        } else if(other.parentRegion == null || !parentRegion.getName().equals(other.parentRegion.getName()))
            return false;
        if(sensorParams == null) {
            if(other.sensorParams != null)
                return false;
        } else if(!sensorParams.equals(other.sensorParams))
            return false;
        
        return true;
    }

}
