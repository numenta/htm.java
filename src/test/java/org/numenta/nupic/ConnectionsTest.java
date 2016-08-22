package org.numenta.nupic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.numenta.nupic.Connections.Activity;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwister;


public class ConnectionsTest {
    
    @Test
    public void testCreateSegment() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        
        Connections connections = new Connections();
        
        retVal.apply(connections);
        TemporalMemory.init(connections);
        
        Cell cell10 = connections.getCell(10);
        List<DistalDendrite> segments = connections.getSegments(cell10);
        // Establish list is empty == no current segments
        assertEquals(0, segments.size());
        
        DistalDendrite segment1 = connections.createSegment(cell10);
        assertEquals(0, segment1.getIndex());
        assertEquals(10, segment1.getParentCell().getIndex());
        
        DistalDendrite segment2 = connections.createSegment(cell10);
        assertEquals(1, segment2.getIndex());
        assertEquals(10, segment2.getParentCell().getIndex());
        
        List<DistalDendrite> expected = Arrays.asList(new DistalDendrite[] { segment1, segment2 });
        assertEquals(expected, connections.getSegments(cell10));
        assertEquals(2, connections.getSegmentCount());
    }

    @Test
    public void testCreateSegmentReuse() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        p.setParameterByKey(KEY.MAX_SEGMENTS_PER_CELL, 2);
        
        Connections connections = new Connections();
        
        p.apply(connections);
        TemporalMemory.init(connections);
        
        Cell cell42 = connections.getCell(42);
        
        DistalDendrite segment1 = connections.createSegment(cell42);
        connections.createSynapse(segment1, connections.getCell(1), 0.5);
        connections.createSynapse(segment1, connections.getCell(2), 0.5);
        
        DistalDendrite segment2 = connections.createSegment(cell42);
        Set<Cell> activeInput = Arrays.stream(new Cell[] { connections.getCell(1), connections.getCell(2) }).collect(Collectors.toCollection(LinkedHashSet::new));
        
        Activity retVal = connections.newComputeActivity(activeInput, 0.5, 2, 0.1, 1, true);
        assertEquals(1, retVal.activeSegments.size());
        assertEquals(segment1, retVal.activeSegments.get(0));
        
        DistalDendrite segment3 = connections.createSegment(cell42);
        assertTrue(segment2 == segment3);
    }
    
    /**
     * Creates a segment, destroys it, and makes sure it got destroyed along
     * with all of its synapses.
     */
    @Test
    public void testDestroySegment() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        
        Connections connections = new Connections();
        
        p.apply(connections);
        TemporalMemory.init(connections);
        
        connections.createSegment(connections.getCell(10));
        DistalDendrite segment2 = connections.createSegment(connections.getCell(20));
        connections.createSegment(connections.getCell(30));
        connections.createSegment(connections.getCell(40));
        
        connections.createSynapse(segment2, connections.getCell(80), 0.85);
        connections.createSynapse(segment2, connections.getCell(81), 0.85);
        connections.createSynapse(segment2, connections.getCell(82), 0.15);
        
        assertEquals(4, connections.numSegments());
        assertEquals(3, connections.numSynapses());
        
        connections.destroySegment(segment2);
        
        assertEquals(3, connections.numSegments());
        assertEquals(0, connections.numSynapses());
        
        Connections c = connections;
        Set<Cell> activeInput = Arrays.stream(
            new Cell[] { c.getCell(80), c.getCell(81), c.getCell(82) })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        
        Activity activity = connections.newComputeActivity(activeInput, 0.5, 2, 0.1, 1, true);
        assertEquals(0, activity.activeSegments.size());
        assertEquals(0, activity.matchingSegments.size());
    }
    
    /**
     * Creates a segment, creates a number of synapses on it, destroys a
     * synapse, and makes sure it got destroyed.
     */
    @Test
    public void testDestroySynapse() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        
        Connections connections = new Connections();
        p.apply(connections);
        TemporalMemory.init(connections);
        
        DistalDendrite segment = connections.createSegment(connections.getCell(20));
        Synapse synapse1 = connections.createSynapse(segment, connections.getCell(80), 0.85);
        Synapse synapse2 = connections.createSynapse(segment, connections.getCell(81), 0.85);
        Synapse synapse3 = connections.createSynapse(segment, connections.getCell(82), 0.15);
        
        assertEquals(3, connections.numSynapses());
        
        connections.destroySynapse(synapse2);
        
        assertEquals(2, connections.numSynapses());
        List<Synapse> expected = new ArrayList<>();
        expected.add(synapse1);expected.add(synapse3);
        assertEquals(expected, connections.unDestroyedSynapsesForSegment(segment));
        
        List<Cell> actives = IntStream.of(80, 81, 82)
            .mapToObj(i -> connections.getCell(i))
            .collect(Collectors.toList());
        Activity act = connections.newComputeActivity(actives, 0.5, 2, 0.0, 1, true);
        assertEquals(0, act.activeSegments.size());
        assertEquals(1, act.matchingSegments.size());
        assertEquals(2, act.matchingSegments.get(0).getOverlap());
    }
    
    /**
     * Creates segments and synapses, then destroys segments and synapses on
     * either side of them and verifies that existing Segment and Synapse
     * instances still point to the same segment / synapse as before.
     */
    @Test
    public void testPathsNotInvalidatedByOtherDestroys() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        
        Connections connections = new Connections();
        p.apply(connections);
        TemporalMemory.init(connections);
        
        DistalDendrite segment1 = connections.createSegment(connections.getCell(11));
        connections.createSegment(connections.getCell(12));
        DistalDendrite segment3 = connections.createSegment(connections.getCell(13));
        connections.createSegment(connections.getCell(14));
        DistalDendrite segment5 = connections.createSegment(connections.getCell(15));
        
        Cell cell203 = connections.getCell(203);
        Synapse synapse1 = connections.createSynapse(segment3, connections.getCell(201), .85);
        Synapse synapse2 = connections.createSynapse(segment3, connections.getCell(202), .85);
        Synapse synapse3 = connections.createSynapse(segment3, cell203, .85);
        Synapse synapse4 = connections.createSynapse(segment3, connections.getCell(204), .85);
        Synapse synapse5 = connections.createSynapse(segment3, connections.getCell(205), .85);
        
        assertEquals(cell203, synapse3.getPresynapticCell());
        connections.destroySynapse(synapse1);
        assertEquals(cell203, synapse3.getPresynapticCell());
        connections.destroySynapse(synapse5);
        assertEquals(cell203, synapse3.getPresynapticCell());
        
        connections.destroySegment(segment1);
        List<Synapse> l234 = Arrays.stream(new Synapse[] { synapse2, synapse3, synapse4 }).collect(Collectors.toList());
        assertEquals(connections.unDestroyedSynapsesForSegment(segment3), l234);
        connections.destroySegment(segment5);
        assertEquals(connections.unDestroyedSynapsesForSegment(segment3), l234);
        assertEquals(cell203, synapse3.getPresynapticCell());
    }
    
    /**
     * Destroy a segment that has a destroyed synapse and a non-destroyed
     * synapse. Make sure nothing gets double-destroyed.
     */
    @Test
    public void testDestroySegmentWithDestroyedSynapses() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        
        Connections connections = new Connections();
        p.apply(connections);
        TemporalMemory.init(connections);
        
        DistalDendrite segment1 = connections.createSegment(connections.getCell(11));
        DistalDendrite segment2 = connections.createSegment(connections.getCell(12));
        
        connections.createSynapse(segment1, connections.getCell(101), .85);
        Synapse synapse2a = connections.createSynapse(segment2, connections.getCell(201), .85);
        connections.createSynapse(segment2, connections.getCell(202), .85);
        
        assertEquals(3, connections.numSynapses());
        
        connections.destroySynapse(synapse2a);
        
        assertEquals(2, connections.numSegments());
        assertEquals(2, connections.numSynapses());
        
        connections.destroySegment(segment2);
        
        assertEquals(1, connections.numSegments());
        assertEquals(1, connections.numSynapses());
    }

    @Test
    public void testColumnForCell1D() {
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 2048 });
        cn.setCellsPerColumn(5);
        TemporalMemory.init(cn);
        
        assertEquals(0, cn.getCell(0).getColumn().getIndex());
        assertEquals(0, cn.getCell(4).getColumn().getIndex());
        assertEquals(1, cn.getCell(5).getColumn().getIndex());
        assertEquals(2047, cn.getCell(10239).getColumn().getIndex());
    }
    
    @Test
    public void testColumnForCell2D() {
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        TemporalMemory.init(cn);
        
        assertEquals(0, cn.getCell(0).getColumn().getIndex());
        assertEquals(0, cn.getCell(3).getColumn().getIndex());
        assertEquals(1, cn.getCell(4).getColumn().getIndex());
        assertEquals(4095, cn.getCell(16383).getColumn().getIndex());
    }
    
    @Test
    public void testAsCellIndexes() {
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        TemporalMemory.init(cn);
        
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
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        TemporalMemory.init(cn);
        
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
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        TemporalMemory.init(cn);
        
        int[] indexes = { 0, 3, 4, 16383 };
        Set<Integer> idxSet = new HashSet<Integer>(
            IntStream.of(indexes).boxed().collect(Collectors.toList()));
        
        List<Cell> cells = cn.asCellObjects(idxSet);
        for(Cell cell : cells) 
            assertTrue(idxSet.contains(cell.getIndex()));
    }

    @Test
    public void testAsColumnObjects() {
        Connections cn = new Connections();
        cn.setColumnDimensions(new int[] { 64, 64 });
        cn.setCellsPerColumn(4);
        TemporalMemory.init(cn);
        
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
        TemporalMemory tm = new TemporalMemory();
        TemporalMemory.init(con);
        
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
