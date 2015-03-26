package org.numenta.nupic.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.EncoderTuple;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.Network.Node;
import org.numenta.nupic.network.Network.NodeImpl;
import org.numenta.nupic.research.ComputeCycle;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MethodSignature;



public class Layer {
    
    private static final int SENSOR_SLOT = 0;
    private static final int SPATIAL_POOLER_SLOT = 1;
    private static final int TEMPORAL_MEMORY_SLOT = 2;
    private static final int CLASSIFIER_SLOT = 3;
    private static final int ANOMALY_SLOT = 4;    
    
    private List<Node<?>> nodeList;
    private Parameters parameters;
    private Connections connections;
    private Network network;
    private Region region;
    
    /**
     * Constructs a new {@code Layer}
     * @param network
     * @param parameters
     */
    public Layer(Network network, Parameters parameters) {
        if(parameters == null) {
            throw new IllegalArgumentException(
                "Cannot construct a Layer without previously " +
                    "configuring Parameters! --> parameters = null");
        }
        this.network = network;
        this.parameters = network.getParameters().copy().union(parameters);
        this.nodeList = new ArrayList<>();
        for(int i = 0;i <= ANOMALY_SLOT;i++) {
            nodeList.add(null);
        }
    }
    
    /**
     * Sets a reference to this {@code Layer}'s {@link Region}
     * @param region
     */
    public void setRegion(Region region) {
        this.region = region;
    }
    
    /**
     * * public int[] encode(T inputData)
     * public void compute(Connections c, int[] inputVector, int[] activeArray, boolean learn, boolean stripNeverLearned)
     * public ComputeCycle compute(Connections connections, int[] activeColumns, boolean learn)
     * public <T> ClassifierResult<T> compute(int recordNum, Map<String, Object> classification, int[] patternNZ, boolean learn, boolean infer)
     * public abstract double compute(int[] activeColumns, int[] predictedColumns, double inputValue, long timestamp);
     */
    int[] sdr = null;
    
    int[] ddr = null;
    ComputeCycle cycle = null;
    MethodSignature tuple = new MethodSignature(6);
    boolean isLearning = true;
    int recordNum = 0;
    Map<String, Object> classification = new LinkedHashMap<String, Object>();
    
    public <T, K> K compute(T input) {
        int[] inputVector = null;
        Node<?> n = nodeList.get(SENSOR_SLOT);
        ((HTMSensor)n.getElement()).getOutputStream();
        ddr = new int[this.connections.getNumColumns()];
        int idx = -1;
        while(++idx < 5 && (n = nodeList.get(idx)) == null);
        
        switch(idx) {
            case SENSOR_SLOT: // MultiEncoder
                inputVector = n.process(tuple.setParams(input));
                n = nodeList.get(SPATIAL_POOLER_SLOT);
                
            case SPATIAL_POOLER_SLOT: // Spatial Pooler
                if(n != null) {
                    inputVector = inputVector == null ? (int[])input : inputVector;
                    n.process(tuple.setParams(connections, inputVector, ddr, isLearning, true));
                }
                n = nodeList.get(TEMPORAL_MEMORY_SLOT);
                
            case TEMPORAL_MEMORY_SLOT: // TemporalMemory
                if(n != null) {
                    ddr = ddr == null ? (int[])input : ddr;
                    cycle = n.process(tuple.setParams(connections, sdr = ArrayUtils.where(ddr, ArrayUtils.WHERE_1), isLearning));
                }
                n = nodeList.get(CLASSIFIER_SLOT);
                
            case CLASSIFIER_SLOT: // CLAClassifier
                if(n != null) {
                    HTMSensor<?> sensor = (HTMSensor<?>)nodeList.get(SENSOR_SLOT).getElement();
                    Map<String, Object> encoderInputMap = sensor.getInputMap();
                    MultiEncoder me = sensor.getEncoder();
                    
                    Map<String, Object> inputMap = me.getEncoders(me).stream()
                        .collect(
                            Collectors.toMap(EncoderTuple::getName, e -> {
                                Object in = encoderInputMap.get(e.getName());
                                if(in instanceof DateTime) {
                                    return ((DateEncoder)e.getEncoder()).getBucketIndices((DateTime)in);
                                }else if(in instanceof Double) {
                                    return e.getEncoder().getBucketIndices((double)in);
                                }else{
                                    return e.getEncoder().getBucketIndices(in.toString());
                                }
                            }));
                    n.process(tuple.setParams(recordNum++, inputMap, sdr, true, true));
                }
        }
        return inputVector == null ? (K)input : (K)inputVector;
    }
    
    /**
     * Returns this Region's <em>effective</em> {@link Parameters}.
     * Effective parameters are the <em>union</em> of parameters set at 
     * the parent network level (if any) together with any Parameters 
     * which may have been set at this Region level.
     * 
     * The union of and establishment of this {@code Region}'s Parameters
     * is conducted at construction time.
     *  
     * @return  this {@code Region}'s effective {@link Parameters}
     */
    public Parameters getParameters() {
        return parameters;
    }
    
    public Iterator<Node<?>> iterator() {
        return nodeList.iterator();
    }
    
    private void preAdd(Node<?> n, int slot) {
        nodeList.add(slot, n);
    }
    
    public Layer connect(Layer l) {
        return this;
    }
    
    public <T> Layer add(Sensor<T> inputSensor) {
        this.network.setSensor((HTMSensor<T>)inputSensor);
        this.parameters.setInputDimensions(
            new int[] { ((HTMSensor<T>)inputSensor).getEncoder().getWidth() }
        );
        this.parameters.apply(this.connections);
        preAdd(new NodeImpl<Sensor<T>>(inputSensor), SENSOR_SLOT);
        return this;
    }
    
    public Layer add(SpatialPooler sp) {
        preAdd(new NodeImpl<SpatialPooler>(sp), SPATIAL_POOLER_SLOT);
        if(this.connections != null) sp.init(this.connections);
        return this;
    }
    
    public Layer add(TemporalMemory tm) {
        preAdd(new NodeImpl<TemporalMemory>(tm), TEMPORAL_MEMORY_SLOT);
        if(this.connections != null) tm.init(this.connections);
        return this;
    }
    
    public Layer add(CLAClassifier classifier) {
        this.network.setClassifier(classifier);
        preAdd(new NodeImpl<CLAClassifier>(classifier), CLASSIFIER_SLOT);
        return this;
    }
    
    public Layer add(Anomaly anomaly) {
        this.
        preAdd(new NodeImpl<Anomaly>(anomaly), ANOMALY_SLOT);
        return this;
    }
    
    /**
     * Called internally following the setting of the {@link Connections}
     * object, to set local helper vars.
     */
    private void initLocalVars() {
        if(connections == null) {
            throw new IllegalStateException("Connections object unset.");
        }
//        inputVector = new int[connections.getNumInputs()];
//        ddr = new int[connections.getNumColumns()];
    }
    
    /**
     * Sets the {@link Connections} object to be used by this {@code Layer}
     * @param connections
     * @return
     */
    public Layer using(Connections connections) {
        this.connections = connections;
        this.parameters.apply(this.connections);
        if(nodeList.get(SPATIAL_POOLER_SLOT) != null) {
            ((SpatialPooler)nodeList.get(SPATIAL_POOLER_SLOT)).init(this.connections);
        }
        if(nodeList.get(TEMPORAL_MEMORY_SLOT) != null) {
            ((SpatialPooler)nodeList.get(TEMPORAL_MEMORY_SLOT)).init(this.connections);
        }
        
        initLocalVars();
        
        return this;
    }
}
