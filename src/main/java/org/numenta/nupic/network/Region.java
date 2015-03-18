package org.numenta.nupic.network;

import java.util.ArrayList;
import java.util.List;



public class Region {
    private Network network;
    private List<Layer> layers;
    
    public Region(Network container) {
        this.network = container;
        layers = new ArrayList<>();
    }
    
    public int[] compute(int[] input) {
        int[] topLayerOutput = input;
        for(Layer l : layers) {
            topLayerOutput = l.compute(topLayerOutput);
        }
        return topLayerOutput;
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
