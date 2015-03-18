package org.numenta.nupic.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.Tuple;


public interface Network {
    public enum Mode { MANUAL, AUTO, REACTIVE };
    
    
    /**
     * Updates the network with count of the number of inputs to
     * process from all {@link SensorFactory}s which exist at the bottom 
     * of this {@code Network}'s graph of nodes.
     * 
     * @param count
     */
    public void run(int count);
    
    /**
     * Halts this {@code Network}, stopping all threads and closing
     * all {@link SensorFactory} connections to incoming data, freeing up 
     * any resources associated with the input connections.
     */
    public void halt();
    
    /**
     * Pauses all underlying {@code Network} nodes, maintaining any 
     * connections (leaving them open until they possibly time out).
     * Does nothing to prevent any sensor connections from timing out
     * on their own. 
     */
    public void pause();
    /**
     * If {@link Network.Mode} == {@link Mode#AUTO}, calling this 
     * method will start the main engine thread which pulls in data
     * from the connected {@link SensorFactory}(s).
     * 
     * <em>Warning:</em> Calling this method with any other Mode than 
     * {@link Mode#AUTO} will result in an {@link UnsupportedOperationException}
     * being thrown.
     */
    public default void start() {
        throw new UnsupportedOperationException("Calling start is not valid for " +
            getMode());
    }
    /**
     * Returns the current {@link Mode} with which this {@link Network} is 
     * currently configured.
     * 
     * @return
     */
    public Mode getMode();
    /**
     * Returns a {@link Iterator} capable of walking the tree of regions
     * from the root {@link Region} down through all the child Regions. In turn,
     * a {@link Region} may be queried for a {@link Iterator} which will return
     * an iterator capable of traversing the Region's contained {@link Node}s.
     * 
     * @return
     */
    public default Iterator<Region> iterator() {
        return getRegions().iterator();
    }
    /**
     * Returns a {@link List} view of the contained {@link Region}s.
     * @return
     */
    public List<Region> getRegions();
    
    /**
     * Creates and returns an implementation of {@link Network}
     * 
     * @param parameters
     * @return
     */
    public static Network create(Parameters parameters) {
        return new NetworkImpl(parameters);
    }
    
    /**
     * Adds a {@link Region} to this {@code Network}
     * @param region
     * @return
     */
    public Network add(Region region);
    
    /**
     * Creates and returns a child {@link Region} of this {@code Network}
     * 
     * @return
     */
    public Region createRegion();
    
    /**
     * Creates a {@link Layer} to hold algorithmic components
     * 
     * @param p
     * @return
     */
    public Layer createLayer(Parameters p);
    
    /**
     * Creates a {@link Layer} using the default {@link Network} level parameters
     * @return
     */
    public Layer createLayer();
    
    /**
     * Returns the network-level {@link Parameters}.
     * @return
     */
    public Parameters getParameters();
    
    /**
     * Sets the reference to this {@code Network}'s Sensor
     * @param encoder
     */
    public void setSensor(HTMSensor<?> encoder);
    
    /**
     * Returns the encoder present in one of this {@code Network}'s
     * {@link Sensor}s
     * 
     * @return
     */
    public HTMSensor<?> getSensor();
    
    /**
     * Sets the reference to this {@code Network}'s classifier.
     * @param classifier
     */
    public void setClassifier(CLAClassifier classifier);
    
    /**
     * Returns the classifier used by this {@code Network}
     * @return
     */
    public CLAClassifier getClassifier();
    
    
    /////////////////////////////////////////////////////////////////////////
    //                   Internal Interface Definitions                    //
    /////////////////////////////////////////////////////////////////////////
    
    /**
     * Internal type to handle connectivity and locality within a given
     * graph or tree of {@link Network} components.
     * 
     * public int[] encode(T inputData)
     * public void compute(Connections c, int[] inputVector, int[] activeArray, boolean learn, boolean stripNeverLearned)
     * public ComputeCycle compute(Connections connections, int[] activeColumns, boolean learn)
     * public <T> ClassifierResult<T> compute(int recordNum, Map<String, Object> classification, int[] patternNZ, boolean learn, boolean infer)
     * public abstract double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp);
     */
    public interface Node<T> {
        
        /**
         * Identifies the type contained within a given {@link Node}
         */
        public enum Type { SP, TM, SENSOR, CLASSIFIER, ANOMALY, LAYER, REGION;
            /**
             * Returns the {@link Node.Type} associated with the specified {@link Class}
             * @param class1
             * @return
             */
            public static Type forClass(Class<? extends Object> class1) {
                if(SpatialPooler.class.isAssignableFrom(class1)) {
                    return SP;
                }else if(TemporalMemory.class.isAssignableFrom(class1)) {
                    return TM;
                }else if(Sensor.class.isAssignableFrom(class1)) {
                    return SENSOR;
                }else if(CLAClassifier.class.isAssignableFrom(class1)) {
                    return CLASSIFIER;
                }else if(Anomaly.class.isAssignableFrom(class1)) {
                    return ANOMALY;
                }else if(Layer.class.isAssignableFrom(class1)) {
                    return LAYER;
                }else if(Region.class.isAssignableFrom(class1)) {
                    return REGION;
                }
                return null;
            }
        }
        
        /**
         * Returns this Node's {@link Node.Type}
         * @return
         */
        public Type type();
        /**
         * Returns the contained algorithmic object.
         * @return
         */
        public T getElement();
        /**
         * Connects this {@code Node} to an incoming Node's
         * output.
         * @param node
         */
        public void connect(Node<T> node);
        
        @SuppressWarnings("unchecked")
        public default <P> P process(Tuple argList) {
            switch(type()) {
                case ANOMALY:
                    
                    return (P)new Double(((Anomaly)getElement()).compute(
                        (int[])argList.get(0), 
                        (int[])argList.get(1), 
                        ((Number)argList.get(2)).doubleValue(), 
                        ((Number)argList.get(3)).longValue()));
                    
                case CLASSIFIER:
                    
                    return (P)((CLAClassifier)getElement()).compute(
                        (int)argList.get(0),
                        (Map<String, Object>)argList.get(1),
                        (int[])argList.get(2),
                        (boolean)argList.get(3),
                        (boolean)argList.get(4));
                    
                case SENSOR:
                    
                    HTMSensor<?> sensor = (HTMSensor<?>)getElement();
                    List<int[]> output = new ArrayList<>();
                    sensor.input(
                        (String[])argList.get(0), 
                        sensor.getFieldNames(), 
                        sensor.getFieldTypes(),
                        output, 
                        sensor.getInputStream().isParallel());
                    return (P)output.get(0);
                    
                case LAYER:
                    
                    return (P)((Layer)getElement()).compute((int[])argList.get(0));
                    
                case REGION:
                    
                    return (P)((Region)getElement()).compute((int[])argList.get(0));
                    
                case SP:
                    
                    SpatialPooler sp = (SpatialPooler)getElement();
                    sp.compute(
                        (Connections)argList.get(0),
                        (int[])argList.get(1),
                        (int[])argList.get(2),
                        (boolean)argList.get(3),
                        (boolean)argList.get(4));
                    return (P)(int[])argList.get(2);
                    
                case TM:
                    
                    return (P)((TemporalMemory)getElement()).compute(
                        (Connections)argList.get(0), (int[])argList.get(1), (boolean)argList.get(2));
                    
                default:
                    break;
            }
            
            throw new IllegalArgumentException(
                "Argument Tuple did not contain the correct number of parameters: " + 
                    (argList == null ? null : argList.size()));
        }
    }
    
    class NodeImpl<T> implements Node<T> {
        private T element;
        private Type type;
        
        public NodeImpl(T t) {
            this.element = t;
            this.type = Type.forClass(element.getClass());
        }

        @Override
        public org.numenta.nupic.network.Network.Node.Type type() {
            return type;
        }

        @Override
        public T getElement() {
            return element;
        }

        @Override
        public void connect(Node<T> node) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    /**
     * Implementation of the {@link Network} interface.
     * 
     * @author David Ray
     * @see Network
     */
    public static class NetworkImpl implements Network {
        private Parameters parameters;
        private HTMSensor<?> sensor;
        private CLAClassifier classifier;
        
        private List<Region> regions = new ArrayList<>();
        
        /**
         * Creates a new {@link NetworkImpl}
         * @param parameters
         */
        public NetworkImpl(Parameters parameters) {
            this.parameters = parameters;
        }
        

        @Override
        public void run(int count) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void halt() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void pause() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Mode getMode() {
            // TODO Auto-generated method stub
            return null;
        }
        
        /**
         * Adds a {@link Region} to this {@code Network}
         * @param region
         * @return
         */
        @Override
        public Network add(Region region) {
            regions.add(region);
            return this;
        }

        @Override
        public List<Region> getRegions() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Region createRegion() {
            Region r = new Region(this);
            return r;
        }
        
        @Override
        public Layer createLayer() {
            return new Layer(this, this.parameters).using(new Connections());
        }
        
        @Override
        public Layer createLayer(Parameters p) {
            return new Layer(this, p).using(new Connections());
        }

        @Override
        public Parameters getParameters() {
            return parameters;
        }
        
        /**
         * Sets the reference to this {@code Network}'s encoder
         * @param encoder
         */
        @Override
        public void setSensor(HTMSensor<?> sensor) {
            this.sensor = sensor;
        }
        
        /**
         * Returns the encoder present in one of this {@code Network}'s
         * {@link Sensor}s
         * 
         * @return
         */
        @Override
        public HTMSensor<?> getSensor() {
            return sensor;
        }
        
        /**
         * Sets the reference to this {@code Network}'s classifier.
         * @param classifier
         */
        @Override
        public void setClassifier(CLAClassifier classifier) {
            this.classifier = classifier;
        }
        
        /**
         * Returns the classifier used by this {@code Network}
         * @return
         */
        @Override
        public CLAClassifier getClassifier() {
            return classifier;
        }
        
    }
     
}
