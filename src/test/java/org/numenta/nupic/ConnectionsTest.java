package org.numenta.nupic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;


public class ConnectionsTest {

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
    
    @Test
    public void testAsCellIndexes() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        tm.init(cn);
        
        int[] expectedIndexes = { 0, 3, 4, 16383 };
        Set<Cell> cells = cn.getCellSet(expectedIndexes);
        
        List<Integer> cellIdxList = Connections.asCellIndexes(cells);
        
        // Unordered test of equality
        Set<Integer> cellIdxSet = new HashSet<>(cellIdxList);
        Set<Integer> expectedIdxSet = new HashSet<Integer>(
            IntStream.of(expectedIndexes).boxed().collect(Collectors.toList()));
        assertTrue(cellIdxSet.equals(expectedIdxSet));
    }
    
    @Test
    public void testAsColumnIndexes() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        tm.init(cn);
        
        int[] expectedIndexes = { 0, 3, 4, 4095 };
        Set<Column> columns = cn.getColumnSet(expectedIndexes);
        
        List<Integer> columnIdxList = Connections.asColumnIndexes(columns);
        
        // Unordered test of equality
        Set<Integer> columnIdxSet = new HashSet<>(columnIdxList);
        Set<Integer> expectedIdxSet = new HashSet<Integer>(
            IntStream.of(expectedIndexes).boxed().collect(Collectors.toList()));
        assertTrue(columnIdxSet.equals(expectedIdxSet));
    }
    
    @Test
    public void testAsCellObjects() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        tm.init(cn);
        
        int[] indexes = { 0, 3, 4, 16383 };
        Set<Integer> idxSet = new HashSet<Integer>(
            IntStream.of(indexes).boxed().collect(Collectors.toList()));
        
        List<Cell> cells = cn.asCellObjects(idxSet);
        for(Cell cell : cells) 
            assertTrue(idxSet.contains(cell.getIndex()));
    }

    @Test
    public void testAsColumnObjects() {
        TemporalMemory tm = new TemporalMemory();
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        tm.init(cn);
        
        int[] indexes = { 0, 3, 4, 4095 };
        Set<Integer> idxSet = new HashSet<Integer>(
            IntStream.of(indexes).boxed().collect(Collectors.toList()));
        
        List<Column> columns = cn.asColumnObjects(idxSet);
        for(Column column : columns) 
            assertTrue(idxSet.contains(column.getIndex()));
    }
}
