package org.numenta.nupic.network;

import java.util.ArrayList;
import java.util.List;

import org.numenta.nupic.network.Network.Node;



public class Region {
    private Network network;
    private List<Layer> layers;
    
    public Region(Network container) {
        this.network = container;
        layers = new ArrayList<>();
    }
    
    public <T, K> K compute(T input) {
        K topLayerOutput = null;
        for(Layer l : layers) {
            topLayerOutput = l.compute(input);
        }
        return (K)topLayerOutput;
    }
    
    public Region add(Layer l) {
        layers.add(l);
        l.setRegion(this);
        
        
        return this;
    }
    
    public Region connect(Region inputRegion) {
        return this;
    }
    
}
