package org.numenta.nupic.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Connections.Activity;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwister;

import com.cedarsoftware.util.DeepEquals;

public class ConnectionsTest {
    @Test
    public void testSegmentPositionForSortKey() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.set(KEY.CELLS_PER_COLUMN, 4);

        Connections connections = new Connections();

        retVal.apply(connections);
        TemporalMemory.init(connections);
        
        Cell cell10 = connections.getCell(10);
        DistalDendrite segment0 = connections.createSegment(cell10);
        
        Cell cell9 = connections.getCell(9);
        DistalDendrite segment1 = connections.createSegment(cell9);
        
        Cell cell11 = connections.getCell(11);
        DistalDendrite segment2 = connections.createSegment(cell11);
        DistalDendrite segment3 = connections.createSegment(cell11);
        DistalDendrite segment4 = connections.createSegment(cell11);
       
        List<DistalDendrite> expected = Arrays.asList(segment1, segment0, segment2, segment3, segment4);
        List<DistalDendrite> segments = Arrays.asList(segment3, segment2, segment0, segment4, segment1);
        assertFalse(DeepEquals.deepEquals(expected, segments));
        
        Collections.sort(segments, connections.segmentPositionSortKey);
        assertTrue(DeepEquals.deepEquals(expected, segments));
    }

    @Test
    public void testCopy() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.set(KEY.CELLS_PER_COLUMN, 4);

        Connections connections = new Connections();

        retVal.apply(connections);
        TemporalMemory.init(connections);

        assertTrue(DeepEquals.deepEquals(connections, connections.copy()));
    }

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
        assertEquals(2, connections.numSegments());
    }

    @Test
    public void testCreateSegmentReuse() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.set(KEY.CELLS_PER_COLUMN, 4);
        retVal.set(KEY.MAX_SEGMENTS_PER_CELL, 2);

        Connections connections = new Connections();

        retVal.apply(connections);
        TemporalMemory.init(connections);

        Cell cell42 = connections.getCell(42);
        Cell cell1 = connections.getCell(1);
        Cell cell2 = connections.getCell(2);
        DistalDendrite segment1 = connections.createSegment(cell42);
        connections.createSynapse(segment1, cell1, 0.5);
        connections.createSynapse(segment1, cell2, 0.5);

        // Let some time pass
        connections.startNewIteration();
        connections.startNewIteration();
        connections.startNewIteration();

        // Create a segment with 3 synapses.
        Cell cell3 = connections.getCell(3);
        DistalDendrite segment2 = connections.createSegment(cell42);
        connections.createSynapse(segment2, cell1, 0.5);
        connections.createSynapse(segment2, cell2, 0.5);
        connections.createSynapse(segment2, cell3, 0.5);
        connections.startNewIteration();

        // Give the first segment some activity.
        connections.recordSegmentActivity(segment1);

        // Create a new segment with 1 synapse.
        DistalDendrite segment3 = connections.createSegment(cell42);
        connections.createSynapse(segment3, cell1, 0.5);

        List<DistalDendrite> segments = connections.getSegments(cell42);
        assertEquals(2, segments.size());

        // Verify first segment is there with same synapses.
        Set<Cell> expected = IntStream.range(1, 3).mapToObj(i -> connections.getCell(i)).collect(Collectors.toSet());
        assertTrue(DeepEquals.deepEquals(expected, 
            connections.getSynapses(segments.get(0))
                .stream()
                .map(s -> s.getPresynapticCell())
                .collect(Collectors.toSet())));

        // Verify second segment has been replaced.
        expected = IntStream.range(1, 2).mapToObj(i -> connections.getCell(i)).collect(Collectors.toSet());
        System.out.println("expected = " + expected);
        System.out.println("actual = " + connections.getSynapses(segments.get(1))
            .stream()
            .map(s -> s.getPresynapticCell())
            .collect(Collectors.toSet()));

        assertTrue(DeepEquals.deepEquals(expected, 
            connections.getSynapses(segments.get(1))
                .stream()
                .map(s -> s.getPresynapticCell())
                .collect(Collectors.toSet())));

        // Verify the flatIdxs were properly reused.
        assertTrue(segment1.getIndex() < 2);
        assertTrue(segment3.getIndex() < 2);
        assertEquals(segment1, connections.segmentForFlatIdx(segment1.getIndex()));
        assertEquals(segment3, connections.segmentForFlatIdx(segment3.getIndex()));
    }

    /**
     * Creates a synapse over the synapses per segment limit, and verifies
     * that the lowest permanence synapse is removed to make room for the new
     * synapse.
     */
    @Test
    public void testSynapseReuse() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.set(KEY.CELLS_PER_COLUMN, 4);
        retVal.set(KEY.MAX_SYNAPSES_PER_SEGMENT, 2);

        Connections connections = new Connections();

        retVal.apply(connections);
        TemporalMemory.init(connections);

        Cell cell10 = connections.getCell(10);
        DistalDendrite segment1 = connections.createSegment(cell10);
        Synapse synapse1 = connections.createSynapse(segment1, connections.getCell(50), 0.34);
        Synapse synapse2 = connections.createSynapse(segment1, connections.getCell(51), 0.48);

        assertTrue(DeepEquals.deepEquals(
            Arrays.asList(synapse1, synapse2), connections.getSynapses(segment1)));

        // Add an additional synapse to force it over the limit of num synapses
        // per segment.
        connections.createSynapse(segment1, connections.getCell(52), .52);
        
        // Ensure lower permanence synapse was removed.
        Set<Cell> expected = IntStream.range(51, 53).mapToObj(i -> connections.getCell(i)).collect(Collectors.toSet());
        assertTrue(DeepEquals.deepEquals(expected, 
            connections.getSynapses(segment1)
                .stream()
                .map(s -> s.getPresynapticCell())
                .collect(Collectors.toSet())));
    }
    
    @Test
    public void testDestroySegment() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.set(KEY.CELLS_PER_COLUMN, 4);
        
        Connections connections = new Connections();

        retVal.apply(connections);
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
        
        Activity activity = connections.computeActivity(
            IntStream.rangeClosed(80, 82)
                .mapToObj(i -> connections.getCell(i)).collect(Collectors.toList()),
                    0.5D);
        
        assertEquals(0, activity.numActiveConnected[segment2.getIndex()]);
        assertEquals(0, activity.numActivePotential[segment2.getIndex()]);
    }
    
    /**
     * Creates a segment, creates a number of synapses on it, destroys a
     * synapse, and makes sure it got destroyed.
     */
    @Test
    public void testDestroySynapse() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.set(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.set(KEY.CELLS_PER_COLUMN, 4);
        
        Connections connections = new Connections();

        retVal.apply(connections);
        TemporalMemory.init(connections);
        
        Cell cell20 = connections.getCell(20);
        DistalDendrite segment = connections.createSegment(cell20);
        Synapse synapse1 = connections.createSynapse(segment, connections.getCell(80), 0.85);
        Synapse synapse2 = connections.createSynapse(segment, connections.getCell(81), 0.85);
        Synapse synapse3 = connections.createSynapse(segment, connections.getCell(82), 0.15);
        
        assertEquals(3, connections.numSynapses());
        
        connections.destroySynapse(synapse2);
        
        assertEquals(2, connections.numSynapses());
        assertEquals(Arrays.asList(synapse1, synapse3), 
            connections.getSynapses(segment));
        
        Activity activity = connections.computeActivity(
            IntStream.rangeClosed(80, 82).mapToObj(i -> connections.getCell(i)).collect(Collectors.toList()),
                0.5D);
        
        assertEquals(1, activity.numActiveConnected[segment.getIndex()]);
        assertEquals(2, activity.numActivePotential[segment.getIndex()]);
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
        assertEquals(connections.getSynapses(segment3), l234);
        connections.destroySegment(segment5);
        assertEquals(connections.getSynapses(segment3), l234);
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
        assertEquals(0, connections.getSynapses(reincarnated).size());
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
        connections.createSegment(connections.getCell(11));
        assertEquals(2, connections.numSegments(connections.getCell(11)));
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
        connections.createSynapse(segment, connections.getCell(203), .8);
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
        connections.createSynapse(segment, connections.getCell(204), .8);
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
        
        // Cell with 1 segment.
        // Segment with:
        // - 2 connected synapse: 2 active
        // - 3 matching synapses: 3 active
        DistalDendrite segment2a = connections.createSegment(connections.getCell(20));
        connections.createSynapse(segment2a, connections.getCell(80), .85);
        connections.createSynapse(segment2a, connections.getCell(81), .85);
        Synapse synapse = connections.createSynapse(segment2a, connections.getCell(82), .85);
        synapse.setPermanence(null, 0.15);
        
        Connections c = connections;
        List<Cell> inputVec = IntStream.of(50, 52, 53, 80, 81, 82, 150, 151)
            .mapToObj(i -> c.getCell(i))
            .collect(Collectors.toList());
        
        Activity activity = c.computeActivity(inputVec, .5);
        assertEquals(1, activity.numActiveConnected[segment1a.getIndex()]);
        assertEquals(2, activity.numActivePotential[segment1a.getIndex()]);
        
        assertEquals(2, activity.numActiveConnected[segment2a.getIndex()]);
        assertEquals(3, activity.numActivePotential[segment2a.getIndex()]);

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
    
    @Test
    public void testGetPrintString() {
        Parameters p = getParameters();
        Connections con = new Connections();
        p.apply(con);
        TemporalMemory.init(con);
        
        String output = con.getPrintString();
        assertTrue(output.length() > 1000);
        
        Set<String> fieldSet = Parameters.getEncoderDefaultParameters().keys().stream().
            map(k -> k.getFieldName()).collect(Collectors.toCollection(LinkedHashSet::new));
        
        for(KEY k : p.keys()) {
            // Exclude Encoder fields
            if(fieldSet.contains(k.getFieldName())) {
                continue;
            }
            if(output.indexOf(k.getFieldName()) == -1) {
                System.out.println("missing: " + k.getFieldName());
                fail();
            }
            assertTrue(output.indexOf(k.getFieldName()) != -1);
        }
    }
    
    @Test
    public void testDoSpatialPoolerPostInit() {
        Parameters p = getParameters();
        p.set(KEY.SYN_PERM_CONNECTED, 0.2);
        p.set(KEY.SYN_PERM_ACTIVE_INC, 0.003);
        
        ///////////////////// First without Post Init /////////////////////
        SpatialPooler sp = new SpatialPooler();
        @SuppressWarnings("serial")
        Connections conn = new Connections() {
            @Override
            public void doSpatialPoolerPostInit() {
                // Override to do nothing
            }
        };
        p.apply(conn);
        sp.init(conn);
        
        double synPermConnected = conn.getSynPermConnected();
        double synPermActiveInc = conn.getSynPermActiveInc();
        double synPermBelowStimulusInc = conn.getSynPermBelowStimulusInc();
        double synPermTrimThreshold = conn.getSynPermTrimThreshold();
       
        // Assert that static values (synPermConnected & synPermActiveInc) don't change,
        // and that synPermBelowStimulusInc & synPermTrimThreshold are the defaults
        assertEquals(0.2, synPermConnected, 0.001);
        assertEquals(0.003, synPermActiveInc, 0.001);
        assertEquals(0.01, synPermBelowStimulusInc, 0.001);
        assertEquals(0.025, synPermTrimThreshold, 0.0001);
        
        
        ///////////////////// Now with Post Init /////////////////////
        sp = new SpatialPooler();
        conn = new Connections();
        p.apply(conn);
        sp.init(conn);
        
        synPermConnected = conn.getSynPermConnected();
        synPermActiveInc = conn.getSynPermActiveInc();
        synPermBelowStimulusInc = conn.getSynPermBelowStimulusInc();
        synPermTrimThreshold = conn.getSynPermTrimThreshold();
        
        // Assert that static values (synPermConnected & synPermActiveInc) don't change,
        // and that synPermBelowStimulusInc & synPermTrimThreshold change due to postInit()
        assertEquals(0.2, synPermConnected, 0.001);
        assertEquals(0.003, synPermActiveInc, 0.001);
        assertEquals(0.02, synPermBelowStimulusInc, 0.001); // affected by postInit()
        assertEquals(0.0015, synPermTrimThreshold, 0.0001);   // affected by postInit()
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
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.1);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.1);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.set(KEY.MAX_BOOST, 10.0);
        parameters.set(KEY.SEED, 42);
        
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
