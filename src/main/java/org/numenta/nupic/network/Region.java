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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.network.sensor.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;

 

/**
 * <p>
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
public class Region implements Persistable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Region.class);
    
    private Network parentNetwork;
    private Region upstreamRegion;
    private Region downstreamRegion;
    private Layer<?> tail;
    private Layer<?> head;
    
    private Map<String, Layer<Inference>> layers = new HashMap<>();
    private transient Observable<Inference> regionObservable;
        
    /** Marker flag to indicate that assembly is finished and Region initialized */
    private boolean assemblyClosed;
    
    /** stores the learn setting */
    private boolean isLearn = true;
    
    /** Temporary variables used to determine endpoints of observable chain */
    private HashSet<Layer<Inference>> sources;
    private HashSet<Layer<Inference>> sinks;
    
    /** Stores the overlap of algorithms state for {@link Inference} sharing determination */
    byte flagAccumulator = 0;
    /** 
     * Indicates whether algorithms are repeated, if true then no, if false then yes
     * (for {@link Inference} sharing determination) see {@link Region#configureConnection(Layer, Layer)} 
     * and {@link Layer#getMask()}
     */
    boolean layersDistinct = true;
    
    private Object input;
    
    private String name;
    
    /**
     * Constructs a new {@code Region}
     * 
     * Warning: name cannot be null or empty
     * 
     * @param name          A unique identifier for this Region (uniqueness is enforced)
     * @param network       The containing {@link Network} 
     */
    public Region(String name, Network network) {
        if(name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name may not be null or empty. " +
                "...not that anyone here advocates name calling!");
        }
        
        this.name = name;
        this.parentNetwork = network;
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Region preSerialize() {
        layers.values().stream().forEach(l -> l.preSerialize());
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Region postDeSerialize() {
        layers.values().stream().forEach(l -> l.postDeSerialize());
        
        // Connect Layer Observable chains (which are transient so we must 
        // rebuild them and their subscribers)
        if(isMultiLayer()) {
            Layer<Inference> curr = (Layer<Inference>)head;
            Layer<Inference> prev = curr.getPrevious();
            do {
                connect(curr, prev); 
            } while((curr = prev) != null && (prev = prev.getPrevious()) != null);
        }
        return this;
    }

    /**
     * Sets the parent {@link Network} of this {@code Region}
     * @param network
     */
    public void setNetwork(Network network) {
        this.parentNetwork = network;
        for(Layer<?> l : layers.values()) {
            l.setNetwork(network);
            // Set the sensor & encoder reference for global access.
            if(l.hasSensor() && network != null) {
                network.setSensor(l.getSensor());
                network.setEncoder(l.getSensor().getEncoder());
            }else if(network != null && l.getEncoder() != null) {
                network.setEncoder(l.getEncoder());
            }
        }
    }
   
    /**
     * Returns a flag indicating whether this {@code Region} contain multiple
     * {@link Layer}s.
     * 
     * @return  true if so, false if not.
     */
    public boolean isMultiLayer() {
        return layers.size() > 1;
    }
    
    /**
     * Closes the Region and completes the finalization of its assembly.
     * After this call, any attempt to mutate the structure of a Region
     * will result in an {@link IllegalStateException} being thrown.
     * 
     * @return
     */
    public Region close() {
        if(layers.size() < 1) {
            LOGGER.warn("Closing region: " + name + " before adding contents.");
            return this;
        }
        
        completeAssembly();
        
        Layer<?> l = tail;
        do {
            l.close();
        }while((l = l.getNext()) != null);
        
        return this;
    }
    
    /**
     * Returns a flag indicating whether this {@code Region} has had
     * its {@link #close} method called, or not.
     * 
     * @return
     */
    public boolean isClosed() {
        return assemblyClosed;
    }
    
    /**
     * Sets the learning mode.
     * @param isLearn
     */
    public void setLearn(boolean isLearn) {
        this.isLearn = isLearn;
        Layer<?> l = tail;
        while(l != null) {
            l.setLearn(isLearn);
            l = l.getNext();
        }
    }
    
    /**
     * Returns the learning mode setting.
     * @return
     */
    public boolean isLearn() {
        return isLearn;
    }
    
    /**
     * Used to manually input data into a {@link Region}, the other way 
     * being the call to {@link Region#start()} for a Region that contains
     * a {@link Layer} which in turn contains a {@link Sensor} <em>-OR-</em>
     * subscribing a receiving Region to this Region's output Observable.
     * 
     * @param input One of (int[], String[], {@link ManualInput}, or Map&lt;String, Object&gt;)
     */
    @SuppressWarnings("unchecked")
    public <T> void compute(T input) {
        if(!assemblyClosed) {
            close();
        }
        this.input = input;
        ((Layer<T>)tail).compute(input);
    }
    
    /**
     * Returns the current input into the region. This value may change
     * after every call to {@link Region#compute(Object)}.
     * 
     * @return
     */
    public Object getInput() {
        return input;
    }
    
    /**
     * Adds the specified {@link Layer} to this {@code Region}. 
     * @param l
     * @return
     * @throws IllegalStateException if Region is already closed
     * @throws IllegalArgumentException if a Layer with the same name already exists.
     */
    @SuppressWarnings("unchecked")
    public Region add(Layer<?> l) {
        if(assemblyClosed) {
            throw new IllegalStateException("Cannot add Layers when Region has already been closed.");
        }
        
        if(sources == null) {
            sources = new HashSet<Layer<Inference>>();
            sinks = new HashSet<Layer<Inference>>();
        }
        
        // Set the sensor reference for global access.
        if(l.hasSensor() && parentNetwork != null) {
            parentNetwork.setSensor(l.getSensor());
            parentNetwork.setEncoder(l.getSensor().getEncoder());
        }
        
        String layerName = name.concat(":").concat(l.getName());
        if(layers.containsKey(layerName)) {
            throw new IllegalArgumentException("A Layer with the name: " + l.getName() + " has already been added to this Region.");
        }
        
        l.name(layerName);
        layers.put(l.getName(), (Layer<Inference>)l);
        l.setRegion(this);
        l.setNetwork(parentNetwork);
        
        return this;
    }
    
    /**
     * Returns the String identifier for this {@code Region}
     * @return
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns an {@link Observable} which can be used to receive
     * {@link Inference} emissions from this {@code Region}
     * @return
     */
    public Observable<Inference> observe() {
        if(regionObservable == null && !assemblyClosed) {
            close();
        }
        
        if(head.isHalted() || regionObservable == null) {
            regionObservable = head.observe();
        }
        
        return regionObservable;
    }
    
    /**
     * Calls {@link Layer#start()} on this Region's input {@link Layer} if 
     * that layer contains a {@link Sensor}. If not, this method has no 
     * effect.
     * 
     * @return flag indicating that thread was started
     */
    public boolean start() {
        if(!assemblyClosed) {
            close();
        }
        
        if(tail.hasSensor()) {
            LOGGER.info("Starting Region [" + getName() + "] input Layer thread.");
            tail.start();
            return true;
        }else{
            LOGGER.warn("Start called on Region [" + getName() + "] with no effect due to no Sensor present.");
        }
        
        return false;
    }
    
    /**
     * Calls {@link Layer#restart(boolean)} on this Region's input {@link Layer} if
     * that layer contains a {@link Sensor}. If not, this method has no effect. If
     * "startAtIndex" is true, the Network will start at the last saved index as 
     * obtained from the serialized "recordNum" field; if false then the Network
     * will restart from 0.
     * 
     * @param startAtIndex      flag indicating whether to start from the previous save
     *                          point or not. If true, this region's Network will start
     *                          at the previously stored index, if false then it will 
     *                          start with a recordNum of zero.
     * @return  flag indicating whether the call to restart had an effect or not.
     */
    public boolean restart(boolean startAtIndex) {
        if(!assemblyClosed) {
            return start();
        }
        
        if(tail.hasSensor()) {
            LOGGER.info("Re-Starting Region [" + getName() + "] input Layer thread.");
            tail.restart(startAtIndex);
            return true;
        }else{
            LOGGER.warn("Re-Start called on Region [" + getName() + "] with no effect due to no Sensor present.");
        }
        
        return false;
    }
    
    /**
     * Returns an {@link rx.Observable} operator that when subscribed to, invokes an operation
     * that stores the state of this {@code Network} while keeping the Network up and running.
     * The Network will be stored at the pre-configured location (in binary form only, not JSON).
     * 
     * @return  the {@link CheckPointOp} operator 
     */
    CheckPointOp<byte[]> getCheckPointOperator() {
        LOGGER.debug("Region [" + getName() + "] CheckPoint called at: " + (new DateTime()));
        if(tail != null) {
            return tail.getCheckPointOperator();
            
        }else{
            close();
            return tail.getCheckPointOperator();
        }
    }
    
    /**
     * Stops each {@link Layer} contained within this {@code Region}
     */
    public void halt() {
        LOGGER.debug("Halt called on Region [" + getName() + "]");
        if(tail != null) {
            tail.halt();
        }else{
            close();
            tail.halt();
        }
        LOGGER.debug("Region [" + getName() + "] halted.");
    }
    
    /**
     * Returns a flag indicating whether this Region has a Layer
     * whose Sensor thread is halted.
     * @return  true if so, false if not
     */
    public boolean isHalted() {
        if(tail != null) {
            return tail.isHalted();
        }
        return false;
    }
    
    /**
     * Finds any {@link Layer} containing a {@link TemporalMemory} 
     * and resets them.
     */
    public void reset() {
        for(Layer<?> l : layers.values()) {
            if(l.hasTemporalMemory()) {
                l.reset();
            }
        }
    }
    
    /**
     * Resets the recordNum in all {@link Layer}s.
     */
    public void resetRecordNum() {
        for(Layer<?> l : layers.values()) {
            l.resetRecordNum();
        }
    }
    
    /**
     * Connects the output of the specified {@code Region} to the 
     * input of this Region
     * 
     * @param inputRegion   the Region who's emissions will be observed by 
     *                      this Region.
     * @return
     */
    Region connect(Region inputRegion) {
        inputRegion.observe().subscribe(new Observer<Inference>() {
            ManualInput localInf = new ManualInput();
            
            @Override public void onCompleted() {
            	tail.notifyComplete();
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @SuppressWarnings("unchecked")
            @Override public void onNext(Inference i) {
                localInf.sdr(i.getSDR()).recordNum(i.getRecordNum()).classifierInput(i.getClassifierInput()).layerInput(i.getSDR());
                if(i.getSDR().length > 0) {
                    ((Layer<Inference>)tail).compute(localInf);
                }
            }
        });
        // Set the upstream region
        this.upstreamRegion = inputRegion;
        inputRegion.downstreamRegion = this;
        
        return this;
    }
    
    /**
     * Returns this {@code Region}'s upstream region,
     * if it exists.
     * 
     * @return
     */
    public Region getUpstreamRegion() {
        return upstreamRegion;
    }
    
    /**
     * Returns the {@code Region} that receives this Region's
     * output.
     * 
     * @return
     */
    public Region getDownstreamRegion() {
        return downstreamRegion;
    }
    
    /**
     * Returns the top-most (last in execution order from
     * bottom to top) {@link Layer} in this {@code Region}
     * 
     * @return
     */
    public Layer<?> getHead() {
        return this.head;
    }
    
    /**
     * Returns the bottom-most (first in execution order from
     * bottom to top) {@link Layer} in this {@code Region}
     * 
     * @return
     */
    public Layer<?> getTail() {
        return this.tail;
    }
    
    /**
     * Connects two layers to each other in a unidirectional fashion 
     * with "toLayerName" representing the receiver or "sink" and "fromLayerName"
     * representing the sender or "source".
     * 
     * This method also forwards shared constructs up the connection chain
     * such as any {@link Encoder} which may exist, and the {@link Inference} result
     * container which is shared among layers.
     * 
     * @param toLayerName       the name of the sink layer
     * @param fromLayerName     the name of the source layer
     * @return
     * @throws IllegalStateException if Region is already closed
     */
    @SuppressWarnings("unchecked")
    public Region connect(String toLayerName, String fromLayerName) {
        if(assemblyClosed) {
            throw new IllegalStateException("Cannot connect Layers when Region has already been closed.");
        }
        
        Layer<Inference> in = (Layer<Inference>)lookup(toLayerName);
        Layer<Inference> out = (Layer<Inference>)lookup(fromLayerName);
        if(in == null) {
            throw new IllegalArgumentException("Could not lookup (to) Layer with name: " + toLayerName);
        }else if(out == null){
            throw new IllegalArgumentException("Could not lookup (from) Layer with name: " + fromLayerName);
        }
        
        // Set source's pointer to its next Layer --> (sink : going upward).
        out.next(in);
        // Set the sink's pointer to its previous Layer --> (source : going downward)
        in.previous(out);
        // Connect out to in
        configureConnection(in, out);
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
    public Layer<?> lookup(String layerName) {
        if(layerName.indexOf(":") != -1) {
            return layers.get(layerName);
        }
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
            if(layers.size() == 0) return;
            
            if(layers.size() == 1) {
                head = tail = layers.values().iterator().next();
            }
            
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
            }
            
            regionObservable = head.observe();
            
            assemblyClosed = true;
        }
    }
    
    /**
     * Called internally to configure the connection between two {@link Layer} 
     * {@link Observable}s taking care of other connection details such as passing
     * the inference up the chain and any possible encoder.
     * 
     * @param in         the sink end of the connection between two layers
     * @param out        the source end of the connection between two layers
     * @throws IllegalStateException if Region is already closed
     */
    <I extends Layer<Inference>, O extends Layer<Inference>> void configureConnection(I in, O out) {
        if(assemblyClosed) {
            throw new IllegalStateException("Cannot add Layers when Region has already been closed.");
        }
        
        Set<Layer<?>> all = new HashSet<>(sources);
        all.addAll(sinks);
        byte inMask = in.getMask();
        byte outMask = out.getMask();
        if(!all.contains(out)) {
            layersDistinct = (flagAccumulator & outMask) < 1;
            flagAccumulator |= outMask;
        }
        if(!all.contains(in)) {
            layersDistinct = (flagAccumulator & inMask) < 1;
            flagAccumulator |= inMask;
        }
        
        sources.add(out);
        sinks.add(in);
    }
    
    /**
     * Called internally to actually connect two {@link Layer} 
     * {@link Observable}s taking care of other connection details such as passing
     * the inference up the chain and any possible encoder.
     * 
     * @param in         the sink end of the connection between two layers
     * @param out        the source end of the connection between two layers
     * @throws IllegalStateException if Region is already closed 
     */
    <I extends Layer<Inference>, O extends Layer<Inference>> void connect(I in, O out) {
        out.subscribe(new Subscriber<Inference>() {
            ManualInput localInf = new ManualInput();
            
            @Override public void onCompleted() { in.notifyComplete(); }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                if(layersDistinct) {
                    in.compute(i);
                }else{
                    localInf.sdr(i.getSDR()).recordNum(i.getRecordNum()).layerInput(i.getSDR());
                    in.compute(localInf);
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (assemblyClosed ? 1231 : 1237);
        result = prime * result + (isLearn ? 1231 : 1237);
        result = prime * result + ((layers == null) ? 0 : layers.size());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        Region other = (Region)obj;
        if(assemblyClosed != other.assemblyClosed)
            return false;
        if(isLearn != other.isLearn)
            return false;
        if(layers == null) {
            if(other.layers != null)
                return false;
        } else if(!layers.equals(other.layers))
            return false;
        if(name == null) {
            if(other.name != null)
                return false;
        } else if(!name.equals(other.name))
            return false;
        return true;
    }
    
}
