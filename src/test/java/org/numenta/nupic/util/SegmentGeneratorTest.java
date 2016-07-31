/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.util;

import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.DistalDendrite;

public class SegmentGeneratorTest {

    private Parameters getDefaultParameters() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        
        return retVal;
    }
    
    @Test
    public void testGenerateSegments() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        tm.init(cn);
        
        Cell cell4 = cn.getCell(4);
        
        DistalDendrite activeSegment = cell4.createSegment(cn);
        DistalDendrite activeSegment2 = cell4.createSegment(cn);
        DistalDendrite activeSegment3 = cell4.createSegment(cn);
        DistalDendrite activeSegment4 = cell4.createSegment(cn);
        
        List<DistalDendrite> segments = Stream.of(
            activeSegment, activeSegment2, activeSegment3, activeSegment4).collect(Collectors.toList());
        
        Generator<Integer> gen = IntGenerator.of(1, 4);
        
        Generator<DistalDendrite> ddGen = SegmentGenerator.of(segments, gen);
        assertTrue(ddGen.hasNext());
        assertTrue(ddGen.next() == activeSegment2);
        assertFalse(ddGen.next() == activeSegment2); // Assert can't return same thing twice (i.e. that it increments)
        assertTrue(ddGen.next() == activeSegment4);  // Skipped activeSegment3 do to above call to next
        assertFalse(ddGen.hasNext());                // Make sure exhaust condition of < 5 holds true.
    }

}
