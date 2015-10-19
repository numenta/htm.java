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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

/**
 * Generic test for Matrix hirearchy.
 * 
 * @author Jose Luis Martin
 */
public class MatrixTest {

    private int[] dimensions =  { 5, 2 };
    private int[] indexes = { 0, 4, 6, 7, 8 };

    @Test
    public void testBitSetMatrixSet() {
        Boolean[] expected = {true, false, false, false, true, false, true, true, true, false }; 
        BitSetMatrix bsm = new BitSetMatrix(this.dimensions);

        for (int index : this.indexes) {
            bsm.set(index, true);
        }

        assertArrayEquals(expected, asDense(bsm));
        assertEquals(Arrays.toString(expected), FlatArrayMatrix.print1DArray(asDense(bsm)));
    }

    @Test
    public void testFlatArrayMatrixSet() {
        Integer[] expected = { 1, 0, 0, 0, 1, 0, 1, 1, 1, 0 };
        FlatArrayMatrix<Integer> fam = new FlatArrayMatrix<Integer>(this.dimensions);
        fam.fill(0);

        for (int index : this.indexes) {
            fam.set(index, 1);
        }

        assertArrayEquals(expected, asDense(fam));
        assertEquals(Arrays.toString(expected), FlatArrayMatrix.print1DArray(asDense(fam)));
    }

    private Object[] asDense(FlatMatrix<?> matrix) {
        Object[] dense = new Object[matrix.getMaxIndex() + 1];

        for (int i = 0; i < matrix.getMaxIndex() + 1; i++) {
            dense[i] = matrix.get(i);
        }



        return dense;
    }
}
