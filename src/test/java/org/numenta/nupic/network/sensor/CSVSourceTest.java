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
