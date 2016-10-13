package org.numenta.nupic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.algorithms.TemporalMemory.ColumnData;
import org.numenta.nupic.util.GroupBy2;
import org.numenta.nupic.util.Tuple;
import org.numenta.nupic.util.UniversalRandom;

import chaschev.lang.Pair;

public class ComputeCycleTest {

    @Test
    public void testConversionConstructor() {
        Column column = new Column(10, 0);
        List<Cell> cells = IntStream.range(0, 10)
            .mapToObj(i -> new Cell(column, i))
            .collect(Collectors.toList());
        
        Connections cnx = new Connections();
        cnx.setActiveCells(new LinkedHashSet<Cell>(
            Arrays.asList(
                new Cell[] { cells.get(0), cells.get(1), cells.get(2), cells.get(3) })));
        
        cnx.setWinnerCells(new LinkedHashSet<Cell>(
            Arrays.asList(
                new Cell[] { cells.get(1), cells.get(3), })));
        
        ComputeCycle cc = new ComputeCycle(cnx);
        assertNotNull(cc.activeCells);
        assertEquals(4, cc.activeCells.size());
        assertNotNull(cc.winnerCells);
        assertEquals(2, cc.winnerCells.size());
        assertNotNull(cc.predictiveCells());
        assertEquals(0, cc.predictiveCells().size());
        
        ComputeCycle cc1 = new ComputeCycle(cnx);
        assertEquals(cc, cc1);
        assertEquals(cc.hashCode(), cc1.hashCode());
        
        // Now test negative equality
        cnx.setWinnerCells(new LinkedHashSet<Cell>(
            Arrays.asList(
                new Cell[] { cells.get(4), cells.get(3), })));
        ComputeCycle cc2 = new ComputeCycle(cnx);
        assertNotEquals(cc1, cc2);
        assertFalse(cc1.hashCode() == cc2.hashCode());
        
      
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testActiveColumnsRetrievable() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        Parameters p = getDefaultParameters(null, KEY.CELLS_PER_COLUMN, 1);
        p = getDefaultParameters(p, KEY.MIN_THRESHOLD, 1);
        p.apply(cn);
        TemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0, 1, 2, 3 };
        Set<Cell> prevWinnerCells = cn.getCellSet(new int[] { 0, 1, 2, 3 });
        int[] activeColumnsIndices = { 4 };
        
        DistalDendrite matchingSegment = cn.createSegment(cn.getCell(4));
        cn.createSynapse(matchingSegment, cn.getCell(0), 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertTrue(cc.winnerCells().equals(prevWinnerCells));
        //cc = tm.compute(cn, activeColumnsIndices, true);
        
        Function<Column, Column> identity = Function.identity();
        Function<DistalDendrite, Column> segToCol = segment -> segment.getParentCell().getColumn(); 
        
        List<Column> activeColumns = Arrays.stream(activeColumnsIndices)
                .sorted()
                .mapToObj(i -> cn.getColumn(i))
                .collect(Collectors.toList());
        
        GroupBy2<Column> grouper = GroupBy2.<Column>of(
            new Pair(activeColumns, identity),
            new Pair(new ArrayList(cn.getActiveSegments()), segToCol),
            new Pair(new ArrayList(cn.getMatchingSegments()), segToCol));
        
        ColumnData columnData = new ColumnData();
        for(Tuple t : grouper) { // Executes only once
            columnData = columnData.set(t);
            assertTrue(columnData.activeColumns().equals(activeColumns));
            assertTrue(columnData.activeSegments().isEmpty());
            
            List<DistalDendrite> sos = columnData.matchingSegments();
            assertEquals(1, sos.size());
            assertEquals(0, sos.get(0).getIndex());
            assertEquals(4, sos.get(0).getParentCell().getIndex());
            
            assertTrue(columnData.column().equals(cn.getColumn(4)));
        }
    }
    
    private Parameters getDefaultParameters(Parameters p, KEY key, Object value) {
        Parameters retVal = p == null ? getDefaultParameters() : p;
        retVal.set(key, value);
        
        return retVal;
    }
    
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

}
