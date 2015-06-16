/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.research;

import static org.junit.Assert.assertEquals;
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
import org.numenta.nupic.Connections;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.research.ComputeCycle;
import org.numenta.nupic.research.TemporalMemory;


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
        
        tm.activateCorrectlyPredictiveCells(c, cn.getCellSet(prevPredictiveCells), cn.getColumnSet(activeColumns));
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
    }
    
    @Test
    public void testActivateCorrectlyPredictiveCellsEmpty() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        ComputeCycle c = new ComputeCycle();
        
        int[] prevPredictiveCells = new int[] {};
        int[] activeColumns = new int[] { 32, 47, 823 };
        
        tm.activateCorrectlyPredictiveCells(c, cn.getCellSet(prevPredictiveCells), cn.getColumnSet(activeColumns));
        Set<Cell> activeCells = c.activeCells();
        Set<Cell> winnerCells = c.winnerCells();
        Set<Column> predictedColumns = c.successfullyPredictedColumns();
        
        assertTrue(activeCells.isEmpty());
        assertTrue(winnerCells.isEmpty());
        assertTrue(predictedColumns.isEmpty());
        
        //---
        
        prevPredictiveCells = new int[] { 0, 237, 1026, 26337, 26339, 55536 };
        activeColumns = new int[] {};
        tm.activateCorrectlyPredictiveCells(c, cn.getCellSet(prevPredictiveCells), cn.getColumnSet(activeColumns));
        activeCells = c.activeCells();
        winnerCells = c.winnerCells();
        predictedColumns = c.successfullyPredictedColumns();
        
        assertTrue(activeCells.isEmpty());
        assertTrue(winnerCells.isEmpty());
        assertTrue(predictedColumns.isEmpty());
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
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4, synapseCounter++);
        dd.createSynapse(cn, cn.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd2 = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s2 = dd2.createSynapse(cn, cn.getCell(49), 0.9, synapseCounter++);
        dd2.createSynapse(cn, cn.getCell(3), 0.8, synapseCounter++);
        
        DistalDendrite dd3 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s3 = dd3.createSynapse(cn, cn.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd4 = cn.getCell(108).createSegment(cn, segmentCounter++);
        dd4.createSynapse(cn, cn.getCell(486), 0.9, synapseCounter++);
        
        int[] activeColumns = new int[] { 0, 1, 26 };
        int[] predictedColumns = new int[] {26};
        
        Map<DistalDendrite, Set<Synapse>> activeSynapseSegments = new LinkedHashMap<DistalDendrite, Set<Synapse>>();
        activeSynapseSegments.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
        activeSynapseSegments.put(dd2, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s2 })));
        activeSynapseSegments.put(dd3, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3 })));
        
        ComputeCycle cycle = new ComputeCycle();
        tm.burstColumns(cycle, cn, cn.getColumnSet(activeColumns), cn.getColumnSet(predictedColumns), activeSynapseSegments);
        
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
    }
    
    @Test
    public void testBurstColumnsEmpty() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setCellsPerColumn(4);
        cn.setConnectedPermanence(0.50);
        cn.setMinThreshold(1);
        cn.setSeed(42);
        tm.init(cn);
        
        Connections c = new Connections();
        
        int[] activeColumns = new int[] {};
        int[] predictedColumns = new int[] {};
        
        Map<DistalDendrite, Set<Synapse>> activeSynapseSegments = new LinkedHashMap<DistalDendrite, Set<Synapse>>();
        
        ComputeCycle cycle = new ComputeCycle();
        tm.burstColumns(cycle, c, cn.getColumnSet(activeColumns), cn.getColumnSet(predictedColumns), activeSynapseSegments);
        
        List<Cell> activeCells = new ArrayList<Cell>(c.getActiveCells());
        List<Cell> winnerCells = new ArrayList<Cell>(c.getWinnerCells());
        List<DistalDendrite> learningSegments = new ArrayList<DistalDendrite>(c.getLearningSegments());
        
        assertEquals(0, activeCells.size());
        assertEquals(0, winnerCells.size());
        assertEquals(0, learningSegments.size());
    }
    
    @Test
    public void testLearnOnSegments() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setMaxNewSynapseCount(2);
        tm.init(cn);
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd2 = cn.getCell(8).createSegment(cn, segmentCounter++);
        Synapse s4 = dd2.createSynapse(cn, cn.getCell(486), 0.9, synapseCounter++);
        
        DistalDendrite dd3 = cn.getCell(100).createSegment(cn, segmentCounter++);
        
        Set<DistalDendrite> prevActiveSegments = new LinkedHashSet<DistalDendrite>();
        prevActiveSegments.add(dd);
        prevActiveSegments.add(dd2);
        
        Map<DistalDendrite, Set<Synapse>> prevActiveSynapsesForSegment = new LinkedHashMap<DistalDendrite, Set<Synapse>>();
        prevActiveSynapsesForSegment.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
        prevActiveSynapsesForSegment.put(dd1, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3 })));
        
        Set<DistalDendrite> learningSegments = new LinkedHashSet<DistalDendrite>();
        learningSegments.add(dd1);
        learningSegments.add(dd3);
        Set<Cell> winnerCells = new LinkedHashSet<Cell>();
        winnerCells.add(cn.getCell(0));
        Set<Cell> prevWinnerCells = new LinkedHashSet<Cell>();
        prevWinnerCells.add(cn.getCell(10));
        prevWinnerCells.add(cn.getCell(11));
        prevWinnerCells.add(cn.getCell(12));
        prevWinnerCells.add(cn.getCell(13));
        prevWinnerCells.add(cn.getCell(14));
        
        ///////////////// Validate State Before and After //////////////////////
        
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
        
        tm.learnOnSegments(cn, prevActiveSegments, learningSegments, prevActiveSynapsesForSegment, winnerCells, prevWinnerCells);
        
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
        
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testComputePredictiveCells() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setActivationThreshold(2);
        tm.init(cn);
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.5, synapseCounter++);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(733), 0.7, synapseCounter++);
        Synapse s4 = dd1.createSynapse(cn, cn.getCell(733), 0.4, synapseCounter++);
        
        DistalDendrite dd2 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s5 = dd2.createSynapse(cn, cn.getCell(974), 0.9, synapseCounter++);
        
        DistalDendrite dd3 = cn.getCell(8).createSegment(cn, segmentCounter++);
        Synapse s6 = dd3.createSynapse(cn, cn.getCell(486), 0.9, synapseCounter++);
        
        DistalDendrite dd4 = cn.getCell(100).createSegment(cn, segmentCounter++);
        
        Map<DistalDendrite, Set<Synapse>> activeSynapseSegments = new LinkedHashMap<DistalDendrite, Set<Synapse>>();
        activeSynapseSegments.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
        activeSynapseSegments.put(dd2, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3, s4 })));
        activeSynapseSegments.put(dd3, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s5 })));
        
        ComputeCycle cycle = new ComputeCycle();
        tm.computePredictiveCells(cn, cycle, activeSynapseSegments);
        
        assertTrue(cycle.activeSegments().contains(dd) && cycle.activeSegments().size() == 1);
        assertTrue(cycle.predictiveCells().contains(cn.getCell(0)) && cycle.predictiveCells().size() == 1);
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testComputeActiveSynapses() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd2 = cn.getCell(8).createSegment(cn, segmentCounter++);
        Synapse s4 = dd2.createSynapse(cn, cn.getCell(486), 0.9, synapseCounter++);
        
        Set<Cell> activeCells = new LinkedHashSet<Cell>(
            Arrays.asList(
                new Cell[] {
                	cn.getCell(23), cn.getCell(37), cn.getCell(733), cn.getCell(4973) 
                } 
            )
        );
        
        Map<DistalDendrite, Set<Synapse>> activeSegmentSynapses = tm.computeActiveSynapses(cn, activeCells);
        
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
        Connections cn = new Connections();
        cn.setConnectedPermanence(0.50);
        cn.setMinThreshold(1);
        cn.setSeed(42);
        tm.init(cn);
       
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(49), 0.9, synapseCounter++);
        Synapse s4 = dd1.createSynapse(cn, cn.getCell(3), 0.8, synapseCounter++);
        
        DistalDendrite dd2 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s5 = dd2.createSynapse(cn, cn.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd3 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s6 = dd3.createSynapse(cn, cn.getCell(486), 0.9, synapseCounter++);
        
        Map<DistalDendrite, Set<Synapse>> activeSegments = new LinkedHashMap<DistalDendrite, Set<Synapse>>();
        activeSegments.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
        activeSegments.put(dd1, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3 })));
        activeSegments.put(dd2, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s5 })));
        
        Object[] result = tm.getBestMatchingCell(cn, cn.getColumn(0), activeSegments);
        assertEquals(dd, result[0]);
        assertEquals(0, ((Cell)result[1]).getIndex());
        
        result = tm.getBestMatchingCell(cn, cn.getColumn(3), activeSegments);
        assertNull(result[0]);
        assertEquals(107, ((Cell)result[1]).getIndex());
        
        result = tm.getBestMatchingCell(cn, cn.getColumn(999), activeSegments);
        assertNull(result[0]);
        assertEquals(31993, ((Cell)result[1]).getIndex());
        
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
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, 0);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(3), 0.3, 0);
        
        Map<DistalDendrite, Set<Synapse>> activeSegments = new LinkedHashMap<DistalDendrite, Set<Synapse>>();
        
        //Never pick cell 0, always pick cell 1
        for(int i = 0;i < 100;i++) {
            Object[] result = tm.getBestMatchingCell(cn, cn.getColumn(0), activeSegments);
            assertEquals(1, ((Cell)result[1]).getIndex());
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
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(49), 0.9, synapseCounter++);
        Synapse s4 = dd1.createSynapse(cn, cn.getCell(3), 0.8, synapseCounter++);
        
        DistalDendrite dd2 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s5 = dd2.createSynapse(cn, cn.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd3 = cn.getCell(8).createSegment(cn, segmentCounter++);
        Synapse s6 = dd3.createSynapse(cn, cn.getCell(486), 0.9, synapseCounter++);
        
        Map<DistalDendrite, Set<Synapse>> activeSegments = new LinkedHashMap<DistalDendrite, Set<Synapse>>();
        activeSegments.put(dd, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s0, s1 })));
        activeSegments.put(dd1, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s3 })));
        activeSegments.put(dd2, new LinkedHashSet<Synapse>(Arrays.asList(new Synapse[] { s5 })));
        
        DistalDendrite result = tm.getBestMatchingSegment(cn, cn.getCell(0), activeSegments);
        List<Synapse> resultSynapses = new ArrayList<Synapse>(result.getConnectedActiveSynapses(activeSegments, 0));
        assertEquals(dd, result);
        assertEquals(s0, resultSynapses.get(0));
        assertEquals(s1, resultSynapses.get(1));
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(1), activeSegments);
        resultSynapses = new ArrayList<Synapse>(result.getConnectedActiveSynapses(activeSegments, 0));
        assertEquals(dd2, result);
        assertEquals(s5, resultSynapses.get(0));
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(8), activeSegments);
        assertEquals(null, result);
        
        result = tm.getBestMatchingSegment(cn, cn.getCell(100), activeSegments);
        assertEquals(null, result);
        
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
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, 0);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(3), 0.3, 0);
        
        Column column0 = cn.getColumn(0);
        Random random = cn.getRandom();
        for(int i = 0;i < 100;i++) {
            Cell leastUsed = column0.getLeastUsedCell(cn, cn.getRandom());
            assertEquals(1, leastUsed.getIndex());
        }
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testComputeActiveSynapsesNoActivity() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd2 = cn.getCell(8).createSegment(cn, segmentCounter++);
        Synapse s4 = dd2.createSynapse(cn, cn.getCell(486), 0.9, synapseCounter++);
        
        Map<DistalDendrite, Set<Synapse>> result = tm.computeActiveSynapses(cn, new LinkedHashSet<Cell>());
        assertTrue(result.isEmpty());
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testGetConnectedActiveSynapsesForSegment() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, segmentCounter++);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9, synapseCounter++);
        
        DistalDendrite dd1 = cn.getCell(1).createSegment(cn, segmentCounter++);
        Synapse s3 = dd1.createSynapse(cn, cn.getCell(733), 0.7, synapseCounter++);
        
        DistalDendrite dd2 = cn.getCell(8).createSegment(cn, segmentCounter++);
        Synapse s4 = dd2.createSynapse(cn, cn.getCell(486), 0.9, synapseCounter++);
        
        Map<DistalDendrite, Set<Synapse>> activeSegments = new LinkedHashMap<DistalDendrite, Set<Synapse>>();
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
        Connections cn = new Connections();
        tm.init(cn);
        
        int segmentCounter = 0;
        int synapseCounter = 0;
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, segmentCounter);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.6, synapseCounter++);
        Synapse s1 = dd.createSynapse(cn, cn.getCell(37), 0.4, synapseCounter++);
        Synapse s2 = dd.createSynapse(cn, cn.getCell(477), 0.9, synapseCounter++);
        
        Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>();
        activeSynapses.add(s0);
        activeSynapses.add(s1);
        dd.adaptSegment(cn, activeSynapses, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        
        assertEquals(0.7, s0.getPermanence(), 0.01);
        assertEquals(0.5, s1.getPermanence(), 0.01);
        assertEquals(0.8, s2.getPermanence(), 0.01);
    }
    
    @Test
    public void testAdaptSegmentToMax() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, 0);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.9, 0);
        
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
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, 0);
        Synapse s0 = dd.createSynapse(cn, cn.getCell(23), 0.1, 0);
        
        Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>();
        
        dd.adaptSegment(cn, activeSynapses, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertEquals(0.0, s0.getPermanence(), 0.01);
        
        dd.adaptSegment(cn, activeSynapses, cn.getPermanenceIncrement(), cn.getPermanenceDecrement());
        assertEquals(0.0, s0.getPermanence(), 0.01);
    }
    
    @Test
    public void testPickCellsToLearnOn() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        tm.init(cn);
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, 0);
        
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
        
        DistalDendrite dd = cn.getCell(0).createSegment(cn, 0);
        dd.createSynapse(cn, cn.getCell(23), 0.6, 0);
        
        Set<Cell> winnerCells = new LinkedHashSet<Cell>();
        winnerCells.add(cn.getCell(23));
        
        List<Cell> learnCells = new ArrayList<Cell>(dd.pickCellsToLearnOn(cn, 2, winnerCells, cn.getRandom()));
        assertTrue(learnCells.isEmpty());
    }
}
