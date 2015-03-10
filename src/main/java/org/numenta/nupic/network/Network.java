package org.numenta.nupic.network;

import java.util.Iterator;
import java.util.List;


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
    
    
    /////////////////////////////////////////////////////////////////////////
    //                   Internal Interface Definitions                    //
    /////////////////////////////////////////////////////////////////////////
    
    /**
     * Internal type to handle connectivity and locality within a given
     * graph or tree of {@link Network} components.
     */
    public interface Node {
        enum Type { SP, TM, ENCODER, MULT_ENC, CLASSIFIER, ANOMALY, REGION };
        
        /**
         * Returns this Node's {@link Node.Type}
         * @return
         */
        public Type type();
        /**
         * Returns the contained algorithmic object.
         * @return
         */
        public Object getElement();
        /**
         * Returns the algorithmic component who's type is specified by
         * the type &lt;T&gt; of the specified class.
         * 
         * The method {@link #type()} should be called to obtain the type of 
         * the object contained within to avoid {@link ClassCastException}s 
         * being thrown when retrieving the internally contained object.
         * 
         * @param c     the class of type &lt;T&gt; indicating the type expected
         *              to be returned.
         * @return      the contained object 
         * @throws      ClassCastException if the type specified is incorrect
         */
        public default <T> T get(Class<T> c) {
            return c.cast(getElement());
        }
    }
     
}
