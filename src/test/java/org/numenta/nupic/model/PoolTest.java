/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.model;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.numenta.nupic.util.ArrayUtils;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;


public class PoolTest {

    @Test
    public void testGetDenseConnected() {
        Connections connections = new Connections();
        connections.setSynPermConnected(0.2d);
        connections.setNumInputs(5);
        
        Pool pool = new Pool(3);
        Synapse[] synapses = new Synapse[3];
        double[] perms = new double[] { 0.0, 0.2, 0.0, 0.2, 0.2 };
        for(int i = 0,j = 0;i < perms.length;i++) {
            if(perms[i] == 0) continue;
            synapses[j] = new Synapse(connections, null, null, pool, i, i);
            synapses[j++].setPermanence(connections, perms[i]);
        }
        
        int[] expected = { 0, 1, 0, 1, 1 };
        int[] dense = pool.getDenseConnected(connections);
        assertTrue(Arrays.equals(expected, dense));
        
        int[] expected2 = { 0, 1, 0, 1, 0 };           // expect last bit below synPermConnected
        synapses[2].setPermanence(connections, 0.19d); // set last bit below synPermConnected
        dense = pool.getDenseConnected(connections);
        assertTrue(Arrays.equals(expected2, dense));
    }
    
    @Test
    public void testGetDensePotential() {
        Connections connections = new Connections();
        connections.setNumInputs(10);
        
        Pool pool = new Pool(3);
        Synapse[] synapses = new Synapse[3];
        int[] connecteds = new int[] { 0, 1, 0, 0, 1, 1, 0, 1, 0, 1 };
        for(int i = 0,j = 0;i < connecteds.length;i++) {
            if(connecteds[i] == 0) continue;
            synapses[j] = new Synapse(connections, null, null, pool, i, i);
            synapses[j].setPermanence(connections, 0.0); // 0.0 -> These aren't set as connected but simply members.
        }
        
        int[] expected = { 0, 1, 0, 0, 1, 1, 0, 1, 0, 1 };
        int[] dense = pool.getDensePotential(connections);
        assertTrue(Arrays.equals(expected, dense));
    }
    
    @Test
    public void testTroveMapPreservesSynapseOrdering() {
        Synapse s = new Synapse();
        TIntObjectMap<Synapse> t = new TIntObjectHashMap<>();
        t.put(2, s);
        t.put(4, s);
        t.put(6, s);
        t.put(8, s);
        t.put(10, s);
        t.put(12, s);
        
        int[] expectedKeys = { 12, 10, 8, 6, 4, 2 };
        assertTrue(Arrays.equals(expectedKeys, t.keys()));
        
        // Insert a cell index in the middle, check that order is still
        // preserved following inserts
        t.put(3, s);
        expectedKeys = new int[] { 12, 10, 8, 6, 4, 3, 2 };
        assertTrue(Arrays.equals(expectedKeys, t.keys()));
        
        // Check reversing produces ascending ordered Synapses
        expectedKeys = new int[] { 2, 3, 4, 6, 8, 10, 12 };
        assertTrue(Arrays.equals(expectedKeys, ArrayUtils.reverse(t.keys())));
    }

}
