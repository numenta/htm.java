package org.numenta.nupic.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.Segment;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.research.ComputeCycle;
import org.numenta.nupic.research.Connections;
import org.numenta.nupic.research.TemporalMemory;

public class TemporalMemoryTest {

	@Test
	public void testActivateCorrectlyPredictiveCells() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		ComputeCycle c = new ComputeCycle();
		
		int[] prevPredictiveCells = new int[] { 0, 237, 1026, 26337, 26339, 55536 };
		int[] activeColumns = new int[] { 32, 47, 823 };
		
		tm.activateCorrectlyPredictiveCells(c, tm.getCells(prevPredictiveCells), tm.getColumns(activeColumns));
		Set<Cell> activeCells = tm.getActiveCells();
		Set<Cell> winnerCells = tm.getWinnerCells();
		Set<Column> predictedColumns = tm.getPredictedColumns();
		
		int[] expectedActiveWinners = new int[] { 1026, 26337, 26339 };
		int[] expectedPredictCols = new int[] { 32, 823 };
		int idx = 0;
		for(Cell cell : activeCells) {
			assertEquals(expectedActiveWinners[idx++], cell.getIndex());
		}
		idx = 0;
		for(Cell cell : winnerCells) {
			assertEquals(expectedActiveWinners[idx++], cell.getIndex());
		}
		idx = 0;
		for(Column col : predictedColumns) {
			assertEquals(expectedPredictCols[idx++], col.getIndex());
		}
	}
	
	@Test
	public void testActivateCorrectlyPredictiveCellsEmpty() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		ComputeCycle c = new ComputeCycle();
		
		int[] prevPredictiveCells = new int[] {};
		int[] activeColumns = new int[] { 32, 47, 823 };
		
		tm.activateCorrectlyPredictiveCells(c, tm.getCells(prevPredictiveCells), tm.getColumns(activeColumns));
		Set<Cell> activeCells = c.activeCells();
		Set<Cell> winnerCells = c.winnerCells();
		Set<Column> predictedColumns = c.predictedColumns();
		
		assertTrue(activeCells.isEmpty());
		assertTrue(winnerCells.isEmpty());
		assertTrue(predictedColumns.isEmpty());
		
		//---
		
		prevPredictiveCells = new int[] { 0, 237, 1026, 26337, 26339, 55536 };
		activeColumns = new int[] {};
		tm.activateCorrectlyPredictiveCells(c, tm.getCells(prevPredictiveCells), tm.getColumns(activeColumns));
		activeCells = c.activeCells();
		winnerCells = c.winnerCells();
		predictedColumns = c.predictedColumns();
		
		assertTrue(activeCells.isEmpty());
		assertTrue(winnerCells.isEmpty());
		assertTrue(predictedColumns.isEmpty());
	}
	
	@Test
	public void testBurstColumns() {
		TemporalMemory tm = new TemporalMemory();
		tm.setCellsPerColumn(4);
		tm.setConnectedPermanence(0.50);
		tm.setMinThreshold(1);
		tm.setSeed(42);
		tm.init();
		
		Connections c = new Connections();
		
		int segmentCounter = 0;
		int synapseCounter = 0;
		
		Segment dd = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.6, synapseCounter++);
		Synapse s1 = dd.createSynapse(c, tm.getCell(37), 0.4, synapseCounter++);
		dd.createSynapse(c, tm.getCell(477), 0.9, synapseCounter++);
		
		Segment dd2 = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s2 = dd2.createSynapse(c, tm.getCell(49), 0.9, synapseCounter++);
		dd2.createSynapse(c, tm.getCell(3), 0.8, synapseCounter++);
		
		Segment dd3 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s3 = dd3.createSynapse(c, tm.getCell(733), 0.7, synapseCounter++);
		
		Segment dd4 = tm.getCell(108).createSegment(c, segmentCounter++);
		dd4.createSynapse(c, tm.getCell(486), 0.9, synapseCounter++);
		
		int[] activeColumns = new int[] { 0, 1, 26 };
		int[] predictedColumns = new int[] {26};
		
		Map<Segment, Set<Synapse>> activeSynapseSegments = new LinkedHashMap<Segment, Set<Synapse>>();
		activeSynapseSegments.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
		activeSynapseSegments.put(dd2, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s2 })));
		activeSynapseSegments.put(dd3, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3 })));
		
		ComputeCycle cycle = new ComputeCycle();
		tm.burstColumns(cycle, c, tm.getColumns(activeColumns), tm.getColumns(predictedColumns), activeSynapseSegments);
		
		List<Cell> activeCells = new ArrayList<Cell>(cycle.activeCells());
		List<Cell> winnerCells = new ArrayList<Cell>(cycle.winnerCells());
		List<Segment> learningSegments = new ArrayList<Segment>(cycle.learningSegments());
		
		assertEquals(8, activeCells.size());
		for(int i = 0;i < 8;i++) {
			assertEquals(i, activeCells.get(i).getIndex());
		}
		assertEquals(0, winnerCells.get(0).getIndex());
		assertEquals(6, winnerCells.get(1).getIndex());
		
		assertEquals(dd, learningSegments.get(0));
		//Test that one of the learning Dendrites was created during call to burst...
		assertNotEquals(dd, learningSegments.get(1));
		assertNotEquals(dd2, learningSegments.get(1));
		assertNotEquals(dd3, learningSegments.get(1));
		assertNotEquals(dd4, learningSegments.get(1));
	}
	
	@Test
	public void testBurstColumnsEmpty() {
		TemporalMemory tm = new TemporalMemory();
		tm.setCellsPerColumn(4);
		tm.setConnectedPermanence(0.50);
		tm.setMinThreshold(1);
		tm.setSeed(42);
		tm.init();
		
		Connections c = new Connections();
		
		int[] activeColumns = new int[] {};
		int[] predictedColumns = new int[] {};
		
		Map<Segment, Set<Synapse>> activeSynapseSegments = new LinkedHashMap<Segment, Set<Synapse>>();
		
		ComputeCycle cycle = new ComputeCycle();
		tm.burstColumns(cycle, c, tm.getColumns(activeColumns), tm.getColumns(predictedColumns), activeSynapseSegments);
		
		List<Cell> activeCells = new ArrayList<Cell>(c.activeCells());
		List<Cell> winnerCells = new ArrayList<Cell>(c.winnerCells());
		List<Segment> learningSegments = new ArrayList<Segment>(c.learningSegments());
		
		assertEquals(0, activeCells.size());
		assertEquals(0, winnerCells.size());
		assertEquals(0, learningSegments.size());
	}
	
	@Test
	public void testLearnOnSegments() {
		TemporalMemory tm = new TemporalMemory();
		tm.setMaxNewSynapseCount(2);
		tm.init();
		
		Connections c = new Connections();
		
		int segmentCounter = 0;
		int synapseCounter = 0;
		
		Segment dd = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.6, synapseCounter++);
		Synapse s1 = dd.createSynapse(c, tm.getCell(37), 0.4, synapseCounter++);
		Synapse s2 = dd.createSynapse(c, tm.getCell(477), 0.9, synapseCounter++);
		
		Segment dd1 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s3 = dd1.createSynapse(c, tm.getCell(733), 0.7, synapseCounter++);
		
		Segment dd2 = tm.getCell(8).createSegment(c, segmentCounter++);
		Synapse s4 = dd2.createSynapse(c, tm.getCell(486), 0.9, synapseCounter++);
		
		Segment dd3 = tm.getCell(100).createSegment(c, segmentCounter++);
		
		Set<Segment> prevActiveSegments = new LinkedHashSet<Segment>();
		prevActiveSegments.add(dd);
		prevActiveSegments.add(dd2);
		
		Map<Segment, Set<Synapse>> prevActiveSynapsesForSegment = new LinkedHashMap<Segment, Set<Synapse>>();
		prevActiveSynapsesForSegment.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
		prevActiveSynapsesForSegment.put(dd1, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3 })));
		
		Set<Segment> learningSegments = new LinkedHashSet<Segment>();
		learningSegments.add(dd1);
		learningSegments.add(dd3);
		Set<Cell> winnerCells = new LinkedHashSet<Cell>();
		winnerCells.add(tm.getCell(0));
		Set<Cell> prevWinnerCells = new LinkedHashSet<Cell>();
		prevWinnerCells.add(tm.getCell(10));
		prevWinnerCells.add(tm.getCell(11));
		prevWinnerCells.add(tm.getCell(12));
		prevWinnerCells.add(tm.getCell(13));
		prevWinnerCells.add(tm.getCell(14));
		
		///////////////// Validate State Before and After //////////////////////
		
		//Before
		
		//Check segment 0
		assertEquals(0.6, s0.getPermanence(), 0.01);
		assertEquals(0.4, s1.getPermanence(), 0.01);
		assertEquals(0.9, s2.getPermanence(), 0.01);
		
		//Check segment 1
		assertEquals(0.7, s3.getPermanence(), 0.01);
		assertEquals(1, dd1.getAllSynapses(c).size(), 0);
		
		//Check segment 2
		assertEquals(0.9, s4.getPermanence(), 0.01);
		assertEquals(1, dd2.getAllSynapses(c).size(), 0);
		
		//Check segment 3
		assertEquals(0, dd3.getAllSynapses(c).size(), 0);
		
		tm.learnOnSegments(c, prevActiveSegments, learningSegments, prevActiveSynapsesForSegment, winnerCells, prevWinnerCells);
		
		//After
		
		//Check segment 0
		assertEquals(0.7, s0.getPermanence(), 0.01); //was 0.6
		assertEquals(0.5, s1.getPermanence(), 0.01); //was 0.4
		assertEquals(0.8, s2.getPermanence(), 0.01); //was 0.9
		
		//Check segment 1
		assertEquals(0.8, s3.getPermanence(), 0.01); //was 0.7
		assertEquals(2, dd1.getAllSynapses(c).size(), 0); // was 1
		
		//Check segment 2
		assertEquals(0.9, s4.getPermanence(), 0.01); //unchanged
		assertEquals(1, dd2.getAllSynapses(c).size(), 0); //unchanged
		
		//Check segment 3
		assertEquals(2, dd3.getAllSynapses(c).size(), 0);// was 0
		
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testComputePredictiveCells() {
		TemporalMemory tm = new TemporalMemory();
		tm.setActivationThreshold(2);
		tm.init();
		
		Connections c = new Connections();
		
		int segmentCounter = 0;
		int synapseCounter = 0;
		
		Segment dd = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.6, synapseCounter++);
		Synapse s1 = dd.createSynapse(c, tm.getCell(37), 0.5, synapseCounter++);
		Synapse s2 = dd.createSynapse(c, tm.getCell(477), 0.9, synapseCounter++);
		
		Segment dd1 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s3 = dd1.createSynapse(c, tm.getCell(733), 0.7, synapseCounter++);
		Synapse s4 = dd1.createSynapse(c, tm.getCell(733), 0.4, synapseCounter++);
		
		Segment dd2 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s5 = dd2.createSynapse(c, tm.getCell(974), 0.9, synapseCounter++);
		
		Segment dd3 = tm.getCell(8).createSegment(c, segmentCounter++);
		Synapse s6 = dd3.createSynapse(c, tm.getCell(486), 0.9, synapseCounter++);
		
		Segment dd4 = tm.getCell(100).createSegment(c, segmentCounter++);
		
		Map<Segment, Set<Synapse>> activeSynapseSegments = new LinkedHashMap<Segment, Set<Synapse>>();
		activeSynapseSegments.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
		activeSynapseSegments.put(dd2, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3, s4 })));
		activeSynapseSegments.put(dd3, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s5 })));
		
		ComputeCycle cycle = new ComputeCycle();
		tm.computePredictiveCells(cycle, activeSynapseSegments);
		
		assertTrue(cycle.activeSegments().contains(dd) && cycle.activeSegments().size() == 1);
		assertTrue(cycle.predictiveCells().contains(tm.getCell(0)) && cycle.predictiveCells().size() == 1);
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testComputeActiveSynapses() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		Connections c = new Connections();
		
		int segmentCounter = 0;
		int synapseCounter = 0;
		
		Segment dd = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.6, synapseCounter++);
		Synapse s1 = dd.createSynapse(c, tm.getCell(37), 0.4, synapseCounter++);
		Synapse s2 = dd.createSynapse(c, tm.getCell(477), 0.9, synapseCounter++);
		
		Segment dd1 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s3 = dd1.createSynapse(c, tm.getCell(733), 0.7, synapseCounter++);
		
		Segment dd2 = tm.getCell(8).createSegment(c, segmentCounter++);
		Synapse s4 = dd2.createSynapse(c, tm.getCell(486), 0.9, synapseCounter++);
		
		Set<Cell> activeCells = new LinkedHashSet<Cell>(
			Arrays.asList(
				new Cell[] {
					tm.getCell(23), tm.getCell(37), tm.getCell(733), tm.getCell(4973) 
				} 
			)
		);
		
		Map<Segment, Set<Synapse>> activeSegmentSynapses = tm.computeActiveSynapses(c, activeCells);
		
		Set<Synapse> syns = activeSegmentSynapses.get(dd);
		assertEquals(2, syns.size());
		assertTrue(syns.contains(s0));
		assertTrue(syns.contains(s1));
		
		syns = activeSegmentSynapses.get(dd1);
		assertEquals(1, syns.size());
		assertTrue(syns.contains(s3));
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testGetBestMatchingCell() {
		TemporalMemory tm = new TemporalMemory();
		tm.setConnectedPermanence(0.50);
		tm.setMinThreshold(1);
		tm.setSeed(42);
		tm.init();
		
		Connections c = new Connections();
		
		int segmentCounter = 0;
		int synapseCounter = 0;
		
		Segment dd = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.6, synapseCounter++);
		Synapse s1 = dd.createSynapse(c, tm.getCell(37), 0.4, synapseCounter++);
		Synapse s2 = dd.createSynapse(c, tm.getCell(477), 0.9, synapseCounter++);
		
		Segment dd1 = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s3 = dd1.createSynapse(c, tm.getCell(49), 0.9, synapseCounter++);
		Synapse s4 = dd1.createSynapse(c, tm.getCell(3), 0.8, synapseCounter++);
		
		Segment dd2 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s5 = dd2.createSynapse(c, tm.getCell(733), 0.7, synapseCounter++);
		
		Segment dd3 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s6 = dd3.createSynapse(c, tm.getCell(486), 0.9, synapseCounter++);
		
		Map<Segment, Set<Synapse>> activeSegments = new LinkedHashMap<Segment, Set<Synapse>>();
		activeSegments.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
		activeSegments.put(dd1, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3 })));
		activeSegments.put(dd2, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s5 })));
		
		Object[] result = tm.getBestMatchingCell(c, tm.getColumn(0), activeSegments);
		assertEquals(dd, result[0]);
		assertEquals(0, ((Cell)result[1]).getIndex());
		
		result = tm.getBestMatchingCell(c, tm.getColumn(3), activeSegments);
		assertNull(result[0]);
		assertEquals(119, ((Cell)result[1]).getIndex());
		
		result = tm.getBestMatchingCell(c, tm.getColumn(999), activeSegments);
		assertNull(result[0]);
		assertEquals(31969, ((Cell)result[1]).getIndex());
		
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testGetBestMatchingCellFewestSegments() {
		TemporalMemory tm = new TemporalMemory();
		tm.setColumnDimensions(2);
		tm.setCellsPerColumn(2);
		tm.setConnectedPermanence(0.50);
		tm.setMinThreshold(1);
		tm.setSeed(42);
		tm.init();
		
		Connections c = new Connections();
		
		Segment dd = tm.getCell(0).createSegment(c, 0);
		Synapse s0 = dd.createSynapse(c, tm.getCell(3), 0.3, 0);
		
		Map<Segment, Set<Synapse>> activeSegments = new LinkedHashMap<Segment, Set<Synapse>>();
		
		//Never pick cell 0, always pick cell 1
		for(int i = 0;i < 100;i++) {
			Object[] result = tm.getBestMatchingCell(c, tm.getColumn(0), activeSegments);
			assertEquals(1, ((Cell)result[1]).getIndex());
		}
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testGetBestMatchingSegment() {
		TemporalMemory tm = new TemporalMemory();
		tm.setConnectedPermanence(0.50);
		tm.setMinThreshold(1);
		tm.init();
		
		Connections c = new Connections();
		
		int segmentCounter = 0;
		int synapseCounter = 0;
		
		Segment dd = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.6, synapseCounter++);
		Synapse s1 = dd.createSynapse(c, tm.getCell(37), 0.4, synapseCounter++);
		Synapse s2 = dd.createSynapse(c, tm.getCell(477), 0.9, synapseCounter++);
		
		Segment dd1 = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s3 = dd1.createSynapse(c, tm.getCell(49), 0.9, synapseCounter++);
		Synapse s4 = dd1.createSynapse(c, tm.getCell(3), 0.8, synapseCounter++);
		
		Segment dd2 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s5 = dd2.createSynapse(c, tm.getCell(733), 0.7, synapseCounter++);
		
		Segment dd3 = tm.getCell(8).createSegment(c, segmentCounter++);
		Synapse s6 = dd3.createSynapse(c, tm.getCell(486), 0.9, synapseCounter++);
		
		Map<Segment, Set<Synapse>> activeSegments = new LinkedHashMap<Segment, Set<Synapse>>();
		activeSegments.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
		activeSegments.put(dd1, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3 })));
		activeSegments.put(dd2, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s5 })));
		
		Segment result = tm.getBestMatchingSegment(c, tm.getCell(0), activeSegments);
		List<Synapse> resultSynapses = new ArrayList<Synapse>(result.getConnectedActiveSynapses(activeSegments, 0));
		assertEquals(dd, result);
		assertEquals(s0, resultSynapses.get(0));
		assertEquals(s1, resultSynapses.get(1));
		
		result = tm.getBestMatchingSegment(c, tm.getCell(1), activeSegments);
		resultSynapses = new ArrayList<Synapse>(result.getConnectedActiveSynapses(activeSegments, 0));
		assertEquals(dd2, result);
		assertEquals(s5, resultSynapses.get(0));
		
		result = tm.getBestMatchingSegment(c, tm.getCell(8), activeSegments);
		assertEquals(null, result);
		
		result = tm.getBestMatchingSegment(c, tm.getCell(100), activeSegments);
		assertEquals(null, result);
		
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testGetLeastUsedCell() {
		TemporalMemory tm = new TemporalMemory();
		tm.setColumnDimensions(2);
		tm.setCellsPerColumn(2);
		tm.setSeed(42);
		tm.init();
		
		Connections c = new Connections();
		
		Segment dd = tm.getCell(0).createSegment(c, 0);
		Synapse s0 = dd.createSynapse(c, tm.getCell(3), 0.3, 0);
		
		Column column0 = tm.getColumn(0);
		Random random = tm.getRandom();
		for(int i = 0;i < 100;i++) {
			Cell leastUsed = column0.getLeastUsedCell(c, tm.getRandom());
			assertEquals(1, leastUsed.getIndex());
		}
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testComputeActiveSynapsesNoActivity() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		Connections c = new Connections();
		
		int segmentCounter = 0;
		int synapseCounter = 0;
		
		Segment dd = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.6, synapseCounter++);
		Synapse s1 = dd.createSynapse(c, tm.getCell(37), 0.4, synapseCounter++);
		Synapse s2 = dd.createSynapse(c, tm.getCell(477), 0.9, synapseCounter++);
		
		Segment dd1 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s3 = dd1.createSynapse(c, tm.getCell(733), 0.7, synapseCounter++);
		
		Segment dd2 = tm.getCell(8).createSegment(c, segmentCounter++);
		Synapse s4 = dd2.createSynapse(c, tm.getCell(486), 0.9, synapseCounter++);
		
		Map<Segment, Set<Synapse>> result = tm.computeActiveSynapses(c, new LinkedHashSet<Cell>());
		assertTrue(result.isEmpty());
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testGetConnectedActiveSynapsesForSegment() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		Connections c = new Connections();
		
		int segmentCounter = 0;
		int synapseCounter = 0;
		
		Segment dd = tm.getCell(0).createSegment(c, segmentCounter++);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.6, synapseCounter++);
		Synapse s1 = dd.createSynapse(c, tm.getCell(37), 0.4, synapseCounter++);
		Synapse s2 = dd.createSynapse(c, tm.getCell(477), 0.9, synapseCounter++);
		
		Segment dd1 = tm.getCell(1).createSegment(c, segmentCounter++);
		Synapse s3 = dd1.createSynapse(c, tm.getCell(733), 0.7, synapseCounter++);
		
		Segment dd2 = tm.getCell(8).createSegment(c, segmentCounter++);
		Synapse s4 = dd2.createSynapse(c, tm.getCell(486), 0.9, synapseCounter++);
		
		Map<Segment, Set<Synapse>> activeSegments = new LinkedHashMap<Segment, Set<Synapse>>();
		activeSegments.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
		activeSegments.put(dd1, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3 })));
		
		List<Synapse> connectActive = new ArrayList<Synapse>(dd.getConnectedActiveSynapses(activeSegments, 0.5));
		assertEquals(1, connectActive.size());
		assertEquals(s0, connectActive.get(0));
		
		connectActive = new ArrayList<Synapse>(dd1.getConnectedActiveSynapses(activeSegments, 0.5));
		assertEquals(1, connectActive.size());
		assertEquals(s3, connectActive.get(0));
	}
	
	@Test
	public void testAdaptSegment() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		Connections c = new Connections();
		
		int segmentCounter = 0;
		int synapseCounter = 0;
		
		Segment dd = tm.getCell(0).createSegment(c, segmentCounter);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.6, synapseCounter++);
		Synapse s1 = dd.createSynapse(c, tm.getCell(37), 0.4, synapseCounter++);
		Synapse s2 = dd.createSynapse(c, tm.getCell(477), 0.9, synapseCounter++);
		
		Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>();
		activeSynapses.add(s0);
		activeSynapses.add(s1);
		dd.adaptSegment(c, activeSynapses, tm.getPermanenceIncrement(), tm.getPermanenceDecrement());
		
		assertEquals(0.7, s0.getPermanence(), 0.01);
		assertEquals(0.5, s1.getPermanence(), 0.01);
		assertEquals(0.8, s2.getPermanence(), 0.01);
	}
	
	@Test
	public void testAdaptSegmentToMax() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		Connections c = new Connections();
		
		Segment dd = tm.getCell(0).createSegment(c, 0);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.9, 0);
		
		Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>();
		activeSynapses.add(s0);
		
		dd.adaptSegment(c, activeSynapses, tm.getPermanenceIncrement(), tm.getPermanenceDecrement());
		assertEquals(1.0, s0.getPermanence(), 0.01);
		
		dd.adaptSegment(c, activeSynapses, tm.getPermanenceIncrement(), tm.getPermanenceDecrement());
		assertEquals(1.0, s0.getPermanence(), 0.01);
	}

	@Test
	public void testAdaptSegmentToMin() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		Connections c = new Connections();
		
		Segment dd = tm.getCell(0).createSegment(c, 0);
		Synapse s0 = dd.createSynapse(c, tm.getCell(23), 0.1, 0);
		
		Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>();
		
		dd.adaptSegment(c, activeSynapses, tm.getPermanenceIncrement(), tm.getPermanenceDecrement());
		assertEquals(0.0, s0.getPermanence(), 0.01);
		
		dd.adaptSegment(c, activeSynapses, tm.getPermanenceIncrement(), tm.getPermanenceDecrement());
		assertEquals(0.0, s0.getPermanence(), 0.01);
	}
	
	@Test
	public void testPickCellsToLearnOn() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		Connections c = new Connections();
		
		Segment dd = tm.getCell(0).createSegment(c, 0);
		
		Set<Cell> winnerCells = new LinkedHashSet<Cell>();
		winnerCells.add(tm.getCell(4));
		winnerCells.add(tm.getCell(47));
		winnerCells.add(tm.getCell(58));
		winnerCells.add(tm.getCell(93));
		
		List<Cell> learnCells = new ArrayList<Cell>(dd.pickCellsToLearnOn(c, 2, winnerCells, tm.getRandom()));
		assertEquals(2, learnCells.size());
		assertTrue(learnCells.contains(tm.getCell(4)));
		assertTrue(learnCells.contains(tm.getCell(58)));
		
		learnCells = new ArrayList<Cell>(dd.pickCellsToLearnOn(c, 100, winnerCells, tm.getRandom()));
		assertEquals(4, learnCells.size());
		assertEquals(58, learnCells.get(0).getIndex());
		assertEquals(93, learnCells.get(1).getIndex());
		assertEquals(4, learnCells.get(2).getIndex());
		assertEquals(47, learnCells.get(3).getIndex());
		
		learnCells = new ArrayList<Cell>(dd.pickCellsToLearnOn(c, 0, winnerCells, tm.getRandom()));
		assertEquals(0, learnCells.size());
	}
	
	@Test
	public void testPickCellsToLearnOnAvoidDuplicates() {
		TemporalMemory tm = new TemporalMemory();
		tm.init();
		
		Connections c = new Connections();
		
		Segment dd = tm.getCell(0).createSegment(c, 0);
		dd.createSynapse(c, tm.getCell(23), 0.6, 0);
		
		Set<Cell> winnerCells = new LinkedHashSet<Cell>();
		winnerCells.add(tm.getCell(23));
		
		List<Cell> learnCells = new ArrayList<Cell>(dd.pickCellsToLearnOn(c, 2, winnerCells, tm.getRandom()));
		assertTrue(learnCells.isEmpty());
	}
}
