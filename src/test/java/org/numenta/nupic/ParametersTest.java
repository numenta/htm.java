/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package org.numenta.nupic;

import org.junit.Test;
import org.numenta.nupic.util.MersenneTwister;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class ParametersTest {
    private Parameters parameters;

    @Test
    public void testApply() {
        DummyContainer dc = new DummyContainer();
        Parameters params = new Parameters();
        params.setParameterByKey(Parameters.KEY.MINVAL, 10.0);
        params.setParameterByKey(Parameters.KEY.MAXVAL, 20.0);
        params.setParameterByKey(Parameters.KEY.CELLS_PER_COLUMN, null);
        Parameters.apply(dc, params);
        assertEquals(10.0, dc.getMinVal(), 0);
        assertEquals(20.0, dc.getMaxVal(), 0);
    }

    @Test
    public void testDefaults() {
       Parameters params = Parameters.getDefaultParameters();
        assertEquals(params.getParameterByKey(Parameters.KEY.CELLS_PER_COLUMN), 32);
        assertEquals(params.getParameterByKey(Parameters.KEY.SEED), 42);
        assertEquals(true, ((Random)params.getParameterByKey(Parameters.KEY.RANDOM)).getClass().equals(MersenneTwister.class));
    }

    @Test
    public void testCopy() {
        Parameters params = Parameters.getDefaultParameters();
        Parameters copy = new Parameters(params);
        assertEquals(params.getParameterByKey(Parameters.KEY.CELLS_PER_COLUMN), copy.getParameterByKey(Parameters.KEY.CELLS_PER_COLUMN));
        assertEquals(params.getParameterByKey(Parameters.KEY.SEED), copy.getParameterByKey(Parameters.KEY.SEED));
    }


    public static class DummyContainerBase {
        private double minVal = 0;

        public double getMinVal() {
            return minVal;
        }

        public void setMinVal(double minVal) {
            this.minVal = minVal;
        }
    }

    public static class DummyContainer extends DummyContainerBase {
        private double maxVal = 0;

        public double getMaxVal() {
            return maxVal;
        }

        public void setMaxVal(double maxVal) {
            this.maxVal = maxVal;
        }
    }


}
