package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.algorithms.TemporalMemory.CellSearch;
import org.numenta.nupic.algorithms.TemporalMemory.SegmentSearch;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;

/**
 * Basic unit test for {@link TemporalMemory}
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class TemporalMemoryTest {

    @Test
    public void testActivateCorrectlyPredictiveCells() {
        
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        ComputeCycle c = new ComputeCycle();
        
        int[] prevPredictiveCells = new int[] { 0, 237, 1026, 26337, 26339, 55536 };
        int[] activeColumns = new int[] { 32, 47, 823 };
        Set<Cell> prevMatchingCells = new HashSet<>();
        
        tm.activateCorrectlyPredictiveCells(
            cn, c, cn.getCellSet(prevPredictiveCells), prevMatchingCells, cn.getColumnSet(activeColumns));
        Set<Cell> activeCells = cn.getActiveCells();
        Set<Cell> winnerCells = cn.getWinnerCells();
        Set<Column> predictedColumns = cn.getSuccessfullyPredictedColumns();
        
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
        
        assertTrue(c.predictedInactiveCells().isEmpty());
    }
    
    @Test
    public void testActivateCorrectlyPredictiveCellsEmpty() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        // No previous predictive cells, no active columns
        
        ComputeCycle c = new ComputeCycle();
        
        int[] prevPredictiveCells = new int[] {};
        int[] activeColumns = new int[] {};
        Set<Cell> prevMatchingCells = new HashSet<>();
        
        tm.activateCorrectlyPredictiveCells(
            cn, c, cn.getCellSet(prevPredictiveCells), prevMatchingCells, cn.getColumnSet(activeColumns));
        Set<Cell> activeCells = c.activeCells();
        Set<Cell> winnerCells = c.winnerCells();
        Set<Column> predictedColumns = c.successfullyPredictedColumns();
        
        assertTrue(activeCells.isEmpty());
        assertTrue(winnerCells.isEmpty());
        assertTrue(predictedColumns.isEmpty());
        assertTrue(c.predictedInactiveCells().isEmpty());
        
        // No previous predictive cells, with active columns
        
        c = new ComputeCycle();
        
        prevPredictiveCells = new int[] {};
        activeColumns = new int[] { 32, 47, 823 };
        prevMatchingCells = new HashSet<>();
        
        tm.activateCorrectlyPredictiveCells(
            cn, c, cn.getCellSet(prevPredictiveCells), prevMatchingCells, cn.getColumnSet(activeColumns));
        activeCells = c.activeCells();
        winnerCells = c.winnerCells();
        predictedColumns = c.successfullyPredictedColumns();
        
        assertTrue(activeCells.isEmpty());
        assertTrue(winnerCells.isEmpty());
        assertTrue(predictedColumns.isEmpty());
        assertTrue(c.predictedInactiveCells().isEmpty());
        
        // No active columns, with previously predictive cells
        
        c = new ComputeCycle();
        
        prevPredictiveCells = new int[] { 0, 237, 1026, 26337, 26339, 55536 };
        activeColumns = new int[] {};
        tm.activateCorrectlyPredictiveCells(
            cn, c, cn.getCellSet(prevPredictiveCells), prevMatchingCells, cn.getColumnSet(activeColumns));
        activeCells = c.activeCells();
        winnerCells = c.winnerCells();
        predictedColumns = c.successfullyPredictedColumns();
        
        assertTrue(activeCells.isEmpty());
        assertTrue(winnerCells.isEmpty());
        assertTrue(predictedColumns.isEmpty());
        assertTrue(c.predictedInactiveCells().isEmpty());
    }
    
    @Test
    public void testActivateCorrectlyPredictiveCellsOrphan() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        cn.setPredictedSegmentDecrement(0.001);
        
        ComputeCycle c = new ComputeCycle();
        
        int[] prevPredictiveCells = new int[] {};
        int[] activeColumns = new int[] { 32, 47, 823 };
        int[] prevMatchingCells = new int[] { 32, 47 };
        
        tm.activateCorrectlyPredictiveCells(
            cn, c, cn.getCellSet(prevPredictiveCells), 
                cn.getCellSet(prevMatchingCells), 
                    cn.getColumnSet(activeColumns));
        Set<Cell> activeCells = c.activeCells();
        Set<Cell> winnerCells = c.winnerCells();
        Set<Column> predictedColumns = c.successfullyPredictedColumns();
        Set<Cell> predictedInactiveCells = c.predictedInactiveCells();
        
        assertTrue(activeCells.isEmpty());
        assertTrue(winnerCells.isEmpty());
        assertTrue(predictedColumns.isEmpty());
        
        Integer[] expectedPredictedInactives = { 32, 47 };
        assertTrue(Arrays.equals(
            expectedPredictedInactives, Connections.asCellIndexes(predictedInactiveCells).toArray()));
        
    }
    
    @Test
    public void testBurstColumns() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setCellsPerColumn(4);
        cn.setConnectedPermanence(0.50);
        cn.setMinThreshold(1);
        cn.setSeed(42);
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        dd.createSynapse(cn, cn.getCell(23), 0.6);
        dd.createSynapse(cn, cn.getCell(37), 0.4);
        dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        DistalDendrite dd2 = cn.getCell(0).createSegment(cn);
        dd2.createSynapse(cn, cn.getCell(49), 0.9);
        dd2.createSynapse(cn, cn.getCell(3), 0.8);
        
        DistalDendrite dd3 = cn.getCell(1).createSegment(cn);
        dd3.createSynapse(cn, cn.getCell(733), 0.7);
        
        DistalDendrite dd4 = cn.getCell(108).createSegment(cn);
        dd4.createSynapse(cn, cn.getCell(486), 0.9);
        
        int[] activeColumns = new int[] { 0, 1, 26 };
        int[] predictedColumns = new int[] { 26 };
        int[] prevActiveCells = new int[] { 23, 37, 49, 733 };
        int[] prevWinnerCells = new int[] { 23, 37, 49, 733 };
        
        ComputeCycle cycle = new ComputeCycle();
        tm.burstColumns(cycle, cn, cn.getColumnSet(activeColumns), 
            cn.getColumnSet(predictedColumns), cn.getCellSet(prevActiveCells), cn.getCellSet(prevWinnerCells));
        
        List<Cell> activeCells = new ArrayList<Cell>(cycle.activeCells());
        List<Cell> winnerCells = new ArrayList<Cell>(cycle.winnerCells());
        List<DistalDendrite> learningSegments = new ArrayList<DistalDendrite>(cycle.learningSegments());
        
        assertEquals(8, activeCells.size());
        for(int i = 0;i < 8;i++) {
            assertEquals(i, activeCells.get(i).getIndex());
        }
        assertEquals(0, winnerCells.get(0).getIndex());
        assertEquals(5, winnerCells.get(1).getIndex());
        
        assertEquals(dd, learningSegments.get(0));
        //Test that one of the learning Dendrites was created during call to burst...
        assertTrue(!dd.equals(learningSegments.get(1)));
        assertTrue(!dd2.equals(learningSegments.get(1)));
        assertTrue(!dd3.equals(learningSegments.get(1)));
        assertTrue(!dd4.equals(learningSegments.get(1)));
        
        // Check that new segment was added to winner cell (6) in column 1
        assertTrue(!cn.getSegments(cn.getCell(5)).isEmpty() && cn.getSegments(cn.getCell(5)).get(0).getIndex() == 4);
    }
    
    @Test
    public void testBurstColumnsEmpty() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setCellsPerColumn(4);
        tm.init(cn);
        
        int[] activeColumns = new int[] { };
        int[] predictedColumns = new int[] { };
        int[] prevActiveCells = new int[] { };
        int[] prevWinnerCells = new int[] { };
        
        ComputeCycle cycle = new ComputeCycle();
        tm.burstColumns(cycle, cn, cn.getColumnSet(activeColumns), 
            cn.getColumnSet(predictedColumns), cn.getCellSet(prevActiveCells), cn.getCellSet(prevWinnerCells));
        
        assertTrue(cycle.activeCells().isEmpty());
        assertTrue(cycle.winnerCells().isEmpty());
        assertTrue(cycle.learningSegments.isEmpty());
    }
    
    @Test
    public void testLearnOnSegments() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setMaxNewSynapseCount(2);
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        DistalDendrite dd1 = cn.getCell(1).createSegment(cn);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(733), 0.7);
        
        DistalDendrite dd2 = cn.getCell(8).createSegment(cn);
        Synapse s4 = dd2.createSynapse(cn, cn.getCell(486), 0.9);
        
        DistalDendrite dd3 = cn.getCell(100).createSegment(cn);
        
        Set<DistalDendrite> prevActiveSegments = new LinkedHashSet<>();
        prevActiveSegments.add(dd);
        prevActiveSegments.add(dd2);
        
        Set<DistalDendrite> learningSegments = new LinkedHashSet<>();
        learningSegments.add(dd1);
        learningSegments.add(dd3);
        Set<Cell> prevActiveCells = cn.getCellSet(new int[] { 23, 37, 733 });
        Set<Cell> winnerCells = new LinkedHashSet<>();
        winnerCells.add(cn.getCell(0));
        Set<Cell> prevWinnerCells = new LinkedHashSet<>();
        prevWinnerCells.add(cn.getCell(10));
        prevWinnerCells.add(cn.getCell(11));
        prevWinnerCells.add(cn.getCell(12));
        prevWinnerCells.add(cn.getCell(13));
        prevWinnerCells.add(cn.getCell(14));
        Set<Cell> predictedInactiveCells = new LinkedHashSet<>();
        Set<DistalDendrite> prevMatchingSegments = new LinkedHashSet<>();
        
        //Before
        
        //Check segment 0
        assertEquals(0.6, s0.getPermanence(), 0.01);
        assertEquals(0.4, s1.getPermanence(), 0.01);
        assertEquals(0.9, s2.getPermanence(), 0.01);
        
        //Check segment 1
        assertEquals(0.7, s3.getPermanence(), 0.01);
        assertEquals(1, dd1.getAllSynapses(cn).size(), 0);
        
        //Check segment 2
        assertEquals(0.9, s4.getPermanence(), 0.01);
        assertEquals(1, dd2.getAllSynapses(cn).size(), 0);
        
        //Check segment 3
        assertEquals(0, dd3.getAllSynapses(cn).size(), 0);
        
        // The tested method
        tm.learnOnSegments(cn, prevActiveSegments, learningSegments, prevActiveCells, winnerCells, prevWinnerCells, predictedInactiveCells, prevMatchingSegments);
        
        //After
        
        //Check segment 0
        assertEquals(0.7, s0.getPermanence(), 0.01); //was 0.6
        assertEquals(0.5, s1.getPermanence(), 0.01); //was 0.4
        assertEquals(0.8, s2.getPermanence(), 0.01); //was 0.9
        
        //Check segment 1
        assertEquals(0.8, s3.getPermanence(), 0.01); //was 0.7
        assertEquals(2, dd1.getAllSynapses(cn).size(), 0); // was 1
        
        //Check segment 2
        assertEquals(0.9, s4.getPermanence(), 0.01); //unchanged
        assertEquals(1, dd2.getAllSynapses(cn).size(), 0); //unchanged
        
        //Check segment 3
        assertEquals(2, dd3.getAllSynapses(cn).size(), 0);// was 0
        
        //Check total synapse count
        assertEquals(8, cn.getSynapseCount());
        
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testComputePredictiveCells() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setActivationThreshold(2);
        cn.setMinThreshold(2);
        cn.setPredictedSegmentDecrement(0.004);
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.5);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        DistalDendrite dd1 = cn.getCell(1).createSegment(cn);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(733), 0.7);
        Synapse s4 = dd1.createSynapse(cn, cn.getCell(733), 0.4);
        
        DistalDendrite dd2 = cn.getCell(1).createSegment(cn);
        Synapse s5 = dd2.createSynapse(cn, cn.getCell(974), 0.9);
        
        DistalDendrite dd3 = cn.getCell(8).createSegment(cn);
        Synapse s6 = dd3.createSynapse(cn, cn.getCell(486), 0.9);
        
        DistalDendrite dd4 = cn.getCell(100).createSegment(cn);
        
        Set<Cell> activeCells = cn.getCellSet(new int[] { 733, 37, 974, 23 });
        
        ComputeCycle cycle = new ComputeCycle();
        tm.computePredictiveCells(cn, cycle, activeCells);
        
        assertTrue(cycle.activeSegments().contains(dd) && cycle.activeSegments().size() == 1);
        assertTrue(cycle.predictiveCells().contains(cn.getCell(0)) && cycle.predictiveCells().size() == 1);
        assertTrue(cycle.matchingSegments().contains(dd) && cycle.matchingSegments().contains(dd1));
        assertTrue(cycle.matchingCells().contains(cn.getCell(0)) && cycle.matchingCells().contains(cn.getCell(1)));
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetBestMatchingCell() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setConnectedPermanence(0.50);
        cn.setMinThreshold(1);
        cn.setSeed(42);
        tm.init(cn);
       
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        DistalDendrite dd1 = cn.getCell(0).createSegment(cn);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(49), 0.9);
        Synapse s4 = dd1.createSynapse(cn, cn.getCell(3), 0.8);
        
        DistalDendrite dd2 = cn.getCell(1).createSegment(cn);
        Synapse s5 = dd2.createSynapse(cn, cn.getCell(733), 0.7);
        
        DistalDendrite dd3 = cn.getCell(1).createSegment(cn);
        Synapse s6 = dd3.createSynapse(cn, cn.getCell(486), 0.9);
        
        Set<Cell> activeCells = cn.getCellSet(new int[] { 733, 37, 974, 23 });
        
        CellSearch result = tm.getBestMatchingCell(cn, cn.getColumn(0).getCells(), activeCells);
        assertEquals(dd, result.bestSegment);
        assertEquals(0, result.bestCell.getIndex());
        
        result = tm.getBestMatchingCell(cn, cn.getColumn(3).getCells(), activeCells);
        assertNull(result.bestSegment);
        assertEquals(107, result.bestCell.getIndex());
        
        result = tm.getBestMatchingCell(cn, cn.getColumn(999).getCells(), activeCells);
        assertNull(result.bestSegment);
        assertEquals(31993, result.bestCell.getIndex());
        
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetBestMatchingCellFewestSegments() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 2 });
        cn.setCellsPerColumn(2);
        cn.setConnectedPermanence(0.50);
        cn.setMinThreshold(1);
        cn.setSeed(42);
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(3), 0.3);
        
        Set<Cell> activeCells = new HashSet<>();
        
        //Never pick cell 0, always pick cell 1
        for(int i = 0;i < 100;i++) {
            CellSearch result = tm.getBestMatchingCell(cn, cn.getColumn(0).getCells(), activeCells);
            assertEquals(1, result.bestCell.getIndex());
        }
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetBestMatchingSegment() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setConnectedPermanence(0.50);
        cn.setMinThreshold(1);
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        DistalDendrite dd1 = cn.getCell(0).createSegment(cn);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(49), 0.9);
        Synapse s4 = dd1.createSynapse(cn, cn.getCell(3), 0.8);
        
        DistalDendrite dd2 = cn.getCell(1).createSegment(cn);
        Synapse s5 = dd2.createSynapse(cn, cn.getCell(733), 0.7);
        
        DistalDendrite dd3 = cn.getCell(8).createSegment(cn);
        Synapse s6 = dd3.createSynapse(cn, cn.getCell(486), 0.9);
        
        Set<Cell> activeCells = cn.getCellSet(new int[] { 733, 37, 974, 23 });
        
        SegmentSearch result = tm.getBestMatchingSegment(cn, cn.getCell(0), activeCells);
        assertEquals(dd, result.bestSegment);
        assertEquals(2, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(1), activeCells);
        assertEquals(dd2, result.bestSegment);
        assertEquals(1, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(8), activeCells);
        assertEquals(null, result.bestSegment);
        assertEquals(0, result.numActiveSynapses);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(100), activeCells);
        assertEquals(null, result.bestSegment);
        assertEquals(0, result.numActiveSynapses);
        
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetLeastUsedCell() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 2 });
        cn.setCellsPerColumn(2);
        cn.setSeed(42);
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(3), 0.3);
        
        Column column0 = cn.getColumn(0);
        Random random = cn.getRandom();
        // Never pick cell 0, always pick cell 1
        for(int i = 0;i < 100;i++) {
            Cell leastUsed = column0.getLeastUsedCell(cn, cn.getRandom());
            assertEquals(1, leastUsed.getIndex());
        }
    }
    
    @Test
    public void testAdaptSegment() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>();
        activeSynapses.add(s0);
        activeSynapses.add(s1);
        dd.adaptSegment(cn, activeSynapses, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        
        assertEquals(0.7, s0.getPermanence(), 0.01);
        assertEquals(0.5, s1.getPermanence(), 0.01);
        assertEquals(0.8, s2.getPermanence(), 0.01);
    }
    
    @Test
    public void testSegmentSynapseDeletion() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(23), 0.4);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        assertTrue(cn.getSynapses(dd).contains(s0));
        s0.destroy(cn);
        assertFalse(cn.getSynapses(dd).contains(s0));
        assertEquals(2, cn.getSynapseCount());
        assertEquals(2, cn.getSynapses(dd).size());
        
        s1.destroy(cn);
        assertFalse(cn.getSynapses(dd).contains(s1));
        assertEquals(1, cn.getSynapseCount());
        assertEquals(1, cn.getSynapses(dd).size());
        
        s2.destroy(cn);
        assertFalse(cn.getSynapses(dd).contains(s2));
        assertEquals(0, cn.getSynapseCount());
        assertEquals(0, cn.getSynapses(dd).size());
    }
    
    @Test
    public void testAdaptSegmentToMax() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.9);
        
        Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>();
        activeSynapses.add(s0);
        
        dd.adaptSegment(cn, activeSynapses, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertEquals(1.0, s0.getPermanence(), 0.01);
        
        dd.adaptSegment(cn, activeSynapses, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertEquals(1.0, s0.getPermanence(), 0.01);
    }

    @Test
    public void testAdaptSegmentToMin() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.1);
        
        Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>();
        
        
        // Changed due to new algorithm implementation
        dd.adaptSegment(cn, activeSynapses, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertFalse(cn.getSynapses(dd).contains(s0));
    }
    
    @Test
    public void testPickCellsToLearnOn() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        
        Set<Cell> winnerCells = new LinkedHashSet<Cell>();
        winnerCells.add(cn.getCell(4));
        winnerCells.add(cn.getCell(47));
        winnerCells.add(cn.getCell(58));
        winnerCells.add(cn.getCell(93));
        
        List<Cell> learnCells = new ArrayList<Cell>(dd.pickCellsToLearnOn(cn, 2, winnerCells, cn.getRandom()));
        assertEquals(2, learnCells.size());
        assertTrue(learnCells.contains(cn.getCell(47)));
        assertTrue(learnCells.contains(cn.getCell(93)));
        
        learnCells = new ArrayList<Cell>(dd.pickCellsToLearnOn(cn, 100, winnerCells, cn.getRandom()));
        assertEquals(4, learnCells.size());
        assertEquals(93, learnCells.get(0).getIndex());
        assertEquals(58, learnCells.get(1).getIndex());
        assertEquals(47, learnCells.get(2).getIndex());
        assertEquals(4, learnCells.get(3).getIndex());
        
        learnCells = new ArrayList<Cell>(dd.pickCellsToLearnOn(cn, 0, winnerCells, cn.getRandom()));
        assertEquals(0, learnCells.size());
    }
    
    @Test
    public void testPickCellsToLearnOnAvoidDuplicates() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        dd.createSynapse(cn, cn.getCell(23), 0.6);
        
        Set<Cell> winnerCells = new LinkedHashSet<Cell>();
        winnerCells.add(cn.getCell(23));
        
        // Ensure that no additional (duplicate) cells were picked
        List<Cell> learnCells = new ArrayList<Cell>(dd.pickCellsToLearnOn(cn, 2, winnerCells, cn.getRandom()));
        assertTrue(learnCells.isEmpty());
    }
    
    @Test
    public void testColumnForCell1D() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 2048 });
        cn.setCellsPerColumn(5);
        tm.init(cn);
        
        assertEquals(0, cn.getCell(0).getColumn().getIndex());
        assertEquals(0, cn.getCell(4).getColumn().getIndex());
        assertEquals(1, cn.getCell(5).getColumn().getIndex());
        assertEquals(2047, cn.getCell(10239).getColumn().getIndex());
    }
    
    @Test
    public void testColumnForCell2D() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        tm.init(cn);
        
        assertEquals(0, cn.getCell(0).getColumn().getIndex());
        assertEquals(0, cn.getCell(3).getColumn().getIndex());
        assertEquals(1, cn.getCell(4).getColumn().getIndex());
        assertEquals(4095, cn.getCell(16383).getColumn().getIndex());
    }
}
