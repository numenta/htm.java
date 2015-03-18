package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class HTMSensorTest {

    @Test
    public void testPadTo() {
        List<String[]> l = new ArrayList<>();
        l.add(new String[] { "0", "My"});
        l.add(new String[] { "3", "list"});
        l.add(new String[] { "4", "can "});
        l.add(new String[] { "1", "really"});
        l.add(new String[] { "6", "frustrate."});
        l.add(new String[] { "2", "unordered"});
        l.add(new String[] { "5", "also"});
        
        List<String> out = new ArrayList<>();
        for(String[] sa : l) {
            int idx = Integer.parseInt(sa[0]);
            out.set(HTMSensor.padTo(idx, out), sa[1]);
        }
        
        assertEquals("[My, really, unordered, list, can , also, frustrate.]", out.toString());
    }

}
