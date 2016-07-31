package org.numenta.nupic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.OldTemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwister;


public class ConnectionsTest {

    @Test
    public void testColumnForCell1D() {
        OldTemporalMemory tm = new OldTemporalMemory();
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
        OldTemporalMemory tm = new OldTemporalMemory();
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
        OldTemporalMemory tm = new OldTemporalMemory();
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
        OldTemporalMemory tm = new OldTemporalMemory();
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
        OldTemporalMemory tm = new OldTemporalMemory();
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
        OldTemporalMemory tm = new OldTemporalMemory();
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
    
    @Test
    public void testClear() {
        final int[] input1 = new int[] { 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0 };
        final int[] input2 = new int[] { 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0 };
        final int[] input3 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
        final int[] input4 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input5 = new int[] { 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0 };
        final int[] input6 = new int[] { 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 };
        final int[] input7 = new int[] { 0, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0 };
        final int[][] inputs = { input1, input2, input3, input4, input5, input6, input7 };
        
        Parameters p = getParameters();
        Connections con = new Connections();
        p.apply(con);
        OldTemporalMemory tm = new OldTemporalMemory();
        tm.init(con);
        
        for(int x = 0;x < 602;x++) {
            for(int[] i : inputs) {
                tm.compute(con, ArrayUtils.where(i, ArrayUtils.WHERE_1), true);
            }
        }
        
        assertFalse(con.getActiveCells().isEmpty());
        con.clear();
        assertTrue(con.getActiveCells().isEmpty());
    }
    
    public static Parameters getParameters() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 20 });
        parameters.setParameterByKey(KEY.CELLS_PER_COLUMN, 6);
        
        //SpatialPooler specific
        parameters.setParameterByKey(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.setParameterByKey(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.setParameterByKey(KEY.GLOBAL_INHIBITION, false);
        parameters.setParameterByKey(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.setParameterByKey(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.setParameterByKey(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.setParameterByKey(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.setParameterByKey(KEY.MAX_BOOST, 10.0);
        parameters.setParameterByKey(KEY.SEED, 42);
        parameters.setParameterByKey(KEY.SP_VERBOSITY, 0);
        
        //Temporal Memory specific
        parameters.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.setParameterByKey(KEY.MIN_THRESHOLD, 5);
        parameters.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.05);
        parameters.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.05);
        parameters.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 4);
        parameters.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        return parameters;
    }
}
