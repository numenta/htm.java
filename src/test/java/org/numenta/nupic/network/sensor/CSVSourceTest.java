 package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

public class CSVSourceTest {

    // Quick test of essential functionality: 
    //      construction of header, body
    //      specification of types and conversion
    @Test
    public void test() {
        CSVSource csv = new CSVSource("rec-center-hourly-small.csv", "MM/dd/YY HH:mm");
        assertNotNull(csv);
        assertNotNull(csv.getHeader());
        assertNotNull(csv.getBody());
        assertEquals(3, csv.getHeader().size());
        assertEquals(17, csv.getBody().size());

        Iterator<Map<String, Object>> it = csv.multiIterator();
        Map<String, Object> map = it.next();
        assertTrue(map.get("timestamp").toString().indexOf("2010-07-02T00") != -1);
        assertEquals(new Double(21.2), map.get("consumption"));
        
        map = it.next();
        assertTrue(map.get("timestamp").toString().indexOf("2010-07-02T01") != -1);
        assertEquals(new Double(16.4), map.get("consumption"));
        
        map = it.next();
        assertTrue(map.get("timestamp").toString().indexOf("2010-07-02T02") != -1);
        assertEquals(new Double(4.7), map.get("consumption"));
        
        map = it.next();
        assertTrue(map.get("timestamp").toString().indexOf("2010-07-02T03") != -1);
        assertEquals(new Double(4.7), map.get("consumption"));
        
        map = it.next();
        assertTrue(map.get("timestamp").toString().indexOf("2010-07-02T04") != -1);
        assertEquals(new Double(4.6), map.get("consumption"));
    }

}
