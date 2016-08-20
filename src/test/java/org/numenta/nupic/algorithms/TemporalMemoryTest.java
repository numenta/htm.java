package org.numenta.nupic.algorithms;

import static org.junit.Assert.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.util.UniversalRandom;

public class TemporalMemoryTest { 
    
    private Parameters getDefaultParameters() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        retVal.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 3);
        retVal.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.21);
        retVal.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.5);
        retVal.setParameterByKey(KEY.MIN_THRESHOLD, 2);
        retVal.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 3);
        retVal.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.10);
        retVal.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.10);
        retVal.setParameterByKey(KEY.PREDICTED_SEGMENT_DECREMENT, 0.0);
        retVal.setParameterByKey(KEY.RANDOM, new UniversalRandom(42));
        retVal.setParameterByKey(KEY.SEED, 42);
        
        return retVal;
    }
    
    private Parameters getDefaultParameters(Parameters p, KEY key, Object value) {
        Parameters retVal = p == null ? getDefaultParameters() : p;
        retVal.setParameterByKey(key, value);
        
        return retVal;
    }

    @Test
    public void testActivateCorrectlyPredictiveCells() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        tm.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell cell4 = cn.getCell(4);
        Set<Cell> expectedActiveCells = Stream.of(cell4).collect(Collectors.toSet());
        
        DistalDendrite activeSegment = cell4.createSegment(cn);
        activeSegment.createSynapse(cn, cn.getCell(0), 0.5);
        activeSegment.createSynapse(cn, cn.getCell(1), 0.5);
        activeSegment.createSynapse(cn, cn.getCell(2), 0.5);
        activeSegment.createSynapse(cn, cn.getCell(3), 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertTrue(cc.predictiveCells().equals(expectedActiveCells));
        ComputeCycle cc2 = tm.compute(cn, activeColumns, true);
        assertTrue(cc2.activeCells().equals(expectedActiveCells));
    }

}
