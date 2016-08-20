package org.numenta.nupic.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.OldTemporalMemory;

public class SegmentTest {

    @Test
    public void testCreateSegment() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        
        Connections connections = new Connections();
        
        retVal.apply(connections);
        new OldTemporalMemory().init(connections);
        
        Cell cell10 = connections.getCell(10);
        List<DistalDendrite> segments = connections.getSegments(cell10);
        // Establish list is empty == no current segments
        assertEquals(0, segments.size());
        
        DistalDendrite segment1 = cell10.createSegment(connections);
        assertEquals(0, segment1.getIndex());
        assertEquals(10, segment1.getParentCell().getIndex());
        
        DistalDendrite segment2 = cell10.createSegment(connections);
        assertEquals(1, segment2.getIndex());
        assertEquals(10, segment2.getParentCell().getIndex());
        
        List<DistalDendrite> expected = Arrays.asList(new DistalDendrite[] { segment1, segment2 });
        assertEquals(expected, connections.getSegments(cell10));
        assertEquals(2, connections.getSegmentCount());
    }

    @Test
    public void testCreateSegmentReuse() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 32 });
        retVal.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        retVal.setParameterByKey(KEY.MAX_SEGMENTS_PER_CELL, 2);
        
        Connections connections = new Connections();
        
        retVal.apply(connections);
        new OldTemporalMemory().init(connections);
        
        Cell cell42 = connections.getCell(42);
        
        DistalDendrite segment1 = cell42.createSegment(connections);
        segment1.createSynapse(connections, connections.getCell(1), 0.5);
        segment1.createSynapse(connections, connections.getCell(2), 0.5);
        
        
    }
}
