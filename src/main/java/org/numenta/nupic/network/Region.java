package org.numenta.nupic.network;

import java.util.Iterator;
import java.util.List;

import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.Network.Node;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;


public class Region implements Node {
    private List<Node> nodeList;
    
    public <T> Region add(SensorFactory<T> inputSensor) {
        return this;
    }
    public Region add(SpatialPooler sp) {
        return this;
    }
    public Region add(TemporalMemory tm) {
        return this;
    }
    public Region addMultiple(MultiEncoder me) {
        return this;
    }
    public <T> Region add(Encoder<T> e) {
        return this;
    }
    public Region add(CLAClassifier classifier) {
        return this;
    }
    public Region add(Anomaly anomaly) {
        return this;
    }
    public Region connect(Region inputRegion) {
        return this;
    }
    
    
    
    
    /**
     * Returns this Node's {@link Node.Type}
     * @return
     */
    @Override
    public Type type() {
        return Type.REGION;
    }
    
    /**
     * Returns the contained algorithmic object. In the case 
     * of a {@code Region} it merely returns "this" object,
     * in which case {@link #iterator()} should be called to
     * walk the contained hierarchy of internal elements.
     * @return
     */
    @Override
    public Object getElement() {
        return this;
    }
    
    /**
     * The only type this Node contains is Region, so return it.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Class<T> c) {
        return (T)this;
    }
    
    /**
     * The {@link Iterator} returned from this object's {@link #iterator()} 
     * method will traverse the internal contents of a Region which is either
     * a {@link Node} or {@link Region} (which is a more specific type of Node}
     * @return
     */
    public Iterator<Node> iterator() {
        return nodeList.iterator();
    }
}
