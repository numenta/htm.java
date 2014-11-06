/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package org.numenta.nupic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParametersTest {
    private Parameters parameters;
    private Connections mem;

    public Parameters setupParameters() {
        parameters = new Parameters();
        parameters.setParameterByKey(Parameters.KEY.INPUT_DIMENSIONS, new int[]{5});//5
        parameters.setParameterByKey(Parameters.KEY.COLUMN_DIMENSIONS, new int[]{5});//5
        parameters.setParameterByKey(Parameters.KEY.POTENTIAL_RADIUS, 1);//3
        parameters.setParameterByKey(Parameters.KEY.POTENTIAL_PCT, 0.1);//0.5
        parameters.setParameterByKey(Parameters.KEY.GLOBAL_INHIBITIONS, false);
        parameters.setParameterByKey(Parameters.KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.setParameterByKey(Parameters.KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 1);
        parameters.setParameterByKey(Parameters.KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.setParameterByKey(Parameters.KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.setParameterByKey(Parameters.KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.setParameterByKey(Parameters.KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.setParameterByKey(Parameters.KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(Parameters.KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.setParameterByKey(Parameters.KEY.MAX_BOOST, 10.0);
        parameters.setParameterByKey(Parameters.KEY.SEED, 1);
        parameters.setParameterByKey(Parameters.KEY.SP_VERBOSITY, 1);
        return parameters;
    }

    @Test
    public void testApply() {
        mem = new Connections();
        Parameters params = setupParameters();
        Parameters.apply(mem, parameters);
        assertEquals(5, mem.getInputDimensions()[0]);
        assertEquals(5, mem.getColumnDimensions()[0]);
        assertEquals(1, mem.getPotentialRadius());
        assertEquals(0.1, mem.getPotentialPct(), 0);
        assertEquals(false, mem.getGlobalInhibition());
        assertEquals(-1.0, mem.getLocalAreaDensity(), 0);
        assertEquals(1, mem.getNumActiveColumnsPerInhArea(), 0);
        assertEquals(1, mem.getStimulusThreshold(), 1);
        assertEquals(0.01, mem.getSynPermInactiveDec(), 0);
        assertEquals(0.1, mem.getSynPermActiveInc(), 0);
        assertEquals(0.1, mem.getSynPermConnected(), 0);
        assertEquals(0.1, mem.getMinPctOverlapDutyCycles(), 0);
        assertEquals(0.1, mem.getMinPctActiveDutyCycles(), 0);
        assertEquals(10, mem.getDutyCyclePeriod(), 0);
        assertEquals(10.0, mem.getMaxBoost(), 0);
        assertEquals(1, mem.getSeed());
        assertEquals(1, mem.getSpVerbosity());
        assertEquals(1, mem.getNumInputs());
        assertEquals(5, mem.getNumColumns());
    }
}
