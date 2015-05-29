package org.numenta.nupic.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorFactory;


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
     * an iterator capable of traversing the Region's contained {@link Layer}s.
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
    public <T> List<Region> getRegions();
    
    /**
     * Creates and returns an implementation of {@link Network}
     * 
     * @param parameters
     * @return
     */
    public static Network create(String name, Parameters parameters) {
        return new NetworkImpl(name, parameters);
    }
    
    /**
     * Adds a {@link Region} to this {@code Network}
     * @param region
     * @return
     */
    public <T> Network add(Region region);
    
    /**
     * Creates and returns a child {@link Region} of this {@code Network}
     * 
     * @param   name    The String identifier for the specified {@link Region}
     * @return
     */
    public Region createRegion(String name);
    
    /**
     * Creates a {@link Layer} to hold algorithmic components
     * 
     * @param name  the String identifier for the specified {@link Layer}
     * @param p
     * @return
     */
    public Layer<?> createLayer(String name, Parameters p);
    
    /**
     * Creates a {@link Layer} using the default {@link Network} level parameters
     * 
     * @param name  the String identifier for the specified {@link Layer}
     * @return
     */
    public Layer<?> createLayer(String name);
    
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
     * Sets the {@link MultiEncoder} on this Network
     * @param e
     */
    public void setEncoder(MultiEncoder e);
    
    /**
     * Returns the {@link MultiEncoder} with which this Network is configured.
     * @return
     */
    public MultiEncoder getEncoder();
    
    
    /////////////////////////////////////////////////////////////////////////
    //                   Internal Interface Definitions                    //
    /////////////////////////////////////////////////////////////////////////
    
    /**
     * Implementation of the {@link Network} interface.
     * 
     * @author David Ray
     * @see Network
     */
    public static class NetworkImpl implements Network {
        private String name;
        private Parameters parameters;
        private HTMSensor<?> sensor;
        private MultiEncoder encoder;
        
        private List<Region> regions = new ArrayList<>();
        
        /**
         * Creates a new {@link NetworkImpl}
         * @param parameters
         */
        public NetworkImpl(String name, Parameters parameters) {
            this.name = name;
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

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Region> getRegions() {
            // TODO Auto-generated method stub
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Region createRegion(String name) {
            checkName(name);
            
            Region r = new Region(name, this);
            return r;
        }
        
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public Layer<?> createLayer(String name) {
            checkName(name);
            return new Layer(name, this, this.parameters);
        }
        
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("rawtypes")
        @Override
        public Layer<?> createLayer(String name, Parameters p) {
            checkName(name);
            return new Layer(name, this, p);
        }

        /**
         * {@inheritDoc}
         */
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
            this.sensor.setLocalParameters(this.parameters);
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
         * Sets the {@link MultiEncoder} on this Network
         * @param e
         */
        @Override
        public void setEncoder(MultiEncoder e) {
            this.encoder = e;
        }
        
        /**
         * Returns the {@link MultiEncoder} with which this Network is configured.
         * @return
         */
        @Override
        public MultiEncoder getEncoder() {
            return this.encoder;
        }
        
        /**
         * Checks the name for suitability within a given network, 
         * checking for reserved characters and such.
         * 
         * @param name
         */
        private void checkName(String name) {
            if(name.indexOf(":") != -1) {
                throw new IllegalArgumentException("\":\" is a reserved character.");
            }
        }
    }
     
}
