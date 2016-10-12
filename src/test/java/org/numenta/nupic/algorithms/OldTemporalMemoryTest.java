package org.numenta.nupic.algorithms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.util.UniversalRandom;
import org.nustaq.serialization.FSTConfiguration;

public class OldTemporalMemoryTest { 
    
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
    
    @SuppressWarnings("unchecked")
    private <T> T deepCopyPlain(T t) {
        FSTConfiguration fastSerialConfig = FSTConfiguration.createDefaultConfiguration();
        byte[] bytes = fastSerialConfig.asByteArray(t);
        return (T)fastSerialConfig.asObject(bytes);
    }

    @Test
    public void testActivateCorrectlyPredictiveCells() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
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
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] activeColumns = { 0 };
        Set<Cell> burstingCells = cn.getCellSet(new int[] { 0, 1, 2, 3 });
        
        ComputeCycle cc = tm.compute(cn, activeColumns, true);
        
        assertTrue(cc.activeCells().equals(burstingCells));
    }
    
    @Test
    public void testZeroActiveColumns() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
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
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
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
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.PERMANENCE_DECREMENT, 0.08);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
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
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
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
    
    @Test
    public void testReinforcedSelectedMatchingSegmentInBurstingColumn() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.PERMANENCE_DECREMENT, 0.08);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell[] burstingCells = {cn.getCell(4), cn.getCell(5) };
        
        DistalDendrite activeSegment = cn.createSegment(burstingCells[0]);
        Synapse as1 = cn.createSynapse(activeSegment, previousActiveCells[0], 0.3);
        Synapse as2 = cn.createSynapse(activeSegment, previousActiveCells[0], 0.3);
        Synapse as3 = cn.createSynapse(activeSegment, previousActiveCells[0], 0.3);
        Synapse is1 = cn.createSynapse(activeSegment, cn.getCell(81), 0.3);
        
        DistalDendrite otherMatchingSegment = cn.createSegment(burstingCells[1]);
        cn.createSynapse(otherMatchingSegment, previousActiveCells[0], 0.3);
        cn.createSynapse(otherMatchingSegment, previousActiveCells[1], 0.3);
        cn.createSynapse(otherMatchingSegment, cn.getCell(81), 0.3);
        
        tm.compute(cn, previousActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(0.4, as1.getPermanence(), 0.01);
        assertEquals(0.4, as2.getPermanence(), 0.01);
        assertEquals(0.4, as3.getPermanence(), 0.01);
        assertEquals(0.22, is1.getPermanence(), 0.001);
    }
    
    @Test
    public void testNoChangeToNonSelectedMatchingSegmentsInBurstingColumn() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.PERMANENCE_DECREMENT, 0.08);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell[] burstingCells = {cn.getCell(4), cn.getCell(5) };
        
        DistalDendrite selectedMatchingSegment = cn.createSegment(burstingCells[0]);
        cn.createSynapse(selectedMatchingSegment, previousActiveCells[0], 0.3);
        cn.createSynapse(selectedMatchingSegment, previousActiveCells[1], 0.3);
        cn.createSynapse(selectedMatchingSegment, previousActiveCells[2], 0.3);
        cn.createSynapse(selectedMatchingSegment, cn.getCell(81), 0.3);
        
        DistalDendrite otherMatchingSegment = cn.createSegment(burstingCells[1]);
        Synapse as1 = cn.createSynapse(otherMatchingSegment, previousActiveCells[0], 0.3);
        Synapse as2 = cn.createSynapse(otherMatchingSegment, previousActiveCells[1], 0.3);
        Synapse is1 = cn.createSynapse(otherMatchingSegment, cn.getCell(81), 0.3);
        
        tm.compute(cn, previousActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(0.3, as1.getPermanence(), 0.01);
        assertEquals(0.3, as2.getPermanence(), 0.01);
        assertEquals(0.3, is1.getPermanence(), 0.01);
    }
    
    @Test
    public void testNoChangeToMatchingSegmentsInPredictedActiveColumn() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell expectedActiveCell = cn.getCell(4);
        Set<Cell> expectedActiveCells = Stream.of(expectedActiveCell).collect(Collectors.toCollection(LinkedHashSet<Cell>::new));
        Cell otherBurstingCell = cn.getCell(5);
        
        DistalDendrite activeSegment = cn.createSegment(expectedActiveCell);
        cn.createSynapse(activeSegment, previousActiveCells[0], 0.5);
        cn.createSynapse(activeSegment, previousActiveCells[1], 0.5);
        cn.createSynapse(activeSegment, previousActiveCells[2], 0.5);
        cn.createSynapse(activeSegment, previousActiveCells[3], 0.5);
        
        DistalDendrite matchingSegmentOnSameCell = cn.createSegment(expectedActiveCell);
        Synapse s1 = cn.createSynapse(matchingSegmentOnSameCell, previousActiveCells[0], 0.3);
        Synapse s2 = cn.createSynapse(matchingSegmentOnSameCell, previousActiveCells[1], 0.3);
        
        DistalDendrite matchingSegmentOnOtherCell = cn.createSegment(otherBurstingCell);
        Synapse s3 = cn.createSynapse(matchingSegmentOnOtherCell, previousActiveCells[0], 0.3);
        Synapse s4 = cn.createSynapse(matchingSegmentOnOtherCell, previousActiveCells[1], 0.3);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertTrue(cc.predictiveCells().equals(expectedActiveCells));
        tm.compute(cn, activeColumns, true);
        
        assertEquals(0.3, s1.getPermanence(), 0.01);
        assertEquals(0.3, s2.getPermanence(), 0.01);
        assertEquals(0.3, s3.getPermanence(), 0.01);
        assertEquals(0.3, s4.getPermanence(), 0.01);
    }
    
    @Test
    public void testNoNewSegmentIfNotEnoughWinnerCells() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.MAX_NEW_SYNAPSE_COUNT, 2);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] zeroColumns = {};
        int[] activeColumns = { 0 };
        
        tm.compute(cn, zeroColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(0, cn.getSegmentCount(), 0);
    }
    
    @Test
    public void testNewSegmentAddSynapsesToSubsetOfWinnerCells() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.MAX_NEW_SYNAPSE_COUNT, 2);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0, 1, 2 };
        int[] activeColumns = { 4 };
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        
        Set<Cell> prevWinnerCells = cc.winnerCells();
        assertEquals(3, prevWinnerCells.size());
        
        cc = tm.compute(cn, activeColumns, true);
        
        List<Cell> winnerCells = new ArrayList<>(cc.winnerCells());
        assertEquals(1, winnerCells.size());
        List<DistalDendrite> segments = winnerCells.get(0).getSegments(cn);
        assertEquals(1, segments.size());
        List<Synapse> synapses = cn.unDestroyedSynapsesForSegment(segments.get(0));
        assertEquals(2, synapses.size());
        
        for(Synapse synapse : synapses) {
            assertEquals(0.21, synapse.getPermanence(), 0.01);
            assertTrue(prevWinnerCells.contains(synapse.getPresynapticCell()));
        }
    }
    
    @Test
    public void testNewSegmentAddSynapsesToAllWinnerCells() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0, 1, 2 };
        int[] activeColumns = { 4 };
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        List<Cell> prevWinnerCells = new ArrayList<>(cc.winnerCells());
        assertEquals(3, prevWinnerCells.size());
        
        cc = tm.compute(cn, activeColumns, true);
        
        List<Cell> winnerCells = new ArrayList<>(cc.winnerCells());
        assertEquals(1, winnerCells.size());
        List<DistalDendrite> segments = winnerCells.get(0).getSegments(cn);
        assertEquals(1, segments.size());
        List<Synapse> synapses = segments.get(0).getAllSynapses(cn);
        
        List<Cell> presynapticCells = new ArrayList<>();
        for(Synapse synapse : synapses) {
            assertEquals(0.21, synapse.getPermanence(), 0.01);
            presynapticCells.add(synapse.getPresynapticCell());
        }
        
        Collections.sort(presynapticCells);
        assertTrue(prevWinnerCells.equals(presynapticCells));
    }
    
    @Test
    public void testMatchingSegmentAddSynapsesToSubsetOfWinnerCells() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.CELLS_PER_COLUMN, 1);
        p = getDefaultParameters(p, KEY.MIN_THRESHOLD, 1);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0, 1, 2, 3 };
        Set<Cell> prevWinnerCells = cn.getCellSet(new int[] { 0, 1, 2, 3 });
        int[] activeColumns = { 4 };
        
        DistalDendrite matchingSegment = cn.createSegment(cn.getCell(4));
        cn.createSynapse(matchingSegment, cn.getCell(0), 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertTrue(cc.winnerCells().equals(prevWinnerCells));
        cc = tm.compute(cn, activeColumns, true);
        
        List<Synapse> synapses = cn.unDestroyedSynapsesForSegment(matchingSegment);
        assertEquals(3, synapses.size());
        
        Collections.sort(synapses);
        synapses = synapses.subList(1, synapses.size());
        for(Synapse synapse : synapses) {
            assertEquals(0.21, synapse.getPermanence(), 0.01);
            assertTrue(prevWinnerCells.contains(synapse.getPresynapticCell()));
        }
    }
    
    @Test
    public void testMatchingSegmentAddSynapsesToAllWinnerCells() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.CELLS_PER_COLUMN, 1);
        p = getDefaultParameters(p, KEY.MIN_THRESHOLD, 1);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0, 1 };
        Set<Cell> prevWinnerCells = cn.getCellSet(new int[] { 0, 1 });
        int[] activeColumns = { 4 };
        
        DistalDendrite matchingSegment = cn.createSegment(cn.getCell(4));
        cn.createSynapse(matchingSegment, cn.getCell(0), 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertTrue(cc.winnerCells().equals(prevWinnerCells));
        
        cc = tm.compute(cn, activeColumns, true);
        
        List<Synapse> synapses = cn.unDestroyedSynapsesForSegment(matchingSegment);
        assertEquals(2, synapses.size());
        
        Synapse synapse1 = synapses.get(1);
        assertEquals(.21, synapse1.getPermanence(), 0.001);
        assertEquals(cn.getCell(1), synapse1.getPresynapticCell());
    }
    
    @Test
    public void testDestroyWeakSynapseOnWrongPrediction() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        Cell[] previousActiveCells = { cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        int[] activeColumns = { 2 };
        Cell expectedActiveCell = cn.getCell(5);
        
        DistalDendrite activeSegment = cn.createSegment(expectedActiveCell);
        cn.createSynapse(activeSegment, previousActiveCells[0], 0.5);
        cn.createSynapse(activeSegment, previousActiveCells[1], 0.5);
        cn.createSynapse(activeSegment, previousActiveCells[2], 0.5);
        Synapse weakActiveSynapse = cn.createSynapse(activeSegment, previousActiveCells[3], 0.015);
        
        tm.compute(cn, previousActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertTrue(weakActiveSynapse.destroyed());
    }
    
    @Test
    public void testDestroyWeakSynapseOnActiveReinforce() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        Cell[] previousActiveCells = { cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        int[] activeColumns = { 2 };
        Cell expectedActiveCell = cn.getCell(5);
        
        DistalDendrite activeSegment = cn.createSegment(expectedActiveCell);
        cn.createSynapse(activeSegment, previousActiveCells[0], 0.5);
        cn.createSynapse(activeSegment, previousActiveCells[1], 0.5);
        cn.createSynapse(activeSegment, previousActiveCells[2], 0.5);
        Synapse weakInactSynapse = cn.createSynapse(activeSegment, previousActiveCells[3], 0.009);
        
        tm.compute(cn, previousActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertTrue(weakInactSynapse.destroyed());
    }
    
    @Test
    public void testRecycleWeakestSynapseToMakeRoomForNewSynapse() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.CELLS_PER_COLUMN, 1);
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 100 });
        p = getDefaultParameters(p, KEY.MIN_THRESHOLD, 1);
        p = getDefaultParameters(p, KEY.PERMANENCE_INCREMENT, 0.02);
        p = getDefaultParameters(p, KEY.PERMANENCE_DECREMENT, 0.02);
        p.set(KEY.MAX_SYNAPSES_PER_SEGMENT, 3);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        assertEquals(3, cn.getMaxSynapsesPerSegment());
        
        int[] prevActiveColumns = { 0, 1, 2 };
        Set<Cell> prevWinnerCells = cn.getCellSet(new int[] { 0, 1, 2 });
        int[] activeColumns = { 4 };
        
        DistalDendrite matchingSegment = cn.createSegment(cn.getCell(4));
        cn.createSynapse(matchingSegment, cn.getCell(81), 0.6);
        
        Synapse weakestSynapse = cn.createSynapse(matchingSegment, cn.getCell(0), 0.11);
        
        ComputeCycle cc = tm.compute(cn, prevActiveColumns, true);
        assertEquals(prevWinnerCells, cc.winnerCells);
        tm.compute(cn, activeColumns, true);
        
        assertNotEquals(cn.getCell(0), weakestSynapse.getPresynapticCell());
        
        assertFalse(weakestSynapse.destroyed());
        
        assertEquals(0.21, weakestSynapse.getPermanence(), .001);
    }
    
    @Test
    public void testRecycleLeastRecentlyActiveSegmentToMakeRoomForNewSegment() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.CELLS_PER_COLUMN, 1);
        p = getDefaultParameters(p, KEY.INITIAL_PERMANENCE, 0.5);
        p = getDefaultParameters(p, KEY.PERMANENCE_INCREMENT, 0.02);
        p = getDefaultParameters(p, KEY.PERMANENCE_DECREMENT, 0.02);
        p.set(KEY.MAX_SEGMENTS_PER_CELL, 2);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] prevActiveColumns1 = { 0, 1, 2 };
        int[] prevActiveColumns2 = { 3, 4, 5 };
        int[] prevActiveColumns3 = { 6, 7, 8 };
        int[] activeColumns = { 9 };
        Cell cell9 = cn.getCell(9);
        
        tm.compute(cn, prevActiveColumns1, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(1, cn.unDestroyedSegmentsForCell(cell9).size());
        DistalDendrite oldestSegment = cn.unDestroyedSegmentsForCell(cell9).get(0);
        tm.reset(cn);
        tm.compute(cn,  prevActiveColumns2, true);
        tm.compute(cn,  activeColumns, true);
        
        assertEquals(2, cn.unDestroyedSegmentsForCell(cell9).size());
        
        tm.reset(cn);
        tm.compute(cn,  prevActiveColumns3, true);
        tm.compute(cn,  activeColumns, true);
        assertEquals(2, cn.unDestroyedSegmentsForCell(cell9).size());
        
        List<Synapse> synapses = cn.unDestroyedSynapsesForSegment(oldestSegment);
        assertEquals(3, synapses.size());
        
        Set<Cell> presynapticCells = new LinkedHashSet<>();
        for(Synapse synapse : cn.getSynapses(oldestSegment)) {
            presynapticCells.add(synapse.getPresynapticCell());
        }
        
        Set<Cell> expected = cn.getCellSet(new int[] { 6, 7, 8 });
        assertEquals(expected, presynapticCells);
    }
    
    @Test
    public void testDestroySegmentsWithTooFewSynapsesToBeMatching() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.INITIAL_PERMANENCE, .2);
        p = getDefaultParameters(p, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] prevActiveColumns = { 0 };
        Cell[] prevActiveCells = { cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        int[] activeColumns = { 2 };
        Cell expectedActiveCell = cn.getCell(5);
        
        DistalDendrite matchingSegment = cn.createSegment(cn.getCell(5));
        cn.createSynapse(matchingSegment, prevActiveCells[0], .015);
        cn.createSynapse(matchingSegment, prevActiveCells[1], .015);
        cn.createSynapse(matchingSegment, prevActiveCells[2], .015);
        cn.createSynapse(matchingSegment, prevActiveCells[3], .015);
        
        tm.compute(cn, prevActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertTrue(cn.getSegments(expectedActiveCell).contains(matchingSegment));
        assertFalse(cn.unDestroyedSegmentsForCell(expectedActiveCell).contains(matchingSegment));
        assertTrue(matchingSegment.destroyed());
        assertTrue(cn.unDestroyedSegmentsForCell(expectedActiveCell).isEmpty());
    }
    
    @Test
    public void testPunishMatchingSegmentsInInactiveColumns() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] prevActiveColumns = { 0 };
        Cell[] prevActiveCells = { cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        int[] activeColumns = { 1 };
        Cell previousInactiveCell = cn.getCell(81);
        
        DistalDendrite activeSegment = cn.createSegment(cn.getCell(42));
        Synapse as1 = cn.createSynapse(activeSegment, prevActiveCells[0], .5);
        Synapse as2 = cn.createSynapse(activeSegment, prevActiveCells[1], .5);
        Synapse as3 = cn.createSynapse(activeSegment, prevActiveCells[2], .5);
        Synapse is1 = cn.createSynapse(activeSegment, previousInactiveCell, .5);
        
        DistalDendrite matchingSegment = cn.createSegment(cn.getCell(43));
        Synapse as4 = cn.createSynapse(matchingSegment, prevActiveCells[0], .5);
        Synapse as5 = cn.createSynapse(matchingSegment, prevActiveCells[1], .5);
        Synapse is2 = cn.createSynapse(matchingSegment, previousInactiveCell, .5);
        
        tm.compute(cn, prevActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(0.48, as1.getPermanence(), 0.01);
        assertEquals(0.48, as2.getPermanence(), 0.01);
        assertEquals(0.48, as3.getPermanence(), 0.01);
        assertEquals(0.48, as4.getPermanence(), 0.01);
        assertEquals(0.48, as5.getPermanence(), 0.01);
        assertEquals(0.50, is1.getPermanence(), 0.01);
        assertEquals(0.50, is2.getPermanence(), 0.01);
    }
    
    @Test
    public void testAddSegmentToCellWithFewestSegments() {
        boolean grewOnCell1 = false;
        boolean grewOnCell2 = false;
        
        for(int seed = 0;seed < 100;seed++) {
            OldTemporalMemory tm = new OldTemporalMemory();
            Connections cn = new Connections();
            Parameters p = getDefaultParameters(null, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
            p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
            p = getDefaultParameters(p, KEY.SEED, seed);
            p.apply(cn);
            OldTemporalMemory.init(cn);
            
            int[] prevActiveColumns = { 1, 2, 3, 4 };
            Cell[] prevActiveCells = { cn.getCell(4), cn.getCell(5), cn.getCell(6), cn.getCell(7) };
            int[] activeColumns = { 0 };
            Cell[] nonMatchingCells = { cn.getCell(0), cn.getCell(3) };
            Set<Cell> activeCells = cn.getCellSet(new int[] { 0, 1, 2, 3});
            
            DistalDendrite segment1 = cn.createSegment(nonMatchingCells[0]);
            cn.createSynapse(segment1, prevActiveCells[0], 0.5);
            DistalDendrite segment2 = cn.createSegment(nonMatchingCells[1]);
            cn.createSynapse(segment2, prevActiveCells[1], 0.5);
            
            tm.compute(cn, prevActiveColumns, true);
            ComputeCycle cc = tm.compute(cn, activeColumns, true);
            
            assertTrue(cc.activeCells().equals(activeCells));
            
            assertEquals(3, cn.getSegmentCount());
            assertEquals(1, cn.getCell(0).getSegments(cn).size());
            assertEquals(1, cn.getCell(3).getSegments(cn).size());
            assertEquals(1, segment1.getAllSynapses(cn).size());
            assertEquals(1, segment2.getAllSynapses(cn).size());
            
            List<DistalDendrite> segments = cn.getCell(1).getSegments(cn, true);
            if(segments.size() == 0) {
                List<DistalDendrite> segments2 = cn.getCell(2).getSegments(cn);
                assertFalse(segments2.size() == 0);
                grewOnCell2 = true;
                segments.addAll(segments2);
            } else {
                grewOnCell1 = true;
            }
            
            assertEquals(1, segments.size());
            List<Synapse> synapses = segments.get(0).getAllSynapses(cn);
            assertEquals(4, synapses.size());
            
            Set<Column> columnCheckList = cn.getColumnSet(prevActiveColumns);
            
            for(Synapse synapse : synapses) {
                assertEquals(0.2, synapse.getPermanence(), 0.01);
                
                Column column = synapse.getPresynapticCell().getColumn();
                assertTrue(columnCheckList.contains(column));
                columnCheckList.remove(column);
            }
            
            assertEquals(0, columnCheckList.size());
        }
        
        assertTrue(grewOnCell1);
        assertTrue(grewOnCell2);
    }
    
    @Test
    public void testConnectionsNeverChangeWhenLearningDisabled() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p = getDefaultParameters(p, KEY.INITIAL_PERMANENCE, 0.2);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        int[] prevActiveColumns = { 0 };
        Cell[] prevActiveCells = { cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        int[] activeColumns = { 1, 2 };
        Cell prevInactiveCell = cn.getCell(81);
        Cell expectedActiveCell = cn.getCell(4);
        
        DistalDendrite correctActiveSegment = cn.createSegment(expectedActiveCell);
        cn.createSynapse(correctActiveSegment, prevActiveCells[0], 0.5);
        cn.createSynapse(correctActiveSegment, prevActiveCells[1], 0.5);
        cn.createSynapse(correctActiveSegment, prevActiveCells[2], 0.5);
        
        DistalDendrite wrongMatchingSegment = cn.createSegment(cn.getCell(43));
        cn.createSynapse(wrongMatchingSegment, prevActiveCells[0], 0.5);
        cn.createSynapse(wrongMatchingSegment, prevActiveCells[1], 0.5);
        cn.createSynapse(wrongMatchingSegment, prevInactiveCell, 0.5);
        
        Map<Cell, HashSet<Synapse>> synMapBefore = deepCopyPlain(cn.getReceptorSynapseMapping());
        Map<Cell, List<DistalDendrite>> segMapBefore = deepCopyPlain(cn.getSegmentMapping());
        
        tm.compute(cn, prevActiveColumns, false);
        tm.compute(cn, activeColumns, false);
        
        assertTrue(synMapBefore != cn.getReceptorSynapseMapping());
        assertEquals(synMapBefore, cn.getReceptorSynapseMapping());
        assertTrue(segMapBefore != cn.getSegmentMapping());
        assertEquals(segMapBefore, cn.getSegmentMapping());
    }
    
    @Test
    public void testLeastUsedCell() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.COLUMN_DIMENSIONS, new int[] { 2 });
        p = getDefaultParameters(p, KEY.CELLS_PER_COLUMN, 2);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        DistalDendrite dd = cn.createSegment(cn.getCell(0));
        cn.createSynapse(dd, cn.getCell(3), 0.3);
        
        for(int i = 0;i < 100;i++) {
            assertEquals(1, tm.leastUsedCell(cn, cn.getColumn(0).getCells(), cn.getRandom()).getIndex());
        }
    }
    
    @Test
    public void testAdaptSegment() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = Parameters.getAllDefaultParameters();
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        DistalDendrite dd = cn.createSegment(cn.getCell(0));
        Synapse s1 = cn.createSynapse(dd, cn.getCell(23), 0.6);
        Synapse s2 = cn.createSynapse(dd, cn.getCell(37), 0.4);
        Synapse s3 = cn.createSynapse(dd, cn.getCell(477), 0.9);
        
        tm.adaptSegment(cn, dd, cn.getCellSet(23, 37), cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        
        assertEquals(0.7, s1.getPermanence(), 0.01);
        assertEquals(0.5, s2.getPermanence(), 0.01);
        assertEquals(0.8, s3.getPermanence(), 0.01);
    }
    
    @Test
    public void testAdaptSegmentToMax() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = Parameters.getAllDefaultParameters();
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        DistalDendrite dd = cn.createSegment(cn.getCell(0));
        Synapse s1 = cn.createSynapse(dd, cn.getCell(23), 0.9);
        
        tm.adaptSegment(cn, dd, cn.getCellSet(23), cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertEquals(1.0, s1.getPermanence(), 0.1);
        
        // Now permanence should be at max
        tm.adaptSegment(cn, dd, cn.getCellSet(23), cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertEquals(1.0, s1.getPermanence(), 0.1);
    }
    
    @Test
    public void testAdaptSegmentToMin() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = Parameters.getAllDefaultParameters();
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        DistalDendrite dd = cn.createSegment(cn.getCell(0));
        Synapse s1 = cn.createSynapse(dd, cn.getCell(23), 0.1);
        cn.createSynapse(dd, cn.getCell(1), 0.3);
        
        tm.adaptSegment(cn, dd, cn.getCellSet(), cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertFalse(cn.unDestroyedSynapsesForSegment(dd).contains(s1));
    }
    
    @Test
    public void testNumberOfColumns() {
        Connections cn = new Connections();
        Parameters p = Parameters.getAllDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 64, 64 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        assertEquals(64 * 64, cn.getNumColumns());
    }
    
    @Test
    public void testNumberOfCells() {
        Connections cn = new Connections();
        Parameters p = Parameters.getAllDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 64, 64 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.apply(cn);
        OldTemporalMemory.init(cn);
        
        assertEquals(64 * 64 * 32, cn.getCells().length);
    }
}
