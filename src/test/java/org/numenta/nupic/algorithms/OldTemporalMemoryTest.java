package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.Test;
import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
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
	
	private boolean isSynapseDestroyed(Connections cn, Synapse synapse) {
	    Optional<Synapse> op1 = cn.getReceptorSynapseMapping()
	        .values()
	        .stream()
	        .flatMap(l -> l.stream())
	        .filter(s -> s.equals(synapse))
	        .findFirst();
	    
	    if(op1.isPresent()) { return false; }
	    
	    Optional<Synapse> op2 = cn.getSegmentMapping()
            .values()
            .stream()
            .flatMap(l -> l.stream())
            .flatMap(l2 -> l2.getAllSynapses(cn).stream())
            .filter(s -> s.equals(synapse))
            .findFirst();
        
        if(op2.isPresent()) { return false; }
	    
	    return true;
	}
	
	private boolean isSegmentDestroyed(Connections cn, DistalDendrite segment) {
	    Optional<DistalDendrite> op2 = cn.getSegmentMapping()
            .values()
            .stream()
            .flatMap(l -> l.stream())
            .filter(dd -> dd.equals(segment))
            .findFirst();
        
        if(op2.isPresent()) { return false; }
        
        return true;
	}
	
	@SuppressWarnings("unchecked")
    private <T> T deepCopyPlain(T t) {
	    FSTConfiguration fastSerialConfig = FSTConfiguration.createDefaultConfiguration();
	    byte[] bytes = fastSerialConfig.asByteArray(t);
	    return (T)fastSerialConfig.asObject(bytes);
	}

	@Test
	public void testActivateCorrectlyPredictedCells() {
		OldTemporalMemory tm = new OldTemporalMemory();
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
	
	@Test
	public void testBurstUnpredictedColumns() {
	    OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters();
        p.apply(cn);
        tm.init(cn);
        
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
        tm.init(cn);
        
        int[] previousActiveColumns = { 0 };
        Cell cell4 = cn.getCell(4);
        
        DistalDendrite activeSegment = cell4.createSegment(cn);
        activeSegment.createSynapse(cn, cn.getCell(0), 0.5);
        activeSegment.createSynapse(cn, cn.getCell(1), 0.5);
        activeSegment.createSynapse(cn, cn.getCell(2), 0.5);
        activeSegment.createSynapse(cn, cn.getCell(3), 0.5);
        
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
        tm.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        List<Cell> expectedWinnerCells = new ArrayList<>(cn.getCellSet(new int[] { 4, 6 }));
        
        DistalDendrite activeSegment1 = expectedWinnerCells.get(0).createSegment(cn);
        activeSegment1.createSynapse(cn, previousActiveCells[0], 0.5);
        activeSegment1.createSynapse(cn, previousActiveCells[1], 0.5);
        activeSegment1.createSynapse(cn, previousActiveCells[2], 0.5);
        
        DistalDendrite activeSegment2 = expectedWinnerCells.get(1).createSegment(cn);
        activeSegment2.createSynapse(cn, previousActiveCells[0], 0.5);
        activeSegment2.createSynapse(cn, previousActiveCells[1], 0.5);
        activeSegment2.createSynapse(cn, previousActiveCells[2], 0.5);
        
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
        tm.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell activeCell = cn.getCell(5);
        
        DistalDendrite activeSegment = activeCell.createSegment(cn);
        Synapse as1 = activeSegment.createSynapse(cn, previousActiveCells[0], 0.5);
        Synapse as2 = activeSegment.createSynapse(cn, previousActiveCells[1], 0.5);
        Synapse as3 = activeSegment.createSynapse(cn, previousActiveCells[2], 0.5);
        Synapse is1 = activeSegment.createSynapse(cn, cn.getCell(81), 0.5);
        
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
        tm.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell activeCell = cn.getCell(5);
        
        DistalDendrite activeSegment = activeCell.createSegment(cn);
        activeSegment.createSynapse(cn, previousActiveCells[0], 0.5);
        activeSegment.createSynapse(cn, previousActiveCells[1], 0.5);
        activeSegment.createSynapse(cn, previousActiveCells[2], 0.5);
        
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
        tm.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell[] burstingCells = {cn.getCell(4), cn.getCell(5) };
        
        DistalDendrite selectedMatchingSegment = burstingCells[0].createSegment(cn);
        Synapse as1 = selectedMatchingSegment.createSynapse(cn, previousActiveCells[0], 0.3);
        Synapse as2 = selectedMatchingSegment.createSynapse(cn, previousActiveCells[1], 0.3);
        Synapse as3 = selectedMatchingSegment.createSynapse(cn, previousActiveCells[2], 0.3);
        Synapse is1 = selectedMatchingSegment.createSynapse(cn, cn.getCell(81), 0.3);
        
        DistalDendrite otherMatchingSegment = burstingCells[1].createSegment(cn);
        otherMatchingSegment.createSynapse(cn, previousActiveCells[0], 0.3);
        otherMatchingSegment.createSynapse(cn, previousActiveCells[1], 0.3);
        otherMatchingSegment.createSynapse(cn, cn.getCell(81), 0.3);
        
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
        tm.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell[] burstingCells = {cn.getCell(4), cn.getCell(5) };
        
        DistalDendrite selectedMatchingSegment = burstingCells[0].createSegment(cn);
        selectedMatchingSegment.createSynapse(cn, previousActiveCells[0], 0.3);
        selectedMatchingSegment.createSynapse(cn, previousActiveCells[1], 0.3);
        selectedMatchingSegment.createSynapse(cn, previousActiveCells[2], 0.3);
        selectedMatchingSegment.createSynapse(cn, cn.getCell(81), 0.3);
        
        DistalDendrite otherMatchingSegment = burstingCells[1].createSegment(cn);
        Synapse as1 = otherMatchingSegment.createSynapse(cn, previousActiveCells[0], 0.3);
        Synapse as2 = otherMatchingSegment.createSynapse(cn, previousActiveCells[1], 0.3);
        Synapse is1 = otherMatchingSegment.createSynapse(cn, cn.getCell(81), 0.3);
        
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
        tm.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Cell expectedActiveCell = cn.getCell(4);
        Set<Cell> expectedActiveCells = Stream.of(expectedActiveCell).collect(Collectors.toCollection(LinkedHashSet<Cell>::new));
        Cell otherBurstingCell = cn.getCell(5);
        
        DistalDendrite activeSegment = expectedActiveCell.createSegment(cn);
        activeSegment.createSynapse(cn, previousActiveCells[0], 0.5);
        activeSegment.createSynapse(cn, previousActiveCells[1], 0.5);
        activeSegment.createSynapse(cn, previousActiveCells[2], 0.5);
        activeSegment.createSynapse(cn, previousActiveCells[3], 0.5);
        
        DistalDendrite matchingSegmentOnSameCell = expectedActiveCell.createSegment(cn);
        Synapse s1 = matchingSegmentOnSameCell.createSynapse(cn, previousActiveCells[0], 0.3);
        Synapse s2 = matchingSegmentOnSameCell.createSynapse(cn, previousActiveCells[1], 0.3);
        
        DistalDendrite matchingSegmentOnOtherCell = otherBurstingCell.createSegment(cn);
        Synapse s3 = matchingSegmentOnOtherCell.createSynapse(cn, previousActiveCells[0], 0.3);
        Synapse s4 = matchingSegmentOnOtherCell.createSynapse(cn, previousActiveCells[1], 0.3);
        
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
        tm.init(cn);
        
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
        tm.init(cn);
        
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
        List<Synapse> synapses = segments.get(0).getAllSynapses(cn);
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
        tm.init(cn);
        
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
        tm.init(cn);
        
        int[] previousActiveColumns = { 0, 1, 2, 3 };
        Set<Cell> prevWinnerCells = cn.getCellSet(new int[] { 0, 1, 2, 3 });
        int[] activeColumns = { 4 };
        
        DistalDendrite matchingSegment = cn.getCell(4).createSegment(cn);
        matchingSegment.createSynapse(cn, cn.getCell(0), 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertTrue(cc.winnerCells().equals(prevWinnerCells));
        cc = tm.compute(cn, activeColumns, true);
        
        List<Synapse> synapses = matchingSegment.getAllSynapses(cn);
        assertEquals(3, synapses.size());
        
        Collections.sort(synapses);
        synapses = synapses.subList(1, synapses.size());
        for(Synapse synapse : synapses) {
            assertEquals(0.21, synapse.getPermanence(), 0.01);
            assertTrue(prevWinnerCells.contains(synapse.getPresynapticCell()));
        }
    }
	
	@Test
	public void testDestroyWeakSynapseOnWrongPrediction() {
	    OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.INITIAL_PERMANENCE, 0.5);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        tm.init(cn);
        
        int[] previousActiveColumns = { 0 };
        Cell[] previousActiveCells = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        int[] activeColumns = { 2 };
        Cell expectedActiveCell = cn.getCell(5);
        
        DistalDendrite activeSegment = expectedActiveCell.createSegment(cn);
        activeSegment.createSynapse(cn, previousActiveCells[0], 0.5);
        activeSegment.createSynapse(cn, previousActiveCells[1], 0.5);
        activeSegment.createSynapse(cn, previousActiveCells[2], 0.5);
        Synapse weakActiveSynapse = activeSegment.createSynapse(cn, previousActiveCells[3], 0.015);
        
        tm.compute(cn, previousActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertTrue(isSynapseDestroyed(cn, weakActiveSynapse));
    }
	
	/**
	 * <b>NOTE: From the Python version</b>
	 * createSynapse in connections.py does not destroy the weakest synapses
     * when you reach the cap. This change would require changing the underlying
     * synapseData class (probably just add a destroyed flag) and change how
     * segments and synapses are deleted. See C++ version for reference.
     */
	@Ignore("Python Connections does not support this yet.")
	@Test
	public void testRecycleWeakestSynapseToMakeRoomForNewSynapse() {
	    OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.PERMANENCE_INCREMENT, 0.02);
        p = getDefaultParameters(p, KEY.PERMANENCE_DECREMENT, 0.02);
        //p = getDefaultParameters(p, KEY.MAX_SYNAPSES_PER_SEGMENT, 3); <-- SLATED FOR ADDITION WHEN FUNCTIONALITY IS INCLUDED
        p.apply(cn);
        tm.init(cn);
        
        int[] previousActiveColumns = { 0, 1, 2 };
        Cell[] prevWinnerCellArray = {cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        Set<Cell> prevWinnerCells = Arrays.stream(prevWinnerCellArray).collect(Collectors.toCollection(LinkedHashSet<Cell>::new));
        int[] activeColumns = { 4 };
        
        DistalDendrite matchingSegment = cn.getCell(4).createSegment(cn);
        matchingSegment.createSynapse(cn, cn.getCell(81), 0.6);
        
        Synapse weakestSynapse = matchingSegment.createSynapse(cn, cn.getCell(0), 0.11);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertTrue(cc.winnerCells().equals(prevWinnerCells));
        tm.compute(cn, activeColumns, true);
        
        assertFalse(weakestSynapse.getPresynapticCell().equals(cn.getCell(0)));
        
        assertFalse(isSynapseDestroyed(cn, weakestSynapse));
        
        assertEquals(0.21, weakestSynapse.getPermanence(), 0.01);
    }
	
	/**
	 * create Segment does not recycle segments to make room for similar reasoning
     * to the above test.
     */
	@Ignore("Python Connections does not support this yet.")
	@Test
	public void testRecycleLeastRecentlyActiveSegmentToMakeRoomForNewSegment() {
	    OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.PERMANENCE_INCREMENT, 0.02);        
        p = getDefaultParameters(p, KEY.PERMANENCE_DECREMENT, 0.02);
        p = getDefaultParameters(p, KEY.INITIAL_PERMANENCE, 0.5);
        //p = getDefaultParameters(p, KEY.MAX_SEGMENTS_PER_CELL, 2); <-- SLATED FOR ADDITION WHEN FUNCTIONALITY IS INCLUDED
        p.apply(cn);
        tm.init(cn);
        
        int[] prevActiveColumns1 = { 0, 1, 2 };
        int[] prevActiveColumns2 = { 0, 1, 2 };
        int[] prevActiveColumns3 = { 0, 1, 2 };
        int[] activeColumns = { 9 };
        
        tm.compute(cn, prevActiveColumns1, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(1, cn.getCell(9).getSegments(cn));
        DistalDendrite oldestSegment = cn.getCell(9).getSegments(cn).stream().sorted().findFirst().get();
        
        tm.reset(cn);
        tm.compute(cn, prevActiveColumns2, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(2, cn.getCell(9).getSegments(cn));
        
        tm.reset(cn);
        tm.compute(cn, prevActiveColumns3, true);
        tm.compute(cn, activeColumns, true);
        
        assertEquals(2, cn.getCell(9).getSegments(cn));
        
        List<Synapse> synapses = oldestSegment.getAllSynapses(cn);
        assertEquals(3, synapses.size());
        
        Set<Cell> presynapticCells = new LinkedHashSet<>();
        for(Synapse synapse : synapses) {
            presynapticCells.add(synapse.getPresynapticCell());
        }
        
        Set<Cell> expected = Arrays.stream(new Cell[] { cn.getCell(6), cn.getCell(7), cn.getCell(8) })
            .collect(Collectors.toCollection(LinkedHashSet<Cell>::new));
        assertTrue(expected.equals(presynapticCells));
	}
	
	@Ignore("Python Connections does not support this yet.")
	@Test
	public void testDestroySegmentsWithTooFewSynapsesToBeMatching() {
	    OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        tm.init(cn);
        
        int[] prevActiveColumns = { 0 };
        Cell[] prevActiveCells = { cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        int[] activeColumns = { 2 };
        Cell expectedActiveCell = cn.getCell(5);
        
        DistalDendrite matchingSegment = expectedActiveCell.createSegment(cn);
        matchingSegment.createSynapse(cn, prevActiveCells[0], 0.015);
        matchingSegment.createSynapse(cn, prevActiveCells[1], 0.015);
        matchingSegment.createSynapse(cn, prevActiveCells[2], 0.015);
        matchingSegment.createSynapse(cn, prevActiveCells[3], 0.015);
        
        tm.compute(cn, prevActiveColumns, true);
        tm.compute(cn, activeColumns, true);
        
        assertTrue(isSegmentDestroyed(cn, matchingSegment));
        assertEquals(0, expectedActiveCell.getSegments(cn).size());
	}
	
	@Test
	public void testPunishMatchingSegmentsInInactiveColumns() {
	    OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        p = getDefaultParameters(p, KEY.INITIAL_PERMANENCE, 0.2);
        p = getDefaultParameters(p, KEY.PREDICTED_SEGMENT_DECREMENT, 0.02);
        p.apply(cn);
        tm.init(cn);
        
        int[] prevActiveColumns = { 0 };
        Cell[] prevActiveCells = { cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        int[] activeColumns = { 1 };
        Cell previousInactiveCell = cn.getCell(81);
        
        DistalDendrite activeSegment = cn.getCell(42).createSegment(cn);
        Synapse as1 = activeSegment.createSynapse(cn, prevActiveCells[0], 0.5);
        Synapse as2 = activeSegment.createSynapse(cn, prevActiveCells[1], 0.5);
        Synapse as3 = activeSegment.createSynapse(cn, prevActiveCells[2], 0.5);
        Synapse is1 = activeSegment.createSynapse(cn, previousInactiveCell, 0.5);
        
        DistalDendrite matchingSegment = cn.getCell(43).createSegment(cn);
        Synapse as4 = matchingSegment.createSynapse(cn, prevActiveCells[0], 0.5);
        Synapse as5 = matchingSegment.createSynapse(cn, prevActiveCells[1], 0.5);
        Synapse is2 = matchingSegment.createSynapse(cn, previousInactiveCell, 0.5);
        
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
	        tm.init(cn);
	        
	        int[] prevActiveColumns = { 1, 2, 3, 4 };
	        Cell[] prevActiveCells = { cn.getCell(4), cn.getCell(5), cn.getCell(6), cn.getCell(7) };
	        int[] activeColumns = { 0 };
	        Cell[] nonMatchingCells = { cn.getCell(0), cn.getCell(3) };
	        Set<Cell> activeCells = cn.getCellSet(new int[] { 0, 1, 2, 3});
	        
	        DistalDendrite segment1 = nonMatchingCells[0].createSegment(cn);
	        segment1.createSynapse(cn, prevActiveCells[0], 0.5);
	        DistalDendrite segment2 = nonMatchingCells[1].createSegment(cn);
            segment2.createSynapse(cn, prevActiveCells[1], 0.5);
            
            tm.compute(cn, prevActiveColumns, true);
            ComputeCycle cc = tm.compute(cn, activeColumns, true);
            
            assertTrue(cc.activeCells().equals(activeCells));
            
            assertEquals(3, cn.getSegmentCount());
            assertEquals(1, cn.getCell(0).getSegments(cn).size());
            assertEquals(1, cn.getCell(3).getSegments(cn).size());
            assertEquals(1, segment1.getAllSynapses(cn).size());
            assertEquals(1, segment2.getAllSynapses(cn).size());
            
            List<DistalDendrite> segments = cn.getCell(1).getSegments(cn);
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
        tm.init(cn);
        
        int[] prevActiveColumns = { 0 };
        Cell[] prevActiveCells = { cn.getCell(0), cn.getCell(1), cn.getCell(2), cn.getCell(3) };
        int[] activeColumns = { 1, 2 };
        Cell prevInactiveCell = cn.getCell(81);
        Cell expectedActiveCell = cn.getCell(4);
        
        DistalDendrite correctActiveSegment = expectedActiveCell.createSegment(cn);
        correctActiveSegment.createSynapse(cn, prevActiveCells[0], 0.5);
        correctActiveSegment.createSynapse(cn, prevActiveCells[1], 0.5);
        correctActiveSegment.createSynapse(cn, prevActiveCells[2], 0.5);
        
        DistalDendrite wrongMatchingSegment = cn.getCell(43).createSegment(cn);
        wrongMatchingSegment.createSynapse(cn, prevActiveCells[0], 0.5);
        wrongMatchingSegment.createSynapse(cn, prevActiveCells[1], 0.5);
        wrongMatchingSegment.createSynapse(cn, prevInactiveCell, 0.5);
        
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
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        dd.createSynapse(cn, cn.getCell(3), 0.3);
        
        for(int i = 0;i < 100;i++) {
            assertEquals(1, tm.leastUsedCell(cn, cn.getColumn(0).getCells(), cn.getRandom(), true).getIndex());
        }
	}
	
	@Test
	public void testAdaptSegment() {
	    OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = Parameters.getAllDefaultParameters();
        p.apply(cn);
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(23), 0.6);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(37), 0.4);
        Synapse s3 = dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        dd.adaptSegment(cn.getCellSet(23, 37), cn, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        
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
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(23), 0.9);
        
        dd.adaptSegment(cn.getCellSet(23), cn, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertEquals(1.0, s1.getPermanence(), 0.1);
        
        // Now permanence should be at max
        dd.adaptSegment(cn.getCellSet(23), cn, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertEquals(1.0, s1.getPermanence(), 0.1);
	}
	
	@Test
    public void testAdaptSegmentToMin() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = Parameters.getAllDefaultParameters();
        p.apply(cn);
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(23), 0.1);
        dd.createSynapse(cn, cn.getCell(1), 0.3);
        
        dd.adaptSegment(cn.getCellSet(), cn, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertFalse(dd.getAllSynapses(cn).contains(s1));
    }
	
	@Test
	public void testNumberOfColumns() {
	    OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = Parameters.getAllDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 64, 64 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.apply(cn);
        tm.init(cn);
        
        assertEquals(64 * 64, cn.getNumColumns());
	}
	
	@Test
    public void testNumberOfCells() {
        OldTemporalMemory tm = new OldTemporalMemory();
        Connections cn = new Connections();
        Parameters p = Parameters.getAllDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 64, 64 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.apply(cn);
        tm.init(cn);
        
        assertEquals(64 * 64 * 32, cn.getCells().length);
    }
}
