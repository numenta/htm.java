package org.numenta.nupic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;


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
        
        cnx.setPredictiveCells(new LinkedHashSet<Cell>(
            Arrays.asList(
                new Cell[] { cells.get(2), cells.get(3), cells.get(4), cells.get(5) })));
        
        cnx.setSuccessfullyPredictedColumns(new LinkedHashSet<Column>(
            Arrays.asList(new Column[] { column })));
        
        ComputeCycle cc = new ComputeCycle(cnx);
        assertNotNull(cc.activeCells);
        assertEquals(4, cc.activeCells.size());
        assertNotNull(cc.winnerCells);
        assertEquals(2, cc.winnerCells.size());
        assertNotNull(cc.predictiveCells());
        assertEquals(4, cc.predictiveCells().size());
        assertNotNull(cc.successfullyPredictedColumns);
        assertEquals(1, cc.successfullyPredictedColumns.size());
        
        assertNotNull(cc.activeSegments);
        assertTrue(cc.activeSegments.isEmpty());
        assertNotNull(cc.learningSegments);
        assertTrue(cc.learningSegments.isEmpty());
        assertNotNull(cc.predictedInactiveCells);
        assertTrue(cc.predictedInactiveCells.isEmpty());
        assertNotNull(cc.matchingSegments);
        assertTrue(cc.matchingSegments.isEmpty());
        assertNotNull(cc.matchingCells);
        assertTrue(cc.matchingCells.isEmpty());
        
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

}
