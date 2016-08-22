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
import org.numenta.nupic.Connections.SegmentOverlap;
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
        retVal.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.set(KEY.CELLS_PER_COLUMN, 4);
        
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
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.set(KEY.MAX_SEGMENTS_PER_CELL, 2);
        
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
        assertEquals(segment1, retVal.activeSegments.get(0).segment);
        
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
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        
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
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        
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
        assertEquals(2, act.matchingSegments.get(0).overlap);
    }
    
    /**
     * Creates segments and synapses, then destroys segments and synapses on
     * either side of them and verifies that existing Segment and Synapse
     * instances still point to the same segment / synapse as before.
     */
    @Test
    public void testPathsNotInvalidatedByOtherDestroys() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        
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
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        
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
    
    /**
     * Destroy a segment that has a destroyed synapse and a non-destroyed
     * synapse. Create a new segment in the same place. Make sure its synapse
     * count is correct.
     */
    @Test
    public void testReuseSegmentWithDestroyedSynapses() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        
        Connections connections = new Connections();
        p.apply(connections);
        TemporalMemory.init(connections);
        
        DistalDendrite segment = connections.createSegment(connections.getCell(11));
        
        Synapse synapse1 = connections.createSynapse(segment, connections.getCell(201), .85);
        connections.createSynapse(segment, connections.getCell(202), .85);
        
        connections.destroySynapse(synapse1);
        
        assertEquals(1, connections.numSynapses(segment));
        
        connections.destroySegment(segment);
        
        DistalDendrite reincarnated = connections.createSegment(connections.getCell(11));
        
        assertEquals(0, connections.numSynapses(reincarnated));
        assertEquals(0, connections.unDestroyedSynapsesForSegment(reincarnated).size());
    }
    
    /**
     * Destroy some segments then verify that the maxSegmentsPerCell is still
     * correctly applied.
     */
    @Test
    public void testDestroySegmentsThenReachLimit() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.set(KEY.MAX_SEGMENTS_PER_CELL, 2);
        p.set(KEY.MAX_SYNAPSES_PER_SEGMENT, 2);
        
        Connections connections = new Connections();
        p.apply(connections);
        TemporalMemory.init(connections);
        
        DistalDendrite segment1 = connections.createSegment(connections.getCell(11));
        DistalDendrite segment2 = connections.createSegment(connections.getCell(11));
        
        assertEquals(2, connections.numSegments());
        connections.destroySegment(segment1);
        connections.destroySegment(segment2);
        assertEquals(0, connections.numSegments());
        
        connections.createSegment(connections.getCell(11));
        assertEquals(1, connections.numSegments());
        connections.createSegment(connections.getCell(11));
        assertEquals(2, connections.numSegments());
        DistalDendrite segment3 = connections.createSegment(connections.getCell(11));
        assertTrue(segment3.getIndex() < 2);
        assertEquals(2, connections.numSegments());
    }
    
    /**
     * Destroy some synapses then verify that the maxSynapsesPerSegment is
     * still correctly applied.
     */
    @Test
    public void testDestroySynapsesThenReachLimit() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.set(KEY.MAX_SEGMENTS_PER_CELL, 2);
        p.set(KEY.MAX_SYNAPSES_PER_SEGMENT, 2);
        
        Connections connections = new Connections();
        p.apply(connections);
        TemporalMemory.init(connections);
        
        DistalDendrite segment = connections.createSegment(connections.getCell(10));
        
        Synapse synapse1 = connections.createSynapse(segment, connections.getCell(201), .85);
        Synapse synapse2 = connections.createSynapse(segment, connections.getCell(202), .85);
        
        assertEquals(2, connections.numSynapses());
        connections.destroySynapse(synapse1);
        connections.destroySynapse(synapse2);
        assertEquals(0, connections.numSynapses());
        
        connections.createSynapse(segment, connections.getCell(201), .85);
        assertEquals(1, connections.numSynapses());
        connections.createSynapse(segment, connections.getCell(202), .90);
        assertEquals(2, connections.numSynapses());
        Synapse synapse3 = connections.createSynapse(segment, connections.getCell(203), .8);
        assertTrue(synapse3.getIndex() < 2);
        assertEquals(2, connections.numSynapses());
    }
    
    /**
     * Hit the maxSynapsesPerSegment threshold multiple times. Make sure it
     * works more than once.
     */
    @Test
    public void testReachSegmentLimitMultipleTimes() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.set(KEY.MAX_SEGMENTS_PER_CELL, 2);
        p.set(KEY.MAX_SYNAPSES_PER_SEGMENT, 2);
        
        Connections connections = new Connections();
        p.apply(connections);
        TemporalMemory.init(connections);
        
        DistalDendrite segment = connections.createSegment(connections.getCell(10));
        connections.createSynapse(segment, connections.getCell(201), .85);
        assertEquals(1, connections.numSynapses());
        connections.createSynapse(segment, connections.getCell(202), .9);
        assertEquals(2, connections.numSynapses());
        connections.createSynapse(segment, connections.getCell(203), .8);
        assertEquals(2, connections.numSynapses());
        Synapse synapse = connections.createSynapse(segment, connections.getCell(204), .8);
        assertTrue(synapse.getIndex() < 2);
        assertEquals(2, connections.numSynapses());
    }
    
    /**
     * Creates a sample set of connections, and makes sure that computing the
     * activity for a collection of cells with no activity returns the right
     * activity data.
     */
    @Test
    public void testComputeActivity() {
        Parameters p = Parameters.getTemporalDefaultParameters();
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        
        Connections connections = new Connections();
        p.apply(connections);
        TemporalMemory.init(connections);
        
        // Cell with 1 segment.
        // Segment with:
        // - 1 connected synapse: active
        // - 2 matching synapses
        DistalDendrite segment1a = connections.createSegment(connections.getCell(10));
        connections.createSynapse(segment1a, connections.getCell(150), .85);
        connections.createSynapse(segment1a, connections.getCell(151), .15);
        
        // Cell with 2 segments.
        // Segment with:
        // - 1 connected synapse: active
        // - 2 matching synapses
        DistalDendrite segment2a = connections.createSegment(connections.getCell(20));
        connections.createSynapse(segment2a, connections.getCell(80), .85);
        connections.createSynapse(segment2a, connections.getCell(81), .85);
        Synapse synapse = connections.createSynapse(segment2a, connections.getCell(82), .85);
        synapse.setPermanence(null, 0.15);
        
        // Segment with:
        // - 2 connected synapses: 1 active, 1 inactive
        // - 3 matching synapses: 2 active, 1 inactive
        // - 1 non-matching synapse: 1 active
        DistalDendrite segment2b = connections.createSegment(connections.getCell(20));
        connections.createSynapse(segment2b, connections.getCell(50), .85);
        connections.createSynapse(segment2b, connections.getCell(51), .85);
        connections.createSynapse(segment2b, connections.getCell(52), .15);
        connections.createSynapse(segment2b, connections.getCell(53), .05);
        
        // Cell with 1 segment.
        // Segment with:
        // - 1 non-matching synapse: 1 active
        DistalDendrite segment3a = connections.createSegment(connections.getCell(30));
        connections.createSynapse(segment3a, connections.getCell(53), .05);
        
        Connections c = connections;
        List<Cell> inputVec = IntStream.of(50, 52, 53, 80, 81, 82, 150, 151)
            .mapToObj(i -> c.getCell(i))
            .collect(Collectors.toList());
        
        Activity activity = c.newComputeActivity(inputVec, .5, 2, .1, 1, true);
        List<SegmentOverlap> active = activity.activeSegments;
        List<SegmentOverlap> matching = activity.matchingSegments;
        
        assertEquals(1, active.size());
        assertEquals(segment2a, active.get(0).segment);
        assertEquals(2, active.get(0).overlap);
        
        assertEquals(3, matching.size());
        assertEquals(segment1a, matching.get(0).segment);
        assertEquals(2, matching.get(0).overlap);
        assertEquals(segment2a, matching.get(1).segment);
        assertEquals(3, matching.get(1).overlap);
        assertEquals(segment2b, matching.get(2).segment);
        assertEquals(2, matching.get(2).overlap);
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
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 20 });
        parameters.set(KEY.CELLS_PER_COLUMN, 6);
        
        //SpatialPooler specific
        parameters.set(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.set(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.set(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.set(KEY.MAX_BOOST, 10.0);
        parameters.set(KEY.SEED, 42);
        parameters.set(KEY.SP_VERBOSITY, 0);
        
        //Temporal Memory specific
        parameters.set(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.set(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.set(KEY.MIN_THRESHOLD, 5);
        parameters.set(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.set(KEY.PERMANENCE_INCREMENT, 0.05);
        parameters.set(KEY.PERMANENCE_DECREMENT, 0.05);
        parameters.set(KEY.ACTIVATION_THRESHOLD, 4);
        parameters.set(KEY.RANDOM, new MersenneTwister(42));
        
        return parameters;
    }
}
