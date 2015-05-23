package org.numenta.nupic.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.numenta.nupic.Connections;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.EncoderTuple;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.research.ComputeCycle;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
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
 * <pre>
 *      Map<String, Map<String, Object>> fieldEncodings = new HashMap<>();
 *      Map<String, Object> inner = new HashMap<>();
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
 *      Map<String, Object> inner2 = new HashMap<>();
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
 *  
 * @author David Ray
 * @see LayerTest
 */
public class Layer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalarEncoder.class);

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
    
    private Layer<Inference> next;
    private Layer<Inference> previous;
    
    private List<Observer<Inference>> observers = new ArrayList<Observer<Inference>>();
    private List<Observer<Inference>> subscribers = Collections.synchronizedList(new ArrayList<Observer<Inference>>());
    
    private Map<Class<T>, Observable<ManualInput>> observableDispatch = 
        Collections.synchronizedMap(new HashMap<Class<T>, Observable<ManualInput>>());

    /** This layer's thread */
    private Thread LAYER_THREAD;
    
    
    
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

        if(n == null || p == null) {
            if(n == null) {
                LOGGER.warn("Attempt to instantiate Layer with null Network");
            }else{
                LOGGER.warn("Attempt to instantiate Layer with null Parameters");
            }
            return;
        }
        connections = new Connections();
        this.params.apply(connections);
        
        this.autoCreateClassifiers = (Boolean)p.getParameterByKey(KEY.AUTO_CLASSIFY);

        factory = new FunctionFactory();
        
        observableDispatch = createDispatchMap();
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
    @SuppressWarnings("unchecked")
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
        this.params.apply(connections);

        factory = new FunctionFactory();

        // Create Encoder hierarchy from definitions & auto create classifiers if specified
        if(encoder != null) {
            encoder.addMultipleEncoders(
                (Map<String, Map<String, Object>>)params.getParameterByKey(
                    KEY.FIELD_ENCODING_MAP));
            
            if(autoCreateClassifiers != null && autoCreateClassifiers.booleanValue()) {
                factory.inference.classifiers(makeClassifiers(encoder));
            }
        }

        // Let the SpatialPooler initialize the matrix with its requirements
        if(spatialPooler != null) {
            spatialPooler.init(connections);
        }

        // Let the TemporalMemory initialize the matrix with its requirements
        if(temporalMemory != null) {
            temporalMemory.init(connections);
        }

        this.activeColumns = new int[connections.getNumColumns()];
        this.cellsPerColumn = connections.getCellsPerColumn();
        
        observableDispatch = createDispatchMap();

        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Layer successfully created containing: {}{}{}{}{}",
                (encoder == null ? "" : "MultiEncoder,"),
                (spatialPooler == null ? "" : "SpatialPooler,"),
                (temporalMemory == null ? "" : "TemporalMemory,"),
                (autoCreateClassifiers == null ? "" : "Auto creating CLAClassifiers for each input field."),
                (anomalyComputer == null ? "" : "Anomaly"));
        }
    }
    
    public void init() {
        
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
     * @param <T>           the input value type.
     * 
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
     * 
     * @param c     the {@code Connections} object to use.
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> using(Connections c) {
        this.connections = c;
        if(params != null) {
            params.apply(connections);
        }
        return this;
    }

    /**
     * Adds an {@link HTMSensor} to this {@code Layer}
     * @param sensor
     * @return          this Layer instance (in fluent-style)
     */
    @SuppressWarnings("rawtypes")
    public Layer<T> add(Sensor sensor) {
        this.sensor = (HTMSensor<?>)sensor;
        this.sensor.setLocalParameters(params);
        return this;
    }

    /**
     * Adds a {@link MultiEncoder} to this {@code Layer}
     * @param encoder   the added MultiEncoder
     * @return          this Layer instance (in fluent-style)
     */
    @SuppressWarnings("unchecked")
    public Layer<T> add(MultiEncoder encoder) {
        this.encoder = encoder;
        
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
        
        autoCreateClassifiers = (Boolean)params.getParameterByKey(KEY.AUTO_CLASSIFY);
        if(autoCreateClassifiers != null && autoCreateClassifiers.booleanValue()) {
            factory.inference.classifiers(makeClassifiers(encoder));
        }
        return this;
    }

    /**
     * Adds a {@link SpatialPooler} to this {@code Layer}
     * @param sp        the added SpatialPooler
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> add(SpatialPooler sp) {
        if(sensor != null) {
            connections.setNumInputs(sensor.getEncoder().getWidth());
        }
        // The exact dimensions don't have to be the same but the number of dimensions do!
        int inputLength, columnLength = 0;
        if((inputLength = ((int[])params.getParameterByKey(KEY.INPUT_DIMENSIONS)).length) !=
            (columnLength = ((int[])params.getParameterByKey(KEY.COLUMN_DIMENSIONS)).length)) {
            LOGGER.warn("The number of Input Dimensions (" + inputLength + ") is not same as the number of Column Dimensions " +
                "(" + columnLength + ") in Parameters!");
            
            return this;
        }
        this.spatialPooler = sp;
        spatialPooler.init(connections);
        activeColumns = new int[connections.getNumColumns()];
        return this;
    }

    /**
     * Adds a {@link TemporalMemory} to this {@code Layer}
     * @param tm        the added TemporalMemory
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> add(TemporalMemory tm) {
        this.temporalMemory = tm;
        temporalMemory.init(connections);
        return this;
    }

    /**
     * Adds an {@link Anomaly} computer to this {@code Layer}
     * @param anomalyComputer   the Anomaly instance
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> add(Anomaly anomalyComputer) {
        this.anomalyComputer = anomalyComputer;
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
        this.params = this.params.copy();
        this.params.setParameterByKey(key, value);
        
        if(key == KEY.AUTO_CLASSIFY) {
            this.autoCreateClassifiers = value == null ? 
                false : ((Boolean)value).booleanValue();
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
        increment();
        
        if(!dispatchCompleted()) {
            completeDispatch(t);
        }
        
        publisher.onNext(t);
    }
    
    
    @SuppressWarnings("unchecked")
    public void start() {
        if(sensor == null) {
            throw new IllegalStateException("A sensor must be added when the mode is not Network.Mode.MANUAL");
        }
        
        this.encoder = encoder == null ? sensor.getEncoder() : encoder;
        
        if(autoCreateClassifiers != null && autoCreateClassifiers.booleanValue()) {
            factory.inference.classifiers(makeClassifiers(encoder));
        }
        
        completeDispatch((T)new int[] {});
        
        (LAYER_THREAD = new Thread("Sensor Layer Thread") {
            public void run() {
                LOGGER.debug("Sensor Layer started input stream processing.");
                
                // Applies "terminal" function, at this point the input stream is  "sealed".
                sensor.getOutputStream().forEach(intArray -> {
                    ((ManualInput)Layer.this.factory.inference).encoding(intArray);
                    Layer.this.compute((T)intArray);
                });
            }
        }).start(); 
        
        LOGGER.debug("Sensor Layer startOn called on thread {}", LAYER_THREAD);
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
     * Called by Network infrastructure during assembly, to pass inference
     * settings up the chain. Here we are careful not to overwrite the higher
     * level's stored classifiers with a possible null reference from a lower
     * layer.
     * 
     * @param inference
     * @return
     */
    Inference passInference(Inference inference) {
        // Preserve the top-most Observable's classifiers making sure they 
        // are not overwritten by null references in previous Observable's 
        // because the classifiers are always on the top most layer.
        NamedTuple temp = factory.inference.getClassifiers();
        currentInference = factory.inference = (ManualInput)inference;
        factory.inference.classifiers = temp;
        
        return inference;
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
        
        LOGGER.debug("Could not retrieve encoder from Layer: {}, and finally from the network", getName());
        return null;
    }
    
    /**
     * Internally called during assembly (see {@link Region#connect(Layer, Layer)}) to pass the field defining encoders
     * up the chain to where a Layer encapsulating CLAClassifiers can make
     * use of the encoder to define its classifiers.
     * 
     * @param encoder
     */
    void passEncoder(MultiEncoder encoder) {
        this.encoder = encoder;
        if(((Boolean)getParameters().getParameterByKey(KEY.AUTO_CLASSIFY))) {
            factory.inference.classifiers(makeClassifiers(encoder));
        }
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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

    /**
     * {@inheritDoc}
     */
    //    public List<String[]> getMetaInfo() {
    //        if(metaInfo == null) {
    //            throw new IllegalStateException("No meta information available. " + 
    //                "This is usually associated with a csv input type which has header info.");
    //        }
    //        return metaInfo.getHeader();
    //    }

    //////////////////////////////////////////////////////////////
    //          PRIVATE METHODS AND CLASSES BELOW HERE          //
    //////////////////////////////////////////////////////////////
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
        subscribers.add(0,getDelegateObserver());
        subscription = sequence.subscribe(getDelegateSubscriber());
        
        // The map of input types to transformers is no longer needed.
        observableDispatch.clear();
        observableDispatch = null;
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
        observableDispatch.put((Class<T>)ManualInput.class, factory.createSensorInputFunc(publisher));
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
    
    List<EncoderTuple> encoderTuples;
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
        spatialPooler.compute(connections, input, activeColumns, true, true);
        return activeColumns;
    }

    /**
     * Called internally to invoke the {@link TemporalMemory}
     * 
     * @param input
     * @return
     */
    private int[] temporalInput(int[] input) {
        ComputeCycle cc = temporalMemory.compute(connections, input, true);
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
    class FunctionFactory {
        ManualInput inference = new ManualInput();
        private boolean isDenseInput = true;

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
         * <p>
         * Emits an {@link Observable} which is transformed from a Map input
         * type to one that emits {@link Inference}s. 
         * </p><p>
         * Used to insert data directly into a Layer, bypassing an Encoder but
         * potentially "spoofing" Encoder data which typically would exist.
         * </p>
         */
        class Copy2Inference implements Transformer<ManualInput, ManualInput> {
            @Override
            public Observable<ManualInput> call(Observable<ManualInput> t1) {
                return t1.map(new Func1<ManualInput, ManualInput>() {
                    @Override
                    public ManualInput call(ManualInput t1) {
                        // Indicates a value that skips the encoding step
                        return (inference = t1).layerInput(t1);
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

        public Observable<ManualInput> createSensorInputFunc(Observable<T> in) {
            return in.ofType(ManualInput.class).compose(new Copy2Inference());
        }


        //////////////////////////////////////////////////////////////////////////////
        //                OBSERVABLE COMPONENT CREATION METHODS                     //
        //////////////////////////////////////////////////////////////////////////////
        public Func1<ManualInput, ManualInput> createSpatialFunc(final SpatialPooler sp) {
            isDenseInput = sp != null;

            return new Func1<ManualInput, ManualInput>() {
                @Override
                public ManualInput call(ManualInput t1) {
                    return t1.sdr(spatialInput(t1.getSDR())).activeColumns(t1.getSDR());
                }
            };
        }

        public Func1<ManualInput, ManualInput> createTemporalFunc(final TemporalMemory tm) {
            return new Func1<ManualInput, ManualInput>() {
                @Override
                public ManualInput call(ManualInput t1) {
                    if(isDenseInput) {
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
                Map<String, Object> inputMap = new HashMap<String, Object>();

                @Override
                public ManualInput call(ManualInput t1) {
                    Map<String, NamedTuple> ci = t1.getClassifierInput();
                    int recordNum = getRecordNum();
                    for(String key : ci.keySet()) {
                        NamedTuple inputs = ci.get(key);
                        inputMap.put("bucketIdx", inputs.get("bucketIdx"));
                        inputMap.put("actValue", inputs.get("inputValue"));
                        
                        CLAClassifier c = (CLAClassifier)t1.getClassifiers().get(key);
                        ClassifierResult<Object> result = c.compute(
                            recordNum, inputMap, t1.getSDR(), true, true);

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
                    return t1.anomalyScore(anomalyComputer.compute(t1.getSparseActives(), t1.getPreviousPrediction(), 0, 0));
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
