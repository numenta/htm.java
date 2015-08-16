package org.numenta.nupic.examples.napi.hotgym;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.numenta.nupic.examples.napi.hotgym.NetworkAPIDemo;
import org.numenta.nupic.examples.napi.hotgym.NetworkAPIDemo.Mode;
import org.numenta.nupic.network.Inference;
import org.numenta.nupic.network.Network;

import rx.Subscriber;


public class NetworkAPIDemoTest {

    @Test
    public void testCreateBasicNetwork() {
        NetworkAPIDemo demo = new NetworkAPIDemo(Mode.BASIC);
        Network n = demo.createBasicNetwork();
        assertEquals(1, n.getRegions().size());
        assertNotNull(n.getRegions().get(0).lookup("Layer 2/3"));
        assertNull(n.getRegions().get(0).lookup("Layer 4"));
        assertNull(n.getRegions().get(0).lookup("Layer 5"));
    }
    
    @Test
    public void testCreateMultiLayerNetwork() {
        NetworkAPIDemo demo = new NetworkAPIDemo(Mode.MULTILAYER);
        Network n = demo.createMultiLayerNetwork();
        assertEquals(1, n.getRegions().size());
        assertNotNull(n.getRegions().get(0).lookup("Layer 2/3"));
        assertNotNull(n.getRegions().get(0).lookup("Layer 4"));
        assertNotNull(n.getRegions().get(0).lookup("Layer 5"));
    }
    
    @Test
    public void testCreateMultiRegionNetwork() {
        NetworkAPIDemo demo = new NetworkAPIDemo(Mode.MULTIREGION);
        Network n = demo.createMultiRegionNetwork();
        
        assertEquals(2, n.getRegions().size());
        
        assertNotNull(n.getRegions().get(0).lookup("Layer 2/3"));
        assertNotNull(n.getRegions().get(0).lookup("Layer 4"));
        
        assertNotNull(n.getRegions().get(1).lookup("Layer 2/3"));
        assertNotNull(n.getRegions().get(1).lookup("Layer 4"));
    }

    @Test
    public void testGetSubscriber() {
        NetworkAPIDemo demo = new NetworkAPIDemo(Mode.MULTIREGION);
        Subscriber<Inference> s = demo.getSubscriber();
        assertNotNull(s);
    }
}
