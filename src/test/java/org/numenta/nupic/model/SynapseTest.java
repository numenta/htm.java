package org.numenta.nupic.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class SynapseTest {
    @Test
    public void testSynapseEquality() {
        // Make stuff we need to perform the tests
        Column column = new Column(1, 0);
        Cell cell1 = new Cell(column, 0);
        Cell cell2 = new Cell(column, 1);
        DistalDendrite segment1 = new DistalDendrite(cell1, 0, 0, 0);
        DistalDendrite segment2 = new DistalDendrite(cell1, 1, 1, 1);

        // These are the Synapse objects we will use for the tests
        Synapse synapse1 = new Synapse();
        Synapse synapse2 = new Synapse();

        /* ----- These are the equality tests: ----- */
        // synapse1 should equal itself
        assertTrue(synapse1.equals(synapse1));

        // synapse1 should not equal null
        assertFalse(synapse1.equals(null));

        // synapse1 should not equal a non-Synapse object
        assertFalse(synapse1.equals("This is not a Synapse object"));

        // synapse1 should not equal synapse2 because synapse2's
        // inputIndex != synapse1's inputIndex
        synapse1.setPresynapticCell(cell1);
        assertFalse(synapse1.equals(synapse2));

        // synapse1 should not equal synapse2 because synapse1's
        // segment is null, but synapse2's segment is not null
        synapse2 = new Synapse(cell1, segment1, 0, 0);
        assertFalse(synapse1.equals(synapse2));

        // synapse1 should not equal synapse2 because synapse1's
        // segment != synapse2's segment
        synapse1 = new Synapse(cell1, segment2, 0, 0);
        assertFalse(synapse1.equals(synapse2));

        // synapse1 should not equal synapse2 because synapse1's
        // sourceCell is null, but synapse2's sourceCell is not null
        synapse1.setPresynapticCell(null);
        assertFalse(synapse1.equals(synapse2));

        // synapse1 should not equal synapse2 because synapse1's
        // sourceCell != synapse2's sourceCell
        synapse1.setPresynapticCell(cell2);
        assertFalse(synapse1.equals(synapse2));

        // synapse1 should not equal synapse2 because synapse1's
        // synapseIndex != synapse2's synapseIndex
        synapse1 = new Synapse(cell1, segment1, 0, 0);
        synapse2 = new Synapse(cell1, segment1, 1, 0);
        assertFalse(synapse1.equals(synapse2));

        // synapse1 should not equal synapse2 because synapse1's
        // permanence != synapse2's permanence
        synapse1 = new Synapse(cell1, segment1, 0, 0);
        synapse2 = new Synapse(cell1, segment1, 0, 1);
        assertFalse(synapse1.equals(synapse2));

        // synapse1 should equal synapse2 because all of their
        // relevant properties are equal
        synapse1 = new Synapse(cell1, segment1, 0, 0);
        synapse2 = new Synapse(cell1, segment1, 0, 0);
        assertTrue(synapse1.equals(synapse2));
    }
}
