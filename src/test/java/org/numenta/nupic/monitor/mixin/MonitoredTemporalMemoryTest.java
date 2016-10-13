package org.numenta.nupic.monitor.mixin;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.monitor.MonitoredTemporalMemory;
import org.numenta.nupic.util.UniversalRandom;

public class MonitoredTemporalMemoryTest {

    @Test
    public void test_mmPrettyPrintConnections() {
        MonitoredTemporalMemory mtm = new MonitoredTemporalMemory(new TemporalMemory(), new Connections());
        prepConstructs((TemporalMemory)mtm.getMonitor(), mtm.getConnections());
        
        String expected =
            "Segments: (format => (#) [(source cell=permanence ...),       ...]\n" +
            "------------------------------------\n" +
            "Column   0 / Cell   0:\t(0) []\n" +
            "Column   0 / Cell   1:\t(0) []\n" +
            "Column   0 / Cell   2:\t(0) []\n" +
            "Column   0 / Cell   3:\t(0) []\n\n" +
            
            "Column   1 / Cell   4:\t(1) [(  0=0.60   1=0.60   2=0.60   3=0.60)]\n" +
            "Column   1 / Cell   5:\t(0) []\n" +
            "Column   1 / Cell   6:\t(0) []\n" +
            "Column   1 / Cell   7:\t(0) []\n" +
            "------------------------------------";
        
        String s = mtm.mmPrettyPrintConnections().trim();
        assertTrue(expected.equals(s));
    }
    
    public void prepConstructs(TemporalMemory tm, Connections cn) {
        Parameters p = getDefaultParameters();
        p.apply(cn);
        TemporalMemory.init(cn);
        
        int[] previousActiveColumns = { 0 };
        int[] activeColumns = { 1 };
        Cell cell4 = cn.getCell(4);
        Set<Cell> expectedActiveCells = Stream.of(cell4).collect(Collectors.toSet());
        
        DistalDendrite activeSegment = cn.createSegment(cell4);
        cn.createSynapse(activeSegment, cn.getCell(0), 0.5);
        cn.createSynapse(activeSegment, cn.getCell(1), 0.5);
        cn.createSynapse(activeSegment, cn.getCell(2), 0.5);
        cn.createSynapse(activeSegment, cn.getCell(3), 0.5);
        
        ComputeCycle cc = tm.compute(cn, previousActiveColumns, true);
        assertTrue(cc.predictiveCells().equals(expectedActiveCells));
        ComputeCycle cc2 = tm.compute(cn, activeColumns, true);
        assertTrue(cc2.activeCells().equals(expectedActiveCells));
    }
    
    private Parameters getDefaultParameters() {
        Parameters retVal = Parameters.getTemporalDefaultParameters();
        retVal.set(KEY.COLUMN_DIMENSIONS, new int[] { 2 });
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
