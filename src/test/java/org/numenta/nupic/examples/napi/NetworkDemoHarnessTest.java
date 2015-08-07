package org.numenta.nupic.examples.napi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.Parameters;


public class NetworkDemoHarnessTest {

    @Test
    public void testGetParameters() {
        Parameters p = NetworkDemoHarness.getParameters();
        assertEquals(47, p.size());
    }
    
    @Test
    public void testGetDayDemoTestEncoderParams() {
        Parameters p = NetworkDemoHarness.getDayDemoTestEncoderParams();
        assertEquals(14, p.size());
    }
    
    @Test
    public void testGetDayDemoFieldEncodingMap() {
        Map<String, Map<String, Object>> fieldEncodings = NetworkDemoHarness.getDayDemoFieldEncodingMap();
        assertEquals(1, fieldEncodings.size());
    }
    
    @Test
    public void testGetHotGymTestEncoderParams() {
        Map<String, Map<String, Object>> fieldEncodings = NetworkDemoHarness.getHotGymFieldEncodingMap();
        assertEquals(2, fieldEncodings.size());
    }
    
    @Test
    public void testGetNetworkDemoTestEncoderParams() {
        Parameters p = NetworkDemoHarness.getNetworkDemoTestEncoderParams();
        assertEquals(29, p.size());
    }
    
    @Test
    public void testSetupMap() {
        Map<String, Map<String, Object>> m = NetworkDemoHarness.setupMap(null, 23, 2, 0.0, 0.9, 22.0, 3.0, false, false, null, "cogmission", "ai", "works");
        assertNotNull(m);
        
        // Make sure omission of key doesn't insert null or a default value
        assertTrue(!m.containsKey("forced"));
        
        assertEquals(1, m.size());
        assertEquals(11, m.get("cogmission").size());
    }

}
