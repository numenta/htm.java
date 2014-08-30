/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.numenta.nupic.data.ConsecutivePatternMachine;
import org.numenta.nupic.data.SequenceMachine;
import org.numenta.nupic.integration.TemporalMemoryTestMachine.DetailedResults;
import org.numenta.nupic.research.Parameters;
import org.numenta.nupic.research.Parameters.KEY;


/**
 * Basic {@link TemporalMemory} class
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class BasicTemporalMemoryTest extends AbstractTemporalMemoryTest {
    
    /**
     * Basic static input for all tests in this class
     */
    private void defaultSetup() {
        parameters = new Parameters();
        EnumMap<Parameters.KEY, Object> p = parameters.getMap();
        p.put(KEY.COLUMN_DIMENSIONS, new int[] { 6 });
        p.put(KEY.CELLS_PER_COLUMN, 4);
        p.put(KEY.INITIAL_PERMANENCE, 0.3);
        p.put(KEY.CONNECTED_PERMANENCE, 0.5);
        p.put(KEY.MIN_THRESHOLD, 1);
        p.put(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        p.put(KEY.PERMANENCE_INCREMENT, 0.1);
        p.put(KEY.PERMANENCE_DECREMENT, 0.05);
        p.put(KEY.ACTIVATION_THRESHOLD, 1);
    }
    
    /**
     * Basic first order sequences
     */
    @Test
    public void testA() {
        defaultSetup();
        
        //Basic first order sequences
        initTM();
        
        assertEquals(0.05, tm.getPermanenceDecrement(), .001);
        assertEquals(0.1, tm.getPermanenceIncrement(), .001);
        
        finishSetUp(new ConsecutivePatternMachine(6, 1));
        
        List<Integer> input = Arrays.asList(new Integer[] { 0, 1, 2, 3, -1 });
        sequence = sequenceMachine.generateFromNumbers(input);
        
        DetailedResults detailedResults = feedTM(sequence, true, 1);
        assertEquals(0, detailedResults.predictedActiveColumnsList.get(3).size(), 0);
        
        feedTM(sequence, true, 2);
        
        detailedResults = feedTM(sequence, true, 1);
        assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
        
        feedTM(sequence, true, 4);
        
        detailedResults = feedTM(sequence, true, 1);
        assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
    }
    
    /**
     * High order sequences in order
     */
    @Test
    public void testB() {
        defaultSetup();
        
        initTM();
        
        assertEquals(0.05, tm.getPermanenceDecrement(), .001);
        assertEquals(0.1, tm.getPermanenceIncrement(), .001);
        
        finishSetUp(new ConsecutivePatternMachine(6, 1));
        
        List<Integer> inputA = Arrays.asList(new Integer[] { 0, 1, 2, 3, -1 });
        List<Integer> inputB = Arrays.asList(new Integer[] { 4, 1, 2, 5, -1 });
        
        List<Set<Integer>> sequenceA = sequenceMachine.generateFromNumbers(inputA);
        List<Set<Integer>> sequenceB = sequenceMachine.generateFromNumbers(inputB);
        
        feedTM(sequenceA, true, 5);
        
        DetailedResults detailedResults = feedTM(sequenceA, false, 1);
        assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
        
        feedTM(sequenceB, true, 1);
        feedTM(sequenceB, true, 2);
        detailedResults = feedTM(sequenceB, false, 1);
        assertEquals(1, detailedResults.predictedActiveColumnsList.get(1).size());
        
        feedTM(sequenceB, true, 3);
        detailedResults = feedTM(sequenceB, false, 1);
        assertEquals(1, detailedResults.predictedActiveColumnsList.get(2).size());
        
        feedTM(sequenceB, true, 3);
        detailedResults = feedTM(sequenceB, false, 1);
        assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
        
        detailedResults = feedTM(sequenceA, false, 1);
        assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
        assertEquals(1, detailedResults.predictedInactiveColumnsList.get(3).size());
        
        feedTM(sequenceA, true, 10);
        assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
        /** TODO:   Requires some form of synaptic decay to forget the ABC=>Y
         *          transition that's initially formed
         * assertEquals(1, detailedResults.predictedInactiveColumnsList.get(3).size());
         */
    }
    
    /**
     * High order sequences (alternating)
     */
    @Test
    public void testC() {
        defaultSetup();
        
        initTM();
        
        assertEquals(0.05, tm.getPermanenceDecrement(), .001);
        assertEquals(0.1, tm.getPermanenceIncrement(), .001);
        
        finishSetUp(new ConsecutivePatternMachine(6, 1));
        
        List<Integer> inputA = Arrays.asList(new Integer[] { 0, 1, 2, 3, -1 });
        List<Integer> inputB = Arrays.asList(new Integer[] { 4, 1, 2, 5, -1 });
        
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(inputA);
        sequence.addAll(sequenceMachine.generateFromNumbers(inputB));
        
        feedTM(sequence, true, 1);
        feedTM(sequence, true, 10);
        
        DetailedResults detailedResults = feedTM(sequence, false, 1);
        //TODO: Requires some form of synaptic decay to forget the
        //      ABC=>Y and XBC=>D transitions that are initially formed
        assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
        assertEquals(1, detailedResults.predictedInactiveColumnsList.get(8).size());
    }
    
    /**
     * Endlessly repeating sequence of 2 elements
     */
    @Test
    public void testD() {
        defaultSetup();
        parameters.setColumnDimensions(new int[] { 2 });
        
        initTM();
        
        assertEquals(0.05, tm.getPermanenceDecrement(), .001);
        assertEquals(0.1, tm.getPermanenceIncrement(), .001);
        assertTrue(Arrays.equals(new int[] { 2 }, tm.getColumnDimensions()));
        
        finishSetUp(new ConsecutivePatternMachine(2, 1));
        
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(
            Arrays.asList(new Integer[] { 0, 1 }));
        
        for(int i = 0;i < 7;i++) {
            feedTM(sequence, true, 1);
        }
        
        feedTM(sequence, true, 50);
    }
    
    /**
     * Endlessly repeating sequence of 2 elements with maxNewSynapseCount=1
     */
    @Test
    public void testE() {
        defaultSetup();
        parameters.setColumnDimensions(new int[] { 2 });
        parameters.setMaxNewSynapseCount(1);
        parameters.setCellsPerColumn(10);
        
        initTM();
        
        assertEquals(0.05, tm.getPermanenceDecrement(), .001);
        assertEquals(0.1, tm.getPermanenceIncrement(), .001);
        assertTrue(Arrays.equals(new int[] { 2 }, tm.getColumnDimensions()));
        
        finishSetUp(new ConsecutivePatternMachine(2, 1));
        
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(
            Arrays.asList(new Integer[] { 0, 1 }));
        
        for(int i = 0;i < 7;i++) {
            feedTM(sequence, true, 1);
        }
        
        DetailedResults results = feedTM(sequence, true, 100);
        assertEquals(200, results.predictedActiveColumnsList.size());
        assertEquals(200, results.predictedInactiveColumnsList.size());
    }
    
    /**
     * Long repeating sequence with novel pattern at the end
     */
    @Test
    public void testF() {
        defaultSetup();
        parameters.setColumnDimensions(new int[] { 3 });
        
        initTM();
        
        assertTrue(Arrays.equals(new int[] { 3 }, tm.getColumnDimensions()));
        
        finishSetUp(new ConsecutivePatternMachine(3, 1));
        
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(
            Arrays.asList(new Integer[] { 0, 1 }));
        for(int i = 0;i < 9;i++) {
            sequence.addAll(sequenceMachine.generateFromNumbers(
                Arrays.asList(new Integer[] { 0, 1 })));
        }
        
        sequence.add(patternMachine.get(2));
        sequence.add(SequenceMachine.NONE);
        
        for(int i = 0;i < 4;i++) {
            feedTM(sequence, true, 1);
        }
        
        feedTM(sequence, true, 10);
    }
    
    /**
     * A single endlessly repeating pattern
     */
    @Test
    public void testG() {
        defaultSetup();
        parameters.setColumnDimensions(new int[] { 1 });
        
        initTM();
        
        assertTrue(Arrays.equals(new int[] { 1 }, tm.getColumnDimensions()));
        
        finishSetUp(new ConsecutivePatternMachine(1, 1));
        
        List<Set<Integer>> sequence = new ArrayList<Set<Integer>>();
        sequence.add(patternMachine.get(0));
        
        for(int i = 0;i < 4;i++) {
            feedTM(sequence, true, 1);
        }
        
        for(int i = 0;i < 2;i++) {
            feedTM(sequence, true, 10);
        }
    }

}
