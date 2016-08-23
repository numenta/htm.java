package org.numenta.nupic.algorithms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.util.UniversalRandom;

public class TemporalMemoryTest { 
    
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
    
    private Parameters getDefaultParameters(Parameters p, KEY key, Object value) {
        Parameters retVal = p == null ? getDefaultParameters() : p;
        retVal.set(key, value);
        
        return retVal;
    }

    @Test
    public void testActivateCorrectlyPredictiveCells() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        TemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell cell4 = cn.getCell(4);
        Set<Cell> expectedActiveCells = Stream.of(cell4).collect(Collectors.toSet());
        
        DistalDendrite activeSegment = cn.createSegment(cell4);
        cn.createSynapse(activeSegment, cn.getCell(0), 0.5);
        cn.createSynapse(activeSegment, cn.getCell(1), 0.5);
        cn.createSynapse(activeSegment, cn.getCell(2), 0.5);
        cn.createSynapse(activeSegment, cn.getCell(3), 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertTrue(cc.predictiveCells().equals(expectedActiveCells));
        ComputeCycle cc2 = tm.compute(cn, activeColumns, true);
        assertTrue(cc2.activeCells().equals(expectedActiveCells));
    }
    
    @Test
    public void testBurstUnpredictedColumns() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        TemporalMemory.init(cn);
        
        int[] activeColumns = { 0 };
        Set<Cell> burstingCells = cn.getCellSet(new int[] { 0, 1, 2, 3 });
        
        ComputeCycle cc = tm.compute(cn, activeColumns, true);
        
        assertTrue(cc.activeCells().equals(burstingCells));
    }
    
    @Test
    public void testZeroActiveColumns() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        TemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        Cell cell4 = cn.getCell(4);
        
        DistalDendrite activeSegment = cn.createSegment(cell4);
        cn.createSynapse(activeSegment, cn.getCell(0), 0.5);
        cn.createSynapse(activeSegment, cn.getCell(1), 0.5);
        cn.createSynapse(activeSegment, cn.getCell(2), 0.5);
        cn.createSynapse(activeSegment, cn.getCell(3), 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertFalse(cc.activeCells().size() == 0);
        assertFalse(cc.winnerCells().size() == 0);
        assertFalse(cc.predictiveCells().size() == 0);
        
        int[] zeroColumns = new int[0];
        ComputeCycle cc2 = tm.compute(cn, zeroColumns, true);
        assertTrue(cc2.activeCells().size() == 0);
        assertTrue(cc2.winnerCells().size() == 0);
        assertTrue(cc2.predictiveCells().size() == 0);
    }

    @Test
    public void testPredictedActiveCellsAreAlwaysWinners() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        TemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = { cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        List<Cell> expectedWinnerCells = new ArrayList<>(cn.getCellSet(new int[] { 4, 6 }));
        
        DistalDendrite activeSegment1 = cn.createSegment(expectedWinnerCells.get(0));
        cn.createSynapse(activeSegment1, previousActiveCells[0], 0.5);
        cn.createSynapse(activeSegment1, previousActiveCells[1], 0.5);
        cn.createSynapse(activeSegment1, previousActiveCells[2], 0.5);
        
        DistalDendrite activeSegment2 = cn.createSegment(expectedWinnerCells.get(1));
        cn.createSynapse(activeSegment2, previousActiveCells[0], 0.5);
        cn.createSynapse(activeSegment2, previousActiveCells[1], 0.5);
        cn.createSynapse(activeSegment2, previousActiveCells[2], 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, false); // learn=false
        cc = tm.compute(cn, activeColumns, false); // learn=false
        
        assertTrue(cc.winnerCells.equals(new LinkedHashSet<Cell>(expectedWinnerCells)));
    }
    
    @Test
    public void testReinforcedCorrectlyActiveSegments() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.PERMANENCE_DECREMENT, 0.08);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        TemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell activeCell = cn.getCell(5);
        
        DistalDendrite activeSegment = cn.createSegment(activeCell);
        Synapse as1 = cn.createSynapse(activeSegment, previousActiveCells[0], 0.5);
        Synapse as2 = cn.createSynapse(activeSegment, previousActiveCells[1], 0.5);
        Synapse as3 = cn.createSynapse(activeSegment, previousActiveCells[2], 0.5);
        Synapse is1 = cn.createSynapse(activeSegment, cn.getCell(81), 0.5);
        
        tm.compute(cn, previousActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(0.6, as1.getPermanence(), 0.1);
        assertEquals(0.6, as2.getPermanence(), 0.1);
        assertEquals(0.6, as3.getPermanence(), 0.1);
        assertEquals(0.42, is1.getPermanence(), 0.001);
    }
    
    @Test
    public void testNoGrowthOnCorrectlyActiveSegments() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        TemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell activeCell = cn.getCell(5);
        
        DistalDendrite activeSegment = cn.createSegment(activeCell);
        cn.createSynapse(activeSegment, previousActiveCells[0], 0.5);
        cn.createSynapse(activeSegment, previousActiveCells[1], 0.5);
        cn.createSynapse(activeSegment, previousActiveCells[2], 0.5);
        
        tm.compute(cn, previousActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(3, activeSegment.getAllSynapses(cn).size());
    }
}
