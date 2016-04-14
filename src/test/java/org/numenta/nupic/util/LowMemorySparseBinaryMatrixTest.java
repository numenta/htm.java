/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
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

import org.junit.Test;

import org.junit.Assert;

/**
 * Test for {@link LowMemorySparseBinaryMatrix}
 *  
 * @author Jose Luis Martin
 */
public class LowMemorySparseBinaryMatrixTest {

    @Test
    public void testTrueCount() {
        int[] expected = {4, 3, 2, 1, 0};
        int dense[][] = {
                {0, 1, 1, 1, 1},
                {0, 0, 1, 1, 1},
                {0, 0, 0, 1, 1},
                {0, 0, 0, 0, 1},
                {0, 0, 0, 0, 0}
        };

        LowMemorySparseBinaryMatrix sp = new LowMemorySparseBinaryMatrix(new int[] {5, 5});

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                sp.set(dense[i][j], i, j);
            }
        }

        Assert.assertArrayEquals(expected, sp.getTrueCounts());
    }
    
}
