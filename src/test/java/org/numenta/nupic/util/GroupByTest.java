/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014-2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;

import chaschev.lang.Pair;

public class GroupByTest {

    @Test
    public void testIntegerGroup() {
        List<Integer> l = Arrays.asList(new Integer[] { 7, 12, 16 });
        @SuppressWarnings("unchecked")
        List<Pair<Integer, Integer>> expected = Arrays.asList(
            new Pair[] { 
                new Pair<Integer, Integer>(7, 7),
                new Pair<Integer, Integer>(12, 12),
                new Pair<Integer, Integer>(16, 16)
            });
        GroupBy<Integer, Integer> grouper = GroupBy.of(l, i -> i); 
        
        int i = 0;
        int pairCount = 0;
        for(Pair<Integer, Integer> p : grouper) {
            assertEquals(expected.get(i++), p);
            pairCount++;
        }
        
        assertEquals(3, pairCount);
        
        //////
        
        pairCount = 0;
        l = Arrays.asList(new Integer[] { 2, 4, 4, 5 });
        @SuppressWarnings("unchecked")
        List<Pair<Integer, Integer>> expected2 = Arrays.asList(
            new Pair[] { 
                new Pair<Integer, Integer>(2, 6),
                new Pair<Integer, Integer>(4, 12),
                new Pair<Integer, Integer>(4, 12),
                new Pair<Integer, Integer>(5, 15)
            });
        grouper = GroupBy.of(l, in -> in * 3); 
        
        i = 0;
        for(Pair<Integer, Integer> p : grouper) {
            assertEquals(expected2.get(i++), p);
            pairCount++;
        }
        
        assertEquals(4, pairCount);
    }
    
    @Test
    public void testObjectGroup() {
        Column c0 = new Column(9, 0);
        Column c1 = new Column(9, 1);
        
        // Illustrates the Cell's actual index = colIndex * cellsPerColumn + indexOfCellWithinCol
        assertEquals(7, c0.getCell(7).getIndex());
        assertEquals(12, c1.getCell(3).getIndex());
        assertEquals(16, c1.getCell(7).getIndex());
        
        DistalDendrite dd0 = new DistalDendrite(c0.getCell(7), 0, 0, 0);
        DistalDendrite dd1 = new DistalDendrite(c1.getCell(3 /* Col 1's Cells start at 9 */), 1, 0, 1);
        DistalDendrite dd2 = new DistalDendrite(c1.getCell(7/* Col 1's Cells start at 9 */), 2, 0, 2);
        
        List<DistalDendrite> l = Arrays.asList(
            new DistalDendrite[] { dd0, dd1, dd2 });
        
        @SuppressWarnings("unchecked")
        List<Pair<DistalDendrite, Column>> expected = Arrays.asList(
            new Pair[] { 
                new Pair<DistalDendrite, Column>(dd0, c0),
                new Pair<DistalDendrite, Column>(dd1, c1),
                new Pair<DistalDendrite, Column>(dd2, c1)
            });
        
        GroupBy<DistalDendrite, Column> grouper = GroupBy.of(l, i -> i.getParentCell().getColumn()); 
        
        int i = 0;
        for(Pair<DistalDendrite, Column> p : grouper) {
            assertEquals(expected.get(i++), p);
        }
    }

}
