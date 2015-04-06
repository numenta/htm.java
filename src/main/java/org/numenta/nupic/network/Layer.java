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
 * Implementation of the biological section of a region in the neocortex. Here,
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

    @SuppressWarnings("unused")
    private Network parent;
    
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
    
    private FunctionFactory functions;

    private int[] activeColumns;
    private int[] sparseActives;
    private int[] previousPrediction;
    private int[] currentPrediction;
    private int cellsPerColumn;
    private int recordNum = -1;
    
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
        this.parent = n;
        this.params = p;

        if(n == null || p == null) return;
        connections = new Connections();
        this.params.apply(connections);
        
        this.autoCreateClassifiers = (Boolean)p.getParameterByKey(KEY.AUTO_CLASSIFY);

        functions = new FunctionFactory();
        
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

        this.params = params;
        this.encoder = e;
        this.spatialPooler = sp;
        this.temporalMemory = tm;
        this.autoCreateClassifiers = autoCreateClassifiers;
        this.anomalyComputer = a;

        // Make sure we have a valid parameters object
        if(params == null) {
            throw new IllegalArgumentException("No parameters specified.");
        }

        // Check to see if the Parameters include the encoder configuration.
        if(params.getParameterByKey(KEY.FIELD_ENCODING_MAP) == null && e != null) {
            throw new IllegalArgumentException("The passed in Parameters must contain a field encoding map " +
                "specified by org.numenta.nupic.Parameters.KEY.FIELD_ENCODING_MAP");
        }

        connections = new Connections();
        this.params.apply(connections);

        functions = new FunctionFactory();

        // Create Encoder hierarchy from definitions
        if(encoder != null) {
            encoder.addMultipleEncoders(
                (Map<String, Map<String, Object>>)params.getParameterByKey(
                    KEY.FIELD_ENCODING_MAP));

            functions.inference.classifiers(makeClassifiers(encoder));
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
        return this;
    }

    /**
     * Adds a {@link MultiEncoder} to this {@code Layer}
     * @param encoder   the added MultiEncoder
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> add(MultiEncoder encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * Adds a {@link SpatialPooler} to this {@code Layer}
     * @param sp        the added SpatialPooler
     * @return          this Layer instance (in fluent-style)
     */
    public Layer<T> add(SpatialPooler sp) {
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
     * 
     * @param t
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
            functions.inference.classifiers(makeClassifiers(encoder));
        }
        
        completeDispatch((T)new int[] {});
        
        (LAYER_THREAD = new Thread("Sensor Layer Thread") {
            public void run() {
                LOGGER.debug("Sensor Layer started input stream processing.");
                
                sensor.getOutputStream().forEach(intArray -> {
                    Layer.this.compute((T)intArray);
                });
            }
        }).start(); 
        
        LOGGER.debug("Sensor Layer startOn called on thread {}", LAYER_THREAD);
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
     * Returns the last computed {@link Inference} of this {@code Layer}
     * @return  the last computed inference.
     */
    public Inference getInference() {
        return currentInference;
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
        Observable<ManualInput> sequence = resolveObservableSequence(t);
        sequence = fillInSequence(sequence);
        subscribers.add(0,getDelegateObserver());
        subscription = sequence.subscribe(getDelegateSubscriber());
        
        //Clean up afterwards
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
        
        observableDispatch.put((Class<T>)Map.class, functions.createMultiMapFunc(publisher));
        observableDispatch.put((Class<T>)ManualInput.class, functions.createSensorInputFunc(publisher));
        observableDispatch.put((Class<T>)String[].class, functions.createEncoderFunc(publisher));
        observableDispatch.put((Class<T>)int[].class, functions.createVectorFunc(publisher));
        
        return observableDispatch;
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
                o = o.map(functions.createSpatialFunc(spatialPooler)).skip(skipCount.intValue());
            }else{
                o = o.map(functions.createSpatialFunc(spatialPooler));
            }
        }

        // Temporal Memory config
        if(temporalMemory != null) {
            o = o.map(functions.createTemporalFunc(temporalMemory));
        }

        // Classifier config
        if(autoCreateClassifiers != null && autoCreateClassifiers.booleanValue()) {
            o = o.map(functions.createClassifierFunc());
        }

        // Anomaly config
        if(anomalyComputer != null) {
            o = o.map(functions.createAnomalyFunc(anomalyComputer));
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
    private NamedTuple makeClassifiers(MultiEncoder encoder) {
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
    private class FunctionFactory {
        private ManualInput inference = new ManualInput();
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
                    List<EncoderTuple> encoderTuples = null;

                    @Override
                    public ManualInput call(Map t1) {
                        if(encoderTuples == null) {
                            encoderTuples = encoder.getEncoders(encoder);
                        }
                        
                        // Store the encoding
                        int[] encoding = encoder.encode(t1);
                        inference.sdr(encoding);

                        for(EncoderTuple t : encoderTuples) {
                            String name = t.getName();
                            Encoder e = t.getEncoder();
                            int bucketIdx = -1;
                            Object o = t1.get(name);
                            if(DateTime.class.isAssignableFrom(o.getClass())) {
                                bucketIdx = ((DateEncoder)e).getBucketIndices((DateTime)o)[0];
                            }else if(Number.class.isAssignableFrom(o.getClass())) {
                                bucketIdx = e.getBucketIndices((double)o)[0];
                            }else{
                                bucketIdx = e.getBucketIndices((String)o)[0];
                            }

                            int offset = t.getOffset();
                            int[] tempArray = new int[encoder.getWidth()];
                            System.arraycopy(encoding, offset, tempArray, 0, tempArray.length);

                            inference.getClassifierInput().put(name, 
                                new NamedTuple(new String[] { "name", "inputValue", "bucketIdx", "encoding" }, 
                                    name, o, bucketIdx, tempArray));
                        }

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
                        return inference = t1;
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
                    return t1.sdr(spatialInput(t1.getSDR()));
                }
            };
        }

        public Func1<ManualInput, ManualInput> createTemporalFunc(final TemporalMemory tm) {
            return new Func1<ManualInput, ManualInput>() {
                @Override
                public ManualInput call(ManualInput t1) {
                    if(isDenseInput) {
                        t1 = t1.sdr(sparseActives(ArrayUtils.where(t1.getSDR(), ArrayUtils.WHERE_1)));
                    }
                    int[] input = t1.getSDR();
                    int[] predict = temporalInput(input);

                    return t1.sdr(predict);
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
                        CLAClassifier c = (CLAClassifier)t1.getClassifiers().get(key);

                        NamedTuple inputs = ci.get(key);
                        inputMap.put("bucketIdx", inputs.get("bucketIdx"));
                        inputMap.put("actValue", inputs.get("inputValue"));

                        ClassifierResult<Object> result = c.compute(
                            recordNum, inputMap, t1.getSDR(), true, true);

                        t1.recordNum(recordNum).classify((String)inputs.get("name"), result);
                    }

                    return t1;
                }
            };
        }

        public Func1<ManualInput, ManualInput> createAnomalyFunc(final Anomaly an) {
            return new Func1<ManualInput, ManualInput>() {
                @Override
                public ManualInput call(ManualInput t1) {
                    t1.anomalyScore(anomalyComputer.compute(getSparseActives(), previousPrediction, 0, 0));

                    return t1;
                }
            };
        }
    }
}
