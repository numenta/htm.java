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
import java.util.Iterator;
import java.util.List;

import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorFactory;

import rx.Observable;
import rx.Subscriber;

/**
 * <p>
 * A {@code Network} is the fundamental component of the HTM.java Network API.
 * It is comprised of {@link Region}s which are in turn comprised of {@link Layer}s;
 * each Layer directly containing one or more algorithm or computational components
 * such (i.e. {@link Sensor}, {@link MultiEncoder}, {@link SpatialPooler}, 
 * {@link TemporalMemory}, {@link CLAClassifier} etc.)
 * </p>
 * <p>
 * Networks in HTM.java are extremely easy to compose. For instance, here is an example 
 * of a network which contains everything:
 * <pre>
 * Parameters p = NetworkTestHarness.getParameters();
 * p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
 * 
 * Network network = Network.create("test network", p)
 *     .add(Network.createRegion("r1")
 *         .add(Network.createLayer("1", p)
 *             .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
 *             .add(Anomaly.create())
 *             .add(new TemporalMemory())
 *             .add(new SpatialPooler())
 *             .add(Sensor.create(FileSensor::create, SensorParams.create(
 *                 Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))));
 * </pre>
 *                 
 * <p>
 * As you can see, {@code Networks} can be composed in "fluent" style making their
 * declaration much more concise.
 * 
 * While the above Network contains only 1 Region and only 1 Layer, Networks with many Regions
 * and Layers within them; may be composed. For example:
 * <pre>
 * Connections cons = new Connections();
 * 
 * Network network = Network.create("test network", p)
 *     .add(Network.createRegion("r1")
 *         .add(Network.createLayer("1", p)
 *             .add(Anomaly.create())
 *             .add(new TemporalMemory())
 *             .add(new SpatialPooler())))
 *     .add(Network.createRegion("r2")
 *         .add(Network.createLayer("1", p)
 *             .using(cons)
 *             .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
 *             .add(new TemporalMemory()))                    
 *         .add(Network.createLayer("2", p)
 *             .using(cons)
 *             .add(new SpatialPooler())
 *             .add(Sensor.create(FileSensor::create, SensorParams.create(
 *                 Keys::path, "", ResourceLocator.path("rec-center-hourly.csv")))))
 *         .connect("1", "2")) // Tell the Region to connect the two layers {@link Region#connect(String, String)}.
 *     .connect("r1", "r2");   // Tell the Network to connect the two regions {@link Network#connect(String, String)}.
 * </pre>
 * As you can see above, a {@link Connections} object is being shared among Region "r2"'s inner layers 
 * via the use of the {@link Layer#using(Connections)} method. Additionally, {@link Parameters} may be
 * altered in place without changing the values in the original Parameters object, by calling {@link Layer#alterParameter(KEY, Object)}
 * as seen in the above examples.  
 * 
 * <p>
 * Networks can be "observed", meaning they can return an {@link Observable} object
 * which can be operated on in the map-reduce pattern of usage; and they can be 
 * subscribed to, to receive {@link Inference} objects as output from every process
 * cycle. For instance:
 * <pre>
 * network.observe().subscribe(new Subscriber&lt;Inference&gt;() {
 *      public void onCompleted() { System.out.println("Input Completed!"); }
 *      public void onError(Throwable e) { e.printStackTrace(); }
 *      public void onNext(Inference i) {
 *          System.out.println(String.format("%d: %s", i.getRecordNum(), Arrays.toString(i.getSDR())));
 *      }
 * });
 * </pre>
 * 
 * <p>
 * Likewise, each Region within a Network may be "observed" and/or subscribed to; and each
 * Layer may be observed and subscribed to as well - for those instances where you would like 
 * to obtain the processing from an individual component within the network for use outside 
 * the network in some other area of your application. To find and subscribe to individual
 * components try:
 * <pre>
 * Network network = ...
 * 
 * Region region = network.lookup("&lt;region name&gt;");
 * region.observe().subscribe(new Subscriber&lt;Inference&gt;() {
 *     public void onCompleted() { System.out.println("Input Completed!"); }
 *     public void onError(Throwable e) { e.printStackTrace(); }
 *     public void onNext(Inference i) {
 *         int[] sdr = i.getSDR();
 *         do something...
 *     }    
 * }
 * 
 * Layer l2_3 = region.lookup("&lt;layer name&gt;");
 * l2_3.observe().subscribe(new Subscriber&lt;Inference&gt;() {
 *     public void onCompleted() { System.out.println("Input Completed!"); }
 *     public void onError(Throwable e) { e.printStackTrace(); }
 *     public void onNext(Inference i) {
 *         int[] sdr = i.getSDR();
 *         do something...
 *     }    
 * }
 * </pre>
 * 
 * In addition there are many usage examples to be found in the {@link org.numenta.nupic.examples.napi.hotgym} package
 * where there are tests which may be examined for details, and the {@link NetworkAPIDemo}
 * 
 * @author David Ray
 * @see Region
 * @see Layer
 * @see Inference
 * @see ManualInput
 * @see NetworkAPIDemo
 */
public class Network {
    public enum Mode { MANUAL, AUTO, REACTIVE };

    private String name;
    private Parameters parameters;
    private HTMSensor<?> sensor;
    private MultiEncoder encoder;
    private Region head;
    private Region tail;
    private Region sensorRegion;
    
    private boolean isLearn = true;
    
    private List<Region> regions = new ArrayList<>();

    /**
     * Creates a new {@link Network}
     * @param name
     * @param parameters
     */
    public Network(String name, Parameters parameters) {
        this.name = name;
        this.parameters = parameters;
        if(parameters == null) {
            throw new IllegalArgumentException("Network Parameters were null.");
        }
    }

    /**
     * Creates and returns an implementation of {@link Network}
     * 
     * @param name
     * @param parameters
     * @return
     */
    public static Network create(String name, Parameters parameters) {
        return new Network(name, parameters);
    }

    /**
     * Creates and returns a child {@link Region} of this {@code Network}
     * 
     * @param   name    The String identifier for the specified {@link Region}
     * @return
     */
    public static Region createRegion(String name) {
        Network.checkName(name);

        Region r = new Region(name, null);
        return r;
    }

    /**
     * Creates a {@link Layer} to hold algorithmic components and returns
     * it.
     * 
     * @param name  the String identifier for the specified {@link Layer}
     * @param p     the {@link Parameters} to use for the specified {@link Layer}
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Layer<?> createLayer(String name, Parameters p) {
        Network.checkName(name);
        return new Layer(name, null, p);
    }

    /**
     * Returns the String identifier for this {@code Network}
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * If {@link Network.Mode} == {@link Mode#AUTO}, calling this 
     * method will start the main engine thread which pulls in data
     * from the connected {@link Sensor}(s).
     * 
     * <em>Warning:</em> Calling this method with any other Mode than 
     * {@link Mode#AUTO} will result in an {@link UnsupportedOperationException}
     * being thrown.
     */
    public void start() {
        if(regions.size() < 1) {
            throw new IllegalStateException("Nothing to start - 0 regions");
        }

        Region tail = regions.get(0);
        Region upstream = tail;
        while((upstream = upstream.getUpstreamRegion()) != null) {
            tail = upstream;
        }

        tail.start();
    }

    /**
     * Halts this {@code Network}, stopping all threads and closing
     * all {@link SensorFactory} connections to incoming data, freeing up 
     * any resources associated with the input connections.
     */
    public void halt() {
        if(regions.size() == 1) {
            this.tail = regions.get(0);
        }
        tail.halt();
    }

    /**
     * Pauses all underlying {@code Network} nodes, maintaining any 
     * connections (leaving them open until they possibly time out).
     * Does nothing to prevent any sensor connections from timing out
     * on their own. 
     */
    public void pause() {
        throw new UnsupportedOperationException("Pausing is not (yet) supported.");
    }
    
    /**
     * Sets the learning mode.
     * @param isLearn
     */
    public void setLearn(boolean isLearn) {
        this.isLearn = isLearn;
        for(Region r : regions) {
            r.setLearn(isLearn);
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
     * Returns the current {@link Mode} with which this {@link Network} is 
     * currently configured.
     * 
     * @return
     */
    public Mode getMode() {
        // TODO Auto-generated method stub
        return null;
    }
    
    /**
     * Finds any {@link Region} containing a {@link Layer} which contains a {@link TemporalMemory} 
     * and resets them.
     */
    public void reset() {
        for(Region r : regions) {
            r.reset();
        }
    }
    
    /**
     * Resets the recordNum in all {@link Region}s.
     */
    public void resetRecordNum() {
        for(Region r : regions) {
            r.resetRecordNum();
        }
    }

    /**
     * Returns an {@link Observable} capable of emitting {@link Inference}s
     * which contain the results of this {@code Network}'s processing chain.
     * @return
     */
    public Observable<Inference> observe() {
        if(regions.size() == 1) {
            this.head = regions.get(0);
        }
        return head.observe();
    }

    /**
     * Returns the top-most (last in execution order from
     * bottom to top) {@link Region} in this {@code Network}
     * 
     * @return
     */
    public Region getHead() {
        if(regions.size() == 1) {
            this.head = regions.get(0);
        }
        return this.head;
    }

    /**
     * Returns the bottom-most (first in execution order from
     * bottom to top) {@link Region} in this {@code Network}
     * 
     * @return
     */
    public Region getTail() {
        if(regions.size() == 1) {
            this.tail = regions.get(0);
        }
        return this.tail;
    }

    /**
     * Returns a {@link Iterator} capable of walking the tree of regions
     * from the root {@link Region} down through all the child Regions. In turn,
     * a {@link Region} may be queried for a {@link Iterator} which will return
     * an iterator capable of traversing the Region's contained {@link Layer}s.
     * 
     * @return
     */
    public Iterator<Region> iterator() {
        return getRegions().iterator();
    }
    
    /**
     * Used to manually input data into a {@link Network}, the other way 
     * being the call to {@link Network#start()} for a Network that contains a
     * Region that contains a {@link Layer} which in turn contains a {@link Sensor} <em>-OR-</em>
     * subscribing a receiving Region to this Region's output Observable.
     * 
     * @param input One of (int[], String[], {@link ManualInput}, or Map&lt;String, Object&gt;)
     */
    public <T> void compute(T input) {
        if(tail == null && regions.size() == 1) {
            this.tail = regions.get(0);
        }
        
        if(head == null) {
            addDummySubscriber();
        }
        
        tail.compute(input);
    }
    
    /**
     * Used to manually input data into a {@link Network} in a synchronous way, the other way 
     * being the call to {@link Network#start()} for a Network that contains a
     * Region that contains a {@link Layer} which in turn contains a {@link Sensor} <em>-OR-</em>
     * subscribing a receiving Region to this Region's output Observable.
     * 
     * @param input One of (int[], String[], {@link ManualInput}, or Map&lt;String, Object&gt;)
     */
    public <T> Inference computeImmediate(T input) {
        if(tail == null && regions.size() == 1) {
            this.tail = regions.get(0);
        }
        
        if(head == null) {
            addDummySubscriber();
        }
        
        tail.compute(input);
        return head.getHead().getInference();
    }
    
    /**
     * Added when a synchronous call is made and there is no subscriber. No
     * Subscriber leads to the observable chain not being constructed, therefore
     * we must always have at least one subscriber.
     */
    private void addDummySubscriber() {
        observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {}
        });
    }

    /**
     * Connects the specified source to the specified sink (the order of
     * processing flows from source to sink, or lower level region to higher
     * level region). 
     * @param regionSink        the receiving end of the connection
     * @param regionSource      the source end of the connection
     * 
     * @return  this {@code Network}
     */
    public Network connect(String regionSink, String regionSource) {
        Region source = lookup(regionSource);
        if(source == null) {
            throw new IllegalArgumentException("Region with name: " + regionSource + " not added to Network.");
        }

        Region sink = lookup(regionSink);
        if(sink == null) {
            throw new IllegalArgumentException("Region with name: " + regionSink + " not added to Network.");
        }

        sink.connect(source);

        tail = tail == null ? source : tail;
        head = head == null ? sink : head;

        Region bottom = source;
        while((bottom = bottom.getUpstreamRegion()) != null) {
            tail = bottom;
        }

        Region top = sink;
        while((top = top.getDownstreamRegion()) != null) {
            head = top;
        }

        return this;
    }

    /**
     * Adds a {@link Region} to this {@code Network}
     * @param region
     * @return
     */
    public Network add(Region region) {
        regions.add(region);
        region.setNetwork(this);
        return this;
    }

    /**
     * Returns a {@link List} view of the contained {@link Region}s.
     * @return
     */
    public List<Region> getRegions() {
        return new ArrayList<Region>(regions);
    }

    /**
     * Sets a reference to the {@link Region} which contains the {@link Sensor}
     * (if any).
     * 
     * @param r
     */
    public void setSensorRegion(Region r) {
        this.sensorRegion = r;
    }

    /**
     * Returns a reference to the {@link Region} which contains the {@link Sensor}
     * 
     * @return  the Region which contains the Sensor
     */
    public Region getSensorRegion() {
        return sensorRegion;
    }

    /**
     * Returns the {@link Region} with the specified name
     * or null if it doesn't exist within this {@code Network}
     * @param regionName
     * @return
     */
    public Region lookup(String regionName) {
        for(Region r : regions) {
            if(r.getName().equals(regionName)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns the network-level {@link Parameters}.
     * @return
     */
    public Parameters getParameters() {
        return parameters;
    }

    /**
     * Sets the reference to this {@code Network}'s Sensor
     * @param sensor
     */
    public void setSensor(HTMSensor<?> sensor) {
        this.sensor = sensor;
        this.sensor.initEncoder(this.parameters);
    }

    /**
     * Returns the encoder present in one of this {@code Network}'s
     * {@link Sensor}s
     * 
     * @return
     */
    public HTMSensor<?> getSensor() {
        return sensor;
    }

    /**
     * Sets the {@link MultiEncoder} on this Network
     * @param e
     */
    public void setEncoder(MultiEncoder e) {
        this.encoder = e;
    }

    /**
     * Returns the {@link MultiEncoder} with which this Network is configured.
     * @return
     */
    public MultiEncoder getEncoder() {
        return this.encoder;
    }

    /**
     * Checks the name for suitability within a given network, 
     * checking for reserved characters and such.
     * 
     * @param name
     */
    private static void checkName(String name) {
        if(name.indexOf(":") != -1) {
            throw new IllegalArgumentException("\":\" is a reserved character.");
        }
    }

}
