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

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.util.UniversalRandom;

public class DistalDendriteTest {

    @Test
    public void testGetActiveSynapses() {
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.CELLS_PER_COLUMN, 1);
        p = getDefaultParameters(p, KEY.MIN_THRESHOLD, 1);
        p.apply(cn);
        TemporalMemory.init(cn);
        
        Set<Cell> prevWinnerCells = cn.getCellSet(new int[] { 0, 1, 2, 3 });
        
        DistalDendrite matchingSegment = cn.createSegment(cn.getCell(4));
        cn.createSynapse(matchingSegment, cn.getCell(0), 0.5);
        
        Set<Synapse> syns = matchingSegment.getActiveSynapses(cn, prevWinnerCells);
        assertTrue(syns.size() == 1);
        assertTrue(syns.iterator().next().getPresynapticCell().equals(cn.getCell(0)));
    }

    private Parameters getDefaultParameters(Parameters p, KEY key, Object value) {
        Parameters retVal = p == null ? getDefaultParameters() : p;
        retVal.set(key, value);
        
        return retVal;
    }
    
    private Parameters getDefaultParameters() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.set(KEY.CELLS_PER_COLUMN, 4);
        retVal.set(KEY.ACTIVATION_THRESHOLD, 3);
        retVal.set(KEY.INITIAL_PERMANENCE, 0.21);
        retVal.set(KEY.CONNECTED_PERMANENCE, 0.5);
        retVal.set(KEY.MIN_THRESHOLD, 2);
        retVal.set(KEY.MAX_NEW_SYNAPSE_COUNT, 3);
        retVal.set(KEY.PERMANENCE_INCREMENT, 0.10);
        retVal.set(KEY.PERMANENCE_DECREMENT, 0.10);
        retVal.set(KEY.PREDICTED_SEGMENT_DECREMENT, 0.0);
        retVal.set(KEY.RANDOM, new UniversalRandom(42));
        retVal.set(KEY.SEED, 42);
        
        return retVal;
    }
}
