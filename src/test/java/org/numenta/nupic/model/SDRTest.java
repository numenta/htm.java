package org.numenta.nupic.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.SDR;


public class SDRTest {

    @Test
    public void testAsCellIndices() {
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        TemporalMemory.init(cn);
        
        int[] expectedIndexes = { 0, 3, 4, 16383 };
        Set<Cell> cells = cn.getCellSet(expectedIndexes);
        
        int[] cellIndices = SDR.asCellIndices(cells);
        
        assertTrue(Arrays.equals(cellIndices, expectedIndexes));
    }
    
    @Test
    public void testAsColumnIndices() {
        int cellsPerColumn = 4;
        
        int[] expectedIndexes = { 0, 3, 4, 4095 };
        int[] inputIndexes = Arrays.stream(expectedIndexes).map(i -> i * cellsPerColumn).toArray();
        int[] result = SDR.asColumnIndices(inputIndexes, cellsPerColumn);
        assertTrue(Arrays.equals(expectedIndexes, result));
        
        // Test failure 
        expectedIndexes = new int[] { 0, 3, 4, 4, 4095 }; // Has duplicate ("4")
        inputIndexes = Arrays.stream(expectedIndexes).map(i -> i * cellsPerColumn).toArray();
        result = SDR.asColumnIndices(inputIndexes, cellsPerColumn); // "true" is Erroneous state
        assertFalse(Arrays.equals(expectedIndexes, result));
        
        // Test correct state fixes above
        int[] arrInputIndexes = new int[] { 0, 3, 4, 4, 4095 }; // Has duplicate ("4")
        expectedIndexes = new int[] { 0, 3, 4, 4095 }; // Has duplicate ("4")
        inputIndexes = Arrays.stream(arrInputIndexes).map(i -> i * cellsPerColumn).toArray();
        result = SDR.asColumnIndices(inputIndexes, cellsPerColumn); // "false" is correct state
        assertTrue(Arrays.equals(expectedIndexes, result));
    }
    
    @Test
    public void testAsColumnIndicesList() {
        int cellsPerColumn = 4;
        
        int[] expectedIndexes = { 0, 3, 4, 4095 };
        int[] inputIndexes = Arrays.stream(expectedIndexes).map(i -> i * cellsPerColumn).toArray();
        int[] result = SDR.asColumnIndices(
            Arrays.stream(inputIndexes).boxed().collect(Collectors.toList()), cellsPerColumn);
        assertTrue(Arrays.equals(expectedIndexes, result));
        
        // Test failure 
        expectedIndexes = new int[] { 0, 3, 4, 4, 4095 }; // Has duplicate ("4")
        inputIndexes = Arrays.stream(expectedIndexes).map(i -> i * cellsPerColumn).toArray();
        result = SDR.asColumnIndices(
            Arrays.stream(inputIndexes).boxed().collect(Collectors.toList()), cellsPerColumn); // "true" is Erroneous state
        assertFalse(Arrays.equals(expectedIndexes, result));
        
        // Test correct state fixes above
        int[] arrInputIndexes = new int[] { 0, 3, 4, 4, 4095 }; // Has duplicate ("4")
        expectedIndexes = new int[] { 0, 3, 4, 4095 }; // Has duplicate ("4")
        inputIndexes = Arrays.stream(arrInputIndexes).map(i -> i * cellsPerColumn).toArray();
        System.out.println("result = " + Arrays.toString(result));
        result = SDR.asColumnIndices(
            Arrays.stream(inputIndexes).boxed().collect(Collectors.toList()), cellsPerColumn); // "false" is correct state
        assertTrue(Arrays.equals(expectedIndexes, result));
    }
    
    @Test
    public void testCellsAsColumnIndicesList() {
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        TemporalMemory.init(cn);
        
        int[] expectedIndexes = { 0, 3, 4, 4095 };
        int[] inputIndices = Arrays.stream(expectedIndexes).map(i -> i * cn.getCellsPerColumn()).toArray();
        List<Cell> cells = new ArrayList<Cell>(cn.getCellSet(inputIndices));
        
        int[] result = SDR.cellsToColumns(cells, cn.getCellsPerColumn());
        
        assertTrue(Arrays.equals(expectedIndexes, result));
    }
    
    @Test
    public void testCellsAsColumnIndicesSet() {
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        TemporalMemory.init(cn);
        
        int[] expectedIndexes = { 0, 3, 4, 4095 };
        int[] inputIndices = Arrays.stream(expectedIndexes).map(i -> i * cn.getCellsPerColumn()).toArray();
        Set<Cell> cells = cn.getCellSet(inputIndices);
        
        int[] result = SDR.cellsAsColumnIndices(cells, cn.getCellsPerColumn());
        
        assertTrue(Arrays.equals(expectedIndexes, result));
    }
   
}
