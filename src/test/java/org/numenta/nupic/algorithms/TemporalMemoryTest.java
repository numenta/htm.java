package org.numenta.nupic.algorithms;

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
		retVal.setParameterByKey(KEY.SEED, 42);
		
		return retVal;
	}

	@Test
	public void testActivateCorrectlyPredictedCells() {
		TemporalMemory tm = new TemporalMemory();
		Connections cn = new Connections();
		Parameters p = getDefaultParameters();
		p.apply(cn);
        tm.init(cn);
        
        Cell cell4 = cn.getCell(4);
        Set<Cell> expectedActiveCells = Stream.of(cell4).collect(Collectors.toSet());
        int[] previousActiveColumns = new int[32];
        
        DistalDendrite activeSegment = cell4.createSegment(cn);
        activeSegment.createSynapse(cn, cn.getCell(0), 0.5);
        activeSegment.createSynapse(cn, cn.getCell(1), 0.5);
        activeSegment.createSynapse(cn, cn.getCell(2), 0.5);
        activeSegment.createSynapse(cn, cn.getCell(3), 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        
        cc.predictiveCells();
    }

}
