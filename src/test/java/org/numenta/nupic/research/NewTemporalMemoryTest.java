package org.numenta.nupic.research;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Segment;
import org.numenta.nupic.model.Synapse;

/**
 * Basic unit test for {@link TemporalMemory}
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class NewTemporalMemoryTest {

    @Test
    public void testActivateCorrectlyPredictiveCells() {
        
        NewTemporalMemory tm = new NewTemporalMemory();
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
        NewTemporalMemory tm = new NewTemporalMemory();
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
        NewTemporalMemory tm = new NewTemporalMemory();
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
        NewTemporalMemory tm = new NewTemporalMemory();
        Connections cn = new Connections();
        cn.setCellsPerColumn(4);
        cn.setConnectedPermanence(0.50);
        cn.setMinThreshold(1);
        cn.setSeed(42);
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4);
        dd.createSynapse(cn, cn.getCell(477), 0.9);
        
        DistalDendrite dd2 = cn.getCell(0).createSegment(cn);
        Synapse s2 = dd2.createSynapse(cn, cn.getCell(49), 0.9);
        dd2.createSynapse(cn, cn.getCell(3), 0.8);
        
        DistalDendrite dd3 = cn.getCell(1).createSegment(cn);
        Synapse s3 = dd3.createSynapse(cn, cn.getCell(733), 0.7);
        
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
}
