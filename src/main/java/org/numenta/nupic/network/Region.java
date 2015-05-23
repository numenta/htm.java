package org.numenta.nupic.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.network.sensor.Sensor;

import rx.Observable;
import rx.Subscriber;

 

/**
 * Regions are collections of {@link Layer}s, which are in turn collections
 * of algorithmic components. Regions can be connected to each other to establish
 * a hierarchy of processing. To connect one Region to another, typically one 
 * would do the following:
 * </p><p>
 * <pre>
 *      Parameters p = Parameters.getDefaultParameters(); // May be altered as needed
 *      Network n = Network.create("Test Network", p);
 *      Region region1 = n.createRegion("r1"); // would typically add Layers to the Region after this
 *      Region region2 = n.createRegion("r2"); 
 *      region1.connect(region2);
 * </pre>
 * <b>--OR--</b>
 * <pre>
 *      n.connect(region1, region2);
 * </pre>
 * <b>--OR--</b>
 * <pre>
 *      Network.lookup("r1").connect(Network.lookup("r2"));
 * </pre>    
 * 
 * @author cogmission
 *
 */
public class Region {
    private Network network;
    private Map<String, Layer<Inference>> layers = new HashMap<>();
    private Observable<Inference> regionObservable;
    private Layer<?> tail;
    private Layer<?> head;
    
    /** Marker flag to indicate that assembly is finished and Region initialized */
    private boolean assemblyClosed;
    
    /** Temporary variables used to determine endpoints of observable chain */
    private HashSet<Layer<Inference>> sources;
    private HashSet<Layer<Inference>> sinks;
    
    
    private String name;
    
    public Region(String name, Network container) {
        this.name = name;
        this.network = container;
    }
    
    /**
     * Used to manually input data into a {@link Region}, the other way 
     * being the call to {@link Region#start()} for a Region that contains
     * a {@link Layer} which in turn contains a {@link Sensor} <em>-OR-</em>
     * subscribing a receiving Region to this Region's output Observable.
     * 
     * @param input One of (int[], String[], {@link ManualInput}, or Map<String, Object>)
     */
    @SuppressWarnings("unchecked")
    public <T> void compute(T input) {
        ((Layer<T>)tail).compute(input);
    }
    
    /**
     * 1. From the second added layer on, we connect the previous last layer to the incoming
     * layer via subscription to the incoming layer's observable.
     * 
     * 2. We add a step in the incoming layer's Observable, creating another observable which passes
     * the incoming layer's Inference object to the next higher up (the previous last layer). This
     * has the effect of passing the bottom most Inference all the way up the chain of layers. In this
     * way, the layer containing the Sensor can propagate the encoder inputs and bucket indexes up the chain
     * to the top-most layer which contains the classifier which uses it. Note: this will also work for 
     * "spoofed" out entries which don't come from an encoder but do come from manually composed ManualInputs.
     * 
     * 3. If the layer being added is the one with the Sensor, we get the Sensor from the layer and set it on the 
     * Network object - so that it may be referenced from any Region.
     * 
     * 4. We then call passEncoder which passes the lower Layer's contained Sensor's Encoder if 
     * it exists. 
     * 
     * 5. If the "receiving" layer is configured to autoCreateClassifiers, the passEncoder method 
     * will then create classifiers for each field in the passed encoder.
     * 
     * @param l
     * @return
     */
    @SuppressWarnings("unchecked")
    public Region add(Layer<?> l) {
        // Set the sensor reference for global access.
        if(l.hasSensor()) {
            network.setSensor(l.getSensor());
            network.setEncoder(l.getSensor().getEncoder());
            tail = l;
        }
        
        l.name(name.concat(":").concat(l.getName()));
        layers.put(l.getName(), (Layer<Inference>)l);
        
        return this;
    }
    
    /**
     * Returns the String identifier for this {@code Region}
     * @return
     */
    public String getName() {
        return name;
    }
    
    public Observable<Inference> observe() {
        completeAssembly();
        return regionObservable;
    }
    
    public void start() {
        completeAssembly();
        if(tail.hasSensor()) {
            tail.start();
        }
    }
    
    public Region connect(Region inputRegion) {
        completeAssembly();
        return this;
    }
    
    /**
     * Connects two layers to each other in a unidirectional fashion 
     * with "toLayerName" representing the receiver or "sink" and "fromLayerName"
     * representing the sender or "source".
     * 
     * @param toLayerName       the name of the sink layer
     * @param fromLayerName     the name of the source layer
     * @return
     */
    public Region connect(String toLayerName, String fromLayerName) {
        if(sources == null) {
            sources = new HashSet<Layer<Inference>>();
            sinks = new HashSet<Layer<Inference>>();
        }
        
        Layer<Inference> in = lookup(toLayerName);
        Layer<Inference> out = lookup(fromLayerName);
        if(in == null) {
            throw new IllegalArgumentException("Could not lookup (to) Layer with name: " + toLayerName);
        }else if(out == null){
            throw new IllegalArgumentException("Could not lookup (from) Layer with name: " + fromLayerName);
        }
        
        // Set source's pointer to its next Layer.
        out.next(in);
        // Set the sink's pointer to its previous Layer
        in.previous(out);
        // Connect out to in
        connect(in, out);
        
        return this;
    }
    
    /**
     * Does a straight associative lookup by first creating a composite
     * key containing this {@code Region}'s name concatenated with the specified
     * {@link Layer}'s name, and returning the result.
     * 
     * @param layerName
     * @return
     */
    public Layer<Inference> lookup(String layerName) {
        return layers.get(name.concat(":").concat(layerName));
    }
    
    /**
     * Called by {@link #start()}, {@link #observe()} and {@link #connect(Region)}
     * to finalize the internal chain of {@link Layer}s contained by this {@code Region}.
     * This method assigns the head and tail Layers and composes the {@link Observable}
     * which offers this Region's emissions to any upstream {@link Region}s.
     */
    private void completeAssembly() {
        if(!assemblyClosed) {
            if(tail == null) {
                Set<Layer<Inference>> temp = new HashSet<Layer<Inference>>(sources);
                temp.removeAll(sinks);
                if(temp.size() != 1) {
                    throw new IllegalArgumentException("Detected misconfigured Region too many or too few sinks.");
                }
                tail = temp.iterator().next();
            }
            if(head == null) {
                Set<Layer<Inference>> temp = new HashSet<Layer<Inference>>(sinks);
                temp.removeAll(sources);
                if(temp.size() != 1) {
                    throw new IllegalArgumentException("Detected misconfigured Region too many or too few sources.");
                }
                head = temp.iterator().next();
                
                regionObservable = head.observe();
            }
            assemblyClosed = true;
        }
    }
    
    /**
     * Called internally to "connect" two {@link Layer} {@link Observable}s
     * taking care of other connection details such as passing the inference
     * up the chain and any possible encoder.
     * 
     * @param source
     * @param sink
     */
    <I extends Layer<Inference>, O extends Layer<Inference>> void connect(I in, O out) {
        //Pass the Inference object from lowest to highest layer.
        // The passing of the Inference is done in an Observable so that
        // it happens on every pass through the chain, while the Encoder
        // is only passed once, so the method is called directly here. We
        // don't check to see if it exists or is null, since it is already
        // null in the receiver, it doesn't hurt anything.
        Observable<Inference> o = out.observe().map(i -> {
            in.passInference(i);
            return i;
        });
        in.passEncoder(out.getEncoder());
        
        sources.add(out);
        sinks.add(in);
        
        o.subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                in.compute(i);
            }
        });
    }
    
}
