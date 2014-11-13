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

package org.numenta.nupic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;
import org.numenta.nupic.util.MersenneTwister;

public class ParametersTest {
    @SuppressWarnings("unused")
	private Parameters parameters;

    @Test
    public void testApply() {
        DummyContainer dc = new DummyContainer();
        Parameters params = Parameters.getAllDefaultParameters();
        params.setParameterByKey(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        params.setParameterByKey(Parameters.KEY.POTENTIAL_PCT, 20.0);
        params.setParameterByKey(Parameters.KEY.CELLS_PER_COLUMN, null);
        params.apply(dc);
        assertTrue(Arrays.equals(new int[] { 2048 }, dc.getColumnDimensions()));
        assertEquals(20.0, dc.getPotentialPct(), 0);
    }

    @Test
    public void testDefaults() {
        Parameters params = Parameters.getAllDefaultParameters();
        assertEquals(params.getParameterByKey(Parameters.KEY.CELLS_PER_COLUMN), 32);
        assertEquals(params.getParameterByKey(Parameters.KEY.SEED), 42);
        assertEquals(true, ((Random)params.getParameterByKey(Parameters.KEY.RANDOM)).getClass().equals(MersenneTwister.class));
    }

    public static class DummyContainerBase {
        private int[] columnDimensions;

        public int[] getColumnDimensions() {
            return columnDimensions;
        }

        public void setColumnDimensions(int[] columnDimensions) {
            this.columnDimensions = columnDimensions;
        }
    }

    public static class DummyContainer extends DummyContainerBase {
        private double potentialPct = 0;

        public double getPotentialPct() {
            return potentialPct;
        }

        public void setPotentialPct(double potentialPct) {
            this.potentialPct = potentialPct;
        }
    }


}
